# Phase 6 secondary systems — source-budgeted weathering projection

Status: `phase6-alpha.1` (gossan/oxidation/supergene copper projection).

## Alpha.1 — world-column supergene projection

`OverworldSecondaryWeatheringPlanner` is the first Phase 6 worldgen projection. It consumes the
existing Phase 3 `SupergeneCopperState` through the Phase 2 material facade and classifies the
preserved profile in world columns as `LEACHED_CAP`, `OXIDIZED_COPPER`, or `SUPERGENE_SULFIDE`.
Every point is transformed through the owning province's inverse local frame before classification;
the planner therefore does not assume that a province's local axes are world-aligned. Province
ownership is checked against the base-terrain owner for every column.

`OverworldSecondaryWeatheringColumnPlan` is a bounded overlay, not a new material run or block
inventory. Intervals retain the primary porphyry deposit, weathering process, horizon body, and the
fixed-point profile budgets: `105,000` source units, `65,000` retained hypogene units, `40,000`
leachable units, `24,000` supergene units, and `16,000` oxidized/dissolved loss units. The plan
rejects inconsistent closure and refuses any interval or budget for a barren profile. A horizon's
allocation is the authored profile allocation reference; it is deliberately not multiplied once
per block or silently converted into grade.

The planner exposes one column, one-Y lookup, and exactly the authorized 16×16 target footprint in
stable X-then-Z order. It only scans the bounded profile thickness inside already realized solid
terrain, does not read neighbors or mutate chunks, and returns identical results when the same seam
column is queried through either adjacent chunk context. The NeoForge `/geology secondary` command
shows the interval at the caller's current block, while the standalone
`secondaryWeathering` task writes a deterministic four-chunk seam/budget review artifact to
`atlas-cli/build/phase6/secondary/secondary-weathering.json`.

This slice does not yet generate bauxite/Ni-Co laterite, cassiterite/heavy-mineral/diamond placers,
karst or paleosurface refinements, or optional glacial transport. Those are separate bounded
source-to-sink transformations and remain future Phase 6 slices.

## Exit evidence

`OverworldSecondaryWeatheringPlannerTest` covers formed and barren behavior, world/local frame
projection, source-budget retention, bounded target-chunk order, and adjacent-context seam
agreement. `SecondaryWeatheringPacketGeneratorTest` checks byte-for-byte deterministic JSON,
budget closure, horizon presence, and seam stability for the fixed seed fixture. The artifact is a
review aid, not a save format or a voxel-grade prediction.
