package com.applify.securepass

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

    // Number pad key buttons (12 keys: 0-9, *, #)
    private val keyButtons = mutableListOf<Button>()
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

        // Bind 12 key buttons
        keyButtons.clear()
        val keyIds = arrayOf(
            R.id.btnKey0, R.id.btnKey1, R.id.btnKey2,
            R.id.btnKey3, R.id.btnKey4, R.id.btnKey5,
            R.id.btnKey6, R.id.btnKey7, R.id.btnKey8,
            R.id.btnKey9, R.id.btnKey10, R.id.btnKey11
        )
        for (id in keyIds) {
            keyButtons.add(findViewById(id))
        }

        btnDelete = findViewById(R.id.btnDelete)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnBiometric = findViewById(R.id.btnBiometric)

        btnBiometric?.setImageResource(ThemeHelper.getCurrentThemeIconResId(this))

        // Set control pad listeners
        setControlPadListeners()

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

    override fun onResume() {
        super.onResume()
        // Reshuffle numbers on every launch / display of lock screen
        shuffleAndBindNumberPad()
    }

    private fun isVaultSetup(): Boolean {
        val saltFile = File(filesDir, "vault.salt")
        return saltFile.exists()
    }

    private fun shuffleAndBindNumberPad() {
        // Includes digits 0-9 plus '*' and '#'
        val keys = mutableListOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "#")
        keys.shuffle()

        val numberListener = View.OnClickListener { v ->
            val value = (v as Button).text.toString()
            if (value in "0".."9") {
                if (enteredCode.length < 6) {
                    enteredCode += value
                    updateDotDisplay()
                    tvError.visibility = View.GONE
                }
            } else {
                // '*' or '#' was tapped - PIN only accepts numbers
                tvError.text = "PIN accepts numbers only"
                tvError.visibility = View.VISIBLE
            }
        }

        for (i in keyButtons.indices) {
            keyButtons[i].text = keys[i]
            keyButtons[i].setOnClickListener(numberListener)
        }
    }

    private fun setControlPadListeners() {
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
                if (pendingCode.isEmpty()) {
                    pendingCode = code
                    enteredCode = ""
                    updateDotDisplay()
                    tvInstruction.text = "Confirm your 6-digit code"
                } else {
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
                vaultManager.unlock(code)
                val intent = Intent(this@MainActivity, VaultActivity::class.java)
                intent.putExtra("USER_CODE", code)
                startActivity(intent)
                finish()
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