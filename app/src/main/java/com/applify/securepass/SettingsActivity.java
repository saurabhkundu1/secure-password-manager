package com.applify.securepass;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.util.Log;
import android.widget.GridLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.applify.securepass.crypto.CryptoManager;
import com.applify.securepass.data.VaultItem;
import com.applify.securepass.data.VaultManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import javax.crypto.SecretKey;

public class SettingsActivity extends BaseLockActivity {
    private static final String TAG = "SettingsActivity";
    private SwitchMaterial switchFingerprint;
    private Button btnChangeCode, btnLockVault, btnExport, btnImport, btnGithub, btnCheckUpdates, btnSubmitFeedback;
    private SharedPreferences prefs;
    private VaultManager vaultManager;

    private Spinner spinnerAutoLock;

    private final ActivityResultLauncher<String> createDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/plain"), this::onBackupFileCreated);

    private final ActivityResultLauncher<String[]> openDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onBackupFileOpened);

    // Theme preferences keys
    private static final String KEY_THEME_MODE = "theme_mode";      // 0=light, 1=dark, 2=system
    private static final String KEY_COLOR_PALETTE = "color_palette"; // 0=teal, 1=blue, 2=green, 3=purple, 4=red
    private static final String KEY_AUTO_LOCK = "auto_lock_time";

    // Auto-lock values in ms
    private static final long[] AUTO_LOCK_VALUES = {
            0,              // Never
            60 * 1000,      // 1 minute
            5 * 60 * 1000,  // 5 minutes
            15 * 60 * 1000, // 15 minutes
            30 * 60 * 1000  // 30 minutes
    };

    private RadioGroup rgThemeMode;
    private RadioButton rbLight, rbDark, rbSystem;
    private GridLayout llColorPalette;

    // Color palette definitions (hex)
    private static final int[] PALETTE_COLORS = {
            0xFF00897B, // Teal (default)
            0xFF1976D2, // Blue
            0xFF388E3C, // Green
            0xFF7B1FA2, // Purple
            0xFFD32F2F, // Red
            0xFFF57C00, // Orange
            0xFF3F51B5, // Indigo
            0xFFD81B60, // Pink
            0xFF212121, // Onyx
            0xFFFBC02D, // Yellow
            0xFF00BCD4, // Cyan
            0xFF795548, // Brown
            0xFF9E9E9E  // Grey
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        prefs = getSharedPreferences("secure_pass_prefs", MODE_PRIVATE);
        vaultManager = new VaultManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        switchFingerprint = findViewById(R.id.switchFingerprint);
        TextView tvLastCodeTime = findViewById(R.id.tvLastCodeTime);
        btnChangeCode = findViewById(R.id.btnChangeCode);
        btnLockVault = findViewById(R.id.btnLockVault);

        // Theme controls
        rgThemeMode = findViewById(R.id.rgThemeMode);
        rbLight = findViewById(R.id.rbLight);
        rbDark = findViewById(R.id.rbDark);
        rbSystem = findViewById(R.id.rbSystem);
        llColorPalette = findViewById(R.id.llColorPalette);
        spinnerAutoLock = findViewById(R.id.spinnerAutoLock);
        btnExport = findViewById(R.id.btnExport);
        btnImport = findViewById(R.id.btnImport);
        btnGithub = findViewById(R.id.btnGithub);
        btnCheckUpdates = findViewById(R.id.btnCheckUpdates);
        btnSubmitFeedback = findViewById(R.id.btnSubmitFeedback);

        // Set initial switch state
        boolean fingerprintEnabled = prefs.getBoolean("fingerprint_enabled", false);
        switchFingerprint.setChecked(fingerprintEnabled);

        // Update last code time
        long lastTime = prefs.getLong("last_code_time", 0);
        if (lastTime > 0) {
            tvLastCodeTime.setText("Last code entry: " +
                    DateUtils.getRelativeTimeSpanString(lastTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
        }

        // Restore theme selections
        restoreThemeSettings();
        setupAutoLockSpinner();

        // Fingerprint toggle listener
        switchFingerprint.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !fingerprintEnabled) {
                showMasterCodePrompt("enable fingerprint", () -> {
                    try {
                        if (!vaultManager.isUnlocked() && VaultManager.getGlobalKey() == null) {
                            Toast.makeText(this, "Vault not unlocked", Toast.LENGTH_SHORT).show();
                            switchFingerprint.setChecked(false);
                            return;
                        }
                        SecretKey key = vaultManager.getCurrentKey();
                        if (key == null) {
                            Toast.makeText(this, "Key not available", Toast.LENGTH_SHORT).show();
                            switchFingerprint.setChecked(false);
                            return;
                        }
                        String encryptedKey = BiometricHelper.encryptKeyWithBiometric(key.getEncoded());
                        prefs.edit()
                                .putBoolean("fingerprint_enabled", true)
                                .putString("encrypted_vault_key", encryptedKey)
                                .putLong("last_code_time", System.currentTimeMillis())
                                .apply();
                        Toast.makeText(this, "Fingerprint enabled", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Fingerprint setup failed", e);
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        switchFingerprint.setChecked(false);
                    }
                });
            } else if (!isChecked && fingerprintEnabled) {
                prefs.edit()
                        .putBoolean("fingerprint_enabled", false)
                        .remove("encrypted_vault_key")
                        .apply();
                Toast.makeText(this, "Fingerprint disabled", Toast.LENGTH_SHORT).show();
            }
        });

        // Theme mode listener
        rgThemeMode.setOnCheckedChangeListener((group, checkedId) -> {
            int mode;
            if (checkedId == R.id.rbLight) {
                mode = 0;
            } else if (checkedId == R.id.rbDark) {
                mode = 1;
            } else {
                mode = 2; // system
            }
            prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
            // Apply theme change immediately
            ThemeHelper.applyThemeMode(mode);
            // Restart activity to refresh colors fully
            recreate();
        });

        // Build color palette buttons
        buildColorPalette();

        btnChangeCode.setOnClickListener(v -> showChangeCodeDialog());
        btnExport.setOnClickListener(v -> promptBackupPassword());
        btnImport.setOnClickListener(v -> openDocumentLauncher.launch(new String[]{"text/plain"}));
        
        btnGithub.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/saurabhkundu1/secure-password-manager"));
            startActivity(intent);
        });

        btnCheckUpdates.setOnClickListener(v -> {
            // Note: This is an offline-friendly link to the releases page. 
            // It does not perform any background updates, complying with F-Droid policies.
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/saurabhkundu1/secure-password-manager/releases/latest"));
            startActivity(intent);
        });

        btnSubmitFeedback.setOnClickListener(v -> {
            // Replace the URL below with your actual Google Form or website contact form link
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/saurabhkundu1/secure-password-manager/issues"));
            startActivity(intent);
        });

        btnLockVault.setOnClickListener(v -> {
            VaultManager.clearGlobalKey();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void restoreThemeSettings() {
        int themeMode = prefs.getInt(KEY_THEME_MODE, 2); // default system
        if (themeMode == 0) {
            rbLight.setChecked(true);
        } else if (themeMode == 1) {
            rbDark.setChecked(true);
        } else {
            rbSystem.setChecked(true);
        }

        // Color palette restoration is handled in buildColorPalette()
    }

    private void setupAutoLockSpinner() {
        String[] options = {"Never", "1 minute", "5 minutes", "15 minutes", "30 minutes"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAutoLock.setAdapter(adapter);

        long currentTime = prefs.getLong(KEY_AUTO_LOCK, 0);
        for (int i = 0; i < AUTO_LOCK_VALUES.length; i++) {
            if (java.util.Objects.equals(AUTO_LOCK_VALUES[i], currentTime)) {
                spinnerAutoLock.setSelection(i);
                break;
            }
        }

        spinnerAutoLock.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putLong(KEY_AUTO_LOCK, AUTO_LOCK_VALUES[position]).apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void buildColorPalette() {
        llColorPalette.removeAllViews();
        int currentPalette = prefs.getInt(KEY_COLOR_PALETTE, 0); // default teal

        for (int i = 0; i < PALETTE_COLORS.length; i++) {
            final int index = i;
            View colorCircle = new View(this);
            int size = (int) getResources().getDimension(androidx.appcompat.R.dimen.abc_action_bar_default_height_material) / 2;
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(16, 16, 16, 16);
            colorCircle.setLayoutParams(params);
            colorCircle.setBackgroundColor(PALETTE_COLORS[i]);
            if (i == currentPalette) {
                // Add a white border to indicate selection
                colorCircle.setBackgroundResource(androidx.appcompat.R.drawable.abc_btn_colored_material);
                // Actually a simpler way: set a ring
                // We'll just draw a border using ShapeDrawable later, but for now we'll use a simple indicator:
                colorCircle.setAlpha(1.0f);
            } else {
                colorCircle.setAlpha(0.5f);
            }

            colorCircle.setOnClickListener(v -> {
                prefs.edit().putInt(KEY_COLOR_PALETTE, index).apply();
                // Rebuild to update selection
                buildColorPalette();
                // Apply the new palette by restarting the activity (or we can call recreate())
                recreate();
            });
            llColorPalette.addView(colorCircle);
        }
    }

    // ---------- Backup & Restore Logic ----------

    private String tempBackupPassword;
    private void promptBackupPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Backup Password");
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("Continue", (dialog, which) -> {
            tempBackupPassword = input.getText().toString();
            createDocumentLauncher.launch("secure_pass_backup.txt");
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void onBackupFileCreated(Uri uri) {
        if (uri == null || tempBackupPassword == null) return;
        try {
            // 1. Prepare data
            List<VaultItem> entries = vaultManager.loadEntries();
            String json = new Gson().toJson(entries);

            // 2. Encrypt
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            SecretKey key = CryptoManager.deriveKey(tempBackupPassword, salt);
            
            // CryptoManager.encrypt returns Base64(IV + Ciphertext)
            String encryptedIVData = CryptoManager.encrypt(json, key);
            byte[] ivData = Base64.getDecoder().decode(encryptedIVData);

            // 3. Combine: salt + IV + ciphertext
            ByteBuffer buffer = ByteBuffer.allocate(salt.length + ivData.length);
            buffer.put(salt);
            buffer.put(ivData);
            
            String finalBase64 = Base64.getEncoder().encodeToString(buffer.array());

            // 4. Write to file
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                if (os != null) {
                    os.write(finalBase64.getBytes(StandardCharsets.UTF_8));
                }
            }
            Toast.makeText(this, "Backup exported successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Export failed", e);
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            tempBackupPassword = null;
        }
    }

    private void onBackupFileOpened(Uri uri) {
        if (uri == null) return;
        getPassword(password -> handleImport(uri, password));
    }

    private interface PasswordCallback { void onPassword(String password); }
    private void getPassword(PasswordCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Backup Password");
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> callback.onPassword(input.getText().toString()));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Refined Import flow
    private void handleImport(Uri uri, String password) {
        try {
            // 1. Read file
            byte[] fileBytes;
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) throw new Exception("Could not open file");
                // Read all bytes using a loop or available bytes if small enough
                int size = is.available();
                byte[] encodedBytes = new byte[size];
                int read = is.read(encodedBytes);
                if (read <= 0) throw new Exception("File is empty or could not be read");
                fileBytes = Base64.getDecoder().decode(new String(encodedBytes, StandardCharsets.UTF_8));
            }

            // 2. Extract salt and data
            ByteBuffer buffer = ByteBuffer.wrap(fileBytes);
            byte[] salt = new byte[16];
            buffer.get(salt);
            byte[] ivData = new byte[buffer.remaining()];
            buffer.get(ivData);

            // 3. Decrypt
            SecretKey key = CryptoManager.deriveKey(password, salt);
            String encryptedIVData = Base64.getEncoder().encodeToString(ivData);
            String json = CryptoManager.decrypt(encryptedIVData, key);

            // 4. Parse
            List<VaultItem> importedEntries = new Gson().fromJson(json, new TypeToken<List<VaultItem>>(){}.getType());

            // 5. Merge or Replace
            showMergeDialog(importedEntries);

        } catch (Exception e) {
            Log.e(TAG, "Import failed", e);
            Toast.makeText(this, "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showMergeDialog(List<VaultItem> importedEntries) {
        new AlertDialog.Builder(this)
                .setTitle("Restore Backup")
                .setMessage("Found " + importedEntries.size() + " entries. Do you want to merge them with current entries or replace everything?")
                .setPositiveButton("Merge", (dialog, which) -> {
                    try {
                        vaultManager.mergeEntries(importedEntries);
                        Toast.makeText(this, "Merged successfully", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Merge failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Replace", (dialog, which) -> {
                    try {
                        vaultManager.saveEntries(importedEntries);
                        Toast.makeText(this, "Replaced successfully", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Replace failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ============ (existing methods unchanged below) ============
    private void showMasterCodePrompt(String reason, Runnable onSuccess) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Master Code");
        builder.setMessage("To " + reason + ", enter your 6‑digit code.");
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String code = input.getText().toString();
            try {
                vaultManager.unlock(code);
                onSuccess.run();
            } catch (Exception e) {
                Toast.makeText(SettingsActivity.this, "Wrong code", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showChangeCodeDialog() {
        AlertDialog.Builder oldCodeBuilder = new AlertDialog.Builder(this);
        oldCodeBuilder.setTitle("Current Code");
        oldCodeBuilder.setMessage("Enter your current 6‑digit code.");
        final android.widget.EditText oldInput = new android.widget.EditText(this);
        oldInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        oldCodeBuilder.setView(oldInput);
        oldCodeBuilder.setPositiveButton("Next", (dialog, which) -> {
            String oldCode = oldInput.getText().toString();
            try {
                vaultManager.unlock(oldCode);
                showNewCodeDialog();
            } catch (Exception e) {
                Toast.makeText(SettingsActivity.this, "Wrong current code", Toast.LENGTH_SHORT).show();
            }
        });
        oldCodeBuilder.setNegativeButton("Cancel", null);
        oldCodeBuilder.show();
    }

    private void showNewCodeDialog() {
        AlertDialog.Builder newCodeBuilder = new AlertDialog.Builder(this);
        newCodeBuilder.setTitle("New Code");
        newCodeBuilder.setMessage("Enter a new 6‑digit code.");
        final android.widget.EditText newInput = new android.widget.EditText(this);
        newInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        newCodeBuilder.setView(newInput);
        newCodeBuilder.setPositiveButton("Next", (dialog, which) -> {
            String newCode = newInput.getText().toString();
            if (newCode.length() != 6) {
                Toast.makeText(SettingsActivity.this, "Code must be 6 digits", Toast.LENGTH_SHORT).show();
                return;
            }
            showConfirmNewCodeDialog(newCode);
        });
        newCodeBuilder.setNegativeButton("Cancel", null);
        newCodeBuilder.show();
    }

    private void showConfirmNewCodeDialog(String newCode) {
        AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(this);
        confirmBuilder.setTitle("Confirm New Code");
        confirmBuilder.setMessage("Re‑enter the new 6‑digit code.");
        final android.widget.EditText confirmInput = new android.widget.EditText(this);
        confirmInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        confirmBuilder.setView(confirmInput);
        confirmBuilder.setPositiveButton("Change", (dialog, which) -> {
            String confirmCode = confirmInput.getText().toString();
            if (!java.util.Objects.equals(newCode, confirmCode)) {
                Toast.makeText(SettingsActivity.this, "Codes do not match", Toast.LENGTH_SHORT).show();
                return;
            }
            showOldCodeForChange(newCode);
        });
        confirmBuilder.setNegativeButton("Cancel", null);
        confirmBuilder.show();
    }

    private void showOldCodeForChange(String newCode) {
        AlertDialog.Builder oldBuilder = new AlertDialog.Builder(this);
        oldBuilder.setTitle("Current Code");
        oldBuilder.setMessage("Enter your current 6‑digit code to confirm change.");
        final android.widget.EditText oldInput = new android.widget.EditText(this);
        oldInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        oldBuilder.setView(oldInput);
        oldBuilder.setPositiveButton("Confirm", (dialog, which) -> {
            String oldCode = oldInput.getText().toString();
            try {
                vaultManager.changeMasterCode(oldCode, newCode);
                prefs.edit().putBoolean("fingerprint_enabled", false).remove("encrypted_vault_key").apply();
                switchFingerprint.setChecked(false);
                Toast.makeText(SettingsActivity.this, "Master code changed", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(SettingsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        oldBuilder.setNegativeButton("Cancel", null);
        oldBuilder.show();
    }
}