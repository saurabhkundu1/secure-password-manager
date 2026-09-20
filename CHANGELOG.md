# Changelog

All notable changes to the **Secure Pass** project will be documented in this file.

## [1.0.0.4] - 2026-09-14

### Added ✨
- **Customizable App Icon**: Explicit option added in Settings to select your preferred App Launcher Icon color independently.
- **Icon Synchronization**: Added a setting to automatically sync the app's internal color palette with your chosen launcher icon color.
- **Custom Swipe Actions**: You can now define what Swiping Right and Swiping Left does in the Vault! Choose between 'Delete', 'Pin to Top (Favorite)', or 'None' via the Settings menu.
- **Swipe Visuals**: Added intuitive background color gradients while swiping cards (Red for Delete, Amber for Pin).

### Improved 🎨
- **Aspect Ratio & Scaling**: Implemented proper dynamic screen ratio parameters (`resizeableActivity`, `max_aspect`) to ensure the app adjusts seamlessly to ultra-tall aspect ratios and varying screen sizes.
- **Image Aspect Ratios**: Added `fitCenter` to ensure the splash screen icons never distort.

## [1.0.0.3] - 2026-09-14

### Added ✨
- **Dynamic Launcher Icons**: Selecting a color palette in Settings now instantly updates the app's official launcher icon on your home screen to match! 
- **Splash Screen Optimization**: Significantly reduced the memory footprint of the high-res app icons to ensure they render smoothly on the system Splash Screen and Android Biometric Prompt dialog without invisible dropouts.
- **Biometric UI Update**: Updated the internal biometric button on the lock screen to use the active colored app logo instead of a generic system 'info' icon.

### Fixed 🐛
- **F-Droid Ghost App**: Removed an invalid `icon` listing in the F-Droid repository that appeared as an empty standalone app.

## [1.0.0.2] - 2026-09-14

### Fixed 🐛
- **UI Contrast**: Improved the contrast of the lock screen number pad buttons in Dark Mode to ensure they remain clearly legible against system backgrounds.
- **Edge-to-Edge Padding**: Added `android:fitsSystemWindows="true"` across main layouts (`activity_main.xml`, `activity_setting.xml`, `activity_vault.xml`, `activity_add_edit.xml`) to prevent the app UI from bleeding into or hiding behind the Android notification status bar.

## [1.0.0.1] - 2026-09-13

### Added ✨
- **Theme-Aware App Icons**: Dynamic light and dark mode icon variants for Classic Blue, Crimson Red, Royal Purple, Forest Green, and Amber Gold.
- **Color Palette Alignment**: App icons and theme colors now dynamically match user selection across all display modes.

### Fixed 🐛
- **F-Droid Integrity Verification**: Configured persistent keystore signing for Applify repository publishing.

## [1.1.0] - 2026-08-02

### Added ✨
- **Offline Update Checker**: Quick access to download the latest version via GitHub Releases without needing app internet permissions.
- **Private Feedback System**: Direct link to submit issues and suggestions on GitHub, keeping developer email hidden and out of local mail history.

### Improved 🎨
- **Minimalist UI Refresh**: Redesigned the app icon to a sleek, modern lock for a more professional visual identity.
- **Support Cleanup**: Streamlined the Settings screen by replacing complex dropdowns with clean, browser-based support links.
- **Contact Update**: Set official support email to `kundusaurabh8@gmail.com` for developer communications.

## [1.0.2] - 2026-08-02

### Changed ⚙️
- **Build System**: Updated Android Gradle Plugin (AGP) to 9.3.1 for improved build stability.

## [1.0.1] - 2026-07-19

### Added ✨
- **Support & Feedback Section**: A dedicated area in Settings for user communication.
- **GitHub Integration**: Direct link to the official repository.

### Improved 🎨
- **Minimalist Icon**: Redesigned the app icon to a clean, bold lock for a modern look.
- **Simplified Support**: Replaced the complex feedback system with a single offline contact button.
- **Privacy Focus**: Maintained 100% offline status while enabling developer contact via system mail app.
- **Developer Info**: Updated contact email to `kundusaurabh8@gmail.com`.

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
