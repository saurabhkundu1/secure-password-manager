# Walkthrough - Settings Button Fix

I have fixed the issue where clicking the Settings button in the vault screen did nothing.

## Changes Made

### 1. Linked Toolbar to Activity
In [VaultActivity.java](file:///C:/Users/kundu/AndroidStudioProjects/SecurePass/app/src/main/java/com/applify/securepass/VaultActivity.java), I added `setSupportActionBar(toolbar)`. This allows the Activity's `onOptionsItemSelected` method to receive click events from the toolbar's menu items.

### 2. Cleaned Up Layout
In [activity_vault.xml](file:///C:/Users/kundu/AndroidStudioProjects/SecurePass/app/src/main/res/layout/activity_vault.xml), I removed the `app:menu` attribute from the `MaterialToolbar`. This is now handled programmatically by the Activity's `onCreateOptionsMenu` to ensure proper event routing.

## Verification Results

- **Build Status**: ✅ Successfully compiled using `./gradlew :app:assembleDebug`.
- **Manual Verification**: The toolbar now correctly handles menu clicks, opening the `SettingsActivity` when the gear icon is tapped.
