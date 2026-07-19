# Fix All Errors and Complete Biometric Integration

The project currently fails to build due to missing symbols in `SettingsActivity.java`. Specifically, `R.layout.activity_settings` is incorrectly named (should be `activity_setting`) and the `BiometricHelper` class is entirely missing. This plan addresses these compilation errors and completes the biometric unlock feature which appears to be partially implemented.

## User Review Required

> [!IMPORTANT]
> The `BiometricHelper` implementation will use the Android Keystore to securely wrap the vault's encryption key. This requires adding the `androidx.biometric:biometric` dependency.

## Proposed Changes

### Build Configuration

#### [MODIFY] [build.gradle](file:///C:/Users/kundu/AndroidStudioProjects/SecurePass/app/build.gradle)
- Add `androidx.biometric:biometric:1.1.0` dependency.

### Source Code

#### [NEW] [BiometricHelper.java](file:///C:/Users/kundu/AndroidStudioProjects/SecurePass/app/src/main/java/com/applify/securepass/BiometricHelper.java)
- Implement `encryptKeyWithBiometric(byte[] key)`:
    - Generates/Retrieves a hardware-backed AES key in Android KeyStore.
    - Encrypts the provided `key` (vault master key) using this Keystore key.
    - Returns a Base64 encoded string containing the IV and ciphertext.
- Implement `decryptKeyWithBiometric(FragmentActivity activity, String encryptedKey, BiometricPrompt.AuthenticationCallback callback)`:
    - Shows the `BiometricPrompt`.
    - Upon successful authentication, decrypts the `encryptedKey` and returns the raw master key.

#### [MODIFY] [SettingsActivity.java](file:///C:/Users/kundu/AndroidStudioProjects/SecurePass/app/src/main/java/com/applify/securepass/SettingsActivity.java)
- Fix layout reference: `R.layout.activity_settings` -> `R.layout.activity_setting`.
- Ensure imports are correct for `BiometricHelper` (if needed, though it will be in the same package).

#### [MODIFY] [MainActivity.java](file:///C:/Users/kundu/AndroidStudioProjects/SecurePass/app/src/main/java/com/applify/securepass/MainActivity.java)
- Add biometric unlock option if `fingerprint_enabled` is true in `SharedPreferences`.
- Display a fingerprint icon or button to trigger `BiometricHelper.decryptKeyWithBiometric`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to verify compilation.
- Run existing unit tests: `./gradlew :app:testDebugUnitTest`.

### Manual Verification
- Deploy to an emulator with biometric support.
- Go to Settings, enable Fingerprint (requires master code).
- Close and reopen the app.
- Verify that biometric unlock is offered and works correctly.
- Verify that changing the master code disables biometrics (as already implemented in `SettingsActivity`).
