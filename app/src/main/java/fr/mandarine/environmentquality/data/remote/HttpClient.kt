package fr.mandarine.environmentquality.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Fetches a URL and returns its body. Implementations throw [IOException] on failure. */
fun interface HttpClient {
    suspend fun get(url: String): String
}

class HttpStatusException(val status: Int, url: String) : IOException("HTTP $status for $url")

/** The platform's own client: the app makes a handful of GETs and needs nothing more. */
class UrlConnectionHttpClient(
    private val userAgent: String,
    private val timeoutMillis: Int = 15_000,
) : HttpClient {

    override suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", userAgent)
            val status = connection.responseCode
            // Hub'Eau answers 206 Partial Content when more pages exist.
            if (status !in 200..299) throw HttpStatusException(status, url)
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

/** Builds `base?k=v&…`, skipping null values and encoding the rest. */
internal fun buildUrl(base: String, vararg params: Pair<String, Any?>): String {
    val query = params
        .filter { it.second != null }
        .joinToString("&") { (key, value) -> key + "=" + URLEncoder.encode(value.toString(), "UTF-8") }
    return if (query.isEmpty()) base else "$base?$query"
}
