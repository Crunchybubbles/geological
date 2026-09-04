# Phase 8 dimension profiles — Nether and End native histories

Status: `phase8-alpha.3` (Nether histories/resources plus End parent-fragment terrain and regolith).

## Alpha.1 — Nether thermal cavern boundary

The Nether compiler is deliberately dimension-native. It consumes the frozen
`minecraft:the_nether` profile and accepts only a matching seed, model version, scientific digest,
and profile ID. Its province descriptor retains a stable province, refractory-basement, and
magma-province identity, a 512-block province cell, a four-way synthetic thermal/magmatic family,
and bounded heat and volatile potentials. These are causal state labels, not Earth-geology claims,
ore grades, temperatures, or voxel inventories.

`NetherThermalTerrainCompiler` builds each target chunk as 256 independently queryable columns in
stable X-then-Z order. Every column has a bounded solid floor and roof, an optional hanging-bridge
interval, and a lava interval contained inside the cavern. The vertical envelope is the frozen
Nether `-64..127` range. The compiler uses random-access fields keyed by block coordinates, so a
column queried directly is byte-for-byte equal to the same column reached through either adjacent
chunk plan. The profile's forbidden rifting, sedimentation, uplift, weathering, and surface-water
families remain explicit review evidence.

The standalone `netherThermal` task writes
`atlas-cli/build/phase8/nether-thermal/nether-thermal.json`. The artifact records the frozen
profile digest, province-family and province-ID counts, floor/roof/lava summaries, forbidden
processes, and seam stability. It is deterministic review evidence, not a save format or a claim
that the fictional Nether represents terrestrial geology.

`NetherThermalTerrainCompilerTest` covers identity locking, deterministic repeated plans, random
access at chunk seams, interval bounds, and province potentials. `NetherThermalPacketGeneratorTest`
covers byte-repeatable JSON, all four province families, lava and bridge evidence, and seam proof.

## Alpha.2 — Nether material history and resource prototypes

`NetherMaterialHistoryState` carries an ordered, dimension-native chronicle from the thermal
province through refractory basement formation, mafic lava and pyroclastic packages, layered
intrusions, dike/sill emplacement, collapse brecciation, cooling contraction, volatile alteration,
and roof/fissure condensation. The history retains stable province, basement, magma, and event
identities and closes its bounded retained-material/alteration-loss ledger. Soul-ash accumulation
and refractory breccia are explicit fictional families; no biome name is treated as a deep cause.

`NetherResourceSystemState` gates one source-linked prototype family per coherent province family:
Nether quartz from porous volcanic silica segregation, Nether gold from mafic magmatic/volatile
concentration, glowstone from roof-fissure volatile condensation, and ancient debris from deep
refractory cumulate/breccia conduit history. Each formed body retains source, pathway, host, and
trap identities, three contiguous bounded horizons, and a released/loss/deposit ledger. An
incoherent host returns a named barren gate rather than falling back to independent ore noise.
Resource intervals are clipped to solid, non-lava terrain in `NetherResourcePlanner` and remain
equal when queried directly or through adjacent chunk plans.

The standalone `netherResources` task writes
`atlas-cli/build/phase8/nether-resources/nether-resources.json`, including event/material/resource
family counts, source/path/trap evidence, the default-negative host proof, closed budgets, and seam
stability. The packet is review evidence, not an assay, reserve, temperature, or voxel inventory.
`NetherResourcePlannerTest` and `NetherResourcePacketGeneratorTest` cover identity locking, all
four families, material-event ordering, closed ledgers, barren host gating, repeatable JSON, and
chunk seams.

## Alpha.3 — End parent fragments, impacts, and void regolith

`EndParentBodyState` establishes a stable parent-body provenance record before fragmentation. Each
bounded lattice cell chooses a primitive, silicate-differentiated, metal-separated, or previously
melted parent family and retains composition/metal reservoir identities, a fragment identity, an
impactor identity, an ordered differentiation/fragmentation/impact/void/regolith chronicle, and
closed parent-material and regolith ledgers. Central progression, gateway-ring, and outer-island
roles are explicit. Cells outside the bounded outer ring are void; they do not receive an implicit
continuous crust.

`EndFragmentTerrainCompiler` evaluates complete 3-D island membership from an ellipsoidal body,
not a single Overworld-style surface. It preserves a central island and open void gap, bounded
gateway/outer bodies, impact excavation, shock/breccia or impact-melt events, and a short
void-exposed regolith interval. Every column carries its parent-fragment provenance or an explicit
void result. The frozen End profile's `VOID` medium and forbidden plate, water, sediment, uplift,
and hydrothermal process families are emitted as review evidence. Direct and isolated shuffled
chunk access produce equal columns at seams.

The standalone `endFragments` task writes
`atlas-cli/build/phase8/end-fragments/end-fragments.json`, including role/family/differentiation
counts, impact and regolith events, void columns, progression-contract identity, closed ledgers,
and seam stability. It is a fictional dimension review packet, not an asteroid claim or a voxel
save format. `EndFragmentTerrainCompilerTest` and `EndFragmentPacketGeneratorTest` cover central,
gateway, outer, void, all parent families, impact/regolith bounds, identity locking, repeatable
JSON, and chunk seams.

## Alpha.4 — End progression and protected structure contract

`EndProgressionContract` freezes the canonical central-island, void-gap, gateway-ring, and
outer-island references produced by the fragment compiler. It assigns stable protected slots for
the exit portal, dragon arena, four gateways, outer End cities, and chorus habitats. The contract
retains anchor body IDs, coordinates, radii, the profile's progression-contract identity, and
explicit safety flags. `EndProgressionPlanner` exposes read-only structure lookup and denies geology
terrain writes inside protected slots while allowing the open void gap to remain available to the
platform structure system.

The standalone `endProgression` task writes
`atlas-cli/build/phase8/end-progression/end-progression.json`, including all anchor IDs, protected
slot counts, topology validation, portal/dragon safety, forbidden-process evidence, and seam
stability. `EndProgressionPlannerTest` and `EndProgressionPacketGeneratorTest` cover deterministic
anchors, role ownership, protected-write behavior, central/void topology, repeatable JSON, and
adjacent chunk seams. The contract does not implement Minecraft structure placement; it is the
geology-owned boundary that prevents terrain generation from overwriting progression structures.

## Remaining Phase 8 slices

The next bounded increments are an End parent-body/provenance and void-bounded terrain compiler,
progression contracts, platform adapters and debug views, and a cross-dimensional seam/lore review.
None of those later slices may weaken the frozen dimension profiles or silently reuse Overworld
surface assumptions.
