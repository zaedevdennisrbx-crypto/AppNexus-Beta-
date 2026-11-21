package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LibraryStatus
import com.example.ui.components.AppIconBadge
import com.example.ui.components.GroundingSourcesCard
import com.example.ui.components.StatusPill
import com.example.ui.components.VerifiedCreatorBadge
import com.example.ui.components.openExternalUrl
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MintEmerald
import com.example.ui.viewmodel.AiScoutViewModel
import com.example.ui.viewmodel.AppBriefUiState
import com.example.ui.viewmodel.StoreViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppDetailScreen(
    appId: String,
    currentUserId: String,
    storeViewModel: StoreViewModel,
    aiScoutViewModel: AiScoutViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allApps by storeViewModel.allCatalogApps.collectAsStateWithLifecycle()
    val libraryState by storeViewModel.libraryState.collectAsStateWithLifecycle()
    val reviewsState by storeViewModel.selectedAppReviewsState.collectAsStateWithLifecycle()
    val briefState by aiScoutViewModel.appBriefState.collectAsStateWithLifecycle()
    val verifiedCreators by storeViewModel.verifiedCreatorsMap.collectAsStateWithLifecycle()
    val isAdmin by storeViewModel.isAdmin.collectAsStateWithLifecycle()

    val app = remember(allApps, appId) { allApps.firstOrNull { it.id == appId } }
    val creatorBadge = app?.submitterUid?.let { verifiedCreators[it] }

    LaunchedEffect(appId) {
        storeViewModel.observeReviews(appId)
        aiScoutViewModel.resetAppBriefIfNeeded(appId)
    }

    val libraryEntry = remember(libraryState, appId) {
        when (val s = libraryState) {
            is UiState.Success -> s.data.firstOrNull { it.appId == appId }
            else -> null
        }
    }

    var reviewRating by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(app?.appName ?: "App Details") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (app == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("App not found in catalog.", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            val resolvedCreator = app.creatorName.ifBlank { app.developer }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AppIconBadge(
                            appName = app.appName,
                            category = app.category,
                            accentIndex = app.accentIndex,
                            size = 76.dp
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = if (app.isCommunitySubmitted) "Creator: $resolvedCreator • ${app.developer}" else app.developer,
                                style = MaterialTheme.typography.titleSmall,
                                color = ElectricCyan
                            )
                            if (creatorBadge != null) {
                                VerifiedCreatorBadge(badgeLabel = creatorBadge.badgeLabel)
                            }
                            Text(
                                text = app.packageName,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(status = libraryEntry?.statusEnum())
                    }
                }

                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MetricTile(label = "RATING", value = "${String.format("%.1f", app.rating)} ★")
                            MetricTile(label = "VERSION", value = app.version)
                            MetricTile(label = "SIZE", value = "${String.format("%.1f", app.sizeMb)} MB")
                            MetricTile(label = "REACH", value = app.downloadsLabel)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                storeViewModel.saveToLibrary(app, LibraryStatus.INSTALLED)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = Color(0xFF002029)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("detail_install_button")
                        ) {
                            Icon(
                                imageVector = if (libraryEntry?.statusEnum() == LibraryStatus.INSTALLED) {
                                    Icons.Rounded.CheckCircle
                                } else {
                                    Icons.Rounded.Download
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (libraryEntry?.statusEnum() == LibraryStatus.INSTALLED) {
                                    "Saved as Installed"
                                } else {
                                    "Add as Installed"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { storeViewModel.saveToLibrary(app, LibraryStatus.WISHLIST) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("detail_wishlist_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BookmarkAdd,
                                contentDescription = "Save to wishlist"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wishlist")
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { openExternalUrl(context, app.websiteUrl) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("detail_open_website_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Visit Official Site / Store Page")
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "About this app",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = app.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            app.highlights.forEach { tag ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(tag) }
                                )
                            }
                        }
                        Surface(
                            color = MintEmerald.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MintEmerald.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = MintEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = app.permissionsSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = ElectricCyan
                                    )
                                    Text(
                                        text = "Gemini Live Security & Alt Brief",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }

                            when (val brief = briefState) {
                                AppBriefUiState.Idle -> {
                                    Text(
                                        text = "Run a live Google Search-grounded Gemini check for latest version notes, privacy posture, and top alternatives.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = { aiScoutViewModel.analyzeApp(app) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("run_ai_brief_button")
                                    ) {
                                        Text("Run AI Brief")
                                    }
                                }
                                AppBriefUiState.Loading -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = ElectricCyan,
                                            strokeWidth = 2.dp
                                        )
                                        Text("Checking live web intelligence...")
                                    }
                                }
                                is AppBriefUiState.Streaming -> {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = ElectricCyan
                                    )
                                    Text(
                                        text = brief.text.ifBlank { "Analyzing..." },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                is AppBriefUiState.Success -> {
                                    Text(
                                        text = brief.text,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    GroundingSourcesCard(
                                        sources = brief.sources,
                                        searchEntryPointHtml = brief.searchEntryPointHtml
                                    )
                                }
                                is AppBriefUiState.Error -> {
                                    Text(
                                        text = brief.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    OutlinedButton(onClick = { aiScoutViewModel.analyzeApp(app) }) {
                                        Text("Retry AI Brief")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Community Reviews",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Leave a verified review",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    (1..5).forEach { star ->
                                        IconButton(
                                            onClick = { reviewRating = star },
                                            modifier = Modifier.testTag("review_star_$star")
                                        ) {
                                            Icon(
                                                imageVector = if (star <= reviewRating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                                contentDescription = "Rate $star stars",
                                                tint = AmberGold
                                            )
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = reviewComment,
                                    onValueChange = { if (it.length <= 500) reviewComment = it },
                                    label = { Text("Share your experience (3-500 chars)") },
                                    maxLines = 3,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("review_comment_input")
                                )
                                Button(
                                    onClick = {
                                        storeViewModel.postReview(
                                            appId = app.id,
                                            appName = app.appName,
                                            rating = reviewRating,
                                            comment = reviewComment,
                                            onSuccess = { reviewComment = "" }
                                        )
                                    },
                                    enabled = reviewComment.trim().length >= 3,
                                    modifier = Modifier.testTag("post_review_button")
                                ) {
                                    Text("Post Review")
                                }
                            }
                        }
                    }
                }

                when (val rState = reviewsState) {
                    UiState.Loading -> {
                        item {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        }
                    }
                    is UiState.Error -> {
                        item {
                            Text(
                                text = rState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    is UiState.Success -> {
                        if (rState.data.isEmpty()) {
                            item {
                                Text(
                                    text = "No community reviews yet. Be the first to review ${app.appName}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(rState.data, key = { it.id }) { review ->
                                val reviewerBadge = verifiedCreators[review.userId]
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = review.authorName,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                if (reviewerBadge != null) {
                                                    VerifiedCreatorBadge(
                                                        badgeLabel = reviewerBadge.badgeLabel,
                                                        compact = true
                                                    )
                                                }
                                                Text(
                                                    text = "★ ${review.rating}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AmberGold
                                                )
                                            }
                                            Text(
                                                text = review.comment,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (review.userId == currentUserId || isAdmin) {
                                            IconButton(
                                                onClick = { storeViewModel.deleteReview(review.id) },
                                                modifier = Modifier.testTag("delete_review_${review.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                    contentDescription = "Delete review",
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
    }
}

@Composable
private fun MetricTile(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
