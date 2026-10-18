package fr.mandarine.environmentquality.domain.usecase

import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.repository.CommuneRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchCommunesUseCaseTest {

    private val repository = mockk<CommuneRepository>()
    private val search = SearchCommunesUseCase(repository)

    @Test
    fun `should search by postal code when the query is five digits`() = runTest {
        coEvery { repository.searchByPostalCode("27200") } returns listOf(Commune("27681", "Vernon"))

        val result = search(" 27200 ")

        assertEquals("27681", result.single().code)
        coVerify(exactly = 0) { repository.searchByName(any(), any()) }
    }

    @Test
    fun `should sort homonyms by population when searching by name`() = runTest {
        coEvery { repository.searchByName("Vernon", any()) } returns listOf(
            Commune("07336", "Vernon", population = 235),
            Commune("27681", "Vernon", population = 25290),
        )

        val result = search("Vernon")

        assertEquals(listOf("27681", "07336"), result.map { it.code })
    }

    @Test
    fun `should not call the api when the query is too short or a partial postal code`() = runTest {
        assertTrue(search("V").isEmpty())
        assertTrue(search("272").isEmpty())
        coVerify(exactly = 0) { repository.searchByName(any(), any()) }
        coVerify(exactly = 0) { repository.searchByPostalCode(any()) }
    }
}
