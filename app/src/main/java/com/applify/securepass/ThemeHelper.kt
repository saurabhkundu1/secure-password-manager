package com.applify.securepass

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {
    private const val KEY_THEME_MODE = "theme_mode" // 0=light, 1=dark, 2=system
    private const val KEY_COLOR_PALETTE = "color_palette" // 0=teal, 1=blue, 2=green, 3=purple, 4=red

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
     * The theme icons automatically switch between light and dark mode variants via res/drawable and res/drawable-night.
     */
    @JvmStatic
    fun getThemeIconResId(palette: Int): Int {
        return when (palette) {
            1 -> R.drawable.classic_blue
            2 -> R.drawable.forest_green
            3 -> R.drawable.royal_purple
            4 -> R.drawable.crimson_red
            5 -> R.drawable.amber_gold
            else -> R.drawable.classic_blue
        }
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
     * Dynamically changes the app launcher icon based on the selected palette.
     */
    @JvmStatic
    fun updateAppIcon(context: Context, palette: Int) {
        val pm = context.packageManager
        val pkg = context.packageName

        // Map palette index to alias name
        val activeAlias = when (palette) {
            1 -> "$pkg.MainActivityBlue"
            2 -> "$pkg.MainActivityGreen"
            3 -> "$pkg.MainActivityPurple"
            4 -> "$pkg.MainActivityRed"
            5 -> "$pkg.MainActivityAmber"
            else -> "$pkg.MainActivityBlue"
        }

        val allAliases = arrayOf(
            "$pkg.MainActivityBlue",
            "$pkg.MainActivityGreen",
            "$pkg.MainActivityPurple",
            "$pkg.MainActivityRed",
            "$pkg.MainActivityAmber"
        )

        for (alias in allAliases) {
            val state = if (alias == activeAlias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            pm.setComponentEnabledSetting(
                ComponentName(pkg, alias),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    // Deprecated but kept for compatibility if needed elsewhere temporarily
    @JvmStatic
    fun applyThemeFromPreferences(prefs: SharedPreferences) {
        val mode = prefs.getInt(KEY_THEME_MODE, 2)
        applyThemeMode(mode)
    }
}