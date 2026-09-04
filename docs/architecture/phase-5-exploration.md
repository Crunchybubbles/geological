# Phase 5 exploration geology — observations and hand samples

Status: completed the fifth bounded exploration slice (`phase5-alpha.5`): deterministic transient
outcrop, float, contact, and structural observations derived from the Phase 4 Overworld column
trace, stable observation IDs, provenance body references, confidence/scale fields, and the
read-only `/geology observations` command, plus coarse hand-sample identification and surface
sediment sampling, interval-valued geochemical anomaly estimates, and bounded drill-core logs.

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

## Alpha.3 soil and sediment sampling

`OverworldSedimentSampler` adds three transient field methods: soil samples require exposed
in-situ regolith or colluvial mantle; stream-sediment samples require a generated channel reach;
and heavy-mineral samples use the same channel setting and return a deterministic concentrate
spectrum weighted by constituent density and weathering resistance. Each result retains its surface
material kind, source/body provenance, flow accumulation, hydraulic-trap score, and channel
distance. Major constituent modes are bounded to eight entries, and sparse geochemical indicators
are reduced to coarse order-of-magnitude signals. These signals are not detection-limit-aware
assays; that uncertainty and reporting layer remains a separate slice. Read-only server commands
are `/geology soil`, `/geology stream-sediment`, and `/geology heavy-mineral`.

The sampler rejects unsupported settings instead of manufacturing a clue, and it never writes or
persists a sample or an upstream sediment inventory. A barren heavy-mineral concentrate is a valid
result with lower confidence.

## Alpha.4 geochemical uncertainty and detection limits

`OverworldGeochemicalAnomalyPlanner` consumes only the coarse indicator signals in a transient
surface sample. For each reported element it derives a method-specific detection limit, a bounded
interval, a censored/non-detected flag, and a normalized anomaly score using a stable object-keyed
random stream. The result preserves sample and body provenance and is deterministic across repeated
queries and traversal order. It is explicitly not a laboratory assay: intervals are observation
uncertainty, and values below the detection limit remain censored rather than silently becoming
zero. The read-only command is `/geology anomaly <soil|stream|heavy>`.

## Alpha.5 drill-core logging

`OverworldDrillCorePlanner` accepts a surface collar plus a bounded depth (or an explicit solid
Y interval) and resolves the exact compressed Phase 2 material runs in that column. Each transient
`DrillCoreInterval` reports its stable interval ID, lithology/rock definition, resolved texture,
coarse visible constituent modes, coarse indicator signals, and body/deposit provenance. The log
validates contiguous coverage, refuses air or fluid intervals and depths beyond `256` blocks, and
retains material-evaluation counts for review. `/geology drill <depth>` exposes the log without
writing a shaft, changing terrain, or persisting hidden geology.

Remaining Phase 5 slices are vertical cross-sections; a discovery notebook/map that persists player
observations rather than hidden truth; and Phase 5 telemetry/exit review.
