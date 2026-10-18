package fr.mandarine.environmentquality.domain.model

/** A point in WGS84 degrees. */
data class GeoPoint(val latitude: Double, val longitude: Double)

/**
 * A French municipality, identified by its INSEE code. Two communes can share a
 * name (there are three Vernons), so the department travels with it.
 */
data class Commune(
    val code: String,
    val name: String,
    val postalCodes: List<String> = emptyList(),
    val departmentCode: String? = null,
    val departmentName: String? = null,
    val regionName: String? = null,
    val population: Int? = null,
    val centre: GeoPoint? = null,
)
