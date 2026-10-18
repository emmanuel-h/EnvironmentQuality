package fr.mandarine.environmentquality.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.domain.model.ParameterResult
import fr.mandarine.environmentquality.ui.theme.Palette

/** `<=50 mg/L` becomes `≤ 50 mg/L`, `>=6,5 et <=9` becomes `≥ 6,5 et ≤ 9`. */
fun prettyBound(text: String): String =
    text.replace("<=", "≤ ").replace(">=", "≥ ").replace("  ", " ").trim()

/** The sentence under a parameter: its limit, reference, or that it has none. */
@Composable
fun boundLabel(result: ParameterResult): String = when {
    result.exceedsLimit -> stringResource(R.string.parameter_over_limit, prettyBound(result.limitText.orEmpty()))
    result.nearLimit -> stringResource(R.string.parameter_near_limit, prettyBound(result.limitText.orEmpty()))
    result.limitText != null -> stringResource(R.string.parameter_limit, prettyBound(result.limitText))
    result.exceedsReference -> stringResource(R.string.parameter_over_reference, prettyBound(result.referenceText.orEmpty()))
    result.referenceText != null -> stringResource(R.string.parameter_reference, prettyBound(result.referenceText))
    result.numericValue == null -> stringResource(R.string.parameter_qualitative)
    else -> stringResource(R.string.parameter_no_limit)
}

/** Amber for a breached or nearly breached bound, red over a limit, green or blue otherwise. */
fun ParameterResult.gaugeColor(): Color = when {
    exceedsLimit -> Palette.NonConform
    nearLimit || exceedsReference -> Palette.Reserves
    limit != null -> Palette.Conform
    else -> Palette.Water
}

val ParameterResult.isFlagged: Boolean get() = exceedsLimit || exceedsReference || nearLimit
