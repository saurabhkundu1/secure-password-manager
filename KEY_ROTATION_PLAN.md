# SecurePass signing key rotation plan

## Current state

The repository is public and has published direct-download APKs (`v1.0.1` and `v1.0.2`). Its release workflow signs with the GitHub Actions secret `SIGNING_KEY`; the Applify F-Droid repository distributes the same APK. The Applify workflow also uses this certificate for repository-index signing, and pins it for Skycast. Treat the private key as compromised. Its public SHA-256 certificate fingerprint is `DE62E0D386DB9B6FC616ADF6573F458AAC8D04F873007C73B8585FBEEF0960255`.

The working copy is fail-closed for releases signed by the exposed certificate. Secure Pass and Skycast retain their original package IDs and use separate replacement app keys; Applify uses a third replacement repository-index key. Existing installs must be uninstalled before installing the same package IDs signed with the new keys. The Applify Pages presentation remains untouched. Public GitHub history, releases, APK files and old signed repository indexes still exist and remain unsafe until an owner-approved migration removes or replaces them.

## Safe migration sequence

1. Keep the established package IDs: `com.applify.securepass` and `com.applify.skycast`. An APK with a new signer cannot update the old install; uninstall the old copy first, then install the same package ID from the refreshed Applify repo.
2. Use separate new keys for Secure Pass, Skycast, and the Applify repository index. Keep private keystores outside Git and make encrypted backups. Use alias `securepass` for the Applify index key. Never send keystores or passwords in chat or commit them.
3. Set Secure Pass secrets `SIGNING_KEY`, `KEYSTORE_PASSWORD`, `KEY_ALIAS=securepass-recovery`, `KEY_PASSWORD`; set `EXPECTED_SIGNER_SHA256` to its new app fingerprint. Set Skycast secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS=skycast-recovery`, `KEY_PASSWORD`, and `PAT_TOKEN`; set `EXPECTED_SKYCAST_SIGNER_SHA256` to its new app fingerprint. Set Applify secrets `REPO_KEYSTORE_BASE64`, `KEYSTORE_PASS`, `KEY_PASS`; set `EXPECTED_SIGNER_SHA256`, `EXPECTED_SKYCAST_SIGNER_SHA256`, and `EXPECTED_REPO_SIGNER_SHA256` to the matching fingerprints. Do not configure recovery package IDs.
4. The next Secure Pass release is `1.0.3` (version code 4); the next Skycast release is `1.0.1` (version code 2). Both release workflows must confirm the expected new signer and original package ID before publishing.
5. Rebuild and sign Applify's indexes with the new repository key, then publish its new fingerprint. The repository URL stays the same; existing Droidify clients may need the repo removed and re-added to trust its changed index signer.
6. Export an encrypted Secure Pass vault backup, uninstall the old Secure Pass, install the new `com.applify.securepass`, and import the backup. Uninstall the old Skycast before installing `com.applify.skycast`; saved places and preferences may need setup again.
7. Publish a clear migration notice before distributing replacement apps. Update GitHub releases, Applify metadata, and all announcements. The current changes only prepare the workspace; they do not alter public repositories, releases, or Git history.
8. After replacement signing is active, rewrite public Git history to remove the JKS blob and coordinate with clone/fork owners. History cleanup cannot erase APKs already downloaded or clones already fetched.

## Android install constraint

Android compares package ID and signing identity for an update. Removing the old package clears its installed signing identity; the replacement can then be freshly installed with the same package ID and new signer. Back up app data first because uninstalling removes local data.

## Release gate

Do not distribute another release until replacement app and repository fingerprints are configured, APK package IDs and signer checks pass, indexes are regenerated, and the encrypted Secure Pass backup has been verified. Do not sign any new release with the exposed private key.
