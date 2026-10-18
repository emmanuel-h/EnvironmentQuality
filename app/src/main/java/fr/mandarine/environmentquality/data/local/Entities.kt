package fr.mandarine.environmentquality.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.model.GeoPoint

@Entity(tableName = "cached_response")
data class CachedResponseEntity(
    @PrimaryKey val url: String,
    val body: String,
    val fetchedAtMillis: Long,
)

/** A commune flattened into columns, shared by the followed and recent tables. */
data class CommuneColumns(
    val code: String,
    val name: String,
    val postalCodes: String,
    val departmentCode: String?,
    val departmentName: String?,
    val regionName: String?,
    val population: Int?,
    val latitude: Double?,
    val longitude: Double?,
) {
    fun toCommune() = Commune(
        code = code,
        name = name,
        postalCodes = postalCodes.split(',').filter { it.isNotBlank() },
        departmentCode = departmentCode,
        departmentName = departmentName,
        regionName = regionName,
        population = population,
        centre = if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null,
    )

    companion object {
        fun of(commune: Commune) = CommuneColumns(
            code = commune.code,
            name = commune.name,
            postalCodes = commune.postalCodes.joinToString(","),
            departmentCode = commune.departmentCode,
            departmentName = commune.departmentName,
            regionName = commune.regionName,
            population = commune.population,
            latitude = commune.centre?.latitude,
            longitude = commune.centre?.longitude,
        )
    }
}

@Entity(tableName = "followed_commune")
data class FollowedCommuneEntity(
    @PrimaryKey val code: String,
    @Embedded(prefix = "commune_") val commune: CommuneColumns,
    val lastSampleCode: String?,
    val lastStatus: String,
    val lastSampledAtMillis: Long?,
    val hasUpdate: Boolean,
    val followedAtMillis: Long,
)

@Entity(tableName = "recent_commune")
data class RecentCommuneEntity(
    @PrimaryKey val code: String,
    @Embedded(prefix = "commune_") val commune: CommuneColumns,
    val viewedAtMillis: Long,
)

/** A single row, id 0. */
@Entity(tableName = "alert_settings")
data class AlertSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val nonConform: Boolean,
    val newSample: Boolean,
    val referenceExceeded: Boolean,
)
