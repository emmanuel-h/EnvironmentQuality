package fr.mandarine.environmentquality.domain.usecase

import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.model.ParameterResult
import fr.mandarine.environmentquality.domain.model.WaterSample
import fr.mandarine.environmentquality.domain.repository.WaterQualityRepository
import java.time.LocalDate

data class CommuneSide(
    val commune: Commune,
    val sample: WaterSample?,
    val conformMonths: Int?,
    val measuredMonths: Int?,
)

data class Comparison(val left: CommuneSide, val right: CommuneSide) {
    /** Key parameters measured on at least one side. */
    val parameters: List<Pair<ParameterResult?, ParameterResult?>>
        get() = ParameterResult.KEY_PARAMETERS.map { code ->
            left.sample?.parameter(code) to right.sample?.parameter(code)
        }.filter { (a, b) -> a != null || b != null }
}

class CompareCommunesUseCase(
    private val water: WaterQualityRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) {
    suspend operator fun invoke(left: Commune, right: Commune): Comparison =
        Comparison(side(left), side(right))

    private suspend fun side(commune: Commune): CommuneSide {
        val sample = water.latestSample(commune.code)
        val history = runCatching { water.history(commune.code, today().minusMonths(12)) }.getOrNull()
        return CommuneSide(commune, sample, history?.conformMonths, history?.measuredMonths)
    }
}
