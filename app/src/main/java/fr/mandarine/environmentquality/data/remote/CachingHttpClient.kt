package fr.mandarine.environmentquality.data.remote

import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** Stored responses keyed by URL. Backed by Room in the app, by a map in tests. */
interface ResponseCache {
    suspend fun read(url: String): CachedResponse?
    suspend fun write(url: String, response: CachedResponse)
}

data class CachedResponse(val body: String, val fetchedAt: Instant)

/**
 * Serves a stored response while it is younger than the URL's max age, and an
 * older one when the network fails, so a commune viewed once stays readable offline.
 */
class CachingHttpClient(
    private val network: HttpClient,
    private val cache: ResponseCache,
    private val clock: Clock = Clock.systemUTC(),
    private val maxAge: (String) -> Duration = ::defaultMaxAge,
) : HttpClient {

    override suspend fun get(url: String): String {
        val now = clock.instant()
        val cached = cache.read(url)
        if (cached != null && Duration.between(cached.fetchedAt, now) < maxAge(url)) return cached.body
        return try {
            network.get(url).also { cache.write(url, CachedResponse(it, now)) }
        } catch (e: IOException) {
            cached?.body ?: throw e
        }
    }

    companion object {
        /** Live river data moves every few minutes; restrictions daily; the rest monthly. */
        fun defaultMaxAge(url: String): Duration = when {
            "observations_tr" in url -> Duration.ofMinutes(10)
            "vigieau" in url -> Duration.ofHours(3)
            else -> Duration.ofHours(24)
        }
    }
}
