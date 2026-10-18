package fr.mandarine.environmentquality.domain.model

import java.time.LocalDate

/** VigiEau gravity levels, in increasing order. */
enum class DroughtLevel {
    NONE,
    VIGILANCE,
    ALERT,
    REINFORCED_ALERT,
    CRISIS,
    ;

    companion object {
        fun fromCode(code: String?): DroughtLevel = when (code?.trim()?.lowercase()?.replace('_', ' ')) {
            "vigilance" -> VIGILANCE
            "alerte" -> ALERT
            "alerte renforcee", "alerte renforcée" -> REINFORCED_ALERT
            "crise" -> CRISIS
            else -> NONE
        }
    }
}

/** SUP surface water, SOU groundwater, AEP drinking water. */
enum class WaterResource {
    SURFACE,
    GROUNDWATER,
    DRINKING_WATER,
    ;

    companion object {
        fun fromCode(code: String?): WaterResource? = when (code?.uppercase()) {
            "SUP" -> SURFACE
            "SOU" -> GROUNDWATER
            "AEP" -> DRINKING_WATER
            else -> null
        }
    }
}

enum class Audience { INDIVIDUALS, COMMUNITIES, FARMS, COMPANIES }

data class WaterUse(
    val name: String,
    val theme: String?,
    val rule: String,
    val audiences: Set<Audience>,
)

data class DroughtZone(
    val name: String,
    val resource: WaterResource?,
    val level: DroughtLevel,
    val validFrom: LocalDate?,
    val validUntil: LocalDate?,
    val decreeUrl: String?,
    val uses: List<WaterUse>,
)

data class DroughtStatus(val zones: List<DroughtZone>) {
    val level: DroughtLevel get() = zones.maxOfOrNull { it.level } ?: DroughtLevel.NONE
    val worstZone: DroughtZone? get() = zones.maxByOrNull { it.level }
    val validUntil: LocalDate? get() = worstZone?.validUntil

    /** Every use, merged across zones, keeping the strictest zone's wording first. */
    fun usesFor(audience: Audience): List<WaterUse> =
        zones.sortedByDescending { it.level }
            .flatMap { it.uses }
            .filter { audience in it.audiences }
            .distinctBy { it.name.trim().lowercase() }
}
