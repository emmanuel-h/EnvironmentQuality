package fr.mandarine.environmentquality.domain.model

import java.time.Instant

/**
 * One of the four conformity flags Hub'Eau publishes per sample.
 * `C` conforme, `N` non conforme, `S` when the family was not analysed.
 */
enum class Conformity {
    CONFORM,
    NON_CONFORM,
    NOT_ANALYSED,
    ;

    companion object {
        fun fromCode(code: String?): Conformity = when (code?.trim()?.uppercase()) {
            "C" -> CONFORM
            "N", "D" -> NON_CONFORM
            else -> NOT_ANALYSED
        }
    }
}

/**
 * What the app shows in colour. A breached quality limit is a health matter; a
 * breached quality reference is only an indicator, hence "réserves".
 */
enum class WaterStatus { CONFORM, RESERVES, NON_CONFORM, UNKNOWN }

data class ConformityFlags(
    val bacteriologyLimits: Conformity,
    val physicoChemicalLimits: Conformity,
    val bacteriologyReferences: Conformity,
    val physicoChemicalReferences: Conformity,
) {
    val status: WaterStatus
        get() {
            val limits = listOf(bacteriologyLimits, physicoChemicalLimits)
            val references = listOf(bacteriologyReferences, physicoChemicalReferences)
            return when {
                Conformity.NON_CONFORM in limits -> WaterStatus.NON_CONFORM
                Conformity.NON_CONFORM in references -> WaterStatus.RESERVES
                Conformity.CONFORM in limits + references -> WaterStatus.CONFORM
                else -> WaterStatus.UNKNOWN
            }
        }
}

data class Network(val code: String, val name: String)

/** A sampling event and every parameter analysed in it. */
data class WaterSample(
    val code: String,
    val communeCode: String,
    val sampledAt: Instant,
    val networks: List<Network>,
    val distributor: String?,
    val owner: String?,
    val conclusion: String?,
    val flags: ConformityFlags,
    val results: List<ParameterResult>,
) {
    val status: WaterStatus get() = flags.status
}
