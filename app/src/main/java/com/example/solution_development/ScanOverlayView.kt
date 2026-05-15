package com.example.solution_development

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class ScanOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val frameWidthRatio = 0.80f
    private val frameHeightRatio = 0.16f

    private val framePaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }
    private val bgPaint = Paint().apply {
        color = Color.parseColor("#88000000")
    }

    private var scanRect: Rect = Rect()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val frameWidth = width * frameWidthRatio
        val frameHeight = this.height * frameHeightRatio

        val left = (this.width - frameWidth) / 2
        val top = (this.height - frameHeight) / 2
        val right = left + frameWidth
        val bottom = top + frameHeight

        scanRect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())

        canvas.drawRect(0f, 0f, width.toFloat(), top, bgPaint)
        canvas.drawRect(0f, bottom, width.toFloat(), height.toFloat(), bgPaint)
        canvas.drawRect(0f, top, left, bottom, bgPaint)
        canvas.drawRect(right, top, width.toFloat(), bottom, bgPaint)

        canvas.drawRect(scanRect, framePaint)
    }

    fun getScanRect(): Rect {
        return scanRect
    }
}