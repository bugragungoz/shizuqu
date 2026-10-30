package moe.shizuku.manager.screenshots

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.android.resources.NightMode
import com.google.android.material.appbar.MaterialToolbar
import androidx.preference.PreferenceViewHolder
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.app.Palette
import moe.shizuku.manager.settings.PalettePreference
import moe.shizuku.manager.utils.EmptySharedPreferencesImpl
import moe.shizuku.manager.databinding.AboutDialogBinding
import moe.shizuku.manager.databinding.AdbPairingTutorialActivityBinding
import moe.shizuku.manager.databinding.ConfirmationDialogBinding
import moe.shizuku.manager.databinding.HomeExtraStepRequiredBinding
import moe.shizuku.manager.databinding.HomeStartAdbBinding
import moe.shizuku.manager.databinding.HomeStartRootBinding
import moe.shizuku.manager.databinding.TerminalTutorialActivityBinding
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import moe.shizuku.manager.databinding.HomeLearnMoreBinding
import moe.shizuku.manager.databinding.HomeManageAppsItemBinding
import moe.shizuku.manager.databinding.HomeServerStatusBinding
import moe.shizuku.manager.databinding.HomeStartWirelessAdbBinding
import moe.shizuku.manager.databinding.HomeTerminalBinding
import org.junit.Rule
import org.junit.Test

/**
 * Renders the real layouts and themes with layoutlib (no device) so design changes can be
 * reviewed as images. Opt-in: ./gradlew :manager:recordPaparazziDebug -Pscreenshots
 * Images: manager/src/test/snapshots/images/
 */
class DesignScreenshots {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        theme = "AppTheme",
        renderingMode = SessionParams.RenderingMode.SHRINK,
        showSystemUi = false,
    )

    private fun night(on: Boolean) {
        paparazzi.unsafeUpdateConfig(
            deviceConfig = DeviceConfig.PIXEL_6.copy(nightMode = if (on) NightMode.NIGHT else NightMode.NOTNIGHT)
        )
    }

    private fun themed(palette: Palette, night: Boolean): Context =
        ContextThemeWrapper(paparazzi.context, R.style.AppTheme).apply {
            theme.applyStyle(palette.overlay(night), true)
        }

    private fun Context.attrColor(attr: Int): Int {
        val value = android.util.TypedValue()
        theme.resolveAttribute(attr, value, true)
        return if (value.resourceId != 0) getColor(value.resourceId) else value.data
    }

    private fun dp(context: Context, v: Int) = (v * context.resources.displayMetrics.density).toInt()

    private fun card(inflater: LayoutInflater, parent: ViewGroup, inner: (LayoutInflater, ViewGroup) -> Unit): View {
        val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
        inner(inflater, outer.root)
        (outer.root.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin = dp(parent.context, 12)
        return outer.root
    }

    private fun homeScreen(context: Context): View {
        val inflater = LayoutInflater.from(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(context.attrColor(android.R.attr.colorBackground))
            layoutParams = ViewGroup.LayoutParams(dp(context, 412), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val toolbar = MaterialToolbar(context).apply {
            title = context.getString(R.string.app_name)
            inflateMenu(R.menu.main)
        }
        root.addView(toolbar)
        val list = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(context, 16)
            setPadding(pad, dp(context, 8), pad, pad)
        }
        root.addView(list)

        list.addView(card(inflater, list) { i, p ->
            val b = HomeServerStatusBinding.inflate(i, p, true)
            b.icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_server_ok_24dp))
            b.text1.text = context.getString(R.string.home_status_service_is_running, context.getString(R.string.app_name))
            b.text2.text = context.getString(R.string.home_status_service_version, "adb", "13.7")
        })
        list.addView(card(inflater, list) { i, p ->
            val b = HomeManageAppsItemBinding.inflate(i, p, true)
            b.text1.text = context.resources.getQuantityString(R.plurals.home_app_management_authorized_apps_count, 3, 3)
        })
        list.addView(card(inflater, list) { i, p ->
            val b = HomeStartWirelessAdbBinding.inflate(i, p, true)
            b.text1.text = androidx.core.text.HtmlCompat.fromHtml(
                context.getString(R.string.home_wireless_adb_description), androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
            )
        })
        list.addView(card(inflater, list) { i, p -> HomeTerminalBinding.inflate(i, p, true) })
        list.addView(card(inflater, list) { i, p -> HomeLearnMoreBinding.inflate(i, p, true) })
        return root
    }

    @Test
    fun home_bugra_dark() {
        night(true)
        paparazzi.snapshot(homeScreen(themed(Palette.BUGRA, true)))
    }

    @Test
    fun home_bugra_light() {
        night(false)
        paparazzi.snapshot(homeScreen(themed(Palette.BUGRA, false)))
    }

    @Test
    fun permission_dialog_bugra_dark() {
        night(true)
        val context = themed(Palette.BUGRA, true)
        val b = ConfirmationDialogBinding.inflate(LayoutInflater.from(context))
        b.title.text = androidx.core.text.HtmlCompat.fromHtml(
            context.getString(R.string.permission_warning_template, "Example app", context.getString(R.string.permission_group_description)),
            androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
        )
        b.root.setBackgroundColor(context.attrColor(com.google.android.material.R.attr.colorSurfaceContainerHigh))
        b.root.layoutParams = ViewGroup.LayoutParams(dp(context, 360), ViewGroup.LayoutParams.WRAP_CONTENT)
        paparazzi.snapshot(FrameLayout(context).apply { addView(b.root) })
    }

    @Test
    fun starter_log_bugra_dark() {
        night(true)
        val context = themed(Palette.BUGRA, true)
        val view = LayoutInflater.from(context).inflate(R.layout.starter_activity, null)
        view.findViewById<TextView>(android.R.id.text1).text = """
            Starting with wireless adb in port 37241...

            info: starter begin
            info: killing old process...
            info: apk path is /data/app/~~x/moe.shizuku.privileged.api-1/base.apk
            info: cmd: CLASSPATH=... app_process -Djava.class.path=... /system/bin --nice-name=shizuku_server rikka.shizuku.server.ShizukuService
            info: shizuku_server pid is 12345
            info: shizuku_starter exit with 0
        """.trimIndent()
        view.setBackgroundColor(context.attrColor(android.R.attr.colorBackground))
        view.layoutParams = ViewGroup.LayoutParams(dp(context, 412), dp(context, 420))
        paparazzi.snapshot(view)
    }

    @Test
    fun settings_palette_bugra_dark() = settingsPalette(true)

    @Test
    fun settings_palette_bugra_light() = settingsPalette(false)

    /** Settings > User interface, with the color palette picker as SettingsActivity themes it. */
    private fun settingsPalette(night: Boolean) {
        night(night)
        val context = themed(Palette.BUGRA, night).apply {
            theme.applyStyle(R.style.ThemeOverlay_Shizuqu_Preference, true)
        }
        // layoutlib has no device-protected storage: read defaults (palette = Bugra)
        ShizukuSettings::class.java.getDeclaredField("sPreferences").apply { isAccessible = true }
            .set(null, EmptySharedPreferencesImpl())

        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(context.attrColor(android.R.attr.colorBackground))
            layoutParams = ViewGroup.LayoutParams(dp(context, 412), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val category = TextView(context).apply {
            text = context.getString(R.string.settings_user_interface)
            val ta = android.util.TypedValue()
            context.theme.resolveAttribute(androidx.preference.R.attr.preferenceTheme, ta, true)
            val prefTheme = ContextThemeWrapper(context, ta.resourceId)
            prefTheme.theme.resolveAttribute(androidx.preference.R.attr.preferenceCategoryTitleTextAppearance, ta, true)
            setTextAppearance(ta.resourceId)
            setTextColor(context.attrColor(com.google.android.material.R.attr.colorPrimary))
            setPadding(dp(context, 24), dp(context, 24), dp(context, 24), dp(context, 8))
        }
        column.addView(category)

        val preference = PalettePreference(context).apply { title = context.getString(R.string.settings_color_palette) }
        val row = LayoutInflater.from(context).inflate(preference.layoutResource, column, false)
        preference.onBindViewHolder(PreferenceViewHolder.createInstanceForTests(row))
        column.addView(row)
        paparazzi.snapshot(column)
    }

    // layoutlib cannot inflate <adaptive-icon> (no system icon mask), so stack its layers
    private fun launcherDrawable(context: Context) = android.graphics.drawable.LayerDrawable(arrayOf(
        ContextCompat.getDrawable(context, R.drawable.ic_launcher_background)!!,
        ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)!!,
    ))

    /** Launcher icon in the common masks and the themed icon (the small icon is in the permission dialog). */
    @Test
    fun icon() {
        night(false)
        val context = themed(Palette.BUGRA, false)
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFFEEF0F4.toInt())
            setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16))
        }
        fun launcher() = launcherDrawable(context)
        fun masked(drawable: Int, radiusFraction: Float, size: Int, background: Int? = null, tint: Int? = null) =
            FrameLayout(context).apply {
                val px = dp(context, size)
                layoutParams = LinearLayout.LayoutParams(px, px).apply { marginEnd = dp(context, 16) }
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, view.width * radiusFraction)
                    }
                }
                clipToOutline = true
                background?.let { setBackgroundColor(it) }
                addView(android.widget.ImageView(context).apply {
                    setImageDrawable(
                        if (drawable == R.drawable.ic_launcher) launcher()
                        else ContextCompat.getDrawable(context, drawable)!!.mutate().apply { tint?.let { setTint(it) } }
                    )
                    scaleType = android.widget.ImageView.ScaleType.FIT_XY
                    // adaptive icons draw 108dp with the outer 18dp outside the mask
                    val inset = if (drawable == R.drawable.ic_launcher || drawable == R.drawable.ic_monochrome) -px / 4 else 0
                    layoutParams = FrameLayout.LayoutParams(px - 2 * inset, px - 2 * inset).apply {
                        leftMargin = inset; topMargin = inset
                    }
                })
            }
        row.addView(masked(R.drawable.ic_launcher, 0.5f, 96))
        row.addView(masked(R.drawable.ic_launcher, 0.3f, 96))
        row.addView(masked(R.drawable.ic_launcher, 0.3f, 48))
        row.addView(masked(R.drawable.ic_monochrome, 0.5f, 72, background = 0xFF2E3A2F.toInt(), tint = 0xFFD9F2DC.toInt()))
        paparazzi.snapshot(row)
    }

    /**
     * The start cards not on the main home screenshot: root, computer (adb), adb limited.
     * Two images, because a render taller than the device screen squeezes the last card.
     */
    @Test
    fun home_other_cards_bugra_dark() = otherCards(root = true)

    @Test
    fun home_other_cards_2_bugra_dark() = otherCards(root = false)

    private fun otherCards(root: Boolean) {
        night(true)
        val context = themed(Palette.BUGRA, true)
        val inflater = LayoutInflater.from(context)
        val list = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(context.attrColor(android.R.attr.colorBackground))
            layoutParams = ViewGroup.LayoutParams(dp(context, 412), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        if (root) list.addView(card(inflater, list) { i, p ->
            HomeStartRootBinding.inflate(i, p, true).text1.text = context.getString(R.string.home_root_description, "<b>Sui</b>")
                .let { androidx.core.text.HtmlCompat.fromHtml(it, androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT) }
        })
        if (root) list.addView(card(inflater, list) { i, p ->
            HomeStartAdbBinding.inflate(i, p, true).text1.text = context.getString(R.string.home_adb_description, "")
                .let { androidx.core.text.HtmlCompat.fromHtml(it, androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT) }
        })
        if (!root) list.addView(card(inflater, list) { i, p -> HomeExtraStepRequiredBinding.inflate(i, p, true) })
        paparazzi.snapshot(list)
    }

    @Test
    fun about_dialog_bugra_dark() {
        night(true)
        val context = themed(Palette.BUGRA, true)
        val b = AboutDialogBinding.inflate(LayoutInflater.from(context))
        b.icon.setImageDrawable(launcherDrawable(context))
        b.versionName.text = "13.7.0.r1120.abcdef12"
        b.sourceCode.text = androidx.core.text.HtmlCompat.fromHtml(
            context.getString(R.string.about_view_source_code, "<b><a href=\"https://github.com/\">GitHub</a></b>") + "<br>" +
                context.getString(R.string.about_based_on_upstream, "<b><a href=\"https://github.com/RikkaApps/Shizuku\">Shizuku</a></b>"),
            androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
        )
        b.root.setBackgroundColor(context.attrColor(com.google.android.material.R.attr.colorSurfaceContainerHigh))
        b.root.layoutParams = ViewGroup.LayoutParams(dp(context, 360), ViewGroup.LayoutParams.WRAP_CONTENT)
        paparazzi.snapshot(FrameLayout(context).apply { addView(b.root) })
    }

    @Test
    fun wireless_tutorial_bugra_dark() {
        night(true)
        val context = themed(Palette.BUGRA, true)
        val b = AdbPairingTutorialActivityBinding.inflate(LayoutInflater.from(context))
        b.root.setBackgroundColor(context.attrColor(android.R.attr.colorBackground))
        b.root.layoutParams = ViewGroup.LayoutParams(dp(context, 412), dp(context, 900))
        paparazzi.snapshot(b.root)
    }

    @Test
    fun terminal_tutorial_bugra_dark() {
        night(true)
        val context = themed(Palette.BUGRA, true)
        val b = TerminalTutorialActivityBinding.inflate(LayoutInflater.from(context))
        b.root.setBackgroundColor(context.attrColor(android.R.attr.colorBackground))
        b.root.layoutParams = ViewGroup.LayoutParams(dp(context, 412), dp(context, 900))
        paparazzi.snapshot(b.root)
    }

    @Test
    fun palettes_dark() = palettes(true)

    @Test
    fun palettes_light() = palettes(false)

    private fun palettes(night: Boolean) {
        night(night)
        val base = paparazzi.context
        val stack = LinearLayout(base).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(dp(base, 380), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        for (palette in Palette.entries.filter { it != Palette.SYSTEM }) {
            val context = themed(palette, night)
            val inflater = LayoutInflater.from(context)
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(context.attrColor(android.R.attr.colorBackground))
                setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 4))
            }
            row.addView(TextView(context).apply {
                text = context.getString(palette.title)
                setTextAppearance(R.style.TextAppearance_Shizuqu_TitleSmall)
                setTextColor(context.attrColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, 0, 0, dp(context, 8))
            })
            row.addView(card(inflater, row) { i, p ->
                val b = HomeServerStatusBinding.inflate(i, p, true)
                b.icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_server_ok_24dp))
                b.text1.text = context.getString(R.string.home_status_service_is_running, context.getString(R.string.app_name))
                b.text2.text = context.getString(R.string.home_status_service_version, "adb", "13.7")
            })
            val buttons = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            buttons.addView(com.google.android.material.button.MaterialButton(
                ContextThemeWrapper(context, 0), null, com.google.android.material.R.attr.materialButtonStyle
            ).apply { text = context.getString(R.string.home_root_button_start) })
            buttons.addView(View(context), LinearLayout.LayoutParams(dp(context, 8), 1))
            buttons.addView(com.google.android.material.button.MaterialButton(
                context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply { text = context.getString(R.string.adb_pairing) })
            row.addView(buttons)
            stack.addView(row)
        }
        paparazzi.snapshot(stack)
    }
}
