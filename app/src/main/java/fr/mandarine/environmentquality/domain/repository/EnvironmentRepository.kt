package fr.mandarine.environmentquality.domain.repository

import fr.mandarine.environmentquality.domain.model.CommuneRisks
import fr.mandarine.environmentquality.domain.model.DroughtStatus
import fr.mandarine.environmentquality.domain.model.FlowObservation
import fr.mandarine.environmentquality.domain.model.FlowStation
import fr.mandarine.environmentquality.domain.model.GeoPoint
import fr.mandarine.environmentquality.domain.model.GroundwaterLevel
import fr.mandarine.environmentquality.domain.model.GroundwaterSampling
import fr.mandarine.environmentquality.domain.model.GroundwaterStation
import fr.mandarine.environmentquality.domain.model.HydroReading
import fr.mandarine.environmentquality.domain.model.HydroStation
import fr.mandarine.environmentquality.domain.model.PiezometerStation
import fr.mandarine.environmentquality.domain.model.RiskDetails
import fr.mandarine.environmentquality.domain.model.RiverQuality
import fr.mandarine.environmentquality.domain.model.TemperatureReading
import fr.mandarine.environmentquality.domain.model.TemperatureStation
import java.time.LocalDate

interface DroughtRepository {
    suspend fun status(communeCode: String): DroughtStatus
}

interface RiskRepository {
    suspend fun risks(communeCode: String): CommuneRisks

    /** Clay exposure needs a point; without [centre] it is left unknown. */
    suspend fun riskDetails(communeCode: String, centre: GeoPoint?): RiskDetails
}

interface StationRepository {
    suspend fun hydroStations(departmentCode: String): List<HydroStation>
    suspend fun hydroReading(stationCode: String): HydroReading?
    suspend fun temperatureStations(departmentCode: String): List<TemperatureStation>
    /** The station's last readings, newest first. */
    suspend fun recentTemperatures(stationCode: String): List<TemperatureReading>
    suspend fun groundwaterStations(communeCode: String, departmentCode: String): List<GroundwaterStation>
    /** The station's results on [sampledOn], usually its last sampling; null when there are none. */
    suspend fun groundwaterSampling(stationCode: String, sampledOn: LocalDate): GroundwaterSampling?
    suspend fun flowStations(departmentCode: String): List<FlowStation>
    /** The latest observation of each station that has one, by station code. */
    suspend fun latestFlows(stationCodes: List<String>): Map<String, FlowObservation>
    suspend fun piezometers(departmentCode: String): List<PiezometerStation>
    /** About the last six weeks of daily levels, newest first. */
    suspend fun recentLevels(stationCode: String): List<GroundwaterLevel>
    /** The nearest station around [centre] sampled in the last year, with each parameter's latest result. */
    suspend fun riverQuality(centre: GeoPoint): RiverQuality?
}
