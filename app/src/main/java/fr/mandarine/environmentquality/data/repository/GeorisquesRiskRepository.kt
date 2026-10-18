package fr.mandarine.environmentquality.data.repository

import fr.mandarine.environmentquality.data.remote.CatnatDto
import fr.mandarine.environmentquality.data.remote.GeorisquesPage
import fr.mandarine.environmentquality.data.remote.HttpClient
import fr.mandarine.environmentquality.data.remote.InstallationDto
import fr.mandarine.environmentquality.data.remote.RadonDto
import fr.mandarine.environmentquality.data.remote.RisquesCommuneDto
import fr.mandarine.environmentquality.data.remote.SeismicDto
import fr.mandarine.environmentquality.data.remote.apiJson
import fr.mandarine.environmentquality.data.remote.buildUrl
import fr.mandarine.environmentquality.domain.model.CommuneRisks
import fr.mandarine.environmentquality.domain.model.GeoPoint
import fr.mandarine.environmentquality.domain.model.IndustrialSite
import fr.mandarine.environmentquality.domain.model.NaturalDisaster
import fr.mandarine.environmentquality.domain.model.RiskGroup
import fr.mandarine.environmentquality.domain.model.SevesoStatus
import fr.mandarine.environmentquality.domain.repository.RiskRepository
import kotlinx.coroutines.async
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

    private suspend fun <T> page(serializer: KSerializer<T>, path: String, communeCode: String): List<T> {
        val url = buildUrl("$baseUrl/$path", "code_insee" to communeCode, "page_size" to PAGE_SIZE)
        return apiJson.decodeFromString(GeorisquesPage.serializer(serializer), http.get(url)).data
    }

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

    private fun toSite(dto: InstallationDto) = IndustrialSite(
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
    )

    private fun sevesoRank(status: SevesoStatus) = when (status) {
        SevesoStatus.HIGH -> 0
        SevesoStatus.LOW -> 1
        SevesoStatus.NONE -> 2
        SevesoStatus.UNKNOWN -> 3
    }

    private fun parseFrenchDate(text: String?): LocalDate? =
        text?.let { runCatching { LocalDate.parse(it, FRENCH_DATE) }.getOrNull() }

    private companion object {
        const val PAGE_SIZE = 100
        val FRENCH_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
}
