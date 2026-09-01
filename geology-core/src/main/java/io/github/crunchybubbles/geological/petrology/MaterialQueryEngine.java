package io.github.crunchybubbles.geological.petrology;

import io.github.crunchybubbles.geological.atlas.BoundedDescriptorCache;
import io.github.crunchybubbles.geological.atlas.DescriptorCache;
import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.mineral.DepositType;
import io.github.crunchybubbles.geological.mineral.MineralSystemDecision;
import io.github.crunchybubbles.geological.model.EventType;
import io.github.crunchybubbles.geological.model.GeologicalEvent;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.query.ColumnQueryResult;
import io.github.crunchybubbles.geological.query.ColumnRequest;
import io.github.crunchybubbles.geological.query.GeologicalSample;
import io.github.crunchybubbles.geological.query.GeologyQueryEngine;
import io.github.crunchybubbles.geological.query.MaterialRun;
import io.github.crunchybubbles.geological.query.MaterialState;
import io.github.crunchybubbles.geological.surface.SurfaceSample;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Phase 2 facade deriving mineralogy and process state from immutable Phase 1 geology. */
public final class MaterialQueryEngine {
  private final GeologyQueryEngine geology;
  private final MaterialCatalogSnapshot catalog;
  private final Map<RecipeKey, RecipeTemplate> recipeTemplates;
  private final BodyCompositionSampler compositionSampler;
  private final DescriptorCache<BodyRecipeKey, ResolvedRecipe> bodyRecipeCache;
  private final DescriptorCache<StableId, List<ElementReservoirLedger>> reservoirLedgerCache;

  public MaterialQueryEngine(GeologyQueryEngine geology, MaterialCatalogSnapshot catalog) {
    if (geology == null || catalog == null) {
      throw new IllegalArgumentException("geology query and material catalog are required");
    }
    this.geology = geology;
    this.catalog = catalog;
    this.recipeTemplates = compileRecipeTemplates(catalog);
    this.compositionSampler = new BodyCompositionSampler(geology.atlas().identity());
    this.bodyRecipeCache = new BoundedDescriptorCache<>(512);
    this.reservoirLedgerCache = new BoundedDescriptorCache<>(256);
  }

  public GeologyQueryEngine geology() {
    return geology;
  }

  public MaterialCatalogSnapshot catalog() {
    return catalog;
  }

  public int resolvedRecipeCount() {
    return recipeTemplates.size();
  }

  public int bodyRecipeCacheSize() {
    return bodyRecipeCache.size();
  }

  public List<ElementReservoirLedger> elementReservoirLedgers(Province province) {
    return reservoirLedgerCache.get(province.id(), ignored -> compileReservoirLedgers(province));
  }

  public void clearCaches() {
    geology.clearCaches();
    bodyRecipeCache.clear();
    reservoirLedgerCache.clear();
  }

  public PetrologicSample sample(Point3 worldPoint) {
    Province province = geology.atlas().provinceAt(new Point2(worldPoint.x(), worldPoint.z()));
    GeologicalSample geological = geology.sample(province, worldPoint);
    return resolve(province, geological);
  }

  public PetrologicColumnResult column(ColumnRequest request) {
    ColumnQueryResult geologicalColumn = geology.column(request);
    Province province = geology.atlas().provinceAt(request.horizontalPoint());
    if (!province.id().equals(geologicalColumn.provinceId())) {
      throw new IllegalStateException("column owner changed between geological query stages");
    }
    List<PetrologicRun> runs = new ArrayList<>();
    for (MaterialRun geologicalRun : geologicalColumn.runs()) {
      GeologicalSample representative =
          geologicalSample(
              province,
              new Point3(request.x(), geologicalRun.minYInclusive() + 0.5, request.z()),
              geologicalRun.state());
      PetrologicState state = PetrologicState.from(resolve(province, representative));
      if (!runs.isEmpty() && runs.getLast().state().equals(state)) {
        PetrologicRun previous = runs.removeLast();
        runs.add(new PetrologicRun(previous.minYInclusive(), geologicalRun.maxYExclusive(), state));
      } else {
        runs.add(
            new PetrologicRun(geologicalRun.minYInclusive(), geologicalRun.maxYExclusive(), state));
      }
    }
    return new PetrologicColumnResult(geologicalColumn, runs, geologicalColumn.runs().size());
  }

  public SurfacePetrologicSample surface(Point2 worldPoint) {
    Province province = geology.atlas().provinceAt(worldPoint);
    SurfaceSample surface = geology.surface(worldPoint);
    GeologicalSample bedrock = surface.bedrock();
    if (!province.id().equals(bedrock.provinceId())) {
      throw new IllegalStateException("surface owner changed between geological query stages");
    }

    GeologicalSample surfaceGeology;
    SurfaceMaterialContext context;
    if (surface.fields().drainage().sourceLinkedPlacer()) {
      MineralSystemDecision placer = formedPlacer(province);
      long trapped = placer.ledger().allocations().getOrDefault("placer_trap", 0L);
      surfaceGeology =
          new GeologicalSample(
              bedrock.point(),
              bedrock.macroDomainId(),
              bedrock.provinceId(),
              placer.deposit().id(),
              surface.surfaceMaterial(),
              placer.deposit().formationAge(),
              surface.surfaceOverprint(),
              bedrock.faultDamageZone(),
              List.of(placer.deposit().id()));
      context =
          new SurfaceMaterialContext(
              SurfaceMaterialKind.ALLUVIAL_PLACER,
              placer.deposit().id(),
              placer.deposit().sourceIds(),
              Optional.of(placer.deposit().id()),
              Optional.of(placer.ledger().element()),
              Optional.of(placer.ledger().unit()),
              placer.ledger().sourceAmount(),
              trapped);
    } else {
      SurfaceMaterialKind kind =
          surface.fields().outcrop()
              ? SurfaceMaterialKind.BEDROCK_OUTCROP
              : SurfaceMaterialKind.IN_SITU_REGOLITH;
      surfaceGeology =
          new GeologicalSample(
              bedrock.point(),
              bedrock.macroDomainId(),
              bedrock.provinceId(),
              bedrock.rockBodyId(),
              surface.surfaceMaterial(),
              bedrock.formationAge(),
              surface.surfaceOverprint(),
              bedrock.faultDamageZone(),
              bedrock.depositIds());
      context =
          new SurfaceMaterialContext(
              kind,
              bedrock.rockBodyId(),
              List.of(bedrock.rockBodyId()),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              0,
              0);
    }
    return new SurfacePetrologicSample(surface, resolve(province, surfaceGeology), context);
  }

  public PetrologicSample resolve(Province province, GeologicalSample geological) {
    if (!province.id().equals(geological.provinceId())) {
      throw new IllegalArgumentException("sample does not belong to the supplied province");
    }
    BodyRecipeKey key =
        new BodyRecipeKey(geological.rockBodyId(), geological.lithology(), geological.overprint());
    ResolvedRecipe recipe = bodyRecipeCache.get(key, this::compileBodyRecipe);
    RockDefinition rock = recipe.rock();
    AlterationDefinition alteration = catalog.requireAlteration(geological.overprint());

    return new PetrologicSample(
        geological,
        rock,
        recipe.primaryAssemblage(),
        recipe.resolvedAssemblage(),
        recipe.primarySolidSolutions(),
        recipe.resolvedSolidSolutions(),
        recipe.primaryComposition(),
        recipe.resolvedComposition(),
        recipe.elementLedger(),
        materialProcessLedger(province, geological, alteration, recipe.elementLedger()),
        metamorphicHistory(province, rock, alteration),
        alteration.processClass(),
        alteration.fluidState(),
        recipe.porosityFraction(),
        recipe.permeabilityIndex(),
        recipe.erodibilityIndex(),
        magmaLineage(province, geological),
        sedimentaryState(province, rock),
        ledgersForSample(province, geological));
  }

  private static MaterialProcessLedger materialProcessLedger(
      Province province,
      GeologicalSample geological,
      AlterationDefinition alteration,
      ElementTransferLedger elementLedger) {
    Optional<StableId> processId =
        switch (geological.overprint()) {
          case NONE -> Optional.empty();
          case CONTACT_HORNFELS -> Optional.of(province.geometry().aureoleId());
          case POTASSIC_ALTERATION, PHYLLIC_ALTERATION, PROPYLITIC_ALTERATION ->
              Optional.of(province.proofIds().porphyrySystemId());
          case CHLORITIC_FOOTWALL -> Optional.of(province.proofIds().vmsSystemId());
          case WEATHERED_UNCONFORMITY, OXIDIZED_GOSSAN, WEATHERED_REGOLITH ->
              Optional.of(province.proofIds().weatheringId());
        };
    return MaterialProcessLedger.from(
        processId,
        alteration.processClass(),
        processEvents(province, alteration.processClass()),
        elementLedger);
  }

  private static MetamorphicHistory metamorphicHistory(
      Province province, RockDefinition rock, AlterationDefinition alteration) {
    if (alteration.facies() != MetamorphicFacies.NONE) {
      return new MetamorphicHistory(
          rock.id(),
          alteration.facies(),
          alteration.path(),
          alteration.minimumTemperatureCelsius(),
          alteration.maximumTemperatureCelsius(),
          alteration.minimumPressureMpa(),
          alteration.maximumPressureMpa(),
          events(province, EventType.CONTACT_METAMORPHISM));
    }
    if (rock.lithology() == Lithology.GRANITIC_GNEISS) {
      return new MetamorphicHistory(
          rock.id(),
          MetamorphicFacies.AMPHIBOLITE,
          MetamorphicPath.COLLISION_CLOCKWISE,
          600.0,
          750.0,
          400.0,
          800.0,
          events(province, EventType.ESTABLISH_BASEMENT));
    }
    return new MetamorphicHistory(
        rock.id(),
        MetamorphicFacies.NONE,
        MetamorphicPath.NONE,
        alteration.minimumTemperatureCelsius(),
        alteration.maximumTemperatureCelsius(),
        alteration.minimumPressureMpa(),
        alteration.maximumPressureMpa(),
        processEvents(province, alteration.processClass()));
  }

  private static Optional<MagmaLineageState> magmaLineage(
      Province province, GeologicalSample sample) {
    List<RiftArcGeometry.PlutonPulse> pulses = province.geometry().plutonPulses();
    for (int index = 0; index < pulses.size(); index++) {
      RiftArcGeometry.PlutonPulse pulse = pulses.get(index);
      if (pulse.id().equals(sample.rockBodyId())) {
        double progress =
            switch (index) {
              case 0 -> 0.25;
              case 1 -> 0.55;
              default -> 0.85;
            };
        String fluidPotential =
            switch (index) {
              case 0 -> "moderate";
              case 1 -> "high";
              default -> "very_high";
            };
        return Optional.of(
            new MagmaLineageState(
                province.proofIds().magmaLineageId(),
                pulse.id(),
                index,
                progress,
                "hydrated_mantle_wedge_plus_lower_crust",
                "water_rich",
                "oxidized",
                fluidPotential));
      }
    }
    return Optional.empty();
  }

  private static Optional<SedimentaryState> sedimentaryState(
      Province province, RockDefinition rock) {
    if (rock.geneticFamily() != GeneticFamily.SEDIMENTARY) {
      return Optional.empty();
    }
    List<StableId> sources = new ArrayList<>();
    sources.add(province.geometry().basementId());
    if (rock.lithology() == Lithology.MARINE_VOLCANICLASTIC) {
      sources.add(province.proofIds().magmaLineageId());
    }
    return Optional.of(
        switch (rock.lithology()) {
          case BASAL_CONGLOMERATE ->
              new SedimentaryState(
                  "rift_margin_alluvial_fan",
                  "gravel",
                  "compositionally_immature",
                  "compacted_and_locally_cemented",
                  sources);
          case MARINE_VOLCANICLASTIC ->
              new SedimentaryState(
                  "submarine_volcanic_apron",
                  "sand_to_tuff",
                  "volcanogenic_immature",
                  "burial_chlorite_calcite",
                  sources);
          case BASIN_SHALE ->
              new SedimentaryState(
                  "offshore_low_energy",
                  "mud",
                  "clay_rich_mature",
                  "compacted_low_permeability",
                  sources);
          case BASIN_SANDSTONE ->
              new SedimentaryState(
                  "shallow_marine_shoreface",
                  "sand",
                  "submature",
                  "quartz_calcite_cemented",
                  sources);
          case SILTSTONE ->
              new SedimentaryState(
                  "delta_front_to_offshore_transition",
                  "silt",
                  "submature_micaceous",
                  "compacted_clay_matrix_and_calcite_cement",
                  sources);
          case LIMESTONE ->
              new SedimentaryState(
                  "carbonate_platform",
                  "carbonate_mud_to_sand",
                  "biochemical_chemical",
                  "calcite_lithified",
                  sources);
          case DOLOSTONE ->
              new SedimentaryState(
                  "dolomitized_carbonate_platform",
                  "carbonate_crystalline",
                  "replacement_modified",
                  "magnesium_replacement_and_recrystallization",
                  sources);
          case CHERT ->
              new SedimentaryState(
                  "marine_bedded_silica",
                  "microcrystalline",
                  "silica_rich",
                  "silica_precipitation_and_recrystallization",
                  sources);
          case BANDED_IRON_FORMATION ->
              new SedimentaryState(
                  "ancient_iron_silica_precipitation_basin",
                  "microcrystalline_banded",
                  "chemical_precipitate_redox_controlled",
                  "iron_oxide_carbonate_silica_recrystallization",
                  sources);
          case GYPSUM_ANHYDRITE_EVAPORITE ->
              new SedimentaryState(
                  "restricted_evaporite_margin",
                  "crystalline_sulfate",
                  "chemical_precipitate",
                  "gypsum_anhydrite_hydration_recrystallization",
                  sources);
          case HALITE_POTASH_EVAPORITE ->
              new SedimentaryState(
                  "restricted_evaporite_basin_center",
                  "crystalline_salt",
                  "late_stage_brine_precipitate",
                  "salt_recrystallization_dissolution_and_halokinesis",
                  sources);
          default ->
              throw new IllegalStateException("unmapped sedimentary lithology " + rock.lithology());
        });
  }

  private static List<StableId> processEvents(
      Province province, MaterialProcessClass processClass) {
    return switch (processClass) {
      case NONE -> List.of();
      case ISOCHEMICAL_METAMORPHISM -> events(province, EventType.CONTACT_METAMORPHISM);
      case HYDROTHERMAL_METASOMATISM -> events(province, EventType.MINERALIZE);
      case WEATHERING -> events(province, EventType.WEATHER);
    };
  }

  private static List<StableId> events(Province province, EventType type) {
    return province.chronicle().events().stream()
        .filter(event -> event.type() == type)
        .map(GeologicalEvent::id)
        .sorted()
        .toList();
  }

  private static Map<RecipeKey, RecipeTemplate> compileRecipeTemplates(
      MaterialCatalogSnapshot catalog) {
    Map<RecipeKey, RecipeTemplate> result = new HashMap<>();
    for (Lithology lithology : Lithology.values()) {
      RockDefinition rock = catalog.requireRock(lithology);
      for (Overprint overprint : Overprint.values()) {
        AlterationDefinition alteration = catalog.requireAlteration(overprint);
        if (result.put(new RecipeKey(lithology, overprint), new RecipeTemplate(rock, alteration))
            != null) {
          throw new IllegalStateException("duplicate material recipe");
        }
      }
    }
    return Map.copyOf(result);
  }

  private ResolvedRecipe compileBodyRecipe(BodyRecipeKey key) {
    RecipeTemplate template = recipeTemplates.get(new RecipeKey(key.lithology(), key.overprint()));
    if (template == null) {
      throw new IllegalStateException("material recipe matrix is incomplete");
    }
    RockDefinition rock = template.rock();
    AlterationDefinition alteration = template.alteration();
    MineralAssemblage primary = compositionSampler.sample(rock, key.bodyId());
    MineralAssemblage resolved =
        alteration.replacementPpm() == 0
            ? primary
            : MineralAssemblage.blend(
                primary,
                alteration.targetAssemblage(rock.geneticFamily()),
                alteration.replacementPpm());
    List<SolidSolutionState> primarySolidSolutions = catalog.solidSolutionStates(primary);
    List<SolidSolutionState> resolvedSolidSolutions = catalog.solidSolutionStates(resolved);
    BulkComposition primaryComposition = catalog.composition(primary);
    BulkComposition resolvedComposition = catalog.composition(resolved);
    ElementTransferLedger ledger =
        ElementTransferLedger.between(primaryComposition, resolvedComposition);
    if (alteration.processClass() == MaterialProcessClass.ISOCHEMICAL_METAMORPHISM
        && !ledger.isIsochemical()) {
      throw new IllegalStateException("isochemical response changed bulk composition");
    }
    return new ResolvedRecipe(
        rock,
        primary,
        resolved,
        primarySolidSolutions,
        resolvedSolidSolutions,
        primaryComposition,
        resolvedComposition,
        ledger,
        clamp(
            compositionSampler.sample(
                    rock.porosityDistribution(), key.bodyId(), "porosity_fraction")
                * alteration.porosityMultiplier()),
        clamp(
            compositionSampler.sample(
                    rock.permeabilityDistribution(), key.bodyId(), "permeability_index")
                * StrictMath.sqrt(StrictMath.max(0.0, alteration.porosityMultiplier()))),
        clamp(
            compositionSampler.sample(
                    rock.erodibilityDistribution(), key.bodyId(), "erodibility_index")
                + alteration.erodibilityDelta()));
  }

  private List<ElementReservoirLedger> ledgersForSample(
      Province province, GeologicalSample geological) {
    if (geological.depositIds().isEmpty()) {
      return List.of();
    }
    Set<StableId> deposits = Set.copyOf(geological.depositIds());
    return elementReservoirLedgers(province).stream()
        .filter(ledger -> ledger.depositId().filter(deposits::contains).isPresent())
        .toList();
  }

  private List<ElementReservoirLedger> compileReservoirLedgers(Province province) {
    return geology.mineralDecisions(province).stream()
        .filter(decision -> decision.deposit() != null && decision.ledger() != null)
        .map(MaterialQueryEngine::reservoirLedger)
        .sorted(java.util.Comparator.comparing(ElementReservoirLedger::systemId))
        .toList();
  }

  private static ElementReservoirLedger reservoirLedger(MineralSystemDecision decision) {
    StableId source = decision.deposit().sourceIds().getFirst();
    List<ReservoirTransfer> transfers =
        decision.ledger().allocations().entrySet().stream()
            .map(
                allocation -> {
                  String role = allocation.getKey();
                  if (role.equals("deposit") || role.equals("placer_trap")) {
                    return new ReservoirTransfer(
                        role,
                        ReservoirSinkKind.DEPOSIT,
                        Optional.of(decision.deposit().id()),
                        allocation.getValue());
                  }
                  if (role.startsWith("retained_")) {
                    return new ReservoirTransfer(
                        role,
                        ReservoirSinkKind.RETAINED_SOURCE,
                        Optional.of(source),
                        allocation.getValue());
                  }
                  ReservoirSinkKind kind =
                      role.contains("transport")
                          ? ReservoirSinkKind.TRANSPORT_LOSS
                          : ReservoirSinkKind.DIFFUSE_HALO_OR_LOSS;
                  return new ReservoirTransfer(role, kind, Optional.empty(), allocation.getValue());
                })
            .toList();
    return new ElementReservoirLedger(
        decision.candidateId(),
        source,
        Optional.of(decision.deposit().id()),
        decision.ledger().element(),
        decision.ledger().unit(),
        decision.ledger().sourceAmount(),
        transfers);
  }

  private MineralSystemDecision formedPlacer(Province province) {
    return geology.mineralDecisions(province).stream()
        .filter(decision -> decision.deposit() != null)
        .filter(decision -> decision.deposit().type() == DepositType.ALLUVIAL_PLACER_AU)
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "surface field selected placer material without a formed placer decision"));
  }

  private static GeologicalSample geologicalSample(
      Province province, Point3 point, MaterialState state) {
    return new GeologicalSample(
        point,
        province.macroDomainId(),
        province.id(),
        state.rockBodyId(),
        state.lithology(),
        state.formationAge(),
        state.overprint(),
        state.faultDamageZone(),
        state.depositIds());
  }

  private static double clamp(double value) {
    return StrictMath.max(0.0, StrictMath.min(1.0, value));
  }

  private record RecipeKey(Lithology lithology, Overprint overprint) {}

  private record RecipeTemplate(RockDefinition rock, AlterationDefinition alteration) {}

  private record BodyRecipeKey(StableId bodyId, Lithology lithology, Overprint overprint) {}

  private record ResolvedRecipe(
      RockDefinition rock,
      MineralAssemblage primaryAssemblage,
      MineralAssemblage resolvedAssemblage,
      List<SolidSolutionState> primarySolidSolutions,
      List<SolidSolutionState> resolvedSolidSolutions,
      BulkComposition primaryComposition,
      BulkComposition resolvedComposition,
      ElementTransferLedger elementLedger,
      double porosityFraction,
      double permeabilityIndex,
      double erodibilityIndex) {}
}
