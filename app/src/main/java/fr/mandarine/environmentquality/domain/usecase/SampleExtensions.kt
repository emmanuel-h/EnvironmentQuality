package fr.mandarine.environmentquality.domain.usecase

import fr.mandarine.environmentquality.domain.model.ParameterResult
import fr.mandarine.environmentquality.domain.model.WaterSample

/** The highlighted parameters that were measured in this sample, in display order. */
fun WaterSample.keyParameters(): List<ParameterResult> =
    ParameterResult.KEY_PARAMETERS.mapNotNull { code -> results.firstOrNull { it.code == code } }

fun WaterSample.parameter(code: String): ParameterResult? = results.firstOrNull { it.code == code }
