package fr.mandarine.environmentquality.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GeoCommuneDto(
    val nom: String = "",
    val code: String = "",
    val codesPostaux: List<String> = emptyList(),
    val centre: GeoPointDto? = null,
    val population: Int? = null,
    val departement: GeoNamedDto? = null,
    val region: GeoNamedDto? = null,
)

/** GeoJSON point: coordinates are longitude first. */
@Serializable
internal data class GeoPointDto(val coordinates: List<Double> = emptyList())

@Serializable
internal data class GeoNamedDto(val code: String? = null, val nom: String? = null)

@Serializable
internal data class VigieauZoneDto(
    val nom: String? = null,
    val type: String? = null,
    val niveauGravite: String? = null,
    val arrete: VigieauDecreeDto? = null,
    val usages: List<VigieauUsageDto> = emptyList(),
)

@Serializable
internal data class VigieauDecreeDto(
    val cheminFichier: String? = null,
    val dateDebutValidite: String? = null,
    val dateFinValidite: String? = null,
)

@Serializable
internal data class VigieauUsageDto(
    val nom: String = "",
    val thematique: String? = null,
    val description: String? = null,
    val concerneParticulier: Boolean = false,
    val concerneCollectivite: Boolean = false,
    val concerneExploitation: Boolean = false,
    val concerneEntreprise: Boolean = false,
)

/** Géorisques wraps rows in its own pagination envelope. */
@Serializable
internal data class GeorisquesPage<T>(
    val results: Int = 0,
    val page: Int = 1,
    @SerialName("total_pages") val totalPages: Int = 1,
    val data: List<T> = emptyList(),
)

@Serializable
internal data class RisquesCommuneDto(
    @SerialName("risques_detail") val details: List<RisqueDetailDto> = emptyList(),
)

@Serializable
internal data class RisqueDetailDto(
    @SerialName("num_risque") val code: String = "",
    @SerialName("libelle_risque_long") val label: String = "",
)

@Serializable
internal data class CatnatDto(
    @SerialName("libelle_risque_jo") val label: String = "",
    @SerialName("date_debut_evt") val start: String? = null,
    @SerialName("date_fin_evt") val end: String? = null,
    @SerialName("date_publication_jo") val published: String? = null,
)

@Serializable
internal data class RadonDto(@SerialName("classe_potentiel") val potentialClass: String? = null)

@Serializable
internal data class SeismicDto(@SerialName("code_zone") val zone: String? = null)

@Serializable
internal data class InstallationDto(
    val raisonSociale: String = "",
    val regime: String? = null,
    val statutSeveso: String? = null,
    val etatActivite: String? = null,
    val longitude: Double? = null,
    val latitude: Double? = null,
    val inspections: List<InspectionDto> = emptyList(),
)

@Serializable
internal data class InspectionDto(
    val dateInspection: String? = null,
    val fichierInspection: InspectionFileDto? = null,
)

@Serializable
internal data class InspectionFileDto(val urlFichier: String? = null)

/** Géorisques answers an empty body, not JSON, where no clay exposure is mapped. */
@Serializable
internal data class RgaDto(val codeExposition: String? = null)

/** The GASPAR plan endpoints page like Spring Data, not like the rest of Géorisques. */
@Serializable
internal data class GasparPlanPage(val content: List<RiskPlanDto> = emptyList())

@Serializable
internal data class RiskPlanDto(
    val libPpr: String = "",
    val modeleProcedure: String? = null,
    val dateModification: String? = null,
)

@Serializable
internal data class TriDto(
    @SerialName("libelle_tri") val name: String = "",
    @SerialName("date_arrete_carte") val mapsApproved: String? = null,
)

@Serializable
internal data class AziDto(
    @SerialName("libelle_azi") val name: String = "",
    @SerialName("date_diffusion") val published: String? = null,
)

@Serializable
internal data class SoilSectorDto(
    val nom: String = "",
    val adresse: String? = null,
    @SerialName("fiche_risque") val sheet: String? = null,
)

@Serializable
internal data class SoilActionDto(val statut: String? = null)
