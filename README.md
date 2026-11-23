# Secure Pass 🔐

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg)](https://developer.android.com)
[![Compile SDK](https://img.shields.io/badge/Compile%20SDK-37-brightgreen.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-37-brightgreen.svg)](https://developer.android.com)
[![Encryption](https://img.shields.io/badge/Encryption-AES--256--GCM-red.svg)](https://developer.android.com/reference/javax/crypto/Cipher)
[![KDF](https://img.shields.io/badge/KDF-PBKDF2%20(100k%20rounds)-orange.svg)](https://en.wikipedia.org/wiki/PBKDF2)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline-success.svg)](https://saurabhkundu1.github.io/Applify/)

> **High-assurance, offline credential management** — A privacy-first Android password manager protecting your credentials with authenticated AES-256-GCM encryption, PBKDF2 key derivation, hardware-backed biometric authentication, real-time entropy estimation, and built-in TOTP authenticator.

---

## 🔒 Security Architecture & Cryptographic Model

Secure Pass operates on an uncompromising zero-trust, 100% offline security architecture:

```
                                 ┌─────────────────────────────────┐
                                 │       6-Digit Master PIN        │
                                 └────────────────┬────────────────┘
                                                  │
                                                  ▼
      ┌─────────────────────┐          ┌──────────────────────┐
      │ Unique 128-bit Salt │ ───────▶ │ PBKDF2 (HmacSHA256)  │
      └─────────────────────┘          │ 100,000 Iterations   │
                                       └──────────┬───────────┘
                                                  │
                                                  ▼
                                       ┌──────────────────────┐
                                       │ 256-bit Master Key   │
                                       └──────────┬───────────┘
                                                  │
                                                  ▼
┌───────────────────────┐              ┌──────────────────────┐
│  Hardware Keystore    │ ── Biometric ──▶ AES-256-GCM Cipher │
│  (BiometricPrompt)    │   Unlock     │ (Authenticated Auth) │
└───────────────────────┘              └──────────┬───────────┘
                                                  │
                                                  ▼
                                       ┌──────────────────────┐
                                       │ Plaintext Vault JSON │
                                       │ (RAM-Only Lifetime)  │
                                       └──────────────────────┘
```

### Cryptographic Foundations
1. **Authenticated Encryption (AES-256-GCM):**
   - All vault records are serialized to JSON and encrypted using AES in Galois/Counter Mode (GCM) with 128-bit authentication tags.
   - Protects against ciphertext tampering and bit-flipping attacks.
2. **Key Derivation (PBKDF2 with HmacSHA256):**
   - Derives a 256-bit symmetric key from the master PIN using 100,000 iterations of HMAC-SHA256 and a cryptographically secure random salt generated on-device.
3. **Biometrics with 24-Hour Master PIN Policy:**
   - Supports fingerprint/biometric unlock via Android `BiometricPrompt` backed by hardware Android Keystore.
   - Enforces a mandatory master PIN re-entry every 24 hours to ensure you never forget your primary decryption secret.
4. **RAM-Only Decryption Lifetime:**
   - Unencrypted vault entries exist purely in volatile application memory and are immediately cleared when the auto-lock timer expires or the app enters the background.
5. **Private Clipboard Protection (API 33+):**
   - All copied sensitive fields (passwords, OTP codes) are tagged with Android 13's `ClipDescription.EXTRA_IS_SENSITIVE`, preventing clipboard overlay snooping and notifications.

---

## ✨ Features

### 1. Entropy-Based Password Strength Estimator
- Built-in multi-factor password strength evaluator (`PasswordStrengthEstimator.kt`):
  - **Shannon Pool Entropy:** Evaluates character space diversity across uppercase, lowercase, numbers, and symbols.
  - **Pattern Penalties:** Penalizes repeated sequences (e.g., `aaaa`), sequential runs (`12345`, `abcdef`), and common keyboard row walks (`qwerty`).
  - **Actionable Guidance:** Provides real-time suggestions (e.g., *"Add special characters"*, *"Avoid consecutive sequences"*, *"Increase length"*).

### 2. EFF Passphrase & Password Generator
- **Diceware-Style Passphrases:** Generate memorable passphrases with 6 to 12 random words separated by double hyphens (`--`).
  - Uses the bundled, deduplicated 7,776-word EFF long wordlist.
  - Delivers ~77.5 bits of true word-choice entropy at the 6-word default using on-device `SecureRandom`.
- **Character Generator:** Customizable password generation with granular switches for symbols, numbers, and ambiguous characters.

### 3. Integrated TOTP Two-Factor Authenticator
- Built-in RFC 6238 Time-based One-Time Password (TOTP) generator (`TotpGenerator.kt`).
- Stores Base32 secret keys or parses standard `otpauth://totp/` URIs directly inside vault entries.
- One-tap copying of current 6-digit codes with live 30-second countdown indicators.

### 4. Native Android Autofill Service
- Fully integrated with Android's system Autofill framework (`SecurePassAutofillService.kt`).
- Suggests matching credentials for apps and websites directly in input fields after secure PIN/biometric unlock.

### 5. Password Reuse Audit
- On-device audit detecting saved accounts sharing identical passwords.
- Privacy-preserving dialog displays account identifiers without exposing the underlying plaintext passwords.

### 6. Encrypted Vault Backup & Import
- Export and import your vault as an authenticated, encrypted Base64 text payload (`vault.txt`).
- Cloud backup is strictly disabled (`android:allowBackup="false"`) to protect against unencrypted cloud snapshots.

### 7. Customization & Theming
- 13 distinct Material Design 3 color palettes: Teal, Blue, Green, Purple, Red, Orange, Indigo, Pink, Onyx, Yellow, Cyan, Brown, and Grey.
- Dynamic Light, Dark, and System default display modes.

---

## 🛠️ Architecture & Tech Stack

```
SecurePass/
├── app/
│   ├── src/main/java/com/applify/securepass/
│   │   ├── MainActivity.kt               # Entry, unlock, and biometric authentication
│   │   ├── VaultActivity.kt              # Vault explorer, search, and credential manager
│   │   ├── CryptoManager.kt              # AES-256-GCM & PBKDF2 encryption routines
│   │   ├── PasswordStrengthEstimator.kt  # Shannon entropy & pattern penalty estimator
│   │   ├── PasswordGenerator.kt          # EFF wordlist passphrase & character generator
│   │   ├── TotpGenerator.kt              # RFC 6238 TOTP authenticator engine
│   │   ├── SecurePassAutofillService.kt  # Android Autofill framework integration
│   │   └── VaultManager.kt               # JSON serialization and in-memory vault state
│   ├── src/test/java/com/applify/securepass/
│   │   ├── PasswordStrengthEstimatorTest.kt
│   │   ├── PasswordGeneratorTest.kt
│   │   ├── TotpTest.kt
│   │   ├── TestCrypto.kt
│   │   └── VaultManagerTest.kt
│   └── build.gradle                      # Module build configuration (SDK 37)
├── LICENSE                               # MIT License
├── NOTICE                                # EFF Wordlist attribution notice
└── README.md
```

### Key Technologies
- **Language:** Kotlin 2.x
- **Target SDK:** 37 (Android 15+)
- **Compile SDK:** 37
- **Minimum SDK:** 26 (Android 8.0 Oreo)
- **UI:** Material Design 3 (`com.google.android.material:material:1.12.0`)
- **Biometrics:** AndroidX Biometric 1.1.0
- **Testing:** JUnit 4 unit tests covering crypto, password generation, TOTP, and entropy estimation
- **Quality Gates:** `warningsAsErrors = true` enforced

---

## 🚀 Building & Testing

### Prerequisites
- **JDK:** 17 or higher
- **Android SDK:** API 37 with Build Tools installed

### Command-Line Build

```powershell
# Assemble debug APK
.\gradlew.bat assembleDebug

# Run unit tests
.\gradlew.bat testDebugUnitTest

# Run strict Android lint checks
.\gradlew.bat lintDebug
```

On Linux or macOS:
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📦 Distribution & Releases

- **Applify Portal:** Hosted on [Applify](https://saurabhkundu1.github.io/Applify/) (`com.applify.securepass`).
- **Droidify / F-Droid:** Add the Applify repository to your F-Droid client:
  ```
  https://saurabhkundu1.github.io/Applify/fdroid/repo
  ```
- **Signing Key Migration:** Releases beginning with v1.5.0 (code 6) use a newly provisioned signer. When upgrading from older legacy builds, export your encrypted backup (`vault.txt`), uninstall the old build, install v1.5.0 from Applify or GitHub Releases, and import your backup.

---

## 📄 License

Secure Pass is open source software released under the **[MIT License](LICENSE)**.

The bundled passphrase word list is derived from the [Electronic Frontier Foundation (EFF)](https://www.eff.org/dice) Deep-Dive Wordlist under the CC-BY 3.0 US license. See [NOTICE](NOTICE) for full attribution.
