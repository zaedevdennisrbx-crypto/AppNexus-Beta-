package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CatalogApp
import com.example.data.model.LibraryStatus
import com.example.ui.components.AppCatalogRowCard
import com.example.ui.components.GroundingSourcesCard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MintEmerald
import com.example.ui.viewmodel.AiScoutViewModel
import com.example.ui.viewmodel.ScoutUiState
import com.example.ui.viewmodel.StoreViewModel
import com.example.ui.viewmodel.UiState

private val quickScoutPrompts = listOf(
    "Best open-source Android note apps 2026",
    "Offline music players with FLAC support",
    "Privacy-focused Android browsers comparison",
    "Top indie roguelike games on Android"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiScoutScreen(
    aiScoutViewModel: AiScoutViewModel,
    storeViewModel: StoreViewModel,
    onOpenAppDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scoutState by aiScoutViewModel.scoutState.collectAsStateWithLifecycle()
    val historyState by aiScoutViewModel.historyState.collectAsStateWithLifecycle()
    val libraryState by storeViewModel.libraryState.collectAsStateWithLifecycle()

    var queryInput by rememberSaveable { mutableStateOf("") }

    val libraryMap = when (val lib = libraryState) {
        is UiState.Success -> lib.data.associateBy { it.appId }
        else -> emptyMap()
    }

    val runSearch: (String) -> Unit = { prompt ->
        queryInput = prompt
        aiScoutViewModel.runScoutSearch(prompt) { discovered ->
            storeViewModel.registerScoutApps(discovered)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_ai_scout_banner),
                    contentDescription = "Nexus AI Scout banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x55070B14), Color(0xE6070B14))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MintEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "GEMINI + GOOGLE SEARCH GROUNDED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MintEmerald
                        )
                    }
                    Text(
                        text = "Nexus AI Scout",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = "Ask for app recommendations, live version checks, or privacy comparisons.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD6E0F5)
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = queryInput,
                    onValueChange = { queryInput = it },
                    placeholder = { Text("Ask Scout to find Android apps...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Scout query"
                        )
                    },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_scout_query_input")
                )

                Button(
                    onClick = { runSearch(queryInput) },
                    enabled = queryInput.isNotBlank() && scoutState !is ScoutUiState.Loading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF002029)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("ai_scout_submit_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scout with Google Search",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickScoutPrompts.forEachIndexed { idx, prompt ->
                        AssistChip(
                            onClick = { runSearch(prompt) },
                            label = {
                                Text(
                                    text = prompt,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier.testTag("scout_prompt_chip_$idx")
                        )
                    }
                }
            }
        }

        when (val state = scoutState) {
            ScoutUiState.Idle -> {
                // Idle prompt suggestions shown above
            }
            ScoutUiState.Loading -> {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = ElectricCyan,
                                strokeWidth = 2.5.dp
                            )
                            Text(
                                text = "Searching live web sources & analyzing apps...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            is ScoutUiState.Streaming -> {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = ElectricCyan
                            )
                            Text(
                                text = state.text.ifBlank { "Synthesizing results..." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                if (state.sources.isNotEmpty() || !state.searchEntryPointHtml.isNullOrBlank()) {
                    item {
                        GroundingSourcesCard(
                            sources = state.sources,
                            searchEntryPointHtml = state.searchEntryPointHtml
                        )
                    }
                }
            }
            is ScoutUiState.Success -> {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scout_result_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = state.query,
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricCyan
                            )
                            Text(
                                text = state.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (state.sources.isNotEmpty() || !state.searchEntryPointHtml.isNullOrBlank()) {
                    item {
                        GroundingSourcesCard(
                            sources = state.sources,
                            searchEntryPointHtml = state.searchEntryPointHtml
                        )
                    }
                }

                if (state.isExtractingApps) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MintEmerald
                            )
                            Text(
                                text = "Building interactive app cards...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (state.extractedApps.isNotEmpty()) {
                    item {
                        Text(
                            text = "Discovered Apps (${state.extractedApps.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    items(state.extractedApps, key = { "scout_${it.id}" }) { app: CatalogApp ->
                        val status = libraryMap[app.id]?.statusEnum()
                        AppCatalogRowCard(
                            app = app,
                            libraryStatus = status,
                            onOpenDetail = { onOpenAppDetail(app.id) },
                            onQuickAction = {
                                storeViewModel.saveToLibrary(app, LibraryStatus.WISHLIST)
                            }
                        )
                    }
                }
            }
            is ScoutUiState.Error -> {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }

        if (historyState is UiState.Success && (historyState as UiState.Success).data.isNotEmpty()) {
            val historyList = (historyState as UiState.Success).data
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Recent Cloud Scout History",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            items(historyList.take(5), key = { it.id }) { entry ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            queryInput = entry.query
                            aiScoutViewModel.loadHistoryEntry(entry)
                        }
                        .testTag("scout_history_${entry.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = entry.query,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${entry.sourcesCount} web sources • Tap to reopen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { aiScoutViewModel.deleteHistoryEntry(entry.id) },
                            modifier = Modifier.testTag("delete_history_${entry.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete scout history entry",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
