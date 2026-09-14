package com.applify.securepass;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {
    private static final String KEY_THEME_MODE = "theme_mode";      // 0=light, 1=dark, 2=system
    private static final String KEY_COLOR_PALETTE = "color_palette"; // 0=teal, 1=blue, 2=green, 3=purple, 4=red

    /**
     * Applies the saved theme mode (light/dark/system) and color palette to the activity.
     * This must be called BEFORE super.onCreate() and setContentView().
     */
    public static void applyTheme(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("secure_pass_prefs", Context.MODE_PRIVATE);
        
        // 1. Apply Theme Mode (Night/Day)
        int mode = prefs.getInt(KEY_THEME_MODE, 2); // default system
        applyThemeMode(mode);

        // 2. Apply Color Palette Theme
        int palette = prefs.getInt(KEY_COLOR_PALETTE, 0); // default teal
        activity.setTheme(getPaletteTheme(palette));
    }

    /**
     * Helper to apply just the theme mode. Useful when changing settings.
     */
    public static void applyThemeMode(int mode) {
        switch (mode) {
            case 0:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private static int getPaletteTheme(int palette) {
        switch (palette) {
            case 1: return R.style.Theme_SecurePass_Blue;
            case 2: return R.style.Theme_SecurePass_Green;
            case 3: return R.style.Theme_SecurePass_Purple;
            case 4: return R.style.Theme_SecurePass_Red;
            case 5: return R.style.Theme_SecurePass_Amber;
            case 6: return R.style.Theme_SecurePass_Indigo;
            case 7: return R.style.Theme_SecurePass_Pink;
            case 8: return R.style.Theme_SecurePass_Onyx;
            case 9: return R.style.Theme_SecurePass_Yellow;
            case 10: return R.style.Theme_SecurePass_Cyan;
            case 11: return R.style.Theme_SecurePass_Brown;
            case 12: return R.style.Theme_SecurePass_Grey;
            default: return R.style.Theme_SecurePass_Teal;
        }
    }

    /**
     * Returns the matching theme icon drawable resource ID for the active color palette.
     * The theme icons automatically switch between light and dark mode variants via res/drawable and res/drawable-night.
     */
    public static int getThemeIconResId(int palette) {
        switch (palette) {
            case 1: return R.drawable.classic_blue;
            case 2: return R.drawable.forest_green;
            case 3: return R.drawable.royal_purple;
            case 4: return R.drawable.crimson_red;
            case 5: return R.drawable.amber_gold;
            default: return R.drawable.classic_blue;
        }
    }

    /**
     * Helper to get the theme icon resource ID for the currently saved theme palette.
     */
    public static int getCurrentThemeIconResId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("secure_pass_prefs", Context.MODE_PRIVATE);
        int palette = prefs.getInt(KEY_COLOR_PALETTE, 0);
        return getThemeIconResId(palette);
    }

    /**
     * Dynamically changes the app launcher icon based on the selected palette.
     */
    public static void updateAppIcon(Context context, int palette) {
        PackageManager pm = context.getPackageManager();
        String pkg = context.getPackageName();

        // Map palette index to alias name
        String activeAlias = pkg + ".MainActivityBlue"; // Default fallback
        switch (palette) {
            case 1: activeAlias = pkg + ".MainActivityBlue"; break;
            case 2: activeAlias = pkg + ".MainActivityGreen"; break;
            case 3: activeAlias = pkg + ".MainActivityPurple"; break;
            case 4: activeAlias = pkg + ".MainActivityRed"; break;
            case 5: activeAlias = pkg + ".MainActivityAmber"; break;
        }

        String[] allAliases = {
            pkg + ".MainActivityBlue",
            pkg + ".MainActivityGreen",
            pkg + ".MainActivityPurple",
            pkg + ".MainActivityRed",
            pkg + ".MainActivityAmber"
        };

        for (String alias : allAliases) {
            int state = alias.equals(activeAlias) ?
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                    
            pm.setComponentEnabledSetting(
                    new ComponentName(pkg, alias),
                    state,
                    PackageManager.DONT_KILL_APP
            );
        }
    }

    // Deprecated but kept for compatibility if needed elsewhere temporarily
    public static void applyThemeFromPreferences(SharedPreferences prefs) {
        int mode = prefs.getInt(KEY_THEME_MODE, 2);
        applyThemeMode(mode);
    }
}