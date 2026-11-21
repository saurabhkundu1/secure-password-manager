package com.applify.securepass

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {
    private const val KEY_THEME_MODE = "theme_mode" // 0=light, 1=dark, 2=system
    private const val KEY_COLOR_PALETTE = "color_palette" // 0=teal, 1=blue, 2=green, 3=purple, 4=red, 5=amber, ...

    val iconOptions = arrayOf(
        "Teal (Default)", "Classic Blue", "Forest Green", "Royal Purple", "Crimson Red", "Amber Gold",
        "Indigo", "Pink", "Onyx", "Yellow", "Cyan", "Brown", "Grey"
    )

    private val iconAliases = arrayOf(
        "Teal", "Blue", "Green", "Purple", "Red", "Amber", "Indigo",
        "Pink", "Onyx", "Yellow", "Cyan", "Brown", "Grey"
    )
    private val themeDrawables = intArrayOf(
        R.drawable.securepass_foreground_teal, R.drawable.securepass_foreground_blue, R.drawable.securepass_foreground_green,
        R.drawable.securepass_foreground_purple, R.drawable.securepass_foreground_red, R.drawable.securepass_foreground_amber,
        R.drawable.securepass_foreground_indigo, R.drawable.securepass_foreground_pink, R.drawable.securepass_foreground_onyx,
        R.drawable.securepass_foreground_yellow, R.drawable.securepass_foreground_cyan, R.drawable.securepass_foreground_brown,
        R.drawable.securepass_foreground_grey
    )

    /**
     * Applies the saved theme mode (light/dark/system) and color palette to the activity.
     * This must be called BEFORE super.onCreate() and setContentView().
     */
    @JvmStatic
    fun applyTheme(activity: Activity) {
        val prefs = activity.getSharedPreferences("secure_pass_prefs", Context.MODE_PRIVATE)

        // 1. Apply Theme Mode (Night/Day)
        val mode = prefs.getInt(KEY_THEME_MODE, 2) // default system
        applyThemeMode(mode)

        // 2. Apply Color Palette Theme
        val palette = prefs.getInt(KEY_COLOR_PALETTE, 0) // default teal
        activity.setTheme(getPaletteTheme(palette))
    }

    /**
     * Helper to apply just the theme mode. Useful when changing settings.
     */
    @JvmStatic
    fun applyThemeMode(mode: Int) {
        when (mode) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun getPaletteTheme(palette: Int): Int {
        return when (palette) {
            1 -> R.style.Theme_SecurePass_Blue
            2 -> R.style.Theme_SecurePass_Green
            3 -> R.style.Theme_SecurePass_Purple
            4 -> R.style.Theme_SecurePass_Red
            5 -> R.style.Theme_SecurePass_Amber
            6 -> R.style.Theme_SecurePass_Indigo
            7 -> R.style.Theme_SecurePass_Pink
            8 -> R.style.Theme_SecurePass_Onyx
            9 -> R.style.Theme_SecurePass_Yellow
            10 -> R.style.Theme_SecurePass_Cyan
            11 -> R.style.Theme_SecurePass_Brown
            12 -> R.style.Theme_SecurePass_Grey
            else -> R.style.Theme_SecurePass_Teal
        }
    }

    /**
     * Returns the matching theme icon drawable resource ID for the active color palette.
     */
    @JvmStatic
    fun getThemeIconResId(palette: Int): Int {
        return themeDrawables.getOrElse(palette) { themeDrawables[0] }
    }

    /**
     * Helper to get the theme icon resource ID for the currently saved theme palette.
     */
    @JvmStatic
    fun getCurrentThemeIconResId(context: Context): Int {
        val prefs = context.getSharedPreferences("secure_pass_prefs", Context.MODE_PRIVATE)
        val palette = prefs.getInt(KEY_COLOR_PALETTE, 0)
        return getThemeIconResId(palette)
    }

    /**
     * Dynamically changes the app launcher icon based on the selected palette index or icon index.
     */
    @JvmStatic
    fun updateAppIcon(context: Context, iconIndex: Int) {
        val pm = context.packageManager
        val pkg = context.packageName

        val targetSuffix = iconAliases.getOrElse(iconIndex) { iconAliases[0] }
        val targetAlias = "$pkg.MainActivity$targetSuffix"

        val targetComponent = ComponentName(pkg, targetAlias)
        val currentSetting = pm.getComponentEnabledSetting(targetComponent)

        // If the target alias is already the enabled component, do nothing.
        // This avoids unnecessary launcher broadcasts that can drop home-screen shortcuts.
        if (currentSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            return
        }

        // Enable the new target alias first so there is always a valid launcher activity
        pm.setComponentEnabledSetting(
            targetComponent,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // Disable all other aliases only if they are not already disabled
        for (suffix in iconAliases) {
            if (suffix != targetSuffix) {
                val aliasComponent = ComponentName(pkg, "$pkg.MainActivity$suffix")
                if (pm.getComponentEnabledSetting(aliasComponent) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    pm.setComponentEnabledSetting(
                        aliasComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
        }
    }

    @JvmStatic
    fun applyThemeFromPreferences(prefs: SharedPreferences) {
        val mode = prefs.getInt(KEY_THEME_MODE, 2)
        applyThemeMode(mode)
    }
}
