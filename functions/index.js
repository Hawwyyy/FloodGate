"use strict";

const { initializeApp } = require("firebase-admin/app");
const { getDatabase } = require("firebase-admin/database");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { defineSecret, defineString } = require("firebase-functions/params");
const nodemailer = require("nodemailer");
const { createOtpService, RecoveryError } = require("./otp-service");

initializeApp();
const smtpHost = defineString("SMTP_HOST");
const smtpPort = defineString("SMTP_PORT", { default: "465" });
const smtpUser = defineString("SMTP_USER");
const smtpFrom = defineString("SMTP_FROM");
const databaseUrl = defineString("RECOVERY_DATABASE_URL");
const smtpPassword = defineSecret("SMTP_PASSWORD");
const otpSecret = defineSecret("OTP_HMAC_KEY");

function database() { return getDatabase(undefined, databaseUrl.value()); }
function service() {
  const port = Number(smtpPort.value());
  if (![465, 587].includes(port)) throw new Error("Use TLS SMTP port 465 or 587");
  const transport = nodemailer.createTransport({
    host: smtpHost.value(), port, secure: port === 465, requireTLS: true,
    auth: { user: smtpUser.value(), pass: smtpPassword.value() },
    connectionTimeout: 10_000, greetingTimeout: 10_000, socketTimeout: 20_000,
    tls: { minVersion: "TLSv1.2" }
  });
  return createOtpService({
    secret: otpSecret.value(),
    store: {
      async transact(path, update) {
        const result = await database().ref(path).transaction(update, undefined, false);
        return { committed: result.committed, value: result.snapshot.val() };
      }
    },
    mailer: {
      async sendCode(email, code) {
        const result = await transport.sendMail({
          from: smtpFrom.value(), to: email, subject: "Your FloodGate verification code",
          text: `Your FloodGate verification code is ${code}.\n\nIt expires in 10 minutes. Do not share it with anyone.\nIf you did not request this code, ignore this email.`
        });
        if (!result.accepted?.length || result.rejected?.length) throw new Error("Mail not accepted");
      }
    }
  });
}

const options = {
  region: "us-central1", enforceAppCheck: true, maxInstances: 5,
  timeoutSeconds: 60, memory: "256MiB", secrets: [smtpPassword, otpSecret]
};
async function callSafely(action) {
  try { return await action(); }
  catch (error) {
    // Never log SMTP errors, email addresses, request bodies, codes or secrets.
    if (error instanceof RecoveryError) throw new HttpsError(error.code, "Unable to complete verification.");
    throw new HttpsError("unavailable", "Verification is temporarily unavailable.");
  }
}
exports.sendRecoveryCode = onCall(options, (request) => callSafely(() =>
  service().send(request.data?.email, request.rawRequest.ip)
));
exports.verifyRecoveryCode = onCall(options, (request) => callSafely(() =>
  service().verify(request.data?.email, request.data?.challengeId, request.data?.code, request.rawRequest.ip)
));

exports.cleanupRecoveryRecords = onSchedule({
  schedule: "every 60 minutes", region: "us-central1", maxInstances: 1
}, async () => {
  for (const collection of ["recoveryChallenges", "recoveryRateLimits"]) {
    // Bounded batches; transactional expiry check cannot delete a concurrently renewed record.
    const expired = await database().ref(collection).orderByChild("cleanupAt").endAt(Date.now()).limitToFirst(500).get();
    await Promise.all(Object.keys(expired.val() || {}).map((key) =>
      database().ref(`${collection}/${key}`).transaction((value) =>
        !value || value.cleanupAt <= Date.now() ? null : undefined
      )
    ));
  }
});
