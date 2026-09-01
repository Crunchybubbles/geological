# Registry authoring contract

The effective Phase 1 scientific content is authored at `geology-core/src/main/resources/data/geological/registry/phase1-scientific.json`. It is an ordinary public classpath resource and is packaged in `geology-core`; neither loading nor compilation reaches outside this repository.

## Boundary and format

The root declares `geological:registry_authoring:v1` and contains four logical collections: units, citations, definition schemas, and definitions. Every scientific quantity has a finite numeric value, an explicit supported unit ID, and exactly one provenance basis: `citation_id` or `tunable_design_value`. Definitions name their schema/version, confidence, model version, citations, and definition dependencies.

`RegistryJsonLoader` is deliberately strict. It rejects duplicate keys, trailing JSON documents, missing/null required fields, unknown fields, unsupported enum values, a document larger than 1 MiB, and any unit ID/symbol/dimension tuple that differs from the runtime unit vocabulary. Logical compilation then rejects duplicate or malformed stable IDs, unresolved references, dependency cycles, schema/kind/version mismatches, undeclared parameters, wrong dimensions, and values outside inclusive constraints. An invalid document cannot initialize a Phase 1 world.

Jackson is contained at this parsing boundary. Core atlas, registry, and query APIs expose only project-owned immutable Java types. The authored JSON is not loaded per query and there is no runtime file watcher or mutable reload path.

## Canonical identity

Authoring syntax is not world identity. Successful compilation sorts all unordered logical content, normalizes strings to UTF-8 NFC, writes `geological:registry_snapshot:v1` canonical JSON, and hashes every canonical byte with SHA-256. Whitespace and input collection order are insignificant; any effective definition, schema, unit, citation, or provenance change changes the scientific digest and therefore all derived world-object identities.

The authored Phase 1 resource recompiles to the published digest:

```text
sha256:3404480eb62c77f249bd91f66fe4ac399cae742541e9736b36316e42cf9235f4
```

Phase 2 adds a separate typed material catalog at `geology-core/src/main/resources/data/geological/registry/phase2-materials.json`. Authoring schema v5 retains the same strict-boundary principles while validating mineral formulas, unique/referenced solid-solution endmembers, exclusive endmember ownership, exact central modal closure, bounded mass-conserving modal-variation axes, typed textures, ordered unit-interval property distributions, mineral references, exact and non-overlapping protolith-family coverage for every alteration target, and complete bounded process-fluid states where mass-transfer processes require them. Solid-solution definitions declare their deliberately ideal mixing model; definition and endmember authoring order is canonicalized before hashing. Every non-isochemical proof response authors multiple distinct host-family targets; recipe and family order is likewise canonicalized. The frozen material digest is `sha256:d78beb50fa432e5ee91ba0cbc256d66a4fc6340f79229087c09616fe589a234a`; a canonical manifest composes it with the unchanged Phase 1 registry digest rather than rewriting the Phase 1 snapshot.

The initial JSON resource replaced the earlier Java-built fixture without changing canonical content. Later scientific catalog changes advance the Phase 2 model version and material/composite digests while leaving the Phase 1 identity frozen. Tests freeze the digests, fuzz authoring collection order, and exercise malformed JSON and unit-vocabulary drift.

## Contributor workflow

Edit the authoring resource, run `./gradlew build generateExampleAtlas`, and review both the digest golden and generated `registry-snapshot.json`. An intended effective change requires an explicit model-version/world-identity decision; do not update a golden merely to make a failing build pass. A later catalog may split authoring across files or add a friendlier YAML/JSON5 front end, but it must compile through the same logical validation and canonical snapshot boundary.
