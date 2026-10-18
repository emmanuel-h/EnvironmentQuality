package fr.mandarine.environmentquality.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.ui.theme.Palette

val CardShape = RoundedCornerShape(18.dp)

/** White card with the mockup's hairline border. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickable = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(Palette.Surface)
            .border(1.dp, Palette.Border, CardShape)
            .then(clickable)
            .padding(contentPadding),
        content = content,
    )
}

/** Round white button with a shadow, as on the map and commune page. */
@Composable
fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Palette.Ink,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Palette.Surface,
        shadowElevation = 2.dp,
        modifier = modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

/** Back arrow, title and subtitle at the top of a detail screen. */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String?,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(AppIcons.Back, contentDescription = stringResource(R.string.action_back), tint = Palette.Ink)
            }
        } else {
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f).padding(start = 2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
        actions()
    }
}

/** Large title and subtitle at the top of a tab without a back arrow. */
@Composable
fun TabHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Palette.InkSecondary)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        action?.invoke()
    }
}

/** Upper-case grey label above a list, as on the search screen. */
@Composable
fun OverlineTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = Palette.InkSecondary,
        modifier = modifier.semantics { heading() },
    )
}

data class ChipOption<T>(val value: T, val label: String)

/** Single-choice chips: dark when selected, white with a shadow otherwise. */
@Composable
fun <T> ChoiceChips(
    options: List<ChipOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()).padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected
            Text(
                text = option.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color.White else Palette.Ink,
                modifier = Modifier
                    .shadow(1.dp, CircleShape)
                    .clip(CircleShape)
                    .background(if (isSelected) Palette.Ink else Palette.Surface)
                    .semantics { this.selected = isSelected }
                    .clickable(role = Role.Tab) { onSelect(option.value) }
                    .heightIn(min = 36.dp)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Palette.Water)
    }
}

@Composable
fun ErrorBox(onRetry: () -> Unit, modifier: Modifier = Modifier, message: String = stringResource(R.string.error_generic)) {
    AppCard(modifier.fillMaxWidth()) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Palette.InkSecondary)
        TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 0.dp)) {
            Text(stringResource(R.string.action_retry), color = Palette.Water, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun EmptyText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = Palette.InkSecondary,
        textAlign = TextAlign.Start,
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
    )
}

/** Thin progress bar for a value against its bound. */
@Composable
fun Gauge(ratio: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Palette.Track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(ratio.coerceIn(0.02f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
    }
}

/** Primary filled button in the water blue. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, trailing: Boolean = false) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Palette.Water,
        modifier = modifier.height(48.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (trailing) {
                Spacer(Modifier.width(8.dp))
                Icon(AppIcons.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Outlined square button holding an icon. */
@Composable
fun OutlinedIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (filled) Palette.Water else Palette.Surface,
        border = if (filled) null else BorderStroke(1.5.dp, Palette.Handle),
        modifier = modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, tint = if (filled) Color.White else Palette.Ink, modifier = Modifier.size(20.dp))
        }
    }
}
