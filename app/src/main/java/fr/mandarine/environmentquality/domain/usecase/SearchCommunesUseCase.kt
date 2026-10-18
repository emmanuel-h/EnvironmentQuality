package fr.mandarine.environmentquality.domain.usecase

import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.repository.CommuneRepository

/**
 * Five digits are a postal code; anything else of two characters or more is a
 * name. Homonyms come back sorted by population so the likely one is first.
 */
class SearchCommunesUseCase(private val communes: CommuneRepository) {

    suspend operator fun invoke(query: String): List<Commune> {
        val trimmed = query.trim()
        return when {
            POSTAL_CODE.matches(trimmed) -> communes.searchByPostalCode(trimmed)
            trimmed.all { it.isDigit() } -> emptyList()
            trimmed.length >= MIN_LENGTH -> communes.searchByName(trimmed)
            else -> emptyList()
        }.sortedByDescending { it.population ?: 0 }
    }

    companion object {
        const val MIN_LENGTH = 2
        private val POSTAL_CODE = Regex("""\d{5}""")
    }
}
