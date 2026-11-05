package com.sjpixels.mantramala.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.sjpixels.mantramala.R

/**
 * A devotional colour palette. Each has a light ("day") and dark ("night") variant.
 * [deep] is the darkest tone, used for gradients and dark-mode tints.
 */
data class SacredPalette(
    val id: String,
    @param:StringRes val nameRes: Int,
    val primary: Color,
    val primaryNight: Color,
    val accent: Color,
    val accentNight: Color,
    val deep: Color,
)

object Palettes {
    val all = listOf(
        SacredPalette("saffron", R.string.palette_saffron, Color(0xFFD9560B), Color(0xFFFF8A3D), Color(0xFFB8860B), Color(0xFFE8B84A), Color(0xFF6E2205)),
        SacredPalette("kumkum", R.string.palette_kumkum, Color(0xFFB3191E), Color(0xFFF0605A), Color(0xFFB8860B), Color(0xFFF2C94C), Color(0xFF560A0C)),
        SacredPalette("swarna", R.string.palette_swarna, Color(0xFF946A0C), Color(0xFFE3B54E), Color(0xFF8A1F1F), Color(0xFFD9675F), Color(0xFF3F2C04)),
        SacredPalette("neel", R.string.palette_neel, Color(0xFF1F4FA3), Color(0xFF7FA6FF), Color(0xFFB8860B), Color(0xFFF2C14E), Color(0xFF0B1C40)),
        SacredPalette("tulsi", R.string.palette_tulsi, Color(0xFF2B6E35), Color(0xFF72C47F), Color(0xFF9C7A24), Color(0xFFDDBB5C), Color(0xFF0F3317)),
        SacredPalette("kamal", R.string.palette_kamal, Color(0xFFB4305F), Color(0xFFF285AC), Color(0xFFB07A1C), Color(0xFFF2BE5C), Color(0xFF520D29)),
        SacredPalette("rudraksha", R.string.palette_rudraksha, Color(0xFF7A3E1F), Color(0xFFD9956A), Color(0xFFA3561A), Color(0xFFE7A067), Color(0xFF34170A)),
        SacredPalette("mayur", R.string.palette_mayur, Color(0xFF00695E), Color(0xFF4DD0C0), Color(0xFF2A4C9E), Color(0xFF94ABF5), Color(0xFF00332D)),
        SacredPalette("bhasma", R.string.palette_bhasma, Color(0xFF455A64), Color(0xFFB4C3CB), Color(0xFF8D5F4C), Color(0xFFD7B9A5), Color(0xFF1A2428)),
    )

    fun byId(id: String): SacredPalette = all.firstOrNull { it.id == id } ?: all.first()
}

private val Ivory = Color(0xFFFBF6EE)
private val Ink = Color(0xFF2A1D14)
private val NightBase = Color(0xFF0F0B09)
private val NightInk = Color(0xFFF3E9DC)

fun SacredPalette.colorScheme(dark: Boolean): ColorScheme = if (dark) {
    val p = primaryNight
    val bg = lerp(NightBase, deep, 0.18f)
    darkColorScheme(
        primary = p,
        onPrimary = lerp(deep, Color.Black, 0.55f),
        primaryContainer = lerp(bg, p, 0.28f),
        onPrimaryContainer = lerp(p, Color.White, 0.65f),
        secondary = accentNight,
        onSecondary = lerp(deep, Color.Black, 0.6f),
        secondaryContainer = lerp(bg, accentNight, 0.22f),
        onSecondaryContainer = lerp(accentNight, Color.White, 0.6f),
        tertiary = accentNight,
        onTertiary = Color.Black,
        background = bg,
        onBackground = NightInk,
        surface = bg,
        onSurface = NightInk,
        surfaceVariant = lerp(bg, p, 0.10f),
        onSurfaceVariant = Color(0xFFCFC0B0),
        surfaceContainerLowest = lerp(bg, Color.Black, 0.3f),
        surfaceContainerLow = lerp(bg, p, 0.04f),
        surfaceContainer = lerp(bg, p, 0.07f),
        surfaceContainerHigh = lerp(bg, p, 0.10f),
        surfaceContainerHighest = lerp(bg, p, 0.14f),
        outline = lerp(Color(0xFF6E6055), p, 0.35f),
        outlineVariant = lerp(Color(0xFF3A302A), p, 0.22f),
        inversePrimary = primary,
        error = Color(0xFFFF8A80),
        onError = Color(0xFF3B0907),
    )
} else {
    val p = primary
    val bg = lerp(Ivory, p, 0.035f)
    lightColorScheme(
        primary = p,
        onPrimary = Color.White,
        primaryContainer = lerp(Color.White, p, 0.16f),
        onPrimaryContainer = deep,
        secondary = accent,
        onSecondary = Color.White,
        secondaryContainer = lerp(Color.White, accent, 0.18f),
        onSecondaryContainer = lerp(accent, Color.Black, 0.55f),
        tertiary = accent,
        onTertiary = Color.White,
        background = bg,
        onBackground = Ink,
        surface = bg,
        onSurface = Ink,
        surfaceVariant = lerp(Color(0xFFF3E9DC), p, 0.08f),
        onSurfaceVariant = Color(0xFF5E4F44),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = lerp(Color(0xFFFFFCF7), p, 0.03f),
        surfaceContainer = lerp(Color(0xFFFFFAF2), p, 0.055f),
        surfaceContainerHigh = lerp(Color(0xFFFBF3E8), p, 0.08f),
        surfaceContainerHighest = lerp(Color(0xFFF6ECDF), p, 0.11f),
        outline = lerp(Color(0xFFB3A190), p, 0.35f),
        outlineVariant = lerp(Color(0xFFE6DACB), p, 0.18f),
        inversePrimary = primaryNight,
        error = Color(0xFFB3261E),
        onError = Color.White,
    )
}
