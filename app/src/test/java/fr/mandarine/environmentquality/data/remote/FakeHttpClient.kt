package fr.mandarine.environmentquality.data.remote

/** Answers each request with the first fixture whose key the URL contains, and records the URLs. */
class FakeHttpClient(private val routes: List<Pair<String, String>>) : HttpClient {

    val requests = mutableListOf<String>()

    override suspend fun get(url: String): String {
        requests += url
        val fixture = routes.firstOrNull { (key, _) -> key in url }?.second
            ?: throw HttpStatusException(404, url)
        return fixture(fixture)
    }

    companion object {
        fun fixture(name: String): String =
            requireNotNull(FakeHttpClient::class.java.getResource("/fixtures/$name")) { "Missing fixture $name" }.readText()
    }
}
