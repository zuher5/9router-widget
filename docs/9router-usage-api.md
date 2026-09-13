# 9Router Usage API Investigation Report

Status: COMPLETED (Verified from source `C:\Users\Zuher\projects\9router-mibp-version`)

## 1. Usage API Endpoints

Semua endpoint dilindungi oleh `dashboardGuard.js` (membutuhkan cookie `auth_token` atau `requireLogin === false`).

### A. GET `/api/usage/stats`
- **Source**: `src/app/api/usage/stats/route.js:11` -> `src/lib/db/repos/usageRepo.js:346` (`getUsageStats`)
- **Query Params**: `period` (pilihan: `today`, `24h`, `7d`, `30d`, `60d`, `all`. Default: `7d`)
- **Response Schema**:
  ```json
  {
    "totalRequests": 128,
    "totalPromptTokens": 512342,
    "totalCompletionTokens": 89321,
    "totalCachedTokens": 120987,
    "totalCost": 4.82,
    "byProvider": {
      "anthropic": { "requests": 80, "promptTokens": 310000, "completionTokens": 55000, "cachedTokens": 99000, "cost": 3.21 }
    },
    "byModel": {
      "claude-sonnet-4-5 (anthropic)": {
        "requests": 80, "promptTokens": 310000, "completionTokens": 55000, "cachedTokens": 99000, "cost": 3.21,
        "rawModel": "claude-sonnet-4-5", "provider": "Anthropic", "lastUsed": "2026-09-13T09:41:02.000Z"
      }
    },
    "byAccount": {},
    "byApiKey": {},
    "recentRequests": [
      {
        "timestamp": "2026-09-13T09:41:02.000Z",
        "model": "claude-sonnet-4-5",
        "provider": "anthropic",
        "promptTokens": 4000,
        "completionTokens": 300,
        "cachedTokens": 1200,
        "status": "ok"
      }
    ],
    "activeRequests": [],
    "errorProvider": ""
  }
  ```

### B. GET `/api/usage/chart`
- **Source**: `src/app/api/usage/chart/route.js:4` -> `getChartData(period)`
- **Response**: Array of `{ "label": "...", "tokens": 1200, "cost": 0.05 }`

### C. GET `/api/usage/request-details`
- **Source**: `src/app/api/usage/request-details/route.js:5`
- **Query Params**: `page`, `pageSize` (1-100), `provider`, `model`, `status`, `startDate`, `endDate`
- **Response Schema**:
  ```json
  {
    "details": [
      {
        "id": "...",
        "provider": "anthropic",
        "model": "claude-sonnet-4-5",
        "timestamp": "2026-09-13T09:41:02.000Z",
        "status": "success",
        "latency": { "ttft": 412, "total": 3210 },
        "tokens": { "prompt_tokens": 4000, "completion_tokens": 300, "cached_tokens": 1200 },
        "request": { "redacted": true },
        "response": { "redacted": true }
      }
    ],
    "pagination": { "page": 1, "pageSize": 20, "totalItems": 128, "totalPages": 7, "hasNext": true, "hasPrev": false }
  }
  ```
- **Catatan Keamanan**: Isi request dan response di-redact menjadi `{ "redacted": true }`, menjamin prompt tidak bisa bocor ke client.

## 2. Metrik Derivatif untuk Android
- **Success Rate**: Tidak dihitung di server. Android menghitung persentase status `ok` pada array `recentRequests` di response stats.
- **Average Latency**: Dihitung dari sampel halaman 1 `request-details` (`latency.total`).
- **Solar System Mapping**: Matahari = `totalTokens`/`totalRequests`, Planet = entri dalam `byModel`.
