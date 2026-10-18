package fr.mandarine.environmentquality.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.domain.model.WaterStatus
import fr.mandarine.environmentquality.ui.theme.Palette

/**
 * Colour is never the only signal: conform is a round mark with a tick,
 * reserves a square with "!", non conform a diamond with a cross.
 */
data class StatusLook(
    val color: Color,
    val text: Color,
    val background: Color,
    val icon: ImageVector,
    @param:StringRes val label: Int,
)

val WaterStatus.look: StatusLook
    get() = when (this) {
        WaterStatus.CONFORM -> StatusLook(Palette.Conform, Palette.Conform, Palette.ConformBg, AppIcons.Check, R.string.status_conform)
        WaterStatus.RESERVES -> StatusLook(Palette.Reserves, Palette.ReservesText, Palette.ReservesBg, AppIcons.Warning, R.string.status_reserves)
        WaterStatus.NON_CONFORM -> StatusLook(Palette.NonConform, Palette.NonConform, Palette.NonConformBg, AppIcons.Close, R.string.status_non_conform)
        WaterStatus.UNKNOWN -> StatusLook(Palette.InkSecondary, Palette.InkSecondary, Palette.UnknownBg, AppIcons.Minus, R.string.status_unknown)
    }

fun WaterStatus.markShape(size: Dp): Shape = when (this) {
    WaterStatus.CONFORM, WaterStatus.UNKNOWN -> CircleShape
    WaterStatus.RESERVES -> RoundedCornerShape(size / 4)
    WaterStatus.NON_CONFORM -> RoundedCornerShape(size / 6)
}

/** The filled status mark used on the map legend, lists and history months. */
@Composable
fun StatusMark(status: WaterStatus, size: Dp, modifier: Modifier = Modifier) {
    val look = status.look
    val iconSize = size * 0.55f
    Box(
        modifier = modifier
            .size(size)
            .rotate(if (status == WaterStatus.NON_CONFORM) 45f else 0f)
            .clip(status.markShape(size))
            .background(look.color),
        contentAlignment = Alignment.Center,
    ) {
        val glyph = if (status == WaterStatus.RESERVES) AppIcons.Exclamation else look.icon
        Icon(
            imageVector = glyph,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(iconSize)
                .rotate(if (status == WaterStatus.NON_CONFORM) -45f else 0f),
        )
    }
}

@Composable
fun StatusBadge(status: WaterStatus, modifier: Modifier = Modifier) {
    val look = status.look
    Text(
        text = stringResource(look.label),
        color = look.text,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(look.background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
