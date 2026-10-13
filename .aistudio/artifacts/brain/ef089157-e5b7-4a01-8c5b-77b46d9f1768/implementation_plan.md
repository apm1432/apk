# TG CourseStream Bot - Android App Implementation Plan

An Android application built to connect directly to the **TG-coursestreamBot** backend deployed at `https://sick-nikaniki-shubham8208989-6d7c863a.koyeb.app/`, fully synchronized with the Go source code architecture of `AnataVortex7/TG-coursestreamBot`. The app provides complete, seamless access for both **Admins** and **Students**, complete with an in-app video player, offline download manager, PDF study materials viewer, Telegram channel sync, and Room SQLite database cache.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The plan has been revised based on your public GitHub repository (`AnataVortex7/TG-coursestreamBot`) and your deployed Koyeb server:

- **Confirmed Backend**: Target server set to `https://sick-nikaniki-shubham8208989-6d7c863a.koyeb.app/`. Handles Koyeb cold-starts with automatic health checking and retry mechanisms.
- **Confirmed Source Code Mirroring**:
  - Implements the exact API contracts found in `internal/routes/admin_api.go`, `web.go`, `watch.go`, `offline.go`, and `channel.go`.
  - **Admin Studio**: Login, Course Management (Add, Edit, Delete, Bulk Rename), Channel Sync (`/api/admin/sync`), Stats & Live Streams (`/api/admin/stream/live`), Worker Registry, and Announcements.
  - **Student Portal**: Course exploration, Folder/Lecture browsing, Telegram OTP or Password login, Stream playback (`/watch-stream/:token`), and PDF viewer (`/pdf-view/:token`).
- **Confirmed Offline Playback Engine**: In-app video player with gesture controls and speed settings, background OkHttp download engine saving to private app storage, and local Room cache for courses and watch history.

---

## 1. Overview & Core Concept

- **What It Does**: Bridges the Golang TG-coursestreamBot backend into a modern, native Android educational experience. Both students and administrators can access all functionality from a single APK.
- **Target Audience**: Students streaming course lectures without Telegram app distractions, and Admins managing lecture libraries, syncing Telegram channels, and monitoring active streams on the go.
- **Key Value**: Delivers zero-buffer native streaming, 100% offline playback in airplane mode, PDF reading, and seamless mobile management for Telegram bot owners.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Authentication & Role Selection**:
   - **Student Login**: Choice between Password login or Telegram OTP (`/login/otp/send` -> verify) with automatic deep-link handling.
   - **Admin Login**: Admin portal (`/api/admin/login`) with elevated privilege mode and direct dashboard access.
   - **Demo / Offline Mode**: Instant offline access to previously cached courses and downloaded lectures without requiring re-authentication.

2. **Student Experience**:
   - **Course Catalog**: Categorized course grid (e.g., Development, Design, Competitive Exams) with search, filter by "Downloaded Only", and enrolled course badges.
   - **Folder & Module Explorer**: Course curriculum hierarchy (Folders -> Lectures & PDFs) mirroring `handleFolderFiles` in Go.
   - **In-App Video Player**: Custom controls with Play/Pause, ±10s seek, speed changer (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x), full-screen toggle, sleep timer, and auto-resume from `WatchHistory`.
   - **Offline Downloads Manager**: Progress monitoring, pause/resume/delete downloads, and offline video badge indicators.
   - **Study Materials (PDF Viewer)**: In-app document and PDF viewer for course notes and lecture slides.

3. **Admin Studio (Full Management)**:
   - **Dashboard & Stats**: Live user count, active streams, total storage, and Koyeb worker status.
   - **Course Manager**: Create new courses, upload thumbnails, edit course details, add folder structures, and bulk-rename files.
   - **Telegram Channel Sync**: Trigger instant Telegram channel sync (`/api/admin/sync`) to pull newly posted videos from Telegram into the course library.
   - **Broadcast & Announcements**: Create and publish announcements to all students.
   - **Server & URL Monitor**: Ping `sick-nikaniki-shubham8208989-6d7c863a.koyeb.app/` with round-trip latency graphs and status badges.

### Visual Identity & Theme

- **Aesthetic Direction**: *Deep Cyber Obsidian & Neon Sapphire* — sleek, modern dark UI designed for low glare during video watching, with high-contrast text and vibrant accents.
- **Color Palette**:
  - `Primary`: Electric Blue `#388BFD`
  - `Secondary`: Cyan Glow `#58A6FF`
  - `Background`: Deep Midnight `#0D1117`
  - `Surface`: Slate `#161B22`
  - `Surface Variant`: Border `#30363D`
  - `Success`: Mint `#2EA043`
  - `Warning`: Amber `#D29922`
- **Typography & Polish**: Clear hierarchy with responsive layouts for phones and tablets, animated download progress bars, and Material 3 ripple interactions.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dual-Mode Architecture in One APK**:
  - *Chosen Approach*: Unified app allowing instant switching between Student View and Admin Studio with role-based security.
  - *Why*: Eliminates the need to maintain and install two separate APKs while keeping administrative controls secure.
- **Decision 2: Hybrid Online/Offline Media Pipeline**:
  - *Chosen Approach*: When online, the video player streams directly from `/watch-stream/:token`. When downloaded, the player automatically plays from local storage (`context.filesDir/downloads`) without internet.
  - *Why*: Guarantees zero buffering when offline and seamless transition for students on poor mobile data.
- **Decision 3: Room Database as Single Source of Truth**:
  - *Chosen Approach*: All courses, modules, lectures, watch positions, and download tasks are cached in Room SQLite.
  - *Why*: Instant screen loads, offline browsing, and automatic synchronization when network is available.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
│ ┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────┐ │
│ │   Student Portal     │ │  Native Video Player │ │   Admin Studio   │ │
│ │  (Catalog, Folders,  │ │  (±10s, Speed, Full- │ │ (Courses, Users, │ │
│ │   PDFs, Downloads)   │ │   screen, Position)  │ │  Sync, Monitor)  │ │
│ └──────────┬───────────┘ └──────────┬───────────┘ └────────┬─────────┘ │
└────────────┼────────────────────────┼──────────────────────┼───────────┘
             ▼                        ▼                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     ViewModel & StateFlow Layer                        │
│   CourseViewModel │ PlayerViewModel │ DownloadViewModel │ AdminViewModel│
└────────────────────────────────────┬───────────────────────────────────┘
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Repository & API Client                         │
│ ┌───────────────────────────────┐   ┌────────────────────────────────┐ │
│ │      CourseRepository         │   │       AdminRepository          │ │
│ └──────────────┬────────────────┘   └───────────────┬────────────────┘ │
└────────────────┼────────────────────────────────────┼──────────────────┘
                 ▼                                    ▼
┌─────────────────────────────────┐   ┌──────────────────────────────────┐
│          Room Database          │   │      Koyeb Backend API Engine    │
│  - CourseEntity                 │   │  - https://sick-nikaniki-...     │
│  - LectureEntity                │   │  - /api/admin/* (Courses, Sync)  │
│  - DownloadTaskEntity           │   │  - /courses, /watch-stream/*     │
│  - WatchHistoryEntity           │   │  - OkHttp Offline Stream Pipes   │
└─────────────────────────────────┘   └──────────────────────────────────┘
```

### Room Schema & Entities

1. **CourseEntity**:
   - `id`: String (Primary Key)
   - `title`: String
   - `category`: String
   - `description`: String
   - `instructor`: String
   - `thumbnailUrl`: String
   - `lectureCount`: Int
   - `totalDurationSeconds`: Int
   - `isEnrolled`: Boolean
2. **LectureEntity**:
   - `id`: String (Primary Key)
   - `courseId`: String (Index)
   - `folderId`: String?
   - `title`: String
   - `streamToken`: String
   - `streamUrl`: String
   - `durationSeconds`: Int
   - `fileType`: String (`VIDEO`, `PDF`, `AUDIO`)
   - `fileSize`: Long
   - `isDownloaded`: Boolean
   - `localPath`: String?
3. **DownloadTaskEntity**:
   - `id`: String (Primary Key)
   - `lectureId`: String
   - `courseId`: String
   - `title`: String
   - `progress`: Float
   - `bytesDownloaded`: Long
   - `totalBytes`: Long
   - `status`: String (`PENDING`, `DOWNLOADING`, `COMPLETED`, `PAUSED`, `FAILED`)
4. **WatchHistoryEntity**:
   - `lectureId`: String (Primary Key)
   - `courseId`: String
   - `lastPositionMs`: Long
   - `durationMs`: Long
   - `completed`: Boolean
   - `lastWatchedAt`: Long
