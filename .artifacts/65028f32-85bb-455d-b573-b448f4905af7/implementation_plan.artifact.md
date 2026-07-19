# Fix App Startup Crash

The app is crashing on startup due to a `NullPointerException` in `MainActivity`. This is caused by variable shadowing: a local `SharedPreferences` variable is declared and initialized, but the class-level field remains null. When `checkBiometricStatus()` is called, it attempts to use the null field.

## Proposed Changes

### [Component Name]

#### [MODIFY] [MainActivity.java](file:///C:/Users/kundu/AndroidStudioProjects/SecurePass/app/src/main/java/com/applify/securepass/MainActivity.java)
- Remove the local type declaration for `prefs` in `onCreate`.
- Remove the redundant initialization of `prefs` later in `onCreate`.
- This ensures the class field `prefs` is properly initialized and used.

#### [MODIFY] [SettingsActivity.java](file:///C:/Users/kundu/AndroidStudioProjects/SecurePass/app/src/main/java/com/applify/securepass/SettingsActivity.java)
- Initialize `prefs` before calling `applySavedTheme()` in `onCreate`.
- This ensures the saved theme is correctly retrieved from preferences.

## Verification Plan

### Automated Tests
- Build the project using `./gradlew :app:assembleDebug`.
- Run the app and verify it no longer crashes on start.

### Manual Verification
- Deploy to a device/emulator.
- Observe that the app opens to the PIN setup or entry screen without crashing.
- Check logcat to ensure no `NullPointerException` is reported for `com.applify.securepass`.
