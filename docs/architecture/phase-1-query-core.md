# Phase 1 query-core increment

Status: engineering exit candidate implemented; geological review and representative-platform baselines remain
Identity: model `phase1.0-alpha.2`, profile `geological:overworld_phase1`, scientific digest `sha256:3404480eb62c77f249bd91f66fe4ac399cae742541e9736b36316e42cf9235f4`

This increment asks whether finite geological objects can be selected and reconstructed as exact integer-block column runs without storing a block or chunk geology map. It adds a separate world identity rather than silently changing the fixed Phase 0 proof.

## Bounded candidate index

Each immutable province descriptor compiles a disposable 256-block uniform-grid index containing finite conservative bounds for its stratigraphic package, unconformity/weathering profile, pluton pulses, fault damage zone, fold influence, contact aureole, and only those mineral systems that actually formed. Candidate and query cell counts have hard caps. Results are filtered against the controlling horizontal kernel and sorted by stable ID.

The index is an acceleration layer, not geological truth. It stores references to province-owned descriptors, never blocks, chunks, or an unbounded list of visited objects. Recompilation and cache eviction produce the same candidates and column states.

## Validated scientific snapshot

`Phase1ScientificRegistry` loads the first small effective snapshot from the public, packaged `phase1-scientific.json` resource; it is not the future full rock or deposit catalog. A strict parser rejects duplicate/trailing JSON, missing and unknown fields, unsupported enum values, and unit-vocabulary drift before its platform-neutral compiler validates:

- unique namespaced definition, schema, and citation IDs;
- schema version/kind agreement and exact parameter fields;
- physical dimension and inclusive numeric constraints;
- definition/citation reference resolution and definition dependency cycles;
- finite numbers carrying either a citation or an explicit `tunable_design_value` rationale.

Successful compilation sorts logical content, emits UTF-8 NFC canonical JSON, and retains the full SHA-256 digest in world identity. Invalid content fails before a world can be created with diagnostics tied to logical schema paths. Tests prove input-order and whitespace independence, reject malformed authoring and a deliberately dimensionally invalid field, and prove that changing effective content changes object IDs. Moving the same logical content from Java construction to JSON authoring preserved the frozen digest. The initial conceptual sources are [Catuneanu (2019)](https://doi.org/10.1016/j.earscirev.2018.09.017), [Laurent et al. (2016)](https://doi.org/10.1016/j.epsl.2016.09.040), and [Grose et al. (2021)](https://doi.org/10.5194/gmd-14-6197-2021); compressed block dimensions remain labeled engineering tunables rather than scientific claims. The complete format and migration contract is in [registry authoring](registry-authoring.md).

## Stratigraphic coordinate and unconformity

The Phase 1 chronicle now erodes a bounded, aged unconformity after rifting and before basin opening. Its implicit surface has finite horizontal support, bounded relief, and a six-block weathering profile. The younger package uses that surface as its base; accommodation-controlled thickness decreases toward the elliptical margin, so member surfaces onlap and pinch out instead of forming horizontal world-wide bands. A normalized `StratigraphicCoordinate` selects basal conglomerate, volcaniclastic, shale, and sandstone members.

Both surfaces live in formation coordinates. Query points pull back through only younger fold/fault events, while trace artifacts show both formation and reconstructed present boundary elevations. The fixed Phase 0 chronicle and its old basin evaluator remain unchanged.

## Column/run evaluation

`GeologyQueryEngine.column` accepts a bounded half-open integer Y interval. Samples represent block centers. The evaluator:

1. resolves the owning province and horizontally intersecting candidates;
2. analytically intersects the vertical line with every relevant package member, unconformity profile, ellipsoid, lens/feeder, fault zone, aureole, and alteration shell;
3. pushes formation-space transition elevations through younger deformation;
4. isolates integer blocks adjacent to each conservative transition and proves every remaining interval uniform;
5. samples one block center per proven interval and compresses identical coordinate-independent `MaterialState` values into contiguous `MaterialRun` records.

`ColumnIntervalProof` exposes the analytic transition elevations and integer split coordinates used by each result. Tests compare every returned block state with an independent point query at a porphyry/pluton contact, across a 49-column multi-province grid, and in seven targeted package, VMS, pluton, porphyry, and fault columns. For the reference 384-block porphyry contact, the new proof requires 29 point evaluations instead of the prior conservative 384 while returning the same seven runs.

Every result also exposes descriptive `ColumnPlanComplexity`. A caller supplies `ColumnPlanBudget`; exceeding it reports deterministic diagnostics and never changes material identity or substitutes fallback geology. The Phase 1 review budget is 16 candidates, 64 analytic transitions, 64 point evaluations, and 32 material runs per 384-block column. Seed/cell fuzz cases and four measured 16×16 chunks remain inside those ceilings.

## Deformation guard

The fold has a closed-form forward/inverse pair. The finite fault uses a smooth vertical taper and a bounded Newton inverse: at most eight iterations, a residual below 1/256 block, and a Jacobian determinant constrained to the accepted `0.25..4` interval. Formation-to-present and present-to-formation property grids exercise both directions. Point traces expose the intermediate fault/fold pullback coordinates and measured round-trip residual.

## Chronicle grammar

The Phase 1 profile deterministically selects one of three small rift-to-arc outcomes:

- exhumed fertile: porphyry, VMS, and source-linked placer form;
- buried fertile: porphyry and VMS form, but the inaccessible primary source rejects the placer;
- dry barren: tectonic, basin, pluton, contact, fold, fault, surface, and drainage history remains, while VMS and porphyry driver/source gates fail and no deposit geometry leaks into queries.

The grammar changes the chronicle itself, not merely a display flag. Missing deposits cannot appear in point, surface, spatial-index, or column results. Every failed main candidate still carries explicit gate and provenance evidence.

## Validation and measurement gate

Deterministically enumerated property cases cover 16 world seeds and cells from the origin through negative coordinates and million-cell offsets. Failure messages carry replay seed, cell, event/model, point, column, and Y evidence so the first failing case is directly replayable. The suite checks chronicle dependency/order uniqueness, formed/rejected mineral gates and ledgers, finite bounded deformation inverses, interval proof equality at every block center, and the Phase 1 complexity budget. A complete 16×16 porphyry-contact chunk is also queried in forward and reverse order across cache clearing.

The measurement harness now records cold observations plus warm p50/p95/p99 runtime and current-thread allocation for porphyry-contact, VMS, fault-zone, and background 16×16 chunks. It records maximum/total candidate, transition, evaluation, and run counts, diagnostic violations, and a result signature. These are transparent engineering observations rather than a JMH claim or a cross-platform service-level objective.

The platform-neutral code now meets the implemented Phase 1 engineering exit questions: point queries can say what is present and why, and chunk-shaped column plans remain transient, bounded, reproducible, and independent of query/cache order. Remaining sign-off work is geological review of the synthetic fixture, baselines on representative contributor/CI hardware, and calibration of explicit tunables. Additional geometry or deposit families should not be added before that review. Minecraft/NeoForge block realization remains deliberately deferred. No accepted planning contract was contradicted by this implementation.
