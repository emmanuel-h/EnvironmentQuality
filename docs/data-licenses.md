# Can the app be published with these data sources? (checked 2026-10-18)

Short answer: yes. Every source the app uses today is published under the French
**Licence Ouverte / Open Licence (Etalab) 2.0**, which allows free reuse, including
commercial reuse (paid app, ads) and redistribution, with no fee and no sign-up.

## What the licence asks of the app

1. Name the source (the producer of the data).
2. Show the date of the data's last update.
3. Don't alter the data or twist its meaning.
4. Don't suggest the State or the producer endorses the app.
5. Logos are excluded from the licence: don't use Marianne, Hub'Eau, Géorisques or
   VigiEau logos (in the app or on the Play Store listing) without permission.

## Per source

| Source | Licence | Commercial use | Rate limit | Official page |
|---|---|---|---|---|
| Hub'Eau (drinking water, rivers, groundwater, flow, temperature) | Licence Ouverte Etalab | Yes: "librement et gratuitement utilisables et réutilisables, y compris dans un but commercial" | No figure. Must use filters, may be cut "en cas d'usage abusif". Drinking-water API: max 20,000 records deep per query | [CGU](https://hubeau.eaufrance.fr/page/conditions-generales), [drinking water API](https://hubeau.eaufrance.fr/page/api-qualite-eau-potable) |
| geo.api.gouv.fr (Découpage administratif) | Etalab (DINUM service; no licence text on its own pages) | Yes (Licence Ouverte) | No figure found; access can be refused for abuse | [Mentions légales](https://geo.api.gouv.fr/cgu), [FAQ](https://geo.api.gouv.fr/faq) |
| VigiEau (drought restrictions) | Licence Ouverte 2.0, producer Ministère de la Transition écologique | Yes | None published; API described as public and "stabilisée" | [data.gouv.fr dataset](https://www.data.gouv.fr/datasets/donnee-secheresse-vigieau), [API repo](https://github.com/MTES-MCT/vigieau-api) |
| Géorisques | Etalab 2.0 ("Sauf mention contraire, tous les contenus de ce site sont sous licence etalab-2.0") | Yes | None found on the pages read | [Mentions légales](https://www.georisques.gouv.fr/mentions-legales) |
| Météo-France (Météo des forêts, vigilance) — not in the app yet | Free reuse of all public data since 1 Jan 2024; older Météo-France page names the Licence Ouverte | Yes per the government announcement | Needs a free account and API key on portail-api.meteofrance.fr. Its CGU and quotas could not be read (the portal blocks automated reading): **check them yourself before building** | [info.gouv.fr announcement](https://www.info.gouv.fr/actualite/meteo-france-la-reutilisation-des-donnees-publiques-devient-gratuite), [Météo-France conditions](https://donneespubliques.meteofrance.fr/?fond=dossier&id_dossier=1) |
| Géod'air (air quality) — not in the app yet | Licence Ouverte; must cite sources and last update date; must not imply Ineris endorses the reuse | Not excluded for the data (only for links and logos) | Account required; "limiter l'usage de l'API à une interrogation par date, heure, polluant et type de statistique"; excessive use → account suspended | [Géod'air CGU (home page)](https://www.geodair.fr), [API page](https://www.geodair.fr/donnees/api) |

Licence text: [Licence Ouverte 2.0](https://www.etalab.gouv.fr/licence-ouverte-open-licence), listed on [data.gouv.fr licences](https://www.data.gouv.fr/pages/legal/licences).

## Map tiles

None. The map is drawn from a built-in outline (`app/src/main/java/fr/mandarine/environmentquality/ui/components/FranceOutline.kt`), so there is
no OpenStreetMap, IGN or other tile licence to honour. If tiles are added later, they bring
their own attribution (for OSM, "© OpenStreetMap contributors") and usage policy; the public
tile.openstreetmap.org servers are not meant for heavy app traffic. This last point is from
general knowledge, not checked today.

## Does the Sources screen already cover it?

Mostly.

- Covered: all five sources are listed with their producer and a link (`SourcesScreen.kt`),
  and the drought, risks and nearby screens each carry a "Source : …" line.
- Missing: the licence name ("Licence Ouverte 2.0 / Etalab") is not shown anywhere.
- Missing: the last-update date is shown for drinking-water results only ("Résultats
  jusqu'au …"); drought, risks and river readings don't show when the data was last updated.
- To change later: `sources_disclaimer` says "sans clé d'API", which stops being true once
  Météo-France or Géod'air is added.

## Before adding the key-based sources

Météo-France and Géod'air keys/accounts are per developer. Shipping one key inside a
public app means every user shares its quota, and Géod'air explicitly suspends accounts for
excessive use. A small server that fetches once an hour and caches (or the hourly files
Géod'air also publishes on data.gouv.fr) avoids that. This is a recommendation, not a stated rule.
