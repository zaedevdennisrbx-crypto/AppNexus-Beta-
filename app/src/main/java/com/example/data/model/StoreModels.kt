package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class LibraryStatus(val label: String) {
    WISHLIST("Wishlist"),
    INSTALLED("Installed"),
    UPDATE_AVAILABLE("Update Ready");

    companion object {
        fun fromRaw(raw: String?): LibraryStatus {
            return entries.firstOrNull { it.name == raw } ?: WISHLIST
        }
    }
}

enum class AdStatus(val label: String) {
    PENDING("Pending Review"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    companion object {
        fun fromRaw(raw: String?): AdStatus {
            return entries.firstOrNull { it.name == raw } ?: PENDING
        }
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val photoUrl: String? = null,
    val preferredCategory: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LibraryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val appId: String = "",
    val appName: String = "",
    val developer: String = "",
    val category: String = "",
    val version: String = "",
    val sizeMb: Double = 0.0,
    val rating: Double = 0.0,
    val status: String = LibraryStatus.WISHLIST.name,
    val personalNote: String? = null,
    val sourceUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): LibraryStatus = LibraryStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class ScoutHistoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val query: String = "",
    val summary: String = "",
    val sourcesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommunityApp(
    @DocumentId val id: String = "",
    val userId: String = "",
    val creatorName: String = "",
    val authorName: String = "",
    val appName: String = "",
    val packageName: String = "",
    val developer: String = "",
    val category: String = "",
    val version: String = "",
    val sizeMb: Double = 0.0,
    val tagline: String = "",
    val description: String = "",
    val websiteUrl: String? = null,
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppReview(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val appId: String = "",
    val appName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VerifiedCreator(
    @DocumentId val id: String = "",
    val userId: String = "",
    val creatorName: String = "",
    val badgeLabel: String = "Verified Creator",
    val verifiedByUid: String = "",
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StoreAd(
    @DocumentId val id: String = "",
    val userId: String = "",
    val creatorName: String = "",
    val title: String = "",
    val tagline: String = "",
    val targetAppName: String = "",
    val targetUrl: String = "",
    val category: String = "",
    val ctaLabel: String = "Learn More",
    val status: String = AdStatus.PENDING.name,
    val rejectionReason: String? = null,
    val reviewedBy: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): AdStatus = AdStatus.fromRaw(status)
}

data class CatalogApp(
    val id: String,
    val appName: String,
    val packageName: String,
    val developer: String,
    val creatorName: String = "",
    val category: String,
    val version: String,
    val sizeMb: Double,
    val rating: Double,
    val downloadsLabel: String,
    val tagline: String,
    val description: String,
    val highlights: List<String>,
    val permissionsSummary: String,
    val websiteUrl: String,
    val isEditorsChoice: Boolean = false,
    val isCommunitySubmitted: Boolean = false,
    val submitterUid: String? = null,
    val accentIndex: Int = 0
)

data class GroundingSource(
    val title: String,
    val uri: String
)

data class ScoutDiscoveryApp(
    val appName: String,
    val developer: String,
    val category: String,
    val version: String,
    val sizeMb: Double,
    val tagline: String,
    val websiteUrl: String
) {
    fun toCatalogApp(index: Int): CatalogApp {
        val safeId = "scout_" + appName.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(40)
            .ifBlank { "app_$index" }
        return CatalogApp(
            id = safeId,
            appName = appName.take(80),
            packageName = "scout.${safeId.take(30)}",
            developer = developer.take(80),
            creatorName = developer.take(60),
            category = category.take(40),
            version = version.take(30),
            sizeMb = sizeMb.coerceIn(1.0, 5000.0),
            rating = 4.6,
            downloadsLabel = "Web Scout",
            tagline = tagline.take(120),
            description = "$tagline\n\nDiscovered via AppNexus AI Scout with live Google Search grounding.",
            highlights = listOf("Google Search verified", "AI Scout recommendation", category),
            permissionsSummary = "Standard Android runtime permissions",
            websiteUrl = websiteUrl,
            accentIndex = index
        )
    }
}
