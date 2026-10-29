package moe.shizuku.manager.app

import android.content.Context
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import moe.shizuku.manager.R

/**
 * Color palettes the user can pick in Settings > Appearance.
 *
 * Each palette is a pair of theme overlays (light, dark) that only set Material 3
 * color roles, applied on top of the base theme by [AppActivity]:
 * - [BUGRA] is hand-written from the Bugra design system (default),
 * - [SYSTEM] is Material You dynamic color (Android 12+),
 * - the rest are generated from one seed color by materialThemeBuilder (manager/build.gradle).
 */
enum class Palette(
    val id: String,
    @StringRes val title: Int,
    @StyleRes private val lightOverlay: Int,
    @StyleRes private val darkOverlay: Int,
    @ColorRes private val swatchLight: Int,
    @ColorRes private val swatchDark: Int,
) {
    BUGRA(
        "bugra", R.string.palette_bugra,
        R.style.ThemeOverlay_Palette_Bugra_Light, R.style.ThemeOverlay_Palette_Bugra_Dark,
        R.color.bugra_accent, R.color.bugra_accent
    ),
    SYSTEM(
        "system", R.string.palette_system,
        R.style.ThemeOverlay_DynamicColors_Light, R.style.ThemeOverlay_DynamicColors_Dark,
        R.color.bugra_accent, R.color.bugra_accent
    ),
    INDIGO(
        "indigo", R.string.palette_indigo,
        R.style.ThemeOverlay_Palette_Indigo_Light, R.style.ThemeOverlay_Palette_Indigo_Dark,
        R.color.md_theme_indigo_light_primary, R.color.md_theme_indigo_dark_primary
    ),
    OCEAN(
        "ocean", R.string.palette_ocean,
        R.style.ThemeOverlay_Palette_Ocean_Light, R.style.ThemeOverlay_Palette_Ocean_Dark,
        R.color.md_theme_ocean_light_primary, R.color.md_theme_ocean_dark_primary
    ),
    TEAL(
        "teal", R.string.palette_teal,
        R.style.ThemeOverlay_Palette_Teal_Light, R.style.ThemeOverlay_Palette_Teal_Dark,
        R.color.md_theme_teal_light_primary, R.color.md_theme_teal_dark_primary
    ),
    VIOLET(
        "violet", R.string.palette_violet,
        R.style.ThemeOverlay_Palette_Violet_Light, R.style.ThemeOverlay_Palette_Violet_Dark,
        R.color.md_theme_violet_light_primary, R.color.md_theme_violet_dark_primary
    ),
    ROSE(
        "rose", R.string.palette_rose,
        R.style.ThemeOverlay_Palette_Rose_Light, R.style.ThemeOverlay_Palette_Rose_Dark,
        R.color.md_theme_rose_light_primary, R.color.md_theme_rose_dark_primary
    ),
    AMBER(
        "amber", R.string.palette_amber,
        R.style.ThemeOverlay_Palette_Amber_Light, R.style.ThemeOverlay_Palette_Amber_Dark,
        R.color.md_theme_amber_light_primary, R.color.md_theme_amber_dark_primary
    ),
    GRAPHITE(
        "graphite", R.string.palette_graphite,
        R.style.ThemeOverlay_Palette_Graphite_Light, R.style.ThemeOverlay_Palette_Graphite_Dark,
        R.color.md_theme_graphite_light_primary, R.color.md_theme_graphite_dark_primary
    );

    @StyleRes
    fun overlay(night: Boolean): Int = if (night) darkOverlay else lightOverlay

    /** Color of the swatch shown in the picker. */
    @ColorInt
    fun swatchColor(context: Context, night: Boolean): Int {
        if (this == SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return context.getColor(
                if (night) android.R.color.system_accent1_200 else android.R.color.system_accent1_600
            )
        }
        return context.getColor(if (night) swatchDark else swatchLight)
    }

    companion object {

        val DEFAULT = BUGRA

        /** Palettes available on this device (Material You needs Android 12). */
        fun available(): List<Palette> = entries.filter {
            it != SYSTEM || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        }

        fun fromId(id: String?): Palette {
            val palette = entries.firstOrNull { it.id == id } ?: DEFAULT
            return if (palette in available()) palette else DEFAULT
        }
    }
}
