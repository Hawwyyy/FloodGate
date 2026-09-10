"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const { createOtpService, normalizeEmail } = require("./otp-service");

function fixture() {
  let time = 1_000_000;
  const records = new Map();
  const messages = [];
  let failMail = false;
  const service = createOtpService({
    secret: "a-test-only-key-that-is-longer-than-32-characters",
    now: () => time,
    store: { async transact(path, update) {
      const next = update(structuredClone(records.get(path) ?? null));
      if (next === undefined) return { committed: false };
      if (next === null) records.delete(path); else records.set(path, next);
      return { committed: true, value: structuredClone(next) };
    } },
    mailer: { async sendCode(email, code) {
      if (failMail) throw new Error("private SMTP error");
      messages.push({ email, code });
    } }
  });
  return { ...service, records, messages, advance: (ms) => { time += ms; }, failMail: () => { failMail = true; } };
}
const email = "user@example.com";
const ip = "192.0.2.10";
const failure = (code) => (error) => error.code === code;

test("normalizes email and rejects empty, malformed and header-injected addresses", () => {
  assert.equal(normalizeEmail(" User@Example.COM "), email);
  for (const value of [null, "", "name", "x@", "x@y", "x\r\nBcc:z@example.com", "a..b@example.com", "x@-bad.com"]) {
    assert.throws(() => normalizeEmail(value), failure("invalid-argument"));
  }
});
test("send returns only challenge metadata; never stores or returns plaintext email/code", async () => {
  const f = fixture();
  const sent = await f.send(email, ip);
  assert.match(sent.challengeId, /^[a-f0-9]{48}$/);
  assert.equal(sent.expiresInSeconds, 600);
  assert.equal(sent.resendAfterSeconds, 60);
  assert.match(f.messages[0].code, /^\d{6}$/);
  const stored = JSON.stringify([...f.records]);
  assert.ok(!stored.includes(email));
  assert.ok(!stored.includes(`"${f.messages[0].code}"`));
  assert.deepEqual(Object.keys(sent).sort(), ["challengeId", "expiresInSeconds", "resendAfterSeconds"]);
});
test("correct code succeeds once and cannot be replayed", async () => {
  const f = fixture();
  const c = await f.send(email, ip);
  assert.deepEqual(await f.verify(email, c.challengeId, f.messages[0].code, ip), { verified: true });
  await assert.rejects(f.verify(email, c.challengeId, f.messages[0].code, ip), failure("failed-precondition"));
});
test("wrong codes use attempts; fifth failure locks even a subsequent correct code", async () => {
  const f = fixture();
  const c = await f.send(email, ip);
  const wrong = f.messages[0].code === "000000" ? "000001" : "000000";
  for (let attempt = 1; attempt <= 5; attempt++) {
    await assert.rejects(f.verify(email, c.challengeId, wrong, ip), failure(attempt === 5 ? "resource-exhausted" : "invalid-argument"));
  }
  await assert.rejects(f.verify(email, c.challengeId, f.messages[0].code, ip), failure("resource-exhausted"));
});
test("expiry is enforced on the server, regardless of client clock", async () => {
  const f = fixture();
  const c = await f.send(email, ip);
  f.advance(600_000);
  await assert.rejects(f.verify(email, c.challengeId, f.messages[0].code, ip), failure("failed-precondition"));
});
test("resend cooldown and hourly limit survive new client sessions", async () => {
  const f = fixture();
  await f.send(email, ip);
  await assert.rejects(f.send(email, ip), failure("resource-exhausted"));
  for (let i = 0; i < 4; i++) { f.advance(60_000); await f.send(email, ip); }
  f.advance(60_000);
  await assert.rejects(f.send(email, ip), failure("resource-exhausted"));
  f.advance(3_600_000);
  await f.send(email, ip);
  assert.equal(f.messages.length, 6);
});
test("resending invalidates old challenge, and email cannot be substituted", async () => {
  const f = fixture();
  const first = await f.send(email, ip);
  f.advance(60_000);
  const second = await f.send(email, ip);
  await assert.rejects(f.verify(email, first.challengeId, f.messages[0].code, ip), failure("failed-precondition"));
  await assert.rejects(f.verify("other@example.com", second.challengeId, f.messages[1].code, ip), failure("failed-precondition"));
  assert.deepEqual(await f.verify(email, second.challengeId, f.messages[1].code, ip), { verified: true });
});
test("SMTP failure never reports delivery and retains cooldown", async () => {
  const f = fixture(); f.failMail();
  await assert.rejects(f.send(email, ip), failure("unavailable"));
  await assert.rejects(f.send(email, ip), failure("resource-exhausted"));
  assert.equal(f.messages.length, 0);
  const failed = [...f.records.values()].find((r) => r.state === "failed");
  assert.ok(failed); assert.equal(failed.digest, undefined);
});
test("parallel sends produce one mail and parallel verifies consume once", async () => {
  const f = fixture();
  const requests = await Promise.allSettled([f.send(email, ip), f.send(email, ip)]);
  assert.equal(requests.filter((r) => r.status === "fulfilled").length, 1);
  assert.equal(f.messages.length, 1);
  const c = requests.find((r) => r.status === "fulfilled").value;
  const attempts = await Promise.allSettled([
    f.verify(email, c.challengeId, f.messages[0].code, ip),
    f.verify(email, c.challengeId, f.messages[0].code, ip)
  ]);
  assert.equal(attempts.filter((r) => r.status === "fulfilled").length, 1);
});
test("IP limit prevents sending to many different email addresses", async () => {
  const f = fixture();
  for (let i = 0; i < 30; i++) await f.send(`user${i}@example.com`, ip);
  await assert.rejects(f.send("next@example.com", ip), failure("resource-exhausted"));
});
test("invalid input never reaches SMTP", async () => {
  const f = fixture();
  await assert.rejects(f.send("bad", ip), failure("invalid-argument"));
  await assert.rejects(f.verify(email, "bad", "123456", ip), failure("invalid-argument"));
  assert.equal(f.messages.length, 0); assert.equal(f.records.size, 0);
});
