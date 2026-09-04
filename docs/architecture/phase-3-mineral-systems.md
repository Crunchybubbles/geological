# Phase 3 mineral-system increment

Status: third Phase 3 architecture-proving slice; porphyry, VMS, and LCT pegmatite topologies are explicit, while three deposit families are still open

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

## Remaining Phase 3 slices

The next bounded slices should add richer porphyry fluid/metal distributions and supergene gating,
then the BIF age/ocean-redox sheet, restricted-basin evaporite/potash sequence, and the full
source-linked placer budget. Each family must retain barren outcomes, explicit source budgets, and
deterministic provenance before Minecraft presentation is attempted.
