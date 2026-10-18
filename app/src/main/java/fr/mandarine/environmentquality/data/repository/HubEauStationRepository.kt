package fr.mandarine.environmentquality.data.repository

import fr.mandarine.environmentquality.data.remote.FlowObservationDto
import fr.mandarine.environmentquality.data.remote.FlowStationDto
import fr.mandarine.environmentquality.data.remote.GroundwaterAnalysisDto
import fr.mandarine.environmentquality.data.remote.GroundwaterStationDto
import fr.mandarine.environmentquality.data.remote.HttpClient
import fr.mandarine.environmentquality.data.remote.HubEauPage
import fr.mandarine.environmentquality.data.remote.HydroObservationDto
import fr.mandarine.environmentquality.data.remote.HydroStationDto
import fr.mandarine.environmentquality.data.remote.PiezometerReadingDto
import fr.mandarine.environmentquality.data.remote.PiezometerStationDto
import fr.mandarine.environmentquality.data.remote.RiverAnalysisDto
import fr.mandarine.environmentquality.data.remote.TemperatureDto
import fr.mandarine.environmentquality.data.remote.TemperatureStationDto
import fr.mandarine.environmentquality.data.remote.apiJson
import fr.mandarine.environmentquality.data.remote.buildUrl
import fr.mandarine.environmentquality.domain.model.FlowObservation
import fr.mandarine.environmentquality.domain.model.FlowState
import fr.mandarine.environmentquality.domain.model.FlowStation
import fr.mandarine.environmentquality.domain.model.GeoPoint
import fr.mandarine.environmentquality.domain.model.GroundwaterLevel
import fr.mandarine.environmentquality.domain.model.GroundwaterResult
import fr.mandarine.environmentquality.domain.model.GroundwaterSampling
import fr.mandarine.environmentquality.domain.model.GroundwaterStation
import fr.mandarine.environmentquality.domain.model.HydroReading
import fr.mandarine.environmentquality.domain.model.HydroStation
import fr.mandarine.environmentquality.domain.model.PiezometerStation
import fr.mandarine.environmentquality.domain.model.RiverQuality
import fr.mandarine.environmentquality.domain.model.RiverResult
import fr.mandarine.environmentquality.domain.model.TemperatureReading
import fr.mandarine.environmentquality.domain.model.TemperatureStation
import fr.mandarine.environmentquality.domain.model.distanceKm
import kotlinx.serialization.KSerializer
import fr.mandarine.environmentquality.domain.repository.StationRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Hub'Eau hydrometry, river temperature and quality, groundwater quality and level, and flow observation. */
class HubEauStationRepository(
    private val http: HttpClient,
    private val baseUrl: String = "https://hubeau.eaufrance.fr/api",
    private val clock: Clock = Clock.systemUTC(),
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

    /** Readings are hourly, so this is about the last two weeks the station reported. */
    override suspend fun recentTemperatures(stationCode: String): List<TemperatureReading> =
        page(
            TemperatureDto.serializer(),
            buildUrl("$baseUrl/v1/temperature/chronique", "code_station" to stationCode, "size" to TEMPERATURE_READINGS, "sort" to "desc"),
        ).mapNotNull { dto ->
            val date = dto.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@mapNotNull null
            val time = dto.time?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: LocalTime.MIDNIGHT
            TemperatureReading(dto.resultat ?: return@mapNotNull null, LocalDateTime.of(date, time))
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
            waterBody = dto.waterBodies.joinToString(", ").ifEmpty { null },
            altitude = dto.altitude,
            location = point(dto.latitude, dto.longitude),
            lastSampledOn = dto.lastSampledOn?.let(::date),
        )
    }

    /** A sampling is a few hundred rows, one per substance; `fields` keeps the answer small. */
    override suspend fun groundwaterSampling(stationCode: String, sampledOn: LocalDate): GroundwaterSampling? {
        val rows = page(
            GroundwaterAnalysisDto.serializer(),
            buildUrl(
                "$baseUrl/v1/qualite_nappes/analyses",
                "bss_id" to stationCode,
                "date_debut_prelevement" to sampledOn,
                "sort" to "desc",
                "size" to GROUNDWATER_RESULTS,
                "fields" to GROUNDWATER_FIELDS,
            ),
        )
        return toSampling(rows)
    }

    /** Keeps the latest day in the rows: the station may have reported again since its listing was updated. */
    internal fun toSampling(rows: List<GroundwaterAnalysisDto>): GroundwaterSampling? {
        val dated = rows.mapNotNull { row -> row.sampledAt?.take(10)?.let(::date)?.let { it to row } }
        val day = dated.maxOfOrNull { it.first } ?: return null
        val results = dated.filter { it.first == day }.mapNotNull { (_, row) ->
            GroundwaterResult(
                code = row.code ?: return@mapNotNull null,
                name = row.name ?: row.code.toString(),
                value = row.resultat,
                unit = row.unit,
                quantified = row.remark == 1,
            )
        }
        return GroundwaterSampling(day, results)
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

    /** Every station of a department is checked on the same campaign day, about monthly. */
    override suspend fun latestFlows(stationCodes: List<String>): Map<String, FlowObservation> {
        if (stationCodes.isEmpty()) return emptyMap()
        val rows = page(
            FlowObservationDto.serializer(),
            buildUrl(
                "$baseUrl/v1/ecoulement/observations",
                "code_station" to stationCodes.joinToString(","),
                "sort" to "desc",
                "size" to stationCodes.size * FLOW_CAMPAIGNS,
                "fields" to "code_station,date_observation,code_ecoulement",
            ),
        )
        return rows
            .mapNotNull { dto -> dto.observedOn?.let(::date)?.let { dto.stationCode to FlowObservation(flowState(dto.code), it) } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, observations) -> observations.maxBy { it.observedOn } }
    }

    override suspend fun piezometers(departmentCode: String): List<PiezometerStation> =
        page(
            PiezometerStationDto.serializer(),
            buildUrl(
                "$baseUrl/v1/niveaux_nappes/stations",
                "code_departement" to departmentCode,
                "size" to 200,
                "fields" to "code_bss,nom_commune,libelle_pe,noms_masse_eau_edl,x,y,date_fin_mesure",
            ),
        ).map { dto ->
            PiezometerStation(
                code = dto.code,
                // Labels are often a bare well number, "PZ3": the commune says more.
                name = dto.commune ?: dto.label?.trim()?.ifEmpty { null } ?: dto.code,
                aquifer = dto.waterBodies.joinToString(", ").ifEmpty { null },
                location = point(dto.y, dto.x),
                lastMeasuredOn = dto.lastMeasuredOn?.let(::date),
            )
        }

    override suspend fun recentLevels(stationCode: String): List<GroundwaterLevel> =
        page(
            PiezometerReadingDto.serializer(),
            buildUrl(
                "$baseUrl/v1/niveaux_nappes/chroniques",
                "code_bss" to stationCode,
                "sort" to "desc",
                "size" to LEVEL_DAYS,
                "fields" to "date_mesure,niveau_nappe_eau,profondeur_nappe",
            ),
        ).mapNotNull { dto ->
            val day = dto.date?.let(::date) ?: return@mapNotNull null
            GroundwaterLevel(day, dto.level ?: return@mapNotNull null, dto.depth)
        }

    /** One call for every station within [RIVER_RADIUS_KM]: the rows carry each station's position. */
    override suspend fun riverQuality(centre: GeoPoint): RiverQuality? {
        val rows = page(
            RiverAnalysisDto.serializer(),
            buildUrl(
                "$baseUrl/v2/qualite_rivieres/analyse_pc",
                "longitude" to centre.longitude,
                "latitude" to centre.latitude,
                "distance" to RIVER_RADIUS_KM,
                "code_parametre" to RiverQuality.PARAMETERS.joinToString(","),
                "date_debut_prelevement" to LocalDate.now(clock).minusYears(1),
                "sort" to "desc",
                "size" to RIVER_RESULTS,
                "fields" to "code_station,libelle_station,longitude,latitude,date_prelevement,code_parametre,libelle_parametre,resultat,symbole_unite",
            ),
        )
        return toRiverQuality(rows, centre)
    }

    internal fun toRiverQuality(rows: List<RiverAnalysisDto>, centre: GeoPoint): RiverQuality? {
        val nearest = rows
            .groupBy { it.stationCode }
            .minByOrNull { (_, station) -> point(station.first().latitude, station.first().longitude)?.distanceKm(centre) ?: Double.MAX_VALUE }
            ?.value ?: return null
        val results = nearest
            .mapNotNull { row ->
                val code = row.code?.toIntOrNull() ?: return@mapNotNull null
                RiverResult(
                    code = code,
                    name = row.name ?: code.toString(),
                    value = row.resultat ?: return@mapNotNull null,
                    unit = row.unit,
                    sampledOn = row.sampledOn?.let(::date) ?: return@mapNotNull null,
                )
            }
            .groupBy { it.code }
            .map { (_, results) -> results.maxBy { it.sampledOn } }
        val first = nearest.first()
        return RiverQuality(first.stationCode, first.stationName ?: first.stationCode, point(first.latitude, first.longitude), results)
    }

    private fun flowState(code: String?) = when (code) {
        "1a" -> FlowState.FLOWING
        "1f" -> FlowState.LOW
        "1" -> FlowState.VISIBLE
        "2" -> FlowState.NOT_VISIBLE
        "3" -> FlowState.DRY
        else -> FlowState.UNKNOWN
    }

    private suspend fun <T> page(serializer: KSerializer<T>, url: String): List<T> =
        apiJson.decodeFromString(HubEauPage.serializer(serializer), http.get(url)).data

    private fun point(latitude: Double?, longitude: Double?) =
        if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null

    private fun instant(text: String): Instant? = runCatching { Instant.parse(text) }.getOrNull()

    private fun date(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()

    private companion object {
        const val TEMPERATURE_READINGS = 14 * 24
        const val GROUNDWATER_RESULTS = 1000
        const val GROUNDWATER_FIELDS = "date_debut_prelevement,code_param,nom_param,resultat,symbole_unite,code_remarque_analyse"
        /** Enough rows for each station's last few campaigns, in case one was skipped. */
        const val FLOW_CAMPAIGNS = 3
        const val LEVEL_DAYS = 40
        const val RIVER_RADIUS_KM = 20
        const val RIVER_RESULTS = 1000
    }
}
