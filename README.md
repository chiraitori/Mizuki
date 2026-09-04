# Mizuki

An open-source, local-first video and audio downloader for Android powered by yt-dlp, aria2c, and FFmpeg, built with Jetpack Compose and Material 3 Expressive motion.

[![Android CI](https://github.com/chiraitori/Mizuki/actions/workflows/ci.yml/badge.svg)](https://github.com/chiraitori/Mizuki/actions/workflows/ci.yml)
[![Version](https://img.shields.io/badge/version-v0.1-blue.svg)](https://github.com/chiraitori/Mizuki/releases)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPLv3-green.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-10%2B%20(API%2029%2B)-brightgreen.svg)](https://developer.android.com)

---

## Overview

Mizuki runs extraction and downloading tasks entirely on-device without remote servers or telemetry. It bridges Python-based yt-dlp engines with Android's native storage access framework, hardware codecs, and notification services.

```
[ User Input / Share Sheet ]
             │
             ▼
       [ UrlParser ]
             │
      ┌──────┴──────────────────────────┐
      │ Supported direct host           │ Universal / Fallback
      ▼                                 ▼
[ DirectFastExtractor ]         [ YtDlpWrapper ]
(In-process HTTP parsing)       (Chaquopy + yt-dlp runtime)
      │                                 │
      └──────────────┬──────────────────┘
                     ▼
           [ DownloaderEngine ]
     (aria2c multi-stream / FFmpeg muxer)
                     │
                     ▼
        [ Android MediaStore / SAF ]
        (Movies/Mizuki, Music/Mizuki)
```

## Highlights

- **Dual Extraction Pipeline**: Direct HTTP parsing for zero-overhead downloads on supported hosts, backed by an embedded yt-dlp engine for thousands of media sites.
- **Customizable Format Engine**: Download up to 4K 60fps video, or extract high-bitrate audio (Opus, AAC, MP3, FLAC) with embedded album art and chapters.
- **Post-Processing with FFmpeg**: Automatic video/audio muxing, subtitle embedding (SRT, VTT, ASS), and chapter splitting.
- **SponsorBlock & Metadata**: Automatically omit sponsored segments, intros, and credits. Tag downloads with original upload metadata.
- **Cookie & Auth Management**: Import Netscape-format cookies to download private or member-only playlists securely.
- **Modern Jetpack Compose UI**: Dynamic Material You palette, per-screen independent scaffold architecture, and fluid physics-based transitions.
- **Zero Tracker Policy**: No analytics SDKs, no advertising frameworks, and no network connections other than target media hosts.

## Downloads & Architecture Variants

Pre-built APKs for version `v0.1` are available on the [Releases](https://github.com/chiraitori/Mizuki/releases) page.

| Package File | Target Architecture | Description |
|---|---|---|
| `Mizuki-v0.1-arm64-v8a.apk` | `arm64-v8a` (64-bit ARM) | Recommended for modern Android smartphones (2017+). Smallest footprint. |
| `Mizuki-v0.1-universal.apk` | Universal | Contains all native binaries. Compatible with any device; larger file size. |
| `Mizuki-v0.1-armeabi-v7a.apk` | `armeabi-v7a` (32-bit ARM) | For legacy 32-bit Android devices. |
| `Mizuki-v0.1-x86_64.apk` | `x86_64` (64-bit Intel/AMD) | For Android emulators and x86 tablets. |
| `Mizuki-v0.1-x86.apk` | `x86` (32-bit Intel) | For legacy 32-bit x86 environments. |

## Requirements

- Android 10 (API Level 29) or higher
- 64-bit ARM (`arm64-v8a`) device recommended for optimal FFmpeg transcoding speed

## Building from Source

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or newer
- JDK 21 (Eclipse Temurin recommended)
- Android SDK Platform 36

### Build Commands

```bash
# Clone the repository
git clone https://github.com/chiraitori/Mizuki.git
cd Mizuki

# Build Debug APKs (produces per-ABI and universal splits)
./gradlew assembleDebug

# Build Release APKs with R8 full-mode optimization
./gradlew assembleRelease
```

Generated APKs will be located in:
- `app/build/outputs/apk/debug/`
- `app/build/outputs/apk/release/`

### Install via ADB

```bash
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

## Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | Stream extraction and media downloading. |
| `ACCESS_NETWORK_STATE` | Detect Wi-Fi and metered network state to enforce network preference rules. |
| `POST_NOTIFICATIONS` | Foreground download progress and completion notifications. |
| `FOREGROUND_SERVICE` | Background downloading when the app is minimized. |
| `FOREGROUND_SERVICE_DATA_SYNC` | Data synchronization classification on Android 14+. |
| `VIBRATE` | Haptic feedback on action triggers and download completion. |

## Credits & Upstream Projects

Mizuki relies on the open-source work of:
- [yt-dlp](https://github.com/yt-dlp/yt-dlp) - Media extractor and core downloader engine.
- [FFmpeg](https://ffmpeg.org/) - Audio/video post-processing and container remuxing.
- [aria2](https://github.com/aria2/aria2) - Multi-connection download accelerator.
- [Chaquopy](https://chaquo.com/chaquopy/) - Python runtime environment for Android.
- [Seal](https://github.com/JunkFood02/Seal) - Architecture reference for Android yt-dlp client design.

## License

```
Mizuki Video Downloader
Copyright (C) 2026 Chiraitori

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
```
