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
    @SerialName("noms_masse_eau_edl") val waterBody: String? = null,
    val altitude: Double? = null,
    val longitude: Double? = null,
    val latitude: Double? = null,
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
