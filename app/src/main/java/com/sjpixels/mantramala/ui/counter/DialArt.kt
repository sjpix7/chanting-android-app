package com.sjpixels.mantramala.ui.counter

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import com.sjpixels.mantramala.data.CounterStyle
import com.sjpixels.mantramala.data.MALA_SIZE
import com.sjpixels.mantramala.ui.components.drawDiamond
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class DialColors(
    val primary: Color,
    val accent: Color,
    val track: Color,
    val line: Color,
    val surface: Color,
)

@Composable
fun dialColors(): DialColors {
    val s = MaterialTheme.colorScheme
    return DialColors(
        primary = s.primary,
        accent = s.secondary,
        track = s.outlineVariant,
        line = s.outline.copy(alpha = 0.55f),
        surface = s.surfaceContainerLow,
    )
}

/**
 * Draws the counter art for [progress] beads (fractional while animating, 0..108).
 * [burst] 0..1 drives the ring of light when a mala completes.
 */
@Composable
fun DialArt(progress: Float, style: CounterStyle, modifier: Modifier = Modifier, burst: Float = 0f, colors: DialColors = dialColors()) {
    Canvas(modifier) {
        when (style) {
            CounterStyle.MALA -> drawMala(progress, colors)
            CounterStyle.PADMA -> drawPadma(progress, colors)
            CounterStyle.YANTRA -> drawYantra(progress, colors)
        }
        if (burst in 0.001f..0.999f) {
            val half = size.minDimension / 2f
            drawCircle(
                colors.accent.copy(alpha = (1f - burst) * 0.7f),
                radius = half * (0.55f + 0.5f * burst),
                style = Stroke(width = half * 0.06f * (1f - burst) + 1f),
            )
            drawCircle(
                colors.primary.copy(alpha = (1f - burst) * 0.5f),
                radius = half * (0.45f + 0.62f * burst),
                style = Stroke(width = 2f),
            )
        }
    }
}

private fun DrawScope.polar(r: Float, deg: Double): Offset {
    val a = deg * PI / 180.0
    return Offset(center.x + (r * cos(a)).toFloat(), center.y + (r * sin(a)).toFloat())
}

private fun beadFill(progress: Float, index: Int) = (progress - index).coerceIn(0f, 1f)

// ---------------------------------------------------------------- Mala: 108 beads + Meru
private fun DrawScope.drawMala(progress: Float, c: DialColors) {
    val half = size.minDimension / 2f
    val slots = MALA_SIZE + 1 // one slot for the Meru (guru) bead
    val ringR = half * 0.86f
    val step = 360.0 / slots
    val beadR = (2 * PI * ringR / slots).toFloat() * 0.36f

    // thread
    drawCircle(c.line, ringR, center, style = Stroke(1.2f))

    // inner ornament ring
    val innerR = ringR * 0.84f
    drawCircle(c.accent.copy(alpha = 0.45f), innerR, center, style = Stroke(1.5f))
    drawCircle(c.accent.copy(alpha = 0.25f), innerR * 0.94f, center, style = Stroke(1f))
    for (k in 0 until 8) drawDiamond(polar(innerR, k * 45.0 - 90), beadR * 0.9f, c.accent.copy(alpha = 0.6f))

    val current = progress.toInt()
    for (i in 0 until MALA_SIZE) {
        val pos = polar(ringR, -90 + (i + 1) * step)
        val f = beadFill(progress, i)
        val col = lerp(c.track, c.primary, f)
        drawCircle(col, beadR * (1f + 0.18f * f), pos)
        if (f > 0.5f) drawCircle(Color.White.copy(alpha = 0.28f), beadR * 0.38f, pos + Offset(-beadR * 0.3f, -beadR * 0.3f))
        if (i == current && progress < MALA_SIZE) {
            drawCircle(c.accent, beadR * 1.9f, pos, style = Stroke(2f))
        }
    }
    // every 27th bead marker (quarter mala)
    for (q in 1..3) {
        val pos = polar(ringR + beadR * 2.6f, -90 + (q * 27) * step)
        drawDiamond(pos, beadR * 0.7f, c.accent.copy(alpha = 0.7f))
    }

    // Meru bead + tassel at the top
    val meru = polar(ringR, -90.0)
    val meruR = beadR * 2.1f
    drawCircle(c.accent, meruR, meru)
    drawCircle(Color.White.copy(alpha = 0.3f), meruR * 0.35f, meru + Offset(-meruR * 0.3f, -meruR * 0.3f))
    val tassel = Path().apply {
        moveTo(meru.x, meru.y - meruR)
        lineTo(meru.x - meruR * 0.75f, meru.y - meruR * 2.9f)
        lineTo(meru.x, meru.y - meruR * 2.4f)
        lineTo(meru.x + meruR * 0.75f, meru.y - meruR * 2.9f)
        close()
    }
    drawPath(tassel, c.accent)
}

// ---------------------------------------------------------------- Padma: 16-petal lotus
private fun DrawScope.drawPadma(progress: Float, c: DialColors) {
    val half = size.minDimension / 2f
    val frac = progress / MALA_SIZE

    // outer progress ring
    val ringR = half * 0.93f
    val sw = half * 0.045f
    drawCircle(c.track, ringR, center, style = Stroke(sw))
    if (frac > 0f) {
        drawArc(
            color = c.primary,
            startAngle = -90f, sweepAngle = 360f * frac, useCenter = false,
            topLeft = Offset(center.x - ringR, center.y - ringR), size = Size(ringR * 2, ringR * 2),
            style = Stroke(sw, cap = StrokeCap.Butt),
        )
    }
    for (q in 0 until 4) drawDiamond(polar(ringR, q * 90.0 - 90), sw * 0.9f, c.accent)

    // outer 16 petals fill one by one
    val petals = 16
    val baseR = half * 0.58f
    val tipR = half * 0.84f
    val perPetal = MALA_SIZE / petals.toFloat()
    for (k in 0 until petals) {
        val a = k * 360.0 / petals - 90 + 360.0 / petals / 2
        val f = ((progress - k * perPetal) / perPetal).coerceIn(0f, 1f)
        val path = petalPath(a, baseR, tipR, 360.0 / petals * 0.48)
        drawPath(path, lerp(c.surface, c.primary, f * 0.9f))
        drawPath(path, lerp(c.line, c.accent, f), style = Stroke(1.6f, join = StrokeJoin.Miter))
    }
    // inner 8 petals, outline
    for (k in 0 until 8) {
        val a = k * 45.0 - 90
        drawPath(petalPath(a, half * 0.44f, half * 0.6f, 22.0 * 0.5), c.accent.copy(alpha = 0.55f), style = Stroke(1.4f))
    }
    drawCircle(c.accent.copy(alpha = 0.5f), half * 0.44f, center, style = Stroke(1.5f))
}

/** A lotus petal with a sharp tip, built from two quadratic curves. */
private fun DrawScope.petalPath(angleDeg: Double, baseR: Float, tipR: Float, halfWidthDeg: Double): Path {
    val left = polar(baseR, angleDeg - halfWidthDeg)
    val right = polar(baseR, angleDeg + halfWidthDeg)
    val tip = polar(tipR, angleDeg)
    val midR = baseR + (tipR - baseR) * 0.55f
    val cl = polar(midR, angleDeg - halfWidthDeg * 1.05)
    val cr = polar(midR, angleDeg + halfWidthDeg * 1.05)
    return Path().apply {
        moveTo(left.x, left.y)
        quadraticTo(cl.x, cl.y, tip.x, tip.y)
        quadraticTo(cr.x, cr.y, right.x, right.y)
        close()
    }
}

// ---------------------------------------------------------------- Yantra: Bhupura + circle + triangles
private fun DrawScope.drawYantra(progress: Float, c: DialColors) {
    val half = size.minDimension / 2f
    val frac = progress / MALA_SIZE
    val s = half * 0.82f
    val g = s * 0.2f
    val d = half * 0.13f

    // Bhupura: square with four T-gates, drawn twice
    for ((i, inset) in listOf(0f, half * 0.045f).withIndex()) {
        val ss = s - inset
        val gg = g - inset * 0.5f
        val dd = d
        val o = center
        val p = Path().apply {
            moveTo(o.x - ss, o.y - ss)
            lineTo(o.x - gg, o.y - ss); lineTo(o.x - gg, o.y - ss - dd + inset); lineTo(o.x + gg, o.y - ss - dd + inset); lineTo(o.x + gg, o.y - ss)
            lineTo(o.x + ss, o.y - ss)
            lineTo(o.x + ss, o.y - gg); lineTo(o.x + ss + dd - inset, o.y - gg); lineTo(o.x + ss + dd - inset, o.y + gg); lineTo(o.x + ss, o.y + gg)
            lineTo(o.x + ss, o.y + ss)
            lineTo(o.x + gg, o.y + ss); lineTo(o.x + gg, o.y + ss + dd - inset); lineTo(o.x - gg, o.y + ss + dd - inset); lineTo(o.x - gg, o.y + ss)
            lineTo(o.x - ss, o.y + ss)
            lineTo(o.x - ss, o.y + gg); lineTo(o.x - ss - dd + inset, o.y + gg); lineTo(o.x - ss - dd + inset, o.y - gg); lineTo(o.x - ss, o.y - gg)
            close()
        }
        drawPath(p, if (i == 0) c.accent else c.accent.copy(alpha = 0.45f), style = Stroke(if (i == 0) 2.2f else 1.2f, join = StrokeJoin.Miter))
    }

    // progress circle
    val ringR = s * 0.88f
    val sw = half * 0.05f
    drawCircle(c.track, ringR, center, style = Stroke(sw))
    if (frac > 0f) {
        drawArc(
            color = c.primary,
            startAngle = -90f, sweepAngle = 360f * frac, useCenter = false,
            topLeft = Offset(center.x - ringR, center.y - ringR), size = Size(ringR * 2, ringR * 2),
            style = Stroke(sw, cap = StrokeCap.Butt),
        )
        drawDiamond(polar(ringR, -90.0 + 360.0 * frac), sw * 0.95f, c.accent)
    }

    // 8 sharp petals
    val pBase = s * 0.6f
    val pTip = s * 0.78f
    for (k in 0 until 8) {
        val a = k * 45.0 - 90 + 22.5
        val l = polar(pBase, a - 18)
        val r = polar(pBase, a + 18)
        val t = polar(pTip, a)
        drawPath(Path().apply { moveTo(l.x, l.y); lineTo(t.x, t.y); lineTo(r.x, r.y) }, c.line, style = Stroke(1.4f, join = StrokeJoin.Miter))
    }
    drawCircle(c.line, pBase, center, style = Stroke(1.2f))

    // Shatkona: upward (Shiva) and downward (Shakti) triangles, faint so the number stays legible
    val tr = s * 0.58f
    rotate(0f, center) {
        for (up in listOf(true, false)) {
            val base = if (up) -90.0 else 90.0
            val a = polar(tr, base); val b = polar(tr, base + 120); val cc = polar(tr, base + 240)
            drawPath(
                Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(cc.x, cc.y); close() },
                lerp(c.line, c.primary, frac).copy(alpha = 0.35f + 0.4f * frac),
                style = Stroke(1.6f, join = StrokeJoin.Miter),
            )
        }
    }
}
