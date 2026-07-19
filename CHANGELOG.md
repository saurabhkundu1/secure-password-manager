# Changelog

All notable changes to the **Secure Pass** project will be documented in this file.

## [1.0.0] - 2026-07-19

### Initial Release 🚀
Complete offline password management solution with the following features:

#### 🔐 Security & Core
- **Military-Grade Encryption**: AES-256-GCM authenticated encryption for all vault data.
- **Key Derivation**: Secure PBKDF2 with HmacSHA256 (100,000 iterations).
- **Biometric Unlock**: Integrated fingerprint support with hardware-backed Keystore.
- **Auto-Lock Timer**: Configurable inactivity timer to prevent unauthorized access.
- **Master PIN**: Mandatory 6-digit PIN for primary access.

#### 📁 Data Management
- **Search Bar**: Real-time filtering of accounts by website or username.
- **Favorites**: Ability to pin priority accounts to the top of the vault.
- **Encrypted Backup**: Export/Import vault as encrypted `.txt` files with separate backup passwords.
- **Password Generator**: Customizable tool for generating cryptographically strong passwords.
- **Strength Indicator**: Real-time visual feedback on password complexity.

#### 🎨 Personalization
- **9 Color Palettes**: Dynamic theme engine supporting Teal, Blue, Green, Purple, Red, Orange, Indigo, Pink, and Onyx.
- **Dark Mode**: Complete support for Light, Dark, and System default display modes.
- **Modern UI**: Clean Material 3 style interface.

#### 🛠️ Technical
- **Offline First**: No internet permissions; 100% data privacy.
- **Storage**: Uses private internal storage for all sensitive files.
- **UI Architecture**: Centralized `ThemeHelper` and `BaseLockActivity` for consistent app-wide behavior.
