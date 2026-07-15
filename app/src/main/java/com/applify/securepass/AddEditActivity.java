package com.applify.securepass;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.applify.securepass.data.VaultItem;
import com.applify.securepass.data.VaultManager;
import com.google.android.material.textfield.TextInputEditText;

public class AddEditActivity extends AppCompatActivity {

    private TextInputEditText etWebsite, etUsername, etPassword, etNotes;
    private VaultManager vaultManager;
    private String userCode;
    private String editingItemId = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        vaultManager = new VaultManager(this);
        userCode = getIntent().getStringExtra("USER_CODE");

        etWebsite = findViewById(R.id.etWebsite);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etNotes = findViewById(R.id.etNotes);
        Button btnGenerate = findViewById(R.id.btnGeneratePassword);
        Button btnSave = findViewById(R.id.btnSave);

        if (getIntent().hasExtra("ITEM_ID")) {
            editingItemId = getIntent().getStringExtra("ITEM_ID");
            loadExistingItem(editingItemId);
        }

        btnGenerate.setOnClickListener(v -> showPasswordGeneratorDialog());
        btnSave.setOnClickListener(v -> saveEntry());
    }

    private void loadExistingItem(String itemId) {
        try {
            vaultManager.unlock(userCode);
            for (VaultItem item : vaultManager.loadEntries()) {
                if (item.id.equals(itemId)) {
                    etWebsite.setText(item.website);
                    etUsername.setText(item.username);
                    etPassword.setText(item.password);
                    etNotes.setText(item.notes);
                    break;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveEntry() {
        String website = etWebsite.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();
        String notes = etNotes.getText().toString().trim();

        if (website.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            vaultManager.unlock(userCode);
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
            e.printStackTrace();
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
            try { length = Integer.parseInt(etLength.getText().toString()); }
            catch (NumberFormatException e) { length = 16; }
            String pwd = PasswordGenerator.generate(length,
                    cbUpper.isChecked(), cbLower.isChecked(),
                    cbDigits.isChecked(), cbSymbols.isChecked());
            etPassword.setText(pwd);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}