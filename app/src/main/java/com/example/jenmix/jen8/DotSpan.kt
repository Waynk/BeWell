package com.example.jenmix.jen8

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.LineBackgroundSpan

class DotSpan(
    private val radius: Float,
    private val color: Int
) : LineBackgroundSpan {
    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        left: Int, right: Int,
        top: Int, baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int, end: Int,
        lineNum: Int
    ) {
        val oldColor = paint.color
        paint.color = color
        val cx = (left + right) / 2f
        val cy = bottom + radius
        canvas.drawCircle(cx, cy, radius, paint)
        paint.color = oldColor
    }
}
