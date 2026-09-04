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
| 3 — mineral systems | Six architecture families, porphyry fluid/metal zoning, gated supergene refinement, complete qualifying 228-row porphyry, 608-row VMS, 86-row LCT, 66-row BIF, 102-row potash, and 83-row placer tables, deterministic held-out quantile/covariance projections, and a bounded non-concentric porphyry footprint are implemented. | 1 sign-off slice: review redistribution and statistical coverage for the historical source releases. |
| 4 — Minecraft vertical slice | Platform-neutral canonical Overworld/Nether/End profile identities plus deterministic seed/profile/chunk identity, 16×16 target bounds, logical stage order, stage-keyed streams, immutable worldgen snapshots, supplied-executor context, and chunk-local write contract are checked in. A separate `neoforge-adapter` pins the exact 1.21.1/21.1.249/ModDevGradle 2.0.146 toolchain, loads the mod, bridges Minecraft dimension/chunk callbacks into the core request contract, exposes deterministic read-only Overworld coarse terrain controls, produces a bounded target-chunk base-terrain/lithology column plan, provides a chunk-local writer that accepts an injected material-to-block resolver, supplies a total small vanilla block palette for every canonical lithology, registers a custom Overworld generator codec plus `geological:geological` world preset with vanilla Nether/End companions, emits explicit target-chunk air/surface-water states, applies bounded Phase 2 regolith/surface-clue material at `buildSurface`, exposes read-only `/geology` column traces with source/clue provenance, adds bounded deterministic map/section overlays, and records serial/shuffled/seam server-runtime observations. | 0 required implementation slices; rerun the server observation on contributor hardware when compatibility baselines change. |
| 5 — exploration geology | Phase 5 alpha.8 provides transient deterministic outcrop/float/contact/structural observations, bounded hand-sample and soil/stream/heavy-mineral sampling, interval-valued geochemical anomaly estimates, contiguous drill-core logs, vertical cross-section traces, world-persistent per-player notebooks/maps containing only player-visible evidence, and bounded clue-sufficiency/travel-burden telemetry. Stable IDs, frozen world identity, provenance, confidence/detection evidence, bounded NBT, deterministic notebook/telemetry digests, the player notebook/map paths, and the standalone `explorationTelemetry` review artifact are covered by deterministic tests. | 0 required implementation slices; external scientific/readability review and contributor-world telemetry reruns remain sign-off work. |
| 6 — secondary expansion | `phase6-alpha.5` projects the gated Phase 3 gossan/oxidation/supergene Cu profile, source-gated bauxite/Ni-Co laterite profiles, cassiterite/heavy-mineral/diamond placer families, typed present/buried/karst paleosurface refinements, and an opt-in glacial transport prototype into world-column overlays with province-frame transforms, fixed-point source budgets where applicable, barren behavior, chunk seams, `/geology secondary`, `/geology laterite`, `/geology placers`, `/geology paleosurface`, and `/geology glacial` views, and deterministic four-chunk review artifacts. | 0 required implementation slices; glacial positive behavior remains opt-in until a production ice-history compiler is authored. |
| 7 — deposit-family expansion | `phase7-alpha.10` adds the greisen residual-fluid proxy, a source-gated skarn carbonate-contact fixture, shallow epithermal and deformation/metamorphic-fluid orogenic-gold projections, separate MVT/SEDEX/sediment-hosted-copper basin/redox projections, source-gated unconformity/sandstone uranium projections, layered-intrusion chromite/Ni-Cu-PGE projections, carbonatite/peralkaline REE plus kimberlite/diamond projections, sedimentary phosphorite/manganese/coal/lithium-brine/potash-borate/helium-resource projections, and geothermal heat/reservoir projections around the existing engines. All retain bounded horizons, closed source/release/loss/deposit or reservoir accounting, explicit actual-host/source gates, separate kimberlite cargo evidence, porous-medium fluid/gas/heat semantics, chunk seams, read-only commands, and deterministic review artifacts. | 0 required implementation slices; external scientific calibration and contributor-world review remain sign-off work. |
| 8 — Nether/End profiles | `phase8-alpha.2` adds the profile-locked Nether thermal/magmatic province compiler plus dimension-native material history and source-linked quartz, gold, glowstone, and ancient-debris prototypes. Histories retain ordered lava/pyroclastic/intrusive/volatile events; resource bodies retain host/path/trap identities, bounded horizons, closed ledgers, chunk seams, and the `netherThermal`/`netherResources` review artifacts. | 4–7 slices: End parent-body atlas and terrain, progression contracts, platform adapters/debug seams, cross-dimensional review, and expert sign-off. |
| 9 — comprehensive geochemistry | Current reservoirs and element vectors are bounded proof subsets, not the full all-element/partition system. | 4–6 slices: element expansion, solid-solution/polymorph refinement, isotopic/provenance additions, and reviewed response datasets. |
| 10 — extraction/processing | Deliberately not started; the roadmap makes this a separate future design after generation is credible. | A new design phase, not an outstanding generation bug. |

Phase 3 still has one external raw-table redistribution/statistical sign-off slice outstanding;
porphyry, VMS, LCT, BIF, potash, and placer now have complete 228-row, 608-row, 86-row, 66-row,
102-row qualifying, and 83-row `RAW_TABLE_AUDITED` resources. Together they establish the
metadata, structured source-coverage counts, missing/censoring, budget, and deterministic
quantile/covariance report contracts without claiming that historical source tables are unbiased
natural populations. The NeoForge adapter lock and binding to the Phase 4 chunk identity contract
are now complete as a minimal loader slice. The read-only Overworld coarse-terrain control slice
is also complete: it reconstructs bounded atlas/surface fields from the frozen request and snapshot
and binds them to the platform-supplied executor without a live world handle. The bounded
base-terrain/lithology planning slice is also complete: it clamps the geology-owned surface to the
`-64..319` envelope, clips material runs to the solid interval, verifies the terrain and lithology
province owners agree, and enumerates exactly the authorized 16×16 target footprint. The chunk-writer
slice now applies those solid runs to the authorized `ChunkAccess` through a memoized, injected
material-to-block resolver. The small vanilla palette maps every canonical lithology to a coarse
state without encoding grade or alteration. The air/fluid slice now emits explicit air above each
clipped solid interval and surface water up to the bounded sea level, with no groundwater or cave
fluid inference. The regolith slice resolves present-surface material through the frozen Phase 2
catalog, clips a bounded weathering/transport interval, carries source/deposit/clue provenance,
and applies a separate regolith palette during `buildSurface`. The generator/preset slice now registers a real
`GeologicalOverworldChunkGenerator` codec, captures the platform seed at generator-state creation,
and invokes the writer during `fillFromNoise`; the preset keeps vanilla biome/structure inputs and
vanilla Nether/End companions until their dimension-native slices. The map/section overlay slice
adds bounded, deterministic histogram summaries over the same column trace. The Phase 4 benchmark
slice now compares a target chunk in serial and seeded-shuffled column order, checks both adjacent
chunk seams, and writes `atlas-cli/build/phase4/worldgen/worldgen-benchmark.json` with runtime
observations; invariant booleans are separate from non-gating timings. Phase 4's required
implementation slices are complete without allowing vanilla surface rules to overwrite geology.

The Phase 8 Nether terrain and material/resource boundary is now complete through alpha.2. The
compiler consumes the existing frozen Nether dimension profile and rejects mismatched world
identities; it never imports Overworld surface-water, rifting, sedimentation, uplift, or weathering
assumptions. Each target chunk enumerates exactly 256 columns in stable X-then-Z order, with bounded
solid roof/floor runs, lava intervals, optional hanging bridges, province IDs, and heat/volatile
potentials. The `netherThermal` task writes
`atlas-cli/build/phase8/nether-thermal/nether-thermal.json`, including all four synthetic province
families, forbidden-process evidence, and adjacent-chunk seam equality. The `netherResources` task
writes `atlas-cli/build/phase8/nether-resources/nether-resources.json`, retaining ordered
lava/pyroclastic/intrusive/volatile histories and source-linked quartz, gold, glowstone, and
ancient-debris prototype ledgers. End parent-body generation, progression, adapters, and
cross-dimensional review remain separate slices.
