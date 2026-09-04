# Phase 5 exploration geology — observations and hand samples

Status: completed the second bounded exploration slice (`phase5-alpha.2`): deterministic transient
outcrop, float, contact, and structural observations derived from the Phase 4 Overworld column
trace, stable observation IDs, provenance body references, confidence/scale fields, and the
read-only `/geology observations` command, plus coarse hand-sample identification.

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

## Alpha.2 hand-sample identification

`OverworldHandSamplePlanner` adds a bounded hand-sample readout for the exposed solid surface and
for an explicitly selected solid block. It reports the material's coarse lithology, rock definition,
texture, overprint, stable sample ID, provenance bodies, and confidence. Visible constituent modes
are limited to major modes (at least `25,000` ppm, with a maximum of eight and a deterministic
fallback for fine-grained material); any unresolved remainder sets `assayRequired=true`. Air and
fluid intervals are rejected, and the readout remains transient: it does not persist samples or
expose hidden assay truth. Servers can inspect the current surface with the read-only
`/geology hand-sample` command.

Remaining Phase 5 slices are soil, stream-sediment, and heavy-mineral sampling; uncertainty and
detection-limit handling; drill-core/logging and section tools; and a discovery notebook/map that
persists player observations rather than hidden truth.
