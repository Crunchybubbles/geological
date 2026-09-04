# Phase 8 dimension profiles — Nether and End native histories

Status: `phase8-alpha.1` (Nether thermal/magmatic province and 3-D cavern terrain compiler).

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

## Remaining Phase 8 slices

The next bounded increments are Nether material/resource history, an End parent-body/provenance and
void-bounded terrain compiler, progression contracts, platform adapters and debug views, and a
cross-dimensional seam/lore review. None of those later slices may weaken the frozen dimension
profiles or silently reuse Overworld surface assumptions.
