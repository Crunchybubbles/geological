# Phase 3 mineral-system increment

Status: first Phase 3 architecture-proving slice; porphyry topology is explicit, while the remaining five deposit families are still open

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

## Remaining Phase 3 slices

The next bounded slices should add, in order, richer porphyry fluid/metal distributions and
supergene gating, then the VMS stratiform lens/feeder state, LCT pegmatite child-body lineage, BIF
age/ocean-redox sheet, restricted-basin evaporite/potash sequence, and the full source-linked
placer budget. Each family must retain barren outcomes, explicit source budgets, and deterministic
provenance before Minecraft presentation is attempted.
