# Geological

Geological is a causal geology and terrain-generation project for Minecraft Java 1.21.1 on NeoForge 21.1.x. Its selected world preset will eventually own the canonical Overworld, Nether, and End through dimension-specific geological histories rather than add a separate geology dimension.

The repository contains the **Phase 0 standalone geological atlas proof**, the **Phase 1 platform-neutral query core**, and the first forty-eight **Phase 2 petrologic material-state increments**. It intentionally produces no Minecraft blocks: deterministic random-access geology, validated scientific identity, explainable point queries, transient column/run plans, correlated body-scale bulk-rock composition and physical properties, queryable ideal solid-solution states, mafic–ultramafic, intermediate-to-felsic volcanic, welded glass-rich pyroclastic, rare alkaline–carbonatitic–kimberlitic, sedimentary, lateritic/colluvial/alluvial/glacial surficial, and protolith-aware pelitic, mafic, quartz-rich, carbonate, and serpentinite metamorphic catalog slices, explicit separation of primary pyroclastic rock from reworked marine volcaniclastics and kimberlite carrier rock from unresolved mantle/diamond cargo, bounded, adaptively routed upslope-source colluvial composition with per-reach terrain-decision provenance and route-directness-, route-grade-, and runoff-conditioned transport, coarse transport-process selection and deposited process-mixture accounting, explicit transport-loss and bypass sink roles, cohesion-versus-erodibility state, per-source usage accounting, source-to-grain provenance, within-bin dispersion proxies, sorting and profile state, grain-size state, source-conditioned physical properties, and an exactly closed normalized production/transport ledger with slope-, roughness-, and reach-path-conditioned grain-selective delivery controlling the deposited mixture and texture, host-conditioned alteration/weathering responses with primary-to-resolved texture projection, typed process-fluid conditions, and coarse element-reservoir conservation are proven before coupling the model to NeoForge.

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
./gradlew generateExampleAtlas
./gradlew measureAtlas
```

On Windows, use `gradlew.bat` in place of `./gradlew`. Maps, cross-sections, column plans, JSON traces, and measurements are written below `atlas-cli/build/phase1/`; the deterministic material/catalog/reservoir review is written below `atlas-cli/build/phase2/`.

## Modules

- `geology-core` is a platform-neutral Java library. It implements stable identity, cell- and object-keyed deterministic random streams, strict public JSON registry authoring and canonical compilation, atlas descriptors, stratigraphic and unconformity kernels, varied event grammar, bounded spatial candidates, exact interval-proved column runs, causal mineral-system proofs, Overworld surface queries, and on-demand Phase 2 constituent assemblage, ideal solid-solution composition, host-conditioned process response, bulk-composition, and typed reservoir-ledger resolution for points, columns, and surface materials.
- `atlas-cli` is a standalone renderer and measurement harness. It emits maps, cross-sections, the canonical registry snapshot, stratigraphic/deformation/provenance traces, column proofs, and engineering observations. It depends on the core but no Minecraft or loader classes.

See [the Phase 0 architecture](docs/architecture/phase-0-atlas.md), [Phase 1 query-core increment](docs/architecture/phase-1-query-core.md), [Phase 2 material-state increment](docs/architecture/phase-2-material-state.md), [registry authoring contract](docs/architecture/registry-authoring.md), [reproducibility contract](docs/architecture/reproducibility.md), and [toolchain policy](docs/development/toolchain.md) before extending the model.

## Repository boundary

Long-form research and pre-implementation planning are maintained separately. This public repository is independently buildable and testable; builds, tests, and runtime code have no private repository dependency. Contributor-facing contracts required to understand the implemented behavior are maintained here as ordinary public files.
