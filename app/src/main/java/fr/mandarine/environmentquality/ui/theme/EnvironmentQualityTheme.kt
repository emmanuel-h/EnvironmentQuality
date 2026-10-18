package fr.mandarine.environmentquality.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import fr.mandarine.environmentquality.R

/** The approved mockup's palette. Status colours always come with a shape and a label. */
object Palette {
    val Ink = Color(0xFF0B1F2A)
    val InkSecondary = Color(0xFF4A5D68)
    val Muted = Color(0xFF7A8B94)
    val Water = Color(0xFF0B6E99)
    val WaterDark = Color(0xFF084E6D)
    val WaterLight = Color(0xFFDCEEF5)
    val WaterTint = Color(0xFFE6F0F4)
    val Background = Color(0xFFF4F7F6)
    val Surface = Color(0xFFFFFFFF)
    val Border = Color(0xFFE1E8E8)
    val Divider = Color(0xFFEEF2F2)
    val Track = Color(0xFFE6EDED)
    val ChipBorder = Color(0xFFDCE4E3)
    val Handle = Color(0xFFCBD6D8)
    val Field = Color(0xFFF1F5F5)

    val Conform = Color(0xFF0F7B5F)
    val ConformBg = Color(0xFFDDF1EA)
    val Reserves = Color(0xFFA15C00)
    val ReservesText = Color(0xFF8A4E00)
    val ReservesBg = Color(0xFFFBEBD3)
    val NonConform = Color(0xFFB42318)
    val NonConformBg = Color(0xFFFBE1DE)
    val UnknownBg = Color(0xFFE6EDED)

    val DroughtBg = Color(0xFFFFF4C2)
    val DroughtIcon = Color(0xFF7A5C00)
    val DroughtText = Color(0xFF4D3A00)

    val MapSea = Color(0xFFD7E6EA)
    val MapLand = Color(0xFFF7FAF9)
    val MapStroke = Color(0xFFB9CCCB)
    val River = Color(0xFF9CC3D3)
}

@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: FontWeight) =
    Font(resId, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

/** Sora for headings and figures. Variable font; below API 26 it renders at its default weight. */
val Sora = FontFamily(
    variable(R.font.sora, FontWeight.Medium),
    variable(R.font.sora, FontWeight.SemiBold),
    variable(R.font.sora, FontWeight.Bold),
)

/** Public Sans for body text. */
val PublicSans = FontFamily(
    variable(R.font.public_sans, FontWeight.Normal),
    variable(R.font.public_sans, FontWeight.Medium),
    variable(R.font.public_sans, FontWeight.SemiBold),
    variable(R.font.public_sans, FontWeight.Bold),
)

private val base = TextStyle(fontFamily = PublicSans, color = Palette.Ink)

private val AppTypography = Typography(
    headlineLarge = base.copy(fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.02).em),
    headlineMedium = base.copy(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    headlineSmall = base.copy(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleLarge = base.copy(fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 19.sp, letterSpacing = (-0.01).em),
    titleMedium = base.copy(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = base.copy(fontSize = 16.sp),
    bodyMedium = base.copy(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = base.copy(fontSize = 13.sp, color = Palette.InkSecondary),
    labelLarge = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelMedium = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelSmall = base.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Palette.InkSecondary),
)

private val AppColors = lightColorScheme(
    primary = Palette.Water,
    onPrimary = Color.White,
    primaryContainer = Palette.WaterLight,
    onPrimaryContainer = Palette.WaterDark,
    secondary = Palette.Conform,
    onSecondary = Color.White,
    background = Palette.Background,
    onBackground = Palette.Ink,
    surface = Palette.Surface,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.Field,
    onSurfaceVariant = Palette.InkSecondary,
    outline = Palette.ChipBorder,
    outlineVariant = Palette.Border,
    error = Palette.NonConform,
)

/**
 * The mockup was designed light only, and status colours were checked for
 * contrast on light grounds, so the app does not switch to a dark scheme.
 */
@Composable
fun EnvironmentQualityTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, typography = AppTypography, content = content)
}
