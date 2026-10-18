package fr.mandarine.environmentquality.domain.repository

import fr.mandarine.environmentquality.domain.model.GeoPoint

/** The device's approximate position, or null when unknown or not permitted. */
fun interface LocationProvider {
    suspend fun current(): GeoPoint?
}
