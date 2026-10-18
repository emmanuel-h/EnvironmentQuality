package fr.mandarine.environmentquality.data.repository

import fr.mandarine.environmentquality.data.remote.FlowStationDto
import fr.mandarine.environmentquality.data.remote.GroundwaterStationDto
import fr.mandarine.environmentquality.data.remote.HttpClient
import fr.mandarine.environmentquality.data.remote.HubEauPage
import fr.mandarine.environmentquality.data.remote.HydroObservationDto
import fr.mandarine.environmentquality.data.remote.HydroStationDto
import fr.mandarine.environmentquality.data.remote.TemperatureDto
import fr.mandarine.environmentquality.data.remote.TemperatureStationDto
import fr.mandarine.environmentquality.data.remote.apiJson
import fr.mandarine.environmentquality.data.remote.buildUrl
import fr.mandarine.environmentquality.domain.model.FlowStation
import fr.mandarine.environmentquality.domain.model.GeoPoint
import fr.mandarine.environmentquality.domain.model.GroundwaterStation
import fr.mandarine.environmentquality.domain.model.HydroReading
import fr.mandarine.environmentquality.domain.model.HydroStation
import fr.mandarine.environmentquality.domain.model.TemperatureReading
import fr.mandarine.environmentquality.domain.model.TemperatureStation
import kotlinx.serialization.KSerializer
import fr.mandarine.environmentquality.domain.repository.StationRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Hub'Eau hydrometry, river temperature, groundwater quality and flow observation. */
class HubEauStationRepository(
    private val http: HttpClient,
    private val baseUrl: String = "https://hubeau.eaufrance.fr/api",
) : StationRepository {

    override suspend fun hydroStations(departmentCode: String): List<HydroStation> =
        page(
            HydroStationDto.serializer(),
            buildUrl("$baseUrl/v2/hydrometrie/referentiel/stations", "code_departement" to departmentCode, "en_service" to true, "size" to 100),
        ).map { dto ->
            HydroStation(
                code = dto.code,
                name = dto.name ?: dto.siteName ?: dto.code,
                river = dto.river,
                location = point(dto.latitude, dto.longitude),
                inService = dto.inService ?: false,
                comment = dto.influence,
            )
        }

    override suspend fun hydroReading(stationCode: String): HydroReading? {
        val rows = page(
            HydroObservationDto.serializer(),
            buildUrl("$baseUrl/v2/hydrometrie/observations_tr", "code_entite" to stationCode, "size" to 20),
        )
        return toReading(rows)
    }

    /** Rows come newest first, height (mm) and flow (l/s) interleaved. */
    internal fun toReading(rows: List<HydroObservationDto>): HydroReading? {
        if (rows.isEmpty()) return null
        val heights = rows.filter { it.quantity == "H" && it.value != null }
        val flows = rows.filter { it.quantity == "Q" && it.value != null }
        val heightChange = if (heights.size >= 2) heights[0].value!! - heights[1].value!! else null
        return HydroReading(
            heightMetres = heights.firstOrNull()?.value?.div(1000),
            heightAt = heights.firstOrNull()?.observedAt?.let(::instant),
            flowCubicMetres = flows.firstOrNull()?.value?.div(1000),
            flowAt = flows.firstOrNull()?.observedAt?.let(::instant),
            previousFlowCubicMetres = flows.getOrNull(1)?.value?.div(1000),
            heightChangeMillimetres = heightChange,
        )
    }

    override suspend fun temperatureStations(departmentCode: String): List<TemperatureStation> =
        page(
            TemperatureStationDto.serializer(),
            buildUrl("$baseUrl/v1/temperature/station", "code_departement" to departmentCode, "size" to 100),
        ).map { TemperatureStation(it.code, it.name ?: it.code, it.river, point(it.latitude, it.longitude)) }

    override suspend fun latestTemperature(stationCode: String): TemperatureReading? =
        page(
            TemperatureDto.serializer(),
            buildUrl("$baseUrl/v1/temperature/chronique", "code_station" to stationCode, "size" to 1, "sort" to "desc"),
        ).firstOrNull()?.let { dto ->
            val date = dto.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
            val time = dto.time?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: LocalTime.MIDNIGHT
            TemperatureReading(dto.resultat ?: return null, LocalDateTime.of(date, time))
        }

    /** Stations in the commune first; the department's otherwise. Many legacy points have no location. */
    override suspend fun groundwaterStations(communeCode: String, departmentCode: String): List<GroundwaterStation> {
        val inCommune = groundwater(buildUrl("$baseUrl/v1/qualite_nappes/stations", "code_commune" to communeCode, "size" to 20))
        if (inCommune.isNotEmpty()) return inCommune
        return groundwater(buildUrl("$baseUrl/v1/qualite_nappes/stations", "code_departement" to departmentCode, "size" to 200))
            .filter { it.location != null }
            .take(20)
    }

    private suspend fun groundwater(url: String) = page(GroundwaterStationDto.serializer(), url).map { dto ->
        GroundwaterStation(
            code = dto.bssId.ifEmpty { dto.code.orEmpty() },
            commune = dto.commune,
            nature = dto.nature,
            waterBody = dto.waterBody,
            altitude = dto.altitude,
            location = point(dto.latitude, dto.longitude),
        )
    }

    override suspend fun flowStations(departmentCode: String): List<FlowStation> =
        page(
            FlowStationDto.serializer(),
            buildUrl("$baseUrl/v1/ecoulement/stations", "code_departement" to departmentCode, "size" to 100),
        ).map { dto ->
            FlowStation(
                code = dto.code,
                name = dto.name ?: dto.code,
                river = dto.river,
                commune = dto.commune,
                active = dto.state.equals("Active", ignoreCase = true),
                location = point(dto.latitude, dto.longitude),
            )
        }

    private suspend fun <T> page(serializer: KSerializer<T>, url: String): List<T> =
        apiJson.decodeFromString(HubEauPage.serializer(serializer), http.get(url)).data

    private fun point(latitude: Double?, longitude: Double?) =
        if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null

    private fun instant(text: String): Instant? = runCatching { Instant.parse(text) }.getOrNull()
}
