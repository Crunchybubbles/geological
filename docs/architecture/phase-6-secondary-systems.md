# Phase 6 secondary systems — source-budgeted weathering projection

Status: `phase6-alpha.5` (gossan/oxidation/supergene copper, laterite, secondary placers,
structural paleosurface refinements, and opt-in glacial transport).

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
placers, karst or paleosurface refinements, or optional glacial transport; alpha.2 through alpha.5
add those families as separate bounded source-to-sink transformations.

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

The laterite slice remains independently queryable and source-budgeted; the later alpha increments
add placer, paleosurface, and opt-in glacial overlays without changing its parent/source contract.

## Alpha.3 — cassiterite, heavy-mineral, and diamond placer projections

`SecondaryPlacerState` adds three mechanical source-to-sink families over the existing channel and
hydraulic-trap proof. Cassiterite requires a formed, fertile LCT child body; because the Phase 2
catalog intentionally has no Sn/cassiterite definition yet, its source ledger is explicitly named
an LCT residual cassiterite proxy. Heavy-mineral sand uses the resolved upstream source parent and
only durable dense catalog phases (ilmenite, magnetite, hematite, chromite, garnet proxies,
diamond, and perovskite) as an indicator vector. Diamond requires a `DIAMOND_BEARING`
`MantleCargoState`; the current kimberlite cargo remains source-context-unresolved, so it cannot
produce a diamond placer.

Each family requires a connected fluvial channel, a hydraulic gradient-break trap, and preserved
alluvial-bar context. Released, transport-loss, and trapped budgets close exactly against the
source proxy, and three contiguous basal/bar/rework horizons carry the allocation without turning
it into per-voxel inventory. `OverworldSecondaryPlacerPlanner` resolves a separate upstream parent
near the province source before classifying a downstream channel parcel, preserving provenance
across basin cover. It emits all three family states in bounded 16×16 X-then-Z order and compares
equal at adjacent chunk seams. `/geology placers` reports all family intervals at the caller's Y;
`secondaryPlacers` writes a deterministic four-chunk artifact to
`atlas-cli/build/phase6/secondary-placers/secondary-placers.json`.

## Alpha.4 — regolith, buried paleosurfaces, and karst gates

`PaleosurfaceState` adds three typed structural refinements over the existing surface, parent, and
unconformity evidence: present exposed residual regolith, a preserved buried unconformity
weathering profile, and a karst-bauxite pocket candidate. The first two are explicitly structural
profiles with no commodity ledger. Present residual regolith requires an in-situ weathered surface,
no active channel/outcrop, and low relief; buried profiles require a point inside the authored
unconformity footprint with enough present cover to preserve the ancient weathering thickness.
Formation ages remain distinct (`0.02 Ma` for present weathering versus the unconformity age), and
horizons retain their process overprint and stable body identity.

Karst bauxite is deliberately stricter: the parent must be limestone or dolostone, the point must
be inside the paleokarst footprint, and an external source body must be present in the surface
material context. Carbonate alone therefore produces a typed barren state with a
`carbonate_parent`, `paleokarst`, or `aluminum_source` gate rather than an ore pocket. The current
fixture has no formed karst pockets; this is an explicit negative proof, not an implicit catalog
fallback.

`OverworldPaleosurfacePlanner` emits all three profiles in bounded 16×16 X-then-Z order, clips
horizon intervals to realized solid terrain, and compares equal at adjacent chunk seams. The
NeoForge `/geology paleosurface` view reports all horizons at the caller's Y. The `paleosurface`
CLI task writes a deterministic structural review to
`atlas-cli/build/phase6/paleosurface/paleosurface.json`; the artifact is marked
`structural_refinement_no_ore_inventory` and carries no grade or voxel inventory.

## Alpha.5 — opt-in glacial transport prototype

`GlacialTransportState` and `GlacialHistoryPolicy` establish the missing event-local contract for
glacial transport: an explicit ice class, unit flow direction, connected flow, ice thickness,
entrainment, deposition efficiency, formation age, and finite source inventory. The formed proof
closes released material into transport loss plus deposited till/outwash and exposes basal till,
melt-outwash, and indicator-train horizons. `OverworldGlacialTransportPlanner` clips those
horizons to realized solid columns and remains seam-stable.

The default Overworld policy is `none`, so no glacial profile can appear without an authored
ice-history descriptor. The deterministic review fixture uses an explicit opt-in policy only to
exercise the positive ledger and seam paths; `/geology glacial` in the adapter uses the safe
default and reports the `ice_history` barren gate. This preserves the specification's rule that
glacial transport is not inferred from present terrain or a random climate flag.

The `glacial` CLI task writes the opt-in prototype review to
`atlas-cli/build/phase6/glacial/glacial.json`, including the default no-ice negative proof,
source/release/loss/deposit closure, and seam stability. It is a prototype contract rather than a
claim that the current Overworld has a reconstructed Pleistocene ice sheet.

## Exit evidence

`OverworldSecondaryWeatheringPlannerTest` covers formed and barren behavior, world/local frame
projection, source-budget retention, bounded target-chunk order, and adjacent-context seam
agreement. `OverworldLateritePlannerTest` covers formed bauxite ledger closure, bounded target
chunks, ultramafic-only Ni-Co eligibility, and adjacent-context seam agreement. The packet
generator tests check byte-for-byte deterministic JSON, budget closure, horizon presence, and seam
stability for the fixed seed fixture. `OverworldSecondaryPlacerPlannerTest` covers LCT/heavy-source
provenance, diamond-fertility gating, bounded target chunks, and seam equality; its packet test
checks the three-family artifact and zero unresolved diamond placement. The artifacts are review
aids, not save formats or voxel-grade predictions. `OverworldPaleosurfacePlannerTest` covers
present and buried formation ages/horizons, carbonate-only karst rejection, bounded intervals, and
seam equality; its packet test checks deterministic JSON, explicit structural-inventory labeling,
and seam stability. `OverworldGlacialTransportPlannerTest` covers the explicit no-ice gate, closed
fixture till budget, bounded intervals, and seam equality; its packet test checks deterministic
positive/negative prototype evidence and seam stability.
