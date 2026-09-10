"use strict";

const { createHmac, randomInt, randomBytes, timingSafeEqual } = require("node:crypto");

const EXPIRY_MS = 10 * 60_000;
const COOLDOWN_MS = 60_000;
const WINDOW_MS = 60 * 60_000;
const RETENTION_MS = 24 * WINDOW_MS;
const MAX_ATTEMPTS = 5;

class RecoveryError extends Error {
  constructor(code) { super(code); this.code = code; }
}

function normalizeEmail(value) {
  if (typeof value !== "string") throw new RecoveryError("invalid-argument");
  const email = value.trim().toLowerCase();
  // Deliberately reject control characters/header injection as well as malformed addresses.
  if (email.length > 254 || !/^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+$/i.test(email)) {
    throw new RecoveryError("invalid-argument");
  }
  const local = email.split("@")[0];
  if (local.length > 64 || local.startsWith(".") || local.endsWith(".") || local.includes("..")) {
    throw new RecoveryError("invalid-argument");
  }
  return email;
}

/** The store must provide atomic transactions. No code, raw email or password is persisted. */
function createOtpService({ store, mailer, secret, now = Date.now }) {
  if (typeof secret !== "string" || secret.length < 32) throw new Error("OTP secret must be at least 32 characters");
  const digest = (value) => createHmac("sha256", secret).update(value).digest("hex");

  async function limitIp(ip) {
    if (!ip) throw new RecoveryError("unavailable");
    const time = now();
    const result = await store.transact(`recoveryRateLimits/${digest(`ip:${ip}`)}`, (old) => {
      const bucket = old && time < old.windowStart + WINDOW_MS ? old : { windowStart: time, count: 0 };
      if (bucket.count >= 30) return undefined;
      return { ...bucket, count: bucket.count + 1, cleanupAt: time + RETENTION_MS };
    });
    if (!result.committed) throw new RecoveryError("resource-exhausted");
  }

  async function send(rawEmail, ip) {
    const email = normalizeEmail(rawEmail);
    await limitIp(ip);
    const time = now();
    const id = randomBytes(24).toString("hex");
    const code = randomInt(0, 1_000_000).toString().padStart(6, "0");
    const path = `recoveryChallenges/${digest(`email:${email}`)}`;
    const result = await store.transact(path, (old) => {
      if (old && time < old.nextSendAt) return undefined;
      const windowStart = old && time < old.windowStart + WINDOW_MS ? old.windowStart : time;
      const sends = windowStart === old?.windowStart ? old.sends : 0;
      if (sends >= 5) return undefined;
      return {
        id, digest: digest(`${email}:${id}:${code}`), state: "sending", attempts: 0,
        expiresAt: time + EXPIRY_MS, nextSendAt: time + COOLDOWN_MS,
        windowStart, sends: sends + 1, cleanupAt: time + RETENTION_MS
      };
    });
    if (!result.committed) throw new RecoveryError("resource-exhausted");

    try {
      // Wait for SMTP acceptance before telling the app to open the code screen.
      await mailer.sendCode(email, code);
      const ready = await store.transact(path, (old) => {
        // RTDB may initially supply null on a cold cache. A no-op CAS loads server state;
        // aborting immediately would incorrectly fail a successfully sent message.
        if (!old) return null;
        if (old?.id !== id || old.state !== "sending") return undefined;
        return { ...old, state: "ready" };
      });
      if (!ready.committed || ready.value?.id !== id || ready.value?.state !== "ready") {
        throw new RecoveryError("unavailable");
      }
    } catch (_) {
      await store.transact(path, (old) => {
        if (!old) return null;
        if (old?.id !== id) return undefined;
        // Preserve throttling even on mail failures so retries cannot flood the sender.
        const { digest: unused, ...rest } = old;
        return { ...rest, state: "failed" };
      });
      throw new RecoveryError("unavailable");
    }
    return {
      challengeId: id,
      expiresInSeconds: Math.max(1, Math.floor((time + EXPIRY_MS - now()) / 1000)),
      resendAfterSeconds: Math.max(1, Math.ceil((time + COOLDOWN_MS - now()) / 1000))
    };
  }

  async function verify(rawEmail, id, code, ip) {
    const email = normalizeEmail(rawEmail);
    if (typeof id !== "string" || !/^[a-f0-9]{48}$/.test(id) || typeof code !== "string" || !/^\d{6}$/.test(code)) {
      throw new RecoveryError("invalid-argument");
    }
    await limitIp(ip);
    let outcome = "failed-precondition";
    const path = `recoveryChallenges/${digest(`email:${email}`)}`;
    const result = await store.transact(path, (old) => {
      // Recomputed on every transaction retry: concurrent verify requests cannot both succeed.
      outcome = "failed-precondition";
      if (!old || old.id !== id || old.state !== "ready" || now() >= old.expiresAt) return old;
      if (old.attempts >= MAX_ATTEMPTS) {
        outcome = "resource-exhausted";
        return old;
      }
      const expected = Buffer.from(old.digest, "hex");
      const supplied = Buffer.from(digest(`${email}:${id}:${code}`), "hex");
      if (expected.length !== supplied.length || !timingSafeEqual(expected, supplied)) {
        outcome = old.attempts + 1 >= MAX_ATTEMPTS ? "resource-exhausted" : "invalid-argument";
        return { ...old, attempts: old.attempts + 1 };
      }
      outcome = "verified";
      const { digest: unused, ...rest } = old;
      return { ...rest, state: "consumed" };
    });
    if (!result.committed || outcome !== "verified") throw new RecoveryError(outcome === "verified" ? "unavailable" : outcome);
    // This acknowledges this challenge only. It is NOT a Firebase session or password-reset grant.
    return { verified: true };
  }
  return { send, verify };
}

module.exports = { createOtpService, RecoveryError, normalizeEmail };
