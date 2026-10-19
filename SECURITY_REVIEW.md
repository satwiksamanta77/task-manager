# TaskManager — Security & Compatibility Review

Date: 2026-10-20. Scope: `worker/` (TypeScript/Hono + D1 on Cloudflare Workers) and
`android/` (Kotlin/Jetpack Compose). Static review only — no source code was modified.
The `web/` SPA does not exist yet, so it was skipped.

## Summary

The backend's security fundamentals are solid: PBKDF2-SHA256 password hashing with
per-user salts and constant-time verification, 256-bit random session tokens with
expiry and server-side invalidation, fully parameterized SQL, and per-user ownership
checks on every protected query. The Android app talks HTTPS-only, asks for no
dangerous permissions, and uses no APIs that threaten compatibility.

The main risks are operational, not cryptographic: the auth token is stored
unencrypted and backed up, the public release APK is a debuggable debug build, and
the public API has no rate limiting. None of these block installation or use on any
recent Android version, but items 1–4 of the fix list should be addressed before the
app is shared widely.

## Findings

### SEC-01 — Auth token stored unencrypted in DataStore
- **Severity:** Medium
- **File:** `android/app/src/main/java/com/taskmanager/app/data/local/SessionManager.kt:14`
- **Description:** The 30-day session token is written to plain DataStore preferences
  (`session.preferences_pb`). On a rooted device, or via backup/transfer, the token
  is readable in cleartext and grants full account access until it expires.
- **Recommendation:** Store the token with Jetpack Security (`EncryptedFile` or the
  `security-crypto` DataStore), or at minimum exclude it from backup (see SEC-02).

### SEC-02 — Auto Backup includes the session token
- **Severity:** Medium
- **File:** `android/app/src/main/AndroidManifest.xml:7` (`android:allowBackup="true"`)
- **Description:** With backup enabled, the DataStore file holding the auth token is
  uploaded to Google Drive Auto Backup and restored onto new devices, widening the
  token's exposure and letting a stale token survive on old devices.
- **Recommendation:** Set `android:allowBackup="false"`, or add a backup-rules XML
  that excludes the datastore (`<exclude domain="file" path="datastore/"/>`).

### SEC-03 — No rate limiting on the public API
- **Severity:** Medium
- **File:** `worker/src/routes/auth.ts` (register/login), all `worker/src/routes/*.ts`
- **Description:** Login accepts unlimited attempts (credential stuffing / password
  guessing), register accepts unlimited account creation (spam, DB fill), and every
  endpoint is unthrottled. There is no account lockout either.
- **Recommendation:** Add Cloudflare Rate Limiting rules for `/api/auth/*`, and/or a
  KV- or D1-backed attempt counter with exponential backoff and lockout.

### SEC-04 — Public release APK is a debuggable debug build
- **Severity:** Medium
- **File:** `.github/workflows/release.yml` (`gradle -p android assembleDebug`)
- **Description:** The GitHub Release publishes a debug-signed APK, which is
  `debuggable=true` and signed with the publicly known debug keystore. Anyone can
  attach a debugger, dump memory (including the in-memory token), and the APK cannot
  be upgraded to a properly signed release later without uninstalling.
- **Recommendation:** Build `assembleRelease` with a real keystore stored in GitHub
  Actions secrets, and publish that APK instead.

### SEC-05 — No maximum password length
- **Severity:** Low
- **File:** `worker/src/routes/auth.ts:48-50` (minimum 6 chars, no maximum)
- **Description:** PBKDF2 at 100,000 iterations runs over an unbounded password.
  Multi-megabyte passwords turn each register/login call into a CPU-DoS vector,
  amplified by the lack of rate limiting (SEC-03).
- **Recommendation:** Reject passwords longer than ~128 characters server-side.

### SEC-06 — Permissive CORS (`*`)
- **Severity:** Low
- **File:** `worker/src/index.ts:14` (`origin: '*'`)
- **Description:** Any website can call the API. Risk is low today because auth is
  token-based (no cookies to steal via CSRF), but a leaked token becomes usable
  from any browser origin, and the planned `web/` SPA will deserve an explicit
  allow-list.
- **Recommendation:** Keep `*` for now; restrict to the Pages domain when the web
  client ships.

### SEC-07 — Email enumeration via register
- **Severity:** Low
- **File:** `worker/src/routes/auth.ts:60` (409 `Email already registered`)
- **Description:** An attacker can probe which emails have accounts. Login itself
  correctly returns a generic `Invalid email or password`.
- **Recommendation:** Acceptable standard tradeoff; no change required. Note it in
  the threat model.

### SEC-08 — HTTP logging interceptor ships in the release artifact
- **Severity:** Low
- **File:** `android/app/src/main/java/com/taskmanager/app/data/api/ApiClient.kt:17-19`
- **Description:** `HttpLoggingInterceptor.Level.BASIC` is always on. BASIC does not
  log headers (the token is safe), but request/response lines still go to logcat in
  production builds.
- **Recommendation:** Enable the interceptor only under `BuildConfig.DEBUG`.

### SEC-09 — Missing proguard-rules.pro
- **Severity:** Info
- **File:** `android/app/build.gradle.kts:29-32` (release block references it)
- **Description:** The file does not exist. Harmless while `isMinifyEnabled = false`
  and CI builds debug, but a future release/minify build will fail.
- **Recommendation:** Add an (initially empty) `app/proguard-rules.pro` before any
  release build.

### SEC-10 — `!!` on navigation arguments
- **Severity:** Info
- **File:** `AddEditTaskViewModel.kt:38,78`, `NoteEditorViewModel.kt:39,76`
- **Description:** `taskId!!` / `noteId!!` are safe in practice (callers guard null
  first), but a future route change could turn them into crashes.
- **Recommendation:** Prefer `checkNotNull` with a message or early-return paths.

### Verified good (no action needed)

- **Password hashing** (`worker/src/auth.ts:4-8,32-48`): PBKDF2-SHA256, 100,000
  iterations, 16-byte random salt, `salt:hash` hex storage, constant-time compare.
- **Session tokens** (`worker/src/auth.ts:51-54,74-100`): 256-bit entropy from
  `crypto.getRandomValues`, 30-day expiry, expired sessions deleted on use, logout
  deletes the token server-side.
- **SQL injection**: every query uses `.bind(...)` parameters (`worker/src/db.ts`,
  all routes). The only string-interpolated SQL builds `SET` clauses from hardcoded
  column names, never user input (`tasks.ts:195`, `notes.ts:150`).
- **Authorization**: every protected query filters by `user_id`; subtask access is
  verified via JOIN to the parent task (`findOwnedSubtask`, `tasks.ts:283-291`).
- **Error handling**: generic `{ error }` messages, no stack traces
  (`index.ts` onError → `Internal server error`); login gives no user-enumeration
  signal.
- **Transport**: `BASE_URL` is HTTPS-only (`ApiClient.kt:11`); no cleartext traffic
  configured; Cloudflare edge terminates TLS with its managed certificate.
- **Secrets**: none hardcoded — no tokens/keys in code, `wrangler.toml`, or gradle
  files. `database_id` in `wrangler.toml` is a non-secret resource identifier.
- **Android manifest**: only `INTERNET` permission; `MainActivity exported=true` is
  required for the launcher; no providers, receivers, or deep links to audit.
- **Dependencies**: hono 4.6, OkHttp 4.12, Retrofit 2.11, Compose BOM 2024.06.00,
  AGP 8.5.2 — all recent with no known critical CVEs affecting this usage.
  (Gson comes in at 2.11 via converter-gson, past the 2.8.9 deserialization CVE.)

## Compatibility verdict

**Compatible — no install blockers found.** Expected to install and run on
Android 7.0 (API 24) through Android 15 (API 35) and later.

- **Version matrix (all mutually compatible):** AGP 8.5.2 ↔ Gradle 8.7 ↔ JDK 17;
  Kotlin 1.9.24 ↔ Compose Compiler 1.5.14; Compose BOM 2024.06.00 ↔ Kotlin 1.9.x;
  compileSdk/targetSdk 34 ↔ AGP 8.5.2. CI already pins exactly this set.
- **minSdk 24 coverage:** every dependency supports API 24+ (Compose ≥ 21,
  DataStore ≥ 21, OkHttp ≥ 21, Retrofit ≥ 21). No `java.time`/`android.icu` usage —
  date display is plain string slicing (`Format.kt`). `enableEdgeToEdge()` is the
  androidx compat wrapper and is safe on API 24.
- **targetSdk 34 behavior changes:** none apply — the app uses no notifications, no
  foreground services, no exact alarms, no photo picker, no scoped-storage writes,
  no WebView. Predictive back is not implemented; system back works everywhere.
- **ABI / 64-bit:** the default universal APK bundles arm64-v8a, armeabi-v7a, and
  x86_64 — compliant with the 64-bit requirement; no native-library extraction
  issues (no native libs).
- **Installability:** debug-signed APK sideloads normally on all versions (user
  approves "install unknown apps" once). No split APKs, no missing densities.

## Prioritized fix list

1. **Ship a signed release APK** (SEC-04) — switch the workflow to
   `assembleRelease` with a keystore in GitHub secrets; also resolves debuggability.
2. **Disable Auto Backup or exclude the token** (SEC-02) — one-line manifest fix.
3. **Encrypt the token at rest** (SEC-01) — Jetpack Security crypto DataStore.
4. **Rate-limit the API**, especially login/register (SEC-03).
5. **Cap password length server-side** (SEC-05).
6. **Gate the logging interceptor behind `BuildConfig.DEBUG`** (SEC-08).
7. **Restrict CORS to the Pages domain** once the web client exists (SEC-06).
8. **Add the missing `proguard-rules.pro`** before any minified release build (SEC-09).
