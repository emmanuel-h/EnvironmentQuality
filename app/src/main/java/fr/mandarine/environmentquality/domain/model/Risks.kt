package fr.mandarine.environmentquality.domain.model

import java.time.LocalDate

/** A family of risks (Inondation) and its detailed types. */
data class RiskGroup(val code: String, val name: String, val details: List<String>)

data class NaturalDisaster(
    val label: String,
    val start: LocalDate?,
    val end: LocalDate?,
    val published: LocalDate?,
)

enum class SevesoStatus { HIGH, LOW, NONE, UNKNOWN }

data class IndustrialSite(
    val name: String,
    val regime: String?,
    val seveso: SevesoStatus,
    val activity: String?,
    val location: GeoPoint?,
    val lastInspection: LocalDate? = null,
    /** The published report of the most recent inspection that has one; older visits often have none. */
    val inspectionReportUrl: String? = null,
)

data class CommuneRisks(
    val groups: List<RiskGroup>,
    val disasters: List<NaturalDisaster>,
    val radonClass: Int?,
    val seismicZone: Int?,
    val sites: List<IndustrialSite>,
) {
    /** Families and their detailed types, counted the way Géorisques lists them. */
    val riskCount: Int get() = groups.size + groups.sumOf { it.details.size }
    val hasForestFire: Boolean get() = groups.any { "feu" in it.name.lowercase() || "incendie" in it.name.lowercase() }
    val sevesoCount: Int get() = sites.count { it.seveso == SevesoStatus.HIGH || it.seveso == SevesoStatus.LOW }
}
