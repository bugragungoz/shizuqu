package moe.shizuku.manager.app;

import android.content.Context;

import androidx.annotation.StyleRes;

import moe.shizuku.manager.R;
import moe.shizuku.manager.ShizukuSettings;
import moe.shizuku.manager.utils.EnvironmentUtils;
import rikka.core.util.ResourceUtils;

public class ThemeHelper {

    private static final String THEME_DEFAULT = "DEFAULT";
    private static final String THEME_BLACK = "BLACK";

    public static final String KEY_LIGHT_THEME = "light_theme";
    public static final String KEY_BLACK_NIGHT_THEME = "black_night_theme";

    public static boolean isBlackNightTheme(Context context) {
        return ShizukuSettings.getPreferences().getBoolean(KEY_BLACK_NIGHT_THEME, EnvironmentUtils.isWatch(context));
    }

    /** Preference key of the color palette; values are {@link Palette#getId()}. */
    public static final String KEY_COLOR_PALETTE = "color_palette";

    /*
     * The old "use system theme color" switch (use_system_color) is not migrated: the
     * switch saved its default the first time Settings was opened, so its value does not
     * tell a choice, and upstream installs cannot be updated in place anyway (different
     * signature). Everyone starts with Palette.DEFAULT.
     */
    public static Palette getPalette() {
        return Palette.Companion.fromId(ShizukuSettings.getPreferences().getString(KEY_COLOR_PALETTE, null));
    }

    public static void setPalette(Palette palette) {
        ShizukuSettings.getPreferences().edit().putString(KEY_COLOR_PALETTE, palette.getId()).apply();
    }

    public static String getTheme(Context context) {
        if (isBlackNightTheme(context)
                && ResourceUtils.isNightMode(context.getResources().getConfiguration()))
            return THEME_BLACK;

        return ShizukuSettings.getPreferences().getString(KEY_LIGHT_THEME, THEME_DEFAULT);
    }

    @StyleRes
    public static int getThemeStyleRes(Context context) {
        switch (getTheme(context)) {
            case THEME_BLACK:
                return R.style.ThemeOverlay_Black;
            case THEME_DEFAULT:
            default:
                return R.style.ThemeOverlay;
        }
    }
}
