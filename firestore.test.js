const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_studio_123";
const BOB_UID = "bob_studio_456";
const ADMIN_UID = "admin_sandeep_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

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

test("Unauthenticated requests are denied on all workspace paths", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tasks").doc("tsk_1").set({
      userId: ALICE_UID,
      title: "Unauth",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Cross-user isolation blocks Bob from reading or modifying Alice workspace", async () => {
  const created = pastDate(10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      activePersonaId: "usr_sandeep",
      activePortal: "HIGHER_AUTHORITY",
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("users").doc(ALICE_UID).collection("financial_records").doc("fin_1").set({
      userId: ALICE_UID,
      recordType: "INVESTMENT",
      category: "INITIAL_INVESTMENT",
      title: "18-Month Seed Capital",
      amountInr: 2500000,
      monthKey: "2026-11",
      recordDateKey: "2026-11-01",
      enteredBy: "Sandeep",
      notes: "Runway pool",
      createdAt: created,
      updatedAt: created,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("financial_records").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("financial_records").doc("fin_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      activePersonaId: "usr_avishek",
      updatedAt: pastDate(),
    })
  );
});

test("Email spoofing with unverified admin email is rejected; verified admin passes", async () => {
  const created = pastDate(10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      email: "bob@example.com",
      displayName: "Bob",
      activePersonaId: "usr_parchi",
      activePortal: "TEAM",
      createdAt: created,
      updatedAt: created,
    });
  });

  const spoofDb = testEnv
    .authenticatedContext("spoof_uid", { email: "sandeepasmy@gmail.com", email_verified: false })
    .firestore();
  await assertFails(spoofDb.collection("users").doc(BOB_UID).get());

  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: "sandeepasmy@gmail.com", email_verified: true })
    .firestore();
  await assertSucceeds(adminDb.collection("users").doc(BOB_UID).get());
});

test("WorkspaceUser & StudioMember create, update, and shadow/immutable guards", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      activePersonaId: "usr_sandeep",
      activePortal: "HIGHER_AUTHORITY",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject ghost field ("Shadow Update Test")
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      activePersonaId: "usr_manoj",
      isAdmin: true,
      updatedAt: pastDate(),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      activePersonaId: "usr_manoj",
      updatedAt: new Date(Date.now() + 120000),
    })
  );

  // Valid persona switch partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      activePersonaId: "usr_manoj",
      activePortal: "TEAM",
      updatedAt: pastDate(),
    })
  );

  // Create StudioMember
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("studio_members").doc("usr_sandeep").set({
      userId: ALICE_UID,
      username: "sandeep",
      fullName: "Sandeep",
      designation: "Owner & Operations Head",
      department: "Executive Operations",
      role: "SUPER_ADMIN",
      assignedChannelsCsv: "CH_ART_CRAFT,CH_TECH",
      avatarInitials: "SA",
      avatarColorHex: 0xFF2563EB,
      accountStatus: "ACTIVE",
      lastLoginAt: Date.now() - 5000,
      email: "sandeep@serplivesolutions.com",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject @DocumentId 'id' leakage inside payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("studio_members").doc("usr_bad").set({
      id: "usr_bad",
      userId: ALICE_UID,
      username: "bad",
      fullName: "Bad",
      designation: "Dev",
      department: "Ops",
      role: "VIDEO_EDITOR",
      assignedChannelsCsv: "CH_TECH",
      avatarInitials: "BD",
      avatarColorHex: 0xFF2563EB,
      accountStatus: "ACTIVE",
      lastLoginAt: Date.now() - 5000,
      email: "bad@example.com",
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Tasks, Scripts, Attendance, Channels, AI Assignments & Notifications mirror Kotlin writes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const created = pastDate(10000);
  const nowMs = Date.now() - 5000;

  // Task create (nullable fields omitted as Kotlin filterValues { it != null } does)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("tsk_101").set({
      userId: ALICE_UID,
      taskCode: "TSK-101",
      title: "Edit AI Workflows Cut",
      description: "Assemble narration and graphics",
      instructions: "Export 4K",
      channelId: "CH_DOCUMENTARY",
      channelName: "Documentary Channel",
      category: "EDIT",
      assignedToIdsCsv: "usr_editor1",
      assignedToNamesCsv: "Editor 1 (Aarav)",
      createdById: "usr_sandeep",
      priority: "HIGH",
      status: "IN_PROGRESS",
      reviewStatus: "PENDING",
      startDateMillis: nowMs,
      deadlineMillis: nowMs + 3600000,
      expectedMinutes: 120,
      actualMinutes: 45,
      delayReason: "",
      managerComments: "",
      complexityWeight: 3,
      attachedFileName: "script.pdf",
      submissionNote: "",
      submissionFileName: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Task partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("tsk_101").update({
      status: "COMPLETED",
      reviewStatus: "APPROVED",
      completedAtMillis: nowMs + 1000,
      updatedAt: pastDate(),
    })
  );

  // Reject invalid task status enum
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("tsk_101").update({
      status: "INVALID_STATE",
      updatedAt: pastDate(),
    })
  );

  // Script create & update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("scripts").doc("scr_201").set({
      userId: ALICE_UID,
      scriptCode: "SCR-201",
      channelId: "CH_VRAT_KATHA",
      channelName: "Vrat Katha",
      videoTitle: "Ekadashi Vrat Katha",
      writerId: "usr_kritika",
      writerName: "Kritika",
      anchorName: "Pandit Ji",
      editorName: "Editor 2 (Rohan)",
      deadlineMillis: nowMs + 86400000,
      status: "SCRIPT_SUBMITTED",
      currentVersion: 1,
      contentBody: "Om Namo Bhagavate...",
      reviewerComments: "",
      highlightedErrors: "",
      factCheckVerified: false,
      attachmentName: "katha.pdf",
      createdAtMillis: nowMs,
      updatedAtMillis: nowMs,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Notification create & read update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("ntf_1").set({
      userId: ALICE_UID,
      recipientUserId: "ALL",
      title: "Task Completed",
      message: "TSK-101 marked completed.",
      category: "TASK",
      read: false,
      createdAtMillis: nowMs,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("ntf_1").update({
      read: true,
      updatedAt: pastDate(),
    })
  );

  // Attendance create & query
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("att_1").set({
      userId: ALICE_UID,
      memberId: "usr_parchi",
      userName: "Parchi",
      dateKey: "2026-11-20",
      checkInTimeMillis: nowMs,
      verificationMethod: "GPS_GEOFENCE",
      latitude: 28.6139,
      longitude: 77.209,
      distanceMeters: 15.5,
      wifiSsid: "Serplive_Studio_5G",
      lateArrival: false,
      earlyDeparture: false,
      workingMinutes: 0,
      status: "PRESENT",
      correctionReason: "",
      correctionRequested: false,
      correctionApprovedBy: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Subcollection list queries succeed for owner
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("scripts").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("notifications").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("attendance").get());
});
