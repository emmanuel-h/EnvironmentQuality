package fr.mandarine.environmentquality.domain.usecase

import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.model.WaterSample
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** One row per parameter, semicolon separated so a French spreadsheet opens it as is. */
class BuildSampleCsvUseCase(private val zone: ZoneId = ZoneId.of("Europe/Paris")) {

    operator fun invoke(commune: Commune, sample: WaterSample): String {
        val date = DateTimeFormatter.ISO_LOCAL_DATE.format(sample.sampledAt.atZone(zone))
        val network = sample.networks.joinToString(" / ") { it.name }
        val header = listOf(
            "code_commune", "commune", "date_prelevement", "code_prelevement", "reseau",
            "code_parametre", "parametre", "resultat", "unite", "limite_qualite", "reference_qualite",
        )
        val rows = sample.results.map { result ->
            listOf(
                commune.code, commune.name, date, sample.code, network,
                result.code, result.name, result.displayValue, result.unitLabel.orEmpty(),
                result.limitText.orEmpty(), result.referenceText.orEmpty(),
            )
        }
        return (listOf(header) + rows).joinToString("\r\n", postfix = "\r\n") { row ->
            row.joinToString(";") { escape(it) }
        }
    }

    private fun escape(field: String): String =
        if (field.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }

    fun fileName(commune: Commune, sample: WaterSample): String {
        val date = DateTimeFormatter.BASIC_ISO_DATE.format(sample.sampledAt.atZone(zone).toLocalDate())
        return "eau-${commune.code}-$date.csv"
    }
}
