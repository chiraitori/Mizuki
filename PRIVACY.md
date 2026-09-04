# Privacy Policy

**Effective Date:** September 4, 2026  
**Applicable Version:** Mizuki v0.1 and newer

Mizuki is an open-source, local-first utility designed to respect user autonomy and privacy. This policy details how data is handled by the application.

---

## 1. Zero Data Collection

- **No Analytics**: Mizuki does not embed any tracking SDKs, analytics libraries (e.g., Firebase Analytics, Google Analytics), or behavioral telemetry.
- **No Crash Reporting Services**: Mizuki does not transmit crash logs, stack traces, or diagnostic dumps to third-party services.
- **No Account Requirements**: Using Mizuki requires no user account, registration, or personally identifiable information (PII).

---

## 2. On-Device Storage & Local Processing

- **Download Records & History**: Download tasks, saved links, and completion logs are stored exclusively in the app's private SQLite database on your device.
- **Authentication Cookies**: When you import browser cookies (e.g., Netscape format for authenticated downloads), they are stored strictly within the app's sandboxed private storage and are never uploaded or synced to external servers.
- **Scoped Storage**: Downloaded media files (videos, audio, subtitles) are saved to your chosen public directories (`Movies/Mizuki`, `Music/Mizuki`) via Android's MediaStore API and Storage Access Framework (SAF). Mizuki only accesses files it creates.

---

## 3. Network Usage

Mizuki only performs outbound network connections in the following specific scenarios:

1. **User-Initiated Downloads**: Connecting directly to the media hosting platform or content delivery network (CDN) specified by the URL you input or share into the app.
2. **In-App Update Checks**: Making read-only HTTP GET requests to the public GitHub REST API (`https://api.github.com/repos/chiraitori/Mizuki/releases`) to determine if a newer version of Mizuki is available.
3. **yt-dlp Engine Updates**: When manually triggered by the user in Settings, downloading the official updated `yt-dlp` executable wheel directly from the official yt-dlp GitHub repository.

---

## 4. Permissions

- `INTERNET` & `ACCESS_NETWORK_STATE`: Required to resolve URLs, stream media chunks, and respect user-defined metered/Wi-Fi connection preferences.
- `POST_NOTIFICATIONS` & `FOREGROUND_SERVICE`: Required to display ongoing download progress in the system notification shade and maintain active downloads when the application is backgrounded.
- `VIBRATE`: Provides optional tactile feedback upon action completion.

---

## 5. Third-Party Dependencies

Mizuki embeds open-source engines (`yt-dlp`, `FFmpeg`, `aria2c`). These tools run directly in userspace on your device without relaying network requests through intermediary servers.

---

## 6. Contact & Inquiries

For security or privacy concerns, you may open an issue on the [GitHub repository](https://github.com/chiraitori/Mizuki/issues).
