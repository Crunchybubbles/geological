# Geological

Geological is a causal geology and terrain-generation project for Minecraft Java 1.21.1 on NeoForge 21.1.x. Its selected world preset will eventually own the canonical Overworld, Nether, and End through dimension-specific geological histories rather than add a separate geology dimension.

The repository contains the **Phase 0 standalone geological atlas proof** and the first **Phase 1 platform-neutral query increment**. It intentionally produces no Minecraft blocks: deterministic random-access geology, explainable point queries, and transient column/run plans are proven before coupling the model to NeoForge.

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

On Windows, use `gradlew.bat` in place of `./gradlew`. Current maps, cross-sections, column plans, JSON traces, and measurements are written below `atlas-cli/build/phase1/`.

## Modules

- `geology-core` is a platform-neutral Java library. It implements stable identity, deterministic random streams, atlas descriptors, varied event grammar, bounded spatial candidates, point/column/trace queries, causal mineral-system proofs, and Overworld surface queries.
- `atlas-cli` is a standalone renderer and measurement harness. It depends on the core but no Minecraft or loader classes.

See [the Phase 0 architecture](docs/architecture/phase-0-atlas.md), [Phase 1 query-core increment](docs/architecture/phase-1-query-core.md), [reproducibility contract](docs/architecture/reproducibility.md), and [toolchain policy](docs/development/toolchain.md) before extending the model.

## Repository boundary

Long-form research and pre-implementation planning are maintained separately. This public repository is independently buildable and testable; builds, tests, and runtime code have no private repository dependency. Contributor-facing contracts required to understand the implemented behavior are maintained here as ordinary public files.
