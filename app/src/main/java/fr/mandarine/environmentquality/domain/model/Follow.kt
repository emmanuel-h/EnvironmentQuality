package fr.mandarine.environmentquality.domain.model

import java.time.Instant

/** A commune the user follows, with what was known at the last check. */
data class FollowedCommune(
    val commune: Commune,
    val lastSampleCode: String?,
    val lastStatus: WaterStatus,
    val lastSampledAt: Instant?,
    /** True until the user opens the commune after a new sample arrived. */
    val hasUpdate: Boolean,
)

data class AlertSettings(
    val nonConform: Boolean = true,
    val newSample: Boolean = true,
    val referenceExceeded: Boolean = false,
)

/** Something worth a notification, found by comparing a fresh sample with the stored one. */
data class WaterAlert(val commune: Commune, val kind: Kind, val sample: WaterSample) {
    enum class Kind { NON_CONFORM, REFERENCE_EXCEEDED, NEW_SAMPLE }
}
