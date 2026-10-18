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

/** Great-circle distance in kilometres. */
fun GeoPoint.distanceKm(other: GeoPoint): Double {
    val r = 6371.0
    val dLat = Math.toRadians(other.latitude - latitude)
    val dLon = Math.toRadians(other.longitude - longitude)
    val a = kotlin.math.sin(dLat / 2).let { it * it } +
        kotlin.math.cos(Math.toRadians(latitude)) * kotlin.math.cos(Math.toRadians(other.latitude)) *
        kotlin.math.sin(dLon / 2).let { it * it }
    return 2 * r * kotlin.math.asin(kotlin.math.sqrt(a))
}
