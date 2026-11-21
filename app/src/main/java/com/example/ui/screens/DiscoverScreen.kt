package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.data.catalog.CuratedCatalog
import com.example.data.model.CatalogApp
import com.example.data.model.LibraryEntry
import com.example.data.model.LibraryStatus
import com.example.ui.components.AppCatalogRowCard
import com.example.ui.components.AppIconBadge
import com.example.ui.components.SponsoredAdBannerCard
import com.example.ui.components.StatusPill
import com.example.ui.components.VerifiedCreatorBadge
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CobaltIndigo
import com.example.ui.theme.ElectricCyan
import com.example.ui.viewmodel.StoreViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun DiscoverScreen(
    storeViewModel: StoreViewModel,
    onOpenAppDetail: (String) -> Unit,
    onOpenAiScout: () -> Unit,
    onOpenCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by storeViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by storeViewModel.selectedCategory.collectAsStateWithLifecycle()
    val catalogApps by storeViewModel.filteredCatalogApps.collectAsStateWithLifecycle()
    val allApps by storeViewModel.allCatalogApps.collectAsStateWithLifecycle()
    val libraryState by storeViewModel.libraryState.collectAsStateWithLifecycle()
    val verifiedCreators by storeViewModel.verifiedCreatorsMap.collectAsStateWithLifecycle()
    val approvedAdsState by storeViewModel.approvedAdsState.collectAsStateWithLifecycle()

    val libraryMap = remember(libraryState) {
        when (val s = libraryState) {
            is UiState.Success -> s.data.associateBy { it.appId }
            else -> emptyMap<String, LibraryEntry>()
        }
    }

    val visibleApprovedAds = remember(approvedAdsState, selectedCategory, searchQuery) {
        val allAds = when (val s = approvedAdsState) {
            is UiState.Success -> s.data
            else -> emptyList()
        }
        val q = searchQuery.trim().lowercase()
        allAds.filter { ad ->
            val catMatch = selectedCategory == "All" || ad.category.equals(selectedCategory, ignoreCase = true)
            val queryMatch = q.isEmpty() ||
                ad.title.lowercase().contains(q) ||
                ad.targetAppName.lowercase().contains(q) ||
                ad.creatorName.lowercase().contains(q) ||
                ad.tagline.lowercase().contains(q)
            catMatch && queryMatch
        }
    }

    val featuredApps = remember(allApps) {
        allApps.filter { it.isEditorsChoice || it.isCommunitySubmitted }.take(6)
    }

    val spotlightApp = remember(allApps) {
        allApps.firstOrNull { it.isEditorsChoice } ?: allApps.firstOrNull()
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { storeViewModel.setSearchQuery(it) },
                    placeholder = {
                        Text("Search apps, creators, or categories")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search catalog"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { storeViewModel.setSearchQuery("") },
                                modifier = Modifier.testTag("clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("discover_search_input")
                )
            }

            if (spotlightApp != null && searchQuery.isBlank() && selectedCategory == "All") {
                item {
                    SpotlightHeroCard(
                        app = spotlightApp,
                        onInspect = { onOpenAppDetail(spotlightApp.id) },
                        onOpenScout = onOpenAiScout
                    )
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CuratedCatalog.categories) { category ->
                        val selected = selectedCategory == category
                        FilterChip(
                            selected = selected,
                            onClick = { storeViewModel.selectCategory(category) },
                            label = {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = ElectricCyan,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                selectedBorderColor = ElectricCyan,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.testTag("category_chip_${category.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }

            if (visibleApprovedAds.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Sponsored",
                            style = MaterialTheme.typography.titleMedium,
                            color = AmberGold
                        )
                        visibleApprovedAds.take(3).forEach { ad ->
                            val badge = verifiedCreators[ad.userId]
                            SponsoredAdBannerCard(
                                ad = ad,
                                isVerifiedCreator = badge != null,
                                badgeLabel = badge?.badgeLabel ?: "Verified Creator"
                            )
                        }
                    }
                }
            }

            if (featuredApps.isNotEmpty() && searchQuery.isBlank() && selectedCategory == "All") {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Editor's & Community Picks",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(featuredApps, key = { "feat_${it.id}" }) { app ->
                                val isVerified = app.submitterUid != null && verifiedCreators.containsKey(app.submitterUid)
                                FeaturedHorizontalCard(
                                    app = app,
                                    status = libraryMap[app.id]?.statusEnum(),
                                    isVerifiedCreator = isVerified,
                                    onClick = { onOpenAppDetail(app.id) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedCategory == "All") "App Catalog" else selectedCategory,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${catalogApps.size} apps",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (catalogApps.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "No matching apps in local catalog",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Ask Nexus AI Scout to search Google for any Android app you need.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onOpenAiScout,
                                modifier = Modifier.testTag("empty_open_scout_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Search with AI Scout")
                            }
                        }
                    }
                }
            } else {
                items(catalogApps, key = { it.id }) { app ->
                    val entry = libraryMap[app.id]
                    val currentStatus = entry?.statusEnum()
                    val isVerified = app.submitterUid != null && verifiedCreators.containsKey(app.submitterUid)
                    AppCatalogRowCard(
                        app = app,
                        libraryStatus = currentStatus,
                        isVerifiedCreator = isVerified,
                        onOpenDetail = { onOpenAppDetail(app.id) },
                        onQuickAction = {
                            when (currentStatus) {
                                null, LibraryStatus.WISHLIST -> {
                                    storeViewModel.saveToLibrary(app, LibraryStatus.INSTALLED)
                                }
                                LibraryStatus.UPDATE_AVAILABLE -> {
                                    storeViewModel.saveToLibrary(app, LibraryStatus.INSTALLED)
                                }
                                LibraryStatus.INSTALLED -> {
                                    onOpenAppDetail(app.id)
                                }
                            }
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onOpenCreate,
            containerColor = CobaltIndigo,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("publish_app_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Create app or ad"
                )
                Text(
                    text = "Create App / Ad",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun SpotlightHeroCard(
    app: CatalogApp,
    onInspect: () -> Unit,
    onOpenScout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(216.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onInspect)
            .testTag("spotlight_hero_card")
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_hero_spotlight),
            contentDescription = "AppNexus Editor's Choice Spotlight",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x55070B14),
                            Color(0xE8070B14)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ElectricCyan.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "EDITOR'S SPOTLIGHT",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    color = Color(0x99131C31),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onOpenScout)
                        .testTag("hero_scout_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Ask Gemini Scout",
                            tint = ElectricCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "AI Scout",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppIconBadge(
                    appName = app.appName,
                    category = app.category,
                    accentIndex = app.accentIndex,
                    size = 58.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD6E0F5),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = onInspect,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF002029)
                    )
                ) {
                    Text("View", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FeaturedHorizontalCard(
    app: CatalogApp,
    status: LibraryStatus?,
    isVerifiedCreator: Boolean,
    onClick: () -> Unit
) {
    val creatorLabel = app.creatorName.ifBlank { app.developer }
    Surface(
        modifier = Modifier
            .width(226.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("featured_card_${app.id}"),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                AppIconBadge(
                    appName = app.appName,
                    category = app.category,
                    accentIndex = app.accentIndex,
                    size = 48.dp
                )
                StatusPill(status = status)
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isVerifiedCreator) {
                        VerifiedCreatorBadge(compact = true)
                    }
                }
                Text(
                    text = if (app.isCommunitySubmitted) "by $creatorLabel" else app.developer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = AmberGold,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${String.format("%.1f", app.rating)} • ${app.downloadsLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
