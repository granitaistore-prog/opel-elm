package com.granitaistore.obddiagnostic.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class GaugeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var min = 0f
    var max = 100f
    var value = 0f
        set(v) {
            field = v.coerceIn(min, max)
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val r = width * 0.4f

        // arc
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 10f
        paint.color = Color.DKGRAY
        canvas.drawArc(cx - r, cy - r, cx + r, cy + r, 135f, 270f, false, paint)

        // needle
        val angle = 135f + (value / max) * 270f
        val rad = Math.toRadians(angle.toDouble())
        val x = cx + cos(rad).toFloat() * r
        val y = cy + sin(rad).toFloat() * r

        paint.color = Color.RED
        paint.strokeWidth = 12f
        canvas.drawLine(cx, cy, x, y, paint)
    }
}
