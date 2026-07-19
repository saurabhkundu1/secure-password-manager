# Implementation Plan - Fix Compilation Errors and Implement Vault Logic

The project is currently failing to build due to `illegal start of expression` errors in `VaultManager.java` and potentially `VaultActivity.java`. These errors are typically caused by methods being declared inside other methods or missing braces. Additionally, `VaultManager.java` is missing several core methods required by the rest of the application.

## User Review Required

> [!IMPORTANT]
> The implementation of `changeMasterCode` will re-encrypt the entire vault using a new key derived from the new code. This operation requires the vault to be currently unlocked or the old code to be provided.

## Proposed Changes

### Data Layer

#### [MODIFY] [VaultManager.java](file:///C:/Users/kundu/AndroidStudioProjects/SecurePass/app/src/main/java/com/applify/securepass/data/VaultManager.java)
- Implement `setupNewVault(String userCode)`: Initializes the vault with a new salt and an empty list of entries.
- Implement `unlock(String userCode)`: Derives the encryption key from the user code and stored salt.
- Implement `loadEntries()`: Decrypts and parses the vault file into a list of `VaultItem`.
- Implement `saveEntries(List<VaultItem> entries)`: Encrypts and saves the list of entries to the vault file.
- Implement `changeMasterCode(String oldCode, String newCode)`: Re-encrypts the vault with a new key derived from `newCode`.
- Fix the class structure by adding the missing closing brace.

---

### UI Layer

#### [MODIFY] [VaultActivity.java](file:///C:/Users/kundu/AndroidStudioProjects/SecurePass/app/src/main/java/com/applify/securepass/VaultActivity.java)
- Move the `setupSwipeToDelete` method outside of `onCreate` to fix the `illegal start of expression` error.
- Ensure all brackets are balanced.

## Verification Plan

### Automated Tests
- Run `TestVault.java` to verify the vault lifecycle (setup, add, load, unlock).
- Command: `./gradlew :app:testDebugUnitTest --tests com.applify.securepass.data.TestVault`

### Manual Verification
- Deploy the app to a device/emulator.
- Create a new vault.
- Add, edit, and delete entries.
- Change the master code in Settings and verify the vault can still be unlocked with the new code.
