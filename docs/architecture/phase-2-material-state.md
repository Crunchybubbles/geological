# Phase 2 petrologic material-state increment

Status: first implementation increment; Phase 2 content breadth and geological calibration remain open  
Identity: model `phase2.0-alpha.1`, profile `geological:overworld_phase2`  
Base registry digest: `sha256:3404480eb62c77f249bd91f66fe4ac399cae742541e9736b36316e42cf9235f4`  
Material catalog digest: `sha256:fcd78f23997e677682276abc083b588ca88e1fddffa48a28ffc00f24abf8ef52`  
Composite scientific digest: `sha256:d2283325312f05fd7274b30ea1b74af3715957dc7dd1e3c2f6e070a40a88afbd`

This increment asks whether the Phase 1 body/history result can resolve into a coherent bulk-rock parcel without storing mineralogy per block. It deliberately reuses the existing rift-to-arc geometry and deposit proof rather than adding another body or deposit family.

## Typed material catalog

`phase2-materials.json` is a public classpath data pack with a strict boundary. Duplicate JSON keys, trailing documents, missing or unknown fields, invalid enum values, unsupported elements, malformed namespaced IDs, unsafe citation URIs, non-closing mineral modes, unresolved mineral references, and incomplete lithology/overprint coverage all prevent Phase 2 world creation.

The first catalog contains:

- 22 ideal mineral/endmember definitions needed by the existing rock, alteration, sulfide, weathering, and placer proof;
- one primary assemblage and physical-behavior record for every one of the ten currently implemented `Lithology` values;
- one metamorphic, hydrothermal, or weathering response for every implemented `Overprint` value.

Modes use exact integer parts per million and must sum to one million. Ideal formulas use a bounded element vocabulary. Whole-rock element mass fractions are derived from formula mass, modal volume, and mineral density, then closed to exactly one million parts with deterministic largest-remainder rounding. The catalog labels its modes and physical scalars as proof tunables pending geological review; it does not present them as measured universal rock compositions.

## Derived query state

`Phase2World.create(seed)` returns a `MaterialQueryEngine`. The finite matrix of ten lithologies and nine overprints is validated and interned once per engine, so queries reuse the same immutable recipe/composition values. A point query first obtains the immutable Phase 1 geological sample and then derives a `PetrologicSample` containing:

- the primary and resolved mineral assemblages;
- formula-derived primary and resolved bulk element composition;
- an exact local element-delta ledger;
- porosity, permeability, and erodibility response;
- protolith-aware metamorphic facies and a compact P-T-t path;
- an optional linked magma-lineage pulse state;
- an optional sedimentary facies, maturity, diagenesis, and named-provenance state.

The three existing pluton pulses share the province magma-system ID and progress from diorite through granodiorite to the felsic stock. Rock names are therefore outputs of one ordered differentiation abstraction, not independent random rolls. Existing basin members retain basement provenance; the marine volcaniclastic member additionally names the magmatic lineage.

Contact hornfels is an isochemical response: it changes P-T-t and physical state without changing whole-rock element inventory. Potassic, phyllic, propylitic, chloritic, gossan, regolith, and unconformity-weathering responses blend an authored replacement assemblage and expose exact signed element deltas. These local normalized deltas are the element-reservoir/ledger skeleton; the existing mineral-system source ledgers remain the authority for coarse deposit inventory.

## Column and surface projection

`MaterialQueryEngine.column` projects the existing Phase 1 interval proof rather than sampling every block again. It resolves one material state per geological run, retains the underlying transition/candidate/complexity evidence, and returns contiguous `PetrologicRun` values. Exhaustive block-center tests prove that every compressed state equals an independent point-material query, including across cache eviction and fresh engine construction.

`MaterialQueryEngine.surface` resolves the existing surface fields into one of three explicit relationships: exposed bedrock, in-situ regolith, or transported alluvial placer. Bedrock and regolith retain their source body identity. A placer parcel instead uses the formed placer deposit identity/age, names the upstream porphyry deposit, and carries the existing coarse Au source and trap allocations (`100000` source and `20000` trapped `phase0_fixed_units`). A hydraulic trap in a barren province cannot manufacture alluvial placer material.

## Identity and compatibility

Phase 1 model/profile/digest golden values are unchanged. Phase 2 composes the frozen Phase 1 registry digest and the typed material-catalog digest into a new canonical scientific manifest. The new model version and dimension profile ensure that adding scientific material content changes every downstream object identity rather than masquerading as a Phase 1 world.

Tests cover formula and modal closure, every implemented lithology/overprint pair, strict malformed-authoring rejection, frozen digests, magma lineage ordering, sedimentary provenance, isochemical versus mass-transfer behavior, exact ledger closure, point/column equivalence, surface source budgets, barren-trap rejection, and reproducibility across cache eviction and fresh world construction.

## Deliberate limits

This is not the Phase 2 exit candidate. It does not yet provide the planned 38-class/approximately 50-mineral content breadth, solid-solution interpolation, continuously sampled compositional distributions, reviewed property datasets, full system-scale element reservoirs, or presentation/processing policy. Minecraft/NeoForge realization remains deferred to Phase 4.
