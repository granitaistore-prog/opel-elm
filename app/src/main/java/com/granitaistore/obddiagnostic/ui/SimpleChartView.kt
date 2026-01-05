package com.granitaistore.obddiagnostic.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class SimpleChartView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    private val paint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val data = mutableListOf<Int>()

    fun add(value: Int) {
        if (data.size > 100) data.removeAt(0)
        data.add(value)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.size < 2) return

        val max = (data.maxOrNull() ?: 1).toFloat()
        val stepX = width.toFloat() / (data.size - 1)

        for (i in 0 until data.size - 1) {
            val x1 = i * stepX
            val y1 = height - (data[i] / max) * height
            val x2 = (i + 1) * stepX
            val y2 = height - (data[i + 1] / max) * height
            canvas.drawLine(x1, y1, x2, y2, paint)
        }
    }
}
