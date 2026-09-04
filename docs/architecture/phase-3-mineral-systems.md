# Phase 3 mineral-system increment

Status: tenth Phase 3 architecture-proving slice; all six architecture-proving families, the gated porphyry supergene refinement, and source-audit plus held-out evidence contracts are explicit

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
205-block lateral and 160-block vertical extents, zone boundaries, and intensities are proof
tunables. Voxel-scale veins, empirical grade-tonnage sampling, and non-concentric alteration
geometry remain future slices. The state is exposed through both geology and material query
facades and in the deterministic Phase 2 review packet until a dedicated Phase 3 packet exists.

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

The porphyry importer now reads a checked-in, 14-row cleaned subset of the USGS
`PorCuTX2008.txt` table. It preserves source deposit IDs and subtype codes, excludes rows without
positive tonnage/Cu/Mo values under the declared population rule, converts reported percent grades
to mass fractions, and splits the rows into deterministic calibration and held-out roles. Its
`RAW_TABLE_AUDITED_SUBSET` status is intentionally distinct from a full-population audit.

The VMS importer follows the same contract against the 17-row subset of `VMS.tab` from the USGS
2009-1034 data package. It preserves one-based source-row references, deposit names, and the
source's Felsic, Mafic, and Bimodal-Mafic subtype labels, filters to positive tonnage/Cu/Zn rows,
converts percent grades to mass fractions, and retains a deterministic five-row held-out split.

## Alpha.10 — held-out statistical projection

The remaining four families still use `SOURCE_ANCHORS_PROVISIONAL`: those anchors make the import
and report contract deterministic and reviewable, but do not claim that a handful of anchors
replace a raw, licensed table. All reports emit deterministic held-out quantile projections (with
log-space error where values are usable) and calibration covariance/correlation summaries for
every declared variable pair. These metrics make missing, censored, and insufficient held-out
coverage explicit; the porphyry and VMS metrics are subset-audited, while the other families
remain provisional until their raw source rows replace the anchors.

Completing the Phase 3 scientific exit still requires cleaning the source tables, preserving their
row-level bias/censor metadata, auditing redistribution, and promoting the held-out quantile and
covariance comparisons to a source-backed result.

## Remaining Phase 3 slices

The six architecture families and their deterministic evidence contracts are represented. Remaining
Phase 3 work is the raw-table audit/redistribution review and held-out statistical comparison, plus
non-concentric topology refinement. Each new family must retain barren outcomes, explicit source
budgets, and deterministic provenance before Minecraft presentation is attempted.
