/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.qsb

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.view.View
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.R
import com.android.launcher3.util.Themes
import kotlin.math.roundToInt

/** Shared visual styling for the Home and All Apps search bars. */
object SearchBarUI {
    /** Applies Home QSB colors, transparency, corner radius, and stroke to [view]. */
    @JvmStatic
    @JvmOverloads
    fun applyBackground(view: View, insetVerticalPadding: Boolean = false) {
        view.background = createBackground(view.context, insetVerticalPadding)
    }

    /** Creates the common search bar background, optionally inset like the All Apps field. */
    @JvmStatic
    @JvmOverloads
    fun createBackground(context: Context, insetVerticalPadding: Boolean = false): Drawable {
        val prefs = LauncherPrefs.get(context)
        val themed = prefs.get(LauncherPrefs.HOTSEAT_QSB_THEMED)
        val nightMode = context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val baseColor = if (themed) {
            LauncherExpressiveSearchColors.container(context)
        } else if (nightMode) {
            0xFF202124.toInt()
        } else {
            Color.WHITE
        }
        val alpha = (prefs.get(LauncherPrefs.HOTSEAT_QSB_ALPHA) * 2.55f).roundToInt()
        val shape = GradientDrawable().apply {
            setColor(
                Color.argb(
                    alpha,
                    Color.red(baseColor),
                    Color.green(baseColor),
                    Color.blue(baseColor),
                ),
            )
            val height = context.resources.getDimension(R.dimen.uwu_qsb_widget_height)
            val verticalPadding = context.resources
                .getDimension(R.dimen.uwu_qsb_widget_vertical_padding)
            cornerRadius = (height - 2 * verticalPadding) / 2 *
                prefs.get(LauncherPrefs.HOTSEAT_QSB_CORNER_RADIUS)

            val strokeWidth = prefs.get(LauncherPrefs.HOTSEAT_QSB_STROKE_WIDTH)
            if (strokeWidth > 0f) {
                val configuredStrokeColor = prefs.get(LauncherPrefs.HOTSEAT_QSB_STROKE_COLOR)
                setStroke(
                    strokeWidth.roundToInt(),
                    if (configuredStrokeColor == 0) Themes.getColorAccent(context)
                    else configuredStrokeColor,
                )
            }
        }

        if (!insetVerticalPadding) return shape
        val padding = context.resources
            .getDimensionPixelSize(R.dimen.uwu_qsb_widget_vertical_padding)
        return InsetDrawable(shape, 0, padding, 0, padding)
    }
}
