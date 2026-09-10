"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const { initializeApp, deleteApp } = require("firebase-admin/app");
const { getDatabase } = require("firebase-admin/database");
const { createOtpService } = require("./otp-service");

test("Realtime Database transactions and deny-client rules", {
  skip: !process.env.FIREBASE_DATABASE_EMULATOR_HOST
}, async (t) => {
  // Never allow this integration test to connect to or clear a real project.
  assert.equal(process.env.FIREBASE_DATABASE_EMULATOR_HOST, "127.0.0.1:9000");
  const projectId = "demo-floodgate-recovery";
  const app = initializeApp({ projectId, databaseURL: `https://${projectId}-default-rtdb.firebaseio.com` }, "otp-test");
  const db = getDatabase(app);
  const messages = [];
  const service = createOtpService({
    secret: "emulator-only-test-key-longer-than-32-characters",
    store: { async transact(path, update) {
      const result = await db.ref(path).transaction(update, undefined, false);
      return { committed: result.committed, value: result.snapshot.val() };
    } },
    mailer: { async sendCode(email, code) { messages.push({ email, code }); } }
  });
  try {
    await db.ref().set(null);
    await t.test("concurrent sends are serialized by real transactions", async () => {
      const results = await Promise.allSettled(Array.from({ length: 8 }, () => service.send("user@example.com", "192.0.2.1")));
      assert.equal(results.filter((r) => r.status === "fulfilled").length, 1,
        results.map((r) => r.status === "rejected" ? r.reason.stack : "sent").join("\n"));
      assert.equal(messages.length, 1);
      const challenge = results.find((r) => r.status === "fulfilled").value;
      const verified = await Promise.allSettled(Array.from({ length: 8 }, () =>
        service.verify("user@example.com", challenge.challengeId, messages[0].code, "192.0.2.1")
      ));
      assert.equal(verified.filter((r) => r.status === "fulfilled").length, 1);
    });
    await t.test("anonymous clients cannot read/write recovery records", async () => {
      for (const path of ["recoveryChallenges", "recoveryRateLimits"]) {
        const url = `http://127.0.0.1:9000/${path}.json?ns=${projectId}-default-rtdb`;
        assert.equal((await fetch(url)).status, 401);
        assert.equal((await fetch(url, { method: "PUT", body: "null" })).status, 401);
      }
    });
  } finally { await db.ref().set(null); await deleteApp(app); }
});
