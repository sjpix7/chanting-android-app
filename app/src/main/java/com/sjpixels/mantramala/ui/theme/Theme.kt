package com.sjpixels.mantramala.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sjpixels.mantramala.R

@OptIn(ExperimentalTextApi::class)
val Cinzel = FontFamily(
    Font(R.font.cinzel, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.cinzel, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.cinzel, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.cinzel, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900))),
)

/** Tiro Devanagari Sanskrit: designed for Sanskrit, with proper conjuncts and Vedic marks. */
val Devanagari = FontFamily(Font(R.font.tiro_devanagari, FontWeight.Normal))

private val base = Typography()

/**
 * Display and title styles use Cinzel's sharp classical capitals. Scripts Cinzel doesn't cover
 * (Devanagari, Cyrillic, CJK) fall back to the system font automatically.
 */
val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Cinzel, fontWeight = FontWeight.Bold),
    displayMedium = base.displayMedium.copy(fontFamily = Cinzel, fontWeight = FontWeight.Bold),
    displaySmall = base.displaySmall.copy(fontFamily = Cinzel, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Cinzel, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Cinzel, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Cinzel, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = Cinzel, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold),
)

val DevanagariStyle = TextStyle(fontFamily = Devanagari, fontSize = 20.sp, lineHeight = 30.sp)

val SharpShapes = Shapes(
    extraSmall = CutCornerShape(3.dp),
    small = CutCornerShape(6.dp),
    medium = CutCornerShape(10.dp),
    large = CutCornerShape(14.dp),
    extraLarge = CutCornerShape(18.dp),
)

val LocalPalette = staticCompositionLocalOf { Palettes.all.first() }
val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun MantraMalaTheme(palette: SacredPalette, dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPalette provides palette, LocalDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = palette.colorScheme(dark),
            typography = AppTypography,
            shapes = SharpShapes,
            content = content,
        )
    }
}
