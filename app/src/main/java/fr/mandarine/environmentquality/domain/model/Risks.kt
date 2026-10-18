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

/** Exposure to clay shrinking and swelling with drought, which cracks buildings. */
enum class ClayExposure { NONE, LOW, MEDIUM, HIGH }

enum class RiskPlanKind { NATURAL, TECHNOLOGICAL, MINING }

/** A risk prevention plan (PPR): a regulation that zones where and how one may build. */
data class RiskPlan(val name: String, val kind: RiskPlanKind, val procedure: String?, val updated: LocalDate?)

enum class FloodAreaKind {
    /** Territoire à risque important d'inondation, mapped under the EU Floods Directive. */
    HIGH_RISK_TERRITORY,

    /** Atlas des zones inondables: where past floods reached. */
    FLOOD_ATLAS,
}

data class FloodArea(val name: String, val kind: FloodAreaKind, val published: LocalDate?)

/** A soil information sector (SIS): land known to be polluted, which the State has listed. */
data class SoilSector(val name: String, val address: String?, val sheetUrl: String?)

/** Counts are null when their source did not answer. */
data class PollutedSoils(
    val sectors: List<SoilSector>,
    /** Polluted sites the administration has dealt with, and how many of those are still open. */
    val actionSites: Int?,
    val openActions: Int,
    /** Former industrial or service activities listed by the CASIAS inventory. */
    val formerActivities: Int?,
)

/**
 * What the Risks screen adds to [CommuneRisks]. Each part is null or empty when its
 * source did not answer: one missing source must not hide the others.
 */
data class RiskDetails(
    /** At the commune's centre; [ClayExposure.NONE] when no exposure is mapped there. */
    val clay: ClayExposure?,
    val plans: List<RiskPlan>,
    val floodAreas: List<FloodArea>,
    val soils: PollutedSoils?,
)
