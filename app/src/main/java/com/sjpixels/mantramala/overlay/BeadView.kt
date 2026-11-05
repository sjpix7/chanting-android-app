package com.sjpixels.mantramala.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.ViewOutlineProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import com.sjpixels.mantramala.R
import com.sjpixels.mantramala.data.MALA_SIZE

/** The bead drawn over other apps: a coloured circle with a progress ring and the current count. */
@SuppressLint("ViewConstructor")
class BeadView(context: Context, private val padPx: Float) : View(context) {
    private var fillColor: Int = "#D9560B".toColorInt()
    private var count: Int = 0

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.cinzel)
        isFakeBoldText = true
    }
    private val arcRect = RectF()

    init {
        elevation = padPx * 0.8f
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(padPx.toInt(), padPx.toInt(), (view.width - padPx).toInt(), (view.height - padPx).toInt())
            }
        }
    }

    fun setBeadColor(color: Int) {
        if (color != fillColor) { fillColor = color; invalidate() }
    }

    fun setCount(value: Int) {
        if (value != count) { count = value; contentDescription = "$value / $MALA_SIZE"; invalidate() }
    }

    private fun ink(): Int {
        val r = Color.red(fillColor) / 255.0; val g = Color.green(fillColor) / 255.0; val b = Color.blue(fillColor) / 255.0
        val lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
        return if (lum > 0.6) "#2A1D14".toColorInt() else Color.WHITE
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = width / 2f - padPx
        fill.color = fillColor
        canvas.drawCircle(cx, cy, radius, fill)

        val ink = ink()
        val r = radius * 0.80f
        ring.strokeWidth = radius * 0.1f
        ring.color = ink
        ring.alpha = 64
        canvas.drawCircle(cx, cy, r, ring)
        ring.alpha = 255
        arcRect.set(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(arcRect, -90f, 360f * count / MALA_SIZE, false, ring)

        text.color = ink
        text.textSize = radius * if (count >= 100) 0.52f else 0.62f
        val baseline = cy - (text.descent() + text.ascent()) / 2f
        canvas.drawText(count.toString(), cx, baseline, text)
    }
}
