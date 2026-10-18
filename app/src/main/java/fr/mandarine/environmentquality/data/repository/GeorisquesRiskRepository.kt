package fr.mandarine.environmentquality.data.repository

import fr.mandarine.environmentquality.data.remote.AziDto
import fr.mandarine.environmentquality.data.remote.CatnatDto
import fr.mandarine.environmentquality.data.remote.GasparPlanPage
import fr.mandarine.environmentquality.data.remote.GeorisquesPage
import fr.mandarine.environmentquality.data.remote.HttpClient
import fr.mandarine.environmentquality.data.remote.InstallationDto
import fr.mandarine.environmentquality.data.remote.RadonDto
import fr.mandarine.environmentquality.data.remote.RgaDto
import fr.mandarine.environmentquality.data.remote.RisquesCommuneDto
import fr.mandarine.environmentquality.data.remote.SeismicDto
import fr.mandarine.environmentquality.data.remote.SoilActionDto
import fr.mandarine.environmentquality.data.remote.SoilSectorDto
import fr.mandarine.environmentquality.data.remote.TriDto
import fr.mandarine.environmentquality.data.remote.apiJson
import fr.mandarine.environmentquality.data.remote.buildUrl
import fr.mandarine.environmentquality.domain.model.ClayExposure
import fr.mandarine.environmentquality.domain.model.CommuneRisks
import fr.mandarine.environmentquality.domain.model.FloodArea
import fr.mandarine.environmentquality.domain.model.FloodAreaKind
import fr.mandarine.environmentquality.domain.model.GeoPoint
import fr.mandarine.environmentquality.domain.model.IndustrialSite
import fr.mandarine.environmentquality.domain.model.NaturalDisaster
import fr.mandarine.environmentquality.domain.model.PollutedSoils
import fr.mandarine.environmentquality.domain.model.RiskDetails
import fr.mandarine.environmentquality.domain.model.RiskPlan
import fr.mandarine.environmentquality.domain.model.RiskPlanKind
import fr.mandarine.environmentquality.domain.model.RiskGroup
import fr.mandarine.environmentquality.domain.model.SevesoStatus
import fr.mandarine.environmentquality.domain.model.SoilSector
import fr.mandarine.environmentquality.domain.repository.RiskRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.KSerializer
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Géorisques: five calls per commune, made in parallel and merged. */
class GeorisquesRiskRepository(
    private val http: HttpClient,
    private val baseUrl: String = "https://georisques.gouv.fr/api/v1",
) : RiskRepository {

    override suspend fun risks(communeCode: String): CommuneRisks = coroutineScope {
        val risks = async { page(RisquesCommuneDto.serializer(), "gaspar/risques", communeCode) }
        val catnat = async { page(CatnatDto.serializer(), "gaspar/catnat", communeCode) }
        val radon = async { page(RadonDto.serializer(), "radon", communeCode) }
        val seismic = async { page(SeismicDto.serializer(), "zonage_sismique", communeCode) }
        val sites = async { page(InstallationDto.serializer(), "installations_classees", communeCode) }
        CommuneRisks(
            groups = toGroups(risks.await().flatMap { it.details.map { d -> d.code to d.label } }),
            disasters = catnat.await().map(::toDisaster).sortedByDescending { it.start },
            radonClass = radon.await().firstOrNull()?.potentialClass?.trim()?.toIntOrNull(),
            seismicZone = seismic.await().firstOrNull()?.zone?.trim()?.toIntOrNull(),
            sites = sites.await().map(::toSite).sortedBy { sevesoRank(it.seveso) },
        )
    }

    private suspend fun <T> page(serializer: KSerializer<T>, path: String, communeCode: String): List<T> =
        envelope(serializer, path, communeCode, PAGE_SIZE).data

    private suspend fun <T> envelope(serializer: KSerializer<T>, path: String, communeCode: String, pageSize: Int): GeorisquesPage<T> {
        val url = buildUrl("$baseUrl/$path", "code_insee" to communeCode, "page_size" to pageSize)
        return apiJson.decodeFromString(GeorisquesPage.serializer(serializer), http.get(url))
    }

    /** Nine more calls, in parallel; each source that fails only empties its own part. */
    override suspend fun riskDetails(communeCode: String, centre: GeoPoint?): RiskDetails = coroutineScope {
        val clay = async { centre?.let { attempt { clay(it) } } }
        val plans = PLAN_KINDS.map { (path, kind) -> async { attempt { plans(path, kind, communeCode) }.orEmpty() } }
        val territories = async {
            attempt {
                page(TriDto.serializer(), "gaspar/tri", communeCode)
                    .map { FloodArea(cleanName(it.name), FloodAreaKind.HIGH_RISK_TERRITORY, parseFrenchDate(it.mapsApproved)) }
            }.orEmpty()
        }
        val atlases = async {
            attempt {
                page(AziDto.serializer(), "gaspar/azi", communeCode)
                    .map { FloodArea(cleanName(it.name), FloodAreaKind.FLOOD_ATLAS, parseFrenchDate(it.published)) }
            }.orEmpty()
        }
        val soils = async { soils(communeCode) }
        RiskDetails(
            clay = clay.await(),
            plans = plans.awaitAll().flatten(),
            floodAreas = territories.await() + atlases.await(),
            soils = soils.await(),
        )
    }

    private suspend fun clay(centre: GeoPoint): ClayExposure? {
        val body = http.get(buildUrl("$baseUrl/rga", "latlon" to "${centre.longitude},${centre.latitude}"))
        if (body.isBlank()) return ClayExposure.NONE
        return when (apiJson.decodeFromString(RgaDto.serializer(), body).codeExposition?.trim()) {
            "0" -> ClayExposure.NONE
            "1" -> ClayExposure.LOW
            "2" -> ClayExposure.MEDIUM
            "3" -> ClayExposure.HIGH
            else -> null
        }
    }

    private suspend fun plans(path: String, kind: RiskPlanKind, communeCode: String): List<RiskPlan> {
        val body = http.get(buildUrl("$baseUrl/gaspar/$path", "codeInsee" to communeCode))
        return apiJson.decodeFromString(GasparPlanPage.serializer(), body).content.map {
            RiskPlan(it.libPpr.trim(), kind, it.modeleProcedure, parseFrenchDate(it.dateModification))
        }
    }

    /** Null only when none of the three inventories answered. */
    private suspend fun soils(communeCode: String): PollutedSoils? = coroutineScope {
        val sectors = async { attempt { page(SoilSectorDto.serializer(), "ssp/conclusions_sis", communeCode) } }
        val actions = async { attempt { envelope(SoilActionDto.serializer(), "ssp/instructions", communeCode, PAGE_SIZE) } }
        // Only the total is read: each CASIAS row carries the site's outline.
        val former = async { attempt { envelope(SoilActionDto.serializer(), "ssp/casias", communeCode, 1).results } }
        val sectorRows = sectors.await()
        val actionPage = actions.await()
        val formerCount = former.await()
        if (sectorRows == null && actionPage == null && formerCount == null) return@coroutineScope null
        PollutedSoils(
            sectors = sectorRows.orEmpty().map { SoilSector(it.nom.trim(), it.adresse?.trim()?.ifEmpty { null }, it.sheet) },
            actionSites = actionPage?.results,
            openActions = actionPage?.data.orEmpty().count { it.statut.equals("En cours", ignoreCase = true) },
            formerActivities = formerCount,
        )
    }

    /** Runs one source's call; its failure leaves that part empty rather than failing the screen. */
    private suspend fun <T> attempt(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    /** GASPAR labels sometimes end with a stray comma: "Atlas des PHEC,". */
    private fun cleanName(name: String) = name.trim().trimEnd(',').trim()

    /**
     * GASPAR codes nest: `11` Inondation, `112` one of its types. A two-digit code
     * starts a family; longer codes belong to the family sharing their first two digits.
     */
    internal fun toGroups(details: List<Pair<String, String>>): List<RiskGroup> {
        val families = details.filter { it.first.length <= 2 }
        val known = families.map { it.first }.toSet()
        val orphans = details.filter { it.first.length > 2 && it.first.take(2) !in known }
        return families.map { (code, label) ->
            RiskGroup(code, label, details.filter { it.first.length > 2 && it.first.startsWith(code) }.map { it.second })
        } + orphans.map { (code, label) -> RiskGroup(code, label, emptyList()) }
    }

    private fun toDisaster(dto: CatnatDto) = NaturalDisaster(
        label = dto.label,
        start = parseFrenchDate(dto.start),
        end = parseFrenchDate(dto.end),
        published = parseFrenchDate(dto.published),
    )

    private fun toSite(dto: InstallationDto): IndustrialSite {
        val inspections = dto.inspections
            .mapNotNull { inspection -> parseIsoDate(inspection.dateInspection)?.let { it to inspection.fichierInspection?.urlFichier } }
            .sortedByDescending { it.first }
        return IndustrialSite(
            name = dto.raisonSociale.trim(),
            regime = dto.regime,
            seveso = when (dto.statutSeveso?.lowercase()) {
                null -> SevesoStatus.UNKNOWN
                "seveso seuil haut" -> SevesoStatus.HIGH
                "seveso seuil bas" -> SevesoStatus.LOW
                else -> if ("haut" in dto.statutSeveso.lowercase()) SevesoStatus.HIGH else SevesoStatus.NONE
            },
            activity = dto.etatActivite,
            location = if (dto.latitude != null && dto.longitude != null) GeoPoint(dto.latitude, dto.longitude) else null,
            lastInspection = inspections.firstOrNull()?.first,
            inspectionReportUrl = inspections.firstNotNullOfOrNull { it.second?.takeIf(String::isNotBlank) },
        )
    }

    private fun sevesoRank(status: SevesoStatus) = when (status) {
        SevesoStatus.HIGH -> 0
        SevesoStatus.LOW -> 1
        SevesoStatus.NONE -> 2
        SevesoStatus.UNKNOWN -> 3
    }

    private fun parseIsoDate(text: String?): LocalDate? =
        text?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private fun parseFrenchDate(text: String?): LocalDate? =
        text?.let { runCatching { LocalDate.parse(it, FRENCH_DATE) }.getOrNull() }

    private companion object {
        const val PAGE_SIZE = 100
        val PLAN_KINDS = listOf(
            "pprn" to RiskPlanKind.NATURAL,
            "pprt" to RiskPlanKind.TECHNOLOGICAL,
            "pprm" to RiskPlanKind.MINING,
        )
        val FRENCH_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
}
