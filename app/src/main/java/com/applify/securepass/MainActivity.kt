package com.applify.securepass

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.applify.securepass.data.VaultManager
import com.google.android.material.snackbar.Snackbar
import java.io.File
import javax.crypto.spec.SecretKeySpec

class MainActivity : AppCompatActivity() {

    private lateinit var vaultManager: VaultManager
    private var isSetupMode = false
    private var enteredCode = ""
    private var pendingCode = "" // For confirmation during setup
    private lateinit var tvInstruction: TextView
    private lateinit var tvError: TextView
    private lateinit var pinDotsContainer: LinearLayout

    // Number pad buttons
    private lateinit var btn0: Button
    private lateinit var btn1: Button
    private lateinit var btn2: Button
    private lateinit var btn3: Button
    private lateinit var btn4: Button
    private lateinit var btn5: Button
    private lateinit var btn6: Button
    private lateinit var btn7: Button
    private lateinit var btn8: Button
    private lateinit var btn9: Button
    private lateinit var btnDelete: Button
    private lateinit var btnSubmit: Button
    private var btnBiometric: ImageButton? = null
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("secure_pass_prefs", MODE_PRIVATE)
        vaultManager = VaultManager(this)

        // Bind UI elements
        val ivLockIcon: ImageView? = findViewById(R.id.ivLockIcon)
        ivLockIcon?.setImageResource(ThemeHelper.getCurrentThemeIconResId(this))

        tvInstruction = findViewById(R.id.tvInstruction)
        tvError = findViewById(R.id.tvError)
        pinDotsContainer = findViewById(R.id.pinDotsContainer)

        // Bind number buttons
        btn0 = findViewById(R.id.btn0)
        btn1 = findViewById(R.id.btn1)
        btn2 = findViewById(R.id.btn2)
        btn3 = findViewById(R.id.btn3)
        btn4 = findViewById(R.id.btn4)
        btn5 = findViewById(R.id.btn5)
        btn6 = findViewById(R.id.btn6)
        btn7 = findViewById(R.id.btn7)
        btn8 = findViewById(R.id.btn8)
        btn9 = findViewById(R.id.btn9)
        btnDelete = findViewById(R.id.btnDelete)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnBiometric = findViewById(R.id.btnBiometric)

        btnBiometric?.setImageResource(ThemeHelper.getCurrentThemeIconResId(this))

        // Set click listeners
        setNumberPadListeners()

        // Check if vault already exists
        if (isVaultSetup()) {
            isSetupMode = false
            tvInstruction.text = "Enter your 6-digit code"
            checkBiometricStatus()
        } else {
            isSetupMode = true
            tvInstruction.text = "Create a 6-digit code"
        }

        // Initialize dot display
        updateDotDisplay()

        // Security check: Log vault file info
        val vaultFile = File(filesDir, "vault.txt")
        if (vaultFile.exists()) {
            Log.d("SecurePass", "Vault file detected: " + vaultFile.absolutePath)
            Log.d("SecurePass", "File size: " + vaultFile.length() + " bytes")
        }
    }

    private fun isVaultSetup(): Boolean {
        // If the salt file exists, we consider the vault already set up.
        val saltFile = File(filesDir, "vault.salt")
        return saltFile.exists()
    }

    private fun setNumberPadListeners() {
        val numberListener = View.OnClickListener { v ->
            if (enteredCode.length < 6) {
                enteredCode += (v as Button).text.toString()
                updateDotDisplay()
                tvError.visibility = View.GONE
            }
        }

        btn0.setOnClickListener(numberListener)
        btn1.setOnClickListener(numberListener)
        btn2.setOnClickListener(numberListener)
        btn3.setOnClickListener(numberListener)
        btn4.setOnClickListener(numberListener)
        btn5.setOnClickListener(numberListener)
        btn6.setOnClickListener(numberListener)
        btn7.setOnClickListener(numberListener)
        btn8.setOnClickListener(numberListener)
        btn9.setOnClickListener(numberListener)

        btnDelete.setOnClickListener {
            if (enteredCode.isNotEmpty()) {
                enteredCode = enteredCode.substring(0, enteredCode.length - 1)
                updateDotDisplay()
                tvError.visibility = View.GONE
            }
        }

        btnSubmit.setOnClickListener {
            if (enteredCode.length != 6) {
                tvError.text = "Please enter exactly 6 digits."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            processCode(enteredCode)
        }
    }

    private fun updateDotDisplay() {
        pinDotsContainer.removeAllViews()

        val typedValue = TypedValue()
        theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)
        val colorPrimary = typedValue.data

        for (i in 0 until 6) {
            val dot = TextView(this)
            dot.text = if (i < enteredCode.length) "●" else "○"
            dot.textSize = 24f
            dot.setTextColor(colorPrimary)
            dot.setPadding(12, 0, 12, 0)
            pinDotsContainer.addView(dot)
        }
    }

    private fun processCode(code: String) {
        try {
            if (isSetupMode) {
                // Setup mode: first entry is the code, confirm it
                if (pendingCode.isEmpty()) {
                    // First time entering new code
                    pendingCode = code
                    enteredCode = ""
                    updateDotDisplay()
                    tvInstruction.text = "Confirm your 6-digit code"
                } else {
                    // Confirmation
                    if (code == pendingCode) {
                        vaultManager.setupNewVault(code)
                        isSetupMode = false
                        tvInstruction.text = "Vault created! Now unlock."
                        pendingCode = ""
                        enteredCode = ""
                        updateDotDisplay()
                        Snackbar.make(findViewById(android.R.id.content), "Secure Pass ready!", Snackbar.LENGTH_SHORT).show()
                    } else {
                        tvError.text = "Codes do not match. Try again."
                        tvError.visibility = View.VISIBLE
                        pendingCode = ""
                        enteredCode = ""
                        updateDotDisplay()
                        tvInstruction.text = "Create a 6-digit code"
                    }
                }
            } else {
                // Unlock mode
                vaultManager.unlock(code)
                val intent = Intent(this@MainActivity, VaultActivity::class.java)
                intent.putExtra("USER_CODE", code)
                startActivity(intent)
                finish() // so the user can’t press Back to return to the unlock screen
            }
        } catch (e: Exception) {
            tvError.text = "Wrong code. Please try again."
            tvError.visibility = View.VISIBLE
            enteredCode = ""
            updateDotDisplay()
        }
    }

    private fun checkBiometricStatus() {
        val biometricEnabled = prefs.getBoolean("fingerprint_enabled", false)
        val encryptedKey = prefs.getString("encrypted_vault_key", null)
        val lastCodeTime = prefs.getLong("last_code_time", 0)
        val isExpired = (System.currentTimeMillis() - lastCodeTime) > (24 * 60 * 60 * 1000L)

        if (biometricEnabled && encryptedKey != null && !isExpired) {
            btnBiometric?.visibility = View.VISIBLE
            btnBiometric?.setOnClickListener { triggerBiometricUnlock(encryptedKey) }
            // Automatically trigger it on start
            triggerBiometricUnlock(encryptedKey)
        }
    }

    private fun triggerBiometricUnlock(encryptedKey: String) {
        BiometricHelper.decryptKeyWithBiometric(this, encryptedKey, object : BiometricHelper.BiometricAuthenticationCallback {
            override fun onSuccess(decryptedKey: ByteArray) {
                vaultManager.unlockWithKey(SecretKeySpec(decryptedKey, "AES"))
                val intent = Intent(this@MainActivity, VaultActivity::class.java)
                startActivity(intent)
                finish()
            }

            override fun onError(error: String?) {
                Toast.makeText(this@MainActivity, "Biometric failed: $error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}