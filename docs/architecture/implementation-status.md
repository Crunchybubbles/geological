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
| 4 — Minecraft vertical slice | Platform-neutral canonical Overworld/Nether/End profile identities plus deterministic seed/profile/chunk identity, 16×16 target bounds, logical stage order, stage-keyed streams, immutable worldgen snapshots, supplied-executor context, and chunk-local write contract are checked in. A separate `neoforge-adapter` pins the exact 1.21.1/21.1.249/ModDevGradle 2.0.146 toolchain, loads the mod, bridges Minecraft dimension/chunk callbacks into the core request contract, exposes deterministic read-only Overworld coarse terrain controls, produces a bounded target-chunk base-terrain/lithology column plan, provides a chunk-local writer that accepts an injected material-to-block resolver, supplies a total small vanilla block palette for every canonical lithology, registers profile-specific Overworld/Nether/End generator codecs through the `geological:geological` world preset, emits explicit target-chunk air/surface-water states, applies bounded Phase 2 regolith/surface-clue material at `buildSurface`, exposes read-only `/geology` column traces with source/clue provenance, adds bounded deterministic map/section overlays, and records serial/shuffled/seam server-runtime observations. | 0 required implementation slices; rerun the server observation on contributor hardware when compatibility baselines change. |
| 5 — exploration geology | Phase 5 alpha.8 provides transient deterministic outcrop/float/contact/structural observations, bounded hand-sample and soil/stream/heavy-mineral sampling, interval-valued geochemical anomaly estimates, contiguous drill-core logs, vertical cross-section traces, world-persistent per-player notebooks/maps containing only player-visible evidence, and bounded clue-sufficiency/travel-burden telemetry. Stable IDs, frozen world identity, provenance, confidence/detection evidence, bounded NBT, deterministic notebook/telemetry digests, the player notebook/map paths, and the standalone `explorationTelemetry` review artifact are covered by deterministic tests. | 0 required implementation slices; external scientific/readability review and contributor-world telemetry reruns remain sign-off work. |
| 6 — secondary expansion | `phase6-alpha.5` projects the gated Phase 3 gossan/oxidation/supergene Cu profile, source-gated bauxite/Ni-Co laterite profiles, cassiterite/heavy-mineral/diamond placer families, typed present/buried/karst paleosurface refinements, and an opt-in glacial transport prototype into world-column overlays with province-frame transforms, fixed-point source budgets where applicable, barren behavior, chunk seams, `/geology secondary`, `/geology laterite`, `/geology placers`, `/geology paleosurface`, and `/geology glacial` views, and deterministic four-chunk review artifacts. | 0 required implementation slices; glacial positive behavior remains opt-in until a production ice-history compiler is authored. |
| 7 — deposit-family expansion | `phase7-alpha.10` adds the greisen residual-fluid proxy, a source-gated skarn carbonate-contact fixture, shallow epithermal and deformation/metamorphic-fluid orogenic-gold projections, separate MVT/SEDEX/sediment-hosted-copper basin/redox projections, source-gated unconformity/sandstone uranium projections, layered-intrusion chromite/Ni-Cu-PGE projections, carbonatite/peralkaline REE plus kimberlite/diamond projections, sedimentary phosphorite/manganese/coal/lithium-brine/potash-borate/helium-resource projections, and geothermal heat/reservoir projections around the existing engines. All retain bounded horizons, closed source/release/loss/deposit or reservoir accounting, explicit actual-host/source gates, separate kimberlite cargo evidence, porous-medium fluid/gas/heat semantics, chunk seams, read-only commands, and deterministic review artifacts. | 0 required implementation slices; external scientific calibration and contributor-world review remain sign-off work. |
| 8 — Nether/End profiles | `phase8-alpha.8` adds the profile-locked Nether thermal/magmatic province, material history, and source-linked quartz/gold/glowstone/ancient-debris prototypes, plus an End parent-body/fragment compiler with primitive, silicate-differentiated, metal-separated, and previously-melted families, central/gateway/outer roles, void gaps, impact/breccia, regolith ledgers, and a protected central/gateway/outer progression contract. The dimension-aware debug trace routes each canonical dimension through its native compiler, preserves distinct seed/profile/chunk identities, and proves adapter-boundary seam/topology invariants. NeoForge now binds custom Nether and End generator codecs into the selected world preset and writes native host/void/fluid plans without vanilla carvers or surface replacement. Profile-locked biome/substrate controls expose normalized native signals without biome-driven deep materials or write authority, and a fixed seeded shuffle proves Nether/End plans are request-order independent. The compatibility review proves portal-coordinate identity isolation, native process/fluid contracts, progression safety, and premise-relative lore guardrails. Deterministic `netherThermal`, `netherResources`, `endFragments`, `endProgression`, `dimensionTraces`, and `dimensionCompatibility` artifacts prove bounded identities and seams. | 0–1 slice: expert/lore sign-off and contributor-world validation. |
| 9 — comprehensive geochemistry | `phase9-alpha.6` centralizes the 23-element/three-sulfur-state crystal-capture responses in a versioned sparse catalog, wires the magma residual split to it, and exposes confidence/review status in the artifact. `ProcessingAssay` now provides exact sparse resolved-element closure with deterministic authored constituent-host allocations and an explicit ideal-separation upper bound. Response and assay rows remain authored pending external review. | 0 required implementation slices; external scientific/license review of response data and host-allocation assumptions remains a release sign-off activity. |
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
and applies a separate regolith palette during `buildSurface`. The generator/preset slice now
registers real profile-specific `GeologicalOverworldChunkGenerator`,
`GeologicalNetherChunkGenerator`, and `GeologicalEndChunkGenerator` codecs, captures the platform
seed at generator-state creation, and invokes the appropriate native writer during `fillFromNoise`;
the preset keeps vanilla biome/structure inputs while disabling incompatible surface rules/carvers
for the native Nether/End volume models. The map/section overlay slice
adds bounded, deterministic histogram summaries over the same column trace. The Phase 4 benchmark
slice now compares a target chunk in serial and seeded-shuffled column order, checks both adjacent
chunk seams, and writes `atlas-cli/build/phase4/worldgen/worldgen-benchmark.json` with runtime
observations; invariant booleans are separate from non-gating timings. Phase 4's required
implementation slices are complete without allowing vanilla surface rules to overwrite geology.

The Phase 8 Nether terrain/material-resource and End parent-fragment boundary is now complete
through alpha.8. The
compiler consumes the existing frozen Nether dimension profile and rejects mismatched world
identities; it never imports Overworld surface-water, rifting, sedimentation, uplift, or weathering
assumptions. Each target chunk enumerates exactly 256 columns in stable X-then-Z order, with bounded
solid roof/floor runs, lava intervals, optional hanging bridges, province IDs, and heat/volatile
potentials. The `netherThermal` task writes
`atlas-cli/build/phase8/nether-thermal/nether-thermal.json`, including all four synthetic province
families, forbidden-process evidence, and adjacent-chunk seam equality. The `netherResources` task
writes `atlas-cli/build/phase8/nether-resources/nether-resources.json`, retaining ordered
lava/pyroclastic/intrusive/volatile histories and source-linked quartz, gold, glowstone, and
ancient-debris prototype ledgers. The `endFragments` task writes
`atlas-cli/build/phase8/end-fragments/end-fragments.json`, retaining parent-fragment provenance,
differentiation, central/gateway/outer roles, void columns, impact/breccia events, regolith
intervals, closed parent/regolith ledgers, and adjacent-chunk seam equality. The `endProgression`
task writes `atlas-cli/build/phase8/end-progression/end-progression.json`, freezing portal-arrival,
dragon-arena, gateway, outer-city, and chorus anchors as protected structure slots while proving
the central arena write block and void-gap write allowance. The dimension-aware trace planner and
NeoForge bridge now expose read-only identity/provenance summaries for all three canonical
dimensions, with distinct same-seed chunk IDs and direct/adjacent seam checks. The
`dimensionTraces` task writes `atlas-cli/build/phase8/dimension-traces/dimension-traces.json`.
The selected world preset now assigns profile-locked custom generator codecs to all three canonical
dimensions. The Nether writer owns bounded air/host/lava/resource intervals, while the End writer
owns bounded air/parent/regolith/impact intervals and leaves protected progression columns to the
platform structure system; vanilla surface rules and carvers are disabled for these native volume
models. The profile-locked biome/substrate adapter controls and seeded shuffle checks now cover
the native biome boundary without granting write authority. The `dimensionCompatibility` task writes
`atlas-cli/build/phase8/dimension-compatibility/dimension-compatibility.json`; all deterministic
compatibility, identity, process/medium, progression, seam, and premise-relative lore guardrails
pass, with expert review still required before a stable release.
