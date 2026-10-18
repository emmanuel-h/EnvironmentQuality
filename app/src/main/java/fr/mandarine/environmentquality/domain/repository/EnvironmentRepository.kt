package fr.mandarine.environmentquality.domain.repository

import fr.mandarine.environmentquality.domain.model.CommuneRisks
import fr.mandarine.environmentquality.domain.model.DroughtStatus
import fr.mandarine.environmentquality.domain.model.FlowStation
import fr.mandarine.environmentquality.domain.model.GroundwaterStation
import fr.mandarine.environmentquality.domain.model.HydroReading
import fr.mandarine.environmentquality.domain.model.HydroStation
import fr.mandarine.environmentquality.domain.model.TemperatureReading
import fr.mandarine.environmentquality.domain.model.TemperatureStation

interface DroughtRepository {
    suspend fun status(communeCode: String): DroughtStatus
}

interface RiskRepository {
    suspend fun risks(communeCode: String): CommuneRisks
}

interface StationRepository {
    suspend fun hydroStations(departmentCode: String): List<HydroStation>
    suspend fun hydroReading(stationCode: String): HydroReading?
    suspend fun temperatureStations(departmentCode: String): List<TemperatureStation>
    /** The station's last readings, newest first. */
    suspend fun recentTemperatures(stationCode: String): List<TemperatureReading>
    suspend fun groundwaterStations(communeCode: String, departmentCode: String): List<GroundwaterStation>
    suspend fun flowStations(departmentCode: String): List<FlowStation>
}
