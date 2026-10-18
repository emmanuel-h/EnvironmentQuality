package fr.mandarine.environmentquality.ui.components

import fr.mandarine.environmentquality.domain.model.Commune
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Formatting follows the app language: "25 290" and "29 juillet 2026" in French. */
object Format {
    private val paris: ZoneId = ZoneId.of("Europe/Paris")

    fun integer(value: Int): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)

    fun decimal(value: Double, maxDigits: Int = 2): String =
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = maxDigits
            minimumFractionDigits = 0
        }.format(value)

    fun fixed(value: Double, digits: Int): String =
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = digits
            minimumFractionDigits = digits
        }.format(value)

    fun longDate(date: LocalDate): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.getDefault()).format(date)

    fun longDate(instant: Instant): String = longDate(instant.atZone(paris).toLocalDate())

    fun mediumDate(date: LocalDate): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).format(date)

    fun time(instant: Instant): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()).format(instant.atZone(paris))

    fun month(date: LocalDate, pattern: String = "MMM yyyy"): String =
        DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(date)

    /** "27200 · Eure, Normandie": postal code range, department, region. */
    fun place(commune: Commune): String {
        val postal = when {
            commune.postalCodes.isEmpty() -> commune.code
            commune.postalCodes.size == 1 -> commune.postalCodes.first()
            else -> commune.postalCodes.min() + "–" + commune.postalCodes.max()
        }
        val area = listOfNotNull(commune.departmentName, commune.regionName).joinToString(", ")
        return listOf(postal, area).filter { it.isNotEmpty() }.joinToString(" · ")
    }
}
