package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AdStatus
import com.example.data.model.StoreAd
import com.example.ui.components.AdStatusPill
import com.example.ui.components.VerifiedCreatorBadge
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MintEmerald
import com.example.ui.viewmodel.StoreViewModel
import com.example.ui.viewmodel.UiState

private data class CreatorCandidate(
    val uid: String,
    val creatorName: String,
    val appCount: Int,
    val adCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    storeViewModel: StoreViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAdmin by storeViewModel.isAdmin.collectAsStateWithLifecycle()
    val adminAdsState by storeViewModel.adminAdsState.collectAsStateWithLifecycle()
    val communityState by storeViewModel.communityAppsState.collectAsStateWithLifecycle()
    val verifiedCreatorsMap by storeViewModel.verifiedCreatorsMap.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Ad Moderation, 1 = Verify Creators
    var adFilter by remember { mutableStateOf<AdStatus?>(AdStatus.PENDING) }
    var rejectingAd by remember { mutableStateOf<StoreAd?>(null) }
    var rejectionReasonInput by remember { mutableStateOf("") }

    var manualCreatorUid by remember { mutableStateOf("") }
    var manualCreatorName by remember { mutableStateOf("") }
    var manualBadgeLabel by remember { mutableStateOf("Verified Creator") }
    var manualVerifyError by remember { mutableStateOf<String?>(null) }

    val allAds = when (val s = adminAdsState) {
        is UiState.Success -> s.data
        else -> emptyList()
    }
    val pendingCount = allAds.count { it.statusEnum() == AdStatus.PENDING }
    val approvedCount = allAds.count { it.statusEnum() == AdStatus.APPROVED }

    val filteredAds = remember(allAds, adFilter) {
        if (adFilter == null) allAds else allAds.filter { it.statusEnum() == adFilter }
    }

    val creatorCandidates = remember(communityState, allAds, verifiedCreatorsMap) {
        val apps = when (val c = communityState) {
            is UiState.Success -> c.data
            else -> emptyList()
        }
        val map = linkedMapOf<String, CreatorCandidate>()
        apps.forEach { app ->
            val name = app.creatorName.ifBlank { app.authorName }.ifBlank { "Creator" }
            val existing = map[app.userId]
            map[app.userId] = CreatorCandidate(
                uid = app.userId,
                creatorName = existing?.creatorName ?: name,
                appCount = (existing?.appCount ?: 0) + 1,
                adCount = existing?.adCount ?: 0
            )
        }
        allAds.forEach { ad ->
            val name = ad.creatorName.ifBlank { "Creator" }
            val existing = map[ad.userId]
            map[ad.userId] = CreatorCandidate(
                uid = ad.userId,
                creatorName = existing?.creatorName ?: name,
                appCount = existing?.appCount ?: 0,
                adCount = (existing?.adCount ?: 0) + 1
            )
        }
        verifiedCreatorsMap.values.forEach { vc ->
            val existing = map[vc.userId]
            if (existing == null) {
                map[vc.userId] = CreatorCandidate(
                    uid = vc.userId,
                    creatorName = vc.creatorName,
                    appCount = 0,
                    adCount = 0
                )
            }
        }
        map.values.toList()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AdminPanelSettings,
                            contentDescription = null,
                            tint = ElectricCyan
                        )
                        Text("Admin Control Panel")
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_back_button")
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
        if (!isAdmin) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Admin Privileges Required",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Only verified AppNexus administrators can moderate ads or verify app creators.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AdminMetricCard("Pending Ads", pendingCount.toString(), AmberGold, Modifier.weight(1f))
                        AdminMetricCard("Live Ads", approvedCount.toString(), MintEmerald, Modifier.weight(1f))
                        AdminMetricCard("Verified", verifiedCreatorsMap.size.toString(), ElectricCyan, Modifier.weight(1f))
                    }
                }

                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Campaign,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("admin_tab_ads")
                        ) {
                            Text("Accept Ads ($pendingCount)")
                        }
                        SegmentedButton(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("admin_tab_creators")
                        ) {
                            Text("Verify Creators")
                        }
                    }
                }

                if (selectedTab == 0) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = adFilter == AdStatus.PENDING,
                                    onClick = { adFilter = AdStatus.PENDING },
                                    label = { Text("Pending ($pendingCount)") },
                                    modifier = Modifier.testTag("admin_filter_pending")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = adFilter == AdStatus.APPROVED,
                                    onClick = { adFilter = AdStatus.APPROVED },
                                    label = { Text("Approved ($approvedCount)") },
                                    modifier = Modifier.testTag("admin_filter_approved")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = adFilter == AdStatus.REJECTED,
                                    onClick = { adFilter = AdStatus.REJECTED },
                                    label = { Text("Rejected") },
                                    modifier = Modifier.testTag("admin_filter_rejected")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = adFilter == null,
                                    onClick = { adFilter = null },
                                    label = { Text("All (${allAds.size})") },
                                    modifier = Modifier.testTag("admin_filter_all")
                                )
                            }
                        }
                    }

                    when (val state = adminAdsState) {
                        UiState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = ElectricCyan)
                                }
                            }
                        }
                        is UiState.Error -> {
                            item {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        is UiState.Success -> {
                            if (filteredAds.isEmpty()) {
                                item {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = MaterialTheme.shapes.medium,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "No ads match the selected moderation filter.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            } else {
                                items(filteredAds, key = { "admin_ad_${it.id}" }) { ad ->
                                    val creatorBadge = verifiedCreatorsMap[ad.userId]
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = MaterialTheme.shapes.medium,
                                        border = BorderStroke(
                                            1.dp,
                                            if (ad.statusEnum() == AdStatus.PENDING) {
                                                AmberGold.copy(alpha = 0.5f)
                                            } else {
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                            }
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = ad.title,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "${ad.targetAppName} • by ${ad.creatorName}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = ElectricCyan
                                                        )
                                                        if (creatorBadge != null) {
                                                            VerifiedCreatorBadge(compact = true)
                                                        }
                                                    }
                                                }
                                                AdStatusPill(status = ad.statusEnum())
                                            }

                                            Text(
                                                text = ad.tagline,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Text(
                                                text = "${ad.category} • CTA: \"${ad.ctaLabel}\" • ${ad.targetUrl}",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (ad.statusEnum() != AdStatus.APPROVED) {
                                                    Button(
                                                        onClick = { storeViewModel.moderateAd(ad.id, approve = true) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MintEmerald,
                                                            contentColor = Color(0xFF002116)
                                                        ),
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .testTag("admin_accept_ad_${ad.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.CheckCircle,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Accept Ad", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                if (ad.statusEnum() != AdStatus.REJECTED) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            rejectionReasonInput = ""
                                                            rejectingAd = ad
                                                        },
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .testTag("admin_reject_ad_${ad.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Cancel,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Reject")
                                                    }
                                                }
                                                IconButton(
                                                    onClick = { storeViewModel.deleteAd(ad.id, ad.title) },
                                                    modifier = Modifier.testTag("admin_delete_ad_${ad.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.DeleteOutline,
                                                        contentDescription = "Delete ad ${ad.title}",
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
                } else {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = MaterialTheme.shapes.large,
                            border = BorderStroke(1.dp, MintEmerald.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Verify Creator by UID",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                OutlinedTextField(
                                    value = manualCreatorUid,
                                    onValueChange = { if (it.length <= 128) manualCreatorUid = it },
                                    label = { Text("Creator User UID") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_manual_uid_input")
                                )
                                OutlinedTextField(
                                    value = manualCreatorName,
                                    onValueChange = { if (it.length <= 60) manualCreatorName = it },
                                    label = { Text("Creator Display Name") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_manual_name_input")
                                )
                                OutlinedTextField(
                                    value = manualBadgeLabel,
                                    onValueChange = { if (it.length <= 40) manualBadgeLabel = it },
                                    label = { Text("Badge Label") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_manual_badge_input")
                                )
                                if (manualVerifyError != null) {
                                    Text(
                                        text = manualVerifyError ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Button(
                                    onClick = {
                                        val cleanUid = manualCreatorUid.trim()
                                        val cleanName = manualCreatorName.trim()
                                        val cleanBadge = manualBadgeLabel.trim().ifBlank { "Verified Creator" }
                                        when {
                                            cleanUid.isEmpty() || !cleanUid.matches(Regex("^[a-zA-Z0-9_\\-]+$")) ->
                                                manualVerifyError = "Enter a valid alphanumeric/underscore/hyphen UID."
                                            cleanName.isEmpty() ->
                                                manualVerifyError = "Creator display name is required."
                                            else -> {
                                                manualVerifyError = null
                                                storeViewModel.verifyCreator(
                                                    creatorUid = cleanUid,
                                                    creatorName = cleanName,
                                                    badgeLabel = cleanBadge,
                                                    onSuccess = {
                                                        manualCreatorUid = ""
                                                        manualCreatorName = ""
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MintEmerald,
                                        contentColor = Color(0xFF002116)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_manual_verify_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Verified,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Grant Verification Badge", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "App & Ad Creators (${creatorCandidates.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (creatorCandidates.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No community creators found yet. Use the form above or publish an app/ad first.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    } else {
                        items(creatorCandidates, key = { "cand_${it.uid}" }) { candidate ->
                            val existingBadge = verifiedCreatorsMap[candidate.uid]
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(
                                    1.dp,
                                    if (existingBadge != null) {
                                        MintEmerald.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = candidate.creatorName,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            if (existingBadge != null) {
                                                VerifiedCreatorBadge(
                                                    badgeLabel = existingBadge.badgeLabel,
                                                    compact = true
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${candidate.appCount} apps • ${candidate.adCount} ads",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "UID: ${candidate.uid}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ElectricCyan,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (existingBadge == null) {
                                        Button(
                                            onClick = {
                                                storeViewModel.verifyCreator(
                                                    creatorUid = candidate.uid,
                                                    creatorName = candidate.creatorName
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MintEmerald,
                                                contentColor = Color(0xFF002116)
                                            ),
                                            modifier = Modifier.testTag("admin_verify_${candidate.uid}")
                                        ) {
                                            Text("Verify", fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                storeViewModel.revokeCreatorVerification(
                                                    creatorUid = candidate.uid,
                                                    creatorName = candidate.creatorName
                                                )
                                            },
                                            modifier = Modifier.testTag("admin_revoke_${candidate.uid}")
                                        ) {
                                            Text("Revoke")
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

    val pendingRejectAd = rejectingAd
    if (pendingRejectAd != null) {
        AlertDialog(
            onDismissRequest = { rejectingAd = null },
            title = { Text("Reject Ad") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Provide optional feedback for \"${pendingRejectAd.title}\":",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = rejectionReasonInput,
                        onValueChange = { if (it.length <= 200) rejectionReasonInput = it },
                        label = { Text("Rejection Reason") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        storeViewModel.moderateAd(
                            adId = pendingRejectAd.id,
                            approve = false,
                            rejectionReason = rejectionReasonInput,
                            onSuccess = { rejectingAd = null }
                        )
                    }
                ) {
                    Text("Reject Ad")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingAd = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AdminMetricCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
