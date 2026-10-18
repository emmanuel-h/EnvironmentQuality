package fr.mandarine.environmentquality.domain.repository

import fr.mandarine.environmentquality.domain.model.ConformityFlags
import fr.mandarine.environmentquality.domain.model.WaterHistory
import fr.mandarine.environmentquality.domain.model.WaterSample
import java.time.Instant
import java.time.LocalDate

/** The conformity of a commune's latest sample, without its parameters. */
data class LatestStatus(val sampleCode: String, val sampledAt: Instant, val flags: ConformityFlags)

interface WaterQualityRepository {
    suspend fun latestStatus(communeCode: String): LatestStatus?
    suspend fun latestSample(communeCode: String): WaterSample?
    suspend fun history(communeCode: String, since: LocalDate): WaterHistory
}
