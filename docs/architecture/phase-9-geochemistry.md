# Phase 9 comprehensive geochemistry

Status: `phase9-alpha.4` (isotope and provenance evidence).

## Alpha.1 — Condition-qualified element behavior

`ElementBehaviorCatalog` supplies a deterministic profile for every element in the current
Phase 2 vocabulary. Each profile records one or more conditional affinity classes, a coarse
mobility class, compatible host classes, and explicit volatile/radiogenic flags. Conditions are
textual routing evidence for process kernels; they are not fixed Goldschmidt labels, measured
partition coefficients, or equilibrium calculations.

The registry is intentionally separate from `BulkComposition`, `TraceElementVector`, and the
fixed-point reservoir ledgers. Existing material and deposit queries therefore retain their
identities and exact closures, while future reservoir, melting, fluid, and weathering slices can
narrow behavior using P–T, redox, ligand, phase, and host evidence. The Phase 2 review artifact
publishes the registry so reviewers can inspect coverage and conditions without treating the
metadata as an assay.

## Alpha.2 — Expanded element/source vocabulary

The `ChemicalElement` vocabulary now contains 50 deterministic entries. The extension adds helium,
Li–B, transition-metal pathfinders (V, Mn, Co, Ni), chalcophile and evolved-melt elements (Ga,
Rb, Sr, Y, Zr, Nb, Mo, Cd, In, Ag, Sn, Cs), representative light rare-earth elements (La, Ce,
Nd), refractory HFSEs (Hf, Ta, W, Re), and Pb–Th–U. Atomic weights and symbol parsing are part of
the typed identity; the extension entries are appended so existing Phase 2 enum ordinals and
deterministic tie-breaks remain stable.

`ElementBehaviorCatalog` supplies condition-qualified profiles for all 50 entries. The sparse
`TraceElementVector` accepts the expanded pathfinder set, and `MagmaResidualInventoryState` can
partition the newly tracked incompatible/residual elements with the same exact fixed-point
closure. Existing authored mineral recipes remain sparse and do not acquire invented trace mass;
the composition-rounding path now allocates residue only among elements actually present in the
authored constituents.

## Alpha.3 — Solid-solution and polymorph refinement

`MineralPhaseRefinementCatalog` adds an explicit envelope for each of the eight existing ideal
solid-solution definitions. Endmember fraction ranges, interpolation model, and a conditional
cooling/exsolution marker are reviewable without changing the Phase 2 ideal interpolation result;
the range API rejects mixes that do not close to one million fixed units. The catalog also records
the lizardite–chrysotile–antigorite serpentine family with bounded coarse temperature, pressure,
and hydration windows. Variant selection is deterministic and returns no result when the authored
conditions do not justify a variant; it is not a thermodynamic stability solver.

The material-review artifact publishes both refinement groups, including their conditions and
member ranges, so reviewers can distinguish authored bounds from unresolved activity, ordering,
solvus, and exsolution behavior.

## Alpha.4 — Isotope and provenance evidence

`IsotopicProvenanceEvidence` lazily derives optional fixed-point parent-isotope evidence from a
resolved composition, its source reservoir ID, and formation age. The current nuclide set is
K-40→Ar-40, Rb-87→Sr-87, Th-232→He-4, and U-238→He-4. Each returned record retains the total parent
inventory, natural-isotope inventory, bounded decay fraction, accumulated daughter-potential, and
retained isotope with exact closure plus an explicit confidence value. Samples choose the linked
magma-system ID when available and otherwise their rock-body ID, so the evidence remains tied to
the existing provenance graph.

The calculation is intentionally a monotonic accumulated-potential proxy using published half-life
and abundance constants; it does not estimate a rock age, model daughter transport, or simulate
individual atoms. Empty parent systems stay absent from the sparse result, and the material-review
artifact exposes the evidence inputs and closure fields for audit.

## Remaining Phase 9 slices

The next bounded increments are reviewed response/partition datasets and a processing-facing
assay/mineral-liberation API. Each must preserve sparse state, explicit uncertainty, source-linked
hosts, and exact system-scale ledger closure.
