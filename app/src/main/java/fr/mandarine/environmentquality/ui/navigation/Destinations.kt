package fr.mandarine.environmentquality.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Screens are keyed by commune INSEE code; each screen fetches what it needs. */
@Serializable data object MapKey : NavKey

@Serializable data object SearchKey : NavKey

@Serializable data object FollowsKey : NavKey

@Serializable data object SourcesKey : NavKey

@Serializable data class CommuneKey(val code: String) : NavKey

@Serializable data class ParametersKey(val code: String) : NavKey

@Serializable data class HistoryKey(val code: String) : NavKey

@Serializable data class NearbyKey(val code: String) : NavKey

@Serializable data class CompareKey(val code: String) : NavKey

@Serializable data class DroughtKey(val code: String) : NavKey

@Serializable data class RisksKey(val code: String) : NavKey

/** The four bottom tabs, each the root of its own back stack. */
val TabRoots: List<NavKey> = listOf(MapKey, SearchKey, FollowsKey, SourcesKey)
