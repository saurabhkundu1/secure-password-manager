# Secure Pass 🔐

An offline, high-security encrypted password manager for Android.  
Store your credentials safely in a local, encrypted vault using **AES-256-GCM**, unlocked via a **6-digit PIN** or **Biometrics**.

## 🚀 Features

- 🔒 **Total Privacy** – 100% offline. No internet permissions required. Your data never leaves your device.
- 🛡️ **Military-Grade Encryption** – Uses **AES-256-GCM** to encrypt your vault. The encryption key is derived from your PIN using **PBKDF2** with HmacSHA256 (100,000 iterations) and a unique salt.
- 🔢 **Secure Access** – Locked behind a mandatory 6-digit master code.
- 🖐️ **Biometric Integration** – Fingerprint support for convenience. Includes a 24-hour PIN re-entry policy to ensure you never forget your master code.
- ⏱️ **Auto-Lock Timer** – Automatically locks the vault after a configurable period of inactivity to prevent unauthorized access.
- 🎨 **Personalized UI** – Clean, minimalist design with extensive theme support:
  - **13 Color Palettes**: Teal, Blue, Green, Purple, Red, Orange, Indigo, Pink, Onyx, Yellow, Cyan, Brown, and Grey.
  - **Display Modes**: Full support for Light, Dark, and System default modes.
- 🔍 **Search & Favorites** – Instantly filter your entries or pin your most important accounts to the top of the list for quick access.
- 🎲 **Password Utilities** – 
  - **Generator**: Create cryptographically strong passwords with customizable length and character sets.
  - **Strength Indicator**: Real-time visual feedback on your password's complexity.
- 📁 **Encrypted Backups** – Export and import your vault as an encrypted Base64 text file (`vault.txt`) for secure portability.
- 🛠️ **Offline Support** – Built-in links to GitHub Releases and Issues for updates and feedback, maintaining your privacy without requiring app internet access.

## 🛠️ Technical Details

- **Encryption**: AES-256-GCM (Authenticated Encryption).
- **KDF**: PBKDF2 with HmacSHA256 (100,000 iterations).
- **UI Framework**: Material Components (Material 3 style) with dynamic theme attributes.
- **Biometrics**: Android BiometricPrompt API with hardware-backed Keystore integration.
- **Compliance**: Designed for F-Droid with no proprietary blobs and zero network calls.

## 📦 Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/saurabhkundu1/secure-password-manager.git
   ```
2. **Open in Android Studio**: Import the project.
3. **Sync Gradle**: Ensure all dependencies are downloaded.
4. **Build & Run**: Deploy to a device or emulator running **Android 8.0 (API 26)** or higher.

## 🤝 Contribution

Contributions are welcome! Feel free to open issues or submit pull requests on the [GitHub repository](https://github.com/saurabhkundu1/secure-password-manager) to improve security or add features.

## 📄 License

This project is licensed under the **MIT License**. See the `LICENSE` file for details.
