# Geological

Current Phase 2 checkpoint: `phase2.0-alpha.94`, with compact metamorphic P-T-t paths and bounded principal-axis strain-frame, material-buffer, fracture-tensor, and sparse trace-element and typed sedimentary-reservoir, fluid-transport, magma residual-inventory, and magma thermal-context evidence.

Current Phase 3 checkpoint: porphyry, VMS, LCT, BIF, restricted-basin evaporite/potash, and source-linked placer slices link intrusion/fluid/stockwork/alteration, basin/lens/feeder, evolved-lineage child bodies, age/redox-bounded sheets, brine succession, and drainage budgets; porphyry fluid/metal phase zoning, a gated primary-Cu supergene profile, and source-specific distribution/validation reports are also explicit, each with barren outcomes. All six reports now read complete qualifying source tables of 228, 608, 86, 66, 102, and 83 rows respectively; each publishes structured release/qualifying coverage plus deterministic held-out quantile projections and calibration covariance summaries.

The current Phase 4 identity increments freeze platform-neutral canonical dimension profiles for
the Overworld, Nether, and End, plus deterministic seed/profile/chunk identity, immutable
worldgen snapshots, and logical stage contracts. A minimal `neoforge-adapter` now pins the
Minecraft 1.21.1 / NeoForge 21.1.249 toolchain, bridges Minecraft dimension/chunk identity into
that contract, exposes deterministic read-only Overworld terrain controls, builds a bounded
base-terrain/lithology column plan, provides an injected-palette chunk writer plus a small vanilla
block palette, registers an Overworld generator/preset hook with explicit surface water/air, and
applies a bounded Phase 2 regolith/surface-clue projection plus read-only `/geology` column, map,
and section overlays. Phase 8 now includes the platform-neutral Nether thermal/cavern and
material/resource slices, the End parent-fragment/progression contracts, and a cross-dimensional
identity/provenance trace; production Nether/End generator bindings remain a future increment.

See [implementation-status.md](docs/architecture/implementation-status.md) for the roadmap-aligned
remaining estimate by phase. Phase 3 redistribution/statistical review remains an external sign-off
item; the Phase 4 required implementation slices are complete, with the remaining work moving to
the exploration and dimension-native phases described in the status table.
The bounded non-concentric porphyry footprint is implemented.

Phase 5 alpha.8 now exposes transient, provenance-rich Overworld outcrop/float/contact/structural
observations, bounded coarse hand-sample and soil/stream/heavy-mineral sampling, interval-valued
geochemical anomaly estimates, bounded drill-core logs, vertical cross-sections, and persistent
player-visible notebook/map evidence through `/geology observations`,
`/geology hand-sample`, `/geology soil`, `/geology stream-sediment`, `/geology heavy-mineral`,
`/geology anomaly`, `/geology drill`, `/geology vertical-section`, and `/geology notebook` commands;
the bounded `explorationTelemetry` review task measures clue sufficiency and travel burden.

Phase 6 alpha.5 now projects the gated primary-Cu gossan/oxidation/supergene profile,
source-gated bauxite/Ni-Co laterite profiles, and source-linked cassiterite/heavy-mineral/diamond
placer families into bounded world-column overlays through `/geology secondary`,
`/geology laterite`, `/geology placers`, `/geology paleosurface`, and the opt-in `/geology glacial`
view; `secondaryWeathering`, `laterite`, `secondaryPlacers`, `paleosurface`, and `glacial` record
source-budget or structural proof and adjacent-chunk seam stability in deterministic review
artifacts.

Phase 7 alpha.10 now adds a source-gated greisen residual-fluid proxy around an evolved felsic pulse,
an explicit carbonate-contact skarn fixture, shallow epithermal and deformation/metamorphic-fluid
orogenic-gold projections, separate MVT/SEDEX/sediment-hosted copper basin/redox projections,
unconformity/sandstone uranium redox-groundwater projections, and layered-intrusion chromite/
Ni-Cu-PGE projections through `/geology greisen`, `/geology skarn`, `/geology epithermal`,
`/geology orogenic-gold`, `/geology basin-hydrothermal`, `/geology uranium`,
`/geology layered-intrusion`, `/geology carbonatite-kimberlite`, `/geology sedimentary-resources`, `/geology geothermal`, and the matching review tasks. These quartz-muscovite,
tourmaline-proxy, calc-silicate, retrograde, silica, argillic, quartz-carbonate,
dolomite-replacement, exhalative, redox, uranium-system, chromitite, sulfide, PGE-reef,
carbonatite/peralkaline REE, kimberlite-cargo, phosphorite, manganese, coal, brine, helium-resource,
and geothermal heat/reservoir
horizons are bounded projections rather than assays or voxel-grade inventories; default planners
retain explicit actual-host/source gates, and artifacts record closed proxy ledgers and
adjacent-chunk seam proofs.

Phase 8 alpha.4 now adds a profile-locked, platform-neutral Nether thermal/magmatic province,
3-D cavern compiler, ordered material history, and source-linked quartz, gold, glowstone, and
ancient-debris prototypes, plus an End parent-body/fragment compiler. It produces bounded
floor/roof/lava and island/void intervals, optional hanging bridges and impact-melt/regolith
layers, stable provenance identities, four synthetic Nether province families and four End parent
families, closed ledgers, and adjacent-chunk seam evidence through the `netherThermal`,
`netherResources`, and `endFragments` review tasks. It also freezes protected portal, dragon-arena,
gateway, outer-city, and chorus structure slots through `endProgression`. The compilers preserve
the frozen dimension envelopes and forbidden-process contracts; platform adapters and
cross-dimensional review remain separate increments.

Geological is a causal geology and terrain-generation project for Minecraft Java 1.21.1 on NeoForge 21.1.x. Its selected world preset will eventually own the canonical Overworld, Nether, and End through dimension-specific geological histories rather than add a separate geology dimension.

The repository contains the **Phase 0 standalone geological atlas proof**, the **Phase 1 platform-neutral query core**, and the first ninety-four **Phase 2 petrologic material-state increments**. The current loader boundary registers the `geological:geological` world preset, its bounded Overworld generator hook, a Phase 2 regolith/surface-clue projection, and read-only `/geology` column/map/section overlays. The first Phase 8 Nether thermal/cavern compiler is now available as a platform-neutral review slice; dimension-native Nether resources and End generation remain future increments. Deterministic random-access geology, validated scientific identity, explainable point queries, transient column/run plans, correlated body-scale bulk-rock composition and physical properties, queryable ideal solid-solution states, mafic–ultramafic, intermediate-to-felsic volcanic, welded glass-rich pyroclastic, rare alkaline–carbonatitic–kimberlitic, sedimentary, lateritic/colluvial/alluvial/glacial surficial, and protolith-aware pelitic, mafic, quartz-rich, carbonate, and serpentinite metamorphic catalog slices, explicit separation of primary pyroclastic rock from reworked marine volcaniclastics and kimberlite carrier rock from unresolved mantle/diamond cargo, bounded, adaptively routed upslope-source colluvial composition with per-reach terrain-decision provenance and route-directness-, route-grade-, and runoff-conditioned transport, explicit immutable and injectable route-policy provenance, dual Phase 1/Phase 2 identity review, representative receiving surface/bedrock sink evidence with exact assigned sink mass, finite cross-parcel source-claim reconciliation with exact grain ledgers, finite cross-parcel source-capacity reconciliation with grain-resolved constrained allocations, typed sedimentary basin context for facies, accommodation, water connectivity, salinity, redox, source catchments, and named reservoir contributions, typed magma-system differentiation context with source reservoirs, exact crystal/residual-melt closure, typed residual-fluid potential and normalized residual-fluid fractions, and canonical fertility-tag evidence, and facies-derived diagenesis state, typed metamorphic burial, strain, fluid, reaction-family, serpentinization-balance, typed fluid input/output, host-aware carbonate decarbonation, partial-melting, strain-intensity, transfer, and retrogression proxies with canonical paired event ages, bounded spatially zoned regional-metamorphic fold-taper context, joined alteration-contribution evidence for process, ages, exact mineral/element deltas, fluids, and response parameters, provenance-rich element-reservoir transfers with age, process, and confidence evidence, caller-calibrated mass/time views of the normalized sediment ledger including exact grain-class conversions, injectable normalized transport-response policy provenance, coarse transport-process selection and deposited process-mixture accounting, exact per-process bulk/grain ledgers, normalized process mixtures at capacity/mobilized/arrival/deposited stages, normalized source shares within each deposited grain class, normalized production-to-deposition response stages, dimensionless hydraulic storage/infiltration/drainage proxies, representative route-positioned loss/bypass sink provenance, explicit transport-loss and bypass sink roles, cohesion-versus-erodibility state, per-source usage accounting, source-to-grain provenance, within-bin dispersion proxies, sorting and profile state, grain-size state, source-conditioned physical properties, and an exactly closed normalized production/transport ledger with slope-, roughness-, and reach-path-conditioned grain-selective delivery controlling the deposited mixture and texture, host-conditioned alteration/weathering responses with primary-to-resolved texture projection, normalized material-buffer capacities and water/volatile inventory evidence, compact positive-semidefinite fracture-tensor and connectivity/intensity evidence, sparse trace-element abundance and micro-log10 evidence, typed sedimentary reservoir chemistry and bounded inventory profiles, deterministic fluid-transport P-T/water-rock/phase axes, exact normalized magma residual-element inventory splits, typed process-fluid conditions, and coarse element-reservoir conservation are proven before coupling the model to NeoForge.

## Stable identity

```text
display name: Geological
mod ID and data namespace: geological
Java base package: io.github.crunchybubbles.geological
license: MIT
Minecraft target: 1.21.1
NeoForge line: 21.1.x
Java: 21
```

## Build and run

The checked-in Gradle wrapper is the only prerequisite beyond a Java 21 JDK.

```shell
./gradlew build
./gradlew :neoforge-adapter:test
./gradlew generateExampleAtlas
./gradlew measureAtlas
./gradlew benchmarkWorldgen
./gradlew explorationTelemetry
./gradlew secondaryWeathering
./gradlew laterite
./gradlew secondaryPlacers
./gradlew paleosurface
./gradlew glacial
./gradlew greisen
./gradlew skarn
./gradlew epithermal
./gradlew orogenicGold
./gradlew basinHydrothermal
./gradlew uranium
./gradlew layeredIntrusion
./gradlew carbonatiteKimberlite
./gradlew sedimentaryResources
./gradlew geothermal
./gradlew netherThermal
./gradlew netherResources
./gradlew endFragments
./gradlew endProgression
./gradlew dimensionTraces
```

The Phase 8 Nether thermal review is written below
`atlas-cli/build/phase8/nether-thermal/`, the material/resource review below
`atlas-cli/build/phase8/nether-resources/`, and the End fragment review below
`atlas-cli/build/phase8/end-fragments/`, and the End progression review below
`atlas-cli/build/phase8/end-progression/`, and the cross-dimensional adapter trace below
`atlas-cli/build/phase8/dimension-traces/`.

On Windows, use `gradlew.bat` in place of `./gradlew`. Maps, cross-sections, column plans, JSON traces, and measurements are written below `atlas-cli/build/phase1/`; the deterministic material/catalog/reservoir review is written below `atlas-cli/build/phase2/`; the Phase 4 worldgen observation is written below `atlas-cli/build/phase4/worldgen/`; the Phase 5 exploration telemetry is written below `atlas-cli/build/phase5/exploration/`; the Phase 6 secondary-weathering review is written below `atlas-cli/build/phase6/secondary/`, the laterite review below `atlas-cli/build/phase6/laterite/`, the secondary-placer review below `atlas-cli/build/phase6/secondary-placers/`, the paleosurface structural review below `atlas-cli/build/phase6/paleosurface/`, the opt-in glacial review below `atlas-cli/build/phase6/glacial/`, the Phase 7 greisen review below `atlas-cli/build/phase7/greisen/`, the skarn review below `atlas-cli/build/phase7/skarn/`, the epithermal review below `atlas-cli/build/phase7/epithermal/`, the orogenic-gold review below `atlas-cli/build/phase7/orogenic-gold/`, the basin/redox review below `atlas-cli/build/phase7/basin-hydrothermal/`, the uranium review below `atlas-cli/build/phase7/uranium/`, and the layered-intrusion review below `atlas-cli/build/phase7/layered-intrusion/`.

## Modules

- `geology-core` is a platform-neutral Java library. It implements stable identity, cell- and object-keyed deterministic random streams, strict public JSON registry authoring and canonical compilation, atlas descriptors, stratigraphic and unconformity kernels, varied event grammar, bounded spatial candidates, exact interval-proved column runs, causal mineral-system proofs, Overworld surface queries, and on-demand Phase 2 constituent assemblage, ideal solid-solution composition, host-conditioned process response, bulk-composition, and typed reservoir-ledger resolution for points, columns, and surface materials.
- `atlas-cli` is a standalone renderer and measurement harness. It emits maps, cross-sections, the canonical registry snapshot, stratigraphic/deformation/provenance traces, column proofs, and engineering observations. It depends on the core but no Minecraft or loader classes.
- `neoforge-adapter` is the pinned NeoForge 21.1.249 loader boundary. It contains the mod entry
  point, deterministic `ResourceKey<Level>`/`ChunkPos` and stage bridges, a read-only Overworld
  terrain-control sampler, a bounded base-terrain/lithology planner, a chunk writer with an
  injected material-to-block resolver, a total coarse vanilla block palette, and an Overworld
  generator/preset registration plus explicit surface water/air, bounded regolith/surface-clue
  projection, bounded Phase 6 secondary-weathering, laterite, secondary-placer, paleosurface, and opt-in glacial overlays, the Phase 7 greisen residual-fluid proxy, actual-host-only skarn and basin MVT fixture, shallow epithermal, metamorphic-fluid orogenic-gold, basin/redox, unconformity/sandstone uranium, and layered-intrusion chromite/Ni-Cu-PGE overlays, read-only `/geology` column/map/section/vertical-section/observation/hand-sample/sampling/anomaly/drill overlays, and the
  serial/shuffled/seam benchmark harness, and a bounded per-player discovery notebook/map saved-data
  bridge. Phase 8 also exposes a read-only native-dimension identity/provenance trace for
  Overworld, Nether, and End chunks; production Nether/End generator bindings remain future work.

See [the Phase 0 architecture](docs/architecture/phase-0-atlas.md), [Phase 1 query-core increment](docs/architecture/phase-1-query-core.md), [Phase 2 material-state increment](docs/architecture/phase-2-material-state.md), [Phase 5 exploration increment](docs/architecture/phase-5-exploration.md), [Phase 6 secondary-systems increment](docs/architecture/phase-6-secondary-systems.md), [Phase 7 deposit-family increment](docs/architecture/phase-7-deposit-families.md), [Phase 8 dimension-profile increment](docs/architecture/phase-8-dimension-profiles.md), [registry authoring contract](docs/architecture/registry-authoring.md), [reproducibility contract](docs/architecture/reproducibility.md), and [toolchain policy](docs/development/toolchain.md) before extending the model.

## Repository boundary

Long-form research and pre-implementation planning are maintained separately. This public repository is independently buildable and testable; builds, tests, and runtime code have no private repository dependency. Contributor-facing contracts required to understand the implemented behavior are maintained here as ordinary public files.
