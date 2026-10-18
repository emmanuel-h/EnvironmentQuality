package fr.mandarine.environmentquality.domain.model

import java.time.LocalDate
import java.time.YearMonth

data class MonthlyStatus(val month: YearMonth, val status: WaterStatus)

data class SeriesPoint(val date: LocalDate, val value: Double)

data class ParameterSeries(
    val code: String,
    val name: String,
    val unit: String?,
    val limit: QualityRange?,
    val reference: QualityRange?,
    val points: List<SeriesPoint>,
)

/** Twelve months of samples, summarised for the history screen. */
data class WaterHistory(
    val months: List<MonthlyStatus>,
    val series: List<ParameterSeries>,
) {
    val conformMonths: Int get() = months.count { it.status == WaterStatus.CONFORM }
    val measuredMonths: Int get() = months.count { it.status != WaterStatus.UNKNOWN }
}
