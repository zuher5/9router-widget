# 9Router Data Flow Investigation Report

Status: COMPLETED (Verified from source `C:\Users\Zuher\projects\9router-mibp-version`)

## 1. Database Architecture
- **Engine**: SQLite dengan adapter chain di `src/lib/db/driver.js` (`bun:sqlite` -> `better-sqlite3` -> `node:sqlite` -> `sql.js`).
- **Schema**: Didefinisikan di `src/lib/db/schema.js` (versi 1, 11 tabel).
- **Tabel Utama**:
  - `usageHistory`: Menyimpan log per request (id, timestamp, provider, model, connectionId, apiKey, promptTokens, completionTokens, cost, status, tokens [JSON], meta).
  - `usageDaily`: Agregasi harian untuk query jangka panjang (`7d`, `30d`, `all`).
  - `requestDetails`: Data observability detail dengan latency dan payload redacted.
  - `settings`: Konfigurasi global termasuk hash password admin.

## 2. Request Lifecycle & Observability
1. Client -> `/v1/*` -> `chatCore.js` -> upstream provider.
2. Respons provider diterima -> `canonicalizeUsage()` menstandarkan token (input, output, cached).
3. `saveRequestUsage()` menulis ke `usageHistory` dan mengakumulasi ke `usageDaily`.
4. `saveRequestDetail()` menulis ke `requestDetails`.

## 3. Flow Android Client
```
[Android App / Widget]
       |
       |  HTTP/HTTPS + Cookie (auth_token)
       v
[Next.js Gateway (src/dashboardGuard.js)]
       |
       v
[API Handler (src/app/api/usage/stats)]
       |
       v
[Usage Repository (src/lib/db/repos/usageRepo.js)]
       |
       v
[SQLite (usageDaily & usageHistory)]
```

Android mengakses data melalui HTTP API dan tidak menyentuh database SQLite secara langsung.
