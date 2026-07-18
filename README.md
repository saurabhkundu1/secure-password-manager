# Secure Pass 🔐

An offline, encrypted password manager for Android.  
Store credentials locally with **AES-256 encryption**, unlock with a **6-digit PIN** or **fingerprint**.

## Features
- 🔒 **Offline-only** – No internet permission.
- 🛡️ **AES-256-GCM encryption** – Key derived from your master code (PBKDF2).
- 🔢 **6-digit master code** – Hard to brute-force.
- 🖐️ **Fingerprint unlock** – Requires code once every 24h so you never forget it.
- 📋 **Clipboard auto-clear** – Passwords wiped after 30 seconds.
- 🎲 **Strong password generator** – Customise length and character sets.
- 📝 **Secure notes** – Store security questions per entry.

## Screenshots
_(Add screenshots later)_

## Installation
1. Clone the repo: `git clone https://github.com/saurabhkundu1/secure-password-manager.git`
2. Open in Android Studio, sync Gradle, and run on device/emulator (API 26+).

## Security
- Master code never stored; only used to derive encryption key.
- Vault file (`vault.enc`) unreadable without the code.
- Fingerprint key wrapped with hardware‑backed Keystore key.

## License
MIT