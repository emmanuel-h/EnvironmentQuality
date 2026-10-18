package fr.mandarine.environmentquality.domain.model

/** Families used to filter the parameter list, as in the mockup. */
enum class ParameterGroup { NITRATES, PHYSICAL, CHLORINE, SENSORY, BACTERIOLOGY, OTHER }

/**
 * A regulatory bound parsed from Hub'Eau's free text, such as `<=50 mg/L` or
 * `>=6,5 et <=9 unité pH`.
 */
data class QualityRange(val min: Double?, val max: Double?) {

    fun contains(value: Double): Boolean =
        (min == null || value >= min) && (max == null || value <= max)

    companion object {
        private val bound = Regex("""([<>]=?)\s*(-?\d+(?:[.,]\d+)?)""")

        fun parse(text: String?): QualityRange? {
            if (text.isNullOrBlank()) return null
            var min: Double? = null
            var max: Double? = null
            bound.findAll(text).forEach { match ->
                val value = match.groupValues[2].replace(',', '.').toDouble()
                if (match.groupValues[1].startsWith("<")) max = value else min = value
            }
            return if (min == null && max == null) null else QualityRange(min, max)
        }
    }
}

data class ParameterResult(
    val code: String,
    val name: String,
    val numericValue: Double?,
    /** The value as published, with its decimal comma and any `<` sign. */
    val displayValue: String,
    val unit: String?,
    val limitText: String?,
    val referenceText: String?,
) {
    val limit: QualityRange? get() = QualityRange.parse(limitText)
    val reference: QualityRange? get() = QualityRange.parse(referenceText)

    /** A `<0,30` result means "below detection", never a breach. */
    private val isBelowDetection: Boolean get() = displayValue.trimStart().startsWith("<")

    val exceedsLimit: Boolean
        get() = !isBelowDetection && numericValue != null && limit?.contains(numericValue) == false

    val exceedsReference: Boolean
        get() = !isBelowDetection && numericValue != null && reference?.contains(numericValue) == false

    /** Within the limit but at 90 % of it or more, like Vernon's 50 mg/L of nitrates. */
    val nearLimit: Boolean
        get() {
            val max = limit?.max ?: return false
            val value = numericValue ?: return false
            return !exceedsLimit && !isBelowDetection && max > 0 && value >= max * 0.9
        }

    /** Where the value sits against its upper bound, from 0 to 1, for the gauges. */
    val fillRatio: Float?
        get() {
            val max = (limit ?: reference)?.max ?: return null
            val value = numericValue ?: return null
            if (max <= 0) return null
            return (value / max).toFloat().coerceIn(0f, 1f)
        }

    val unitLabel: String?
        get() = unit?.takeUnless { it.equals("SANS OBJET", ignoreCase = true) || it.isBlank() }

    val group: ParameterGroup get() = groupOf(code, name)

    companion object {
        const val NITRATES = "1340"
        const val TURBIDITY = "1295"
        const val PH = "1302"
        const val CONDUCTIVITY = "1303"
        const val TEMPERATURE = "1301"
        const val FREE_CHLORINE = "1398"
        const val TOTAL_CHLORINE = "1399"
        const val E_COLI = "1449"

        /** The four parameters the commune page highlights, in display order. */
        val KEY_PARAMETERS = listOf(NITRATES, TURBIDITY, PH, CONDUCTIVITY)

        fun groupOf(code: String, name: String): ParameterGroup {
            val lower = name.lowercase()
            return when {
                code == NITRATES || code == "1339" || lower.startsWith("nitr") -> ParameterGroup.NITRATES
                code == FREE_CHLORINE || code == TOTAL_CHLORINE || "chlore" in lower -> ParameterGroup.CHLORINE
                code in setOf(TURBIDITY, PH, CONDUCTIVITY, TEMPERATURE) || "turbidit" in lower ||
                    lower.startsWith("ph") || "conductivit" in lower || "température" in lower -> ParameterGroup.PHYSICAL
                "aspect" in lower || "couleur" in lower || "saveur" in lower || "odeur" in lower -> ParameterGroup.SENSORY
                code == E_COLI || "coli" in lower || "entérocoque" in lower || "bact" in lower ||
                    "germes" in lower || "spores" in lower -> ParameterGroup.BACTERIOLOGY
                else -> ParameterGroup.OTHER
            }
        }
    }
}
