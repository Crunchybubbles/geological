# Reproducibility and random-access contract

## World identity

All identities include:

```text
world seed
model version
scientific digest
dimension profile ID
namespace/object type
home-cell coordinates and local index
purpose and explicit counter
```

The tuple is serialized with the deterministic subset of RFC 8949 CBOR used by `CanonicalCbor`. Text is UTF-8 NFC, integer encodings are minimal, and array order is schema-defined. HMAC-SHA-256 supplies both the root key derivation and domain-separated output. A `StableId` is the first 128 bits; full random/digest outputs remain 256 bits.

`RandomStream` is stateless. Every draw supplies an explicit purpose and counter. Adding a new draw cannot shift an existing object's decisions. `ObjectRandomStream` applies the same contract when an upstream stable object ID, rather than a home-cell/local-index tuple, owns a derived distribution. Bounded integers use rejection sampling and unit doubles use the leading 53 bits.

## Spatial ownership

Negative coordinates use mathematical floor division. A descriptor is compiled only from its home cell and world identity. Queries enumerate a bounded owner-cell neighborhood in stable coordinate/ID order, then evaluate exact fields. Bounded caches may retain immutable descriptors, atlas sites, and pure noise-lattice values, but are not geological truth; the cache-independence tests disable all three layers.

## Numeric policy

Identity and ledgers use integers/bytes only. Continuous fields use `StrictMath`, local coordinates relative to an owning site, explicit clamping, and deterministic stable reductions. Seam tests include negative coordinates, exact tile borders, cache eviction, and arbitrary query order.

The tests include fixed HMAC/CBOR/ID golden vectors. Changing a vector is a world-identity change and requires an explicit model-version decision.

Phase 1 compiles its effective definitions, schemas, units, citations, and parameter provenance into compact canonical JSON. Inputs are sorted by stable ID, strings are UTF-8 NFC, and the full SHA-256 digest of those bytes is frozen into `WorldIdentity`. Input list/map order and authoring whitespace therefore cannot change identity, while any effective scientific parameter change must change it. The snapshot golden is published by the tests and as `registry-snapshot.json` in the review packet.

The authored source is a packaged public JSON resource with a versioned authoring schema. Duplicate keys, trailing content, unknown fields, and unit-vocabulary changes fail before canonical compilation. JSON parser objects never enter domain APIs, and the resource is loaded once when the Phase 1 registry initializes rather than during random-access queries.

Phase 0 and Phase 1 intentionally use different model, scientific-digest, and dimension-profile identities. Phase 0 retains the fixed fertile proof grammar; Phase 1 adds deterministic grammar selection, revised deformation, explicit stratigraphy, and the validated registry digest. A Phase 1 result can therefore never masquerade as Phase 0 geology for the same numeric seed.

Phase 2 freezes its typed material catalog independently, then hashes a canonical manifest containing the unchanged Phase 1 registry digest and the material-catalog digest. Its distinct model/profile identity prevents petrologic results from masquerading as Phase 1 geology. Each body samples its primary modes through an object-keyed stream, so cache state and unrelated draws cannot perturb the realization. Mineral modes and formula-derived bulk element mass fractions close on an exact one-million-part basis; domain-separated largest-remainder tie ranks remove iteration-order and floating-point residuals before values enter local element-delta or system reservoir ledgers.
