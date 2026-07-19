package com.applify.securepass;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.applify.securepass.data.VaultItem;
import com.applify.securepass.data.VaultManager;
import com.google.android.material.textfield.TextInputEditText;

public class AddEditActivity extends BaseLockActivity {

    private static final String TAG = "AddEditActivity";
    private TextInputEditText etWebsite, etUsername, etPassword, etNotes;
    private ProgressBar progressStrength;
    private TextView tvStrengthText;
    private VaultManager vaultManager;
    private String userCode;
    private String editingItemId = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        vaultManager = new VaultManager(this);
        userCode = getIntent().getStringExtra("USER_CODE");

        etWebsite = findViewById(R.id.etWebsite);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etNotes = findViewById(R.id.etNotes);
        progressStrength = findViewById(R.id.progressStrength);
        tvStrengthText = findViewById(R.id.tvStrengthText);
        Button btnGenerate = findViewById(R.id.btnGeneratePassword);
        Button btnSave = findViewById(R.id.btnSave);

        if (getIntent().hasExtra("ITEM_ID")) {
            editingItemId = getIntent().getStringExtra("ITEM_ID");
            loadExistingItem(editingItemId);
        }

        // Initial strength check
        updatePasswordStrength(etPassword.getText() != null ? etPassword.getText().toString() : "");

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnGenerate.setOnClickListener(v -> showPasswordGeneratorDialog());
        btnSave.setOnClickListener(v -> saveEntry());
    }

    private void loadExistingItem(String itemId) {
        try {
            if (!vaultManager.isUnlocked() && userCode != null) {
                vaultManager.unlock(userCode);
            }
            for (VaultItem item : vaultManager.loadEntries()) {
                if (item.id.equals(itemId)) {
                    etWebsite.setText(item.website);
                    etUsername.setText(item.username);
                    etPassword.setText(item.password);
                    etNotes.setText(item.notes);
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading existing item", e);
        }
    }

    private void saveEntry() {
        String website = etWebsite.getText() != null ? etWebsite.getText().toString().trim() : "";
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

        if (website.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (!vaultManager.isUnlocked() && userCode != null) {
                vaultManager.unlock(userCode);
            }
            java.util.List<VaultItem> entries = vaultManager.loadEntries();

            if (editingItemId != null) {
                for (VaultItem item : entries) {
                    if (item.id.equals(editingItemId)) {
                        item.website = website;
                        item.username = username;
                        item.password = password;
                        item.notes = notes;
                        item.lastChanged = System.currentTimeMillis();
                        break;
                    }
                }
            } else {
                entries.add(new VaultItem(website, username, password, notes));
            }

            vaultManager.saveEntries(entries);
            Toast.makeText(this, "Entry saved!", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error saving entry", e);
        }
    }

    private void showPasswordGeneratorDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Generate Password");
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_password_generator, null);
        builder.setView(dialogView);

        TextInputEditText etLength = dialogView.findViewById(R.id.etLength);
        CheckBox cbUpper = dialogView.findViewById(R.id.cbUpper);
        CheckBox cbLower = dialogView.findViewById(R.id.cbLower);
        CheckBox cbDigits = dialogView.findViewById(R.id.cbDigits);
        CheckBox cbSymbols = dialogView.findViewById(R.id.cbSymbols);

        builder.setPositiveButton("Generate", (dialog, which) -> {
            int length;
            try {
                String lengthStr = etLength.getText() != null ? etLength.getText().toString() : "16";
                length = Integer.parseInt(lengthStr);
            } catch (NumberFormatException e) {
                length = 16;
            }
            String pwd = PasswordGenerator.generate(length,
                    cbUpper.isChecked(), cbLower.isChecked(),
                    cbDigits.isChecked(), cbSymbols.isChecked());
            etPassword.setText(pwd);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            progressStrength.setProgress(0);
            tvStrengthText.setText("Weak");
            tvStrengthText.setTextColor(0xFFD32F2F);
            progressStrength.setProgressTintList(ColorStateList.valueOf(0xFFD32F2F));
            return;
        }

        int score = 0;
        // Length (max 40 pts)
        score += Math.min(password.length() * 5, 40);

        // Varieties (15 pts each)
        if (password.matches(".*[A-Z].*")) score += 15;
        if (password.matches(".*[a-z].*")) score += 15;
        if (password.matches(".*[0-9].*")) score += 15;
        if (password.matches(".*[^A-Za-z0-9].*")) score += 15;

        progressStrength.setProgress(score);

        if (score <= 40) {
            tvStrengthText.setText("Weak");
            tvStrengthText.setTextColor(0xFFD32F2F); // palette_red_primary
            progressStrength.setProgressTintList(ColorStateList.valueOf(0xFFD32F2F));
        } else if (score <= 70) {
            tvStrengthText.setText("Fair");
            tvStrengthText.setTextColor(0xFFF57C00); // palette_orange_primary
            progressStrength.setProgressTintList(ColorStateList.valueOf(0xFFF57C00));
        } else {
            tvStrengthText.setText("Strong");
            tvStrengthText.setTextColor(0xFF388E3C); // palette_green_primary
            progressStrength.setProgressTintList(ColorStateList.valueOf(0xFF388E3C));
        }
    }
}