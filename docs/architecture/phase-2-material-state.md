# Phase 2 petrologic material-state increment

Status: fifth implementation increment; Phase 2 content breadth and geological calibration remain open

Identity: model `phase2.0-alpha.5`, profile `geological:overworld_phase2`

Base registry digest: `sha256:3404480eb62c77f249bd91f66fe4ac399cae742541e9736b36316e42cf9235f4`

Material catalog digest: `sha256:b7291775f45f73ac91000ed6a30d73bf975b5c39bc1edf1ec06b6436c7921d0b`

Composite scientific digest: `sha256:f0abc63e80c379923a49805ebe64d0967179ecf3e710abe3ed3e4fb510efadb4`

This increment asks whether the Phase 1 body/history result can resolve into a coherent bulk-rock parcel without storing mineralogy per block. It deliberately reuses the existing rift-to-arc geometry and deposit proof rather than adding another body or deposit family.

## Typed material catalog

`phase2-materials.json` is a public classpath data pack with a strict boundary. Duplicate JSON keys, trailing documents, missing or unknown fields, invalid enum values, unsupported elements, malformed namespaced IDs, unsafe citation URIs, non-closing mineral modes, unresolved mineral references, and incomplete lithology/overprint coverage all prevent Phase 2 world creation.

The first catalog contains:

- 22 ideal mineral/endmember definitions needed by the existing rock, alteration, sulfide, weathering, and placer proof;
- one primary assemblage, typed texture, and bounded physical-property distribution record for every one of the ten currently implemented `Lithology` values;
- one metamorphic, hydrothermal, or weathering response for every implemented `Overprint` value, with target assemblages selected through exact protolith-family coverage.

Authored modes are central integer parts per million and must sum to one million. Each rock also declares one or more named modal-variation axes. An axis is a sparse set of signed mineral loadings that must sum to zero; cumulative loading on each mineral may not exceed the rock's declared modal-spread fraction. These axes encode substitution or mixture directions instead of allowing every mineral to vary independently. Rocks additionally declare a coarse texture class independent from presentation and triangular minimum/mode/maximum distributions for porosity, permeability, and erodibility. Ideal formulas use a bounded element vocabulary. Whole-rock element mass fractions are derived from formula mass, modal volume, and mineral density, then closed to exactly one million parts with deterministic largest-remainder rounding. The catalog labels its modes, covariance loadings, spread bounds, textures, and property distributions as proof tunables pending geological review; it does not present them as measured universal rock compositions.

## Derived query state

`Phase2World.create(seed)` returns a `MaterialQueryEngine`. The finite matrix of ten lithologies and nine overprints is validated into immutable recipe templates once per engine. Each geological body receives one deterministic triangular score per authored modal-variation axis; applying the signed loadings produces correlated primary modes while triangular property distributions independently resolve porosity, permeability, and erodibility. All streams are keyed by world identity, stable body ID, and explicit property purpose, so querying another point or clearing a cache cannot consume or perturb a result. Integer modes close exactly to one million parts using largest-remainder allocation with domain-separated deterministic tie-breaking. A bounded cache retains resolved body/overprint recipes without becoming part of the scientific state.

A point query first obtains the immutable Phase 1 geological sample and then derives a `PetrologicSample` containing:

- the primary and resolved mineral assemblages;
- formula-derived primary and resolved bulk element composition;
- an exact local element-delta ledger and causal process attribution;
- optional typed process-fluid conditions for hydrothermal and weathering responses;
- porosity, permeability, and erodibility response;
- protolith-aware metamorphic facies and a compact P-T-t path;
- an optional linked magma-lineage pulse state;
- an optional sedimentary facies, maturity, diagenesis, and named-provenance state.

The three existing pluton pulses share the province magma-system ID and progress from diorite through granodiorite to the felsic stock. Rock names are therefore outputs of one ordered differentiation abstraction, not independent random rolls. Existing basin members retain basement provenance; the marine volcaniclastic member additionally names the magmatic lineage.

Contact hornfels is an isochemical response: it changes P-T-t and physical state without changing whole-rock element inventory. Potassic, phyllic, propylitic, chloritic, gossan, regolith, and unconformity-weathering responses select exactly one target recipe for the host's `GeneticFamily`, blend its assemblage, and expose exact signed element deltas. The current catalog deliberately assigns the same proof target to every family; the schema and resolver now support later calibrated host-dependent recipes without algorithm changes. A typed `MaterialProcessLedger` separates the positive and negative sides of that normalized delta, names the causal aureole, hydrothermal system, or weathering system, and carries matching chronicle events when the supplied geological history contains them. Its additions and removals balance because both bulk compositions are normalized; it is process attribution, not a claim that the local parcel is an absolute closed mass reservoir.

Hydrothermal and weathering definitions must also author a `ProcessFluidState`; inactive and contact-isochemical responses must omit it. This is a sparse rule-table key, not a runtime equilibrium calculation. It contains a fluid medium, five-bin redox class, five-bin pH/acidity class, five-bin salinity class, four-state sulfur axis, independent chloride/reduced-sulfur/carbonate/fluorine-boron capacities on `0..3`, and an integrated fluid-flux class on `0..3`. The pH bins correspond to `<3`, `3–5.5`, `5.5–8.5`, `8.5–10.5`, and `>10.5`; salinity bins correspond to `<0.5`, `0.5–3.5`, `3.5–10`, `10–30`, and `>30` wt% NaCl equivalent. Current assignments are explicit proof tunables pending scientific review.

System-scale inventory remains coarse but is now exposed through typed `ElementReservoirLedger` values. Each formed mineral-system decision is compiled from its exact fixed-point source ledger into an initial source reservoir and explicitly typed deposit, retained-source, diffuse-halo/loss, or transport-loss transfers. Ledgers require exact closure and a formed deposit must receive a deposit allocation. Point and surface samples carry the ledger for a deposit they intersect, including the existing porphyry Cu budget and the upstream-source/trap allocation for placer Au. This adds queryable conservation semantics without introducing new deposit geometry or invented inventory numbers.

## Column and surface projection

`MaterialQueryEngine.column` projects the existing Phase 1 interval proof rather than sampling every block again. It resolves one material state per geological run, retains the underlying transition/candidate/complexity evidence, and returns contiguous `PetrologicRun` values. Exhaustive block-center tests prove that every compressed state equals an independent point-material query, including across cache eviction and fresh engine construction.

`MaterialQueryEngine.surface` resolves the existing surface fields into one of three explicit relationships: exposed bedrock, in-situ regolith, or transported alluvial placer. Bedrock and regolith retain their source body identity. A placer parcel instead uses the formed placer deposit identity/age, names the upstream porphyry deposit, and carries the existing coarse Au source and trap allocations (`100000` source and `20000` trapped `phase0_fixed_units`). A hydraulic trap in a barren province cannot manufacture alluvial placer material.

## Identity and compatibility

Phase 1 model/profile/digest golden values are unchanged. Phase 2 composes the frozen Phase 1 registry digest and the typed material-catalog digest into a new canonical scientific manifest. Advancing the Phase 2 model version for authored modal covariance ensures that the scientific change alters every downstream object identity rather than masquerading as the prior alpha.

Tests cover formula and modal closure, mass-conserving/bounded covariance axes, body-keyed distribution golden vectors, bounded physical-property sampling, same-body continuity and cross-body correlated variation, exact protolith recipe coverage, fluid-state/ligand bounds, authoring-order canonicalization, every implemented lithology/overprint pair, strict malformed-authoring rejection, frozen digests, magma lineage ordering, sedimentary provenance, isochemical versus mass-transfer behavior, exact typed reservoir closure, point/column equivalence, surface source budgets, barren-trap rejection, and reproducibility across cache eviction and fresh world construction.

`./gradlew generateExampleAtlas` also writes `atlas-cli/build/phase2/example/phase2-material-review.json`. The deterministic review artifact publishes the Phase 2 identity, catalog coverage, central modes and modal-variation axes, representative body and overprint realizations, normalized process contributions, typed fluid conditions, system reservoirs, and the source-linked placer context. It is intended for scientific and implementation review, not as a save format.

## Deliberate limits

This is not the Phase 2 exit candidate. It does not yet provide the planned 38-class/approximately 50-mineral content breadth, solid-solution interpolation, geologically differentiated protolith response recipes, reviewed property and fluid-response datasets, calibrated multi-stage reservoir transport, or presentation/processing policy. Minecraft/NeoForge realization remains deferred to Phase 4.
