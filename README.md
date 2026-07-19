# Secure Pass 🔐

An offline, high-security encrypted password manager for Android.  
Store your credentials safely in a local, encrypted vault using **AES-256-GCM**, unlocked via a **6-digit PIN** or **Biometrics**.

## 🚀 Features

- 🔒 **Total Privacy** – 100% offline. No internet permissions required. Your data never leaves your device.
- 🛡️ **Military-Grade Encryption** – Uses **AES-256-GCM** to encrypt your vault. The encryption key is derived from your PIN using **PBKDF2** with HmacSHA256 and a unique salt.
- 🔢 **Secure Access** – Locked behind a mandatory 6-digit master code.
- 🖐️ **Biometric Integration** – Fingerprint support for convenience. Requires PIN entry every 24 hours to ensure security and prevent code amnesia.
- 🎨 **Personalized UI** – Extensive theme support:
  - **9 Color Palettes**: Teal, Blue, Green, Purple, Red, Orange, Indigo, Pink, and Onyx.
  - **Display Modes**: Full support for Light, Dark, and System default modes.
- 🎲 **Password Generator** – Built-in tool to generate cryptographically strong passwords with customizable length and character sets (A-Z, a-z, 0-9, symbols).
- 📝 **Detailed Entries** – Store website URLs, usernames, passwords, and secure notes for every account.
- 📁 **Transparent Storage** – The vault is stored as a Base64-encoded encrypted text file (`vault.txt`) in private internal storage.

## 📱 Screenshots
_(Updated UI screenshots coming soon!)_

## 🛠️ Technical Details

- **Encryption**: AES-256-GCM (Authenticated Encryption).
- **KDF**: PBKDF2 with HmacSHA256 (100,000 iterations).
- **UI Framework**: Material Components (Material 3 style) with dynamic color attributes.
- **Biometrics**: Android BiometricPrompt API with hardware-backed Keystore integration.

## 📦 Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/saurabhkundu1/secure-password-manager.git
   ```
2. **Open in Android Studio**: Import the project.
3. **Sync Gradle**: Ensure all dependencies are downloaded.
4. **Build & Run**: Deploy to a device or emulator running **Android 8.0 (API 26)** or higher.

## 🤝 Contribution

Contributions are welcome! Feel free to open issues or submit pull requests to improve security or add features.

## 📄 License

This project is licensed under the **MIT License**.
