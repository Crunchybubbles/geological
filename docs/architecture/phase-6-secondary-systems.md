# Phase 6 secondary systems — source-budgeted weathering projection

Status: `phase6-alpha.2` (gossan/oxidation/supergene copper plus source-gated laterite projection).

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

The alpha.1 slice did not generate bauxite/Ni-Co laterite, cassiterite/heavy-mineral/diamond
placers, karst or paleosurface refinements, or optional glacial transport; alpha.2 adds the
laterite family below, while the remaining families stay separate bounded source-to-sink
transformations.

## Alpha.2 — bauxite and Ni-Co laterite profiles

`LateriteProfileState` adds a second source-budgeted weathering family. Aluminous silicate parents
can form a warm-humid, percolating, preserved bauxite profile when the resolved parent composition
contains enough aluminum. Komatiitic ultramafic or serpentinite parents are the only eligible
Ni-Co sources; the current Phase 2 element vocabulary has no Ni/Co entries, so those ledgers are
explicitly labeled ultramafic proxies rather than silently inventing catalog elements. Climate,
drainage, and low-relief preservation gates produce a typed barren state when any required proof
is missing.

`OverworldLateritePlanner` projects each formed profile through the owning province frame into a
bounded column overlay with ferricrete, pisolitic bauxite/kaolinitic transition, or Ni-Co limonite,
smectite, and saprolite horizons. Fixed-point source, retained, and dissolved-loss budgets close
per commodity, and no horizon is multiplied into a voxel inventory. The planner is chunk-local,
stable X-then-Z ordered, and seam-equal across adjacent contexts. The NeoForge `/geology laterite`
view reports the current interval, while the `laterite` CLI task writes a deterministic four-chunk
artifact to `atlas-cli/build/phase6/laterite/laterite.json`.

This slice still does not generate cassiterite/heavy-mineral/diamond placers, karst or paleosurface
refinements, or optional glacial transport. Those remain separate bounded Phase 6 slices.

## Exit evidence

`OverworldSecondaryWeatheringPlannerTest` covers formed and barren behavior, world/local frame
projection, source-budget retention, bounded target-chunk order, and adjacent-context seam
agreement. `OverworldLateritePlannerTest` covers formed bauxite ledger closure, bounded target
chunks, ultramafic-only Ni-Co eligibility, and adjacent-context seam agreement. The two packet
generator tests check byte-for-byte deterministic JSON, budget closure, horizon presence, and seam
stability for the fixed seed fixture. The artifacts are review aids, not save formats or voxel-grade
predictions.
