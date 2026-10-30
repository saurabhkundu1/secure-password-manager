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
    private val themeIcons = intArrayOf(
        R.mipmap.ic_launcher_teal, R.mipmap.ic_launcher_blue, R.mipmap.ic_launcher_green,
        R.mipmap.ic_launcher_purple, R.mipmap.ic_launcher_red, R.mipmap.ic_launcher_amber,
        R.mipmap.ic_launcher_indigo, R.mipmap.ic_launcher_pink, R.mipmap.ic_launcher_onyx,
        R.mipmap.ic_launcher_yellow, R.mipmap.ic_launcher_cyan, R.mipmap.ic_launcher_brown,
        R.mipmap.ic_launcher_grey
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
        return themeIcons.getOrElse(palette) { themeIcons[0] }
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

        val activeAlias = "$pkg.MainActivity${iconAliases.getOrElse(iconIndex) { iconAliases[0] }}"
        pm.setComponentEnabledSetting(
            ComponentName(pkg, activeAlias),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        for (suffix in iconAliases) {
            val alias = "$pkg.MainActivity$suffix"
            if (alias != activeAlias) {
                pm.setComponentEnabledSetting(
                    ComponentName(pkg, alias),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }

    @JvmStatic
    fun applyThemeFromPreferences(prefs: SharedPreferences) {
        val mode = prefs.getInt(KEY_THEME_MODE, 2)
        applyThemeMode(mode)
    }
}
