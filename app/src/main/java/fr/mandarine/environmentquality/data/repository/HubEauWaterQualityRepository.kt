package fr.mandarine.environmentquality.data.repository

import fr.mandarine.environmentquality.data.remote.HttpClient
import fr.mandarine.environmentquality.data.remote.HubEauPage
import fr.mandarine.environmentquality.data.remote.ResultatDisDto
import fr.mandarine.environmentquality.data.remote.apiJson
import fr.mandarine.environmentquality.data.remote.buildUrl
import fr.mandarine.environmentquality.domain.model.Conformity
import fr.mandarine.environmentquality.domain.model.ConformityFlags
import fr.mandarine.environmentquality.domain.model.MonthlyStatus
import fr.mandarine.environmentquality.domain.model.Network
import fr.mandarine.environmentquality.domain.model.ParameterResult
import fr.mandarine.environmentquality.domain.model.ParameterSeries
import fr.mandarine.environmentquality.domain.model.QualityRange
import fr.mandarine.environmentquality.domain.model.SeriesPoint
import fr.mandarine.environmentquality.domain.model.WaterHistory
import fr.mandarine.environmentquality.domain.model.WaterSample
import fr.mandarine.environmentquality.domain.model.WaterStatus
import fr.mandarine.environmentquality.domain.repository.LatestStatus
import fr.mandarine.environmentquality.domain.repository.WaterQualityRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Hub'Eau "qualité de l'eau potable". Unbounded queries return hundreds of
 * thousands of rows for a city, so every call is sorted, sized or dated.
 */
class HubEauWaterQualityRepository(
    private val http: HttpClient,
    private val baseUrl: String = "https://hubeau.eaufrance.fr/api/v1/qualite_eau_potable",
    private val zone: ZoneId = ZoneId.of("Europe/Paris"),
) : WaterQualityRepository {

    override suspend fun latestStatus(communeCode: String): LatestStatus? {
        val row = latestRow(communeCode) ?: return null
        return LatestStatus(row.sampleCode, parseInstant(row.sampledAt) ?: return null, flagsOf(row))
    }

    override suspend fun latestSample(communeCode: String): WaterSample? {
        val latest = latestRow(communeCode) ?: return null
        val sampledAt = parseInstant(latest.sampledAt) ?: return null
        val day = sampledAt.atOffset(ZoneOffset.UTC).toLocalDate()
        val rows = fetch(
            buildUrl(
                "$baseUrl/resultats_dis",
                "code_commune" to communeCode,
                "date_min_prelevement" to day,
                "date_max_prelevement" to day.plusDays(1),
                "size" to SAMPLE_PAGE,
            ),
        ).filter { it.sampleCode == latest.sampleCode }.ifEmpty { listOf(latest) }
        return toSample(rows, sampledAt)
    }

    override suspend fun history(communeCode: String, since: LocalDate): WaterHistory {
        val rows = fetch(
            buildUrl(
                "$baseUrl/resultats_dis",
                "code_commune" to communeCode,
                "code_parametre" to HISTORY_PARAMETERS.joinToString(","),
                "date_min_prelevement" to since,
                "sort" to "desc",
                "size" to HISTORY_PAGE,
                "fields" to HISTORY_FIELDS,
            ),
        )
        return toHistory(rows, YearMonth.from(since))
    }

    override suspend fun latestPublication(): LocalDate? =
        fetch(buildUrl("$baseUrl/resultats_dis", "size" to 1, "sort" to "desc"))
            .firstOrNull()
            ?.let { parseInstant(it.sampledAt) }
            ?.atZone(zone)
            ?.toLocalDate()

    private suspend fun latestRow(communeCode: String): ResultatDisDto? =
        fetch(buildUrl("$baseUrl/resultats_dis", "code_commune" to communeCode, "size" to 1, "sort" to "desc"))
            .firstOrNull { it.sampleCode.isNotEmpty() }

    private suspend fun fetch(url: String): List<ResultatDisDto> =
        apiJson.decodeFromString<HubEauPage<ResultatDisDto>>(http.get(url)).data

    internal fun toSample(rows: List<ResultatDisDto>, sampledAt: Instant): WaterSample {
        val first = rows.first()
        return WaterSample(
            code = first.sampleCode,
            communeCode = first.communeCode.orEmpty(),
            sampledAt = sampledAt,
            networks = first.reseaux.map { Network(it.code, it.nom) },
            distributor = first.distributor,
            owner = first.owner,
            conclusion = first.conclusion,
            flags = flagsOf(first),
            results = rows.mapNotNull(::toResult).distinctBy { it.code },
        )
    }

    internal fun toHistory(rows: List<ResultatDisDto>, firstMonth: YearMonth): WaterHistory {
        val dated = rows.mapNotNull { row -> parseInstant(row.sampledAt)?.let { row to it.atZone(zone).toLocalDate() } }
        val bySample = dated.groupBy { it.first.sampleCode }
        val statusByMonth = bySample.values
            .map { group -> YearMonth.from(group.first().second) to flagsOf(group.first().first).status }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, statuses) -> statuses.maxBy(::severity) }
        val lastMonth = dated.maxOfOrNull { YearMonth.from(it.second) }?.let { maxOf(it, firstMonth) } ?: firstMonth
        val months = generateSequence(firstMonth) { it.plusMonths(1) }
            .takeWhile { it <= lastMonth }
            .map { MonthlyStatus(it, statusByMonth[it] ?: WaterStatus.UNKNOWN) }
            .toList()
            .takeLast(12)
        val series = SERIES_PARAMETERS.mapNotNull { code ->
            val points = dated.filter { it.first.parameterCode == code && it.first.numericValue != null }
            if (points.isEmpty()) return@mapNotNull null
            val sample = points.first().first
            ParameterSeries(
                code = code,
                name = sample.parameterName.orEmpty(),
                unit = sample.unit,
                limit = QualityRange.parse(sample.limit),
                reference = QualityRange.parse(sample.reference),
                // One bar per month: the month's highest value, the one that matters against a limit.
                points = points.groupBy { YearMonth.from(it.second) }
                    .map { (month, inMonth) -> SeriesPoint(month.atDay(1), inMonth.maxOf { it.first.numericValue!! }) }
                    .sortedBy { it.date },
            )
        }
        return WaterHistory(months, series)
    }

    private fun toResult(row: ResultatDisDto): ParameterResult? {
        val code = row.parameterCode ?: return null
        val display = row.textValue?.takeIf { it.isNotBlank() }
            ?: row.numericValue?.toString()?.replace('.', ',')
            ?: return null
        return ParameterResult(
            code = code,
            name = row.parameterName.orEmpty(),
            numericValue = row.numericValue.takeIf { display.any(Char::isDigit) },
            displayValue = display,
            unit = row.unit,
            limitText = row.limit,
            referenceText = row.reference,
        )
    }

    private fun flagsOf(row: ResultatDisDto) = ConformityFlags(
        bacteriologyLimits = Conformity.fromCode(row.bactLimits),
        physicoChemicalLimits = Conformity.fromCode(row.pcLimits),
        bacteriologyReferences = Conformity.fromCode(row.bactReferences),
        physicoChemicalReferences = Conformity.fromCode(row.pcReferences),
    )

    private fun severity(status: WaterStatus) = when (status) {
        WaterStatus.UNKNOWN -> 0
        WaterStatus.CONFORM -> 1
        WaterStatus.RESERVES -> 2
        WaterStatus.NON_CONFORM -> 3
    }

    private fun parseInstant(text: String?): Instant? = text?.let { runCatching { Instant.parse(it) }.getOrNull() }

    companion object {
        private const val SAMPLE_PAGE = 1000
        private const val HISTORY_PAGE = 5000
        private val SERIES_PARAMETERS = listOf(
            ParameterResult.NITRATES, ParameterResult.CONDUCTIVITY, ParameterResult.TURBIDITY,
        )

        /** E. coli is in nearly every sample, so it carries the bacteriological verdict. */
        private val HISTORY_PARAMETERS = SERIES_PARAMETERS + listOf(ParameterResult.PH, ParameterResult.E_COLI)
        private const val HISTORY_FIELDS = "code_prelevement,date_prelevement,code_parametre,libelle_parametre," +
            "resultat_numerique,libelle_unite,limite_qualite_parametre,reference_qualite_parametre," +
            "conformite_limites_bact_prelevement,conformite_limites_pc_prelevement," +
            "conformite_references_bact_prelevement,conformite_references_pc_prelevement"
    }
}
