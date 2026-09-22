package com.applify.securepass

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.net.toUri
import com.applify.securepass.crypto.CryptoManager
import com.applify.securepass.data.VaultItem
import com.applify.securepass.data.VaultManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import java.util.Objects
import javax.crypto.SecretKey

class SettingsActivity : BaseLockActivity() {

    private lateinit var switchFingerprint: SwitchMaterial
    private lateinit var btnLockVault: Button
    private lateinit var btnExport: Button
    private lateinit var btnImport: Button
    private lateinit var btnGithub: Button
    private lateinit var btnCheckUpdates: Button
    private lateinit var btnSubmitFeedback: Button
    private lateinit var prefs: SharedPreferences
    private lateinit var vaultManager: VaultManager

    private lateinit var spinnerAutoLock: Spinner

    private val createDocumentLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            onBackupFileCreated(uri)
        }

    private val openDocumentLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            onBackupFileOpened(uri)
        }

    private lateinit var rgThemeMode: RadioGroup
    private lateinit var rbLight: RadioButton
    private lateinit var rbDark: RadioButton
    private lateinit var rbSystem: RadioButton
    private lateinit var llColorPalette: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        prefs = getSharedPreferences("secure_pass_prefs", MODE_PRIVATE)
        vaultManager = VaultManager(this)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        switchFingerprint = findViewById(R.id.switchFingerprint)
        val tvLastCodeTime: TextView = findViewById(R.id.tvLastCodeTime)
        val btnChangeCode: Button = findViewById(R.id.btnChangeCode)
        btnLockVault = findViewById(R.id.btnLockVault)

        // Theme controls
        rgThemeMode = findViewById(R.id.rgThemeMode)
        rbLight = findViewById(R.id.rbLight)
        rbDark = findViewById(R.id.rbDark)
        rbSystem = findViewById(R.id.rbSystem)
        llColorPalette = findViewById(R.id.llColorPalette)
        spinnerAutoLock = findViewById(R.id.spinnerAutoLock)
        btnExport = findViewById(R.id.btnExport)
        btnImport = findViewById(R.id.btnImport)
        btnGithub = findViewById(R.id.btnGithub)
        btnCheckUpdates = findViewById(R.id.btnCheckUpdates)
        btnSubmitFeedback = findViewById(R.id.btnSubmitFeedback)

        // App Icon and Swipe control
        val switchSyncIcon: SwitchMaterial = findViewById(R.id.switchSyncIcon)
        val spinnerAppIcon: Spinner = findViewById(R.id.spinnerAppIcon)
        val spinnerSwipeRight: Spinner = findViewById(R.id.spinnerSwipeRight)
        val spinnerSwipeLeft: Spinner = findViewById(R.id.spinnerSwipeLeft)

        // Set initial switch state
        val fingerprintEnabled = prefs.getBoolean("fingerprint_enabled", false)
        switchFingerprint.isChecked = fingerprintEnabled

        // Update last code time
        val lastTime = prefs.getLong("last_code_time", 0)
        if (lastTime > 0) {
            tvLastCodeTime.text = "Last code entry: " +
                    DateUtils.getRelativeTimeSpanString(lastTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
        }

        // Restore theme selections
        restoreThemeSettings()
        setupAutoLockSpinner()
        setupAppIconAndSwipes(switchSyncIcon, spinnerAppIcon, spinnerSwipeRight, spinnerSwipeLeft)

        // Fingerprint toggle listener
        switchFingerprint.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !fingerprintEnabled) {
                showMasterCodePrompt("enable fingerprint") {
                    try {
                        if (!vaultManager.isUnlocked() && VaultManager.globalKey == null) {
                            Toast.makeText(this, "Vault not unlocked", Toast.LENGTH_SHORT).show()
                            switchFingerprint.isChecked = false
                            return@showMasterCodePrompt
                        }
                        val key = vaultManager.getCurrentKey()
                        if (key == null) {
                            Toast.makeText(this, "Key not available", Toast.LENGTH_SHORT).show()
                            switchFingerprint.isChecked = false
                            return@showMasterCodePrompt
                        }
                        val encryptedKey = BiometricHelper.encryptKeyWithBiometric(key.encoded)
                        prefs.edit {
                            putBoolean("fingerprint_enabled", true)
                            putString("encrypted_vault_key", encryptedKey)
                            putLong("last_code_time", System.currentTimeMillis())
                        }
                        Toast.makeText(this, "Fingerprint enabled", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Fingerprint setup failed", e)
                        Toast.makeText(this, "Failed: " + e.message, Toast.LENGTH_LONG).show()
                        switchFingerprint.isChecked = false
                    }
                }
            } else if (!isChecked && fingerprintEnabled) {
                prefs.edit {
                    putBoolean("fingerprint_enabled", false)
                    remove("encrypted_vault_key")
                }
                Toast.makeText(this, "Fingerprint disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Theme mode listener
        rgThemeMode.setOnCheckedChangeListener { _, checkedId ->
            val mode: Int = when (checkedId) {
                R.id.rbLight -> 0
                R.id.rbDark -> 1
                else -> 2 // system
            }
            prefs.edit { putInt(KEY_THEME_MODE, mode) }
            // Apply theme change immediately
            ThemeHelper.applyThemeMode(mode)
            // Restart activity to refresh colors fully
            recreate()
        }

        // Build color palette buttons
        buildColorPalette()

        btnChangeCode.setOnClickListener { showChangeCodeDialog() }
        btnExport.setOnClickListener { promptBackupPassword() }
        btnImport.setOnClickListener { openDocumentLauncher.launch(arrayOf("text/plain")) }

        btnGithub.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/saurabhkundu1/secure-password-manager".toUri())
            startActivity(intent)
        }

        btnCheckUpdates.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/saurabhkundu1/secure-password-manager/releases/latest".toUri())
            startActivity(intent)
        }

        btnSubmitFeedback.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/saurabhkundu1/secure-password-manager/issues".toUri())
            startActivity(intent)
        }

        btnLockVault.setOnClickListener {
            VaultManager.clearGlobalKey()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun restoreThemeSettings() {
        val themeMode = prefs.getInt(KEY_THEME_MODE, 2) // default system
        when (themeMode) {
            0 -> rbLight.isChecked = true
            1 -> rbDark.isChecked = true
            else -> rbSystem.isChecked = true
        }
    }

    private fun setupAutoLockSpinner() {
        val options = arrayOf("Never", "1 minute", "5 minutes", "15 minutes", "30 minutes")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAutoLock.adapter = adapter

        val currentTime = prefs.getLong(KEY_AUTO_LOCK, 0)
        for (i in AUTO_LOCK_VALUES.indices) {
            if (Objects.equals(AUTO_LOCK_VALUES[i], currentTime)) {
                spinnerAutoLock.setSelection(i)
                break
            }
        }

        spinnerAutoLock.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit { putLong(KEY_AUTO_LOCK, AUTO_LOCK_VALUES[position]) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupAppIconAndSwipes(
        switchSyncIcon: SwitchMaterial,
        spinnerAppIcon: Spinner,
        spinnerSwipeRight: Spinner,
        spinnerSwipeLeft: Spinner
    ) {
        val syncEnabled = prefs.getBoolean("sync_icon_palette", true)
        switchSyncIcon.isChecked = syncEnabled

        switchSyncIcon.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("sync_icon_palette", isChecked) }
            if (isChecked) {
                // Instantly sync
                val currentPalette = prefs.getInt(KEY_COLOR_PALETTE, 0)
                ThemeHelper.updateAppIcon(this, currentPalette)
            }
        }

        // App Icon Selector (Independent if sync is off)
        val iconOptions = arrayOf("Teal (Default)", "Classic Blue", "Forest Green", "Royal Purple", "Crimson Red", "Amber Gold")
        val iconAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, iconOptions)
        iconAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAppIcon.adapter = iconAdapter
        val currentIconIndex = prefs.getInt("custom_app_icon", 0)
        spinnerAppIcon.setSelection(currentIconIndex)

        spinnerAppIcon.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit { putInt("custom_app_icon", position) }
                if (!prefs.getBoolean("sync_icon_palette", true)) {
                    ThemeHelper.updateAppIcon(this@SettingsActivity, position)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Swipe Options
        val swipeOptions = arrayOf("None", "Delete", "Pin to Top / Favorite")
        val swipeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, swipeOptions)
        swipeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerSwipeRight.adapter = swipeAdapter
        spinnerSwipeLeft.adapter = swipeAdapter

        // Right defaults to Delete (1), Left defaults to Favorite (2)
        spinnerSwipeRight.setSelection(prefs.getInt("swipe_right_action", 1))
        spinnerSwipeLeft.setSelection(prefs.getInt("swipe_left_action", 2))

        spinnerSwipeRight.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit { putInt("swipe_right_action", position) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerSwipeLeft.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit { putInt("swipe_left_action", position) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun buildColorPalette() {
        llColorPalette.removeAllViews()
        val currentPalette = prefs.getInt(KEY_COLOR_PALETTE, 0) // default teal

        for (i in PALETTE_COLORS.indices) {
            val index = i
            val colorCircle = View(this)
            val size = resources.getDimension(androidx.appcompat.R.dimen.abc_action_bar_default_height_material).toInt() / 2
            val params = GridLayout.LayoutParams()
            params.width = size
            params.height = size
            params.setMargins(16, 16, 16, 16)
            colorCircle.layoutParams = params
            colorCircle.setBackgroundColor(PALETTE_COLORS[i])
            if (i == currentPalette) {
                colorCircle.setBackgroundResource(androidx.appcompat.R.drawable.abc_btn_colored_material)
                colorCircle.alpha = 1.0f
            } else {
                colorCircle.alpha = 0.5f
            }

            colorCircle.setOnClickListener {
                val isSync = prefs.getBoolean("sync_icon_palette", true)
                prefs.edit {
                    putInt(KEY_COLOR_PALETTE, index)
                    if (isSync) {
                        putInt("custom_app_icon", index)
                    }
                }
                if (isSync) {
                    ThemeHelper.updateAppIcon(this, index)
                }
                buildColorPalette()
                recreate()
            }
            llColorPalette.addView(colorCircle)
        }
    }

    // ---------- Backup & Restore Logic ----------

    private var tempBackupPassword: String? = null

    private fun promptBackupPassword() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set Backup Password")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)
        builder.setPositiveButton("Continue") { _, _ ->
            tempBackupPassword = input.text.toString()
            createDocumentLauncher.launch("secure_pass_backup.txt")
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun onBackupFileCreated(uri: Uri?) {
        val pass = tempBackupPassword
        if (uri == null || pass == null) return
        try {
            // 1. Prepare data
            val entries = vaultManager.loadEntries()
            val json = Gson().toJson(entries)

            // 2. Encrypt
            val salt = ByteArray(16)
            SecureRandom().nextBytes(salt)
            val key = CryptoManager.deriveKey(pass, salt)

            val encryptedIVData = CryptoManager.encrypt(json, key)
            val ivData = Base64.getDecoder().decode(encryptedIVData)

            // 3. Combine: salt + IV + ciphertext
            val buffer = ByteBuffer.allocate(salt.size + ivData.size)
            buffer.put(salt)
            buffer.put(ivData)

            val finalBase64 = Base64.getEncoder().encodeToString(buffer.array())

            // 4. Write to file
            contentResolver.openOutputStream(uri)?.use { os ->
                os.write(finalBase64.toByteArray(StandardCharsets.UTF_8))
            }
            Toast.makeText(this, "Backup exported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            Toast.makeText(this, "Export failed: " + e.message, Toast.LENGTH_LONG).show()
        } finally {
            tempBackupPassword = null
        }
    }

    private fun onBackupFileOpened(uri: Uri?) {
        if (uri == null) return
        getPassword { password -> handleImport(uri, password) }
    }

    private fun interface PasswordCallback {
        fun onPassword(password: String)
    }

    private fun getPassword(callback: PasswordCallback) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Backup Password")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)
        builder.setPositiveButton("OK") { _, _ -> callback.onPassword(input.text.toString()) }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun handleImport(uri: Uri, password: String) {
        try {
            // 1. Read file
            val fileBytes: ByteArray
            contentResolver.openInputStream(uri).use { isStream ->
                if (isStream == null) throw Exception("Could not open file")
                val size = isStream.available()
                val encodedBytes = ByteArray(size)
                val read = isStream.read(encodedBytes)
                if (read <= 0) throw Exception("File is empty or could not be read")
                fileBytes = Base64.getDecoder().decode(String(encodedBytes, StandardCharsets.UTF_8))
            }

            // 2. Extract salt and data
            val buffer = ByteBuffer.wrap(fileBytes)
            val salt = ByteArray(16)
            buffer.get(salt)
            val ivData = ByteArray(buffer.remaining())
            buffer.get(ivData)

            // 3. Decrypt
            val key = CryptoManager.deriveKey(password, salt)
            val encryptedIVData = Base64.getEncoder().encodeToString(ivData)
            val json = CryptoManager.decrypt(encryptedIVData, key)

            // 4. Parse
            val importedEntries: List<VaultItem> = Gson().fromJson(json, object : TypeToken<List<VaultItem>>() {}.type)

            // 5. Merge or Replace
            showMergeDialog(importedEntries)

        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            Toast.makeText(this, "Import failed: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showMergeDialog(importedEntries: List<VaultItem>) {
        AlertDialog.Builder(this)
            .setTitle("Restore Backup")
            .setMessage("Found " + importedEntries.size + " entries. Do you want to merge them with current entries or replace everything?")
            .setPositiveButton("Merge") { _, _ ->
                try {
                    vaultManager.mergeEntries(importedEntries)
                    Toast.makeText(this, "Merged successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Merge failed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Replace") { _, _ ->
                try {
                    vaultManager.saveEntries(importedEntries)
                    Toast.makeText(this, "Replaced successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Replace failed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMasterCodePrompt(reason: String, onSuccess: Runnable) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Master Code")
        builder.setMessage("To $reason, enter your 6‑digit code.")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        builder.setView(input)
        builder.setPositiveButton("OK") { _, _ ->
            val code = input.text.toString()
            try {
                vaultManager.unlock(code)
                onSuccess.run()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Wrong code", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showChangeCodeDialog() {
        val oldCodeBuilder = AlertDialog.Builder(this)
        oldCodeBuilder.setTitle("Current Code")
        oldCodeBuilder.setMessage("Enter your current 6‑digit code.")
        val oldInput = EditText(this)
        oldInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        oldCodeBuilder.setView(oldInput)
        oldCodeBuilder.setPositiveButton("Next") { _, _ ->
            val oldCode = oldInput.text.toString()
            try {
                vaultManager.unlock(oldCode)
                showNewCodeDialog()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Wrong current code", Toast.LENGTH_SHORT).show()
            }
        }
        oldCodeBuilder.setNegativeButton("Cancel", null)
        oldCodeBuilder.show()
    }

    private fun showNewCodeDialog() {
        val newCodeBuilder = AlertDialog.Builder(this)
        newCodeBuilder.setTitle("New Code")
        newCodeBuilder.setMessage("Enter a new 6‑digit code.")
        val newInput = EditText(this)
        newInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        newCodeBuilder.setView(newInput)
        newCodeBuilder.setPositiveButton("Next") { _, _ ->
            val newCode = newInput.text.toString()
            if (newCode.length != 6) {
                Toast.makeText(this@SettingsActivity, "Code must be 6 digits", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            showConfirmNewCodeDialog(newCode)
        }
        newCodeBuilder.setNegativeButton("Cancel", null)
        newCodeBuilder.show()
    }

    private fun showConfirmNewCodeDialog(newCode: String) {
        val confirmBuilder = AlertDialog.Builder(this)
        confirmBuilder.setTitle("Confirm New Code")
        confirmBuilder.setMessage("Re‑enter the new 6‑digit code.")
        val confirmInput = EditText(this)
        confirmInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        confirmBuilder.setView(confirmInput)
        confirmBuilder.setPositiveButton("Change") { _, _ ->
            val confirmCode = confirmInput.text.toString()
            if (newCode != confirmCode) {
                Toast.makeText(this@SettingsActivity, "Codes do not match", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            showOldCodeForChange(newCode)
        }
        confirmBuilder.setNegativeButton("Cancel", null)
        confirmBuilder.show()
    }

    private fun showOldCodeForChange(newCode: String) {
        val oldBuilder = AlertDialog.Builder(this)
        oldBuilder.setTitle("Current Code")
        oldBuilder.setMessage("Enter your current 6‑digit code to confirm change.")
        val oldInput = EditText(this)
        oldInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        oldBuilder.setView(oldInput)
        oldBuilder.setPositiveButton("Confirm") { _, _ ->
            val oldCode = oldInput.text.toString()
            try {
                vaultManager.changeMasterCode(oldCode, newCode)
                prefs.edit {
                    putBoolean("fingerprint_enabled", false)
                    remove("encrypted_vault_key")
                }
                switchFingerprint.isChecked = false
                Toast.makeText(this@SettingsActivity, "Master code changed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Error: " + e.message, Toast.LENGTH_LONG).show()
            }
        }
        oldBuilder.setNegativeButton("Cancel", null)
        oldBuilder.show()
    }

    companion object {
        private const val TAG = "SettingsActivity"
        private const val KEY_THEME_MODE = "theme_mode" // 0=light, 1=dark, 2=system
        private const val KEY_COLOR_PALETTE = "color_palette" // 0=teal, 1=blue, 2=green, 3=purple, 4=red
        private const val KEY_AUTO_LOCK = "auto_lock_time"

        private val AUTO_LOCK_VALUES = longArrayOf(
            0,
            60 * 1000L,
            5 * 60 * 1000L,
            15 * 60 * 1000L,
            30 * 60 * 1000L
        )

        private val PALETTE_COLORS = intArrayOf(
            -0xff7685, // Teal
            -0xe6892e, // Blue
            -0xc771c4, // Green
            -0x84e05e, // Purple
            -0x2ce0d1, // Red
            -0x6000,   // Amber Gold
            -0xc0ae4b, // Indigo
            -0x27e4a0, // Pink
            -0xdededf, // Onyx
            -0x43f0d,  // Yellow
            -0xff432c, // Cyan
            -0x86aa88, // Brown
            -0x616162  // Grey
        )
    }
}