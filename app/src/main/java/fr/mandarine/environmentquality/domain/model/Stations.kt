package fr.mandarine.environmentquality.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

data class HydroStation(
    val code: String,
    val name: String,
    val river: String?,
    val location: GeoPoint?,
    val inService: Boolean,
    val comment: String?,
)

/** Hub'Eau sends height in mm and flow in l/s; these are already converted. */
data class HydroReading(
    val heightMetres: Double?,
    val heightAt: Instant?,
    val flowCubicMetres: Double?,
    val flowAt: Instant?,
    val previousFlowCubicMetres: Double?,
    val heightChangeMillimetres: Double?,
)

data class TemperatureStation(
    val code: String,
    val name: String,
    val river: String?,
    val location: GeoPoint?,
)

data class TemperatureReading(val celsius: Double, val measuredAt: LocalDateTime)

/** One day of a river temperature series: its coolest and warmest reading. */
data class DailyTemperature(val date: LocalDate, val minCelsius: Double, val maxCelsius: Double)

/** Folds readings, in any order, into days, oldest first. */
fun List<TemperatureReading>.byDay(): List<DailyTemperature> =
    groupBy { it.measuredAt.toLocalDate() }
        .map { (date, readings) -> DailyTemperature(date, readings.minOf { it.celsius }, readings.maxOf { it.celsius }) }
        .sortedBy { it.date }

data class GroundwaterStation(
    val code: String,
    val commune: String?,
    val nature: String?,
    val waterBody: String?,
    val altitude: Double?,
    val location: GeoPoint?,
    /** The day of the station's last sampling; null when it never reported one. */
    val lastSampledOn: LocalDate? = null,
)

/** One analysed substance of a groundwater sampling. */
data class GroundwaterResult(
    val code: Int,
    val name: String,
    val value: Double?,
    val unit: String?,
    /** False when the substance stayed below what the laboratory can detect or quantify. */
    val quantified: Boolean,
)

/** Every result of a groundwater station's sampling on one day. */
data class GroundwaterSampling(val sampledOn: LocalDate, val results: List<GroundwaterResult>) {
    val nitrates: GroundwaterResult? get() = results.firstOrNull { it.code == NITRATES }
    val pesticides: GroundwaterResult? get() = results.firstOrNull { it.code == PESTICIDES_TOTAL }
    val quantifiedCount: Int get() = results.count { it.quantified }

    companion object {
        /** Sandre parameter codes. */
        const val NITRATES = 1340
        const val PESTICIDES_TOTAL = 6276

        /** Quality standards for groundwater, the same as for drinking water. */
        const val NITRATES_LIMIT = 50.0
        const val PESTICIDES_TOTAL_LIMIT = 0.5
    }
}

data class FlowStation(
    val code: String,
    val name: String,
    val river: String?,
    val commune: String?,
    val active: Boolean,
    val location: GeoPoint?,
)

/** The Onde network's flow classes, from a visual check of the river bed. */
enum class FlowState { FLOWING, LOW, VISIBLE, NOT_VISIBLE, DRY, UNKNOWN }

data class FlowObservation(val state: FlowState, val observedOn: LocalDate)

/** A piezometer: a well where the groundwater level is measured, daily for most. */
data class PiezometerStation(
    val code: String,
    val name: String,
    val aquifer: String?,
    val location: GeoPoint?,
    val lastMeasuredOn: LocalDate?,
)

/**
 * One day's groundwater level. [levelMetres] is the altitude of the water (NGF);
 * [depthMetres] is how far below the ground it is, negative when it rises above it.
 */
data class GroundwaterLevel(val measuredOn: LocalDate, val levelMetres: Double, val depthMetres: Double?)

/** One parameter's latest result at a river quality station. */
data class RiverResult(val code: Int, val name: String, val value: Double, val unit: String?, val sampledOn: LocalDate)

/** The latest physico-chemical results at the river quality station nearest the commune. */
data class RiverQuality(val stationCode: String, val stationName: String, val location: GeoPoint?, val results: List<RiverResult>) {
    fun result(code: Int): RiverResult? = results.firstOrNull { it.code == code }
    val sampledOn: LocalDate? get() = results.maxOfOrNull { it.sampledOn }

    companion object {
        /** Sandre parameter codes. */
        const val NITRATES = 1340
        const val OXYGEN = 1311
        const val ORTHOPHOSPHATES = 1433
        const val AMMONIUM = 1335
        val PARAMETERS = listOf(NITRATES, OXYGEN, ORTHOPHOSPHATES, AMMONIUM)

        /**
         * The limit of good status for rivers (arrêté du 25 janvier 2010): oxygen must
         * stay above its bound, the others below theirs.
         */
        fun goodStatusBound(code: Int): Double? = when (code) {
            NITRATES -> 50.0
            OXYGEN -> 6.0
            ORTHOPHOSPHATES -> 0.5
            AMMONIUM -> 0.5
            else -> null
        }

        fun meetsGoodStatus(code: Int, value: Double): Boolean? {
            val bound = goodStatusBound(code) ?: return null
            return if (code == OXYGEN) value >= bound else value <= bound
        }
    }
}
