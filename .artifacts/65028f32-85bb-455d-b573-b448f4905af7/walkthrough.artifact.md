# Walkthrough - Fixed App Startup Crash

The app was crashing on startup due to a `NullPointerException` when attempting to access `SharedPreferences` in `MainActivity`. This was caused by the `prefs` variable being declared locally in `onCreate`, which shadowed the class field and left it uninitialized.

## Changes Made

### Core Logic Fixes

#### [MainActivity.java](file:///C:/Users/kundu/AndroidStudioProjects/SecurePass/app/src/main/java/com/applify/securepass/MainActivity.java)
- Removed the local `SharedPreferences` type declaration in `onCreate`.
- Ensured the class-level `prefs` field is correctly initialized at the beginning of `onCreate`.
- Removed a redundant initialization later in the method.

#### [SettingsActivity.java](file:///C:/Users/kundu/AndroidStudioProjects/SecurePass/app/src/main/java/com/applify/securepass/SettingsActivity.java)
- Moved the initialization of the `prefs` field to the very top of `onCreate`, before `applySavedTheme()` is called. This ensures that theme settings can be correctly read from preferences during activity creation.

## Verification Results

### Automated Tests
- Build was successful: `./gradlew :app:assembleDebug`
- Successfully deployed the app to the device.

### Manual Verification
- Verified via Logcat that `MainActivity` starts and successfully transitions to `VaultActivity` (or `SettingsActivity` depending on state) without crashing.
- Confirmed that the `NullPointerException` in `checkBiometricStatus()` is no longer present.
- Screen captures confirm the app is reaching its intended UI states.
