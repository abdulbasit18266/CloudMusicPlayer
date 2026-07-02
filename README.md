# CloudMusicPlayer 🎵

A premium, minimal cloud-based audio streaming application that allows users to stream MP3 files directly from their personal Google Drive storage. Built natively with Kotlin and Android SDK, featuring a modern **Quiet Luxury / Glassmorphic UI** theme.

---

## 🚀 Key Highlights & Architectural Decisions

### Why Physical Device Over Emulator?
During the development phase, a conscious choice was made to use a **Physical Android Device (Xiaomi Redmi 7A)** instead of an Android Virtual Device (AVD) emulator:
* **Network & Account Realism:** Google Sign-In and OAuth 2.0 initialization require actual Google Play Services tokens, which frequently cause fallback bugs on emulators.
* **Low-End Optimization:** Testing on a low-end device (Redmi 7A) ensured the ExoPlayer instance was tightly integrated with background loop handling without causing memory leaks or heating issues.
* **Hardware Lifecycle Triggers:** Allowed real testing of the application's stability during USB debugging interrupts, lifecycle destruction (`onDestroy`), and network handovers.

---

## 🛠️ Tech Stack & Software Requirements

### Software & SDK Requirements
* **Development Environment:** Android Studio (Ladybug / Iguana versions)
* **Compile SDK / Target SDK:** 36 (Updated for strict modern runtime check compatibility)
* **Minimum SDK:** 24 (Android 7.0 Nougat)
* **Programming Language:** Kotlin

### Core Libraries & Dependencies
* **Media Engine:** Jetpack Media3 ExoPlayer (`androidx.media3:media3-exoplayer`)
* **Cloud Infrastructure:** Google Drive API v3 Client Libraries
* **Authentication:** Google Play Services Auth (`com.google.android.gms:play-services-auth`)
* **Concurrency:** Kotlin Coroutines (Dispatchers.Main / Dispatchers.IO)
* **UI Structure:** RecyclerView with dynamic ViewHolders, Glassmorphic Custom Shapes

---

## 🔄 User Flow Execution
1. **Launch Screen:** The user opens the app and encounters a translucent interface displaying their connection status.
2. **Authentication:** The user clicks "Connect Google Drive", triggers the Google Sign-In intent dialog, and authorizes access.
3. **Dynamic Fetching:** The app uses the active token to filter and query only `.mp3` extension files within the user's drive storage.
4. **Song List View:** A clean layout shows the cloud library. Clicking a track initializes an explicit `Intent` package passing metadata.
5. **Now Playing Screen:** A designated glass-card media player takes over, initiating background media buffering while providing dynamic timeline updates and play/pause controls.

---

## 📈 Phase-Wise Project Development & Evolution

The project was systematically conceptualized, coded, and committed across sequential development phases:

### 🔹 Phase 1: Model, Adapter, and Layout for RecyclerView Setup
* Established the core data architecture. Created the `Song` data model class and set up a custom `SongAdapter` to bind dataset items smoothly into the visual list structure.

### 🔹 Phase 2: Added Google Auth, Drive API, and ExoPlayer Dependencies
* Implemented build configuration architectures. Imported stable, non-conflicting dependency branches for authentication scopes, cloud queries, and media streaming.

### 🔹 Phase 3: Project Configuration Tuning & Google Login Validation
* Resolved configuration overheads. Updated `compileSdk` rules to handle layout compilation warnings, fixed client credential mismatches, and successfully authenticated explicit user sessions.

### 🔹 Phase 3 extension: Fixed Web Client ID mismatch and verified successful Google Login
* Rectified OAuth authentication variables inside `MainActivity.kt`, matching them to the Cloud Console configurations to authorize successful logins.

### 🔹 Phase 4: Integrated Google Drive API to Fetch Real MP3 Files
* Developed background data syncing. Wired a custom asynchronous utility `DriveServiceHelper` to query real music storage items, replacing hardcoded dummy arrays.

### 🔹 Phase 5: Integrated Jetpack Media3 ExoPlayer Storage Streaming
* Added core streaming functionalities. Bound active stream links directly to an ExoPlayer container instance on the dashboard for real-time low-latency cloud audio playing.

### 🔹 Phase 6: Session Management Hardening & Playback Overlap Patching
* Implemented silent sign-in caches to preserve valid tokens on orientation changes. Modified player triggers to completely dump previous stream pointers before playing an alternate song, ensuring no overlapping background noise.

### 🔹 Phase 7: Designed Premium Custom Glassmorphic UI Theme
* Transformed the basic default app styling into a "Quiet Luxury" look. Crafted custom deep-gradient background drawable resource configurations (`bg_main_gradient.xml`) paired with semi-transparent frosted-glass styling filters (`bg_glass_card.xml`).

### 🔹 Phase 8: Full-Screen Player Activity Implementation & Intent Routing
* Separated core scopes. Designed a distinct full-screen immersive playback container (`PlayerActivity`) and set up an implicit data payload distribution engine via bundle intents from the library screen.

### 🔹 Phase 9: Playback Synchronization & Final Layout Polishing
* Connected remaining functional components. Wired background threads (`Handler/Runnable`) to update the playback Seekbar pointer and duration clocks concurrently. Adjusted responsive inner paddings to ensure a seamless fit across physical displays.

### 🔹 Phase 10: Implemented premium Google profile dropdown with dynamic email display and functional signout
* Engineered user session visibility. Attached an interactive profile icon container feeding user metadata into a contextual programmatically generated `PopupMenu` component providing clean account truncation states.

### 🔹 Phase 11: Added Previous and Next buttons with complete playlist-index synchronization and auto-play next track
* Expanded media queue intelligence. Refactored intent distributions to safely bundle full dataset URL references and active index mappings across activity boundaries, granting working track skip controls and hands-free playlist looping.

### 🔹 Phase 12: Implemented real-time floating refresh button with conditional Google Drive synchronization and dynamic toast messages
* Added dynamic status indexing. Configured an overlaying Floating Action Button running multi-threaded size evaluations between ongoing arrays and live Google Cloud states to trigger real-time toasts like "Up to date" or "New file fetched".

### 🔹 Feat: Add official CloudMusicPlayer adaptive app logo
* Integrated production brand visibility. Set up tailored adaptive layer drawables (`anydpi` structures) replacing default system launcher vectors with the application's unique branding blueprint across all icon shapes.

### 🔹 Phase 13: Implement folder-wise Google Drive fetching and custom back navigation
* Re-engineered internal file discovery. Transformed global drive listing routines into a structured folder hierarchy system. Introduced `FolderAdapter`, updated `DriveServiceHelper` backend query filters to validate container parent boundaries, and implemented a smart visual back navigation header in `MainActivity`.

---

## ⚠️ Real-World Roadblocks Faced & Solutions Discovered

During development, several platform-specific bugs and constraints were intercepted and resolved:

### 1. The Cloud Stream "Silent Mute" Bug (Drive File Restrictions)
* **The Problem:** After uploading new music to Google Drive, the app retrieved the title correctly, but ExoPlayer threw a file reference exception and remained completely silent.
* **The Root Cause:** Google Drive API requires individual file access to be set to public or unconstrained visibility so external network players can buffer the stream raw binary link.
* **The Solution:** Adjusted the file privacy settings within Google Drive to **"Anyone with the link"**. The stream immediately began rendering audio perfectly without throwing access tokens out of loop bounds.

### 2. Device Installation Interruption (`INSTALL_FAILED_USER_RESTRICTED`)
* **The Problem:** Deploying the code over USB to the physical device was blocked by a rigid system deployment failure exception.
* **The Root Cause:** Built-in manufacturer security configurations (Xiaomi MIUI/HyperOS settings) proactively guard internal memories against non-market deployment procedures.
* **The Solution:** Activated Developer Options on the device and turned on **"Install via USB"**. Note that this process required a working SIM card verification step over mobile carrier data networks to successfully validate permissions.

### 3. XML Resource Syntax Conflict (`Invalid resource reference - missing /`)
* **The Problem:** The compiler crashed across multiple build sequences complaining about invalid typography pointers inside the layout document trees.
* **The Root Cause:** Standard internal system properties require syntax referencing explicitly divided by an slash divider format (`@android:drawable/ic_media_play`), whereas a typing error replaced it with a period dot format (`@android:drawable.ic_media_play`).
* **The Solution:** Swept the resource code documents and manually replaced syntax formatting periods with solid division forward slashes `/`. Cleaned the Gradle build system caching directories, passing compilation instantly.

---

## ⚙️ How to Setup the Project locally

If you are cloning or pulling this project for the first time, follow these steps to make it run on your local machine:

### 1. Clone the Repository
```bash
git clone <your-repository-url>
~~~~~~~~~~~~