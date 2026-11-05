package com.sjpixels.mantramala.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sjpixels.mantramala.R
import com.sjpixels.mantramala.data.MALA_SIZE
import com.sjpixels.mantramala.ui.theme.Cinzel
import kotlin.math.roundToInt

/** Text colour that stays readable on any bead colour the user picks. */
fun beadInk(color: Color): Color = if (color.luminance() > 0.5f) Color(0xFF2A1D14) else Color.White

/**
 * A movable counter bead layered over every screen. Tap to count, drag to move.
 * Its position is stored as fractions of the free area, so it lands in the same spot after rotation.
 */
@Composable
fun FloatingBead(
    count: Int,
    color: Color,
    position: Offset,
    onPositionChange: (Offset) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    beadSize: Dp = 64.dp,
) {
    val a11y = stringResource(R.string.floating_a11y, count, MALA_SIZE)
    val latestOnMove by rememberUpdatedState(onPositionChange)

    BoxWithConstraints(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        val sizePx = with(LocalDensity.current) { beadSize.toPx() }
        val maxX = (constraints.maxWidth - sizePx).coerceAtLeast(1f)
        val maxY = (constraints.maxHeight - sizePx).coerceAtLeast(1f)
        var dragging by remember { mutableStateOf(false) }
        var offset by remember(maxX, maxY) { mutableStateOf(Offset(position.x * maxX, position.y * maxY)) }
        // Follow the saved position (e.g. after rotation or when moved from the over-apps bead).
        LaunchedEffect(position, maxX, maxY) {
            if (!dragging) offset = Offset(position.x * maxX, position.y * maxY)
        }

        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(if (pressed) 0.92f else if (dragging) 1.08f else 1f, label = "bead")
        val ink = beadInk(color)

        Box(
            Modifier
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                .size(beadSize)
                .scale(scale)
                .shadow(10.dp, CircleShape)
                .background(color, CircleShape)
                .pointerInput(maxX, maxY) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            dragging = false
                            latestOnMove(Offset(offset.x / maxX, offset.y / maxY))
                        },
                        onDragCancel = { dragging = false },
                    ) { change, amount ->
                        change.consume()
                        offset = Offset(
                            (offset.x + amount.x).coerceIn(0f, maxX),
                            (offset.y + amount.y).coerceIn(0f, maxY),
                        )
                    }
                }
                .clickable(interactionSource = interaction, indication = ripple(bounded = true), onClick = onTap)
                .semantics { contentDescription = a11y },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(beadSize)) {
                val r = size.minDimension / 2f * 0.80f
                val sw = 3.dp.toPx()
                val tl = Offset(center.x - r, center.y - r)
                drawCircle(ink.copy(alpha = 0.25f), r, center, style = Stroke(sw))
                drawArc(
                    ink, -90f, 360f * count / MALA_SIZE, false, tl, Size(r * 2, r * 2),
                    style = Stroke(sw, cap = StrokeCap.Butt),
                )
            }
            Text(
                count.toString(),
                color = ink,
                fontFamily = Cinzel,
                fontWeight = FontWeight.Bold,
                fontSize = (beadSize.value * if (count >= 100) 0.25f else 0.30f).sp,
            )
        }
    }
}
