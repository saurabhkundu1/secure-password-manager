package com.applify.securepass

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.format.DateUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.applify.securepass.crypto.CryptoManager
import com.applify.securepass.data.VaultItem
import com.applify.securepass.data.VaultManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
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

    private lateinit var switchFingerprint: MaterialSwitch
    private lateinit var btnLockVault: Button
    private lateinit var btnExport: View
    private lateinit var btnImport: View
    private lateinit var btnGithub: View
    private lateinit var btnCheckUpdates: View
    private lateinit var btnSubmitFeedback: View
    private lateinit var prefs: SharedPreferences
    private lateinit var vaultManager: VaultManager

    private lateinit var rowAutoLock: View
    private lateinit var tvAutoLockValue: TextView

    private val createDocumentLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            onBackupFileCreated(uri)
        }

    private val openDocumentLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            onBackupFileOpened(uri)
        }

    private lateinit var toggleThemeMode: MaterialButtonToggleGroup
    private var isRestoringThemeMode = false
    private lateinit var llColorPalette: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting)

        val settingsRoot: View = findViewById(R.id.settingsRoot)
        val settingsScrollView: ScrollView = findViewById(R.id.settingsScrollView)
        ViewCompat.setOnApplyWindowInsetsListener(settingsRoot) { v, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(insets.left, insets.top, insets.right, 0)
            settingsScrollView.setPadding(0, 0, 0, insets.bottom)
            windowInsets
        }

        prefs = getSharedPreferences("secure_pass_prefs", MODE_PRIVATE)
        vaultManager = VaultManager(this)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        switchFingerprint = findViewById(R.id.switchFingerprint)
        val tvLastCodeTime: TextView = findViewById(R.id.tvLastCodeTime)
        val btnChangeCode: View = findViewById(R.id.btnChangeCode)
        btnLockVault = findViewById(R.id.btnLockVault)
        findViewById<View>(R.id.btnAutofill).setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE, "package:$packageName".toUri()))
        }

        // Theme controls
        toggleThemeMode = findViewById(R.id.toggleThemeMode)
        llColorPalette = findViewById(R.id.llColorPalette)
        rowAutoLock = findViewById(R.id.rowAutoLock)
        tvAutoLockValue = findViewById(R.id.tvAutoLockValue)
        btnExport = findViewById(R.id.btnExport)
        btnImport = findViewById(R.id.btnImport)
        btnGithub = findViewById(R.id.btnGithub)
        btnCheckUpdates = findViewById(R.id.btnCheckUpdates)
        btnSubmitFeedback = findViewById(R.id.btnSubmitFeedback)

        // App Icon and Swipe control
        val switchSyncIcon: MaterialSwitch = findViewById(R.id.switchSyncIcon)
        val rowAppIcon: View = findViewById(R.id.rowAppIcon)
        val tvAppIconValue: TextView = findViewById(R.id.tvAppIconValue)
        val rowSwipeRight: View = findViewById(R.id.rowSwipeRight)
        val tvSwipeRightValue: TextView = findViewById(R.id.tvSwipeRightValue)
        val rowSwipeLeft: View = findViewById(R.id.rowSwipeLeft)
        val tvSwipeLeftValue: TextView = findViewById(R.id.tvSwipeLeftValue)

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
        setupAutoLockRow()
        setupAppIconAndSwipes(
            switchSyncIcon,
            rowAppIcon,
            tvAppIconValue,
            rowSwipeRight,
            tvSwipeRightValue,
            rowSwipeLeft,
            tvSwipeLeftValue
        )

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
        toggleThemeMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || isRestoringThemeMode) return@addOnButtonCheckedListener
            val mode: Int = when (checkedId) {
                R.id.btnThemeLight -> 0
                R.id.btnThemeDark -> 1
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
        isRestoringThemeMode = true
        val themeMode = prefs.getInt(KEY_THEME_MODE, 2)
        val buttonId = when (themeMode) {
            0 -> R.id.btnThemeLight
            1 -> R.id.btnThemeDark
            else -> R.id.btnThemeSystem
        }
        toggleThemeMode.check(buttonId)
        isRestoringThemeMode = false
    }

    private fun setupAutoLockRow() {
        val options = arrayOf("Never", "1 minute", "5 minutes", "15 minutes", "30 minutes")
        val currentTime = prefs.getLong(KEY_AUTO_LOCK, 0)
        var selectedIndex = AUTO_LOCK_VALUES.indexOfFirst { Objects.equals(it, currentTime) }.coerceAtLeast(0)
        tvAutoLockValue.text = options[selectedIndex]

        rowAutoLock.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Auto-Lock Timer")
                .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                    selectedIndex = which
                    prefs.edit { putLong(KEY_AUTO_LOCK, AUTO_LOCK_VALUES[which]) }
                    tvAutoLockValue.text = options[which]
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupAppIconAndSwipes(
        switchSyncIcon: MaterialSwitch,
        rowAppIcon: View,
        tvAppIconValue: TextView,
        rowSwipeRight: View,
        tvSwipeRightValue: TextView,
        rowSwipeLeft: View,
        tvSwipeLeftValue: TextView
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

        var currentIconIndex = prefs.getInt("custom_app_icon", 0).coerceIn(ThemeHelper.iconOptions.indices)
        tvAppIconValue.text = ThemeHelper.iconOptions[currentIconIndex]

        rowAppIcon.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Custom App Icon Color")
                .setSingleChoiceItems(ThemeHelper.iconOptions, currentIconIndex) { dialog, which ->
                    currentIconIndex = which
                    prefs.edit { putInt("custom_app_icon", which) }
                    tvAppIconValue.text = ThemeHelper.iconOptions[which]
                    if (!prefs.getBoolean("sync_icon_palette", true)) {
                        ThemeHelper.updateAppIcon(this@SettingsActivity, which)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        ThemeHelper.updateAppIcon(
            this,
            if (syncEnabled) prefs.getInt(KEY_COLOR_PALETTE, 0) else currentIconIndex
        )

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

        var swipeRightIndex = prefs.getInt("swipe_right_action", 1).coerceIn(swipeOptions.indices)
        var swipeLeftIndex = prefs.getInt("swipe_left_action", 2).coerceIn(swipeOptions.indices)
        tvSwipeRightValue.text = swipeOptions[swipeRightIndex]
        tvSwipeLeftValue.text = swipeOptions[swipeLeftIndex]

        rowSwipeRight.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Swipe Right Action")
                .setSingleChoiceItems(swipeOptions, swipeRightIndex) { dialog, which ->
                    swipeRightIndex = which
                    prefs.edit { putInt("swipe_right_action", which) }
                    tvSwipeRightValue.text = swipeOptions[which]
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        rowSwipeLeft.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Swipe Left Action")
                .setSingleChoiceItems(swipeOptions, swipeLeftIndex) { dialog, which ->
                    swipeLeftIndex = which
                    prefs.edit { putInt("swipe_left_action", which) }
                    tvSwipeLeftValue.text = swipeOptions[which]
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun buildColorPalette() {
        llColorPalette.removeAllViews()
        val currentPalette = prefs.getInt(KEY_COLOR_PALETTE, 0)
        val density = resources.displayMetrics.density
        val swatchSize = (40 * density).toInt()
        val touchSize = (48 * density).toInt()
        val ringPad = (4 * density).toInt()
        val strokePx = (2 * density).toInt().coerceAtLeast(1)
        val checkSize = (20 * density).toInt()
        val marginPx = (4 * density).toInt()

        for (i in PALETTE_COLOR_RES_IDS.indices) {
            val paletteColor = getColor(PALETTE_COLOR_RES_IDS[i])
            val isSelected = i == currentPalette

            val container = FrameLayout(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = touchSize
                    height = touchSize
                    setMargins(marginPx, marginPx, marginPx, marginPx)
                }
                if (isSelected) {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.TRANSPARENT)
                        setStroke(strokePx, getColor(R.color.m3_on_surface))
                    }
                    setPadding(ringPad, ringPad, ringPad, ringPad)
                } else {
                    setPadding(ringPad, ringPad, ringPad, ringPad)
                }
            }

            val circle = View(this).apply {
                layoutParams = FrameLayout.LayoutParams(swatchSize, swatchSize, Gravity.CENTER)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(paletteColor)
                }
            }
            container.addView(circle)

            if (isSelected) {
                val checkIcon = ImageView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(checkSize, checkSize, Gravity.CENTER)
                    setImageResource(R.drawable.ic_check_rounded)
                    val luminance = 0.299 * Color.red(paletteColor) +
                        0.587 * Color.green(paletteColor) +
                        0.114 * Color.blue(paletteColor)
                    setColorFilter(if (luminance > 186) Color.BLACK else Color.WHITE)
                }
                container.addView(checkIcon)
            }

            container.setOnClickListener {
                prefs.edit { putInt(KEY_COLOR_PALETTE, i) }
                ThemeHelper.applyTheme(this@SettingsActivity)
                if (prefs.getBoolean("sync_icon_palette", true)) {
                    ThemeHelper.updateAppIcon(this@SettingsActivity, i)
                }
                recreate()
            }
            llColorPalette.addView(container)
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

            checkNotNull(contentResolver.openOutputStream(uri)) { "Could not open the selected backup destination" }.use { os: OutputStream ->
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
                    checkNotNull(contentResolver.openInputStream(uri)) { "Could not open the selected backup file" }.use { isStream: InputStream ->
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

        private val PALETTE_COLOR_RES_IDS = intArrayOf(
            R.color.palette_teal_primary,
            R.color.palette_blue_primary,
            R.color.palette_green_primary,
            R.color.palette_purple_primary,
            R.color.palette_red_primary,
            R.color.palette_amber_primary,
            R.color.palette_indigo_primary,
            R.color.palette_pink_primary,
            R.color.palette_onyx_primary,
            R.color.palette_yellow_primary,
            R.color.palette_cyan_primary,
            R.color.palette_brown_primary,
            R.color.palette_grey_primary
        )
    }
}
