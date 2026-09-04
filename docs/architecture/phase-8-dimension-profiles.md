# Phase 8 dimension profiles — Nether and End native histories

Status: `phase8-alpha.2` (Nether thermal/magmatic terrain, material history, and resource prototypes).

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

## Remaining Phase 8 slices

The next bounded increments are an End parent-body/provenance and void-bounded terrain compiler,
progression contracts, platform adapters and debug views, and a cross-dimensional seam/lore review.
None of those later slices may weaken the frozen dimension profiles or silently reuse Overworld
surface assumptions.
