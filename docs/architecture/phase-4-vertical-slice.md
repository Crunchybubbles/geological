# Phase 4 vertical slice — canonical dimension identity

Status: completed platform-neutral identity increment (`phase4-alpha.2`) plus loader adapter lock,
chunk-writer, vanilla-palette, Overworld generator/preset, explicit air/surface-water, bounded
regolith/surface-clue, and read-only debug-command slices
(`phase4-loader-alpha.1`–`phase4-loader-alpha.7`).

`DimensionGeologyProfile` freezes the shared contract that a future Minecraft adapter will consume
for `minecraft:overworld`, `minecraft:the_nether`, and `minecraft:the_end`. Each profile carries a
stable profile ID/version and scientific digest, vertical envelope, topology, gravity frame,
confidence policy, allowed/forbidden process families, fluid media, boundary terrain model,
material/mineral registry IDs, biome adapter, structure-progression contract, and scale profile.
`DimensionGeologyProfiles` returns the sorted immutable catalog and rejects unknown dimension keys.

The Overworld profile is Earth-analogue and pins the existing Phase 2 scientific digest. The Nether
profile is explicitly fictional, water-poor, high-temperature, and cavern/roof/floor based; it
allows lava and magmatic volatiles while forbidding sedimentation, surface-water drainage, and
sediment transport. The End profile is explicitly fictional, fragmented parent bodies in void;
it uses bounded islands and local gravity, allows fragmentation/impact/void-regolith processes,
and forbids plate-margin, hydrothermal, and surface-water assumptions. Nether and End registries
are intentionally empty at this identity stage; material and mineral-system dossiers are future
increments rather than invented resources.

The profile catalog is platform-neutral and does not instantiate `GeologyAtlas` for the fictional
profiles. `WorldgenChunkIdentity` and `WorldgenChunkRequest` now bind a Minecraft world seed to
exactly one profile and target chunk, expose half-open 16×16×profile-envelope bounds, and derive
stage-keyed random streams without mutable draw state. `WorldgenStage` freezes the required logical
order (context, terrain controls, base terrain, lithology, structures/deposits/alteration,
caves/aquifers, regolith/clues, biome decoration, and validation). A request can only expose its
authorized prefix, marks non-writing stages explicitly, and rejects writes to a neighboring chunk.
Negative chunk coordinates and profile/identity mismatch are covered by deterministic tests. The
separate `neoforge-adapter` module now pins Minecraft `1.21.1`, NeoForge `21.1.249`, ModDevGradle
`2.0.146`, Parchment `2024.11.17`, and Java 21. Its `GeologicalWorldgenAdapter` accepts a
Minecraft `ResourceKey<Level>` and `ChunkPos`, resolves one canonical profile identity, and
returns the platform-neutral `WorldgenChunkRequest`; null identity inputs are rejected before
core work. A minimal `@Mod("geological")` entry point and generated `neoforge.mods.toml` prove
that the packaged loader boundary is real. `GeologicalWorldgenRegistries` now binds the custom
chunk-generator codec, and the `geological:geological` preset selects it for the Overworld while
retaining vanilla Nether/End companions; neighbor generation remains forbidden. `OverworldTerrainControlSampler` now
binds the coarse-terrain stage to the
platform-supplied immutable snapshot and reconstructs deterministic elevation, uplift, slope,
weathering, drainage, outcrop, and province/domain provenance for block-column centers. Samples
from adjacent chunk contexts agree at the same world coordinate, and cache eviction does not alter
the result; this slice remains read-only. `OverworldBaseTerrainPlanner` then derives a bounded
column plan at the writable base-terrain stage: the continuous surface is clamped to the profile
envelope, the existing geological material runs are clipped to the solid interval, and the
terrain/lithology province owner is checked for agreement. Its target-chunk plan enumerates all
256 columns in stable order without touching a Minecraft `ChunkAccess`. `OverworldBaseTerrainWriter`
applies those solid runs through a platform-neutral block sink, and `GeologicalChunkWriter` binds
that sink to the authorized `ChunkAccess` with an injected, memoized material-to-block resolver.
`GeologicalBlockPalette` supplies a total, coarse mapping from every canonical lithology to an
existing vanilla block state; it does not encode grade/alteration or register custom blocks.
`OverworldAirFluidPlanner` and `OverworldAirFluidWriter` now derive and emit explicit air above the
solid surface plus surface water up to the bounded sea level, while leaving groundwater and caves
to their logical stage. `OverworldRegolithPlanner` resolves the Phase 2 surface material at the
same frozen identity, clips weathered/colluvial/alluvial material to the top of solid terrain, and
retains source bodies, material body, deposit IDs, drainage, slope, and clue kind. Its writer applies
only that interval and passes the clue provenance through the sink. `GeologicalWorldgenRegistries` registers the generator codec in the static
Minecraft chunk-generator registry, and `geological:geological` supplies that generator for the
Overworld while retaining vanilla Nether/End companions. `GeologicalOverworldChunkGenerator`
captures the seed supplied to `createState` and invokes the chunk writer from `fillFromNoise`;
vanilla surface rules are intentionally suppressed; `buildSurface` now applies the geological
regolith projection instead of allowing vanilla surface rules to overwrite it.

`OverworldColumnDebugTrace` joins the base, air/fluid, and regolith plans without storing blocks or
granting write authority. The NeoForge `/geology here` and `/geology column <x> <z>` commands emit
that deterministic trace, including surface intervals, lithology, clue kind, source/deposit counts,
weathering, slope, flow, and channel-distance values; non-Overworld commands fail without doing
geological work.

`WorldgenSnapshot` freezes the model, scientific, configuration, presentation, and scale identity
that a generation worker may read. `WorldgenExecutionContext` accepts that snapshot, one authorized
stage, and the executor supplied by the platform callback; it never creates an executor or exposes a
live server/world handle. Non-writing stages and mismatched snapshots fail before work starts, and
writable work still has to name the authorized target chunk. The review packet records the snapshot
digests and the `stage_supplied_only`/`liveServerAccess=forbidden` policy.

The next Phase 4 increment should add map/section debug overlays plus compatibility/benchmark
coverage for shuffled generation, structures, and other terrain authorities while preserving the
plan's clipping and deterministic seam tests. Dimension-native Nether/End generators remain
separate slices.
