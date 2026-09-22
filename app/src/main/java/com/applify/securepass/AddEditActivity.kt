package com.applify.securepass

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.applify.securepass.data.VaultItem
import com.applify.securepass.data.VaultManager
import com.google.android.material.textfield.TextInputEditText
import java.util.Objects

class AddEditActivity : BaseLockActivity() {

    private lateinit var etWebsite: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var progressStrength: ProgressBar
    private lateinit var tvStrengthText: TextView
    private lateinit var vaultManager: VaultManager
    private var userCode: String? = null
    private var editingItemId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit)

        vaultManager = VaultManager(this)
        userCode = intent.getStringExtra("USER_CODE")

        etWebsite = findViewById(R.id.etWebsite)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etNotes = findViewById(R.id.etNotes)
        progressStrength = findViewById(R.id.progressStrength)
        tvStrengthText = findViewById(R.id.tvStrengthText)
        val btnGenerate: Button = findViewById(R.id.btnGeneratePassword)
        val btnSave: Button = findViewById(R.id.btnSave)

        if (intent.hasExtra("ITEM_ID")) {
            editingItemId = intent.getStringExtra("ITEM_ID")
            editingItemId?.let { loadExistingItem(it) }
        }

        // Initial strength check
        updatePasswordStrength(etPassword.text?.toString() ?: "")

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePasswordStrength(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnGenerate.setOnClickListener { showPasswordGeneratorDialog() }
        btnSave.setOnClickListener { saveEntry() }
    }

    private fun loadExistingItem(itemId: String) {
        try {
            val code = userCode
            if (!vaultManager.isUnlocked() && code != null) {
                vaultManager.unlock(code)
            }
            for (item in vaultManager.loadEntries()) {
                if (Objects.equals(item.id, itemId)) {
                    etWebsite.setText(item.website)
                    etUsername.setText(item.username)
                    etPassword.setText(item.password)
                    etNotes.setText(item.notes)
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading existing item", e)
        }
    }

    private fun saveEntry() {
        val website = etWebsite.text?.toString()?.trim() ?: ""
        val username = etUsername.text?.toString()?.trim() ?: ""
        val password = etPassword.text?.toString() ?: ""
        val notes = etNotes.text?.toString()?.trim() ?: ""

        if (website.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val code = userCode
            if (!vaultManager.isUnlocked() && code != null) {
                vaultManager.unlock(code)
            }
            val entries = vaultManager.loadEntries()

            val itemId = editingItemId
            if (itemId != null) {
                for (item in entries) {
                    if (Objects.equals(item.id, itemId)) {
                        item.website = website
                        item.username = username
                        item.password = password
                        item.notes = notes
                        item.lastChanged = System.currentTimeMillis()
                        break
                    }
                }
            } else {
                entries.add(VaultItem(website, username, password, notes))
            }

            vaultManager.saveEntries(entries)
            Toast.makeText(this, "Entry saved!", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error saving entry", e)
        }
    }

    private fun showPasswordGeneratorDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Generate Password")
        val dialogView = layoutInflater.inflate(R.layout.dialog_password_generator, null)
        builder.setView(dialogView)

        val etLength: TextInputEditText = dialogView.findViewById(R.id.etLength)
        val cbUpper: CheckBox = dialogView.findViewById(R.id.cbUpper)
        val cbLower: CheckBox = dialogView.findViewById(R.id.cbLower)
        val cbDigits: CheckBox = dialogView.findViewById(R.id.cbDigits)
        val cbSymbols: CheckBox = dialogView.findViewById(R.id.cbSymbols)

        builder.setPositiveButton("Generate") { _, _ ->
            val length = try {
                val lengthStr = etLength.text?.toString() ?: "16"
                lengthStr.toInt()
            } catch (e: NumberFormatException) {
                16
            }
            val pwd = PasswordGenerator.generate(
                length,
                cbUpper.isChecked, cbLower.isChecked,
                cbDigits.isChecked, cbSymbols.isChecked
            )
            etPassword.setText(pwd)
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun updatePasswordStrength(password: String?) {
        if (password.isNullOrEmpty()) {
            progressStrength.progress = 0
            tvStrengthText.text = "Weak"
            tvStrengthText.setTextColor(-0x2ce0d1)
            progressStrength.progressTintList = ColorStateList.valueOf(-0x2ce0d1)
            return
        }

        var score = 0
        // Length (max 40 pts)
        score += Math.min(password.length * 5, 40)

        // Varieties (15 pts each)
        if (password.contains(Regex(".*[A-Z].*"))) score += 15
        if (password.contains(Regex(".*[a-z].*"))) score += 15
        if (password.contains(Regex(".*[0-9].*"))) score += 15
        if (password.contains(Regex(".*[^A-Za-z0-9].*"))) score += 15

        progressStrength.progress = score

        if (score <= 40) {
            tvStrengthText.text = "Weak"
            tvStrengthText.setTextColor(-0x2ce0d1) // palette_red_primary
            progressStrength.progressTintList = ColorStateList.valueOf(-0x2ce0d1)
        } else if (score <= 70) {
            tvStrengthText.text = "Fair"
            tvStrengthText.setTextColor(-0xa8400) // palette_orange_primary
            progressStrength.progressTintList = ColorStateList.valueOf(-0xa8400)
        } else {
            tvStrengthText.text = "Strong"
            tvStrengthText.setTextColor(-0xc771c4) // palette_green_primary
            progressStrength.progressTintList = ColorStateList.valueOf(-0xc771c4)
        }
    }

    companion object {
        private const val TAG = "AddEditActivity"
    }
}