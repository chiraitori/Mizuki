# Deployment & CI/CD Guide

This document details the build, signing, and release pipeline for Mizuki on GitHub Actions.

---

## Architecture Overview

Mizuki utilizes GitHub Actions for continuous integration and automated production releases:

```
[ Git Push / Tag v* ]
          │
          ├──► [ Android CI (ci.yml) ]
          │    └── Assemble Debug APKs (multi-ABI)
          │    └── Upload build artifacts
          │
          └──► [ Build & Release (release.yml) ]
               └── Inject Release Keystore from Environment Secrets
               └── Compile Proguard/R8 Release Splits
               └── Compute SHA-256 Checksums
               └── Publish GitHub Release & Attach Assets
```

---

## Build Variants & Splits

To keep download sizes minimal while embedding heavy native dependencies (FFmpeg, Python runtime, aria2c), Gradle splits the outputs by CPU ABI:

| ABI | Architecture Target | Typical Use Case |
|---|---|---|
| `arm64-v8a` | 64-bit ARM | Standard for modern Android smartphones (2017+) |
| `universal` | All architectures | Fallback build containing all binaries |
| `armeabi-v7a`| 32-bit ARM | Older legacy Android devices |
| `x86_64` | 64-bit x86 | Android emulators, ChromeOS devices with Intel/AMD |
| `x86` | 32-bit x86 | Legacy 32-bit emulators |

---

## Secrets & Release Signing

Release builds are cryptographically signed using GitHub Environment secrets configured under the `release` environment:

- `KEYSTORE_BASE64`: The Base64-encoded binary content of `mizuki-release.jks`.
- `KEYSTORE_PASSWORD`: Keystore master password.
- `KEY_ALIAS`: Signing key alias name.
- `KEY_PASSWORD`: Key alias private password.

> **Fallback Mode**: If these secrets are not defined, the workflow automatically signs release APKs using the standard Android debug certificate, ensuring non-blocking execution during community testing.

---

## Triggering a Production Release

1. **Automated via Git Tag**:
   ```bash
   git tag v0.2
   git push origin v0.2
   ```
2. **Manual via GitHub CLI**:
   ```bash
   gh workflow run release.yml -f publish_release=true -f is_prerelease=false
   ```
