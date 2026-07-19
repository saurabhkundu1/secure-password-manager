package com.applify.securepass;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.applify.securepass.data.VaultManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import javax.crypto.SecretKey;

public class SettingsActivity extends AppCompatActivity {
    private SwitchMaterial switchFingerprint;
    private TextView tvLastCodeTime;
    private Button btnChangeCode, btnLockVault;
    private SharedPreferences prefs;
    private VaultManager vaultManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        prefs = getSharedPreferences("secure_pass_prefs", MODE_PRIVATE);
        vaultManager = new VaultManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        switchFingerprint = findViewById(R.id.switchFingerprint);
        tvLastCodeTime = findViewById(R.id.tvLastCodeTime);
        btnChangeCode = findViewById(R.id.btnChangeCode);
        btnLockVault = findViewById(R.id.btnLockVault);

        // Set initial switch state
        boolean fingerprintEnabled = prefs.getBoolean("fingerprint_enabled", false);
        switchFingerprint.setChecked(fingerprintEnabled);

        // Update last code time
        long lastTime = prefs.getLong("last_code_time", 0);
        if (lastTime > 0) {
            tvLastCodeTime.setText("Last code entry: " +
                    DateUtils.getRelativeTimeSpanString(lastTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
        }

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

        btnChangeCode.setOnClickListener(v -> showChangeCodeDialog());
        btnLockVault.setOnClickListener(v -> {
            VaultManager.clearGlobalKey();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

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
            try {
                // oldCode is needed, but we haven't stored it. We'll adjust: after verifying old code,
                // we should store it temporarily. For simplicity, we'll re-prompt the old code inside changeMasterCode.
                // However, VaultManager.changeMasterCode needs both old and new. We'll get old code again via dialog.
                // Quick fix: show a final dialog for old code, then call changeMasterCode.
                showOldCodeForChange(newCode);
            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
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
                // Disable fingerprint because key changed
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