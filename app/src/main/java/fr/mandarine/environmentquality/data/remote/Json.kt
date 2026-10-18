package fr.mandarine.environmentquality.data.remote

import kotlinx.serialization.json.Json

/** The APIs add fields over time; unknown ones must not break parsing. */
internal val apiJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}
