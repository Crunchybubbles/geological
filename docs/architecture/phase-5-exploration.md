# Phase 5 exploration geology — surface observations

Status: completed the first bounded exploration slice (`phase5-alpha.1`): deterministic transient
outcrop, float, contact, and structural observations derived from the Phase 4 Overworld column
trace, stable observation IDs, provenance body references, confidence/scale fields, and the
read-only `/geology observations` command.

`OverworldExplorationObservationPlanner` consumes the same immutable
`OverworldRegolithPlanner` used by generation and debug overlays. An exposed bedrock clue yields an
`OUTCROP`; transported colluvial or alluvial material yields a `FLOAT`; a distinct regolith/bedrock
boundary or an exposed lithology transition yields a `CONTACT`; and a fault-damage material
intersecting the exposed interval yields a `STRUCTURAL` observation. Every observation carries the
observed `MaterialState`, any adjacent contact state, sorted source/body IDs, a bounded confidence
value, an observation scale, and a stable ID derived from the frozen world identity, kind, block
cell, and Y coordinate.

The planner is a presentation/evidence projection only. It does not write blocks, persist a
regional geology cache, expose unobserved deposit grades, or mutate the reconstructible material
truth. The command is therefore safe to run on a server and returns only the bounded observations
at the caller's current Overworld column. `planTargetChunk()` provides the same transient evidence
for deterministic review without crossing the chunk-local generation write boundary.

Remaining Phase 5 slices are hand-sample rock/mineral identification; soil, stream-sediment, and
heavy-mineral sampling; uncertainty/detection-limit handling; drill-core/logging and section
tools; and a discovery notebook/map that persists player observations rather than hidden truth.
