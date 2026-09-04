# Phase 7 deposit families — source-gated deposit projections

Status: `phase7-alpha.5` (greisen, explicit skarn host fixture, epithermal shallow-fluid,
orogenic-gold metamorphic-fluid, and basin/redox projections).

## Alpha.1 — evolved-felsic greisen proof

`GreisenSystemState` adds the first Phase 7 deposit-family expansion without inventing a new
Phase 2 assay or block inventory. A profile can form only when the resolved parent is the
province's youngest evolved felsic stock or granodiorite pulse, its magma lineage reports
`VERY_HIGH` residual-fluid potential, and the present surface preserves a low-relief contact
zone. The source ledger sums the existing magma residual-fluid inventory as a capped,
fixed-point `RESIDUAL_FELSIC_FLUID_PROXY`; it is explicitly not a tin/tungsten grade, tonnage, or
absolute fluid mass.

Formed profiles expose three contiguous, depth- and radius-bounded horizons: quartz-muscovite
greisen, a tourmaline proxy, and a kaolinitic margin. Released proxy units close exactly into
transport loss plus deposit allocation. Missing parent, residual-fluid, or preservation evidence
returns a barren system with a named failed gate and no intervals. No random climate, surface
outcrop, or catalog fallback can create a formed profile.

`OverworldGreisenPlanner` transforms each query through the owning province frame, clips the
horizons to realized solid terrain, enumerates the authorized target chunk in stable X-then-Z
order, and compares equal at adjacent chunk seams. The NeoForge `/geology greisen` command is a
read-only current-column view. The standalone `greisen` task writes
`atlas-cli/build/phase7/greisen/greisen.json`, including formed/barren counts, source/release/loss/
deposit closure, horizon counts, and seam stability.

## Exit evidence

`OverworldGreisenPlannerTest` covers the positive evolved-felsic/residual-fluid proof, the barren
gate, and adjacent-context seam equality. `GreisenPacketGeneratorTest` checks byte-for-byte
repeatability, explicit residual-fluid-proxy labeling, closed budgets, formed evidence, and seam
stability. The generated artifact is a review aid, not a save format or voxel-grade prediction.

## Alpha.2 — carbonate-contact skarn

`SkarnSystemState` adds a source-gated calc-silicate contact system over the existing youngest
arc intrusion and magmatic-hydrothermal evidence. A formed profile requires a reactive limestone,
dolostone, marble, or carbonatite host, a younger eligible arc pulse, very-high residual-fluid
evidence, contact/fracture permeability, and a preserved low-relief surface. Prograde
garnet/pyroxene and retrograde amphibole/epidote horizons carry a bounded Cu-Au or Fe
calc-silicate proxy allocation; the released fluid budget closes exactly into transport loss plus
deposit allocation and is not an assay or absolute tonnage.

The current synthetic atlas contains carbonate material definitions but does not generate a
carbonate host body. `SkarnHostPolicy.none()` therefore accepts only an actual resolved bedrock
carbonate and the default `/geology skarn` command cannot invent one. The review-only
`SkarnHostPolicy.fixture()` supplies a named deterministic limestone contact descriptor so the
positive reaction, ledger, interval, and seam paths remain testable without weakening the hard
invariant. Its `skarn` task writes
`atlas-cli/build/phase7/skarn/skarn.json`, labels the fixture policy, and records the default
actual-host gate alongside positive four-chunk evidence.

`OverworldSkarnPlanner` transforms contact coordinates through the province frame, clips the
horizons to realized solid terrain, and compares equal at adjacent chunk seams. Existing catalog
overprints (`CONTACT_HORNFELS` and `PROPYLITIC_ALTERATION`) carry the coarse response because a
new skarn overprint would require a Phase 2 catalog/schema increment.

`OverworldSkarnPlannerTest` covers fixture formation, host/intrusion/fluid gates, closed budgets,
actual-host-only barren behavior, and seam equality. `SkarnPacketGeneratorTest` checks deterministic
JSON, explicit fixture labeling, default negative proof, formed horizons, budget closure, and seam
stability.

## Alpha.3 — shallow epithermal fluid paths

`EpithermalSystemState` derives a bounded Au-Ag proxy from the existing porphyry fluid-phase
state. A profile requires a porphyry-capable province, a resolved receptive volcanic or country-rock
host, a magmatic brine/vapor or mixed meteoric pulse inside the existing province-frame envelope,
a fault/stockwork path, a shallow boiling/cooling/mixing trap, and preserved low-relief ground.
The pulse phase classifies high-, intermediate-, or low-sulfidation behavior; three contiguous
silica/argillic/propylitic horizons carry a fixed-point fluid ledger that closes release into loss
plus deposit allocation. The values are explanatory proxies, not Au-Ag assays or tonnage.

`OverworldEpithermalPlanner` clips the horizons to solid terrain and preserves the same stable
X-then-Z target-chunk order and adjacent-chunk seam equality used by earlier overlays. The
read-only `/geology epithermal` command and `epithermal` task expose the state. The task writes
`atlas-cli/build/phase7/epithermal/epithermal.json` with gate-class counts, formed profiles,
horizon counts, budget closure, and seam stability. Existing `PHYLLIC_ALTERATION` and
`PROPYLITIC_ALTERATION` overprints carry the coarse response until a future Phase 2 alteration
catalog increment adds explicit silica/argillic vocabulary.

`OverworldEpithermalPlannerTest` covers formed shallow-fluid behavior, barren gate retention,
closed budgets, and seam equality. `EpithermalPacketGeneratorTest` checks byte-repeatable JSON,
explicit proxy labeling, formed horizons, budget closure, and seam stability.

## Alpha.4 — metamorphic-fluid orogenic gold

`OrogenicGoldSystemState` is tied to the existing regional metamorphic fold field and inherited
finite fault rather than a generic quartz vein. A formed profile requires a non-NONE regional
collision path with a metamorphic grade/depth class, a receptive metasedimentary or volcanic host,
the fault damage zone as a connected crustal structure, a competency/strain-supported dilational
or reactive trap, and preserved low-relief ground. The source is an explicitly named bounded
`METAMORPHIC_AQUEOUS_CARBONIC_PROXY`, with grade- and strain-conditioned fixed-point fluid units;
it is not a measured fluid mass, Au grade, or tonnage. The fold event supplies formation timing and
the fault/host/driver IDs are retained in the source set.

Three contiguous quartz-carbonate vein, laminated shear-vein, and sulfidation-carbonate-halo
horizons are clipped to solid terrain by `OverworldOrogenicGoldPlanner`. The planner keeps the
stable X-then-Z target-chunk order and adjacent-chunk seam equality. The read-only
`/geology orogenic-gold` command and `orogenicGold` task expose the state; the task writes
`atlas-cli/build/phase7/orogenic-gold/orogenic-gold.json` with depth, fluid, host, structure,
trap, preservation, horizon, ledger, and seam evidence. Existing phyllic/propylitic overprints
carry the coarse alteration response until a future Phase 2 catalog increment adds explicit
quartz-carbonate/sulfide vocabulary.

`OverworldOrogenicGoldPlannerTest` covers formed deformation/fluid behavior, barren gate retention,
closed budgets, and seam equality. `OrogenicGoldPacketGeneratorTest` checks byte-repeatable JSON,
explicit metamorphic-fluid proxy labeling, formed horizons, budget closure, and seam stability.

## Alpha.5 — basin/redox hydrothermal families

`BasinHydrothermalSystemState` adds three separate basin-fluid proofs: Mississippi Valley-type
Pb-Zn replacement, SEDEX Zn-Pb-Ag exhalative horizons, and sediment-hosted copper redox
replacement. Each family requires typed basin facies, saline basin-brine potential, a connected
aquifer/fault or permeability path, a family-specific reaction or redox trap, and preserved basin
relief. The finite brine ledger closes released fluid into transport loss plus deposit allocation;
these are source-budgeted proxies rather than Pb-Zn-Ag-Cu assays, grades, or tonnages.

The default `BasinHydrothermalHostPolicy.none()` resolves actual bedrock only. Because the current
synthetic atlas does not generate a carbonate bedrock body, the review-only deterministic
`BasinHydrothermalHostPolicy.fixture()` supplies a named dolostone host so the MVT reaction path
is testable without weakening the default host gate. Three family-specific horizons are emitted
(`DOLOMITE_REPLACEMENT`/`BRECCIA_OPEN_SPACE_FILL`/`SULFIDE_GOSSAN_HALO`,
`LAMINATED_SULFIDE`/`BARITE_SILICA_EXHALITE`/`FEEDER_ALTERATION`, or
`RED_BED_DISSEMINATION`/`REDOX_REPLACEMENT`/`OXIDIZED_CU_HALO`). Existing phyllic and
propylitic overprints remain coarse response proxies until a future Phase 2 catalog increment.

`OverworldBasinHydrothermalPlanner` clips all horizons to realized solid terrain, retains stable
X-then-Z target-chunk ordering, and checks adjacent-chunk seam equality. The read-only
`/geology basin-hydrothermal` command and `basinHydrothermal` task expose both actual-host and
fixture evidence. The task writes
`atlas-cli/build/phase7/basin-hydrothermal/basin-hydrothermal.json` with family, facies, brine,
pathway, trap, preservation, horizon, ledger, default-negative, and seam evidence.

`OverworldBasinHydrothermalPlannerTest` covers MVT fixture formation, actual-host-only behavior,
barren gate retention, closed ledgers, and seam equality. `BasinHydrothermalPacketGeneratorTest`
checks byte-repeatable JSON, explicit redox-proxy labeling, family/horizon evidence, budget
closure, the default negative proof, and seam stability.

Uranium, layered-intrusion, carbonatite/REE,
phosphorite/coal/brine, and geothermal families remain separate future Phase 7 slices; they must
not inherit these basin proxies or be inferred from a generic catalog lithology.
