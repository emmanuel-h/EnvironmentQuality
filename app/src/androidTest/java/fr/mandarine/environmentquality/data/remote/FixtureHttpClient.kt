package fr.mandarine.environmentquality.data.remote

import androidx.test.platform.app.InstrumentationRegistry

/**
 * Answers each request with the first fixture whose key the URL contains. The
 * fixtures are the unit tests' `fixtures/`, packaged as the test APK's assets.
 */
class FixtureHttpClient(private val routes: List<Pair<String, String>>) : HttpClient {

    override suspend fun get(url: String): String {
        val fixture = routes.firstOrNull { (key, _) -> key in url }?.second
            ?: throw HttpStatusException(404, url)
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        return assets.open("fixtures/$fixture").bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}
