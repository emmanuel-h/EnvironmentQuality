package fr.mandarine.environmentquality.data.repository

import fr.mandarine.environmentquality.data.remote.GeoCommuneDto
import fr.mandarine.environmentquality.data.remote.HttpClient
import fr.mandarine.environmentquality.data.remote.HttpStatusException
import fr.mandarine.environmentquality.data.remote.apiJson
import fr.mandarine.environmentquality.data.remote.buildUrl
import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.model.GeoPoint
import fr.mandarine.environmentquality.domain.repository.CommuneRepository

/** API Découpage administratif (geo.api.gouv.fr). */
class GeoCommuneRepository(
    private val http: HttpClient,
    private val baseUrl: String = "https://geo.api.gouv.fr",
) : CommuneRepository {

    override suspend fun searchByName(name: String, limit: Int): List<Commune> =
        list(buildUrl("$baseUrl/communes", "nom" to name, "fields" to FIELDS, "boost" to "population", "limit" to limit))

    override suspend fun searchByPostalCode(postalCode: String): List<Commune> =
        list(buildUrl("$baseUrl/communes", "codePostal" to postalCode, "fields" to FIELDS))

    override suspend fun byCode(code: String): Commune? = try {
        toCommune(apiJson.decodeFromString<GeoCommuneDto>(http.get(buildUrl("$baseUrl/communes/$code", "fields" to FIELDS))))
    } catch (e: HttpStatusException) {
        if (e.status == 404) null else throw e
    }

    override suspend fun at(latitude: Double, longitude: Double): Commune? =
        list(buildUrl("$baseUrl/communes", "lat" to latitude, "lon" to longitude, "fields" to FIELDS)).firstOrNull()

    private suspend fun list(url: String): List<Commune> =
        apiJson.decodeFromString<List<GeoCommuneDto>>(http.get(url)).map(::toCommune)

    private fun toCommune(dto: GeoCommuneDto) = Commune(
        code = dto.code,
        name = dto.nom,
        postalCodes = dto.codesPostaux,
        departmentCode = dto.departement?.code,
        departmentName = dto.departement?.nom,
        regionName = dto.region?.nom,
        population = dto.population,
        centre = dto.centre?.coordinates?.takeIf { it.size >= 2 }?.let { GeoPoint(latitude = it[1], longitude = it[0]) },
    )

    private companion object {
        const val FIELDS = "nom,code,codesPostaux,centre,population,departement,region"
    }
}
