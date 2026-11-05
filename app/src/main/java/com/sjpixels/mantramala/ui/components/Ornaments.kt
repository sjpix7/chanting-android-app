package com.sjpixels.mantramala.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Small rotated-square jewel, the recurring motif of the app. */
@Composable
fun Diamond(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Canvas(modifier.size(size)) { drawDiamond(center, this.size.minDimension / 2f, color) }
}

fun DrawScope.drawDiamond(c: Offset, r: Float, color: Color, stroke: Float? = null) {
    val p = Path().apply {
        moveTo(c.x, c.y - r); lineTo(c.x + r, c.y); lineTo(c.x, c.y + r); lineTo(c.x - r, c.y); close()
    }
    if (stroke == null) drawPath(p, color) else drawPath(p, color, style = Stroke(stroke))
}

/** Upper-case, letter-spaced section label: ◆ LABEL ───────── */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.secondary
    val line = MaterialTheme.colorScheme.outlineVariant
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Diamond(accent, size = 8.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        Canvas(Modifier.weight(1f).height(1.dp)) {
            drawLine(line, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = size.height)
        }
    }
}

/** ─── ◆ ─── */
@Composable
fun OrnamentDivider(modifier: Modifier = Modifier, width: Dp = 120.dp) {
    val accent = MaterialTheme.colorScheme.secondary
    Canvas(modifier.width(width).height(10.dp)) {
        val cy = size.height / 2
        val r = size.height / 2
        drawLine(accent.copy(alpha = 0.6f), Offset(0f, cy), Offset(size.width / 2 - r * 2, cy), 2f)
        drawLine(accent.copy(alpha = 0.6f), Offset(size.width / 2 + r * 2, cy), Offset(size.width, cy), 2f)
        drawDiamond(Offset(size.width / 2, cy), r, accent)
    }
}

/** Sharp-cornered card with a hairline border. */
@Composable
fun SharpCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (highlighted) scheme.primaryContainer else scheme.surfaceContainer,
        border = BorderStroke(if (highlighted) 1.5.dp else 1.dp, if (highlighted) scheme.primary else scheme.outlineVariant),
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** Faint sacred-geometry line art used behind the counter and headers. */
@Composable
fun MandalaBackdrop(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, alpha: Float = 0.07f) {
    Canvas(modifier) {
        val c = center
        val r = min(size.width, size.height) / 2f
        val col = color.copy(alpha = alpha)
        val sw = 1.2.dp.toPx()
        listOf(1f, 0.86f, 0.62f, 0.38f).forEach { drawCircle(col, r * it, c, style = Stroke(sw)) }
        // 16 pointed petals
        val petals = 16
        for (i in 0 until petals) {
            val a = Math.toRadians(i * 360.0 / petals - 90)
            val a1 = Math.toRadians((i - 0.5) * 360.0 / petals - 90)
            val a2 = Math.toRadians((i + 0.5) * 360.0 / petals - 90)
            val tip = Offset(c.x + (r * 0.99f * cos(a)).toFloat(), c.y + (r * 0.99f * sin(a)).toFloat())
            val b1 = Offset(c.x + (r * 0.86f * cos(a1)).toFloat(), c.y + (r * 0.86f * sin(a1)).toFloat())
            val b2 = Offset(c.x + (r * 0.86f * cos(a2)).toFloat(), c.y + (r * 0.86f * sin(a2)).toFloat())
            drawPath(Path().apply { moveTo(b1.x, b1.y); lineTo(tip.x, tip.y); lineTo(b2.x, b2.y) }, col, style = Stroke(sw))
        }
        // two interlaced squares -> 8-point star
        for (rot in listOf(0f, 45f)) {
            rotate(rot, c) {
                val s = r * 0.62f * 0.7071f
                drawRect(col, Offset(c.x - s, c.y - s), androidx.compose.ui.geometry.Size(s * 2, s * 2), style = Stroke(sw))
            }
        }
        // 12 rays
        for (i in 0 until 12) {
            val a = Math.toRadians(i * 30.0)
            drawLine(
                col,
                Offset(c.x + (r * 0.38f * cos(a)).toFloat(), c.y + (r * 0.38f * sin(a)).toFloat()),
                Offset(c.x + (r * 0.62f * cos(a)).toFloat(), c.y + (r * 0.62f * sin(a)).toFloat()),
                sw,
            )
        }
    }
}

/** Centered om glyph inside a diamond — the app's mark. */
@Composable
fun OmMark(modifier: Modifier = Modifier, size: Dp = 34.dp) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            drawDiamond(center, this.size.minDimension / 2f, scheme.primary)
            drawDiamond(center, this.size.minDimension / 2f - 3.dp.toPx(), scheme.onPrimary.copy(alpha = 0.5f), stroke = 1.dp.toPx())
        }
        Text(
            "ॐ",
            color = scheme.onPrimary,
            fontSize = (size.value * 0.42f).sp,
            fontFamily = com.sjpixels.mantramala.ui.theme.Devanagari,
        )
    }
}

@Composable
fun VerticalSpace(h: Dp) = Spacer(Modifier.height(h))

@Composable
fun Centered(content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, content = content)
