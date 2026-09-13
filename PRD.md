# PRD — 9Router Android Monitor

## 1. Product Overview
9Router Android Monitor adalah aplikasi Android untuk memonitor instance 9Router melalui jaringan. Pengguna memasukkan URL/IP server dan password dashboard 9Router, lalu melihat statistik usage melalui dashboard dan widget. Fitur visual utama adalah representasi "Solar System": 9Router sebagai pusat dan model/provider sebagai planet.

Aplikasi adalah monitor/client, bukan pengganti 9Router dan tidak mengakses database 9Router secara langsung.

## 2. Problem
Pengguna harus membuka dashboard web 9Router hanya untuk melihat statistik seperti request, token, model/provider, success rate, dan latency. Aplikasi menyediakan akses cepat dari Android dan home-screen widget.

## 3. Goals
- Terhubung ke 9Router melalui IP/URL.
- Menggunakan authentication 9Router.
- Tidak meminta API key provider jika tidak diperlukan.
- Menampilkan statistik usage.
- Menyediakan Solar System visualization.
- Menyediakan Android widget.
- Ringan dan hemat baterai.
- Build melalui GitHub Actions tanpa Android Studio.

## 4. Non-Goals
- Menggantikan dashboard 9Router.
- Mengelola API key provider.
- Mengirim chat/completion.
- Mengakses SQLite 9Router secara langsung.
- Membuat backend cloud tambahan.
- Menyimpan prompt/response jika tidak diperlukan.

## 5. Core User Flow
Open App → Add 9Router Server → masukkan URL/IP → masukkan password → Connect → Authentication → Dashboard.

## 6. Authentication
Authentication HARUS mengikuti implementasi asli repository:
https://github.com/mhiqrambg/9router-mibp-version

Sebelum implementasi, agent wajib menemukan endpoint login, method, request/response, session mechanism, cookie/token/header, status check, logout, dan auth middleware. Jangan mengasumsikan implementasi dari upstream lain.

## 7. Dashboard
Tampilkan data yang benar-benar tersedia dari API:
- Total requests
- Total tokens
- Input tokens
- Output tokens
- Success rate
- Average latency
- Cost jika tersedia
- Top model
- Top provider
- Last update

## 8. Solar System
- Sun = overall 9Router usage.
- Planet = model/provider.
- Ukuran planet mengikuti usage.
- Detail planet menampilkan model/provider, requests, tokens, percentage jika tersedia.
- Harus menangani banyak model dan nama panjang.
- Visual harus ringan.

## 9. Widget
Small widget: total tokens, requests, success rate.
Medium widget: Solar System sederhana, top models, total tokens, requests.
Widget memakai cached data dan tidak melakukan network request setiap render.

## 10. Refresh & Offline
Dukung manual/background/widget refresh secara hemat.
State: loading, offline, session expired, empty data, server error.
Tampilkan cached data terakhir jika API tidak tersedia.

## 11. Security
- Jangan log password/session.
- Jangan mengambil/menyimpan provider API key jika tidak diperlukan.
- Jangan mengambil prompt/response jika tidak diperlukan.
- Jangan mengakses database secara langsung.
- Gunakan secure storage Android untuk credential/session sensitif.
- Gunakan HTTPS jika tersedia.
- Dokumentasikan risiko HTTP LAN.

## 12. Architecture
9Router → HTTP/HTTPS → Android API Client → Repository → Local Cache → Dashboard/Widget → Solar System.

UI tidak melakukan HTTP request langsung.

## 13. Suggested Stack
- Kotlin
- Jetpack Compose
- Jetpack Glance
- Coroutines
- ViewModel
- Repository pattern
- Android Keystore/secure storage
- HTTP client sesuai hasil investigasi

## 14. Device Compatibility
Baseline pengembangan/testing: perangkat kelas Redmi Note 11, tetapi target tetap Android secara umum.

## 15. Backend
Tidak perlu backend cloud tambahan. Gunakan API 9Router yang sudah ada. Endpoint baru hanya boleh dibuat jika investigasi membuktikan API existing tidak cukup dan alasannya didokumentasikan.

## 16. Development Phases
1. Research 9Router.
2. Android project.
3. Authentication.
4. Usage API.
5. Dashboard.
6. Solar System.
7. Widget.
8. Testing/optimization.
9. GitHub Actions.

## 17. Acceptance Criteria
- Login dengan URL/password berhasil.
- Invalid credentials ditangani.
- Session dan logout ditangani.
- Usage nyata tampil.
- Model/provider statistics akurat.
- Solar System merepresentasikan usage.
- Small/Medium widget bekerja dan memakai cache.
- Offline/session-expired ditangani.
- GitHub Actions dapat test, lint, build, dan upload APK.

## 18. Definition of Done
Install APK → masukkan IP/URL + password → Connect → authenticated → usage tampil → Solar System tampil → widget dapat dipasang dan menampilkan usage.
