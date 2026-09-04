# Contributing to Mizuki

Thank you for your interest in contributing to Mizuki. This guide outlines the development setup, project structure, code standards, and pull request workflow.

---

## Development Environment Setup

### Requirements
- **JDK 21** (Eclipse Temurin or OpenJDK recommended)
- **Android Studio Ladybug (2024.2.1+)** or newer
- **Android SDK Platform 36** with Android SDK Build-Tools 36.0.0
- **NDK & CMake** (installed via Android Studio SDK Manager)
- **Git**

### Getting the Code
```bash
git clone https://github.com/chiraitori/Mizuki.git
cd Mizuki
```

### Building Locally
```bash
# Build debug APKs for all CPU architectures and universal split
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Install debug APK directly to connected ADB device
./gradlew installDebug
```

---

## Project Architecture

Mizuki follows modern Android architecture principles with pure Jetpack Compose and decoupled engine layers:

- `app/src/main/java/dev/chiraitori/mizuki/core/engine/`
  - `YtDlpWrapper.kt`: Interfacing with Chaquopy Python runtime to execute `yt-dlp` commands, parse output streams, and handle progress events.
  - `DirectFastExtractor.kt`: High-performance HTTP parser for direct stream extraction on supported hosts (e.g., TikTok, Instagram direct endpoints).
  - `DownloaderEngine.kt`: Orchestrates download pipelines, aria2c execution, and FFmpeg muxing jobs.
- `app/src/main/java/dev/chiraitori/mizuki/core/parser/`
  - `UrlParser.kt`: Validates, sanitizes, and categorizes incoming URLs.
- `app/src/main/java/dev/chiraitori/mizuki/ui/screens/`
  - `MainAppScaffold.kt`: Root navigation shell with predictive back support and fluid bottom bar transitions.
  - `home/`: Input bar, format selector modal, and active task progress.
  - `history/`: Download records, media thumbnail previews, and file management actions.
  - `settings/`: Multi-category settings hub (General, Download, Format, Network, Storage, Engine, About).

---

## Guidelines & Code Standards

1. **Jetpack Compose Best Practices**:
   - Keep Composables stateless where possible and lift state to ViewModels or hoisted State holders.
   - Use `remember` and `derivedStateOf` judiciously to minimize recomposition overhead.
   - Screen root composables must declare their own status bar padding (`Modifier.statusBarsPadding()`).

2. **No Analytics or Telemetry**:
   - Mizuki is strictly local-first. Do not introduce any analytics, crash reporting, tracking SDKs, or background ping services.

3. **Multi-ABI Compatibility**:
   - Native libraries (FFmpeg, Python, aria2c) must maintain support for `arm64-v8a`, `armeabi-v7a`, `x86_64`, and `x86`.

4. **Resource Management**:
   - All user-facing strings must reside in `res/values/strings.xml`.
   - Support English (`values`) as default and localized languages where applicable.

---

## Pull Request Workflow

1. Fork the repository and create a feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. Make your changes with focused, descriptive commits following Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`).
3. Ensure the project builds cleanly:
   ```bash
   ./gradlew assembleDebug
   ```
4. Push to your fork and submit a Pull Request against `main`.
5. GitHub Actions CI will automatically build and verify your PR across all ABI targets.
