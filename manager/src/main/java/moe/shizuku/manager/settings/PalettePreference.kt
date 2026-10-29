package moe.shizuku.manager.settings

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import moe.shizuku.manager.R
import moe.shizuku.manager.app.Palette
import moe.shizuku.manager.app.ThemeHelper
import rikka.core.res.isNight
import rikka.core.res.resolveColor

/**
 * Settings row that shows every available [Palette] as a color swatch.
 * Picking one stores it ([ThemeHelper.setPalette]) and calls [onPaletteChanged],
 * which recreates the activity so the new colors apply.
 */
class PalettePreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : Preference(context, attrs) {

    var onPaletteChanged: ((Palette) -> Unit)? = null

    init {
        layoutResource = R.layout.preference_palette
        isSelectable = false
        isPersistent = false
        updateSummary()
    }

    private fun updateSummary() {
        summary = context.getString(ThemeHelper.getPalette().title)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.isClickable = false

        val container = holder.findViewById(R.id.palette_swatches) as LinearLayout
        container.removeAllViews()

        val context = container.context
        val night = context.resources.configuration.isNight()
        val current = ThemeHelper.getPalette()
        val inflater = LayoutInflater.from(context)

        for (palette in Palette.available()) {
            val item = inflater.inflate(R.layout.preference_palette_item, container, false)
            val swatch = item.findViewById<ImageView>(R.id.swatch)
            val label = item.findViewById<TextView>(R.id.label)
            val color = palette.swatchColor(context, night)
            val selected = palette == current
            val name = context.getString(palette.title)

            swatch.background = swatchDrawable(context, color, selected)
            if (selected) {
                swatch.setImageResource(R.drawable.ic_check_24)
                // black or white check, whichever reads better on the swatch
                val onColor = if (ColorUtils.calculateLuminance(color) > 0.4) 0xFF131110.toInt() else 0xFFFFFFFF.toInt()
                swatch.imageTintList = ColorStateList.valueOf(onColor)
            } else {
                swatch.setImageDrawable(null)
            }

            label.text = name
            item.isSelected = selected
            item.contentDescription = name
            ViewCompat.setStateDescription(
                item, context.getString(if (selected) R.string.palette_state_selected else R.string.palette_state_not_selected)
            )
            item.setOnClickListener {
                if (ThemeHelper.getPalette() != palette) {
                    ThemeHelper.setPalette(palette)
                    updateSummary()
                    onPaletteChanged?.invoke(palette)
                }
            }
            container.addView(item)
        }
    }

    private fun swatchDrawable(context: Context, color: Int, selected: Boolean): Drawable {
        val density = context.resources.displayMetrics.density
        val ring = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            if (selected) {
                setStroke((2 * density).toInt(), context.theme.resolveColor(com.google.android.material.R.attr.colorOnSurface))
            } else {
                // hairline so light swatches stay visible on light grounds
                setStroke((1 * density).toInt(), context.theme.resolveColor(com.google.android.material.R.attr.colorOutlineVariant))
            }
        }
        val fill = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
        return LayerDrawable(arrayOf(ring, fill)).apply {
            val inset = ((if (selected) 5 else 4) * density).toInt()
            setLayerInset(1, inset, inset, inset, inset)
            if (!selected) setLayerInset(0, inset - (1 * density).toInt(), inset - (1 * density).toInt(), inset - (1 * density).toInt(), inset - (1 * density).toInt())
        }
    }
}
