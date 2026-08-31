# Phase 0 standalone atlas

Status: implemented proof contract
Planning baseline consulted: `geological-planning` revision `5037a51` (2026-08-30)

Phase 0 answers one question: can Geological reconstruct a coherent local history and present geology from only world identity and coordinates, regardless of query order or cache state?

## Scope

The proof contains a deterministic hierarchy of jittered macro-domain and province sites. Each finite object is owned by a province home cell and has a stable ID derived independently from the world seed, model/digest identity, dimension profile, object type, home cell, local index, and purpose. Neighboring province candidates are reconstructed locally; no finite world map is stored.

Every Phase 0 province uses a small synthetic rift-to-arc archetype so that the architecture's joins are testable rather than scientifically broad. Its ordered chronicle creates:

- inherited basement;
- a rift basin with a four-unit sedimentary/volcaniclastic package;
- a synsedimentary VMS horizon and feeder;
- an arc lineage with three cross-cutting pluton pulses;
- an intrusion contact aureole;
- a finite fold and a finite inherited fault with younger reactivation;
- a porphyry Cu-Au proof system and explicitly rejected candidates;
- uplift, an exhumed surface, analytic catchment-owned trunk drainage, weathering, outcrop, and a fixed-budget source-linked placer Au proof.

The archetype is intentionally a proof fixture, not a claim that every eventual province will contain every process. Chronicle grammar diversity and barren-province frequency belong to later increments.

## Query model

`GeologyAtlas` compiles immutable `MacroDomain` and `Province` descriptors. `GeologyQueryEngine` resolves point, cross-section, and transient raster-tile samples. It never stores geology per block or chunk and never writes neighboring tiles.

Older body geometry is evaluated after pulling the present coordinate backward through younger deformation, youngest first. Pluton pulses then replace older hosts by age. The aureole is an overprint around the youngest intrusion contact rather than an unrelated rock layer.

The Overworld Phase 0 surface compiler is deliberately separate from the dimension-neutral atlas and identity packages. The core interfaces carry a dimension profile ID and surface-topology capability; no core identity or atlas API assumes that every future dimension has rainfall, drainage, or a single surface.

## Mineral proof contract

Each candidate returns `FORMED`, `BARREN_SYSTEM`, or `REJECTED` with ordered gate evidence. A formed primary system names its driver, source, medium, pathway, trap, and preservation evidence. The placer additionally names its upstream primary source and balances an integer fixed-point Au ledger across retained source, released material, transport loss, and placer trap.

These are causal topology proofs. Natural grade-tonnage populations, geochemical response tables, and a complete element registry remain outside Phase 0.

## Deliberate approximations

- The locally reconstructible province graph uses a bounded eight-neighbor candidate set, not a complete global Delaunay triangulation.
- Drainage is an analytic, catchment-owned trunk/tributary field. It proves border/order behavior and source-to-trap direction; Priority-Flood refinement is a future experiment.
- Body geometry is 2.5-D and uses bounded analytic kernels. It is not the Phase 1 column/run engine.
- Runtime measurements are repeatable engineering observations, not JMH-grade microbenchmarks or release gates.

No conflict with the accepted planning architecture was required for this slice. These approximations are explicit Phase 0 reductions, not silent changes to later production behavior.
