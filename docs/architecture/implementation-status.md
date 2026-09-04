# Implementation status and remaining estimate

This is a scope estimate against the authoritative roadmap in
`C:\minecraft_geology\docs\specification\19_implementation_roadmap.md`. A “slice” is one
bounded implementation increment with deterministic tests and review evidence; it is not a
calendar promise. The current repository is the source of truth for what is actually complete.

| Phase | Current evidence | Remaining implementation estimate |
| --- | --- | --- |
| 0 — atlas proof | Implemented deterministic atlas, chronicle, geometry, drainage, map/section traces, and measurements. | 1 sign-off slice: geological review of the synthetic fixture plus contributor-hardware baselines. |
| 1 — 3-D query core | Implemented point/column/map/trace queries, pullback deformation, bounded planning, spatial index, seam/order/property tests. | 1–2 sign-off slices: review/calibration of tunables and final performance baselines. |
| 2 — material state | `phase2.0-alpha.94` exit contract is satisfied by the catalog/query/schema/review tests; no deposit generation is required by this phase. | 0 required implementation slices; scientific catalog review remains an external calibration activity. |
| 3 — mineral systems | Six architecture families, porphyry fluid/metal zoning, gated supergene refinement, audited porphyry/VMS/LCT/potash/placer subsets plus the complete 66-row BIF table, deterministic held-out quantile/covariance projections, and a bounded non-concentric porphyry footprint are implemented. | 1 sign-off slice: review redistribution for the remaining subset families where the source supports it. |
| 4 — Minecraft vertical slice | No NeoForge adapter or canonical-dimension worldgen is checked in yet; GeologyCore remains platform-neutral as required. | 7–10 slices: platform scaffold/identity, Overworld terrain/material stages, clipping/caches, palette, commands/debug traces, compatibility, and benchmarks. |
| 5 — exploration geology | No persistent exploration/observation loop is checked in yet. | 5–7 slices: outcrops/float, hand samples, soil/stream sampling, uncertainty, drilling/cross-sections, and notebook/map persistence. |
| 6 — secondary expansion | Primary-Cu supergene proof exists as a Phase 3 refinement; broader secondary families are not generated. | 4–6 slices: bauxite/Ni-Co, additional heavy-mineral/diamond placers, karst/regolith/paleosurfaces, and optional glacial transport. |
| 7 — deposit-family expansion | No additional skarn, greisen, epithermal, or basin/redox deposit generators beyond the six proving families. | 7+ family slices, each with dossier, data, invariants, calibration, and clues. |
| 8 — Nether/End profiles | No platform dimension adapters or dimension-native atlas generators are checked in. | 6–9 slices: Nether and End histories/terrain, resources, progression contracts, adapters, seam tests, and expert review. |
| 9 — comprehensive geochemistry | Current reservoirs and element vectors are bounded proof subsets, not the full all-element/partition system. | 4–6 slices: element expansion, solid-solution/polymorph refinement, isotopic/provenance additions, and reviewed response datasets. |
| 10 — extraction/processing | Deliberately not started; the roadmap makes this a separate future design after generation is credible. | A new design phase, not an outstanding generation bug. |

The immediate next engineering slice is the Phase 3 raw-table redistribution/sign-off review.
Porphyry, VMS, LCT, evaporite/potash, and placer have checked-in `RAW_TABLE_AUDITED_SUBSET`
resources; BIF now has a complete 66-row `RAW_TABLE_AUDITED` resource. Together they establish
the metadata, missing/censoring, budget, and deterministic quantile/covariance report contracts
without claiming that historical source tables are unbiased natural populations.
