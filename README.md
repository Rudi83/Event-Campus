# EventKampus 🎓

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="100" alt="EventKampus Logo"/>
</p>

<p align="center">
  Aplikasi Android untuk Informasi & Pendaftaran Kegiatan Kampus<br/>
  <strong>Universitas Malikussaleh (UNIMAL)</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?logo=android" />
  <img src="https://img.shields.io/badge/Language-Kotlin-blue?logo=kotlin" />
  <img src="https://img.shields.io/badge/Min%20SDK-API%2024-orange" />
  <img src="https://img.shields.io/badge/Target%20SDK-API%2037-orange" />
  <img src="https://img.shields.io/badge/Version-1.0-purple" />
</p>

---

## 📖 Tentang Aplikasi

**EventKampus** adalah aplikasi mobile berbasis Android yang memudahkan mahasiswa UNIMAL dalam menemukan, mendaftar, dan mengikuti berbagai kegiatan kampus seperti seminar, workshop, lomba, dan kepanitiaan.

Aplikasi ini dibangun sebagai proyek akhir mata kuliah **Pemrograman Mobile** — Program Studi Teknik Informatika, Universitas Malikussaleh.

---

## ✨ Fitur Lengkap

### 👤 Mahasiswa (User)
| Fitur | Keterangan |
|---|---|
| 🔐 Autentikasi | Login & Register akun mahasiswa |
| 🏠 Beranda | Daftar kegiatan terbaru & banner hero |
| 🔍 Eksplorasi | Filter kategori, format, harga, dan kuota |
| 📋 Detail Kegiatan | Info lengkap: pemateri, lokasi, fasilitas, syarat |
| 📝 Pendaftaran | Form registrasi dengan validasi lengkap |
| 🎟️ Slip Pendaftaran | Slip digital dengan nomor registrasi unik |
| 📤 Bagikan Slip | Share slip via WhatsApp, email, dll |
| 🗺️ Peta Lokasi | Navigasi ke lokasi kegiatan via Maps |
| 🔔 Pengingat | Notifikasi pengingat jadwal kegiatan |
| 🏆 E-Sertifikat | Koleksi sertifikat dengan link Google Drive |
| 📊 Poin SKP | Kalkulasi otomatis SKP per kategori kegiatan |
| ✅ Absensi Kehadiran | Absensi via deep link pada 10 menit terakhir event |
| 👤 Profil | Statistik event, SKP, dan sertifikat |
| 🌐 Portal UNIMAL | WebView portal website kampus |

### 🛡️ Admin
| Fitur | Keterangan |
|---|---|
| ➕ Tambah Kegiatan | Form lengkap dengan date picker & peta lokasi |
| ✏️ Edit & Hapus | CRUD kegiatan secara penuh |
| 👥 Kelola Peserta | Lihat peserta per event, dikelompokkan per kegiatan |
| ✅ Setujui / Tolak | Approve atau reject pendaftaran peserta |
| 📱 Ekspor Nomor WA | Unduh nomor WhatsApp seluruh peserta per event |
| 🔗 Link Sertifikat | Input link Google Drive sertifikat per event (1x untuk semua peserta) |
| ⏰ Jadwal Otomatis | Set waktu mulai & selesai event via date-time picker |
| 📡 Notifikasi Sesi | Alarm otomatis saat event dimulai & 10 menit sebelum berakhir |
| 📤 Bagikan Absensi | Share link absensi ke WhatsApp seluruh peserta |
| 📍 Peta Interaktif | Pilih lokasi presisi via OpenStreetMap + reverse geocoding |
| 📊 Statistik | Peserta aktif, total akun, dan total event di profil admin |

---

## 🏗️ Arsitektur & Teknologi

```
EventKampus/
├── data/
│   ├── model/          → Event, Ticket, User, Category
│   ├── db/             → SQLite (EventDatabaseHelper)
│   ├── repository/     → EventRepository, AuthRepository
│   └── session/        → SessionManager (SharedPreferences)
├── ui/
│   ├── auth/           → Login, Register
│   ├── home/           → HomeFragment
│   ├── explore/        → ExploreFragment
│   ├── detail/         → DetailEventActivity
│   ├── register/       → RegisterEventActivity
│   ├── ticket/         → TicketDetailActivity
│   ├── tickets/        → MyTicketsFragment
│   ├── admin/          → ManageParticipants, EventSession, AdminParticipants
│   ├── manage/         → AddEditEventActivity
│   ├── certificate/    → MyCertificatesActivity
│   ├── attendance/     → AttendanceActivity (deep link)
│   ├── map/            → LocationPickerActivity (OSM)
│   ├── profile/        → ProfileFragment
│   └── web/            → CampusWebActivity
├── receiver/           → EventAlarmReceiver
├── util/               → EventScheduler, SkpHelper, CertificateLinkStore
└── assets/             → map_picker.html (Leaflet.js)
```

**Stack Teknologi:**
- **Bahasa:** Kotlin
- **UI:** Material Design 3 + ViewBinding
- **Database:** SQLite (tanpa Room)
- **Storage:** SharedPreferences
- **Peta:** OpenStreetMap + Leaflet.js (via WebView)
- **Geocoding:** Nominatim API
- **Alarm:** AlarmManager + BroadcastReceiver
- **Deep Link:** `eventkampus://attendance?event=...`
- **Min SDK:** API 24 (Android 7.0)
- **Target SDK:** API 37

---

## 🔑 Akun Demo

| Role | Email / NIM | Password |
|---|---|---|
| **Admin** | nasyir@mhs.unimal.ac.id | password123 |
| **Mahasiswa** | sarah@mhs.unimal.ac.id | password123 |

---

## 📊 Sistem SKP

SKP (Satuan Kredit Partisipasi) adalah poin kemahasiswaan wajib di UNIMAL sebagai syarat kelulusan (minimum **100 SKP**).

| Kategori Kegiatan | Poin SKP |
|---|---|
| Seminar | 3 SKP |
| Workshop | 4 SKP |
| Lomba | 5 SKP |
| Kepanitiaan | 6 SKP |

---

## 🔄 Alur Penggunaan

```
Mahasiswa                          Admin
    │                                │
    ├─ Register / Login              ├─ Login (role admin)
    ├─ Lihat daftar event            ├─ Tambah event + pilih lokasi di peta
    ├─ Daftar kegiatan               ├─ Set jadwal (waktu mulai & selesai)
    ├─ Terima slip pendaftaran       ├─ Kelola & setujui/tolak peserta
    ├─ Terima link absensi (WA)      ├─ Share link absensi ke peserta
    ├─ Klik link → absensi hadir     ├─ Input link sertifikat Google Drive
    └─ Unduh e-sertifikat            └─ Monitor kehadiran real-time
```

---

## 📦 Instalasi APK

1. Download `EventKampus-v1.0.apk` dari repository ini
2. Buka file APK di perangkat Android
3. Jika muncul peringatan *"Install unknown apps"*, izinkan instalasi
4. Buka aplikasi dan login menggunakan akun demo

---

## 🚀 Build dari Source

```bash
# Clone repository
git clone https://github.com/Rudi83/Event-Campus.git

# Buka di Android Studio
# File → Open → pilih folder Event-Campus

# Build & Run
# Klik Run ▶ atau Shift+F10
```

**Requirement:**
- Android Studio Ladybug atau lebih baru
- JDK 11+
- Android SDK API 24–37

---

## 📝 Riwayat Commit

| Commit | Fitur |
|---|---|
| `chore: initial project setup` | Setup awal project |
| `feat: add data models` | Model Event, Ticket, User |
| `feat: add SQLite database` | Database & Repository |
| `feat: add login and register` | Autentikasi |
| `feat: add main activity` | Bottom navigation |
| `feat: add home and explore` | Beranda & Eksplorasi |
| `feat: add event detail` | Detail event & pendaftaran |
| `feat: add ticket system` | Slip pendaftaran |
| `feat: add admin panel` | Kelola peserta & approve/reject |
| `feat: add e-certificate` | Sertifikat & SKP |
| `feat: add attendance system` | Absensi & alarm |
| `feat: add location picker` | Peta interaktif OSM |
| `feat: add profile screen` | Profil & portal web |
| `chore: finalize project` | Finalisasi v1.0 |

---

## 👨‍💻 Developer

**Nasyiruddin**  
NIM: 240170050  
Program Studi: Teknik Informatika  
Universitas Malikussaleh (UNIMAL)  

Mata Kuliah: Pemrograman Mobile — Semester 5

---

<p align="center">
  Dibuat dengan ❤️ untuk UNIMAL
</p>
