package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.catalog.CuratedCatalog
import com.example.data.model.AppReview
import com.example.data.model.CatalogApp
import com.example.data.model.CommunityApp
import com.example.data.model.LibraryEntry
import com.example.data.model.LibraryStatus
import com.example.data.model.StoreAd
import com.example.data.model.UserProfile
import com.example.data.model.VerifiedCreator
import com.example.data.repository.StoreRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class StoreViewModel(
    private val repository: StoreRepository,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String?,
    private val userEmailVerified: Boolean,
    private val userPhotoUrl: String?
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedAppIdForReviews = MutableStateFlow<String?>(null)

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _transientScoutApps = MutableStateFlow<List<CatalogApp>>(emptyList())

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing user profile", root)
                emit(UiState.Error(root.message ?: "Failed to load profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val libraryState: StateFlow<UiState<List<LibraryEntry>>> =
        repository.observeLibrary(currentUserId)
            .map<List<LibraryEntry>, UiState<List<LibraryEntry>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing library", root)
                emit(UiState.Error(root.message ?: "Failed to load library"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val communityAppsState: StateFlow<UiState<List<CommunityApp>>> =
        repository.observeCommunityApps()
            .map<List<CommunityApp>, UiState<List<CommunityApp>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing community apps", root)
                emit(UiState.Error(root.message ?: "Failed to load community catalog"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val verifiedCreatorsMap: StateFlow<Map<String, VerifiedCreator>> =
        repository.observeVerifiedCreators()
            .map { list -> list.associateBy { it.userId } }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing verified creators", firestoreError ?: error)
                emit(emptyMap())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyMap()
            )

    val approvedAdsState: StateFlow<UiState<List<StoreAd>>> =
        repository.observeApprovedAds()
            .map<List<StoreAd>, UiState<List<StoreAd>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing approved ads", root)
                emit(UiState.Error(root.message ?: "Failed to load sponsored ads"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val myAdsState: StateFlow<UiState<List<StoreAd>>> =
        repository.observeMyAds(currentUserId)
            .map<List<StoreAd>, UiState<List<StoreAd>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing user ads", root)
                emit(UiState.Error(root.message ?: "Failed to load your ads"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminAdsState: StateFlow<UiState<List<StoreAd>>> =
        isAdmin
            .flatMapLatest { admin ->
                if (!admin) {
                    flowOf(UiState.Success(emptyList()))
                } else {
                    repository.observeAllAdsForAdmin()
                        .map<List<StoreAd>, UiState<List<StoreAd>>> { UiState.Success(it) }
                        .catch { error ->
                            val firestoreError = unwrapFirestoreError(error)
                            if (firestoreError == null && error is CancellationException) throw error
                            val root = firestoreError ?: error
                            Log.w(TAG, "Error observing admin ads queue", root)
                            emit(UiState.Error(root.message ?: "Failed to load ad moderation queue"))
                        }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val allCatalogApps: StateFlow<List<CatalogApp>> = combine(
        communityAppsState,
        _transientScoutApps
    ) { commState, scoutApps ->
        val commCatalog = when (commState) {
            is UiState.Success -> commState.data.mapIndexed { idx, c ->
                val resolvedCreator = c.creatorName.ifBlank { c.authorName }
                CatalogApp(
                    id = c.id,
                    appName = c.appName,
                    packageName = c.packageName,
                    developer = c.developer,
                    creatorName = resolvedCreator,
                    category = c.category,
                    version = c.version,
                    sizeMb = c.sizeMb,
                    rating = 4.7,
                    downloadsLabel = "Community",
                    tagline = c.tagline,
                    description = c.description,
                    highlights = listOf("Community submission", "Creator: $resolvedCreator", c.category),
                    permissionsSummary = "Community verified listing",
                    websiteUrl = c.websiteUrl ?: "https://play.google.com/store",
                    isEditorsChoice = false,
                    isCommunitySubmitted = true,
                    submitterUid = c.userId,
                    accentIndex = idx % 6
                )
            }
            else -> emptyList()
        }
        (commCatalog + CuratedCatalog.apps + scoutApps).distinctBy { it.id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CuratedCatalog.apps
    )

    val filteredCatalogApps: StateFlow<List<CatalogApp>> = combine(
        allCatalogApps,
        _searchQuery,
        _selectedCategory
    ) { apps, query, category ->
        val q = query.trim().lowercase()
        apps.filter { app ->
            val catMatch = category == "All" || app.category.equals(category, ignoreCase = true)
            val queryMatch = q.isEmpty() ||
                app.appName.lowercase().contains(q) ||
                app.developer.lowercase().contains(q) ||
                app.creatorName.lowercase().contains(q) ||
                app.tagline.lowercase().contains(q) ||
                app.category.lowercase().contains(q)
            catMatch && queryMatch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CuratedCatalog.apps
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedAppReviewsState: StateFlow<UiState<List<AppReview>>> =
        _selectedAppIdForReviews
            .flatMapLatest { appId ->
                if (appId.isNullOrBlank()) {
                    flowOf(UiState.Success(emptyList()))
                } else {
                    repository.observeReviewsForApp(appId)
                        .map<List<AppReview>, UiState<List<AppReview>>> { UiState.Success(it) }
                        .catch { error ->
                            val firestoreError = unwrapFirestoreError(error)
                            if (firestoreError == null && error is CancellationException) throw error
                            val root = firestoreError ?: error
                            Log.w(TAG, "Error observing reviews", root)
                            emit(UiState.Error(root.message ?: "Failed to load reviews"))
                        }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                uid = currentUserId,
                displayName = userDisplayName,
                email = userEmail,
                photoUrl = userPhotoUrl
            )
            _isAdmin.value = repository.checkIsAdmin(
                uid = currentUserId,
                email = userEmail,
                isEmailVerified = userEmailVerified
            )
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updatePreferredCategory(category: String) {
        _selectedCategory.value = category
        viewModelScope.launch {
            repository.updatePreferredCategory(currentUserId, category)
                .onSuccess { _actionMessage.value = "Preferred category updated to $category" }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to update preference" }
        }
    }

    fun registerScoutApps(apps: List<CatalogApp>) {
        if (apps.isEmpty()) return
        _transientScoutApps.value = (_transientScoutApps.value + apps).distinctBy { it.id }
    }

    fun findAppById(appId: String): CatalogApp? {
        return allCatalogApps.value.firstOrNull { it.id == appId }
    }

    fun observeReviews(appId: String) {
        _selectedAppIdForReviews.value = appId
    }

    fun saveToLibrary(app: CatalogApp, status: LibraryStatus, note: String? = null) {
        viewModelScope.launch {
            val result = repository.upsertLibraryEntry(currentUserId, app, status, note)
            result.onSuccess {
                _actionMessage.value = "${app.appName} saved as ${status.label}"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not save ${app.appName}"
            }
        }
    }

    fun updateLibraryStatus(entryId: String, status: LibraryStatus, note: String? = null) {
        viewModelScope.launch {
            repository.updateLibraryStatus(currentUserId, entryId, status, note)
                .onSuccess { _actionMessage.value = "Updated status to ${status.label}" }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to update library item"
                }
        }
    }

    fun removeFromLibrary(entryId: String, appName: String) {
        viewModelScope.launch {
            repository.removeLibraryEntry(currentUserId, entryId)
                .onSuccess { _actionMessage.value = "Removed $appName from Library" }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to remove item"
                }
        }
    }

    fun publishCommunityApp(
        creatorName: String,
        appName: String,
        packageName: String,
        developer: String,
        category: String,
        version: String,
        sizeMb: Double,
        tagline: String,
        description: String,
        websiteUrl: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.publishCommunityApp(
                uid = currentUserId,
                creatorName = creatorName,
                authorName = userDisplayName,
                appName = appName,
                packageName = packageName,
                developer = developer,
                category = category,
                version = version,
                sizeMb = sizeMb,
                tagline = tagline,
                description = description,
                websiteUrl = websiteUrl
            )
            result.onSuccess {
                _actionMessage.value = "Published $appName by $creatorName to AppNexus"
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to publish app"
            }
        }
    }

    fun deleteCommunityApp(appId: String, appName: String) {
        viewModelScope.launch {
            repository.deleteCommunityApp(currentUserId, appId)
                .onSuccess { _actionMessage.value = "Deleted $appName listing" }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to delete listing"
                }
        }
    }

    fun submitAd(
        creatorName: String,
        title: String,
        tagline: String,
        targetAppName: String,
        targetUrl: String,
        category: String,
        ctaLabel: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.submitAd(
                uid = currentUserId,
                creatorName = creatorName,
                title = title,
                tagline = tagline,
                targetAppName = targetAppName,
                targetUrl = targetUrl,
                category = category,
                ctaLabel = ctaLabel
            )
            result.onSuccess {
                _actionMessage.value = "Ad \"$title\" submitted for Admin approval"
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to submit ad"
            }
        }
    }

    fun moderateAd(
        adId: String,
        approve: Boolean,
        rejectionReason: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.moderateAd(
                adminUid = currentUserId,
                adId = adId,
                approve = approve,
                rejectionReason = rejectionReason
            ).onSuccess {
                _actionMessage.value = if (approve) "Ad approved and live on Discover" else "Ad rejected"
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to moderate ad"
            }
        }
    }

    fun deleteAd(adId: String, adTitle: String) {
        viewModelScope.launch {
            repository.deleteAd(currentUserId, adId)
                .onSuccess { _actionMessage.value = "Removed ad \"$adTitle\"" }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to delete ad"
                }
        }
    }

    fun verifyCreator(
        creatorUid: String,
        creatorName: String,
        badgeLabel: String = "Verified Creator",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.verifyCreator(
                adminUid = currentUserId,
                creatorUid = creatorUid,
                creatorName = creatorName,
                badgeLabel = badgeLabel
            ).onSuccess {
                _actionMessage.value = "Verified $creatorName"
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to verify creator"
            }
        }
    }

    fun revokeCreatorVerification(creatorUid: String, creatorName: String) {
        viewModelScope.launch {
            repository.revokeCreatorVerification(currentUserId, creatorUid)
                .onSuccess { _actionMessage.value = "Revoked verification for $creatorName" }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to revoke badge"
                }
        }
    }

    fun postReview(appId: String, appName: String, rating: Int, comment: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.postReview(
                uid = currentUserId,
                authorName = userDisplayName,
                appId = appId,
                appName = appName,
                rating = rating,
                comment = comment
            )
            result.onSuccess {
                _actionMessage.value = "Review posted for $appName"
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to post review"
            }
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            repository.deleteReview(currentUserId, reviewId)
                .onSuccess { _actionMessage.value = "Review deleted" }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to delete review"
                }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }

    private companion object {
        const val TAG = "StoreViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
