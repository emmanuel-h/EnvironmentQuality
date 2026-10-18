package fr.mandarine.environmentquality.data.repository

import fr.mandarine.environmentquality.data.remote.FakeHttpClient
import fr.mandarine.environmentquality.domain.model.GeoPoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoCommuneRepositoryTest {

    private val http = FakeHttpClient(
        listOf(
            "/communes/27681" to "geo_commune_27681.json",
            "communes?" to "geo_communes_vernon.json",
        ),
    )
    private val repository = GeoCommuneRepository(http)

    @Test
    fun `should map name, department and a lat lon centre when searching by name`() = runTest {
        val vernon = repository.searchByName("Vernon").first()

        assertEquals("27681", vernon.code)
        assertEquals("Eure", vernon.departmentName)
        assertEquals("Normandie", vernon.regionName)
        assertEquals(25290, vernon.population)
        assertEquals(GeoPoint(49.0921, 1.4827), vernon.centre)
        assertTrue(http.requests.single().contains("nom=Vernon"))
    }

    @Test
    fun `should read a single commune when fetching by code`() = runTest {
        assertEquals("Vernon", repository.byCode("27681")?.name)
    }

    @Test
    fun `should return null when the code does not exist`() = runTest {
        assertNull(repository.byCode("99999"))
    }
}
