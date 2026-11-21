package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CatalogApp
import com.example.data.model.GroundingSource
import com.example.data.model.ScoutHistoryEntry
import com.example.data.repository.AiScoutRepository
import com.example.data.repository.ScoutStreamChunk
import com.example.data.repository.StoreRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ScoutUiState {
    data object Idle : ScoutUiState
    data object Loading : ScoutUiState
    data class Streaming(
        val query: String,
        val text: String,
        val sources: List<GroundingSource>,
        val searchEntryPointHtml: String?
    ) : ScoutUiState

    data class Success(
        val query: String,
        val text: String,
        val sources: List<GroundingSource>,
        val searchEntryPointHtml: String?,
        val extractedApps: List<CatalogApp>,
        val isExtractingApps: Boolean = false
    ) : ScoutUiState

    data class Error(val message: String) : ScoutUiState
}

sealed interface AppBriefUiState {
    data object Idle : AppBriefUiState
    data object Loading : AppBriefUiState
    data class Streaming(
        val text: String,
        val sources: List<GroundingSource>,
        val searchEntryPointHtml: String?
    ) : AppBriefUiState

    data class Success(
        val text: String,
        val sources: List<GroundingSource>,
        val searchEntryPointHtml: String?
    ) : AppBriefUiState

    data class Error(val message: String) : AppBriefUiState
}

class AiScoutViewModel(
    private val aiRepository: AiScoutRepository,
    private val storeRepository: StoreRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _scoutState = MutableStateFlow<ScoutUiState>(ScoutUiState.Idle)
    val scoutState: StateFlow<ScoutUiState> = _scoutState.asStateFlow()

    private val _appBriefState = MutableStateFlow<AppBriefUiState>(AppBriefUiState.Idle)
    val appBriefState: StateFlow<AppBriefUiState> = _appBriefState.asStateFlow()

    private var activeBriefAppId: String? = null

    val historyState: StateFlow<UiState<List<ScoutHistoryEntry>>> =
        storeRepository.observeScoutHistory(currentUserId)
            .map<List<ScoutHistoryEntry>, UiState<List<ScoutHistoryEntry>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing scout history", root)
                emit(UiState.Error(root.message ?: "Failed to load Scout history"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    fun runScoutSearch(query: String, onAppsDiscovered: (List<CatalogApp>) -> Unit = {}) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return

        viewModelScope.launch {
            _scoutState.value = ScoutUiState.Loading
            try {
                var finalResult: ScoutStreamChunk.Completed? = null
                aiRepository.streamGroundedAppDiscovery(cleanQuery).collect { chunk ->
                    when (chunk) {
                        is ScoutStreamChunk.Delta -> {
                            _scoutState.value = ScoutUiState.Streaming(
                                query = cleanQuery,
                                text = chunk.accumulatedText,
                                sources = chunk.sources,
                                searchEntryPointHtml = chunk.searchEntryPointHtml
                            )
                        }
                        is ScoutStreamChunk.Completed -> {
                            finalResult = chunk
                            _scoutState.value = ScoutUiState.Success(
                                query = cleanQuery,
                                text = chunk.fullText,
                                sources = chunk.sources,
                                searchEntryPointHtml = chunk.searchEntryPointHtml,
                                extractedApps = emptyList(),
                                isExtractingApps = true
                            )
                        }
                    }
                }

                val completed = finalResult
                if (completed != null) {
                    // Save discovery summary to user's Firestore Scout history
                    storeRepository.saveScoutHistory(
                        uid = currentUserId,
                        query = cleanQuery,
                        summary = completed.fullText,
                        sourcesCount = completed.sources.size
                    )

                    // Extract structured interactive app cards via Gemini JSON schema
                    val extracted = try {
                        aiRepository.extractStructuredApps(completed.fullText)
                            .mapIndexed { idx, item -> item.toCatalogApp(idx) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Structured app card extraction skipped", e)
                        emptyList()
                    }
                    onAppsDiscovered(extracted)
                    _scoutState.value = ScoutUiState.Success(
                        query = cleanQuery,
                        text = completed.fullText,
                        sources = completed.sources,
                        searchEntryPointHtml = completed.searchEntryPointHtml,
                        extractedApps = extracted,
                        isExtractingApps = false
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Scout timed out", e)
                _scoutState.value = ScoutUiState.Error(
                    "Search timed out after 60 seconds. Please try a shorter query."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Scout failed", e)
                _scoutState.value = ScoutUiState.Error(
                    e.localizedMessage ?: "AI Scout could not complete the search."
                )
            }
        }
    }

    fun loadHistoryEntry(entry: ScoutHistoryEntry) {
        _scoutState.value = ScoutUiState.Success(
            query = entry.query,
            text = entry.summary,
            sources = emptyList(),
            searchEntryPointHtml = null,
            extractedApps = emptyList(),
            isExtractingApps = false
        )
    }

    fun deleteHistoryEntry(entryId: String) {
        viewModelScope.launch {
            storeRepository.deleteScoutHistory(currentUserId, entryId)
        }
    }

    fun resetAppBriefIfNeeded(appId: String) {
        if (activeBriefAppId != appId) {
            activeBriefAppId = appId
            _appBriefState.value = AppBriefUiState.Idle
        }
    }

    fun analyzeApp(app: CatalogApp) {
        activeBriefAppId = app.id
        viewModelScope.launch {
            _appBriefState.value = AppBriefUiState.Loading
            try {
                aiRepository.streamAppSecurityAndComparisonBrief(app).collect { chunk ->
                    when (chunk) {
                        is ScoutStreamChunk.Delta -> {
                            _appBriefState.value = AppBriefUiState.Streaming(
                                text = chunk.accumulatedText,
                                sources = chunk.sources,
                                searchEntryPointHtml = chunk.searchEntryPointHtml
                            )
                        }
                        is ScoutStreamChunk.Completed -> {
                            _appBriefState.value = AppBriefUiState.Success(
                                text = chunk.fullText,
                                sources = chunk.sources,
                                searchEntryPointHtml = chunk.searchEntryPointHtml
                            )
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "App brief timed out", e)
                _appBriefState.value = AppBriefUiState.Error("Analysis timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "App brief failed", e)
                _appBriefState.value = AppBriefUiState.Error(
                    e.localizedMessage ?: "Failed to run Gemini analysis."
                )
            }
        }
    }

    private companion object {
        const val TAG = "AiScoutViewModel"
    }
}
