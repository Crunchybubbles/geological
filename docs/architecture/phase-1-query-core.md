# Phase 1 query-core increment

Status: implemented vertical slice; Phase 1 remains open
Identity: model `phase1.0-alpha.1`, profile `geological:overworld_phase1`, scientific digest `geological:phase1-query-core-v1`

This increment asks whether finite geological objects can be selected and reconstructed as exact integer-block column runs without storing a block or chunk geology map. It adds a separate world identity rather than silently changing the fixed Phase 0 proof.

## Bounded candidate index

Each immutable province descriptor compiles a disposable 256-block uniform-grid index containing finite conservative bounds for its stratigraphic package, pluton pulses, fault damage zone, fold influence, contact aureole, and only those mineral systems that actually formed. Candidate and query cell counts have hard caps. Results are filtered against the controlling horizontal kernel and sorted by stable ID.

The index is an acceleration layer, not geological truth. It stores references to province-owned descriptors, never blocks, chunks, or an unbounded list of visited objects. Recompilation and cache eviction produce the same candidates and column states.

## Column/run evaluation

`GeologyQueryEngine.column` accepts a bounded half-open integer Y interval. Samples represent block centers. The evaluator:

1. resolves the owning province and horizontally intersecting candidates;
2. merges conservative candidate Y intervals;
3. proves candidate-free gaps uniform with one point evaluation;
4. refines candidate intervals at integer block centers;
5. compresses identical coordinate-independent `MaterialState` values into contiguous `MaterialRun` records.

Tests compare every returned block state with an independent point query. This first conservative implementation favors correctness at contacts. Later interval/Lipschitz proofs can skip more evaluations inside broad candidate bounds without changing the API or descriptor identity.

## Deformation guard

The fold has a closed-form forward/inverse pair. The finite fault uses a smooth vertical taper and a bounded Newton inverse: at most eight iterations, a residual below 1/256 block, and a Jacobian determinant constrained to the accepted `0.25..4` interval. Formation-to-present and present-to-formation property grids exercise both directions. Point traces expose the intermediate fault/fold pullback coordinates and measured round-trip residual.

## Chronicle grammar

The Phase 1 profile deterministically selects one of three small rift-to-arc outcomes:

- exhumed fertile: porphyry, VMS, and source-linked placer form;
- buried fertile: porphyry and VMS form, but the inaccessible primary source rejects the placer;
- dry barren: tectonic, basin, pluton, contact, fold, fault, surface, and drainage history remains, while VMS and porphyry driver/source gates fail and no deposit geometry leaks into queries.

The grammar changes the chronicle itself, not merely a display flag. Missing deposits cannot appear in point, surface, spatial-index, or column results. Every failed main candidate still carries explicit gate and provenance evidence.

## Deliberately deferred Phase 1 work

This is not the complete Phase 1 exit. Registry/schema/unit/citation validation, an explicit unconformity kernel, richer stratigraphic coordinates, interval arithmetic inside candidate bounds, fuzzing beyond the deterministic property grids, and formal p95/p99 benchmarking remain subsequent increments. No accepted planning contract was contradicted by this implementation.
