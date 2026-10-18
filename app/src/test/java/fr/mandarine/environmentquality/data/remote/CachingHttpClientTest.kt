package fr.mandarine.environmentquality.data.remote

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class CachingHttpClientTest {

    private class MemoryCache : ResponseCache {
        val entries = mutableMapOf<String, CachedResponse>()
        override suspend fun read(url: String) = entries[url]
        override suspend fun write(url: String, response: CachedResponse) {
            entries[url] = response
        }
    }

    private val now = Instant.parse("2026-10-18T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val cache = MemoryCache()
    private var calls = 0
    private var failing = false
    private val network = HttpClient {
        calls++
        if (failing) throw IOException("offline") else "fresh"
    }
    private val client = CachingHttpClient(network, cache, clock) { Duration.ofHours(1) }

    @Test
    fun `should serve the cached body when it is younger than the max age`() = runTest {
        cache.entries["u"] = CachedResponse("cached", now.minusSeconds(60))

        assertEquals("cached", client.get("u"))
        assertEquals(0, calls)
    }

    @Test
    fun `should refetch and store when the cached body is too old`() = runTest {
        cache.entries["u"] = CachedResponse("cached", now.minus(Duration.ofHours(2)))

        assertEquals("fresh", client.get("u"))
        assertEquals(CachedResponse("fresh", now), cache.entries["u"])
    }

    @Test
    fun `should fall back to a stale body when the network fails`() = runTest {
        cache.entries["u"] = CachedResponse("stale", now.minus(Duration.ofDays(3)))
        failing = true

        assertEquals("stale", client.get("u"))
    }

    @Test(expected = IOException::class)
    fun `should rethrow when the network fails and nothing is cached`() = runTest {
        failing = true

        client.get("u")
    }

    @Test
    fun `should keep live river data for minutes only`() {
        assertEquals(Duration.ofMinutes(10), CachingHttpClient.defaultMaxAge("https://x/hydrometrie/observations_tr?a"))
        assertEquals(Duration.ofHours(24), CachingHttpClient.defaultMaxAge("https://x/resultats_dis"))
    }
}
