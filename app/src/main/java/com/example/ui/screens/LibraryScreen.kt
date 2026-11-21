package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.LibraryEntry
import com.example.data.model.LibraryStatus
import com.example.ui.components.AppIconBadge
import com.example.ui.components.StatusPill
import com.example.ui.components.openExternalUrl
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.StoreViewModel
import com.example.ui.viewmodel.UiState

private enum class LibraryFilter(val label: String) {
    ALL("All"),
    INSTALLED("Installed"),
    WISHLIST("Wishlist"),
    UPDATES("Updates")
}

@Composable
fun LibraryScreen(
    storeViewModel: StoreViewModel,
    onOpenAppDetail: (String) -> Unit,
    onNavigateDiscover: () -> Unit,
    modifier: Modifier = Modifier
) {
    val libraryState by storeViewModel.libraryState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    var editingNoteFor by remember { mutableStateOf<LibraryEntry?>(null) }
    var noteDraft by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LibraryFilter.entries) { filter ->
                val selected = selectedFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                        selectedLabelColor = ElectricCyan,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.testTag("library_filter_${filter.name.lowercase()}")
                )
            }
        }

        when (val state = libraryState) {
            UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricCyan)
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val filtered = remember(state.data, selectedFilter) {
                    when (selectedFilter) {
                        LibraryFilter.ALL -> state.data
                        LibraryFilter.INSTALLED -> state.data.filter { it.statusEnum() == LibraryStatus.INSTALLED }
                        LibraryFilter.WISHLIST -> state.data.filter { it.statusEnum() == LibraryStatus.WISHLIST }
                        LibraryFilter.UPDATES -> state.data.filter { it.statusEnum() == LibraryStatus.UPDATE_AVAILABLE }
                    }
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_empty_library),
                                contentDescription = "Empty AppNexus vault illustration",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 240.dp, height = 180.dp)
                                    .clip(MaterialTheme.shapes.extraLarge)
                            )
                            Text(
                                text = "Your AppNexus Vault is Empty",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Save apps from Discover or AI Scout to track installs, updates, and personal notes across devices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onNavigateDiscover,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = Color(0xFF002029)
                                ),
                                modifier = Modifier.testTag("empty_library_discover_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Explore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Browse Discover", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { entry ->
                            val status = entry.statusEnum()
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = MaterialTheme.shapes.large,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.large)
                                    .clickable { onOpenAppDetail(entry.appId) }
                                    .testTag("library_card_${entry.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        AppIconBadge(
                                            appName = entry.appName,
                                            category = entry.category,
                                            accentIndex = entry.appName.hashCode(),
                                            size = 52.dp
                                        )
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = entry.appName,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${entry.developer} • v${entry.version}",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Star,
                                                    contentDescription = null,
                                                    tint = AmberGold,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "${String.format("%.1f", entry.rating)} • ${String.format("%.1f", entry.sizeMb)} MB",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        StatusPill(status = status)
                                    }

                                    if (!entry.personalNote.isNullOrBlank()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                text = entry.personalNote,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val nextStatus = when (status) {
                                                    LibraryStatus.WISHLIST -> LibraryStatus.INSTALLED
                                                    LibraryStatus.INSTALLED -> LibraryStatus.UPDATE_AVAILABLE
                                                    LibraryStatus.UPDATE_AVAILABLE -> LibraryStatus.INSTALLED
                                                }
                                                storeViewModel.updateLibraryStatus(entry.id, nextStatus)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("cycle_status_${entry.id}")
                                        ) {
                                            Text(
                                                text = when (status) {
                                                    LibraryStatus.WISHLIST -> "Mark Installed"
                                                    LibraryStatus.INSTALLED -> "Flag Update"
                                                    LibraryStatus.UPDATE_AVAILABLE -> "Apply Update"
                                                },
                                                maxLines = 1
                                            )
                                        }

                                        if (!entry.sourceUrl.isNullOrBlank()) {
                                            IconButton(
                                                onClick = { openExternalUrl(context, entry.sourceUrl) },
                                                modifier = Modifier.testTag("open_source_${entry.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                                    contentDescription = "Open official site for ${entry.appName}"
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                editingNoteFor = entry
                                                noteDraft = entry.personalNote.orEmpty()
                                            },
                                            modifier = Modifier.testTag("edit_note_${entry.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.EditNote,
                                                contentDescription = "Edit personal note"
                                            )
                                        }

                                        IconButton(
                                            onClick = { storeViewModel.removeFromLibrary(entry.id, entry.appName) },
                                            modifier = Modifier.testTag("remove_library_${entry.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Remove ${entry.appName} from library",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val targetEntry = editingNoteFor
    if (targetEntry != null) {
        AlertDialog(
            onDismissRequest = { editingNoteFor = null },
            title = { Text("Note for ${targetEntry.appName}") },
            text = {
                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { if (it.length <= 300) noteDraft = it },
                    label = { Text("Personal note (max 300 chars)") },
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_note_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        storeViewModel.updateLibraryStatus(
                            entryId = targetEntry.id,
                            status = targetEntry.statusEnum(),
                            note = noteDraft
                        )
                        editingNoteFor = null
                    },
                    modifier = Modifier.testTag("save_note_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNoteFor = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
