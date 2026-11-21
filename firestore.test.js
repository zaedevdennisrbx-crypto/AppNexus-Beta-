const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";
const ADMIN_EMAIL = "zaedevdennisrbx@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("library").get());
  await assertFails(unauthDb.collection("community_apps").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("reviews").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("verified_creators").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("ads").where("status", "==", "APPROVED").get());
});

test("UserProfile: owner CRUD succeeds; cross-user & ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Dev",
      email: "alice@example.com",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredCategory: "Developer Tools",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("LibraryEntry: owner create, partial status update, list & cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("library").get());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("library").doc("app_termux").set({
      userId: ALICE_UID,
      appId: "app_termux",
      appName: "Termux Studio",
      developer: "Termux Team",
      category: "Developer Tools",
      version: "0.119.0",
      sizeMb: 34.5,
      rating: 4.9,
      status: "WISHLIST",
      personalNote: "Install after backup",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("library").doc("app_termux").update({
      status: "INSTALLED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("library").get());
});

test("CommunityApp with creatorName & Reviews: filtered queries succeed, unfiltered fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("community_apps").doc("comm_1").set({
      userId: ALICE_UID,
      creatorName: "Alice Creations",
      authorName: "Alice",
      appName: "Orbital Notes",
      packageName: "dev.alice.orbital",
      developer: "Alice Labs",
      category: "AI & Productivity",
      version: "2.1.0",
      sizeMb: 18.4,
      tagline: "Local-first encrypted workspace",
      description: "Fast Kotlin Multiplatform notes with markdown and offline sync.",
      visibility: "PUBLIC",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    bobDb.collection("community_apps").where("visibility", "==", "PUBLIC").get()
  );
  await assertFails(bobDb.collection("community_apps").get());

  await assertSucceeds(
    bobDb.collection("reviews").doc("rev_1").set({
      userId: BOB_UID,
      authorName: "Bob",
      appId: "comm_1",
      appName: "Orbital Notes",
      rating: 5,
      comment: "Super snappy and clean Material 3 UI!",
      visibility: "PUBLIC",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("reviews")
      .where("visibility", "==", "PUBLIC")
      .where("appId", "==", "comm_1")
      .get()
  );
});

test("VerifiedCreators: only verified Admin can grant/revoke badges; non-admins & unverified email spoof denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const spoofAdminDb = testEnv
    .authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const now = new Date(Date.now() - 2000);

  // Alice cannot self-verify
  await assertFails(
    aliceDb.collection("verified_creators").doc(ALICE_UID).set({
      userId: ALICE_UID,
      creatorName: "Alice Creations",
      badgeLabel: "Verified Creator",
      verifiedByUid: ALICE_UID,
      visibility: "PUBLIC",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Unverified email spoof cannot verify creators
  await assertFails(
    spoofAdminDb.collection("verified_creators").doc(ALICE_UID).set({
      userId: ALICE_UID,
      creatorName: "Alice Creations",
      badgeLabel: "Verified Creator",
      verifiedByUid: "spoof_uid",
      visibility: "PUBLIC",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Verified Admin succeeds
  await assertSucceeds(
    adminDb.collection("verified_creators").doc(ALICE_UID).set({
      userId: ALICE_UID,
      creatorName: "Alice Creations",
      badgeLabel: "Verified Creator",
      verifiedByUid: ADMIN_UID,
      visibility: "PUBLIC",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Signed-in users can list PUBLIC verified creators
  await assertSucceeds(
    aliceDb.collection("verified_creators").where("visibility", "==", "PUBLIC").get()
  );

  // Non-admin cannot delete badge; Admin can revoke badge
  await assertFails(aliceDb.collection("verified_creators").doc(ALICE_UID).delete());
  await assertSucceeds(adminDb.collection("verified_creators").doc(ALICE_UID).delete());
});

test("StoreAds: creators submit PENDING only; Admin approves; users see APPROVED or own ads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const now = new Date(Date.now() - 2000);

  // Alice cannot create an already-APPROVED ad
  await assertFails(
    aliceDb.collection("ads").doc("ad_spoof").set({
      userId: ALICE_UID,
      creatorName: "Alice Creations",
      title: "Orbital Notes Pro",
      tagline: "Write faster with local markdown sync.",
      targetAppName: "Orbital Notes",
      targetUrl: "https://example.com/orbital",
      category: "AI & Productivity",
      ctaLabel: "Try Now",
      status: "APPROVED",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice creates a valid PENDING ad
  await assertSucceeds(
    aliceDb.collection("ads").doc("ad_1").set({
      userId: ALICE_UID,
      creatorName: "Alice Creations",
      title: "Orbital Notes Pro",
      tagline: "Write faster with local markdown sync.",
      targetAppName: "Orbital Notes",
      targetUrl: "https://example.com/orbital",
      category: "AI & Productivity",
      ctaLabel: "Try Now",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot read Alice's PENDING ad, nor query all PENDING ads
  await assertFails(bobDb.collection("ads").doc("ad_1").get());
  await assertFails(bobDb.collection("ads").where("status", "==", "PENDING").get());

  // Alice can list her own ads
  await assertSucceeds(aliceDb.collection("ads").where("userId", "==", ALICE_UID).get());

  // Alice cannot self-approve her own ad
  await assertFails(
    aliceDb.collection("ads").doc("ad_1").update({
      status: "APPROVED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Admin lists all ads and approves ad_1
  await assertSucceeds(adminDb.collection("ads").get());
  await assertSucceeds(
    adminDb.collection("ads").doc("ad_1").update({
      status: "APPROVED",
      reviewedBy: ADMIN_UID,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob can now query APPROVED ads
  await assertSucceeds(bobDb.collection("ads").where("status", "==", "APPROVED").get());
});
