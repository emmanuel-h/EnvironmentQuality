# Water Quality Map — France
## Project specifications

---

## Overview

An open-source web application that displays **drinking water quality data across French municipalities**, sourced from official French government APIs, overlaid on an interactive map.

---

## Data sources

### Hub'Eau — Qualité de l'eau potable
- **Provider**: BRGM (Bureau de Recherches Géologiques et Minières) × Direction Générale de la Santé × OFB (Office Français de la Biodiversité)
- **Portal**: https://hubeau.eaufrance.fr/page/api-qualite-eau-potable
- **Source data**: Published by the Ministère des Solidarités et de la Santé on data.gouv.fr
- **License**: Open / free access, no API key required
- **Update frequency**: Monthly (last feed: 16/05/2026)
- **Formats**: JSON, CSV
- **CORS support**: Yes (also JSONP for legacy browsers)

#### Endpoints

| Endpoint | Description |
|---|---|
| `GET /api/v1/qualite_eau_potable/communes_udi` | Link between municipalities and distribution units (UDI/networks) |
| `GET /api/v1/qualite_eau_potable/resultats_dis` | Sampling results, analysis results, and sanitary conclusions per municipality |

#### Base URL
```
https://hubeau.eaufrance.fr/api/v1/qualite_eau_potable/
```

#### Example query
```
GET https://hubeau.eaufrance.fr/api/v1/qualite_eau_potable/resultats_dis?code_commune=27576&size=20
```

#### Key fields returned
- `code_commune`, `nom_commune` — municipality identifier
- `date_prelevement` — sampling date
- `conclusion_conformite_prelevement` — overall sanitary conformity (boolean/label)
- `conformite_limites_bact_prelevement` — bacteriological conformity
- `conformite_limites_pc_prelevement` — physicochemical conformity
- `conformite_references_bact_prelevement` — bacteriological reference conformity
- `conformite_references_pc_prelevement` — physicochemical reference conformity
- `nom_parametre`, `code_parametre` — analysed parameter (e.g. nitrates, turbidity, pH)
- `resultat_numerique`, `unite_mesure` — numeric result and unit

#### Pagination
- Default page size: 5,000 — max: 20,000
- Max accessible depth: 20,000 records
- Attributes: `count`, `next`, `prev`, `first`, `last`, `data`

---

### Related Hub'Eau APIs (potential extensions)

| API | Description | URL |
|---|---|---|
| Hydrométrie | Real-time water levels in rivers | `/api/v2/hydrometrie/` |
| Qualité des cours d'eau | River water quality | `/api/v2/qualite_rivieres/` |
| Qualité des nappes | Groundwater quality | `/api/v1/qualite_nappes/` |
| Piézométrie | Groundwater level | `/api/v1/niveaux_nappes/` |
| Écoulement des cours d'eau | River flow status | `/api/v1/ecoulement/` |

All free, open access, documented at https://hubeau.eaufrance.fr

---

### Additional environmental sources

Only official or governmental sources are used. Each source below was tested on 2026-10-18 against Vernon (INSEE `27681`).

#### Ready now — open JSON, no API key

| Topic | Source (publisher) | Endpoint | What we show |
|---|---|---|---|
| Drought water restrictions | VigiEau (Ministère de la Transition écologique) | `GET https://api.vigieau.gouv.fr/api/zones?commune={insee}` | Alert level per zone (`niveauGravite`: vigilance, alerte, alerte renforcée, crise), zone type (surface water, groundwater, drinking water), restricted uses (`usages`), decree validity dates |
| Natural & technological risks (incl. forest fire where it applies) | Géorisques (Ministère de la Transition écologique, BRGM) | `GET https://georisques.gouv.fr/api/v1/gaspar/risques?code_insee={insee}` | List of known risks for the commune (`risques_detail[].libelle_risque_long`) |
| Past natural disasters | Géorisques | `GET https://georisques.gouv.fr/api/v1/gaspar/catnat?code_insee={insee}` | CatNat decrees: event dates, risk label, publication date |
| Radon | Géorisques | `GET https://georisques.gouv.fr/api/v1/radon?code_insee={insee}` | Radon potential class (1 to 3) |
| Earthquake zone | Géorisques | `GET https://georisques.gouv.fr/api/v1/zonage_sismique?code_insee={insee}` | Seismic zone (1 very low to 5 strong) |
| Classified industrial sites (ICPE, Seveso) | Géorisques | `GET https://georisques.gouv.fr/api/v1/installations_classees?code_insee={insee}` | Site name, regime, Seveso status, activity state, coordinates, inspection reports |
| Live river height and flow | Hub'Eau Hydrométrie (OFB, Eaufrance) | `GET https://hubeau.eaufrance.fr/api/v2/hydrometrie/observations_tr?code_entite={station}` | Height (`H`) and flow (`Q`), updated hourly, for stations from `referentiel/stations` |
| River temperature | Hub'Eau Température | `GET https://hubeau.eaufrance.fr/api/v1/temperature/station` and `/chronique` | Temperature stations near the commune and their time series |

Géorisques responses are paginated (`page`, `page_size`, `total_pages`, `next`). VigiEau returns a bare array.

#### Managed later — official, but needs a free account

| Topic | Source (publisher) | Access | Notes |
|---|---|---|---|
| Forest fire danger (Météo des forêts) | Météo-France | Free API key on https://portail-api.meteofrance.fr (`DonneesPubliquesMeteoForets`) | Daily fire danger level per department. Yearly archives are open as CSV on data.gouv.fr ("Archives de la Météo des forêts") |
| Weather warnings (vigilance) | Météo-France | Same portal, free key | Orange / red warnings per department |
| Air quality (NO2, O3, PM10, PM2.5) | Géod'air (Ineris / LCSQA) | Free account at https://www.geodair.fr/donnees/api, strict quotas | Hourly and daily values per regulatory monitoring station |

#### Managed later — weaker options

| Topic | Source | Why it is weaker |
|---|---|---|
| Air quality index (indice ATMO) | Regional Atmo networks (state-approved) | One API per region, no single national endpoint |
| Pollen | Atmo networks, Ministère de la Santé (RNSA archives) | Regional or historical only; no national live API found |
| Past forest fires (BDIFF) | Ministère de l'Agriculture | Website exports only; could not be tested automatically |
| Flood warnings (Vigicrues) | Ministère de la Transition écologique | Public services exist but could not be tested automatically |
| Bathing water quality | Ministère de la Santé / ARS | Mostly regional datasets on data.gouv.fr; Hub'Eau "surveillance littoral" call failed during testing |

---

## Map integration

### Recommended library: OpenLayers or Leaflet.js
- **OpenLayers**: more powerful, better WMS/WFS support, suited for complex geo layers
- **Leaflet**: lighter, simpler DX, large plugin ecosystem — good for MVP

### Base tile layer
- OpenStreetMap (free, no API key): `https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png`
- IGN Géoportail (French national mapping, high quality): requires free API key at https://geoservices.ign.fr

### Municipality boundaries (GeoJSON)
- Source: data.gouv.fr — Contours des communes de France
- URL: https://www.data.gouv.fr/fr/datasets/contours-des-communes-de-france-simplifies/
- Use simplified version (< 5 MB for all of France) for performance

### Geo-coding municipalities
- The Hub'Eau API accepts `code_commune` (INSEE code, 5 digits)
- Geo-reference via the **API Découpage Administratif** (also free):
  ```
  GET https://geo.api.gouv.fr/communes?code=27576&fields=nom,code,contour,centre
  ```

---

## Application features

### MVP (v1)
- [ ] Interactive map of France (OpenLayers or Leaflet)
- [ ] Search by municipality name or postal code
- [ ] Display latest sanitary conformity status per municipality (colour-coded: green / orange / red)
- [ ] Click on a municipality → side panel with latest analysis results
- [ ] Filter by parameter (nitrates, bacteriology, pH, turbidity…)
- [ ] Mobile-responsive layout

### v2 extensions
- [ ] Time slider — visualise conformity evolution over months/years
- [ ] Groundwater quality layer (Qualité des nappes API)
- [ ] River water quality layer (Qualité des cours d'eau API)
- [ ] Export results as CSV for a given area
- [ ] Alert system — subscribe to updates for a commune
- [ ] Comparison view between municipalities

### Environment extensions (ready-now sources)
- [ ] Drought restrictions card on the commune page, with the current alert level (VigiEau)
- [ ] Risks card on the commune page: known risks, past natural disasters, radon, earthquake zone (Géorisques)
- [ ] Classified industrial sites near the commune (Géorisques ICPE)
- [ ] Live river height and flow for nearby stations (Hub'Eau Hydrométrie)
- [ ] River temperature for nearby stations (Hub'Eau Température)

### Later
- [ ] Forest fire danger (Météo des forêts) — needs a free Météo-France key
- [ ] Weather warnings — needs a free Météo-France key
- [ ] Air quality — needs a free Géod'air account
- [ ] Weaker options listed under "Managed later — weaker options"

---

## Tech stack (suggested)

| Layer | Choice | Rationale |
|---|---|---|
| Frontend framework | React + Vite | Fast DX, large ecosystem |
| Map library | Leaflet.js + react-leaflet | Lightweight, easy integration |
| Styling | Tailwind CSS | Utility-first, responsive |
| Data fetching | TanStack Query | Caching, pagination, async state |
| GeoJSON processing | Turf.js | Spatial operations if needed |
| Charts (detail panel) | Recharts or Chart.js | Trend visualisations |
| Hosting | Vercel / Netlify | Free tier, CI/CD from GitHub |

---

## API rate limits & caching strategy

- Hub'Eau has no explicit rate limit documented but recommends avoiding large unbounded queries
- Cache commune-level results in `localStorage` or a service worker with a 24h TTL
- Fetch municipality boundary GeoJSON once at startup and store in memory
- On search: first query `communes_udi` to get the UDI code, then `resultats_dis` with `size=5000` and filter client-side

---

## Data freshness

- Hub'Eau data is updated **monthly**
- Display the `date_prelevement` (sampling date) prominently — data may be 1–3 months old
- Show a "last updated" badge on each municipality panel

---

## Accessibility & internationalisation

- Language: French primary, English toggle optional
- WCAG 2.1 AA compliance target
- Colour-blind safe palette for conformity status (avoid red/green only — add icons and labels)

---

## References

- Hub'Eau portal: https://hubeau.eaufrance.fr
- data.gouv.fr dataset: https://www.data.gouv.fr/fr/datasets/resultats-du-controle-sanitaire-de-leau-distribuee-commune-par-commune/
- API Découpage Administratif: https://geo.api.gouv.fr
- IGN Géoportail: https://geoservices.ign.fr
- OpenStreetMap: https://www.openstreetmap.org
- Ministry of Health water quality portal: http://eaupotable.sante.gouv.fr
