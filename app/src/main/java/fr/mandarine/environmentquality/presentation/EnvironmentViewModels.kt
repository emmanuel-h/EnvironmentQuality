package fr.mandarine.environmentquality.presentation

import fr.mandarine.environmentquality.domain.model.CommuneRisks
import fr.mandarine.environmentquality.domain.model.DroughtStatus
import fr.mandarine.environmentquality.domain.model.FlowStation
import fr.mandarine.environmentquality.domain.model.GeoPoint
import fr.mandarine.environmentquality.domain.model.GroundwaterStation
import fr.mandarine.environmentquality.domain.model.HydroReading
import fr.mandarine.environmentquality.domain.model.HydroStation
import fr.mandarine.environmentquality.domain.model.TemperatureReading
import fr.mandarine.environmentquality.domain.model.TemperatureStation
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

class RisksViewModel(
    code: String,
    communes: CommuneRepository,
    private val risks: RiskRepository,
) : CommuneDataViewModel<CommuneRisks>(code, communes) {
    init {
        refresh()
    }

    override suspend fun fetch(code: String): CommuneRisks = risks.risks(code)
}

data class LiveRiver(val station: HydroStation, val reading: HydroReading, val distanceKm: Double?)

data class RiverTemperature(val station: TemperatureStation, val reading: TemperatureReading?, val stationCount: Int)

data class Nearby(
    val river: LiveRiver?,
    val temperature: RiverTemperature?,
    val flowStations: List<FlowStation>,
    val flowStationCount: Int,
    val groundwater: GroundwaterStation?,
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
        val flows = async { runCatching { stations.flowStations(department) }.getOrDefault(emptyList()) }
        val groundwater = async { runCatching { stations.groundwaterStations(code, department) }.getOrDefault(emptyList()) }
        val flowList = flows.await()
        Nearby(
            river = river.await(),
            temperature = temperature.await(),
            flowStations = nearest(flowList, centre) { it.location }.take(3),
            flowStationCount = flowList.size,
            groundwater = nearest(groundwater.await(), centre) { it.location }.firstOrNull(),
        )
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
        return RiverTemperature(station, runCatching { stations.latestTemperature(station.code) }.getOrNull(), all.size)
    }

    private fun <T> nearest(items: List<T>, centre: GeoPoint?, location: (T) -> GeoPoint?): List<T> =
        if (centre == null) items else items.sortedBy { item -> location(item)?.distanceKm(centre) ?: Double.MAX_VALUE }

    private fun distance(centre: GeoPoint?, point: GeoPoint?) =
        if (centre != null && point != null) centre.distanceKm(point) else null
}
