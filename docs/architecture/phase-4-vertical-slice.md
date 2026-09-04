# Phase 4 vertical slice — canonical dimension identity

Status: first Phase 4 platform-neutral identity increment (`phase4-alpha.1`); no NeoForge
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
profiles. The next Phase 4 increment should lock the NeoForge 21.1.x adapter and connect the
Overworld profile to seed/dimension/chunk-stage identity before adding terrain or palette writes.
