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

`RandomStream` is stateless. Every draw supplies an explicit purpose and counter. Adding a new draw cannot shift an existing object's decisions. Bounded integers use rejection sampling and unit doubles use the leading 53 bits.

## Spatial ownership

Negative coordinates use mathematical floor division. A descriptor is compiled only from its home cell and world identity. Queries enumerate a bounded owner-cell neighborhood in stable coordinate/ID order, then evaluate exact fields. Bounded caches may retain immutable descriptors, atlas sites, and pure noise-lattice values, but are not geological truth; the cache-independence tests disable all three layers.

## Numeric policy

Identity and ledgers use integers/bytes only. Continuous fields use `StrictMath`, local coordinates relative to an owning site, explicit clamping, and deterministic stable reductions. Seam tests include negative coordinates, exact tile borders, cache eviction, and arbitrary query order.

The tests include fixed HMAC/CBOR/ID golden vectors. Changing a vector is a world-identity change and requires an explicit model-version decision.

Phase 0 and Phase 1 intentionally use different model, scientific-digest, and dimension-profile identities. Phase 0 retains the fixed fertile proof grammar; Phase 1 adds deterministic grammar selection and the revised deformation kernel. A Phase 1 result can therefore never masquerade as Phase 0 geology for the same numeric seed.
