package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AdStatus
import com.example.data.model.AppReview
import com.example.data.model.CatalogApp
import com.example.data.model.CommunityApp
import com.example.data.model.LibraryEntry
import com.example.data.model.LibraryStatus
import com.example.data.model.ScoutHistoryEntry
import com.example.data.model.StoreAd
import com.example.data.model.UserProfile
import com.example.data.model.VerifiedCreator
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeDocId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").trim('_')
        return cleaned.take(128).ifBlank { "item_${System.currentTimeMillis()}" }
    }

    suspend fun checkIsAdmin(
        uid: String,
        email: String?,
        isEmailVerified: Boolean
    ): Boolean {
        if (isEmailVerified && email == BOOTSTRAPPED_ADMIN_EMAIL) {
            return true
        }
        return try {
            val safeUid = uid.trim()
            if (safeUid.isEmpty() || !safeUid.matches(VALID_ID_REGEX)) {
                false
            } else {
                db.collection("admins").document(safeUid).get().await().exists()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    suspend fun ensureUserProfile(
        uid: String,
        displayName: String,
        email: String?,
        photoUrl: String?
    ): Result<Unit> {
        val path = "users/$uid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Signed-in user mismatch." }
            val safeName = displayName.trim().ifBlank { "AppNexus Explorer" }.take(80)
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to email?.take(120),
                    "photoUrl" to photoUrl?.take(500),
                    "preferredCategory" to "All",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val updatePayload = mapOf(
                    "displayName" to safeName,
                    "email" to email?.take(120),
                    "photoUrl" to photoUrl?.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> {
        require(uid.isNotBlank()) { "UID must not be blank" }
        return db.collection("users").document(uid)
            .snapshots()
            .map { snap ->
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
    }

    suspend fun updatePreferredCategory(uid: String, category: String): Result<Unit> {
        val path = "users/$uid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Signed-in user mismatch." }
            val safeCategory = category.trim().ifBlank { "All" }.take(40)
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "preferredCategory" to safeCategory,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeLibrary(uid: String): Flow<List<LibraryEntry>> {
        require(uid.isNotBlank()) { "UID must not be blank" }
        return db.collection("users").document(uid).collection("library")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    LibraryEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
    }

    suspend fun upsertLibraryEntry(
        uid: String,
        app: CatalogApp,
        status: LibraryStatus,
        personalNote: String? = null
    ): Result<Unit> {
        val entryId = sanitizeDocId(app.id)
        val path = "users/$uid/library/$entryId"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Signed-in user mismatch." }
            val docRef = db.collection("users").document(uid)
                .collection("library").document(entryId)
            val existing = docRef.get().await()
            val cleanNote = personalNote?.trim()?.takeIf { it.isNotEmpty() }?.take(300)
            val cleanSourceUrl = app.websiteUrl.trim().takeIf { it.isNotEmpty() }?.take(500)

            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "appId" to app.id.take(128).ifBlank { entryId },
                    "appName" to app.appName.trim().ifBlank { "App" }.take(80),
                    "developer" to app.developer.trim().ifBlank { "Developer" }.take(80),
                    "category" to app.category.trim().ifBlank { "Utilities" }.take(40),
                    "version" to app.version.trim().ifBlank { "1.0.0" }.take(30),
                    "sizeMb" to app.sizeMb.coerceIn(0.0, 50000.0),
                    "rating" to app.rating.coerceIn(0.0, 5.0),
                    "status" to status.name,
                    "personalNote" to cleanNote,
                    "sourceUrl" to cleanSourceUrl,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val updatePayload = mapOf(
                    "appName" to app.appName.trim().ifBlank { "App" }.take(80),
                    "developer" to app.developer.trim().ifBlank { "Developer" }.take(80),
                    "category" to app.category.trim().ifBlank { "Utilities" }.take(40),
                    "version" to app.version.trim().ifBlank { "1.0.0" }.take(30),
                    "sizeMb" to app.sizeMb.coerceIn(0.0, 50000.0),
                    "rating" to app.rating.coerceIn(0.0, 5.0),
                    "status" to status.name,
                    "personalNote" to cleanNote,
                    "sourceUrl" to cleanSourceUrl,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateLibraryStatus(
        uid: String,
        entryId: String,
        status: LibraryStatus,
        personalNote: String? = null
    ): Result<Unit> {
        val safeEntryId = sanitizeDocId(entryId)
        val path = "users/$uid/library/$safeEntryId"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Signed-in user mismatch." }
            val docRef = db.collection("users").document(uid)
                .collection("library").document(safeEntryId)
            val updateMap = mutableMapOf<String, Any>(
                "status" to status.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (personalNote != null) {
                updateMap["personalNote"] = personalNote.trim().take(300)
            }
            docRef.update(updateMap).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeLibraryEntry(uid: String, entryId: String): Result<Unit> {
        val safeEntryId = sanitizeDocId(entryId)
        val path = "users/$uid/library/$safeEntryId"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Signed-in user mismatch." }
            db.collection("users").document(uid)
                .collection("library").document(safeEntryId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeCommunityApps(): Flow<List<CommunityApp>> {
        return db.collection("community_apps")
            .whereEqualTo("visibility", "PUBLIC")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CommunityApp::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
    }

    suspend fun publishCommunityApp(
        uid: String,
        creatorName: String,
        authorName: String,
        appName: String,
        packageName: String,
        developer: String,
        category: String,
        version: String,
        sizeMb: Double,
        tagline: String,
        description: String,
        websiteUrl: String?
    ): Result<String> {
        var path = "community_apps"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Signed-in user mismatch." }
            val trimmedCreator = creatorName.trim()
            require(trimmedCreator.length in 1..60) { "Creator name must be 1 to 60 characters." }
            require(appName.trim().length in 2..60) { "App name must be 2 to 60 characters." }
            require(packageName.trim().length in 3..100) { "Package name must be 3 to 100 characters." }
            require(developer.trim().length in 2..60) { "Developer must be 2 to 60 characters." }
            require(tagline.trim().length in 3..120) { "Tagline must be 3 to 120 characters." }
            require(description.trim().length in 10..1000) { "Description must be 10 to 1000 characters." }
            require(sizeMb in 0.1..50000.0) { "Size must be between 0.1 MB and 50,000 MB." }

            val docRef = db.collection("community_apps").document()
            path = docRef.path
            val cleanWebsite = websiteUrl?.trim()?.takeIf { it.isNotEmpty() }?.take(500)
            val payload = mapOf(
                "userId" to uid,
                "creatorName" to trimmedCreator.take(60),
                "authorName" to authorName.trim().ifBlank { trimmedCreator }.take(60),
                "appName" to appName.trim().take(60),
                "packageName" to packageName.trim().take(100),
                "developer" to developer.trim().take(60),
                "category" to category.trim().ifBlank { "Developer Tools" }.take(40),
                "version" to version.trim().ifBlank { "1.0.0" }.take(30),
                "sizeMb" to sizeMb,
                "tagline" to tagline.trim().take(120),
                "description" to description.trim().take(1000),
                "websiteUrl" to cleanWebsite,
                "visibility" to "PUBLIC",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCommunityApp(uid: String, appId: String): Result<Unit> {
        val safeId = sanitizeDocId(appId)
        val path = "community_apps/$safeId"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Signed-in user mismatch." }
            db.collection("community_apps").document(safeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeVerifiedCreators(): Flow<List<VerifiedCreator>> {
        return db.collection("verified_creators")
            .whereEqualTo("visibility", "PUBLIC")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    VerifiedCreator::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
    }

    suspend fun verifyCreator(
        adminUid: String,
        creatorUid: String,
        creatorName: String,
        badgeLabel: String = "Verified Creator"
    ): Result<Unit> {
        val cleanCreatorUid = creatorUid.trim()
        val path = "verified_creators/$cleanCreatorUid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == adminUid) { "Admin session mismatch." }
            require(cleanCreatorUid.length in 1..128 && cleanCreatorUid.matches(VALID_ID_REGEX)) {
                "Creator UID must contain only letters, numbers, underscores, or hyphens."
            }
            val cleanName = creatorName.trim()
            require(cleanName.length in 1..60) { "Creator name must be 1 to 60 characters." }
            val cleanBadge = badgeLabel.trim().ifBlank { "Verified Creator" }.take(40)
            require(cleanBadge.length >= 2) { "Badge label must be at least 2 characters." }

            val docRef = db.collection("verified_creators").document(cleanCreatorUid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to cleanCreatorUid,
                    "creatorName" to cleanName,
                    "badgeLabel" to cleanBadge,
                    "verifiedByUid" to adminUid,
                    "visibility" to "PUBLIC",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updatePayload = mapOf(
                    "creatorName" to cleanName,
                    "badgeLabel" to cleanBadge,
                    "verifiedByUid" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun revokeCreatorVerification(adminUid: String, creatorUid: String): Result<Unit> {
        val cleanCreatorUid = creatorUid.trim()
        val path = "verified_creators/$cleanCreatorUid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == adminUid) { "Admin session mismatch." }
            require(cleanCreatorUid.length in 1..128 && cleanCreatorUid.matches(VALID_ID_REGEX)) {
                "Invalid creator UID."
            }
            db.collection("verified_creators").document(cleanCreatorUid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeApprovedAds(): Flow<List<StoreAd>> {
        return db.collection("ads")
            .whereEqualTo("status", AdStatus.APPROVED.name)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    StoreAd::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
    }

    fun observeMyAds(uid: String): Flow<List<StoreAd>> {
        require(uid.isNotBlank()) { "UID must not be blank" }
        return db.collection("ads")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    StoreAd::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
    }

    fun observeAllAdsForAdmin(): Flow<List<StoreAd>> {
        return db.collection("ads")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    StoreAd::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(
                    compareBy<StoreAd> { ad ->
                        when (ad.statusEnum()) {
                            AdStatus.PENDING -> 0
                            AdStatus.APPROVED -> 1
                            AdStatus.REJECTED -> 2
                        }
                    }.thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                )
            }
    }

    suspend fun submitAd(
        uid: String,
        creatorName: String,
        title: String,
        tagline: String,
        targetAppName: String,
        targetUrl: String,
        category: String,
        ctaLabel: String
    ): Result<String> {
        var path = "ads"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Signed-in user mismatch." }

            val cleanCreator = creatorName.trim()
            val cleanTitle = title.trim()
            val cleanTagline = tagline.trim()
            val cleanAppName = targetAppName.trim()
            val cleanUrl = targetUrl.trim()
            val cleanCategory = category.trim().ifBlank { "Developer Tools" }
            val cleanCta = ctaLabel.trim().ifBlank { "Learn More" }

            require(cleanCreator.length in 1..60) { "Creator name must be 1 to 60 characters." }
            require(cleanTitle.length in 3..80) { "Ad headline must be 3 to 80 characters." }
            require(cleanTagline.length in 5..140) { "Ad copy must be 5 to 140 characters." }
            require(cleanAppName.length in 2..60) { "Target app name must be 2 to 60 characters." }
            require(cleanUrl.length in 5..500) { "Destination URL must be 5 to 500 characters." }
            require(cleanCategory.length in 2..40) { "Category must be 2 to 40 characters." }
            require(cleanCta.length in 2..24) { "CTA label must be 2 to 24 characters." }

            val docRef = db.collection("ads").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "creatorName" to cleanCreator.take(60),
                "title" to cleanTitle.take(80),
                "tagline" to cleanTagline.take(140),
                "targetAppName" to cleanAppName.take(60),
                "targetUrl" to cleanUrl.take(500),
                "category" to cleanCategory.take(40),
                "ctaLabel" to cleanCta.take(24),
                "status" to AdStatus.PENDING.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun moderateAd(
        adminUid: String,
        adId: String,
        approve: Boolean,
        rejectionReason: String? = null
    ): Result<Unit> {
        val safeId = sanitizeDocId(adId)
        val path = "ads/$safeId"
        return try {
            val activeUid = requireUserId()
            check(activeUid == adminUid) { "Admin session mismatch." }

            val nextStatus = if (approve) AdStatus.APPROVED.name else AdStatus.REJECTED.name
            val reason = if (approve) {
                ""
            } else {
                rejectionReason?.trim()?.ifBlank { "Does not meet AppNexus ad guidelines." }?.take(200)
                    ?: "Does not meet AppNexus ad guidelines."
            }

            db.collection("ads").document(safeId)
                .update(
                    mapOf(
                        "status" to nextStatus,
                        "rejectionReason" to reason,
                        "reviewedBy" to adminUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAd(uid: String, adId: String): Result<Unit> {
        val safeId = sanitizeDocId(adId)
        val path = "ads/$safeId"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Signed-in user mismatch." }
            db.collection("ads").document(safeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeReviewsForApp(appId: String): Flow<List<AppReview>> {
        require(appId.isNotBlank()) { "App ID must not be blank" }
        return db.collection("reviews")
            .whereEqualTo("visibility", "PUBLIC")
            .whereEqualTo("appId", appId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AppReview::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
    }

    suspend fun postReview(
        uid: String,
        authorName: String,
        appId: String,
        appName: String,
        rating: Int,
        comment: String
    ): Result<Unit> {
        var path = "reviews"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Signed-in user mismatch." }
            require(rating in 1..5) { "Rating must be between 1 and 5 stars." }
            val trimmedComment = comment.trim()
            require(trimmedComment.length in 3..500) { "Review comment must be 3 to 500 characters." }

            val docRef = db.collection("reviews").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "authorName" to authorName.trim().ifBlank { "AppNexus User" }.take(60),
                "appId" to appId.take(128),
                "appName" to appName.trim().take(80),
                "rating" to rating,
                "comment" to trimmedComment,
                "visibility" to "PUBLIC",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteReview(uid: String, reviewId: String): Result<Unit> {
        val safeId = sanitizeDocId(reviewId)
        val path = "reviews/$safeId"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Signed-in user mismatch." }
            db.collection("reviews").document(safeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeScoutHistory(uid: String): Flow<List<ScoutHistoryEntry>> {
        require(uid.isNotBlank()) { "UID must not be blank" }
        return db.collection("users").document(uid).collection("scout_history")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ScoutHistoryEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
    }

    suspend fun saveScoutHistory(
        uid: String,
        query: String,
        summary: String,
        sourcesCount: Int
    ): Result<Unit> {
        var path = "users/$uid/scout_history"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Signed-in user mismatch." }
            val trimmedQuery = query.trim().take(200)
            val trimmedSummary = summary.trim().take(4000)
            if (trimmedQuery.isEmpty() || trimmedSummary.isEmpty()) {
                return Result.failure(IllegalArgumentException("Query and summary cannot be empty."))
            }
            val docRef = db.collection("users").document(uid)
                .collection("scout_history").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "query" to trimmedQuery,
                "summary" to trimmedSummary,
                "sourcesCount" to sourcesCount.coerceIn(0, 50),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteScoutHistory(uid: String, entryId: String): Result<Unit> {
        val safeId = sanitizeDocId(entryId)
        val path = "users/$uid/scout_history/$safeId"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Signed-in user mismatch." }
            db.collection("users").document(uid)
                .collection("scout_history").document(safeId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    companion object {
        const val BOOTSTRAPPED_ADMIN_EMAIL = "zaedevdennisrbx@gmail.com"
        private val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
    }
}
