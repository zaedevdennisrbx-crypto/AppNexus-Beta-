package com.example

import com.example.data.model.AdStatus
import com.example.data.model.CommunityApp
import com.example.data.model.ScoutDiscoveryApp
import com.example.data.model.StoreAd
import com.example.data.model.VerifiedCreator
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun adStatusParsing_fallsBackToPending() {
        assertEquals(AdStatus.APPROVED, AdStatus.fromRaw("APPROVED"))
        assertEquals(AdStatus.REJECTED, AdStatus.fromRaw("REJECTED"))
        assertEquals(AdStatus.PENDING, AdStatus.fromRaw("PENDING"))
        assertEquals(AdStatus.PENDING, AdStatus.fromRaw("UNKNOWN"))
        assertEquals(AdStatus.PENDING, AdStatus.fromRaw(null))
    }

    @Test
    fun storeModels_preserveCreatorNameAndVerificationDefaults() {
        val communityApp = CommunityApp(
            id = "app_1",
            userId = "uid_1",
            creatorName = "Studio Alpha",
            authorName = "Alice",
            appName = "Alpha Notes"
        )
        assertEquals("Studio Alpha", communityApp.creatorName)

        val ad = StoreAd(
            id = "ad_1",
            userId = "uid_1",
            creatorName = "Studio Alpha",
            title = "Try Alpha Notes",
            status = AdStatus.APPROVED.name
        )
        assertEquals(AdStatus.APPROVED, ad.statusEnum())

        val verified = VerifiedCreator(
            id = "uid_1",
            userId = "uid_1",
            creatorName = "Studio Alpha"
        )
        assertEquals("Verified Creator", verified.badgeLabel)

        val scoutCatalog = ScoutDiscoveryApp(
            appName = "K9 Mail",
            developer = "K9 Team",
            category = "Productivity",
            version = "6.8",
            sizeMb = 12.5,
            tagline = "Open source email",
            websiteUrl = "https://k9mail.app"
        ).toCatalogApp(0)
        assertEquals("K9 Team", scoutCatalog.creatorName)
    }
}
