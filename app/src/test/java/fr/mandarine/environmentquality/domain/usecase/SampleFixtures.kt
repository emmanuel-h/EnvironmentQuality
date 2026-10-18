package fr.mandarine.environmentquality.domain.usecase

import fr.mandarine.environmentquality.domain.model.Commune
import fr.mandarine.environmentquality.domain.model.Conformity
import fr.mandarine.environmentquality.domain.model.ConformityFlags
import fr.mandarine.environmentquality.domain.model.Network
import fr.mandarine.environmentquality.domain.model.ParameterResult
import fr.mandarine.environmentquality.domain.model.WaterSample
import java.time.Instant

internal val vernon = Commune("27681", "Vernon", listOf("27200"), "27", "Eure", "Normandie", 25290)

internal fun sample(
    code: String = "02700176757",
    flags: ConformityFlags = ConformityFlags(
        Conformity.NOT_ANALYSED, Conformity.CONFORM, Conformity.NOT_ANALYSED, Conformity.CONFORM,
    ),
    results: List<ParameterResult> = listOf(
        ParameterResult("1340", "Nitrates (en NO3)", 50.0, "50,0", "mg/L", "<=50 mg/L", null),
        ParameterResult("1303", "Conductivité à 25°C", 694.0, "694", "µS/cm", null, ">=200 et <=1100 µS/cm"),
    ),
) = WaterSample(
    code = code,
    communeCode = "27681",
    sampledAt = Instant.parse("2026-07-29T11:30:00Z"),
    networks = listOf(Network("027000325", "VERNON NORMANDIE")),
    distributor = "STGS",
    owner = "CA SEINE NORMANDIE AGGLOMÉRATION",
    conclusion = "Eau d'alimentation conforme.",
    flags = flags,
    results = results,
)
