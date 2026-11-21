package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.catalog.CuratedCatalog
import com.example.data.model.AdStatus
import com.example.data.model.LibraryStatus
import com.example.firebase.signOut
import com.example.ui.components.AdStatusPill
import com.example.ui.components.VerifiedCreatorBadge
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CobaltIndigo
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MintEmerald
import com.example.ui.viewmodel.StoreViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    currentUserId: String,
    displayName: String,
    email: String?,
    photoUrl: String?,
    storeViewModel: StoreViewModel,
    onOpenAppDetail: (String) -> Unit,
    onOpenCreate: () -> Unit,
    onOpenAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val profileState by storeViewModel.userProfileState.collectAsStateWithLifecycle()
    val libraryState by storeViewModel.libraryState.collectAsStateWithLifecycle()
    val communityState by storeViewModel.communityAppsState.collectAsStateWithLifecycle()
    val myAdsState by storeViewModel.myAdsState.collectAsStateWithLifecycle()
    val verifiedCreators by storeViewModel.verifiedCreatorsMap.collectAsStateWithLifecycle()
    val isAdmin by storeViewModel.isAdmin.collectAsStateWithLifecycle()
    val adminAdsState by storeViewModel.adminAdsState.collectAsStateWithLifecycle()

    val myVerifiedBadge = verifiedCreators[currentUserId]
    val libraryEntries = when (val s = libraryState) {
        is UiState.Success -> s.data
        else -> emptyList()
    }
    val installedCount = libraryEntries.count { it.statusEnum() == LibraryStatus.INSTALLED }
    val wishlistCount = libraryEntries.count { it.statusEnum() == LibraryStatus.WISHLIST }
    val mySubmissions = when (val c = communityState) {
        is UiState.Success -> c.data.filter { it.userId == currentUserId }
        else -> emptyList()
    }
    val myAds = when (val a = myAdsState) {
        is UiState.Success -> a.data
        else -> emptyList()
    }
    val pendingAdminAds = when (val s = adminAdsState) {
        is UiState.Success -> s.data.count { it.statusEnum() == AdStatus.PENDING }
        else -> 0
    }
    val preferredCategory = when (val p = profileState) {
        is UiState.Success -> p.data?.preferredCategory ?: "All"
        else -> "All"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Google profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = "Profile avatar",
                                tint = ElectricCyan,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = displayName.ifBlank { "AppNexus Member" },
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (myVerifiedBadge != null) {
                                VerifiedCreatorBadge(
                                    badgeLabel = myVerifiedBadge.badgeLabel,
                                    compact = true
                                )
                            }
                        }
                        if (!email.isNullOrBlank()) {
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "UID: ${currentUserId.take(14)}...",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                            IconButton(
                                onClick = {
                                    val cm = context.getSystemService(ClipboardManager::class.java)
                                    cm?.setPrimaryClip(ClipData.newPlainText("AppNexus UID", currentUserId))
                                    Toast.makeText(context, "User UID copied", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("copy_uid_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy User UID",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isAdmin) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmberGold.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AdminPanelSettings,
                                contentDescription = "Admin Control Panel",
                                tint = AmberGold
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Admin Control Panel",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (pendingAdminAds > 0) {
                                    "$pendingAdminAds pending ad(s) awaiting review & creator verification"
                                } else {
                                    "Accept store ads & verify app creators"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onOpenAdmin,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberGold,
                                contentColor = Color(0xFF221600)
                            ),
                            modifier = Modifier.testTag("profile_admin_panel_button")
                        ) {
                            Text("Open", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, CobaltIndigo.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddBox,
                        contentDescription = null,
                        tint = ElectricCyan
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Creator Studio", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Publish a new app or submit a sponsored ad",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenCreate,
                        modifier = Modifier.testTag("profile_open_create_button")
                    ) {
                        Text("Create")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(label = "Installed", value = installedCount.toString(), accent = MintEmerald, modifier = Modifier.weight(1f))
                StatCard(label = "Wishlist", value = wishlistCount.toString(), accent = ElectricCyan, modifier = Modifier.weight(1f))
                StatCard(label = "Apps / Ads", value = "${mySubmissions.size}/${myAds.size}", accent = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Preferred Discovery Category",
                    style = MaterialTheme.typography.titleMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CuratedCatalog.categories.forEach { cat ->
                        FilterChip(
                            selected = preferredCategory == cat,
                            onClick = { storeViewModel.updatePreferredCategory(cat) },
                            label = { Text(cat) },
                            modifier = Modifier.testTag("pref_cat_${cat.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        if (myAds.isNotEmpty()) {
            item {
                Text(
                    text = "My Sponsored Ads",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(myAds, key = { "prof_ad_${it.id}" }) { ad ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ad.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${ad.targetAppName} • Creator: ${ad.creatorName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AdStatusPill(status = ad.statusEnum())
                    }
                }
            }
        }

        item {
            Text(
                text = "My Published Community Apps",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (mySubmissions.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "You haven't published any community apps yet. Open Creator Studio to publish an app or ad.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(mySubmissions, key = { it.id }) { app ->
                val resolvedCreator = app.creatorName.ifBlank { app.authorName }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.appName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Creator: $resolvedCreator • v${app.version}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = { onOpenAppDetail(app.id) }) {
                            Text("View")
                        }
                        IconButton(
                            onClick = { storeViewModel.deleteCommunityApp(app.id, app.appName) },
                            modifier = Modifier.testTag("delete_community_${app.id}")
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

        item {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = { signOut(credentialManager = credentialManager, scope = scope) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = "Sign out"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
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
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
