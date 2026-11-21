package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.catalog.CuratedCatalog
import com.example.data.model.AdStatus
import com.example.data.model.StoreAd
import com.example.ui.components.AdStatusPill
import com.example.ui.components.SponsoredAdBannerCard
import com.example.ui.components.VerifiedCreatorBadge
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CobaltIndigo
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.StoreViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateScreen(
    currentUserId: String,
    defaultCreatorName: String,
    storeViewModel: StoreViewModel,
    onOpenAppDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val publishableCategories = remember { CuratedCatalog.categories.filter { it != "All" } }
    val verifiedCreators by storeViewModel.verifiedCreatorsMap.collectAsStateWithLifecycle()
    val communityState by storeViewModel.communityAppsState.collectAsStateWithLifecycle()
    val myAdsState by storeViewModel.myAdsState.collectAsStateWithLifecycle()

    val currentCreatorBadge = verifiedCreators[currentUserId]
    val myApps = when (val s = communityState) {
        is UiState.Success -> s.data.filter { it.userId == currentUserId }
        else -> emptyList()
    }
    val myAds = when (val s = myAdsState) {
        is UiState.Success -> s.data
        else -> emptyList()
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Create App, 1 = Create Ad

    // Create App state
    var appCreatorName by remember(defaultCreatorName) { mutableStateOf(defaultCreatorName.take(60)) }
    var appName by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var developer by remember { mutableStateOf("") }
    var appCategory by remember { mutableStateOf(publishableCategories.first()) }
    var version by remember { mutableStateOf("1.0.0") }
    var sizeInput by remember { mutableStateOf("24.0") }
    var appTagline by remember { mutableStateOf("") }
    var appDescription by remember { mutableStateOf("") }
    var appWebsiteUrl by remember { mutableStateOf("") }
    var appValidationError by remember { mutableStateOf<String?>(null) }

    // Create Ad state
    var adCreatorName by remember(defaultCreatorName) { mutableStateOf(defaultCreatorName.take(60)) }
    var adTitle by remember { mutableStateOf("") }
    var adTargetAppName by remember { mutableStateOf("") }
    var adTagline by remember { mutableStateOf("") }
    var adCategory by remember { mutableStateOf(publishableCategories.first()) }
    var adTargetUrl by remember { mutableStateOf("https://") }
    var adCtaLabel by remember { mutableStateOf("Try Now") }
    var adValidationError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Creator Studio",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Publish a new Android app listing or submit a sponsored ad for Admin approval.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (currentCreatorBadge != null) {
                        VerifiedCreatorBadge(badgeLabel = currentCreatorBadge.badgeLabel)
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("create_mode_app")
                    ) {
                        Text("Create App")
                    }
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Campaign,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("create_mode_ad")
                    ) {
                        Text("Create Ad")
                    }
                }
            }
        }

        if (selectedTab == 0) {
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
                        Text(
                            text = "New Community App Listing",
                            style = MaterialTheme.typography.titleMedium
                        )

                        OutlinedTextField(
                            value = appCreatorName,
                            onValueChange = { if (it.length <= 60) appCreatorName = it },
                            label = { Text("Creator Name (1-60 chars)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_app_creator_name_input")
                        )

                        OutlinedTextField(
                            value = appName,
                            onValueChange = { if (it.length <= 60) appName = it },
                            label = { Text("App Name (2-60 chars)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_app_name_input")
                        )

                        OutlinedTextField(
                            value = packageName,
                            onValueChange = { if (it.length <= 100) packageName = it },
                            label = { Text("Package Name (e.g. dev.studio.app)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_app_package_input")
                        )

                        OutlinedTextField(
                            value = developer,
                            onValueChange = { if (it.length <= 60) developer = it },
                            label = { Text("Developer / Studio (2-60 chars)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_app_developer_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = version,
                                onValueChange = { if (it.length <= 30) version = it },
                                label = { Text("Version") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("create_app_version_input")
                            )
                            OutlinedTextField(
                                value = sizeInput,
                                onValueChange = { sizeInput = it },
                                label = { Text("Size (MB)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("create_app_size_input")
                            )
                        }

                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelMedium
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            publishableCategories.forEach { cat ->
                                FilterChip(
                                    selected = appCategory == cat,
                                    onClick = { appCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = appTagline,
                            onValueChange = { if (it.length <= 120) appTagline = it },
                            label = { Text("One-line Tagline (3-120 chars)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_app_tagline_input")
                        )

                        OutlinedTextField(
                            value = appDescription,
                            onValueChange = { if (it.length <= 1000) appDescription = it },
                            label = { Text("Description (10-1000 chars)") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_app_description_input")
                        )

                        OutlinedTextField(
                            value = appWebsiteUrl,
                            onValueChange = { if (it.length <= 500) appWebsiteUrl = it },
                            label = { Text("Official URL (optional)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_app_url_input")
                        )

                        if (appValidationError != null) {
                            Text(
                                text = appValidationError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = {
                                val parsedSize = sizeInput.toDoubleOrNull()
                                when {
                                    appCreatorName.trim().isEmpty() ->
                                        appValidationError = "Creator name is required."
                                    appName.trim().length < 2 ->
                                        appValidationError = "App name must be at least 2 characters."
                                    packageName.trim().length < 3 ->
                                        appValidationError = "Package name must be at least 3 characters."
                                    developer.trim().length < 2 ->
                                        appValidationError = "Developer studio must be at least 2 characters."
                                    version.trim().isEmpty() ->
                                        appValidationError = "Version is required."
                                    parsedSize == null || parsedSize < 0.1 || parsedSize > 50000.0 ->
                                        appValidationError = "Enter a valid size between 0.1 and 50,000 MB."
                                    appTagline.trim().length < 3 ->
                                        appValidationError = "Tagline must be at least 3 characters."
                                    appDescription.trim().length < 10 ->
                                        appValidationError = "Description must be at least 10 characters."
                                    else -> {
                                        appValidationError = null
                                        storeViewModel.publishCommunityApp(
                                            creatorName = appCreatorName.trim(),
                                            appName = appName.trim(),
                                            packageName = packageName.trim(),
                                            developer = developer.trim(),
                                            category = appCategory,
                                            version = version.trim(),
                                            sizeMb = parsedSize,
                                            tagline = appTagline.trim(),
                                            description = appDescription.trim(),
                                            websiteUrl = appWebsiteUrl.trim().takeIf { it.isNotEmpty() },
                                            onSuccess = {
                                                appName = ""
                                                packageName = ""
                                                developer = ""
                                                appTagline = ""
                                                appDescription = ""
                                                appWebsiteUrl = ""
                                            }
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = Color(0xFF002029)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("create_app_submit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Publish,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Publish App to Store", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Submit Sponsored Store Ad",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Ads remain Pending until an Admin accepts them in the Admin Panel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = adCreatorName,
                            onValueChange = { if (it.length <= 60) adCreatorName = it },
                            label = { Text("Creator / Brand Name (1-60 chars)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_ad_creator_name_input")
                        )

                        OutlinedTextField(
                            value = adTargetAppName,
                            onValueChange = { if (it.length <= 60) adTargetAppName = it },
                            label = { Text("Promoted App Name (2-60 chars)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_ad_app_name_input")
                        )

                        OutlinedTextField(
                            value = adTitle,
                            onValueChange = { if (it.length <= 80) adTitle = it },
                            label = { Text("Ad Headline (3-80 chars)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_ad_title_input")
                        )

                        OutlinedTextField(
                            value = adTagline,
                            onValueChange = { if (it.length <= 140) adTagline = it },
                            label = { Text("Ad Message / Tagline (5-140 chars)") },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_ad_tagline_input")
                        )

                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelMedium
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            publishableCategories.forEach { cat ->
                                FilterChip(
                                    selected = adCategory == cat,
                                    onClick = { adCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = adTargetUrl,
                                onValueChange = { if (it.length <= 500) adTargetUrl = it },
                                label = { Text("Destination URL") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.6f)
                                    .testTag("create_ad_url_input")
                            )
                            OutlinedTextField(
                                value = adCtaLabel,
                                onValueChange = { if (it.length <= 24) adCtaLabel = it },
                                label = { Text("Button CTA") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("create_ad_cta_input")
                            )
                        }

                        Text(
                            text = "Live Ad Preview",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SponsoredAdBannerCard(
                            ad = StoreAd(
                                id = "preview",
                                userId = currentUserId,
                                creatorName = adCreatorName.ifBlank { "Creator" },
                                title = adTitle.ifBlank { "Your Sponsored Headline" },
                                tagline = adTagline.ifBlank { "Describe why Android users will love your app." },
                                targetAppName = adTargetAppName.ifBlank { "Your App" },
                                targetUrl = adTargetUrl.ifBlank { "https://example.com" },
                                category = adCategory,
                                ctaLabel = adCtaLabel.ifBlank { "Try Now" },
                                status = AdStatus.PENDING.name
                            ),
                            isVerifiedCreator = currentCreatorBadge != null,
                            badgeLabel = currentCreatorBadge?.badgeLabel ?: "Verified Creator"
                        )

                        if (adValidationError != null) {
                            Text(
                                text = adValidationError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = {
                                when {
                                    adCreatorName.trim().isEmpty() ->
                                        adValidationError = "Creator name is required."
                                    adTargetAppName.trim().length < 2 ->
                                        adValidationError = "Promoted app name must be at least 2 characters."
                                    adTitle.trim().length < 3 ->
                                        adValidationError = "Ad headline must be at least 3 characters."
                                    adTagline.trim().length < 5 ->
                                        adValidationError = "Ad message must be at least 5 characters."
                                    adTargetUrl.trim().length < 5 ->
                                        adValidationError = "Enter a valid destination URL (min 5 characters)."
                                    adCtaLabel.trim().length < 2 ->
                                        adValidationError = "CTA label must be at least 2 characters."
                                    else -> {
                                        adValidationError = null
                                        storeViewModel.submitAd(
                                            creatorName = adCreatorName.trim(),
                                            title = adTitle.trim(),
                                            tagline = adTagline.trim(),
                                            targetAppName = adTargetAppName.trim(),
                                            targetUrl = adTargetUrl.trim(),
                                            category = adCategory,
                                            ctaLabel = adCtaLabel.trim(),
                                            onSuccess = {
                                                adTitle = ""
                                                adTargetAppName = ""
                                                adTagline = ""
                                                adTargetUrl = "https://"
                                            }
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CobaltIndigo,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("create_ad_submit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Campaign,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submit Ad for Admin Review", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "My Ad Campaigns (${myAds.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (myAds.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No ad submissions yet. Create an ad above to promote your app in Discover.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(myAds, key = { "my_ad_${it.id}" }) { ad ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ad.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${ad.targetAppName} • Creator: ${ad.creatorName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AdStatusPill(status = ad.statusEnum())
                            IconButton(
                                onClick = { storeViewModel.deleteAd(ad.id, ad.title) },
                                modifier = Modifier.testTag("delete_my_ad_${ad.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete ad ${ad.title}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        if (!ad.rejectionReason.isNullOrBlank() && ad.statusEnum() == AdStatus.REJECTED) {
                            Text(
                                text = "Admin note: ${ad.rejectionReason}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "My Published Apps (${myApps.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (myApps.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No published community apps yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(myApps, key = { "my_app_${it.id}" }) { app ->
                val resolvedCreator = app.creatorName.ifBlank { app.authorName }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(app.appName, style = MaterialTheme.typography.titleSmall)
                                if (currentCreatorBadge != null) {
                                    VerifiedCreatorBadge(compact = true)
                                }
                            }
                            Text(
                                text = "Creator: $resolvedCreator • ${app.packageName}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(onClick = { onOpenAppDetail(app.id) }) {
                            Text("View")
                        }
                        IconButton(
                            onClick = { storeViewModel.deleteCommunityApp(app.id, app.appName) },
                            modifier = Modifier.testTag("create_delete_app_${app.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete ${app.appName}",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
