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
import android.widget.LinearLayout
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
            ThemeHelper.applyThemeMode(mode)
            recreate()
        }

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
        val themeMode = prefs.getInt(KEY_THEME_MODE, 2)
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
            val currentPalette = prefs.getInt(KEY_COLOR_PALETTE, 0)
            if (isChecked) {
                ThemeHelper.updateAppIcon(this, currentPalette)
            } else {
                val customIcon = prefs.getInt("custom_app_icon", 0)
                ThemeHelper.updateAppIcon(this, customIcon)
            }
        }

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

        // Swipe Options with distinct color trail indicators (8 Actions)
        val swipeOptions = arrayOf(
            "None",
            "Delete (Red)",
            "Pin / Favorite (Amber)",
            "Copy Password (Blue)",
            "Copy Username (Green)",
            "Edit Entry (Purple)",
            "Copy Notes (Cyan)",
            "Share Credential (Indigo)",
            "View Details (Teal)"
        )
        val swipeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, swipeOptions)
        swipeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerSwipeRight.adapter = swipeAdapter
        spinnerSwipeLeft.adapter = swipeAdapter

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
        val currentPalette = prefs.getInt(KEY_COLOR_PALETTE, 0)

        for (i in PALETTE_COLORS.indices) {
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
            }

            colorCircle.setOnClickListener {
                prefs.edit { putInt(KEY_COLOR_PALETTE, i) }
                ThemeHelper.applyTheme(this@SettingsActivity)
                if (prefs.getBoolean("sync_icon_palette", true)) {
                    ThemeHelper.updateAppIcon(this@SettingsActivity, i)
                }
                recreate()
            }
            llColorPalette.addView(colorCircle)
        }
    }

    private fun showMasterCodePrompt(actionName: String, onValidCode: () -> Unit) {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        AlertDialog.Builder(this)
            .setTitle("Authentication Required")
            .setMessage("Enter your 6-digit master code to " + actionName + ":")
            .setView(input)
            .setPositiveButton("Confirm") { _, _ ->
                val code = input.text.toString()
                try {
                    vaultManager.unlock(code)
                    prefs.edit { putLong("last_code_time", System.currentTimeMillis()) }
                    onValidCode()
                } catch (e: Exception) {
                    Toast.makeText(this, "Incorrect code", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showChangeCodeDialog() {
        val oldCodeInput = EditText(this)
        oldCodeInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        val newCodeInput = EditText(this)
        newCodeInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(32, 16, 32, 16)
        oldCodeInput.hint = "Old 6-digit code"
        newCodeInput.hint = "New 6-digit code"
        container.addView(oldCodeInput)
        container.addView(newCodeInput)

        AlertDialog.Builder(this)
            .setTitle("Change Master Code")
            .setView(container)
            .setPositiveButton("Submit") { _, _ ->
                val oldCode = oldCodeInput.text.toString()
                val newCode = newCodeInput.text.toString()
                if (oldCode.length != 6 || newCode.length != 6) {
                    Toast.makeText(this, "Codes must be 6 digits", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                try {
                    vaultManager.changeMasterCode(oldCode, newCode)
                    prefs.edit { putLong("last_code_time", System.currentTimeMillis()) }
                    Toast.makeText(this, "Master code updated successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to change code: " + e.message, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptBackupPassword() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        input.hint = "Mandatory 6-digit Backup Password"

        AlertDialog.Builder(this)
            .setTitle("Set Backup Password")
            .setMessage("Enter a mandatory 6-digit password to encrypt your backup file:")
            .setView(input)
            .setPositiveButton("Next") { _, _ ->
                val pwd = input.text.toString()
                if (pwd.length != 6) {
                    Toast.makeText(this, "Backup password must be exactly 6 characters long", Toast.LENGTH_LONG).show()
                } else {
                    pendingBackupPassword = pwd
                    createDocumentLauncher.launch("vault-backup.txt")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private var pendingBackupPassword: String? = null

    private fun onBackupFileCreated(uri: Uri?) {
        if (uri == null || pendingBackupPassword == null) return
        val pwd = pendingBackupPassword ?: return
        pendingBackupPassword = null

        try {
            val entries = vaultManager.loadEntries()
            val gson = Gson()
            val json = gson.toJson(entries)

            val salt = ByteArray(16)
            SecureRandom().nextBytes(salt)

            val key = CryptoManager.deriveKey(pwd, salt)
            val encryptedVault = CryptoManager.encrypt(json, key)

            val buffer = ByteBuffer.allocate(16 + encryptedVault.length)
            buffer.put(salt)
            buffer.put(encryptedVault.toByteArray(StandardCharsets.UTF_8))

            val base64Backup = Base64.getEncoder().encodeToString(buffer.array())

            contentResolver.openOutputStream(uri)?.use { os: OutputStream ->
                os.write(base64Backup.toByteArray(StandardCharsets.UTF_8))
            }
            Toast.makeText(this, "Backup exported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            Toast.makeText(this, "Export failed: " + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onBackupFileOpened(uri: Uri?) {
        if (uri == null) return

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        input.hint = "Mandatory 6-digit Backup Password"

        AlertDialog.Builder(this)
            .setTitle("Enter Backup Password")
            .setMessage("Enter the mandatory 6-digit password used when creating this backup:")
            .setView(input)
            .setPositiveButton("Restore") { _, _ ->
                val pwd = input.text.toString()
                if (pwd.length != 6) {
                    Toast.makeText(this, "Backup password must be exactly 6 characters long", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                try {
                    val base64Data = StringBuilder()
                    contentResolver.openInputStream(uri)?.use { isStream: InputStream ->
                        val reader = isStream.bufferedReader(StandardCharsets.UTF_8)
                        reader.forEachLine { line -> base64Data.append(line) }
                    }

                    val fullBytes = Base64.getDecoder().decode(base64Data.toString())
                    if (fullBytes.size < 17) {
                        Toast.makeText(this, "Invalid backup file format", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val buffer = ByteBuffer.wrap(fullBytes)
                    val salt = ByteArray(16)
                    buffer.get(salt)

                    val cipherBytes = ByteArray(fullBytes.size - 16)
                    buffer.get(cipherBytes)
                    val encryptedVault = String(cipherBytes, StandardCharsets.UTF_8)

                    val key = CryptoManager.deriveKey(pwd, salt)
                    val json = CryptoManager.decrypt(encryptedVault, key)

                    val listType = object : TypeToken<MutableList<VaultItem>>() {}.type
                    val imported: List<VaultItem> = Gson().fromJson(json, listType) ?: ArrayList()

                    vaultManager.mergeEntries(imported)
                    Toast.makeText(this, "Imported " + imported.size + " entries", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Import failed", e)
                    Toast.makeText(this, "Import failed. Wrong password or corrupted file.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        private const val TAG = "SettingsActivity"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_AUTO_LOCK = "auto_lock_time"
        private const val KEY_COLOR_PALETTE = "color_palette"

        private val AUTO_LOCK_VALUES = arrayOf(
            0L,
            60 * 1000L,
            5 * 60 * 1000L,
            15 * 60 * 1000L,
            30 * 60 * 1000L
        )

        private val PALETTE_COLORS = intArrayOf(
            -0xe0322d, // Teal
            -0xd7631b, // Blue
            -0xb350a2, // Green
            -0x75cd27, // Purple
            -0x2cd0d3, // Red
            -0x12bb2,  // Orange
            -0xc6820f, // Indigo
            -0x117a22, // Pink
            -0xdcdcdc, // Onyx
            -0x900,    // Yellow
            -0xe05f01, // Cyan
            -0x82a5b6, // Brown
            -0x878788  // Grey
        )
    }
}