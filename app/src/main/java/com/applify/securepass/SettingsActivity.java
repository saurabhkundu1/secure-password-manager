package com.applify.securepass;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.applify.securepass.data.VaultManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import javax.crypto.SecretKey;

public class SettingsActivity extends AppCompatActivity {
    private SwitchMaterial switchFingerprint;
    private Button btnChangeCode, btnLockVault;
    private SharedPreferences prefs;
    private VaultManager vaultManager;

    // Theme preferences keys
    private static final String KEY_THEME_MODE = "theme_mode";      // 0=light, 1=dark, 2=system
    private static final String KEY_COLOR_PALETTE = "color_palette"; // 0=teal, 1=blue, 2=green, 3=purple, 4=red

    // Color palette definitions (hex)
    private static final int[] PALETTE_COLORS = {
            0xFF00897B, // Teal (default)
            0xFF1976D2, // Blue
            0xFF388E3C, // Green
            0xFF7B1FA2, // Purple
            0xFFD32F2F  // Red
    };

    private RadioGroup rgThemeMode;
    private RadioButton rbLight, rbDark, rbSystem;
    private LinearLayout llColorPalette;

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
                        e.printStackTrace();
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

    private void buildColorPalette() {
        llColorPalette.removeAllViews();
        int currentPalette = prefs.getInt(KEY_COLOR_PALETTE, 0); // default teal

        for (int i = 0; i < PALETTE_COLORS.length; i++) {
            final int index = i;
            View colorCircle = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) getResources().getDimension(androidx.appcompat.R.dimen.abc_action_bar_default_height_material) / 2,
                    (int) getResources().getDimension(androidx.appcompat.R.dimen.abc_action_bar_default_height_material) / 2
            );
            params.setMargins(12, 0, 12, 0);
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
            if (!newCode.equals(confirmCode)) {
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