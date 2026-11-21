package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.AdStatus
import com.example.data.model.CatalogApp
import com.example.data.model.GroundingSource
import com.example.data.model.LibraryStatus
import com.example.data.model.StoreAd
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CobaltIndigo
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MintEmerald

fun openExternalUrl(context: Context, rawUrl: String) {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return
    val normalized = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized))
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No browser found to open link", Toast.LENGTH_SHORT).show()
    }
}

private val badgePalettes = listOf(
    listOf(Color(0xFF00E5FF), Color(0xFF2563EB)),
    listOf(Color(0xFF6366F1), Color(0xFF9333EA)),
    listOf(Color(0xFF10B981), Color(0xFF0284C7)),
    listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
    listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)),
    listOf(Color(0xFF14B8A6), Color(0xFF3B82F6))
)

fun categoryIcon(category: String): ImageVector {
    return when {
        category.contains("Developer", ignoreCase = true) -> Icons.Rounded.Code
        category.contains("Security", ignoreCase = true) || category.contains("Privacy", ignoreCase = true) -> Icons.Rounded.Security
        category.contains("Creative", ignoreCase = true) -> Icons.Rounded.Palette
        category.contains("Audio", ignoreCase = true) || category.contains("Media", ignoreCase = true) -> Icons.Rounded.MusicNote
        category.contains("Game", ignoreCase = true) -> Icons.Rounded.Gamepad
        else -> Icons.Rounded.AutoAwesome
    }
}

@Composable
fun AppIconBadge(
    appName: String,
    category: String,
    accentIndex: Int,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val palette = badgePalettes[kotlin.math.abs(accentIndex) % badgePalettes.size]
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Brush.linearGradient(palette)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = "$appName category icon",
            tint = Color.White,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

@Composable
fun VerifiedCreatorBadge(
    badgeLabel: String = "Verified",
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("verified_creator_badge"),
        color = MintEmerald.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MintEmerald.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 2.dp else 4.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Verified,
                contentDescription = badgeLabel,
                tint = MintEmerald,
                modifier = Modifier.size(if (compact) 12.dp else 14.dp)
            )
            Text(
                text = if (compact) "Verified" else badgeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MintEmerald,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AdStatusPill(status: AdStatus, modifier: Modifier = Modifier) {
    val (label, bg, fg, icon) = when (status) {
        AdStatus.PENDING -> Quad("Pending", AmberGold.copy(alpha = 0.18f), AmberGold, Icons.Rounded.HourglassTop)
        AdStatus.APPROVED -> Quad("Approved", MintEmerald.copy(alpha = 0.18f), MintEmerald, Icons.Rounded.CheckCircle)
        AdStatus.REJECTED -> Quad("Rejected", Color(0xFFEF4444).copy(alpha = 0.18f), Color(0xFFFCA5A5), Icons.Rounded.Cancel)
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SponsoredAdBannerCard(
    ad: StoreAd,
    isVerifiedCreator: Boolean,
    badgeLabel: String = "Verified Creator",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sponsored_ad_card_${ad.id}"),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.45f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            AmberGold.copy(alpha = 0.08f),
                            CobaltIndigo.copy(alpha = 0.10f),
                            ElectricCyan.copy(alpha = 0.06f)
                        )
                    )
                )
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = AmberGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Campaign,
                                    contentDescription = "Sponsored",
                                    tint = AmberGold,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "SPONSORED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = ad.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                    }

                    if (isVerifiedCreator) {
                        VerifiedCreatorBadge(badgeLabel = badgeLabel, compact = true)
                    }
                }

                Text(
                    text = ad.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = ad.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ad.targetAppName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "by ${ad.creatorName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { openExternalUrl(context, ad.targetUrl) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF002029)
                        ),
                        modifier = Modifier.testTag("ad_cta_button_${ad.id}")
                    ) {
                        Text(
                            text = ad.ctaLabel.ifBlank { "Learn More" },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = "Open ${ad.targetAppName}",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(status: LibraryStatus?, modifier: Modifier = Modifier) {
    val (label, containerColor, contentColor, icon) = when (status) {
        LibraryStatus.INSTALLED -> Quad("Installed", MintEmerald.copy(alpha = 0.18f), MintEmerald, Icons.Rounded.CheckCircle)
        LibraryStatus.UPDATE_AVAILABLE -> Quad("Update", AmberGold.copy(alpha = 0.18f), AmberGold, Icons.Rounded.Update)
        LibraryStatus.WISHLIST -> Quad("Saved", CobaltIndigo.copy(alpha = 0.22f), Color(0xFFBAC4FF), Icons.Rounded.Bookmark)
        null -> Quad("Get", ElectricCyan.copy(alpha = 0.15f), ElectricCyan, Icons.Rounded.Download)
    }

    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun AppCatalogRowCard(
    app: CatalogApp,
    libraryStatus: LibraryStatus?,
    isVerifiedCreator: Boolean = false,
    onOpenDetail: () -> Unit,
    onQuickAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val creatorLabel = app.creatorName.ifBlank { app.developer }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onOpenDetail)
            .testTag("app_card_${app.id}"),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppIconBadge(
                appName = app.appName,
                category = app.category,
                accentIndex = app.accentIndex,
                size = 56.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
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
                    } else if (app.isCommunitySubmitted) {
                        Surface(
                            color = CobaltIndigo.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "COMMUNITY",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFBFC8FF),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = if (app.isCommunitySubmitted) "$creatorLabel • ${app.tagline}" else app.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = String.format("%.1f", app.rating),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format("%.1f", app.sizeMb)} MB",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = app.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            FilledTonalButton(
                onClick = onQuickAction,
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("app_action_${app.id}")
            ) {
                Text(
                    text = when (libraryStatus) {
                        LibraryStatus.INSTALLED -> "Open"
                        LibraryStatus.UPDATE_AVAILABLE -> "Update"
                        LibraryStatus.WISHLIST -> "Install"
                        null -> "Get"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroundingSourcesCard(
    sources: List<GroundingSource>,
    searchEntryPointHtml: String?,
    modifier: Modifier = Modifier
) {
    if (sources.isEmpty() && searchEntryPointHtml.isNullOrBlank()) return
    val context = LocalContext.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Public,
                    contentDescription = "Google Search sources",
                    tint = ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Google Search Grounding Sources",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (!searchEntryPointHtml.isNullOrBlank()) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(AndroidColor.TRANSPARENT)
                            settings.javaScriptEnabled = false
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString()
                                    if (!url.isNullOrBlank()) {
                                        openExternalUrl(ctx, url)
                                    }
                                    return true
                                }
                            }
                            loadDataWithBaseURL(
                                null,
                                searchEntryPointHtml,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(
                            null,
                            searchEntryPointHtml,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )
            }

            if (sources.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sources.take(6).forEachIndexed { idx, source ->
                        AssistChip(
                            onClick = { openExternalUrl(context, source.uri) },
                            label = {
                                Text(
                                    text = source.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = "Open ${source.title}",
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("grounding_source_chip_$idx")
                        )
                    }
                }
            }
        }
    }
}
