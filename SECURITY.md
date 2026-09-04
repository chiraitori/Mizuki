# Security Policy

## Supported Versions

Security patches and bug fixes are provided for the latest release line:

| Version | Supported          | Status |
| ------- | ------------------ | ------ |
| v0.1.x  | :white_check_mark: | Active (Public Beta) |
| < v0.1  | :x:                | Deprecated |

---

## Reporting a Vulnerability

If you discover a security vulnerability in Mizuki:

1. **Do not create a public issue** on GitHub.
2. Report the vulnerability privately via **[GitHub Private Security Advisory](https://github.com/chiraitori/Mizuki/security/advisories/new)**.
3. Include detailed steps to reproduce the issue, proof of concept code/URL, and affected Android versions or architectures.

### Response SLA
- **Initial Acknowledgment**: Within 48 hours.
- **Triage & Assessment**: Within 5 business days.
- **Fix & Public Disclosure**: Coordinated disclosure after release of a patched version.

---

## Scope & Security Architecture

- **Chaquopy Python Sandbox**: Commands executed via `yt-dlp` are isolated within the Android application sandbox.
- **Storage Security**: Storage operations strictly enforce Scoped Storage without requesting broad `MANAGE_EXTERNAL_STORAGE` permissions.
- **Cookie Security**: User-imported cookies are stored in encrypted app-private directories inaccessible by third-party applications without root privileges.
