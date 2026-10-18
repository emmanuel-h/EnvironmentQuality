package fr.mandarine.environmentquality.presentation

import fr.mandarine.environmentquality.domain.model.CommuneRisks
import fr.mandarine.environmentquality.domain.model.DailyTemperature
import fr.mandarine.environmentquality.domain.model.DroughtStatus
import fr.mandarine.environmentquality.domain.model.FlowObservation
import fr.mandarine.environmentquality.domain.model.FlowStation
import fr.mandarine.environmentquality.domain.model.GeoPoint
import fr.mandarine.environmentquality.domain.model.GroundwaterLevel
import fr.mandarine.environmentquality.domain.model.GroundwaterSampling
import fr.mandarine.environmentquality.domain.model.GroundwaterStation
import fr.mandarine.environmentquality.domain.model.HydroReading
import fr.mandarine.environmentquality.domain.model.HydroStation
import fr.mandarine.environmentquality.domain.model.PiezometerStation
import fr.mandarine.environmentquality.domain.model.RiskDetails
import fr.mandarine.environmentquality.domain.model.RiverQuality
import fr.mandarine.environmentquality.domain.model.TemperatureReading
import fr.mandarine.environmentquality.domain.model.TemperatureStation
import fr.mandarine.environmentquality.domain.model.byDay
import fr.mandarine.environmentquality.domain.model.distanceKm
import fr.mandarine.environmentquality.domain.repository.CommuneRepository
import fr.mandarine.environmentquality.domain.repository.DroughtRepository
import fr.mandarine.environmentquality.domain.repository.RiskRepository
import fr.mandarine.environmentquality.domain.repository.StationRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class DroughtViewModel(
    code: String,
    communes: CommuneRepository,
    private val drought: DroughtRepository,
) : CommuneDataViewModel<DroughtStatus>(code, communes) {
    init {
        refresh()
    }

    override suspend fun fetch(code: String): DroughtStatus = drought.status(code)
}

/** The commune's risk summary, and the details only the Risks screen shows; null when none answered. */
data class RiskOverview(val risks: CommuneRisks, val details: RiskDetails?)

class RisksViewModel(
    code: String,
    private val communes: CommuneRepository,
    private val risks: RiskRepository,
) : CommuneDataViewModel<RiskOverview>(code, communes) {
    init {
        refresh()
    }

    override suspend fun fetch(code: String): RiskOverview = coroutineScope {
        val summary = async { risks.risks(code) }
        val details = async { runCatching { risks.riskDetails(code, communes.byCode(code)?.centre) }.getOrNull() }
        RiskOverview(summary.await(), details.await())
    }
}

data class LiveRiver(val station: HydroStation, val reading: HydroReading, val distanceKm: Double?)

data class RiverTemperature(
    val station: TemperatureStation,
    val reading: TemperatureReading?,
    val stationCount: Int,
    /** The last days the station reported, oldest first. */
    val days: List<DailyTemperature> = emptyList(),
)

/** The nearest reporting piezometer, its latest level and the one about four weeks before. */
data class AquiferLevel(
    val station: PiezometerStation,
    val latest: GroundwaterLevel,
    val monthAgo: GroundwaterLevel?,
    val distanceKm: Double?,
)

data class NearbyRiverQuality(val quality: RiverQuality, val distanceKm: Double?)

data class Nearby(
    val river: LiveRiver?,
    val temperature: RiverTemperature?,
    val flowStations: List<FlowStation>,
    val flowStationCount: Int,
    val groundwater: GroundwaterStation?,
    /** The latest observation of each shown flow station that has one, by station code. */
    val flows: Map<String, FlowObservation> = emptyMap(),
    /** The results of [groundwater]'s last sampling. */
    val groundwaterSampling: GroundwaterSampling? = null,
    val aquiferLevel: AquiferLevel? = null,
    val riverQuality: NearbyRiverQuality? = null,
)

/** The stations of the commune's department, nearest first, and their latest values. */
class NearbyViewModel(
    code: String,
    private val communes: CommuneRepository,
    private val stations: StationRepository,
) : CommuneDataViewModel<Nearby>(code, communes) {
    init {
        refresh()
    }

    override suspend fun fetch(code: String): Nearby = coroutineScope {
        val commune = communes.byCode(code) ?: error("Unknown commune $code")
        val department = commune.departmentCode ?: code.take(2)
        val centre = commune.centre
        val river = async { runCatching { liveRiver(department, centre) }.getOrNull() }
        val temperature = async { runCatching { temperature(department, centre) }.getOrNull() }
        val flowList = async { runCatching { stations.flowStations(department) }.getOrDefault(emptyList()) }
        val groundwaterList = async { runCatching { stations.groundwaterStations(code, department) }.getOrDefault(emptyList()) }
        val shownFlows = async { nearest(flowList.await(), centre) { it.location }.take(FLOWS_SHOWN) }
        val latestFlows = async { runCatching { stations.latestFlows(shownFlows.await().map { it.code }) }.getOrDefault(emptyMap()) }
        val groundwater = async { groundwaterStation(groundwaterList.await(), centre) }
        val sampling = async {
            val station = groundwater.await() ?: return@async null
            val day = station.lastSampledOn ?: return@async null
            runCatching { stations.groundwaterSampling(station.code, day) }.getOrNull()
        }
        val level = async { runCatching { aquiferLevel(department, centre) }.getOrNull() }
        val quality = async {
            centre?.let { point ->
                runCatching { stations.riverQuality(point) }.getOrNull()?.let { NearbyRiverQuality(it, distance(point, it.location)) }
            }
        }
        Nearby(
            river = river.await(),
            temperature = temperature.await(),
            flowStations = shownFlows.await(),
            flowStationCount = flowList.await().size,
            groundwater = groundwater.await(),
            flows = latestFlows.await(),
            groundwaterSampling = sampling.await(),
            aquiferLevel = level.await(),
            riverQuality = quality.await(),
        )
    }

    /** Piezometers that reported in the month before the latest report around; the nearest that answers. */
    private suspend fun aquiferLevel(department: String, centre: GeoPoint?): AquiferLevel? {
        val all = stations.piezometers(department)
        val newest = all.mapNotNull { it.lastMeasuredOn }.maxOrNull() ?: return null
        val reporting = all.filter { it.lastMeasuredOn?.isBefore(newest.minusDays(LEVEL_RECENT_DAYS)) == false }
        for (station in nearest(reporting, centre) { it.location }.take(4)) {
            val levels = runCatching { stations.recentLevels(station.code) }.getOrDefault(emptyList())
            val latest = levels.maxByOrNull { it.measuredOn } ?: continue
            val monthAgo = levels
                .filter { !it.measuredOn.isAfter(latest.measuredOn.minusDays(LEVEL_COMPARISON_DAYS)) }
                .maxByOrNull { it.measuredOn }
            return AquiferLevel(station, latest, monthAgo, distance(centre, station.location))
        }
        return null
    }

    /**
     * The nearest station sampled in the few years before the latest sampling around;
     * many points stopped reporting decades ago. Wells at one spot go to the latest sampled.
     */
    private fun groundwaterStation(all: List<GroundwaterStation>, centre: GeoPoint?): GroundwaterStation? {
        val newest = all.mapNotNull { it.lastSampledOn }.maxOrNull()
        val recent = if (newest == null) all else all.filter { it.lastSampledOn?.isBefore(newest.minusYears(RECENT_YEARS)) == false }
        return recent
            .sortedWith(
                compareBy<GroundwaterStation> { station -> distance(centre, station.location) ?: Double.MAX_VALUE }
                    .thenByDescending { it.lastSampledOn },
            )
            .firstOrNull()
    }

    /** The nearest station that actually reports, trying a few. */
    private suspend fun liveRiver(department: String, centre: GeoPoint?): LiveRiver? {
        val candidates = nearest(stations.hydroStations(department).filter { it.inService }, centre) { it.location }.take(4)
        for (station in candidates) {
            val reading = runCatching { stations.hydroReading(station.code) }.getOrNull() ?: continue
            if (reading.heightMetres == null && reading.flowCubicMetres == null) continue
            return LiveRiver(station, reading, distance(centre, station.location))
        }
        return null
    }

    private suspend fun temperature(department: String, centre: GeoPoint?): RiverTemperature? {
        val all = stations.temperatureStations(department)
        val station = nearest(all, centre) { it.location }.firstOrNull() ?: return null
        val readings = runCatching { stations.recentTemperatures(station.code) }.getOrDefault(emptyList())
        return RiverTemperature(station, readings.maxByOrNull { it.measuredAt }, all.size, readings.byDay())
    }

    private fun <T> nearest(items: List<T>, centre: GeoPoint?, location: (T) -> GeoPoint?): List<T> =
        if (centre == null) items else items.sortedBy { item -> location(item)?.distanceKm(centre) ?: Double.MAX_VALUE }

    private fun distance(centre: GeoPoint?, point: GeoPoint?) =
        if (centre != null && point != null) centre.distanceKm(point) else null

    private companion object {
        const val FLOWS_SHOWN = 3
        const val RECENT_YEARS = 3L
        const val LEVEL_RECENT_DAYS = 30L
        const val LEVEL_COMPARISON_DAYS = 28L
    }
}
