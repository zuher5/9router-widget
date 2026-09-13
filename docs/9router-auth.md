# 9Router Authentication Investigation Report

Status: COMPLETED (Verified from source `C:\Users\Zuher\projects\9router-mibp-version`)

## 1. Login Endpoint
- **Path**: `POST /api/auth/login`
- **Source**: `src/app/api/auth/login/route.js:21` (`export async function POST(request)`)
- **Allow-list**: Terdaftar di `PUBLIC_API_PATHS` (`src/dashboardGuard.js:27`), bebas diakses tanpa auth token.

## 2. HTTP Method & Request Body
- **Method**: `POST`
- **Headers**: `Content-Type: application/json`
- **Body**: `{"password": "plain_password_string"}`
- Tidak ada username/email (single account model).
- Password diverifikasi terhadap bcrypt hash di SQLite `settings.password` (`src/lib/db/repos/settingsRepo.js`) atau fallback ke env `INITIAL_PASSWORD` / `"123456"`.

## 3. Login Response Shape
- **Success (200)**:
  ```json
  { "success": true, "mustChangePassword": false }
  ```
- **Default Password Remote Access (403)**:
  ```json
  { "success": false, "error": "Default password must be changed before remote access...", "mustChangePassword": true }
  ```
- **Invalid Password (401)**:
  ```json
  { "error": "Invalid password. 4 attempt(s) left before lockout.", "remainingBeforeLock": 4 }
  ```
- **Rate Limit Lockout (429)**:
  ```json
  { "error": "Too many login attempts. Locked out for 30s.", "retryAfter": 30, "resetHint": "..." }
  ```
  Header: `Retry-After: 30`
- **SSO Enforced (403)**:
  ```json
  { "error": "Password login is disabled. Use SAML/OIDC sign in." }
  ```

## 4. Session Mechanism
- **Token**: Stateless signed JWT (HS256) menggunakan library `jose`.
- **Cookie Name**: `auth_token`
- **Cookie Flags**: `httpOnly: true`, `sameSite: "lax"`, `path: "/"`, `secure: false` (bila bukan HTTPS).
- **Source**: `src/lib/auth/dashboardSession.js:60-68` (`setDashboardAuthCookie`)
- **Lifetime**: 24 jam (`.setExpirationTime("24h")`).
- **Refresh**: TIDAK ADA sliding expiration atau refresh token. Sesi mati setelah 24 jam.

## 5. Auth Status & Validation
- **Status Endpoint**: `GET /api/auth/status` (`src/app/api/auth/status/route.js:8`)
  ```json
  {
    "requireLogin": true,
    "hasPassword": true,
    "authenticated": true,
    "authMode": "password"
  }
  ```
- **Auth Guard Middleware**: `src/proxy.js` -> `src/dashboardGuard.js:202`.
  Semua route `/api/*` protected by default kecuali di `PUBLIC_API_PATHS`.
  `/api/usage/*` memerlukan cookie `auth_token` valid ATAU `settings.requireLogin === false`.

## 6. Logout Endpoint
- **Path**: `POST /api/auth/logout` (`src/app/api/auth/logout/route.js:1`)
- **Behavior**: Menghapus cookie `auth_token`, `oidc_state`, dll.
- **Response**: `{ "success": true }`

## 7. Android Client Contract
- Gunakan OkHttp dengan custom `CookieJar` untuk menyimpan dan mengirim cookie `auth_token`.
- Simpan server URL dan password di Android Keystore (EncryptedSharedPreferences).
- Saat menerima response `401 Unauthorized`, jalankan auto re-login dengan password tersimpan.
- Tampilkan warning jika menerima HTTP 403 `mustChangePassword` atau HTTP 429 rate limit.
