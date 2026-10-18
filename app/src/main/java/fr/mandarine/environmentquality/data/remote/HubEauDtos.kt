package fr.mandarine.environmentquality.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Every Hub'Eau endpoint wraps its rows the same way. */
@Serializable
internal data class HubEauPage<T>(
    val count: Int = 0,
    val next: String? = null,
    val data: List<T> = emptyList(),
)

@Serializable
internal data class ResultatDisDto(
    @SerialName("code_prelevement") val sampleCode: String = "",
    @SerialName("code_commune") val communeCode: String? = null,
    @SerialName("code_parametre") val parameterCode: String? = null,
    @SerialName("libelle_parametre") val parameterName: String? = null,
    @SerialName("resultat_alphanumerique") val textValue: String? = null,
    @SerialName("resultat_numerique") val numericValue: Double? = null,
    @SerialName("libelle_unite") val unit: String? = null,
    @SerialName("limite_qualite_parametre") val limit: String? = null,
    @SerialName("reference_qualite_parametre") val reference: String? = null,
    @SerialName("nom_distributeur") val distributor: String? = null,
    @SerialName("nom_moa") val owner: String? = null,
    @SerialName("date_prelevement") val sampledAt: String? = null,
    @SerialName("conclusion_conformite_prelevement") val conclusion: String? = null,
    @SerialName("conformite_limites_bact_prelevement") val bactLimits: String? = null,
    @SerialName("conformite_limites_pc_prelevement") val pcLimits: String? = null,
    @SerialName("conformite_references_bact_prelevement") val bactReferences: String? = null,
    @SerialName("conformite_references_pc_prelevement") val pcReferences: String? = null,
    val reseaux: List<ReseauDto> = emptyList(),
)

@Serializable
internal data class ReseauDto(val code: String = "", val nom: String = "")

@Serializable
internal data class HydroStationDto(
    @SerialName("code_station") val code: String = "",
    @SerialName("libelle_station") val name: String? = null,
    @SerialName("libelle_site") val siteName: String? = null,
    @SerialName("libelle_cours_eau") val river: String? = null,
    @SerialName("longitude_station") val longitude: Double? = null,
    @SerialName("latitude_station") val latitude: Double? = null,
    @SerialName("en_service") val inService: Boolean? = null,
    @SerialName("type_station") val type: String? = null,
    @SerialName("commentaire_influence_locale_station") val influence: String? = null,
)

@Serializable
internal data class HydroObservationDto(
    @SerialName("code_station") val stationCode: String? = null,
    @SerialName("grandeur_hydro") val quantity: String? = null,
    @SerialName("date_obs") val observedAt: String? = null,
    @SerialName("resultat_obs") val value: Double? = null,
)

@Serializable
internal data class TemperatureStationDto(
    @SerialName("code_station") val code: String = "",
    @SerialName("libelle_station") val name: String? = null,
    @SerialName("libelle_cours_eau") val river: String? = null,
    val longitude: Double? = null,
    val latitude: Double? = null,
)

@Serializable
internal data class TemperatureDto(
    @SerialName("date_mesure_temp") val date: String? = null,
    @SerialName("heure_mesure_temp") val time: String? = null,
    val resultat: Double? = null,
)

@Serializable
internal data class GroundwaterStationDto(
    @SerialName("bss_id") val bssId: String = "",
    @SerialName("code_bss") val code: String? = null,
    @SerialName("nom_commune") val commune: String? = null,
    @SerialName("code_insee") val communeCode: String? = null,
    @SerialName("nom_nature_pe") val nature: String? = null,
    /** A list: a point can tap several stacked aquifers. */
    @SerialName("noms_masse_eau_edl") val waterBodies: List<String> = emptyList(),
    val altitude: Double? = null,
    val longitude: Double? = null,
    val latitude: Double? = null,
    @SerialName("date_fin_mesure") val lastSampledOn: String? = null,
)

@Serializable
internal data class GroundwaterAnalysisDto(
    @SerialName("date_debut_prelevement") val sampledAt: String? = null,
    @SerialName("code_param") val code: Int? = null,
    @SerialName("nom_param") val name: String? = null,
    val resultat: Double? = null,
    @SerialName("symbole_unite") val unit: String? = null,
    /** Sandre remark code: 1 is a quantified result, others are below a detection or quantification limit. */
    @SerialName("code_remarque_analyse") val remark: Int? = null,
)

@Serializable
internal data class FlowStationDto(
    @SerialName("code_station") val code: String = "",
    @SerialName("libelle_station") val name: String? = null,
    @SerialName("libelle_cours_eau") val river: String? = null,
    @SerialName("libelle_commune") val commune: String? = null,
    @SerialName("etat_station") val state: String? = null,
    val longitude: Double? = null,
    val latitude: Double? = null,
)

@Serializable
internal data class FlowObservationDto(
    @SerialName("code_station") val stationCode: String = "",
    @SerialName("date_observation") val observedOn: String? = null,
    @SerialName("code_ecoulement") val code: String? = null,
)

@Serializable
internal data class PiezometerStationDto(
    @SerialName("code_bss") val code: String = "",
    @SerialName("nom_commune") val commune: String? = null,
    @SerialName("libelle_pe") val label: String? = null,
    @SerialName("noms_masse_eau_edl") val waterBodies: List<String> = emptyList(),
    /** Longitude and latitude, despite the names. */
    val x: Double? = null,
    val y: Double? = null,
    @SerialName("date_fin_mesure") val lastMeasuredOn: String? = null,
)

@Serializable
internal data class PiezometerReadingDto(
    @SerialName("date_mesure") val date: String? = null,
    @SerialName("niveau_nappe_eau") val level: Double? = null,
    @SerialName("profondeur_nappe") val depth: Double? = null,
)

@Serializable
internal data class RiverAnalysisDto(
    @SerialName("code_station") val stationCode: String = "",
    @SerialName("libelle_station") val stationName: String? = null,
    val longitude: Double? = null,
    val latitude: Double? = null,
    @SerialName("date_prelevement") val sampledOn: String? = null,
    @SerialName("code_parametre") val code: String? = null,
    @SerialName("libelle_parametre") val name: String? = null,
    val resultat: Double? = null,
    @SerialName("symbole_unite") val unit: String? = null,
)
