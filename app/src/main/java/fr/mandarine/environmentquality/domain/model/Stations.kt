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
)

data class FlowStation(
    val code: String,
    val name: String,
    val river: String?,
    val commune: String?,
    val active: Boolean,
    val location: GeoPoint?,
)
