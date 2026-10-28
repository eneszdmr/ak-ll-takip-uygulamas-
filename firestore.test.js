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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot read recovery_profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("recovery_profiles").doc(ALICE_UID).get());
});

test("Authenticated user: can create and read their own recovery profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    aliceDb.collection("recovery_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      phoneNumber: "5071231212",
      email: "alice@example.com",
      securityQuestion: "İlkokul öğretmeninizin adı nedir?",
      recoveryCodeHint: "ECZA-****",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("recovery_profiles").doc(ALICE_UID).get());
});

test("Authenticated user: cannot read another user's recovery profile (PII isolation)", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("recovery_profiles").doc(BOB_UID).set({
      userId: BOB_UID,
      phoneNumber: "5329876543",
      email: "bob@example.com",
      createdAt: new Date(Date.now() - 1000),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("recovery_profiles").doc(BOB_UID).get());
});

test("Authenticated user: cannot inject shadow fields on create or update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("recovery_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      phoneNumber: "5071231212",
      email: "alice@example.com",
      isAdmin: true,
      createdAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated user: can query their own recovery profile with matching userId filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("recovery_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      phoneNumber: "5071231212",
      email: "alice@example.com",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("recovery_profiles").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: fails query without userId filter (rules are not filters)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("recovery_profiles").get());
});
