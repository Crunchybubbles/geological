# Phase 3 mineral-system increment

Status: fourteenth Phase 3 architecture-proving slice; all six architecture-proving families, the gated porphyry supergene refinement, source-audit plus held-out evidence contracts, and a bounded non-concentric porphyry footprint are explicit

## Alpha.1 — linked porphyry topology

`PorphyrySystemState` turns the existing fertile or barren porphyry decision into a bounded,
queryable system state. It links the youngest multi-pulse felsic intrusion, the magmatic-
hydrothermal fluid system, and the inherited fault stockwork. A formed system publishes three
ordered alteration zones—potassic core, phyllic intermediate, and propylitic distal—with bounded
intensity proxies and a local `zoneAt` query. A barren system retains the intrusion context but
publishes a disconnected stockwork, a low-volatile source class, no formed zones, and the failed
hard-gate name. Source and deposit fixed-unit budgets are copied from the existing decision ledger;
the state cannot invent metal mass.

The state is deliberately an explanatory envelope rather than a block-placement algorithm. Its
245-block lateral envelope, 160-block vertical extent, 28-degree alteration azimuth, and bounded
zone offsets (0, 18, and 36 blocks) are proof tunables. The shifted annuli make the footprint
deterministically non-concentric while retaining ordered zone precedence; voxel-scale veins and
empirical grade-tonnage sampling remain future slices. The state is exposed through both geology
and material query facades and in the deterministic Phase 2 review packet until a dedicated Phase
3 packet exists.

The focused mineral-system tests verify formed and barren outcomes, source/deposit budget bounds,
intrusion/fluid/stockwork linkage, zone ordering and point classification, failed-gate semantics,
and deterministic review-artifact exposure.

## Alpha.2 — synvolcanic VMS topology

`VmsSystemState` records the basin, coeval heat source, inherited feeder path, and seafloor age for
the primary VMS candidate. A formed state exposes a stratiform massive-sulfide lens and a deeper
chloritic feeder through deterministic local-point classification, with the exact source/deposit
fixed-unit budgets copied from the decision ledger. A barren state retains the basin and candidate
context but reports no coeval hydrothermal fluid or lens and names the failed driver gate. The
bounded ellipsoid/feeder dimensions mirror the existing Phase 1 proof geometry; they are not an
empirical grade or vent-temperature distribution.

Tests cover the lens/feeder split, synvolcanic age, basin and pathway linkage, source-budget
closure, barren-driver rejection, and review JSON exposure.

## Alpha.3 — LCT pegmatite child body

`LctPegmatiteState` derives a stable child-body ID from the province identity and the final evolved
pluton pulse. It requires the evolved residual-fluid potential and a porphyry-capable lineage before
publishing an apical fracture/dike-swarm body with wall, intermediate, and quartz-core zones. The
child allocation is explicitly bounded by a fixed-unit residual source budget. Barren or unresolved
lineages retain a candidate child ID but publish no zones, no allocation, and the failed lineage or
residual-budget gate. Dike dimensions, zone fractions, and proof budgets are tunable proxies rather
than empirical grade-tonnage data.

Tests cover parent/child identity, differentiation progress, fertility and emplacement gates,
internal-zone point classification, budget bounds, barren-lineage rejection, and artifact exposure.

## Alpha.4 — BIF sheet

`BifSystemState` adds a basin-bound chemical-sedimentary sheet proof. The current branch models the
Algoma-style volcano-sedimentary path: a formed candidate carries a stable sheet ID, basin and
iron-source IDs, an ancient formation age, reducing ocean-redox class, and a bounded banded
stratiform footprint with an exact source/sheet allocation. Provinces without the required
volcano-sedimentary context remain barren and publish no sheet or ancient-ocean chemistry. The
state does not select Superior versus Algoma universally, upgrade iron by supergene leaching, or
claim an economic grade.

Tests cover age/redox/sheet evidence, basin and source linkage, footprint bounds, budget bounds,
barren gating, and deterministic review JSON exposure.

## Alpha.5 — restricted-basin evaporite and potash sequence

`EvaporitePotashState` adds a basin-bound chemical-sedimentary proof to the existing sulfate,
halite, and potash material vocabulary. A formed state requires marine basin accommodation,
limited outflow, replenished seawater solute, and repeated concentration/reflooding episodes. It
publishes deterministic sulfate-margin, basin-center halite, and late potash bodies with explicit
formation ages, vertical/radial stage geometry, and a shared fixed-unit source ledger. Provinces
without the marine/restricted-basin branch remain barren and expose the failed restriction or
solute-budget gate; present-day biome or aridity is not used as a shortcut.

Tests cover the restricted-basin gates, younger-upward brine succession, basin-bound point
classification, source-budget closure, barren behavior, and deterministic review JSON exposure.

## Alpha.6 — source-linked placer transport and sorting

`PlacerSystemState` turns the existing alluvial placer decision and surface budget into an explicit
source-to-sink state. A formed state links the exposed porphyry source, weathering event, connected
water catchment, hydraulic gradient break, and dense-mineral sorting class. Release, transport loss,
and trap allocation close exactly against the primary Au source budget. Rejected candidates retain
their hydraulic location but publish no source release or deposit allocation.

Tests cover source and weathering identity, exposure and transport gates, trap classification,
release/loss/deposit closure, barren candidate behavior, and deterministic review JSON exposure.

## Alpha.7 — porphyry fluid phases and metal distributions

`PorphyryFluidMetalState` refines the intrusion-centered proof with three bounded fluid pulses:
concentrated magmatic brine, vapor-rich separated fluid, and a distal meteoric mixture. It also
publishes normalized Cu-Au-S-Zn vectors for the potassic, phyllic, and propylitic zones. The zone
allocations sum exactly to the existing porphyry deposit ledger, while fluid and metal values remain
comparative proxies rather than measured grades or equilibrium calculations. Barren porphyry
lineages publish neither fluid pulses nor metal distributions and retain the failed source gate.

Tests cover phase ordering, salinity/temperature/phase behavior, normalized metal closure, zoned
point queries, source-budget bounds, barren behavior, and deterministic review JSON exposure.

## Alpha.8 — gated porphyry supergene copper profile

`SupergeneCopperState` derives a near-surface oxidation/leaching profile only from an exposed,
formed primary porphyry. The formed state publishes a stable weathering process and paleo-water-
table identity, plus contiguous leached-cap, oxidized-Cu, and supergene-sulfide horizons. Its
fixed-unit debit is bounded by the primary porphyry deposit allocation: 40,000 units are leachable,
24,000 reach the reducing supergene trap, and 16,000 are oxidized or dissolved; the remaining
65,000 units stay in the hypogene source. The profile is a normalized process proxy, not an assay
or an automatic ore layer.

Buried fertile provinces fail the exposure/preservation gate, while dry provinces fail the primary
Cu source gate. Both publish no horizons or secondary allocation. Point queries respect the
subhorizontal blanket envelope and each horizon's radial truncation. Tests cover source,
oxidation, water-table, trap, preservation, profile ordering, point classification, budget
closure, barren behavior, and deterministic review JSON exposure.

## Alpha.9 — empirical distribution and validation evidence

`MineralSystemValidationReport` gives each of the six architecture-proving families a
source-specific distribution definition and an immutable validation projection. Dataset metadata
retains the external source URI/version, population, grouping/aggregation rule, cutoff/resource
basis, variable units, redistribution/licensing note, sampling method, and explicit calibration
versus held-out partition. Every checked-in row or quantile anchor retains subtype, source-row
reference, source version, grouping, cutoff basis, resource basis, and missing/censor flags; the
validator rejects undeclared variables, duplicate rows, contradictory flags, and missing
calibration/held-out partitions. Primary-system ledgers and derived LCT/BIF/evaporite budgets are
checked for exact bounded closure, while barren or rejected candidates retain their failed gate.

The porphyry importer reads all 228 qualifying rows of the USGS `PorCuTX2008.txt` table (228 of
690 source records have positive tonnage, Cu, and Mo). It preserves source row/ deposit IDs,
names, country/state, coordinates and age text, association and geometry fields, mineral/rock
context, comments, and reference excerpts. Cu/Mo percentages are converted to mass fractions;
Au and Ag remain in g/t, including reported zeros. Rows are tonnage-ranked with a deterministic
every-fifth-row held-out split, and the complete qualifying source release is audited without
claiming an unbiased natural population.

The VMS importer follows the same contract against all 608 qualifying rows of `VMS.tab` from the
USGS 2009-1034 data package (608 of 1,090 source records have positive tonnage, Cu, and Zn).
It preserves one-based source-row references, country/district metadata, deposit names, the
source's Felsic, Mafic, and Bimodal-Mafic subtype labels, raw Pb/Au/Ag grades, and contextual
stringer/comment/mineralogy/host-rock/reference fields. Percent grades are converted to mass
fractions, Au and Ag remain in g/t, and zero Pb/Au/Ag values remain measured zeros; the
deterministic every-fifth-row split retains 121 held-out rows. This is a complete audit of the
qualifying source release, not an unbiased natural population.

The LCT importer reads all 86 rows of the USGS v2.0 `LiCsRb_peg_GT_Deposits.csv` release. It
preserves release IDs, country and ore-mineral subtype labels, per-row cutoff values/units,
references, comments, and explicit missingness across Li2O, Cs2O, Rb2O, Ta2O5, and Sn. It converts
tonnes to Mt, percent grades to mass fraction, and ppm concentrations to mass fraction. The
deterministic every-fifth-row split keeps Cs/Rb-only rows and conditional trace-element coverage
explicit rather than treating unreported values as zero; this is a complete audit of the selected
release, not an exhaustive natural population.

The evaporite/potash importer reads all 102 qualifying rows from the USGS `PotashDeposits.xlsx`
workbook in the PotashXL package. The population rule keeps every source row with a positive first
numeric `RR_ORE_MT` and `RR_K2O_PCT` bound (ordinal labels such as `1)` are skipped), preserves
deposit IDs, country/basin/member metadata, K-mineral labels, resource status, raw resource/depth
text, and references, converts K2O percent to mass fraction, and uses the first numeric bound of
ranged bed-depth text as the modeled depth. Qualified/ranged resource values are explicitly marked
censored and missing depths remain missing. The deterministic every-fifth-row split is a complete
audit of this qualifying source release, not a claim that the historical workbook is an unbiased
981-site natural population.

The BIF importer reads all 66 rows of the combined Superior-Algoma Fe table in USGS Open-File
Report 93-0280. It preserves the source page/name/country references and combined-model rule,
converts Fe and P percent grades to mass fractions, and retains blank phosphorus as explicit
missingness. Rows are tonnage-ranked with a deterministic every-fifth-row held-out split; the
historical table's mixed deposit/district sampling remains a documented population-bias limit.

The placer importer reads all 83 rows of the same report's Placer Pt-Au table. It preserves source
page/name/country/district references and companion Os/Ir/Pd values, converts Pt ppb to g/t, and
retains Au in g/t, including explicitly reported zero grades. Rows are tonnage-ranked with a
deterministic every-fifth-row held-out split; the historical table remains a documented population-
bias limit.

## Alpha.10 — held-out statistical projection

The complete 228-row porphyry, 608-row VMS, 86-row LCT, 66-row BIF, 102-row potash, and 83-row
placer tables are `RAW_TABLE_AUDITED`: these statuses make the import and report contract deterministic
and reviewable without claiming that any historical table is an unbiased natural population. All
reports now also publish structured source-coverage evidence (release row count, qualifying row
count, excluded count, qualification rule, and complete-release flag). They emit deterministic
held-out quantile projections (with log-space error where values are usable) and calibration
covariance/correlation summaries for every declared variable pair. These metrics make missing,
censored, and insufficient held-out coverage explicit; redistribution and coverage review remains
outstanding for the historical source releases.

Completing the Phase 3 scientific exit still requires cleaning the source tables, preserving their
row-level bias/censor metadata, auditing redistribution, and promoting the held-out quantile and
covariance comparisons to a source-backed result.

## Alpha.11 — bounded non-concentric alteration footprint

The porphyry topology now carries an explicit 28-degree alteration azimuth and per-zone center
offsets of 0, 18, and 36 blocks. `zoneAt` evaluates each ordered annulus around its shifted center,
with a 245-block lateral envelope that contains the furthest propylitic edge. The constructor
rejects non-finite azimuths and shifted zones that exceed the envelope; formed and barren gate
semantics remain unchanged. Focused tests prove both the existing zone vocabulary and asymmetric
east/west classification, and the review packet preserves the geometry parameters.

## Alpha.12 — structured source-coverage evidence

Each audited empirical release now publishes a structured source-coverage record alongside its
row-level provenance: the source release row count, qualifying row count, excluded count,
qualification rule, qualifying fraction, and complete-release flag. The validation report checks
that the qualifying count equals the imported table and that a `RAW_TABLE_AUDITED` dataset cannot
claim incomplete release coverage. The review packet exposes these fields for redistribution and
coverage sign-off; they describe the selected historical releases and do not imply an unbiased
natural population.

## Remaining Phase 3 slices

The six architecture families and their deterministic evidence contracts are represented. Remaining
Phase 3 work is the raw-table redistribution review and held-out statistical comparison for the
remaining redistribution/statistical sign-off for the historical source releases. Each new family
must retain barren outcomes, explicit source budgets, and deterministic provenance before Minecraft
presentation is attempted.
