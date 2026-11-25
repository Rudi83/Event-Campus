# EventKampus 🎓

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="110" alt="EventKampus Logo"/>
</p>

<p align="center">
  <b>Aplikasi Android Informasi & Pendaftaran Kegiatan Kampus</b><br/>
  Universitas Malikussaleh (UNIMAL)
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Min%20SDK-API%2024%20(Android%207.0)-orange" />
  <img src="https://img.shields.io/badge/Target%20SDK-API%2037-blue" />
  <img src="https://img.shields.io/badge/Version-1.0-success" />
  <img src="https://img.shields.io/badge/UI-Material%20Design%203-6750A4?logo=materialdesign&logoColor=white" />
</p>

<p align="center">
  <a href="https://github.com/Rudi83/Event-Campus/releases/download/v1.0/EventKampus-v1.0.apk">
    <img src="https://img.shields.io/badge/⬇️%20Download%20APK-EventKampus%20v1.0-0D47A1?style=for-the-badge" />
  </a>
</p>

---

## 📖 Tentang Aplikasi

**EventKampus** adalah aplikasi mobile Android yang memudahkan mahasiswa UNIMAL menemukan, mendaftar, dan mengikuti berbagai kegiatan kampus — seminar, workshop, lomba, dan kepanitiaan — langsung dari smartphone.

> Proyek Akhir Mata Kuliah **Pemrograman Mobile**  
> Teknik Informatika — Universitas Malikussaleh (UNIMAL)  
> Semester 5 — 2026

---

## 📱 Screenshot

| Beranda | Eksplorasi | Detail Event |
|:---:|:---:|:---:|
| ![Home](https://via.placeholder.com/160x320/0D47A1/FFFFFF?text=Beranda) | ![Explore](https://via.placeholder.com/160x320/1565C0/FFFFFF?text=Eksplorasi) | ![Detail](https://via.placeholder.com/160x320/0A2463/FFFFFF?text=Detail) |

| Tiket & QR | Profil Admin | Profil User |
|:---:|:---:|:---:|
| ![Ticket](https://via.placeholder.com/160x320/FF8F00/FFFFFF?text=E-Tiket) | ![Admin](https://via.placeholder.com/160x320/0D47A1/FFFFFF?text=Admin) | ![User](https://via.placeholder.com/160x320/1565C0/FFFFFF?text=User) |

---

## ✨ Fitur Aplikasi

### 👤 Mahasiswa (User)
| # | Fitur | Keterangan |
|---|---|---|
| 1 | 🔐 **Autentikasi** | Register & login akun mahasiswa dengan validasi lengkap |
| 2 | 🏠 **Beranda** | Daftar kegiatan terbaru, filter kategori, hero banner |
| 3 | 🔍 **Eksplorasi** | Pencarian teks + filter kategori, format, harga, kuota & sorting |
| 4 | 📋 **Detail Kegiatan** | Info lengkap: pemateri, lokasi, fasilitas, syarat & ketentuan |
| 5 | 📝 **Pendaftaran** | Form registrasi dengan validasi nama, NIM, HP format Indonesia |
| 6 | 🎟️ **E-Tiket + QR Code** | Tiket digital dengan QR Code presensi unik |
| 7 | 🗺️ **Peta Lokasi** | Navigasi ke lokasi kegiatan via Google Maps |
| 8 | 🔔 **Notifikasi** | Konfirmasi pendaftaran & pengingat jadwal kegiatan |
| 9 | 🌐 **Portal UNIMAL** | Akses website kampus langsung dalam aplikasi (WebView) |
| 10 | 👤 **Profil** | Statistik event diikuti, poin SKP, dan e-sertifikat |
| 11 | 🌙 **Dark Mode** | Mendukung mode gelap sistem Android |

### 🛡️ Admin (Nasyiruddin)
| # | Fitur | Keterangan |
|---|---|---|
| 1 | ➕ **Tambah Kegiatan** | Form lengkap: judul, kategori, pemateri, kuota, harga, deskripsi |
| 2 | ✏️ **Edit Kegiatan** | Ubah semua data kegiatan yang sudah dibuat |
| 3 | 🗑️ **Hapus Kegiatan** | Hapus kegiatan dari database dengan konfirmasi |
| 4 | 👁️ **Badge Admin** | Identitas admin terlihat jelas di halaman profil |

---

## 🔑 Akun Demo

> Ketuk baris akun di halaman login untuk mengisi otomatis

| Role | Email | Password |
|---|---|---|
| 🛡️ **Admin** | nasyir@mhs.unimal.ac.id | password123 |
| 👤 **User** | sarah@mhs.unimal.ac.id | password123 |

---

## 🏗️ Arsitektur & Teknologi

```
EventKampus/
├── data/
│   ├── model/        → Event, Ticket, User, Category
│   ├── db/           → SQLite (EventDatabaseHelper)
│   ├── repository/   → EventRepository, AuthRepository
│   └── session/      → SessionManager (SharedPreferences)
├── ui/
│   ├── auth/         → LoginActivity, RegisterActivity
│   ├── home/         → HomeFragment
│   ├── explore/      → ExploreFragment
│   ├── detail/       → DetailEventActivity
│   ├── register/     → RegisterEventActivity
│   ├── ticket/       → TicketDetailActivity
│   ├── tickets/      → MyTicketsFragment
│   ├── manage/       → AddEditEventActivity
│   ├── profile/      → ProfileFragment
│   └── web/          → CampusWebActivity
└── util/             → NotificationHelper, QrCodeGenerator
```

| Komponen | Teknologi |
|---|---|
| Bahasa | Kotlin |
| UI Framework | Material Design 3 + ViewBinding |
| Database | SQLite (tanpa Room/ORM) |
| Session | SharedPreferences |
| Notifikasi | NotificationCompat (lokal) |
| Peta | Google Maps Intent |
| Web | Android WebView |
| Min SDK | API 24 — Android 7.0 Nougat |
| Target SDK | API 37 |

---

## 📦 Download & Instalasi

### Download APK langsung
👉 **[EventKampus-v1.0.apk](https://github.com/Rudi83/Event-Campus/releases/download/v1.0/EventKampus-v1.0.apk)**

### Langkah instalasi
1. Download file APK di atas ke perangkat Android
2. Buka file APK
3. Jika muncul **"Install unknown apps"** → izinkan dari browser/file manager
4. Ikuti proses instalasi
5. Buka aplikasi → login dengan akun demo

> **Requirement:** Android 7.0 (Nougat) atau lebih baru

---

## 🚀 Build dari Source

```bash
# 1. Clone repository
git clone https://github.com/Rudi83/Event-Campus.git

# 2. Buka di Android Studio
#    File → Open → pilih folder Event-Campus

# 3. Tunggu Gradle sync selesai

# 4. Run (Shift+F10 atau klik ▶)
```

**Requirement:**
- Android Studio Ladybug 2024.2+
- JDK 11+
- Android SDK API 24–37

---

## 📋 Riwayat Versi

| Versi | Tanggal | Perubahan |
|---|---|---|
| **v1.0** | November 2026 | Rilis pertama — fitur lengkap CRUD, tiket, notifikasi, Maps, WebView, role admin/user |

---

## 👨‍💻 Developer

<p>
  <b>Nasyiruddin</b><br/>
  NIM: 240170050<br/>
  Program Studi: Teknik Informatika<br/>
  Universitas Malikussaleh (UNIMAL)<br/>
  <br/>
  📧 nasyir@mhs.unimal.ac.id<br/>
  Mata Kuliah: Pemrograman Mobile — Semester 5, 2026
</p>

---

<p align="center">
  Dibuat dengan ❤️ untuk UNIMAL &nbsp;•&nbsp; EventKampus v1.0 &nbsp;•&nbsp; 2026
</p>
