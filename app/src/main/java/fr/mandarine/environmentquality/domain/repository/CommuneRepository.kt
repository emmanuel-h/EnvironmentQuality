package fr.mandarine.environmentquality.domain.repository

import fr.mandarine.environmentquality.domain.model.Commune

interface CommuneRepository {
    suspend fun searchByName(name: String, limit: Int = 10): List<Commune>
    suspend fun searchByPostalCode(postalCode: String): List<Commune>
    suspend fun byCode(code: String): Commune?
    suspend fun at(latitude: Double, longitude: Double): Commune?
}
