# Phase 9 comprehensive geochemistry

Status: `phase9-alpha.1` (condition-qualified element behavior registry).

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

## Remaining Phase 9 slices

The next bounded increments are expanded element vocabulary and source profiles, solid-solution/
polymorph refinement, optional isotope/provenance evidence, reviewed response/partition datasets,
and a processing-facing assay/mineral-liberation API. Each must preserve sparse state, explicit
uncertainty, source-linked hosts, and exact system-scale ledger closure.
