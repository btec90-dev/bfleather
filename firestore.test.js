const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const firebaseCompat = require("/usr/local/lib/node_modules/@firebase/rules-unit-testing/node_modules/firebase/compat/app");
require("/usr/local/lib/node_modules/@firebase/rules-unit-testing/node_modules/firebase/compat/firestore");
const serverTimestamp = () => firebaseCompat.firestore.FieldValue.serverTimestamp();

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid) {
  return {
    userId: uid,
    patientName: "Fatou",
    patientAge: "28",
    currentWeek: 4,
    dueDate: "15 Juillet 2027",
    daysRemaining: 249,
    dashboardDueDateText: "24 Juillet 2026 à 09:30",
    gestationalWeeks: 16,
    gestationalDaysRemainder: 4,
    remainingCountdownDays: 168,
    currentTrimester: "Trimestre 2",
    progressPercent: 0.55,
    healthMood: "Calme",
    healthWeight: "64.5",
    waterGlasses: 4,
    bloodPressure: "118/76",
    personalNotesText: "test",
    momAvatar: "🌸",
    partnerAvatar: "💙",
    babyAvatar: "👶",
    languageCode: "FR",
    themePaletteName: "BLUSH_PEARL",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validTaskPayload(uid) {
  return {
    userId: uid,
    title: "Estrace 2 mg PO",
    date: "06 nov. 2026",
    category: "Médication",
    details: "AM and PM",
    isCompleted: false,
    hasAlarm: true,
    alarmTimeMinutesBefore: 15,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validAppointmentPayload(uid) {
  return {
    userId: uid,
    title: "Consultation Prénatale",
    dateStr: "13 Nov 2026",
    timeStr: "09:30",
    doctor: "Dr. Laurent (OB/GYN)",
    location: "Centre de Santé Maternelle",
    clinicalNotes: "Contrôle mensuel",
    eventType: "Rendez-vous",
    dayOfMonth: 13,
    monthIndex: 10,
    yearNum: 2026,
    isCompleted: false,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validNotePayload(uid) {
  return {
    userId: uid,
    title: "Note Quotidienne",
    content: "Humeur calme",
    category: "daily",
    timestampText: "08 nov. 2026",
    mood: "Calme",
    symptoms: ["Fatigue"],
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
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

test("Unauthenticated user: cannot read or write any user collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("profile").doc("main").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("appointments").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").set(validTaskPayload(ALICE_UID)));
});

test("Cross-user isolation: Bob cannot read or write Alice's collections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("main").set(validProfilePayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").set(validTaskPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("appointments").doc("apt_1").set(validAppointmentPayload(ALICE_UID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("profile").doc("main").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").update({ isCompleted: true, updatedAt: serverTimestamp() }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("appointments").doc("apt_1").delete());
});

test("Identity spoofing & shadow update tests: rejects mismatched userId, ghost fields, and invalid IDs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Mismatched userId
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").set(validTaskPayload(BOB_UID)));

  // Ghost field in create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").set({
      ...validTaskPayload(ALICE_UID),
      isAdmin: true,
    })
  );

  // Invalid doc ID
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("bad id with spaces!").set(validTaskPayload(ALICE_UID))
  );

  // Valid create, then try immutable createdAt mutation or ghost field on update
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").set(validTaskPayload(ALICE_UID)));
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").update({
      isCompleted: true,
      userId: BOB_UID,
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").update({
      isCompleted: true,
      extraField: "hack",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Owner CRUD & partial updates succeed across all synced collections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceRef = aliceDb.collection("users").doc(ALICE_UID);

  // Profile create + partial vitals update + partial water update
  await assertSucceeds(aliceRef.collection("profile").doc("main").set(validProfilePayload(ALICE_UID)));
  await assertSucceeds(aliceRef.collection("profile").doc("main").update({ waterGlasses: 6, updatedAt: serverTimestamp() }));
  await assertSucceeds(aliceRef.collection("profile").doc("main").update({
    healthMood: "Sereine",
    healthWeight: "65.0",
    waterGlasses: 7,
    bloodPressure: "120/78",
    personalNotesText: "Très bien",
    updatedAt: serverTimestamp(),
  }));

  // Tasks create + toggle + delete
  await assertSucceeds(aliceRef.collection("tasks").doc("t1").set(validTaskPayload(ALICE_UID)));
  await assertSucceeds(aliceRef.collection("tasks").doc("t1").update({ isCompleted: true, updatedAt: serverTimestamp() }));
  await assertSucceeds(aliceRef.collection("tasks").get());

  // Notes create + update
  await assertSucceeds(aliceRef.collection("notes").doc("cn_1").set(validNotePayload(ALICE_UID)));
  await assertSucceeds(aliceRef.collection("notes").doc("cn_1").update({
    title: "Note Modifiée",
    content: "Nouveau contenu",
    category: "daily",
    timestampText: "09 nov. 2026",
    mood: "Confiante",
    symptoms: ["Fatigue", "Nausées"],
    updatedAt: serverTimestamp(),
  }));

  // Appointments create + toggle
  await assertSucceeds(aliceRef.collection("appointments").doc("apt_1").set(validAppointmentPayload(ALICE_UID)));
  await assertSucceeds(aliceRef.collection("appointments").doc("apt_1").update({ isCompleted: true, updatedAt: serverTimestamp() }));

  // Documents
  await assertSucceeds(aliceRef.collection("documents").doc("doc_1").set({
    userId: ALICE_UID,
    title: "Ordonnance Estrace",
    category: "Ordonnance",
    dateStr: "06 Nov 2026",
    summary: "2 mg PO AM & PM",
    source: "hub",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));

  // Checklists create + toggle
  await assertSucceeds(aliceRef.collection("checklists").doc("mb_1").set({
    userId: ALICE_UID,
    listKey: "mom_bag",
    label: "Dossier médical",
    isChecked: false,
    sortOrder: 0,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));
  await assertSucceeds(aliceRef.collection("checklists").doc("mb_1").update({ isChecked: true, updatedAt: serverTimestamp() }));

  // Protocol steps create + toggle
  await assertSucceeds(aliceRef.collection("protocol_steps").doc("p1_1").set({
    userId: ALICE_UID,
    phaseId: 1,
    dateStr: "19 sept. 2026",
    stepName: "Blocage ovarien",
    medication: "Nortrel",
    dosage: "1 cp/jour",
    status: "Complété",
    details: "Prise quotidienne",
    isCompleted: true,
    isCriticalWindow: false,
    targetEpochMillis: 0,
    sortOrder: 0,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));
  await assertSucceeds(aliceRef.collection("protocol_steps").doc("p1_1").update({
    isCompleted: false,
    status: "En cours",
    updatedAt: serverTimestamp(),
  }));

  // Care providers
  await assertSucceeds(aliceRef.collection("care_providers").doc("cp_1").set({
    userId: ALICE_UID,
    providerType: "OB/GYN",
    name: "[Doctor's Name]",
    affiliation: "[Preferred Gyn/OB Hospital]",
    phone: "555-019-2834",
    emergencyContact: "555-019-HELP",
    address: "123 Healthcare Ave",
    portalLink: "",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));

  // Voice memos
  await assertSucceeds(aliceRef.collection("voice_memos").doc("vm_1").set({
    userId: ALICE_UID,
    title: "Questions Dr.",
    durationSec: 24,
    timestampText: "06 nov. 2026",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));

  // Wallet cards create + update + read + delete
  const validCard = {
    userId: ALICE_UID,
    cardTitle: "Carte Assurance Santé",
    cardCategory: "Assurance",
    cardNumber: "POL-88492014",
    holderName: "Fatou",
    expiryDate: "12/2028",
    notes: "Couverture complète",
    gradientKey: "teal",
    frontImageBase64: "",
    backImageBase64: "",
    sortOrder: 0,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
  await assertSucceeds(aliceRef.collection("wallet_cards").doc("wc_1").set(validCard));
  await assertSucceeds(aliceRef.collection("wallet_cards").get());
  await assertSucceeds(aliceRef.collection("wallet_cards").doc("wc_1").update({
    cardTitle: "Assurance Principale",
    gradientKey: "deep_purple",
    updatedAt: serverTimestamp(),
  }));
  await assertSucceeds(aliceRef.collection("wallet_cards").doc("wc_1").delete());
});

test("Wallet cards security: rejects unauth, cross-user access, invalid gradient, and oversized images", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const cardPayload = {
    userId: ALICE_UID,
    cardTitle: "Permis de Conduire",
    cardCategory: "Identité",
    cardNumber: "DL-49201928",
    holderName: "Fatou",
    expiryDate: "11/2035",
    notes: "",
    gradientKey: "sunset_orange",
    frontImageBase64: "abc123",
    backImageBase64: "def456",
    sortOrder: 1,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("wallet_cards").doc("wc_1").set(cardPayload));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("wallet_cards").get());

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("wallet_cards").doc("wc_1").set(cardPayload));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("wallet_cards").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("wallet_cards").doc("wc_1").update({
    cardTitle: "Hacked",
    updatedAt: serverTimestamp(),
  }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("wallet_cards").doc("wc_1").delete());

  // Invalid gradient enum
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("wallet_cards").doc("wc_bad_grad").set({
    ...cardPayload,
    gradientKey: "neon_green",
  }));

  // Ghost field
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("wallet_cards").doc("wc_ghost").set({
    ...cardPayload,
    isAdmin: true,
  }));

  // Oversized image > 420000 chars
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("wallet_cards").doc("wc_big").set({
    ...cardPayload,
    frontImageBase64: "a".repeat(420005),
  }));
});

test("Pregnancy Health Hub & Medication Reminders security: owner CRUD succeeds, unauth & cross-user fail", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceRef = aliceDb.collection("users").doc(ALICE_UID);

  // med_reminders
  const medRem = {
    userId: ALICE_UID,
    title: "Estrace 2 mg Matin",
    medication: "Estrace",
    dosage: "2 mg PO",
    hour: 8,
    minute: 0,
    repeatDaily: true,
    isEnabled: true,
    protocolPhase: "Phase 2 TEC",
    notes: "Matin 08:00",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("med_reminders").doc("mr_1").set(medRem));
  await assertSucceeds(aliceRef.collection("med_reminders").doc("mr_1").set(medRem));
  await assertSucceeds(aliceRef.collection("med_reminders").doc("mr_1").update({ isEnabled: false, updatedAt: serverTimestamp() }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("med_reminders").get());
  await assertFails(aliceRef.collection("med_reminders").doc("mr_bad_hour").set({ ...medRem, hour: 25 }));

  // preg_medications
  const pregMed = {
    userId: ALICE_UID,
    name: "Paracétamol (Acétaminophène)",
    activeIngredient: "Acétaminophène",
    category: "Antalgique",
    safetyLevel: "SAFE",
    trimester1: "Sûr",
    trimester2: "Sûr",
    trimester3: "Sûr",
    clinicalNotes: "Premier choix contre fièvre et douleurs.",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
  await assertSucceeds(aliceRef.collection("preg_medications").doc("pm_1").set(pregMed));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("preg_medications").doc("pm_1").delete());
  await assertFails(aliceRef.collection("preg_medications").doc("pm_bad").set({ ...pregMed, safetyLevel: "UNKNOWN" }));

  // nutrition_foods
  const food = {
    userId: ALICE_UID,
    name: "Saumon sauvage bien cuit",
    category: "Poissons",
    safetyStatus: "EAT",
    highlights: "Riche en oméga-3 DHA et protéines.",
    nutrients: "Oméga-3, Vitamine D",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
  await assertSucceeds(aliceRef.collection("nutrition_foods").doc("nf_1").set(food));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("nutrition_foods").get());

  // grocery_items
  const grocery = {
    userId: ALICE_UID,
    label: "Épinards frais bio",
    category: "Fruits & Légumes",
    quantity: "2 sachets",
    isChecked: false,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
  await assertSucceeds(aliceRef.collection("grocery_items").doc("gr_1").set(grocery));
  await assertSucceeds(aliceRef.collection("grocery_items").doc("gr_1").update({ isChecked: true, updatedAt: serverTimestamp() }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("grocery_items").doc("gr_1").update({ isChecked: false, updatedAt: serverTimestamp() }));

  // health_issues
  const issue = {
    userId: ALICE_UID,
    title: "Nausées matinales",
    trimester: "T1",
    severity: "Léger",
    dateStr: "13 nov. 2026",
    symptoms: "Nausées au réveil",
    doctorNotes: "Fractionner les repas",
    prescribedTreatment: "Vitamine B6 25 mg",
    isResolved: false,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
  await assertSucceeds(aliceRef.collection("health_issues").doc("hi_1").set(issue));
  await assertSucceeds(aliceRef.collection("health_issues").doc("hi_1").update({ isResolved: true, updatedAt: serverTimestamp() }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("health_issues").get());

  // expenses
  const exp = {
    userId: ALICE_UID,
    title: "Consultation RMA East Side",
    amount: 250.0,
    currency: "USD",
    category: "Médical & Clinique",
    phase: "Protocole PMA",
    dateStr: "13 nov. 2026",
    notes: "Échographie de contrôle",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
  await assertSucceeds(aliceRef.collection("expenses").doc("exp_1").set(exp));
  await assertFails(aliceRef.collection("expenses").doc("exp_bad_cur").set({ ...exp, currency: "GBP" }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_1").delete());
});


