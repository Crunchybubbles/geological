# Phase 3 mineral-system increment

Status: seventh Phase 3 architecture-proving slice; all six architecture-proving families are explicit, with porphyry fluid/metal refinement also present and supergene gating still open

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
tunables. Voxel-scale veins, empirical grade-tonnage sampling, supergene profiles, and non-
concentric alteration geometry remain future slices. The state is exposed through both geology and
material query facades and in the deterministic Phase 2 review packet until a dedicated Phase 3
packet exists.

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

## Remaining Phase 3 slices

The next bounded slice should add a primary-Cu-dependent oxidation/leaching and supergene blanket
state with an explicit water-table/preservation gate. Each family must retain barren outcomes,
explicit source budgets, and deterministic provenance before Minecraft presentation is attempted.
