package com.applify.securepass;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.GridLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.applify.securepass.data.VaultManager;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

    private VaultManager vaultManager;
    private boolean isSetupMode = false;
    private String enteredCode = "";
    private String pendingCode = ""; // For confirmation during setup
    private TextView tvInstruction;
    private TextView tvError;
    private LinearLayout pinDotsContainer;
    private ImageView ivLockIcon;

    // Number pad buttons
    private Button btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9;
    private Button btnDelete, btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vaultManager = new VaultManager(this);

        // Bind UI elements
        tvInstruction = findViewById(R.id.tvInstruction);
        tvError = findViewById(R.id.tvError);
        pinDotsContainer = findViewById(R.id.pinDotsContainer);
        ivLockIcon = findViewById(R.id.ivLockIcon);

        // Bind number buttons
        btn0 = findViewById(R.id.btn0);
        btn1 = findViewById(R.id.btn1);
        btn2 = findViewById(R.id.btn2);
        btn3 = findViewById(R.id.btn3);
        btn4 = findViewById(R.id.btn4);
        btn5 = findViewById(R.id.btn5);
        btn6 = findViewById(R.id.btn6);
        btn7 = findViewById(R.id.btn7);
        btn8 = findViewById(R.id.btn8);
        btn9 = findViewById(R.id.btn9);
        btnDelete = findViewById(R.id.btnDelete);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Set click listeners
        setNumberPadListeners();

        // Check if vault already exists
        if (isVaultSetup()) {
            isSetupMode = false;
            tvInstruction.setText("Enter your 6-digit code");
        } else {
            isSetupMode = true;
            tvInstruction.setText("Create a 6-digit code");
        }

        // Initialize dot display
        updateDotDisplay();
    }

    private boolean isVaultSetup() {
        // If the salt file exists, we consider the vault already set up.
        java.io.File saltFile = new java.io.File(getFilesDir(), "vault.salt");
        return saltFile.exists();
    }

    private void setNumberPadListeners() {
        View.OnClickListener numberListener = v -> {
            if (enteredCode.length() < 6) {
                enteredCode += ((Button) v).getText().toString();
                updateDotDisplay();
                tvError.setVisibility(View.GONE);
            }
        };

        btn0.setOnClickListener(numberListener);
        btn1.setOnClickListener(numberListener);
        btn2.setOnClickListener(numberListener);
        btn3.setOnClickListener(numberListener);
        btn4.setOnClickListener(numberListener);
        btn5.setOnClickListener(numberListener);
        btn6.setOnClickListener(numberListener);
        btn7.setOnClickListener(numberListener);
        btn8.setOnClickListener(numberListener);
        btn9.setOnClickListener(numberListener);

        btnDelete.setOnClickListener(v -> {
            if (enteredCode.length() > 0) {
                enteredCode = enteredCode.substring(0, enteredCode.length() - 1);
                updateDotDisplay();
                tvError.setVisibility(View.GONE);
            }
        });

        btnSubmit.setOnClickListener(v -> {
            if (enteredCode.length() != 6) {
                tvError.setText("Please enter exactly 6 digits.");
                tvError.setVisibility(View.VISIBLE);
                return;
            }
            processCode(enteredCode);
        });
    }

    private void updateDotDisplay() {
        pinDotsContainer.removeAllViews();
        for (int i = 0; i < 6; i++) {
            TextView dot = new TextView(this);
            dot.setText(i < enteredCode.length() ? "●" : "○");
            dot.setTextSize(24);
            dot.setTextColor(getResources().getColor(R.color.teal_700));
            dot.setPadding(12, 0, 12, 0);
            pinDotsContainer.addView(dot);
        }
    }

    private void processCode(String code) {
        try {
            if (isSetupMode) {
                // Setup mode: first entry is the code, confirm it
                if (pendingCode.isEmpty()) {
                    // First time entering new code
                    pendingCode = code;
                    enteredCode = "";
                    updateDotDisplay();
                    tvInstruction.setText("Confirm your 6-digit code");
                } else {
                    // Confirmation
                    if (code.equals(pendingCode)) {
                        vaultManager.setupNewVault(code);
                        isSetupMode = false;
                        tvInstruction.setText("Vault created! Now unlock.");
                        pendingCode = "";
                        enteredCode = "";
                        updateDotDisplay();
                        Snackbar.make(findViewById(android.R.id.content), "Secure Pass ready!", Snackbar.LENGTH_SHORT).show();
                    } else {
                        tvError.setText("Codes do not match. Try again.");
                        tvError.setVisibility(View.VISIBLE);
                        pendingCode = "";
                        enteredCode = "";
                        updateDotDisplay();
                        tvInstruction.setText("Create a 6-digit code");
                    }
                }
            } else {
                // Unlock mode
                vaultManager.unlock(code);
                // Success! Move to vault list (next step)
                Snackbar.make(findViewById(android.R.id.content), "Unlocked successfully!", Snackbar.LENGTH_SHORT).show();
                // TODO: Start VaultActivity (we'll create it soon)
                // For now, we'll just reset the UI
                enteredCode = "";
                updateDotDisplay();
                tvInstruction.setText("Unlocked! (Vault screen coming soon)");
                ivLockIcon.setImageResource(android.R.drawable.ic_lock_idle_lock); // temporary
            }
        } catch (Exception e) {
            tvError.setText("Wrong code. Please try again.");
            tvError.setVisibility(View.VISIBLE);
            enteredCode = "";
            updateDotDisplay();
        }
    }
}