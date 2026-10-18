package fr.mandarine.environmentquality.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.domain.model.DroughtLevel
import fr.mandarine.environmentquality.ui.theme.Palette

/** VigiEau's own scale: yellow, orange, red, dark red, each darker than the last. */
data class DroughtLook(val background: Color, val text: Color, @param:StringRes val label: Int)

val DroughtLevel.look: DroughtLook
    get() = when (this) {
        DroughtLevel.NONE -> DroughtLook(Palette.ConformBg, Palette.Conform, R.string.drought_level_none)
        DroughtLevel.VIGILANCE -> DroughtLook(Palette.DroughtBg, Palette.DroughtText, R.string.drought_level_vigilance)
        DroughtLevel.ALERT -> DroughtLook(Color(0xFFFCE0B8), Color(0xFF7A3E00), R.string.drought_level_alert)
        DroughtLevel.REINFORCED_ALERT -> DroughtLook(Color(0xFFF9C9B4), Color(0xFF8A2A0A), R.string.drought_level_reinforced)
        DroughtLevel.CRISIS -> DroughtLook(Palette.NonConformBg, Color(0xFF8E1B12), R.string.drought_level_crisis)
    }

@Composable
fun DroughtBadge(level: DroughtLevel, modifier: Modifier = Modifier) {
    val look = level.look
    Text(
        stringResource(look.label),
        color = look.text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(look.background)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** One-line advice with the decree's end date. */
@StringRes
fun droughtAdvice(level: DroughtLevel): Int = when (level) {
    DroughtLevel.NONE -> R.string.drought_none_short
    DroughtLevel.VIGILANCE -> R.string.drought_advice_vigilance
    DroughtLevel.ALERT -> R.string.drought_advice_alert
    DroughtLevel.REINFORCED_ALERT -> R.string.drought_advice_reinforced
    DroughtLevel.CRISIS -> R.string.drought_advice_crisis
}

@StringRes
fun droughtAdviceNoDate(level: DroughtLevel): Int = when (level) {
    DroughtLevel.NONE -> R.string.drought_none_short
    DroughtLevel.VIGILANCE -> R.string.drought_headline_vigilance
    DroughtLevel.ALERT -> R.string.drought_headline_alert
    DroughtLevel.REINFORCED_ALERT -> R.string.drought_headline_reinforced
    DroughtLevel.CRISIS -> R.string.drought_headline_crisis
}
