/*
 * Ophaaldag - a Material 3 client for the Cure Afvalbeheer waste calendar.
 * Copyright (C) 2026 Pere Gomila
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.pegoku.ophaaldag.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue

/** Flat colour-coded dots, drawn once per variant and reused across every marker of that stream. */
object MarkerIcons {

    /** Material's on-surface dark, for the count drawn on the white centre. */
    private val LABEL_COLOR = 0xFF1C1B1F.toInt()

    private val cache = HashMap<String, Drawable>()

    /** A container pin. [selected] draws it larger with a hollow centre so the tapped one stands out. */
    fun dot(context: Context, colorArgb: Int, selected: Boolean = false): Drawable =
        cache.getOrPut("dot-$colorArgb-$selected") {
            val size = dp(context, if (selected) 26f else 18f)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val c = size / 2f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // White ring first, so the dot stays legible against dark roads and water.
            paint.color = Color.WHITE
            canvas.drawCircle(c, c, c - dp(context, 1f) / 2f, paint)
            paint.color = colorArgb
            canvas.drawCircle(c, c, c - dp(context, if (selected) 4f else 3f), paint)
            if (selected) {
                paint.color = Color.WHITE
                canvas.drawCircle(c, c, c - dp(context, 9f), paint)
            }
            BitmapDrawable(context.resources, bitmap)
        }

    /**
     * A pin standing for several containers at one spot: a ring split into one wedge per waste
     * stream present, with the count in the middle. Reading the colours tells you what is there
     * without tapping.
     */
    fun cluster(context: Context, colorsArgb: List<Int>, count: Int, selected: Boolean = false): Drawable {
        if (count <= 1) return dot(context, colorsArgb.firstOrNull() ?: Color.GRAY, selected)
        return cache.getOrPut("cluster-${colorsArgb.joinToString("-")}-$count-$selected") {
            val size = dp(context, if (selected) 38f else 32f)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val c = size / 2f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            paint.color = Color.WHITE
            canvas.drawCircle(c, c, c, paint)

            val inset = dp(context, 2f).toFloat()
            val box = RectF(inset, inset, size - inset, size - inset)
            val sweep = 360f / colorsArgb.size
            colorsArgb.forEachIndexed { index, color ->
                paint.color = color
                // -90 so the first wedge starts at the top; a hair of overlap hides seam artefacts.
                canvas.drawArc(box, -90f + index * sweep, sweep + 0.5f, true, paint)
            }

            paint.color = Color.WHITE
            canvas.drawCircle(c, c, c - dp(context, if (selected) 8f else 7f), paint)

            paint.color = LABEL_COLOR
            paint.textAlign = Paint.Align.CENTER
            paint.isFakeBoldText = true
            paint.textSize = dp(context, if (selected) 14f else 12f).toFloat()
            val baseline = c - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(count.toString(), c, baseline, paint)

            BitmapDrawable(context.resources, bitmap)
        }
    }

    /** The user's own address: a hollow ring, so it never reads as another container. */
    fun home(context: Context, colorArgb: Int): Drawable =
        cache.getOrPut("home-$colorArgb") {
            val size = dp(context, 20f)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val c = size / 2f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.WHITE
            canvas.drawCircle(c, c, c, paint)
            paint.color = colorArgb
            canvas.drawCircle(c, c, c - dp(context, 2f), paint)
            paint.color = Color.WHITE
            canvas.drawCircle(c, c, c - dp(context, 6f), paint)
            BitmapDrawable(context.resources, bitmap)
        }

    private fun dp(context: Context, value: Float): Int = TypedValue
        .applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics)
        .toInt()
}
