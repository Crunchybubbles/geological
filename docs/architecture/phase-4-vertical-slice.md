# Phase 4 vertical slice — canonical dimension identity

Status: second Phase 4 platform-neutral identity increment (`phase4-alpha.2`); no NeoForge
dependency or block placement is introduced yet.

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
Negative chunk coordinates and profile/identity mismatch are covered by deterministic tests. This
is an adapter-facing contract, not a NeoForge implementation: no platform classes, terrain writes,
or neighbor generation are present yet.

`WorldgenSnapshot` freezes the model, scientific, configuration, presentation, and scale identity
that a generation worker may read. `WorldgenExecutionContext` accepts that snapshot, one authorized
stage, and the executor supplied by the platform callback; it never creates an executor or exposes a
live server/world handle. Non-writing stages and mismatched snapshots fail before work starts, and
writable work still has to name the authorized target chunk. The review packet records the snapshot
digests and the `stage_supplied_only`/`liveServerAccess=forbidden` policy.

The next Phase 4 increment should lock the exact NeoForge 21.1.x patch/build plugin and implement
the adapter boundary against this frozen identity contract before adding terrain or palette writes.
