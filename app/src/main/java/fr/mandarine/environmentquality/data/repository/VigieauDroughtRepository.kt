package fr.mandarine.environmentquality.data.repository

import fr.mandarine.environmentquality.data.remote.HttpClient
import fr.mandarine.environmentquality.data.remote.VigieauUsageDto
import fr.mandarine.environmentquality.data.remote.VigieauZoneDto
import fr.mandarine.environmentquality.data.remote.apiJson
import fr.mandarine.environmentquality.data.remote.buildUrl
import fr.mandarine.environmentquality.domain.model.Audience
import fr.mandarine.environmentquality.domain.model.DroughtLevel
import fr.mandarine.environmentquality.domain.model.DroughtStatus
import fr.mandarine.environmentquality.domain.model.DroughtZone
import fr.mandarine.environmentquality.domain.model.WaterResource
import fr.mandarine.environmentquality.domain.model.WaterUse
import fr.mandarine.environmentquality.domain.repository.DroughtRepository
import java.time.LocalDate

/** VigiEau: drought restriction zones covering a commune. A bare array, empty when none apply. */
class VigieauDroughtRepository(
    private val http: HttpClient,
    private val baseUrl: String = "https://api.vigieau.gouv.fr/api",
) : DroughtRepository {

    override suspend fun status(communeCode: String): DroughtStatus {
        val zones = apiJson.decodeFromString<List<VigieauZoneDto>>(http.get(buildUrl("$baseUrl/zones", "commune" to communeCode)))
        return DroughtStatus(zones.map(::toZone))
    }

    private fun toZone(dto: VigieauZoneDto) = DroughtZone(
        name = dto.nom.orEmpty().trim(),
        resource = WaterResource.fromCode(dto.type),
        level = DroughtLevel.fromCode(dto.niveauGravite),
        validFrom = parseDate(dto.arrete?.dateDebutValidite),
        validUntil = parseDate(dto.arrete?.dateFinValidite),
        decreeUrl = dto.arrete?.cheminFichier,
        uses = dto.usages.map(::toUse),
    )

    private fun toUse(dto: VigieauUsageDto) = WaterUse(
        name = dto.nom.trim(),
        theme = dto.thematique,
        rule = dto.description.orEmpty().trim(),
        audiences = buildSet {
            if (dto.concerneParticulier) add(Audience.INDIVIDUALS)
            if (dto.concerneCollectivite) add(Audience.COMMUNITIES)
            if (dto.concerneExploitation) add(Audience.FARMS)
            if (dto.concerneEntreprise) add(Audience.COMPANIES)
        },
    )

    /** `2026-11-30T00:00:00.000Z`: the decree's calendar day is the date part. */
    private fun parseDate(text: String?): LocalDate? =
        text?.takeIf { it.length >= 10 }?.let { runCatching { LocalDate.parse(it.substring(0, 10)) }.getOrNull() }
}
