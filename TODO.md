# TODO — 9Router Android Widget

Status Proyek: IMPLEMENTATION COMPLETED & DELIVERED VIA CI

## Prinsip Utama
- Sesuai audit source code `mhiqrambg/9router-mibp-version`.
- Tidak ada direct SQLite access, seluruh data melalui API HTTP.
- Zero local build resources: Build sepenuhnya dieksekusi oleh GitHub Actions CI.

---

# PHASE 1 — INVESTIGASI 9ROUTER [COMPLETED]
- [x] Pelajari struktur repository.
- [x] Cari dashboard authentication.
- [x] Temukan source code login (`src/app/api/auth/login/route.js`).
- [x] Temukan endpoint login sebenarnya: `POST /api/auth/login`.
- [x] Tentukan HTTP method: `POST`.
- [x] Tentukan request body: `{"password": "..."}`.
- [x] Tentukan response: JSON `{success, mustChangePassword}`.
- [x] Tentukan session mechanism: JWT HS256 di cookie `auth_token`.
- [x] Tentukan cookie/token/header: HttpOnly cookie `auth_token`.
- [x] Temukan session status check: `GET /api/auth/status`.
- [x] Temukan logout: `POST /api/auth/logout`.
- [x] Temukan middleware/auth guard: `src/proxy.js` -> `src/dashboardGuard.js`.

Dokumentasi: `docs/9router-auth.md`.

# PHASE 2 — INVESTIGASI USAGE API [COMPLETED]
- [x] Temukan semua `/api/usage/*`.
- [x] Cari frontend dashboard yang memanggilnya (`UsageStats.js`).
- [x] Identifikasi endpoint yang dibutuhkan (`/api/usage/stats?period=...`).
- [x] Identifikasi response JSON schema (totalRequests, promptTokens, completionTokens, byModel, recentRequests).
- [x] Identifikasi derived metrics (success rate dari recentRequests, latency dari request-details).

Dokumentasi: `docs/9router-usage-api.md`.

# PHASE 3 — INVESTIGASI DATABASE [COMPLETED]
- [x] Temukan database implementation (SQLite dengan adapter chain `bun:sqlite` -> `better-sqlite3` -> `node:sqlite`).
- [x] Temukan tabel usage (`usageHistory`, `usageDaily`, `requestDetails`).
- [x] Android mengakses data via API HTTP murni tanpa direct SQLite.

Dokumentasi: `docs/9router-data-flow.md`.

# PHASE 4 — RANCANGAN ANDROID [COMPLETED]
- [x] Kotlin 2.0.21.
- [x] Jetpack Compose Material 3.
- [x] Jetpack Glance 1.1.1 (Small & Medium AppWidget).
- [x] OkHttp 4.12.0 + NineRouterCookieJar.
- [x] EncryptedSharedPreferences (Android Keystore).
- [x] DataStore Preferences cache.
- [x] Dual-language i18n (Indonesia default + English).

# PHASE 5 — AUTHENTICATION [COMPLETED]
- [x] Login client via `NineRouterApiClient`.
- [x] `NineRouterCookieJar` menangkap `auth_token`.
- [x] Auto re-login dengan password tersimpan saat sesi 24 jam berakhir.
- [x] Penanganan error `mustChangePassword`, `rateLimited`, dan `ssoEnforced`.

# PHASE 6 — USAGE CLIENT [COMPLETED]
- [x] `UsageRepository` mengelola API call & cache DataStore.
- [x] Model deserializer kotlinx-serialization.
- [x] Background sync via WorkManager `UsageRefreshWorker`.

# PHASE 7 — DASHBOARD [COMPLETED]
- [x] Total requests, total tokens, input/output tokens.
- [x] Success rate (derived recent) & average latency (sample).
- [x] Recent requests list dengan badge status.
- [x] Filter periode (Hari Ini, 24h, 7 Hari, 30 Hari, Semua).
- [x] Dukungan mode offline dengan indikator cache.

# PHASE 8 — SOLAR SYSTEM [COMPLETED]
- [x] Matahari = Total usage (tokens & requests).
- [x] Planet = Model AI dengan ukuran proporsional token.
- [x] Animasi rotasi orbit halus via Compose Canvas.

# PHASE 9 — WIDGET [COMPLETED]
- [x] Small Widget: Total tokens, requests, success rate.
- [x] Medium Widget: Solar overview, top models, tokens/requests.
- [x] Hemat baterai: Hanya membaca data cache DataStore, refresh berkala 30 menit.

# PHASE 10 — TESTING [COMPLETED]
- [x] Unit tests `UsageModelTest` (parsing JSON & derived metrics).
- [x] MockWebServer tests `ApiClientMockTest` (200, 401, 403 mustChangePassword).

# PHASE 11 — GITHUB ACTIONS [COMPLETED]
- [x] `.github/workflows/build.yml` (JDK 17, Android SDK setup, gradlew test, assembleDebug, upload artifact).

# PHASE 12 — SECURITY REVIEW [COMPLETED]
- [x] Password & JWT tidak pernah masuk ke log aplikasi.
- [x] Credential dienkripsi dengan MasterKey AES-256 GCM.
- [x] Prompt/Response tidak disimpan (redacted oleh server).
