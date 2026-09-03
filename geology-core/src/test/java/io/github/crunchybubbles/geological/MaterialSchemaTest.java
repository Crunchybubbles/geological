package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Lithology;
import io.github.crunchybubbles.geological.model.Overprint;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.petrology.AcidityClass;
import io.github.crunchybubbles.geological.petrology.AlterationAssemblageRecipe;
import io.github.crunchybubbles.geological.petrology.AlterationDefinition;
import io.github.crunchybubbles.geological.petrology.BodyCompositionSampler;
import io.github.crunchybubbles.geological.petrology.BulkComposition;
import io.github.crunchybubbles.geological.petrology.ChemicalElement;
import io.github.crunchybubbles.geological.petrology.ClastShape;
import io.github.crunchybubbles.geological.petrology.ColluvialAbsoluteMassBudget;
import io.github.crunchybubbles.geological.petrology.ColluvialCohesionState;
import io.github.crunchybubbles.geological.petrology.ColluvialGrainDispersionState;
import io.github.crunchybubbles.geological.petrology.ColluvialGrainSourceShare;
import io.github.crunchybubbles.geological.petrology.ColluvialHorizonState;
import io.github.crunchybubbles.geological.petrology.ColluvialHydraulicState;
import io.github.crunchybubbles.geological.petrology.ColluvialMassScale;
import io.github.crunchybubbles.geological.petrology.ColluvialPhysicalState;
import io.github.crunchybubbles.geological.petrology.ColluvialProductionState;
import io.github.crunchybubbles.geological.petrology.ColluvialRoutePolicy;
import io.github.crunchybubbles.geological.petrology.ColluvialSedimentBudget;
import io.github.crunchybubbles.geological.petrology.ColluvialSinkAllocation;
import io.github.crunchybubbles.geological.petrology.ColluvialSinkDestination;
import io.github.crunchybubbles.geological.petrology.ColluvialSinkState;
import io.github.crunchybubbles.geological.petrology.ColluvialSourceCapacityLedger;
import io.github.crunchybubbles.geological.petrology.ColluvialSourceClaim;
import io.github.crunchybubbles.geological.petrology.ColluvialSourceClaimLedger;
import io.github.crunchybubbles.geological.petrology.ColluvialSourceGrainShare;
import io.github.crunchybubbles.geological.petrology.ColluvialSourceUsage;
import io.github.crunchybubbles.geological.petrology.ColluvialTextureState;
import io.github.crunchybubbles.geological.petrology.ColluvialTransportPolicy;
import io.github.crunchybubbles.geological.petrology.ColluvialTransportProcess;
import io.github.crunchybubbles.geological.petrology.ColluvialTransportProcessMix;
import io.github.crunchybubbles.geological.petrology.ColluvialTransportProcessStageMix;
import io.github.crunchybubbles.geological.petrology.ColluvialTransportProcessUsage;
import io.github.crunchybubbles.geological.petrology.FluidMedium;
import io.github.crunchybubbles.geological.petrology.FluidTransportState;
import io.github.crunchybubbles.geological.petrology.FractureTensorState;
import io.github.crunchybubbles.geological.petrology.GeneticFamily;
import io.github.crunchybubbles.geological.petrology.LigandCapacities;
import io.github.crunchybubbles.geological.petrology.MagmaDifferentiationState;
import io.github.crunchybubbles.geological.petrology.MaterialAssemblage;
import io.github.crunchybubbles.geological.petrology.MaterialBufferState;
import io.github.crunchybubbles.geological.petrology.MaterialProcessClass;
import io.github.crunchybubbles.geological.petrology.MetamorphicEventTiming;
import io.github.crunchybubbles.geological.petrology.MetamorphicFacies;
import io.github.crunchybubbles.geological.petrology.MetamorphicFluidContribution;
import io.github.crunchybubbles.geological.petrology.MetamorphicGrade;
import io.github.crunchybubbles.geological.petrology.MetamorphicHistory;
import io.github.crunchybubbles.geological.petrology.MetamorphicPath;
import io.github.crunchybubbles.geological.petrology.MetamorphicProcessState;
import io.github.crunchybubbles.geological.petrology.MetamorphicReactionState;
import io.github.crunchybubbles.geological.petrology.MetamorphicStrainState;
import io.github.crunchybubbles.geological.petrology.ModalVariationAxis;
import io.github.crunchybubbles.geological.petrology.PrimaryMetamorphicDefinition;
import io.github.crunchybubbles.geological.petrology.ProcessFluidState;
import io.github.crunchybubbles.geological.petrology.RedoxClass;
import io.github.crunchybubbles.geological.petrology.ReservoirSinkKind;
import io.github.crunchybubbles.geological.petrology.ReservoirTransfer;
import io.github.crunchybubbles.geological.petrology.RockDefinition;
import io.github.crunchybubbles.geological.petrology.RockTexture;
import io.github.crunchybubbles.geological.petrology.SalinityClass;
import io.github.crunchybubbles.geological.petrology.SedimentGrainSize;
import io.github.crunchybubbles.geological.petrology.SedimentSorting;
import io.github.crunchybubbles.geological.petrology.SedimentSupport;
import io.github.crunchybubbles.geological.petrology.SedimentaryBasinState;
import io.github.crunchybubbles.geological.petrology.SedimentaryDiagenesisState;
import io.github.crunchybubbles.geological.petrology.SedimentaryInputBudget;
import io.github.crunchybubbles.geological.petrology.SedimentaryReservoirContribution;
import io.github.crunchybubbles.geological.petrology.SedimentaryReservoirKind;
import io.github.crunchybubbles.geological.petrology.SedimentaryReservoirState;
import io.github.crunchybubbles.geological.petrology.SedimentaryState;
import io.github.crunchybubbles.geological.petrology.SulfurState;
import io.github.crunchybubbles.geological.petrology.TraceElementVector;
import io.github.crunchybubbles.geological.petrology.UnitIntervalDistribution;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MaterialSchemaTest {
  @Test
  void reservoirTransferCarriesOptionalAgeProcessAndConfidenceEvidence() {
    StableId process = StableId.parse("00000000000000000000000000000011");
    StableId sink = StableId.parse("00000000000000000000000000000012");
    AgeKey age = new AgeKey(92.0, 0);
    ReservoirTransfer transfer =
        new ReservoirTransfer(
            "deposit",
            ReservoirSinkKind.DEPOSIT,
            Optional.of(sink),
            105_000L,
            Optional.of(process),
            Optional.of(age),
            950_000L);

    assertEquals(Optional.of(process), transfer.processId());
    assertEquals(Optional.of(age), transfer.age());
    assertEquals(950_000L, transfer.confidencePpm());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ReservoirTransfer(
                "deposit",
                ReservoirSinkKind.DEPOSIT,
                Optional.of(sink),
                105_000L,
                Optional.of(process),
                Optional.empty(),
                950_000L));
  }

  @Test
  void sedimentaryInputBudgetClosesAcrossExplicitSourceReservoirClasses() {
    StableId basement = StableId.parse("00000000000000000000000000000001");
    StableId volcanic = StableId.parse("00000000000000000000000000000002");
    SedimentaryInputBudget evaporite =
        SedimentaryInputBudget.proofFor("restricted_evaporite_basin_center");
    SedimentaryInputBudget coal = SedimentaryInputBudget.proofFor("buried_peat_mire");
    List<SedimentaryReservoirContribution> volcanicMix =
        SedimentaryReservoirContribution.proofFor(
            SedimentaryInputBudget.proofFor("submarine_volcanic_apron"),
            List.of(volcanic, basement));

    assertEquals(MaterialAssemblage.SCALE, total(evaporite));
    assertEquals(MaterialAssemblage.SCALE, total(coal));
    assertTrue(evaporite.evaporiticBrinePpm() > 0);
    assertTrue(coal.organicPpm() > 0);
    assertEquals(
        List.of(
            SedimentaryReservoirKind.CLASTIC_TERRIGENOUS,
            SedimentaryReservoirKind.VOLCANIC_ASH,
            SedimentaryReservoirKind.CARBONATE_BIOGENIC,
            SedimentaryReservoirKind.CHEMICAL_PRECIPITATE),
        volcanicMix.stream().map(SedimentaryReservoirContribution::kind).toList());
    assertEquals(List.of(basement, volcanic), volcanicMix.get(0).sourceBodyIds());
    assertEquals(List.of(basement, volcanic), volcanicMix.get(1).sourceBodyIds());
    assertEquals(0, volcanicMix.get(2).sourceBodyIds().size());
    assertEquals(
        MaterialAssemblage.SCALE,
        volcanicMix.stream().mapToLong(SedimentaryReservoirContribution::fractionPpm).sum());
    assertThrows(
        IllegalArgumentException.class,
        () -> new SedimentaryInputBudget(1_000_000L, 1L, 0L, 0L, 0L, 0L));
  }

  @Test
  void sedimentaryReservoirStateAddsTypedChemistryAndInventoryEvidence() {
    StableId source = StableId.parse("00000000000000000000000000000021");
    SedimentaryState peat =
        new SedimentaryState("buried_peat_mire", "fine", "immature", "compacted", List.of(source));
    SedimentaryState evaporite =
        new SedimentaryState(
            "restricted_evaporite_basin_center",
            "fine",
            "mature",
            "salt_recrystallized",
            List.of(source));
    SedimentaryReservoirState peatState = peat.reservoirState();
    SedimentaryReservoirState evaporiteState = evaporite.reservoirState();

    assertEquals(
        MaterialAssemblage.SCALE,
        peatState.aggregateCompositionPpm().values().stream().mapToLong(Long::longValue).sum());
    assertTrue(peatState.organicCarbonCapacityPpm() > 0L);
    assertTrue(peatState.reducedSulfurCapacityPpm() > 0L);
    assertTrue(evaporiteState.waterInventoryPpm() > peatState.waterInventoryPpm());
    assertTrue(
        evaporiteState.components().stream()
            .anyMatch(component -> component.kind() == SedimentaryReservoirKind.EVAPORITIC_BRINE));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            SedimentaryReservoirState.proofFor(
                SedimentaryInputBudget.proofFor("buried_peat_mire"),
                peat.basinState(),
                List.of(
                    new SedimentaryReservoirContribution(
                        SedimentaryReservoirKind.CHEMICAL_PRECIPITATE,
                        MaterialAssemblage.SCALE,
                        List.of()))));
  }

  @Test
  void fluidTransportStateDerivesBoundedTemperaturePressureAndPhaseAxes() {
    FluidTransportState magmatic = FluidTransportState.proofFor(fluidState());
    FluidTransportState meteoric =
        FluidTransportState.proofFor(
            new ProcessFluidState(
                FluidMedium.METEORIC_WATER,
                RedoxClass.OXIDIZING,
                AcidityClass.NEAR_NEUTRAL,
                SalinityClass.FRESH,
                SulfurState.DEPLETED,
                new LigandCapacities(0, 0, 0, 0),
                0));

    assertEquals(FluidTransportState.TemperatureClass.HOT, magmatic.temperatureClass());
    assertEquals(FluidTransportState.PressureClass.HIGH, magmatic.pressureClass());
    assertEquals(FluidTransportState.WaterRockRatioClass.HIGH, magmatic.waterRockRatioClass());
    assertEquals(FluidTransportState.PhaseBehaviorClass.SEPARATION, magmatic.phaseBehaviorClass());
    assertEquals(FluidTransportState.TemperatureClass.COOL, meteoric.temperatureClass());
    assertEquals(FluidTransportState.WaterRockRatioClass.VERY_HIGH, meteoric.waterRockRatioClass());
    assertEquals(FluidTransportState.PhaseBehaviorClass.MIXING, meteoric.phaseBehaviorClass());
    assertThrows(IllegalArgumentException.class, () -> FluidTransportState.proofFor(null));
  }

  @Test
  void sedimentaryDiagenesisStateCarriesBoundedFaciesAndFluidEvidence() {
    StableId source = StableId.parse("00000000000000000000000000000001");
    SedimentaryBasinState basin =
        SedimentaryBasinState.proofFor("dolomitized_carbonate_platform", List.of(source));
    SedimentaryDiagenesisState state =
        SedimentaryDiagenesisState.proofFor("dolomitized_carbonate_platform", basin);

    assertEquals(
        SedimentaryDiagenesisState.DolomitizationClass.ACTIVE_REPLACEMENT,
        state.dolomitizationClass());
    assertEquals(SalinityClass.MODERATE_BRINE, state.fluidSalinity());
    assertTrue(state.retainedPorosityPpm() > 0);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SedimentaryDiagenesisState(
                state.compactionClass(),
                state.cementationClass(),
                state.dissolutionClass(),
                state.dolomitizationClass(),
                state.organicMaturityClass(),
                state.fluidSalinity(),
                MaterialAssemblage.SCALE + 1L));
  }

  private static long total(SedimentaryInputBudget budget) {
    return budget.clasticPpm()
        + budget.volcanicPpm()
        + budget.carbonatePpm()
        + budget.organicPpm()
        + budget.chemicalPrecipitatePpm()
        + budget.evaporiticBrinePpm();
  }

  @Test
  void metamorphicProcessStateSeparatesRegionalContactAndMassTransferPaths() {
    MetamorphicProcessState regional =
        MetamorphicProcessState.proofFor(
            MetamorphicGrade.MEDIUM,
            MetamorphicFacies.AMPHIBOLITE,
            MetamorphicPath.COLLISION_CLOCKWISE,
            MaterialProcessClass.NONE,
            0L,
            Optional.empty());
    MetamorphicProcessState contact =
        MetamorphicProcessState.proofFor(
            MetamorphicGrade.HIGH,
            MetamorphicFacies.HORNBLENDE_HORNFELS,
            MetamorphicPath.CONTACT_LOW_P,
            MaterialProcessClass.ISOCHEMICAL_METAMORPHISM,
            0L,
            Optional.empty());
    MetamorphicProcessState carbonateContact =
        MetamorphicProcessState.proofFor(
            MetamorphicGrade.HIGH,
            MetamorphicFacies.HORNBLENDE_HORNFELS,
            MetamorphicPath.CONTACT_LOW_P,
            MaterialProcessClass.ISOCHEMICAL_METAMORPHISM,
            0L,
            Optional.empty(),
            Optional.of(Lithology.LIMESTONE));
    MetamorphicProcessState felsicAnatexis =
        MetamorphicProcessState.proofFor(
            MetamorphicGrade.HIGH,
            MetamorphicFacies.AMPHIBOLITE,
            MetamorphicPath.COLLISION_CLOCKWISE,
            MaterialProcessClass.NONE,
            0L,
            Optional.empty(),
            Optional.of(Lithology.GRANITIC_GNEISS));
    MetamorphicProcessState altered =
        MetamorphicProcessState.proofFor(
            MetamorphicGrade.NONE,
            MetamorphicFacies.NONE,
            MetamorphicPath.NONE,
            MaterialProcessClass.HYDROTHERMAL_METASOMATISM,
            280_000L,
            Optional.of(
                new ProcessFluidState(
                    FluidMedium.MAGMATIC_HYDROTHERMAL,
                    RedoxClass.OXIDIZING,
                    AcidityClass.NEAR_NEUTRAL,
                    SalinityClass.MODERATE_BRINE,
                    SulfurState.MIXED,
                    new LigandCapacities(1, 1, 1, 0),
                    2)));

    assertEquals(
        MetamorphicProcessState.BurialCurveClass.COLLISIONAL_THICKENING,
        regional.burialCurveClass());
    assertEquals(MetamorphicProcessState.StrainClass.NEMATOBLASTIC, regional.strainClass());
    assertEquals(0L, regional.massTransferPpm());
    assertEquals(
        MetamorphicReactionState.ReactionMechanism.REGIONAL_RECRYSTALLIZATION,
        regional.reactionState().reactionMechanism());
    assertEquals(
        MetamorphicProcessState.BurialCurveClass.CONTACT_HEATING, contact.burialCurveClass());
    assertEquals(
        MetamorphicProcessState.StrainClass.THERMAL_RECRYSTALLIZATION, contact.strainClass());
    assertEquals(0L, contact.massTransferPpm());
    assertEquals(300_000L, contact.strainIntensityPpm());
    assertEquals(
        MetamorphicReactionState.ReactionMechanism.THERMAL_RECRYSTALLIZATION,
        contact.reactionState().reactionMechanism());
    assertEquals(
        MetamorphicReactionState.ReactionMechanism.DECARBONATION,
        carbonateContact.reactionState().reactionMechanism());
    assertEquals(250_000L, carbonateContact.reactionState().decarbonationPpm());
    assertEquals(
        MetamorphicFluidContribution.FluidSpecies.CARBON_DIOXIDE,
        carbonateContact.reactionState().fluidContributions().getFirst().fluidSpecies());
    assertEquals(
        MetamorphicReactionState.ReactionMechanism.PARTIAL_MELTING,
        felsicAnatexis.reactionState().reactionMechanism());
    assertEquals(150_000L, felsicAnatexis.reactionState().partialMeltingPpm());
    assertTrue(felsicAnatexis.reactionState().fluidContributions().isEmpty());
    assertEquals(280_000L, altered.reactionProgressPpm());
    assertEquals(280_000L, altered.massTransferPpm());
    assertEquals(
        MetamorphicReactionState.ReactionMechanism.METASOMATIC_REPLACEMENT,
        altered.reactionState().reactionMechanism());
    assertThrows(
        IllegalArgumentException.class,
        () -> new MetamorphicReactionState.SerpentinizationBalance(1L, 1L, 1L, 0L, 0L));
    assertEquals(
        MetamorphicProcessState.FluidAvailabilityClass.HYDROTHERMAL_FLOW,
        altered.fluidAvailabilityClass());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            MetamorphicProcessState.proofFor(
                MetamorphicGrade.NONE,
                MetamorphicFacies.NONE,
                MetamorphicPath.NONE,
                MaterialProcessClass.WEATHERING,
                100_000L,
                Optional.empty()));
  }

  @Test
  void extendedMetamorphicPathsRetainDistinctBurialAndRetrogressionEvidence() {
    MetamorphicProcessState burial =
        MetamorphicProcessState.proofFor(
            MetamorphicGrade.MEDIUM,
            MetamorphicFacies.GREENSCHIST,
            MetamorphicPath.BURIAL_HEATING,
            MaterialProcessClass.NONE,
            0L,
            Optional.empty());
    MetamorphicProcessState subduction =
        MetamorphicProcessState.proofFor(
            MetamorphicGrade.MEDIUM,
            MetamorphicFacies.GREENSCHIST,
            MetamorphicPath.SUBDUCTION_COLD,
            MaterialProcessClass.NONE,
            0L,
            Optional.empty());
    MetamorphicProcessState exhumation =
        MetamorphicProcessState.proofFor(
            MetamorphicGrade.MEDIUM,
            MetamorphicFacies.GREENSCHIST,
            MetamorphicPath.EXTENSION_DECOMPRESSION,
            MaterialProcessClass.NONE,
            0L,
            Optional.empty());
    MetamorphicProcessState polymetamorphic =
        MetamorphicProcessState.proofFor(
            MetamorphicGrade.MEDIUM,
            MetamorphicFacies.GREENSCHIST,
            MetamorphicPath.POLYMETAMORPHIC,
            MaterialProcessClass.NONE,
            0L,
            Optional.empty());

    assertEquals(
        MetamorphicProcessState.BurialCurveClass.BURIAL_HEATING, burial.burialCurveClass());
    assertEquals(
        MetamorphicProcessState.BurialCurveClass.SUBDUCTION_COOLING, subduction.burialCurveClass());
    assertEquals(
        MetamorphicProcessState.BurialCurveClass.EXHUMATION_DECOMPRESSION,
        exhumation.burialCurveClass());
    assertEquals(
        MetamorphicProcessState.BurialCurveClass.POLYMETAMORPHIC_REWORKING,
        polymetamorphic.burialCurveClass());
    assertEquals(300_000L, burial.retrogressionPotentialPpm());
    assertEquals(400_000L, subduction.retrogressionPotentialPpm());
    assertEquals(650_000L, exhumation.retrogressionPotentialPpm());
    assertEquals(500_000L, polymetamorphic.retrogressionPotentialPpm());
    assertEquals(650_000L, burial.strainIntensityPpm());
    assertEquals(650_000L, subduction.strainIntensityPpm());
    assertEquals(650_000L, exhumation.strainIntensityPpm());
    assertEquals(650_000L, polymetamorphic.strainIntensityPpm());
    assertEquals(
        MetamorphicReactionState.ReactionMechanism.REGIONAL_RECRYSTALLIZATION,
        burial.reactionState().reactionMechanism());
    assertEquals(
        MetamorphicReactionState.ReactionMechanism.REGIONAL_RECRYSTALLIZATION,
        subduction.reactionState().reactionMechanism());
    assertEquals(
        MetamorphicReactionState.ReactionMechanism.REGIONAL_RECRYSTALLIZATION,
        exhumation.reactionState().reactionMechanism());
    assertEquals(
        MetamorphicReactionState.ReactionMechanism.REGIONAL_RECRYSTALLIZATION,
        polymetamorphic.reactionState().reactionMechanism());
    assertTrue(burial.reactionState().fluidContributions().isEmpty());
    assertTrue(subduction.reactionState().fluidContributions().isEmpty());
    assertTrue(exhumation.reactionState().fluidContributions().isEmpty());
    assertTrue(polymetamorphic.reactionState().fluidContributions().isEmpty());
    assertEquals(MetamorphicStrainState.FrameClass.FOLIATION, burial.strainState().frameClass());
    assertEquals(45.0, polymetamorphic.strainState().foliationAzimuthDegrees());
    assertEquals(
        MaterialAssemblage.SCALE,
        burial.strainState().shorteningAxisPpm()
            + burial.strainState().flatteningAxisPpm()
            + burial.strainState().stretchingAxisPpm());
  }

  @Test
  void metamorphicStrainFrameClosesAxesAndRejectsInconsistentEvidence() {
    MetamorphicStrainState collision =
        MetamorphicStrainState.proofFor(
            MetamorphicPath.COLLISION_CLOCKWISE,
            MetamorphicProcessState.StrainClass.DIRECTED_FOLIATION,
            650_000L);

    assertEquals(MetamorphicStrainState.FrameClass.FOLIATION, collision.frameClass());
    assertEquals(650_000L, collision.intensityPpm());
    assertEquals(45.0, collision.foliationAzimuthDegrees());
    assertEquals(135.0, collision.lineationTrendDegrees());
    assertEquals(15.0, collision.lineationPlungeDegrees());
    assertEquals(
        MaterialAssemblage.SCALE,
        collision.shorteningAxisPpm()
            + collision.flatteningAxisPpm()
            + collision.stretchingAxisPpm());
    assertEquals(
        MetamorphicStrainState.FrameClass.NONE, MetamorphicStrainState.none().frameClass());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MetamorphicStrainState(
                MetamorphicStrainState.FrameClass.FOLIATION, 1L, 1L, 1L, 1L, 0.0, 0.0, 0.0));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MetamorphicProcessState(
                MetamorphicProcessState.BurialCurveClass.COLLISIONAL_THICKENING,
                MetamorphicProcessState.StrainClass.DIRECTED_FOLIATION,
                MetamorphicProcessState.FluidAvailabilityClass.BUFFERED_AQUEOUS,
                650_000L,
                MetamorphicStrainState.none(),
                650_000L,
                0L,
                250_000L,
                MetamorphicReactionState.none()));
  }

  @Test
  void materialBufferStateDerivesBoundedHostAndFluidCapacities() {
    MaterialAssemblage coalAssemblage =
        new MaterialAssemblage(
            Map.of(
                "geological:constituent/coal_organic_matter", 800_000L,
                "geological:mineral/kaolinite", 100_000L,
                "geological:mineral/pyrite", 100_000L));
    BulkComposition coalComposition =
        new BulkComposition(
            Map.of(
                ChemicalElement.C, 450_000L,
                ChemicalElement.H, 150_000L,
                ChemicalElement.O, 250_000L,
                ChemicalElement.S, 50_000L,
                ChemicalElement.N, 100_000L),
            1.5);
    MaterialBufferState coal =
        MaterialBufferState.proofFor(
            Lithology.COAL,
            coalAssemblage,
            coalComposition,
            MaterialProcessClass.NONE,
            Optional.empty());

    MaterialAssemblage quartzAssemblage =
        new MaterialAssemblage(Map.of("geological:mineral/quartz", MaterialAssemblage.SCALE));
    BulkComposition quartzComposition =
        new BulkComposition(
            Map.of(ChemicalElement.SI, 467_000L, ChemicalElement.O, 533_000L), 2.65);
    MaterialBufferState quartz =
        MaterialBufferState.proofFor(
            Lithology.QUARTZITE,
            quartzAssemblage,
            quartzComposition,
            MaterialProcessClass.NONE,
            Optional.empty());

    assertTrue(coal.organicCarbonCapacityPpm() > quartz.organicCarbonCapacityPpm());
    assertTrue(coal.reducedSulfurCapacityPpm() > quartz.reducedSulfurCapacityPpm());
    assertTrue(coal.clayCapacityPpm() > quartz.clayCapacityPpm());
    assertTrue(coal.adsorptionCapacityPpm() >= coal.clayCapacityPpm());
    assertTrue(coal.volatileInventoryPpm() >= coal.waterInventoryPpm());

    MaterialBufferState fluidized =
        MaterialBufferState.proofFor(
            Lithology.COAL,
            coalAssemblage,
            coalComposition,
            MaterialProcessClass.HYDROTHERMAL_METASOMATISM,
            Optional.of(fluidState()));
    assertTrue(fluidized.waterInventoryPpm() > coal.waterInventoryPpm());

    assertThrows(
        IllegalArgumentException.class,
        () -> new MaterialBufferState(0L, 0L, 0L, 0L, 0L, 0L, MaterialAssemblage.SCALE + 1L, 0L));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            MaterialBufferState.proofFor(
                Lithology.COAL,
                coalAssemblage,
                coalComposition,
                MaterialProcessClass.WEATHERING,
                Optional.empty()));
  }

  @Test
  void fractureTensorStateClosesAndRejectsNonPositiveSemidefiniteEvidence() {
    FractureTensorState isotropic = FractureTensorState.none();
    FractureTensorState foliated =
        FractureTensorState.proofFor(
            Lithology.MICA_SCHIST,
            RockTexture.SCHISTOSE,
            MaterialProcessClass.NONE,
            MetamorphicStrainState.proofFor(
                MetamorphicPath.COLLISION_CLOCKWISE,
                MetamorphicProcessState.StrainClass.DIRECTED_FOLIATION,
                650_000L));
    FractureTensorState weathered =
        FractureTensorState.proofFor(
            Lithology.SOIL_COLLUVIUM,
            RockTexture.SOIL_COLLUVIAL,
            MaterialProcessClass.WEATHERING,
            MetamorphicStrainState.none());

    assertEquals(MaterialAssemblage.SCALE, foliated.xxPpm() + foliated.yyPpm() + foliated.zzPpm());
    assertTrue(foliated.intensityPpm() > isotropic.intensityPpm());
    assertTrue(weathered.connectivityPpm() > isotropic.connectivityPpm());
    assertEquals(0L, isotropic.intensityPpm());
    assertEquals(0L, isotropic.connectivityPpm());
    assertThrows(
        IllegalArgumentException.class,
        () -> new FractureTensorState(500_000L, 499_999L, 0L, 0L, 0L, 0L, 1L, 1L));
    assertThrows(
        IllegalArgumentException.class,
        () -> new FractureTensorState(500_000L, 500_000L, 0L, 600_000L, 0L, 0L, 1L, 1L));
  }

  @Test
  void traceElementVectorIsSparseAndLogConcentrationConsistent() {
    BulkComposition composition =
        new BulkComposition(
            Map.of(
                ChemicalElement.C, 450_000L,
                ChemicalElement.H, 150_000L,
                ChemicalElement.O, 250_000L,
                ChemicalElement.S, 50_000L,
                ChemicalElement.N, 100_000L),
            1.5);
    TraceElementVector vector = TraceElementVector.from(composition);

    assertEquals(50_000L, vector.concentrationPpm(ChemicalElement.S));
    assertTrue(vector.log10PpmMicros(ChemicalElement.S) > 4_000_000L);
    assertEquals(0L, vector.concentrationPpm(ChemicalElement.C));
    assertEquals(1, vector.concentrationPpm().size());
    assertThrows(
        IllegalArgumentException.class,
        () -> new TraceElementVector(Map.of(ChemicalElement.C, 1L), Map.of(ChemicalElement.C, 0L)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new TraceElementVector(
                Map.of(ChemicalElement.S, 50_000L), Map.of(ChemicalElement.S, 0L)));
  }

  @Test
  void metamorphicEventTimelineSortsIdsAndAgesAsPairs() {
    StableId olderId = StableId.parse("00000000000000000000000000000031");
    StableId youngerId = StableId.parse("00000000000000000000000000000032");
    AgeKey older = new AgeKey(120.0, 0);
    AgeKey younger = new AgeKey(80.0, 1);
    MetamorphicProcessState process =
        MetamorphicProcessState.proofFor(
            MetamorphicGrade.MEDIUM,
            MetamorphicFacies.GREENSCHIST,
            MetamorphicPath.COLLISION_CLOCKWISE,
            MaterialProcessClass.NONE,
            0L,
            Optional.empty());

    MetamorphicHistory history =
        new MetamorphicHistory(
            "geological:rock/protolith",
            MetamorphicGrade.MEDIUM,
            MetamorphicFacies.GREENSCHIST,
            MetamorphicPath.COLLISION_CLOCKWISE,
            500.0,
            650.0,
            500.0,
            900.0,
            List.of(youngerId, olderId),
            List.of(younger, older),
            process);

    assertEquals(List.of(olderId, youngerId), history.eventIds());
    assertEquals(List.of(older, younger), history.eventAges());
    assertEquals(
        List.of(
            new MetamorphicEventTiming(olderId, older),
            new MetamorphicEventTiming(youngerId, younger)),
        history.eventTimeline());
  }

  @Test
  void metamorphicFluidContributionsAreCanonicalAndReactionBound() {
    MetamorphicReactionState dehydration =
        new MetamorphicReactionState(
            MetamorphicReactionState.ReactionMechanism.DEHYDRATION,
            MetamorphicReactionState.RetrogressionClass.LOW,
            450_000L,
            0L,
            0L,
            MetamorphicReactionState.none().serpentinizationBalance(),
            List.of(
                new MetamorphicFluidContribution(
                    MetamorphicFluidContribution.FluidSpecies.WATER,
                    MetamorphicFluidContribution.Direction.OUTPUT,
                    450_000L)));

    assertEquals(
        List.of(
            new MetamorphicFluidContribution(
                MetamorphicFluidContribution.FluidSpecies.WATER,
                MetamorphicFluidContribution.Direction.OUTPUT,
                450_000L)),
        dehydration.fluidContributions());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MetamorphicFluidContribution(
                MetamorphicFluidContribution.FluidSpecies.WATER,
                MetamorphicFluidContribution.Direction.INPUT,
                0L));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MetamorphicReactionState(
                MetamorphicReactionState.ReactionMechanism.DEHYDRATION,
                MetamorphicReactionState.RetrogressionClass.LOW,
                450_000L,
                0L,
                0L,
                MetamorphicReactionState.none().serpentinizationBalance(),
                List.of(
                    new MetamorphicFluidContribution(
                        MetamorphicFluidContribution.FluidSpecies.WATER,
                        MetamorphicFluidContribution.Direction.OUTPUT,
                        449_999L))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MetamorphicReactionState(
                MetamorphicReactionState.ReactionMechanism.REGIONAL_RECRYSTALLIZATION,
                MetamorphicReactionState.RetrogressionClass.LOW,
                0L,
                0L,
                0L,
                MetamorphicReactionState.none().serpentinizationBalance(),
                List.of(
                    new MetamorphicFluidContribution(
                        MetamorphicFluidContribution.FluidSpecies.WATER,
                        MetamorphicFluidContribution.Direction.INPUT,
                        1L))));
  }

  @Test
  void magmaDifferentiationStateClosesAndCanonicalizesSourceReservoirs() {
    StableId basement = StableId.parse("00000000000000000000000000000001");
    StableId system = StableId.parse("00000000000000000000000000000002");
    MagmaDifferentiationState state =
        MagmaDifferentiationState.arcProofFor(2, List.of(system, basement));

    assertEquals(List.of(basement, system), state.sourceReservoirIds());
    assertEquals(
        MaterialAssemblage.SCALE,
        state.cumulativeCrystalFractionPpm() + state.residualMeltFractionPpm());
    assertEquals(
        MagmaDifferentiationState.SulfurSaturationHistory.SATURATED,
        state.sulfurSaturationHistory());
    assertEquals(
        MagmaDifferentiationState.ResidualFluidPotential.VERY_HIGH, state.residualFluidPotential());
    assertEquals(
        List.of("EVOLVED_RESIDUAL_MELT", "OXIDIZED_ARC", "VOLATILE_ENRICHED"),
        state.fertilityTags());
    assertEquals(100_000L, state.residualFluidFractionPpm());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MagmaDifferentiationState(
                state.tectonicSetting(),
                state.sourceReservoirIds(),
                state.meltingMechanism(),
                state.sourceLithologyClass(),
                state.meltFractionClass(),
                state.sulfurSaturationHistory(),
                state.crustalAssimilationClass(),
                state.differentiationPath(),
                900_000L,
                90_000L));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MagmaDifferentiationState(
                state.tectonicSetting(),
                state.sourceReservoirIds(),
                state.meltingMechanism(),
                state.sourceLithologyClass(),
                state.meltFractionClass(),
                state.sulfurSaturationHistory(),
                state.crustalAssimilationClass(),
                state.differentiationPath(),
                state.cumulativeCrystalFractionPpm(),
                state.residualMeltFractionPpm(),
                state.residualFluidPotential(),
                List.of("OXIDIZED_ARC", "OXIDIZED_ARC")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MagmaDifferentiationState(
                state.tectonicSetting(),
                state.sourceReservoirIds(),
                state.meltingMechanism(),
                state.sourceLithologyClass(),
                state.meltFractionClass(),
                state.sulfurSaturationHistory(),
                state.crustalAssimilationClass(),
                state.differentiationPath(),
                state.cumulativeCrystalFractionPpm(),
                state.residualMeltFractionPpm(),
                state.residualMeltFractionPpm() + 1L,
                state.residualFluidPotential(),
                state.fertilityTags()));
    assertThrows(
        IllegalArgumentException.class,
        () -> MagmaDifferentiationState.arcProofFor(-1, List.of(basement)));
  }

  @Test
  void sedimentGrainSizeBlendsCloseExactlyAndIgnoreShareOrder() {
    SedimentGrainSize coarse = new SedimentGrainSize(600_000L, 300_000L, 100_000L);
    SedimentGrainSize matrix = new SedimentGrainSize(100_000L, 400_000L, 500_000L);
    SedimentGrainSize expected = new SedimentGrainSize(275_000L, 365_000L, 360_000L);

    List<SedimentGrainSize.Share> shares =
        List.of(
            new SedimentGrainSize.Share(coarse, 350_000L),
            new SedimentGrainSize.Share(matrix, 650_000L));
    assertEquals(expected, SedimentGrainSize.weightedBlend(shares));
    assertEquals(expected, SedimentGrainSize.weightedBlend(shares.reversed()));
    assertThrows(
        IllegalArgumentException.class, () -> new SedimentGrainSize(600_000L, 300_000L, 99_999L));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            SedimentGrainSize.weightedBlend(
                List.of(new SedimentGrainSize.Share(coarse, 999_999L))));
  }

  @Test
  void colluvialTextureClassifiesSupportAndCoarseSorting() {
    ColluvialTextureState matrix =
        ColluvialTextureState.from(new SedimentGrainSize(250_000L, 350_000L, 400_000L));
    ColluvialTextureState mixed =
        ColluvialTextureState.from(new SedimentGrainSize(400_000L, 350_000L, 250_000L));
    ColluvialTextureState clast =
        ColluvialTextureState.from(new SedimentGrainSize(500_000L, 350_000L, 150_000L));

    assertEquals(SedimentSupport.MATRIX_SUPPORTED, matrix.support());
    assertEquals(SedimentSupport.MIXED_SUPPORT, mixed.support());
    assertEquals(SedimentSupport.CLAST_SUPPORTED, clast.support());
    assertEquals(SedimentSorting.UNSORTED_TO_POORLY_SORTED, mixed.sorting());
    assertEquals(SedimentSorting.UNSORTED_TO_POORLY_SORTED, clast.sorting());
    assertEquals(0.1, mixed.sortingDominanceIndex(), 1.0e-15);
    assertEquals(0.25, clast.sortingDominanceIndex(), 1.0e-15);
    assertEquals(ClastShape.ANGULAR_TO_SUBROUNDED, mixed.clastShape());
  }

  @Test
  void colluvialTransportProcessSelectionUsesSlopeRunoffAndRouteEvidence() {
    SedimentGrainSize grainYield = new SedimentGrainSize(400_000L, 350_000L, 250_000L);
    ColluvialSedimentBudget.TerrainPath path = terrainPath(100.0);
    ColluvialSedimentBudget.ProductionInput creepInput =
        new ColluvialSedimentBudget.ProductionInput(
            350_000L, 8.0, 0.02, 0.8, 0.25, 0.0, path, grainYield);
    ColluvialSedimentBudget.ProductionInput sheetwashInput =
        new ColluvialSedimentBudget.ProductionInput(
            350_000L, 8.0, 0.12, 0.8, 0.25, 1.0, path, grainYield);
    ColluvialSedimentBudget.ProductionInput dryRavelInput =
        new ColluvialSedimentBudget.ProductionInput(
            350_000L, 8.0, 0.24, 0.8, 0.25, 0.0, path, grainYield);
    ColluvialTransportProcess creep = ColluvialTransportProcess.from(creepInput);
    ColluvialTransportProcess sheetwash = ColluvialTransportProcess.from(sheetwashInput);
    ColluvialTransportProcess dryRavel = ColluvialTransportProcess.from(dryRavelInput);

    assertEquals(ColluvialTransportProcess.ProcessClass.HILLSLOPE_CREEP, creep.processClass());
    assertEquals(ColluvialTransportProcess.ProcessClass.SHEETWASH, sheetwash.processClass());
    assertEquals(ColluvialTransportProcess.ProcessClass.DRY_RAVEL, dryRavel.processClass());
    assertEquals(creep, ColluvialTransportProcess.from(creepInput));
    assertEquals(sheetwash, ColluvialTransportProcess.from(sheetwashInput));
    assertEquals(dryRavel, ColluvialTransportProcess.from(dryRavelInput));
    for (ColluvialTransportProcess process : List.of(creep, sheetwash, dryRavel)) {
      assertTrue(process.selectedScore() >= 0.0);
      assertTrue(process.selectedScore() <= 1.0);
      assertTrue(process.selectionMargin() >= 0.0);
    }
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialTransportProcess(
                ColluvialTransportProcess.ProcessClass.DRY_RAVEL, 1.0, 0.0, 0.0));
  }

  @Test
  void processResponsePolicyConditionsMobilizedMassWithoutChangingSelection() {
    SedimentGrainSize grainYield = new SedimentGrainSize(400_000L, 350_000L, 250_000L);
    ColluvialSedimentBudget.TerrainPath localPath = terrainPath(100.0);
    ColluvialSedimentBudget.TerrainPath farPath =
        terrainPath(100.0, 104.0, 103.0, 109.0, 108.0, 112.0, 116.0);
    ColluvialSedimentBudget.ProductionInput matrixInput =
        new ColluvialSedimentBudget.ProductionInput(
            350_000L, 8.0, 0.02, 0.8, 0.25, 0.0, localPath, grainYield);
    ColluvialSedimentBudget.ProductionInput sheetwashInput =
        new ColluvialSedimentBudget.ProductionInput(
            350_000L, 8.0, 0.12, 0.8, 0.25, 1.0, localPath, grainYield);
    ColluvialSedimentBudget.ProductionInput dryRavelInput =
        new ColluvialSedimentBudget.ProductionInput(
            300_000L, 8.0, 0.24, 0.8, 0.25, 0.0, farPath, grainYield);
    ColluvialTransportPolicy tunedPolicy =
        new ColluvialTransportPolicy(
            12.0, 0.24, 0.25, 0.65, 0.50, 0.40, 0.50, 0.75, 0.70, 512.0, 384.0, 256.0, 0.50, 1.0,
            1.0, 0.25);
    ColluvialSedimentBudget baseline =
        ColluvialSedimentBudget.derive(
            0.12,
            matrixInput,
            List.of(
                new ColluvialSedimentBudget.SourceProductionInput(
                    StableId.parse("00000000000000000000000000000c11"), 0, sheetwashInput),
                new ColluvialSedimentBudget.SourceProductionInput(
                    StableId.parse("00000000000000000000000000000c12"), 192, dryRavelInput)));
    ColluvialSedimentBudget tuned =
        ColluvialSedimentBudget.derive(
            0.12,
            matrixInput,
            List.of(
                new ColluvialSedimentBudget.SourceProductionInput(
                    StableId.parse("00000000000000000000000000000c11"), 0, sheetwashInput),
                new ColluvialSedimentBudget.SourceProductionInput(
                    StableId.parse("00000000000000000000000000000c12"), 192, dryRavelInput)),
            tunedPolicy);

    ColluvialSedimentBudget.InputBalance baselineDry = baseline.sourceBalances().get(1).balance();
    ColluvialSedimentBudget.InputBalance tunedDry = tuned.sourceBalances().get(1).balance();
    assertEquals(
        ColluvialTransportProcess.ProcessClass.DRY_RAVEL,
        baselineDry.transportProcess().processClass());
    assertEquals(
        baselineDry.transportProcess().processClass(), tunedDry.transportProcess().processClass());
    assertEquals(1.0, baselineDry.productionState().processResponse(), 1.0e-15);
    assertEquals(0.25, tunedDry.productionState().processResponse(), 1.0e-15);
    assertTrue(tunedDry.mobilizedFixedUnits() < baselineDry.mobilizedFixedUnits());
    assertEquals(
        tunedDry.input().capacityFixedUnits(),
        tunedDry.retainedFixedUnits() + tunedDry.mobilizedFixedUnits());
    assertEquals(
        tunedDry.mobilizedFixedUnits(),
        tunedDry.transportLossFixedUnits()
            + tunedDry.bypassedFixedUnits()
            + tunedDry.depositedFixedUnits());
  }

  @Test
  void callerMassScaleConvertsNormalizedColluvialStagesWithoutChangingClosure() {
    SedimentGrainSize grainYield = new SedimentGrainSize(400_000L, 350_000L, 250_000L);
    ColluvialSedimentBudget.TerrainPath localPath = terrainPath(100.0);
    ColluvialSedimentBudget.TerrainPath farPath =
        terrainPath(100.0, 104.0, 103.0, 109.0, 108.0, 112.0, 116.0);
    ColluvialSedimentBudget budget =
        ColluvialSedimentBudget.derive(
            0.12,
            new ColluvialSedimentBudget.ProductionInput(
                350_000L, 8.0, 0.02, 0.8, 0.25, 0.0, localPath, grainYield),
            List.of(
                new ColluvialSedimentBudget.SourceProductionInput(
                    StableId.parse("00000000000000000000000000000c11"),
                    0,
                    new ColluvialSedimentBudget.ProductionInput(
                        350_000L, 8.0, 0.12, 0.8, 0.25, 1.0, localPath, grainYield)),
                new ColluvialSedimentBudget.SourceProductionInput(
                    StableId.parse("00000000000000000000000000000c12"),
                    192,
                    new ColluvialSedimentBudget.ProductionInput(
                        300_000L, 8.0, 0.24, 0.8, 0.25, 0.0, farPath, grainYield))));
    ColluvialMassScale scale = new ColluvialMassScale("kg", 2_500.0, 2.5);
    ColluvialAbsoluteMassBudget absolute = budget.absoluteMass(scale);

    assertEquals("kg", absolute.massUnit());
    assertEquals(1_000_000L, absolute.capacityFixedUnits());
    assertEquals(2_500.0, absolute.capacityMass(), 1.0e-12);
    assertEquals(1_000.0, absolute.capacityRate(), 1.0e-12);
    assertEquals(scale.mass(absolute.mobilizedFixedUnits()), absolute.mobilizedMass(), 1.0e-12);
    assertEquals(
        scale.productionRate(absolute.depositedFixedUnits()), absolute.depositedRate(), 1.0e-12);
    assertEquals(3, absolute.inputBalances().size());
    assertEquals(
        absolute.capacityFixedUnits(),
        absolute.retainedFixedUnits() + absolute.mobilizedFixedUnits());
    assertEquals(
        absolute.mobilizedFixedUnits(),
        absolute.transportLossFixedUnits()
            + absolute.bypassedFixedUnits()
            + absolute.depositedFixedUnits());
    for (ColluvialAbsoluteMassBudget.InputMassBalance input : absolute.inputBalances()) {
      assertEquals(
          input.capacityFixedUnits(), input.retainedFixedUnits() + input.mobilizedFixedUnits());
      assertEquals(
          input.mobilizedFixedUnits(),
          input.transportLossFixedUnits()
              + input.bypassedFixedUnits()
              + input.depositedFixedUnits());
    }
    assertThrows(IllegalArgumentException.class, () -> new ColluvialMassScale("kg", 0.0, 1.0));
    assertThrows(IllegalArgumentException.class, () -> new ColluvialMassScale("kg", 1.0, 0.0));
    assertThrows(IllegalArgumentException.class, () -> scale.mass(-1L));
  }

  @Test
  void colluvialRoutePolicyClosesNormalizedCapacitiesAndDistanceSampling() {
    ColluvialRoutePolicy policy = ColluvialRoutePolicy.DEFAULT;
    assertEquals(6, policy.routeReachCount());
    assertEquals(96, policy.nearSourceDistanceBlocks());
    assertEquals(192, policy.farSourceDistanceBlocks());
    assertEquals(
        MaterialAssemblage.SCALE,
        policy.weatheredMatrixCapacityFixedUnits()
            + policy.localSourceCapacityFixedUnits()
            + policy.nearSourceCapacityFixedUnits()
            + policy.farSourceCapacityFixedUnits());
    assertEquals(policy, ColluvialRoutePolicy.DEFAULT);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialRoutePolicy(
                0.1, 4.0, 32.0, 4.0, 8.0, 32, 64, 192, 60.0, 350_000L, 350_000L, 200_000L,
                100_001L));
  }

  @Test
  void colluvialTransportPolicyValidatesBoundedResponseParameters() {
    ColluvialTransportPolicy policy = ColluvialTransportPolicy.DEFAULT;
    assertEquals(policy, ColluvialTransportPolicy.DEFAULT);
    assertEquals(512.0, policy.gravelAndCoarserReferenceEFoldingDistanceBlocks());
    assertEquals(0.50, policy.maximumBypassFraction());
    assertEquals(
        1.0, policy.processResponse(ColluvialTransportProcess.ProcessClass.HILLSLOPE_CREEP));
    ColluvialTransportPolicy processPolicy =
        new ColluvialTransportPolicy(
            12.0, 0.24, 0.25, 0.65, 0.50, 0.40, 0.50, 0.75, 0.70, 512.0, 384.0, 256.0, 0.50, 0.80,
            0.90, 0.60);
    assertEquals(
        0.80,
        processPolicy.processResponse(ColluvialTransportProcess.ProcessClass.HILLSLOPE_CREEP));
    assertEquals(
        0.90, processPolicy.processResponse(ColluvialTransportProcess.ProcessClass.SHEETWASH));
    assertEquals(
        0.60, processPolicy.processResponse(ColluvialTransportProcess.ProcessClass.DRY_RAVEL));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialTransportPolicy(
                0.0, 0.24, 0.25, 0.65, 0.50, 0.40, 0.50, 0.75, 0.70, 512.0, 384.0, 256.0, 0.50));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialTransportPolicy(
                12.0, 0.24, 0.25, 0.65, 0.50, 0.40, 0.50, 0.75, 0.70, 512.0, 384.0, 256.0, 1.01));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialTransportPolicy(
                12.0, 0.24, 0.25, 0.65, 0.50, 0.40, 0.50, 0.75, 0.70, 512.0, 384.0, 256.0, 0.50,
                1.01, 0.90, 0.60));
  }

  @Test
  void inactiveColluvialSinkDestinationIsRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSinkDestination(
                ColluvialSinkState.SinkRole.NONE,
                Optional.empty(),
                0,
                new Point2(0.0, 0.0),
                StableId.parse("00000000000000000000000000000001"),
                StableId.parse("00000000000000000000000000000002"),
                Lithology.BASALTIC,
                Overprint.NONE,
                Lithology.BASALTIC,
                Overprint.NONE));
  }

  @Test
  void colluvialSourceClaimLedgerAggregatesAcrossParcelsAndIgnoresInputOrder() {
    StableId parcelA = StableId.parse("00000000000000000000000000000011");
    StableId parcelB = StableId.parse("00000000000000000000000000000012");
    StableId sourceOne = StableId.parse("00000000000000000000000000000021");
    StableId sourceTwo = StableId.parse("00000000000000000000000000000022");
    ColluvialSourceClaim first =
        new ColluvialSourceClaim(
            new Point2(1.0, 2.0),
            parcelA,
            sourceOne,
            0,
            350_000L,
            100_000L,
            250_000L,
            10_000L,
            20_000L,
            70_000L);
    ColluvialSourceClaim second =
        new ColluvialSourceClaim(
            new Point2(3.0, 4.0),
            parcelB,
            sourceOne,
            0,
            350_000L,
            120_000L,
            230_000L,
            12_000L,
            18_000L,
            90_000L);
    ColluvialSourceClaim third =
        new ColluvialSourceClaim(
            new Point2(1.0, 2.0),
            parcelA,
            sourceTwo,
            96,
            200_000L,
            80_000L,
            120_000L,
            8_000L,
            12_000L,
            60_000L);

    ColluvialSourceClaimLedger ledger =
        ColluvialSourceClaimLedger.from(List.of(third, first, second));
    assertEquals(3, ledger.claims().size());
    assertEquals(2, ledger.sourceAggregates().size());
    assertEquals(2, ledger.parcelCount());
    assertTrue(ledger.hasCrossParcelReuse());
    assertEquals(900_000L, ledger.claimedCapacityFixedUnits());
    assertEquals(300_000L, ledger.mobilizedFixedUnits());
    assertEquals(600_000L, ledger.retainedFixedUnits());
    assertEquals(30_000L, ledger.transportLossFixedUnits());
    assertEquals(50_000L, ledger.bypassedFixedUnits());
    assertEquals(220_000L, ledger.depositedFixedUnits());
    assertEquals(ledger, ColluvialSourceClaimLedger.from(List.of(second, third, first)));
    assertThrows(
        IllegalArgumentException.class,
        () -> ColluvialSourceClaimLedger.from(List.of(first, first)));
  }

  @Test
  void colluvialSourceCapacityLedgerReconcilesFiniteMobilizedInventoryExactly() {
    StableId parcelA = StableId.parse("00000000000000000000000000000011");
    StableId parcelB = StableId.parse("00000000000000000000000000000012");
    StableId sourceOne = StableId.parse("00000000000000000000000000000021");
    StableId sourceTwo = StableId.parse("00000000000000000000000000000022");
    ColluvialSourceClaim first =
        new ColluvialSourceClaim(
            new Point2(1.0, 2.0),
            parcelA,
            sourceOne,
            0,
            350_000L,
            100_000L,
            250_000L,
            10_000L,
            20_000L,
            70_000L);
    ColluvialSourceClaim second =
        new ColluvialSourceClaim(
            new Point2(3.0, 4.0),
            parcelB,
            sourceOne,
            0,
            350_000L,
            120_000L,
            230_000L,
            12_000L,
            18_000L,
            90_000L);
    ColluvialSourceClaim third =
        new ColluvialSourceClaim(
            new Point2(1.0, 2.0),
            parcelA,
            sourceTwo,
            96,
            200_000L,
            80_000L,
            120_000L,
            8_000L,
            12_000L,
            60_000L);

    Map<StableId, Long> capacities = Map.of(sourceOne, 100_000L, sourceTwo, 0L);
    ColluvialSourceCapacityLedger ledger =
        ColluvialSourceCapacityLedger.from(List.of(third, first, second), capacities);
    ColluvialSourceCapacityLedger reordered =
        ColluvialSourceCapacityLedger.from(List.of(second, third, first), capacities);

    assertEquals(900_000L, ledger.claimedCapacityFixedUnits());
    assertEquals(300_000L, ledger.requestedMobilizedFixedUnits());
    assertEquals(100_000L, ledger.allocatedMobilizedFixedUnits());
    assertEquals(200_000L, ledger.unallocatedMobilizedFixedUnits());
    assertEquals(800_000L, ledger.retainedFixedUnits());
    assertEquals(10_000L, ledger.transportLossFixedUnits());
    assertEquals(17_273L, ledger.bypassedFixedUnits());
    assertEquals(72_727L, ledger.depositedFixedUnits());
    assertEquals(0L, ledger.remainingSourceCapacityFixedUnits());
    assertTrue(ledger.hasDepletion());
    assertEquals(ledger, reordered);

    for (ColluvialSourceCapacityLedger.ReconciledClaim claim : ledger.claims()) {
      assertEquals(
          claim.claimedCapacityFixedUnits(),
          claim.retainedFixedUnits() + claim.allocatedMobilizedFixedUnits());
      assertEquals(
          claim.requestedMobilizedFixedUnits(),
          claim.allocatedMobilizedFixedUnits() + claim.unallocatedMobilizedFixedUnits());
      assertEquals(
          claim.allocatedMobilizedFixedUnits(),
          claim.transportLossFixedUnits()
              + claim.bypassedFixedUnits()
              + claim.depositedFixedUnits());
    }
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ColluvialSourceCapacityLedger.from(
                List.of(first, second, third), Map.of(sourceOne, 1L)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ColluvialSourceCapacityLedger.from(
                List.of(first, second, third), Map.of(sourceOne, -1L, sourceTwo, 0L)));
  }

  @Test
  void finiteSourceCapacityPreservesExactGrainClosureAcrossClaims() {
    StableId parcelOne = StableId.parse("00000000000000000000000000000031");
    StableId parcelTwo = StableId.parse("00000000000000000000000000000032");
    StableId source = StableId.parse("00000000000000000000000000000033");
    ColluvialSourceClaim first =
        new ColluvialSourceClaim(
            new Point2(1.0, 2.0),
            parcelOne,
            source,
            0,
            1_000L,
            600L,
            400L,
            100L,
            200L,
            300L,
            new ColluvialSedimentBudget.GrainMass(500L, 300L, 200L),
            new ColluvialSedimentBudget.GrainMass(300L, 180L, 120L),
            new ColluvialSedimentBudget.GrainMass(200L, 120L, 80L),
            new ColluvialSedimentBudget.GrainMass(50L, 30L, 20L),
            new ColluvialSedimentBudget.GrainMass(100L, 60L, 40L),
            new ColluvialSedimentBudget.GrainMass(150L, 90L, 60L));
    ColluvialSourceClaim second =
        new ColluvialSourceClaim(
            new Point2(3.0, 4.0),
            parcelTwo,
            source,
            96,
            1_000L,
            500L,
            500L,
            150L,
            130L,
            220L,
            new ColluvialSedimentBudget.GrainMass(200L, 500L, 300L),
            new ColluvialSedimentBudget.GrainMass(100L, 250L, 150L),
            new ColluvialSedimentBudget.GrainMass(100L, 250L, 150L),
            new ColluvialSedimentBudget.GrainMass(30L, 70L, 50L),
            new ColluvialSedimentBudget.GrainMass(20L, 80L, 30L),
            new ColluvialSedimentBudget.GrainMass(50L, 100L, 70L));

    ColluvialSourceCapacityLedger ledger =
        ColluvialSourceCapacityLedger.from(List.of(second, first), Map.of(source, 550L));
    ColluvialSourceCapacityLedger reordered =
        ColluvialSourceCapacityLedger.from(List.of(first, second), Map.of(source, 550L));

    assertEquals(ledger, reordered);
    assertEquals(
        ledger.claimedCapacityGrainMass(),
        ledger.retainedGrainMass().add(ledger.allocatedMobilizedGrainMass()));
    assertEquals(
        ledger.requestedMobilizedGrainMass(),
        ledger.allocatedMobilizedGrainMass().add(ledger.unallocatedMobilizedGrainMass()));
    assertEquals(
        ledger.allocatedMobilizedGrainMass(),
        ledger
            .transportLossGrainMass()
            .add(ledger.bypassedGrainMass())
            .add(ledger.depositedGrainMass()));
    assertEquals(550L, ledger.allocatedMobilizedGrainMass().totalFixedUnits());
    for (ColluvialSourceCapacityLedger.ReconciledClaim claim : ledger.claims()) {
      assertEquals(
          claim.claimedCapacityGrainMass(),
          claim.retainedGrainMass().add(claim.allocatedMobilizedGrainMass()));
      assertEquals(
          claim.requestedMobilizedGrainMass(),
          claim.allocatedMobilizedGrainMass().add(claim.unallocatedMobilizedGrainMass()));
      assertEquals(
          claim.allocatedMobilizedGrainMass(),
          claim
              .transportLossGrainMass()
              .add(claim.bypassedGrainMass())
              .add(claim.depositedGrainMass()));
    }
  }

  @Test
  void colluvialTransportProcessMixAggregatesDepositedTranchesExactly() {
    StableId sheetwashSource = StableId.parse("00000000000000000000000000000c11");
    StableId dryRavelSource = StableId.parse("00000000000000000000000000000c12");
    SedimentGrainSize grainYield = new SedimentGrainSize(400_000L, 350_000L, 250_000L);
    ColluvialSedimentBudget.TerrainPath localPath = terrainPath(100.0);
    ColluvialSedimentBudget.TerrainPath farPath =
        terrainPath(100.0, 104.0, 103.0, 109.0, 108.0, 112.0, 116.0);
    ColluvialSedimentBudget budget =
        ColluvialSedimentBudget.derive(
            0.12,
            new ColluvialSedimentBudget.ProductionInput(
                350_000L, 8.0, 0.02, 0.8, 0.25, 0.0, localPath, grainYield),
            List.of(
                new ColluvialSedimentBudget.SourceProductionInput(
                    sheetwashSource,
                    0,
                    new ColluvialSedimentBudget.ProductionInput(
                        350_000L, 8.0, 0.12, 0.8, 0.25, 1.0, localPath, grainYield)),
                new ColluvialSedimentBudget.SourceProductionInput(
                    dryRavelSource,
                    192,
                    new ColluvialSedimentBudget.ProductionInput(
                        300_000L, 8.0, 0.24, 0.8, 0.25, 0.0, farPath, grainYield))));

    ColluvialTransportProcessMix processMix = budget.transportProcessMix();
    ColluvialTransportProcessStageMix processStages = budget.transportProcessStageMix();
    for (ColluvialTransportProcessMix stage :
        List.of(
            processStages.capacity(),
            processStages.mobilized(),
            processStages.arrived(),
            processStages.deposited())) {
      assertEquals(
          MaterialAssemblage.SCALE,
          stage.hillslopeCreepFractionPpm()
              + stage.sheetwashFractionPpm()
              + stage.dryRavelFractionPpm());
      assertTrue(stage.dominantProcess() != null);
    }
    assertEquals(processStages, budget.transportProcessStageMix());
    List<ColluvialTransportProcessUsage> processUsages = budget.transportProcessUsages();
    assertEquals(3, processUsages.size());
    assertEquals(
        MaterialAssemblage.SCALE,
        processUsages.stream().mapToLong(ColluvialTransportProcessUsage::capacityFixedUnits).sum());
    assertEquals(
        budget.depositedInventoryFixedUnits(),
        processUsages.stream()
            .mapToLong(ColluvialTransportProcessUsage::depositedFixedUnits)
            .sum());
    for (ColluvialTransportProcessUsage usage : processUsages) {
      assertEquals(
          usage.capacityFixedUnits(), usage.retainedFixedUnits() + usage.mobilizedFixedUnits());
      assertEquals(
          usage.mobilizedFixedUnits(),
          usage.transportLossFixedUnits()
              + usage.bypassedFixedUnits()
              + usage.depositedFixedUnits());
      assertEquals(usage.capacityFixedUnits(), usage.capacityGrainMass().totalFixedUnits());
      assertEquals(usage.depositedFixedUnits(), usage.depositedGrainMass().totalFixedUnits());
    }
    for (ColluvialSedimentBudget.InputBalance balance :
        List.of(
            budget.weatheredMatrixBalance(),
            budget.sourceBalances().getFirst().balance(),
            budget.sourceBalances().getLast().balance())) {
      ColluvialProductionState production = balance.productionState();
      assertTrue(production.weatheringAvailability() >= 0.0);
      assertTrue(production.weatheringAvailability() <= 1.0);
      assertTrue(production.mobilizationPotential() >= 0.0);
      assertTrue(production.mobilizationPotential() <= 1.0);
      assertEquals(
          (double) balance.mobilizedFixedUnits() / balance.input().capacityFixedUnits(),
          production.mobilizedFraction(),
          1.0e-15);
      assertEquals(
          (double) balance.retainedFixedUnits() / balance.input().capacityFixedUnits(),
          production.retainedFraction(),
          1.0e-15);
      assertEquals(
          (double) balance.depositedFixedUnits() / balance.input().capacityFixedUnits(),
          production.netDepositionFraction(),
          1.0e-15);
    }
    assertEquals(
        MaterialAssemblage.SCALE,
        processMix.hillslopeCreepFractionPpm()
            + processMix.sheetwashFractionPpm()
            + processMix.dryRavelFractionPpm());
    assertTrue(processMix.hillslopeCreepFractionPpm() > 0);
    assertTrue(processMix.sheetwashFractionPpm() > 0);
    assertTrue(processMix.dryRavelFractionPpm() > 0);
    assertEquals(processMix, budget.transportProcessMix());
    assertEquals(ColluvialTransportProcess.ProcessClass.SHEETWASH, processMix.dominantProcess());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialTransportProcessMix(
                ColluvialTransportProcess.ProcessClass.DRY_RAVEL, 500_000L, 500_000L, 1L));
  }

  @Test
  void colluvialPhysicalStateUsesTextureToSelectValuesInsideAuthoredEnvelopes() {
    UnitIntervalDistribution porosity = new UnitIntervalDistribution(0.22, 0.38, 0.58);
    UnitIntervalDistribution permeability = new UnitIntervalDistribution(0.16, 0.38, 0.68);
    UnitIntervalDistribution erodibility = new UnitIntervalDistribution(0.68, 0.82, 0.95);
    ColluvialTextureState fineMatrix =
        ColluvialTextureState.from(new SedimentGrainSize(200_000L, 200_000L, 600_000L));
    ColluvialTextureState coarseClast =
        ColluvialTextureState.from(new SedimentGrainSize(600_000L, 350_000L, 50_000L));
    ColluvialTextureState sortedMatrix =
        new ColluvialTextureState(
            fineMatrix.grainSize(),
            SedimentSorting.WELL_SORTED,
            fineMatrix.support(),
            fineMatrix.clastShape());
    ColluvialTextureState sandyMatrix =
        ColluvialTextureState.from(new SedimentGrainSize(100_000L, 800_000L, 100_000L));
    ColluvialTextureState pureSand =
        ColluvialTextureState.from(new SedimentGrainSize(0L, 1_000_000L, 0L));

    assertEquals(SedimentSorting.MODERATELY_SORTED, fineMatrix.sorting());
    assertEquals(SedimentSorting.MODERATELY_SORTED, coarseClast.sorting());
    assertEquals(SedimentSorting.WELL_SORTED, sandyMatrix.sorting());
    assertTrue(
        fineMatrix.dispersionState().weightedSpreadIndex()
            > pureSand.dispersionState().weightedSpreadIndex());
    assertEquals(
        ColluvialGrainDispersionState.DispersionClass.MODERATE_WITHIN_BIN_PROXY,
        fineMatrix.dispersionState().dispersionClass());
    assertEquals(
        ColluvialGrainDispersionState.DispersionClass.BROAD_WITHIN_BIN_PROXY,
        coarseClast.dispersionState().dispersionClass());
    assertEquals(
        ColluvialGrainDispersionState.DispersionClass.MODERATE_WITHIN_BIN_PROXY,
        sandyMatrix.dispersionState().dispersionClass());
    assertEquals(
        ColluvialGrainDispersionState.DispersionClass.NARROW_WITHIN_BIN_PROXY,
        pureSand.dispersionState().dispersionClass());

    ColluvialPhysicalState fine =
        ColluvialPhysicalState.derive(fineMatrix, porosity, permeability, erodibility);
    ColluvialPhysicalState coarse =
        ColluvialPhysicalState.derive(coarseClast, porosity, permeability, erodibility);
    ColluvialPhysicalState sorted =
        ColluvialPhysicalState.derive(sortedMatrix, porosity, permeability, erodibility);
    ColluvialPhysicalState sandy =
        ColluvialPhysicalState.derive(sandyMatrix, porosity, permeability, erodibility);

    assertTrue(porosity.contains(fine.porosityFraction()));
    assertTrue(permeability.contains(fine.permeabilityIndex()));
    assertTrue(erodibility.contains(fine.erodibilityIndex()));
    assertTrue(coarse.permeabilityIndex() > fine.permeabilityIndex());
    assertTrue(coarse.porosityFraction() > fine.porosityFraction());
    assertTrue(sorted.permeabilityIndex() > fine.permeabilityIndex());
    assertTrue(sorted.porosityFraction() > fine.porosityFraction());
    assertTrue(sandy.erodibilityIndex() > coarse.erodibilityIndex());
    assertEquals(
        fine, ColluvialPhysicalState.derive(fineMatrix, porosity, permeability, erodibility));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ColluvialPhysicalState(fineMatrix, -0.1, 0.2, 0.3, 0.4, 0.5, 0.6));

    ColluvialCohesionState fineCohesion = fine.cohesionState();
    ColluvialCohesionState coarseCohesion = coarse.cohesionState();
    ColluvialCohesionState sandyCohesion = sandy.cohesionState();
    assertEquals(
        ColluvialCohesionState.CohesionClass.COHESIVE_FINE_MATRIX, fineCohesion.cohesionClass());
    assertEquals(ColluvialCohesionState.CohesionClass.NON_COHESIVE, coarseCohesion.cohesionClass());
    assertEquals(
        ColluvialCohesionState.CohesionClass.MIXED_COHESION, sandyCohesion.cohesionClass());
    assertTrue(fineCohesion.cohesionAdjustedErodibilityIndex() < fine.erodibilityIndex());
    assertTrue(coarseCohesion.cohesionIndex() < fineCohesion.cohesionIndex());
    assertEquals(fineCohesion, fine.cohesionState());
    for (ColluvialPhysicalState physicalState : List.of(fine, coarse, sorted, sandy)) {
      ColluvialHydraulicState hydraulicState = physicalState.hydraulicState();
      assertTrue(hydraulicState.waterStorageIndex() >= 0.0);
      assertTrue(hydraulicState.waterStorageIndex() <= 1.0);
      assertTrue(hydraulicState.infiltrationIndex() >= 0.0);
      assertTrue(hydraulicState.infiltrationIndex() <= 1.0);
      assertTrue(hydraulicState.drainageIndex() >= 0.0);
      assertTrue(hydraulicState.drainageIndex() <= 1.0);
      assertTrue(hydraulicState.runoffPartitionIndex() >= 0.0);
      assertTrue(hydraulicState.runoffPartitionIndex() <= 1.0);
      assertEquals(hydraulicState, physicalState.hydraulicState());
    }
    assertEquals(
        ColluvialHydraulicState.HydraulicClass.LOW_INFILTRATION,
        fine.hydraulicState().hydraulicClass());
    assertTrue(
        coarse.hydraulicState().infiltrationIndex() > fine.hydraulicState().infiltrationIndex());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialCohesionState(
                ColluvialCohesionState.CohesionClass.MIXED_COHESION, -0.1, 0.2, 0.3));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialGrainDispersionState(
                ColluvialGrainDispersionState.DispersionClass.BROAD_WITHIN_BIN_PROXY,
                0.0,
                0.0,
                0.0,
                0.0));
  }

  @Test
  void colluvialSedimentBudgetClosesCapacityAndDerivesDepositSharesFromDelivery() {
    StableId local = StableId.parse("00000000000000000000000000000c01");
    StableId far = StableId.parse("00000000000000000000000000000c02");
    SedimentGrainSize grainYield = new SedimentGrainSize(400_000L, 350_000L, 250_000L);
    ColluvialSedimentBudget.TerrainPath localPath = terrainPath(100.0);
    ColluvialSedimentBudget.TerrainPath farPath =
        terrainPath(100.0, 104.0, 103.0, 109.0, 108.0, 112.0, 116.0);
    ColluvialSedimentBudget.ProductionInput matrix =
        new ColluvialSedimentBudget.ProductionInput(
            350_000L, 8.0, 0.12, 0.8, 0.25, localPath, grainYield);
    ColluvialSedimentBudget.SourceProductionInput localInput =
        new ColluvialSedimentBudget.SourceProductionInput(
            local,
            0,
            new ColluvialSedimentBudget.ProductionInput(
                350_000L, 8.0, 0.12, 0.8, 0.25, localPath, grainYield));
    ColluvialSedimentBudget.SourceProductionInput farInput =
        new ColluvialSedimentBudget.SourceProductionInput(
            far,
            192,
            new ColluvialSedimentBudget.ProductionInput(
                300_000L, 8.0, 0.12, 0.8, 0.25, farPath, grainYield));

    ColluvialSedimentBudget budget =
        ColluvialSedimentBudget.derive(0.12, matrix, List.of(farInput, localInput));

    ColluvialHorizonState horizon = ColluvialHorizonState.from(budget);
    assertEquals(ColluvialHorizonState.ProfileClass.BALANCED_MIXED_PROFILE, horizon.profileClass());
    assertEquals(2.0 / 3.0, horizon.weatheringIndex(), 1.0e-15);
    assertEquals(427_991L, horizon.weatheredMatrixFractionPpm());
    assertEquals(572_009L, horizon.transportedSourceFractionPpm());
    assertTrue(horizon.matches(budget));

    assertEquals(
        ColluvialSedimentBudget.GrainTransportModel
            .SLOPE_ROUGHNESS_PATH_GRADE_RUNOFF_CONDITIONED_DRY_RAVEL_PROOF,
        budget.grainTransportModel());
    assertEquals(MaterialAssemblage.SCALE, budget.sourceCapacityFixedUnits());
    assertEquals(
        budget.sourceCapacityFixedUnits(),
        budget.retainedInventoryFixedUnits() + budget.mobilizedInventoryFixedUnits());
    assertEquals(
        budget.mobilizedInventoryFixedUnits(),
        budget.transportLossFixedUnits()
            + budget.bypassedInventoryFixedUnits()
            + budget.depositedInventoryFixedUnits());
    assertEquals(
        MaterialAssemblage.SCALE,
        budget.weatheredMatrixFractionPpm()
            + budget.sourceDepositShares().stream()
                .mapToLong(ColluvialSedimentBudget.SourceDepositShare::fractionPpm)
                .sum());
    assertTrue(budget.retainedInventoryFixedUnits() > 0);
    assertTrue(budget.transportLossFixedUnits() > 0);
    assertTrue(budget.bypassedInventoryFixedUnits() > 0);
    assertTrue(budget.depositedInventoryFixedUnits() > 0);
    assertEquals(375_000L, budget.mobilizedInventoryFixedUnits());
    assertEquals(625_000L, budget.retainedInventoryFixedUnits());
    assertEquals(68_335L, budget.transportLossFixedUnits());
    assertEquals(76_665L, budget.bypassedInventoryFixedUnits());
    assertEquals(230_000L, budget.depositedInventoryFixedUnits());
    assertEquals(427_991L, budget.weatheredMatrixFractionPpm());
    assertEquals(427_991L, budget.sourceFractionPpm(local, 0));
    assertEquals(144_018L, budget.sourceFractionPpm(far, 192));
    assertEquals(budget.sourceCapacityFixedUnits(), budget.capacityGrainMass().totalFixedUnits());
    assertEquals(
        budget.mobilizedInventoryFixedUnits(), budget.mobilizedGrainMass().totalFixedUnits());
    assertEquals(
        budget.retainedInventoryFixedUnits(), budget.retainedGrainMass().totalFixedUnits());
    assertEquals(
        budget.transportLossFixedUnits(), budget.transportLossGrainMass().totalFixedUnits());
    assertEquals(
        budget.bypassedInventoryFixedUnits(), budget.bypassedGrainMass().totalFixedUnits());
    assertEquals(
        budget.depositedInventoryFixedUnits(), budget.depositedGrainMass().totalFixedUnits());
    assertEquals(
        new ColluvialSedimentBudget.GrainMass(22_890L, 24_109L, 21_336L),
        budget.sourceBalances().getLast().balance().transportLossGrainMass());
    assertEquals(
        new ColluvialSedimentBudget.GrainMass(16_583L, 11_449L, 5_092L),
        budget.sourceBalances().getLast().balance().depositedGrainMass());
    assertEquals(new SedimentGrainSize(414_491L, 349_378L, 236_131L), budget.depositedGrainSize());
    ColluvialSedimentBudget.InputBalance farBalance = budget.sourceBalances().getLast().balance();
    assertEquals(
        ColluvialSinkState.SinkRole.INTERMEDIATE_ROUTE_STORAGE,
        farBalance.sinkState().transportLossSink());
    assertEquals(
        ColluvialSinkState.SinkRole.DOWNSTREAM_CONTINUATION, farBalance.sinkState().bypassSink());
    assertTrue(farBalance.sinkState().transportLossFraction() > 0.0);
    assertTrue(farBalance.sinkState().bypassFraction() > 0.0);
    assertEquals(
        ColluvialSinkState.SinkRole.NONE,
        budget.weatheredMatrixBalance().sinkState().transportLossSink());
    assertEquals(2, budget.sourceUsages().size());
    assertEquals(
        List.of(local, far),
        budget.sourceUsages().stream().map(ColluvialSourceUsage::sourceBodyId).toList());
    assertEquals(1, budget.sourceUsages().getFirst().trancheCount());
    assertEquals(350_000L, budget.sourceUsages().getFirst().claimedCapacityFixedUnits());
    assertEquals(300_000L, budget.sourceUsages().getLast().claimedCapacityFixedUnits());
    for (ColluvialSourceUsage usage : budget.sourceUsages()) {
      assertEquals(
          usage.claimedCapacityFixedUnits(),
          usage.retainedFixedUnits() + usage.mobilizedFixedUnits());
      assertEquals(
          usage.mobilizedFixedUnits(),
          usage.transportLossFixedUnits()
              + usage.bypassedFixedUnits()
              + usage.depositedFixedUnits());
    }
    assertEquals(2, budget.sourceGrainShares().size());
    assertEquals(
        List.of(0, 192),
        budget.sourceGrainShares().stream()
            .map(ColluvialSourceGrainShare::upslopeDistanceBlocks)
            .toList());
    ColluvialSedimentBudget.GrainMass reconstructedDepositedGrainMass =
        budget.weatheredMatrixBalance().depositedGrainMass();
    for (ColluvialSourceGrainShare grainShare : budget.sourceGrainShares()) {
      reconstructedDepositedGrainMass =
          reconstructedDepositedGrainMass.add(grainShare.depositedGrainMass());
    }
    assertEquals(budget.depositedGrainMass(), reconstructedDepositedGrainMass);
    assertEquals(3, budget.grainSourceShares().size());
    assertEquals(
        ColluvialGrainSourceShare.SourceRole.WEATHERED_MATRIX,
        budget.grainSourceShares().getFirst().sourceRole());
    assertTrue(budget.grainSourceShares().getFirst().sourceBodyId().isEmpty());
    for (int grainClass = 0; grainClass < 3; grainClass++) {
      int grainClassIndex = grainClass;
      long classTotal =
          switch (grainClass) {
            case 0 -> budget.depositedGrainMass().gravelAndCoarserFixedUnits();
            case 1 -> budget.depositedGrainMass().sandFixedUnits();
            case 2 -> budget.depositedGrainMass().finesFixedUnits();
            default -> throw new IllegalStateException("unmapped test grain class");
          };
      long sourceFractionTotal =
          budget.grainSourceShares().stream()
              .mapToLong(
                  share ->
                      switch (grainClassIndex) {
                        case 0 -> share.gravelAndCoarserFractionPpm();
                        case 1 -> share.sandFractionPpm();
                        case 2 -> share.finesFractionPpm();
                        default -> throw new IllegalStateException("unmapped test grain class");
                      })
              .sum();
      assertEquals(classTotal > 0 ? MaterialAssemblage.SCALE : 0, sourceFractionTotal);
    }
    ColluvialSedimentBudget repeatedSourceBudget =
        ColluvialSedimentBudget.derive(
            0.12,
            matrix,
            List.of(
                localInput,
                new ColluvialSedimentBudget.SourceProductionInput(local, 192, farInput.input())));
    assertEquals(1, repeatedSourceBudget.sourceUsages().size());
    assertEquals(2, repeatedSourceBudget.sourceUsages().getFirst().trancheCount());
    assertEquals(
        650_000L, repeatedSourceBudget.sourceUsages().getFirst().claimedCapacityFixedUnits());
    assertEquals(6, farPath.reachCount());
    assertEquals(18.0, farPath.cumulativeDownslopeReliefBlocks());
    assertEquals(2.0, farPath.cumulativeBarrierReliefBlocks());
    assertEquals(2.0 / 3.0, farPath.descendingReachFraction());
    assertEquals(47.0 / 60.0, farPath.downslopeContinuityIndex(), 1.0e-15);
    assertEquals(192.0, farPath.straightLineDistanceBlocks());
    assertEquals(192.0, farPath.routedDistanceBlocks());
    assertEquals(1.0, farPath.routeDirectnessIndex(), 1.0e-15);
    assertEquals(16.0, farPath.netUpslopeReliefBlocks(), 1.0e-15);
    assertEquals(25.0 / 72.0, farPath.routeGradeIndex(), 1.0e-15);
    assertEquals(new Point2(96.0, 0.0), farPath.pointAtRoutedDistance(96.0));
    assertEquals(farPath.originPoint(), farPath.pointAtRoutedDistance(0.0));
    assertEquals(farPath.sourcePoint(), farPath.pointAtRoutedDistance(192.0));
    ColluvialSinkAllocation farAllocation = farBalance.sinkAllocation();
    assertTrue(farAllocation.hasTransportLoss());
    assertTrue(farAllocation.hasBypass());
    assertTrue(farAllocation.transportLossDistanceBlocks() > 0.0);
    assertTrue(farAllocation.transportLossDistanceBlocks() < farAllocation.bypassDistanceBlocks());
    assertEquals(farPath.sourcePoint(), farAllocation.bypassPoint());
    assertEquals(farAllocation, farBalance.sinkAllocation());
    assertEquals(
        0.5 + 0.5 * (47.0 / 60.0) * (0.75 + 0.25 * (25.0 / 72.0)),
        farBalance.transportPathResponse(),
        1.0e-15);
    assertEquals(0.5276898871527778, farBalance.transportDistanceScale(), 1.0e-15);
    assertEquals(
        270.1772222222222, farBalance.grainTransportLengths().gravelAndCoarserBlocks(), 1.0e-12);
    assertEquals(202.63291666666666, farBalance.grainTransportLengths().sandBlocks(), 1.0e-12);
    assertEquals(135.0886111111111, farBalance.grainTransportLengths().finesBlocks(), 1.0e-12);
    assertTrue(
        budget.depositedGrainSize().gravelAndCoarserPpm() > grainYield.gravelAndCoarserPpm());
    assertTrue(budget.depositedGrainSize().finesPpm() < grainYield.finesPpm());
    for (ColluvialSedimentBudget.InputBalance balance :
        List.of(
            budget.weatheredMatrixBalance(),
            budget.sourceBalances().getFirst().balance(),
            budget.sourceBalances().getLast().balance())) {
      assertEquals(
          balance.capacityGrainMass(),
          balance.retainedGrainMass().add(balance.mobilizedGrainMass()));
      assertEquals(
          balance.mobilizedGrainMass(),
          balance
              .transportLossGrainMass()
              .add(balance.bypassedGrainMass())
              .add(balance.depositedGrainMass()));
    }
    assertEquals(
        budget, ColluvialSedimentBudget.derive(0.12, matrix, List.of(localInput, farInput)));

    ColluvialSedimentBudget lowResponse =
        singleSourceBudget(local, matrix, 96, 0.12, 4.0, 0.06, 0.2);
    long lowMobilized = lowResponse.sourceBalances().getFirst().balance().mobilizedFixedUnits();
    assertTrue(
        singleSourceBudget(local, matrix, 96, 0.12, 10.0, 0.06, 0.2)
                .sourceBalances()
                .getFirst()
                .balance()
                .mobilizedFixedUnits()
            > lowMobilized);
    ColluvialSedimentBudget steepSourceResponse =
        singleSourceBudget(local, matrix, 96, 0.12, 4.0, 0.20, 0.2);
    assertTrue(
        steepSourceResponse.sourceBalances().getFirst().balance().mobilizedFixedUnits()
            > lowMobilized);
    assertTrue(
        steepSourceResponse.sourceBalances().getFirst().balance().transportDistanceScale()
            > lowResponse.sourceBalances().getFirst().balance().transportDistanceScale());
    assertTrue(
        singleSourceBudget(local, matrix, 96, 0.12, 4.0, 0.06, 0.8)
                .sourceBalances()
                .getFirst()
                .balance()
                .mobilizedFixedUnits()
            > lowMobilized);
    ColluvialSedimentBudget.TerrainPath runoffPath = monotonicTerrainPath(96, 0.12);
    ColluvialSedimentBudget lowRunoffBudget =
        ColluvialSedimentBudget.derive(
            0.12,
            matrix,
            List.of(
                new ColluvialSedimentBudget.SourceProductionInput(
                    local,
                    96,
                    new ColluvialSedimentBudget.ProductionInput(
                        650_000L, 8.0, 0.12, 0.8, 0.25, 0.0, runoffPath, matrix.sedimentYield()))));
    ColluvialSedimentBudget highRunoffBudget =
        ColluvialSedimentBudget.derive(
            0.12,
            matrix,
            List.of(
                new ColluvialSedimentBudget.SourceProductionInput(
                    local,
                    96,
                    new ColluvialSedimentBudget.ProductionInput(
                        650_000L, 8.0, 0.12, 0.8, 0.25, 1.0, runoffPath, matrix.sedimentYield()))));
    ColluvialSedimentBudget.InputBalance lowRunoff =
        lowRunoffBudget.sourceBalances().getFirst().balance();
    ColluvialSedimentBudget.InputBalance highRunoff =
        highRunoffBudget.sourceBalances().getFirst().balance();
    assertTrue(lowRunoff.mobilizedFixedUnits() < highRunoff.mobilizedFixedUnits());
    assertTrue(lowRunoff.transportDistanceScale() < highRunoff.transportDistanceScale());
    ColluvialSedimentBudget gentleTarget =
        singleSourceBudget(local, matrix, 96, 0.02, 8.0, 0.12, 0.8);
    ColluvialSedimentBudget steepTarget =
        singleSourceBudget(local, matrix, 96, 0.24, 8.0, 0.12, 0.8);
    assertTrue(
        steepTarget.bypassedInventoryFixedUnits() > gentleTarget.bypassedInventoryFixedUnits());
    assertTrue(
        steepTarget.depositedInventoryFixedUnits() < gentleTarget.depositedInventoryFixedUnits());

    ColluvialSedimentBudget.InputBalance smoothSource =
        singleSourceBudget(local, matrix, 192, 0.12, 8.0, 0.12, 0.8, 0.0)
            .sourceBalances()
            .getFirst()
            .balance();
    ColluvialSedimentBudget.InputBalance roughSource =
        singleSourceBudget(local, matrix, 192, 0.12, 8.0, 0.12, 0.8, 1.0)
            .sourceBalances()
            .getFirst()
            .balance();
    assertEquals(smoothSource.mobilizedFixedUnits(), roughSource.mobilizedFixedUnits());
    assertTrue(smoothSource.transportDistanceScale() > roughSource.transportDistanceScale());
    assertTrue(smoothSource.transportLossFixedUnits() < roughSource.transportLossFixedUnits());
    assertTrue(
        Math.multiplyExact(
                roughSource.depositedGrainMass().gravelAndCoarserFixedUnits(),
                smoothSource.depositedFixedUnits())
            > Math.multiplyExact(
                smoothSource.depositedGrainMass().gravelAndCoarserFixedUnits(),
                roughSource.depositedFixedUnits()));

    ColluvialSedimentBudget.TerrainPath connectedPath =
        terrainPath(100.0, 104.0, 108.0, 112.0, 116.0, 120.0, 124.0);
    ColluvialSedimentBudget.TerrainPath barrierPath =
        terrainPath(100.0, 104.0, 102.0, 108.0, 106.0, 112.0, 110.0);
    ColluvialSedimentBudget.InputBalance connectedSource =
        singleSourceBudget(local, matrix, 192, 0.12, 8.0, 0.12, 0.8, 0.25, connectedPath)
            .sourceBalances()
            .getFirst()
            .balance();
    ColluvialSedimentBudget.InputBalance barrierSource =
        singleSourceBudget(local, matrix, 192, 0.12, 8.0, 0.12, 0.8, 0.25, barrierPath)
            .sourceBalances()
            .getFirst()
            .balance();
    assertEquals(1.0, connectedPath.downslopeContinuityIndex());
    assertEquals(6.0, barrierPath.cumulativeBarrierReliefBlocks());
    assertEquals(0.5, barrierPath.descendingReachFraction());
    assertEquals(27.0 / 44.0, barrierPath.downslopeContinuityIndex(), 1.0e-15);
    assertEquals(connectedSource.mobilizedFixedUnits(), barrierSource.mobilizedFixedUnits());
    assertTrue(connectedSource.transportPathResponse() > barrierSource.transportPathResponse());
    assertTrue(connectedSource.transportLossFixedUnits() < barrierSource.transportLossFixedUnits());
    assertTrue(
        Math.multiplyExact(
                barrierSource.depositedGrainMass().gravelAndCoarserFixedUnits(),
                connectedSource.depositedFixedUnits())
            > Math.multiplyExact(
                connectedSource.depositedGrainMass().gravelAndCoarserFixedUnits(),
                barrierSource.depositedFixedUnits()));

    ColluvialSedimentBudget.TerrainPath curvedPath =
        new ColluvialSedimentBudget.TerrainPath(
            32,
            List.of(
                new ColluvialSedimentBudget.TerrainPathSample(0, new Point2(0.0, 0.0), 100.0),
                new ColluvialSedimentBudget.TerrainPathSample(32, new Point2(32.0, 0.0), 104.0),
                new ColluvialSedimentBudget.TerrainPathSample(64, new Point2(32.0, 32.0), 108.0)));
    assertEquals(64, curvedPath.distanceBlocks());
    assertEquals(StrictMath.sqrt(2.0) * 32.0, curvedPath.straightLineDistanceBlocks(), 1.0e-12);
    assertEquals(64.0, curvedPath.routedDistanceBlocks());
    assertEquals(StrictMath.sqrt(0.5), curvedPath.routeDirectnessIndex(), 1.0e-12);
    assertEquals(8.0, curvedPath.netUpslopeReliefBlocks(), 1.0e-12);
    assertEquals(25.0 / 48.0, curvedPath.routeGradeIndex(), 1.0e-12);
    assertEquals(90.0, curvedPath.maximumDeflectionFromInitialDegrees(), 1.0e-12);
    assertEquals(2, curvedPath.reaches().size());
    assertEquals(new Point2(1.0, 0.0), curvedPath.reaches().getFirst().routedUpslopeDirection());
    assertEquals(new Point2(0.0, 1.0), curvedPath.reaches().getLast().routedUpslopeDirection());
    ColluvialSedimentBudget curvedBudget =
        singleSourceBudget(local, matrix, 64, 0.12, 8.0, 0.12, 0.8, 0.25, curvedPath);
    assertEquals(
        0.5 + 0.5 * StrictMath.sqrt(0.5) * (0.75 + 0.25 * (25.0 / 48.0)),
        curvedBudget.sourceBalances().getFirst().balance().transportPathResponse(),
        1.0e-15);

    ColluvialSedimentBudget.TerrainPath gentleGradePath = terrainPath(100.0, 104.0, 108.0, 112.0);
    ColluvialSedimentBudget.TerrainPath steepGradePath = terrainPath(100.0, 112.0, 124.0, 136.0);
    ColluvialSedimentBudget.InputBalance gentleGrade =
        singleSourceBudget(local, matrix, 96, 0.12, 8.0, 0.12, 0.8, 0.25, gentleGradePath)
            .sourceBalances()
            .getFirst()
            .balance();
    ColluvialSedimentBudget.InputBalance steepGrade =
        singleSourceBudget(local, matrix, 96, 0.12, 8.0, 0.12, 0.8, 0.25, steepGradePath)
            .sourceBalances()
            .getFirst()
            .balance();
    assertTrue(steepGradePath.routeGradeIndex() > gentleGradePath.routeGradeIndex());
    assertTrue(steepGrade.transportPathResponse() > gentleGrade.transportPathResponse());
    assertTrue(steepGrade.transportDistanceScale() > gentleGrade.transportDistanceScale());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSedimentBudget.TerrainPath(
                32,
                List.of(
                    new ColluvialSedimentBudget.TerrainPathSample(0, new Point2(0.0, 0.0), 100.0),
                    new ColluvialSedimentBudget.TerrainPathSample(
                        32, new Point2(32.0, 0.0), 104.0)),
                List.of(
                    new ColluvialSedimentBudget.TerrainPathReach(
                        0,
                        new Point2(0.0, 0.0),
                        new Point2(32.0, 0.0),
                        new Point2(1.0, 0.0),
                        new Point2(0.0, 1.0),
                        false,
                        false))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSedimentBudget.TerrainPathReach(
                0,
                new Point2(0.0, 0.0),
                new Point2(32.0, 0.0),
                new Point2(2.0, 0.0),
                new Point2(1.0, 0.0),
                false,
                false));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSedimentBudget.ProductionInput(
                -1L, 8.0, 0.12, 0.8, 0.25, localPath, grainYield));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSedimentBudget.ProductionInput(
                350_000L, 8.0, 0.12, 0.8, 1.01, localPath, grainYield));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSedimentBudget.TerrainPath(
                32,
                List.of(
                    new ColluvialSedimentBudget.TerrainPathSample(0, new Point2(0.0, 0.0), 100.0),
                    new ColluvialSedimentBudget.TerrainPathSample(
                        64, new Point2(64.0, 0.0), 104.0))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSedimentBudget.TerrainPath(
                32,
                List.of(
                    new ColluvialSedimentBudget.TerrainPathSample(
                        0, new Point2(0.0, 0.0), -Double.MAX_VALUE),
                    new ColluvialSedimentBudget.TerrainPathSample(
                        32, new Point2(32.0, 0.0), Double.MAX_VALUE))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ColluvialSedimentBudget.TerrainPath(
                32,
                List.of(
                    new ColluvialSedimentBudget.TerrainPathSample(0, new Point2(0.0, 0.0), 100.0),
                    new ColluvialSedimentBudget.TerrainPathSample(
                        32, new Point2(31.0, 0.0), 104.0))));
    assertThrows(
        IllegalArgumentException.class, () -> new ColluvialSedimentBudget.GrainMass(-1L, 1L, 0L));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ColluvialSedimentBudget.derive(
                0.12,
                new ColluvialSedimentBudget.ProductionInput(
                    349_999L, 8.0, 0.12, 0.8, 0.25, localPath, grainYield),
                List.of(localInput, farInput)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ColluvialSedimentBudget.derive(
                0.12,
                matrix,
                List.of(
                    localInput,
                    new ColluvialSedimentBudget.SourceProductionInput(far, 0, farInput.input()))));
    ColluvialSedimentBudget.TerrainPath shiftedFarPath =
        terrainPath(new Point2(1.0, 0.0), 100.0, 104.0, 103.0, 109.0, 108.0, 112.0, 116.0);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ColluvialSedimentBudget.derive(
                0.12,
                matrix,
                List.of(
                    localInput,
                    new ColluvialSedimentBudget.SourceProductionInput(
                        far,
                        192,
                        new ColluvialSedimentBudget.ProductionInput(
                            300_000L, 8.0, 0.12, 0.8, 0.25, shiftedFarPath, grainYield)))));
  }

  private static ColluvialSedimentBudget singleSourceBudget(
      StableId source,
      ColluvialSedimentBudget.ProductionInput matrix,
      int distance,
      double depositionSlope,
      double weatheringDepth,
      double sourceSlope,
      double erodibility) {
    return singleSourceBudget(
        source,
        matrix,
        distance,
        depositionSlope,
        weatheringDepth,
        sourceSlope,
        erodibility,
        matrix.terrainRoughnessIndex());
  }

  private static ColluvialSedimentBudget singleSourceBudget(
      StableId source,
      ColluvialSedimentBudget.ProductionInput matrix,
      int distance,
      double depositionSlope,
      double weatheringDepth,
      double sourceSlope,
      double erodibility,
      double terrainRoughnessIndex) {
    return singleSourceBudget(
        source,
        matrix,
        distance,
        depositionSlope,
        weatheringDepth,
        sourceSlope,
        erodibility,
        terrainRoughnessIndex,
        monotonicTerrainPath(distance, sourceSlope));
  }

  private static ColluvialSedimentBudget singleSourceBudget(
      StableId source,
      ColluvialSedimentBudget.ProductionInput matrix,
      int distance,
      double depositionSlope,
      double weatheringDepth,
      double sourceSlope,
      double erodibility,
      double terrainRoughnessIndex,
      ColluvialSedimentBudget.TerrainPath terrainPath) {
    return ColluvialSedimentBudget.derive(
        depositionSlope,
        matrix,
        List.of(
            new ColluvialSedimentBudget.SourceProductionInput(
                source,
                distance,
                new ColluvialSedimentBudget.ProductionInput(
                    650_000L,
                    weatheringDepth,
                    sourceSlope,
                    erodibility,
                    terrainRoughnessIndex,
                    terrainPath,
                    matrix.sedimentYield()))));
  }

  private static ColluvialSedimentBudget.TerrainPath monotonicTerrainPath(
      int distanceBlocks, double slope) {
    if (distanceBlocks % 32 != 0) {
      throw new IllegalArgumentException("test terrain-path distance must be divisible by 32");
    }
    double[] elevations = new double[distanceBlocks / 32 + 1];
    for (int index = 0; index < elevations.length; index++) {
      elevations[index] = 100.0 + index * 32.0 * slope;
    }
    return terrainPath(elevations);
  }

  private static ColluvialSedimentBudget.TerrainPath terrainPath(double... elevations) {
    return terrainPath(new Point2(0.0, 0.0), elevations);
  }

  private static ColluvialSedimentBudget.TerrainPath terrainPath(
      Point2 origin, double... elevations) {
    List<ColluvialSedimentBudget.TerrainPathSample> samples = new java.util.ArrayList<>();
    for (int index = 0; index < elevations.length; index++) {
      samples.add(
          new ColluvialSedimentBudget.TerrainPathSample(
              index * 32, origin.add(index * 32.0, 0.0), elevations[index]));
    }
    return new ColluvialSedimentBudget.TerrainPath(32, samples);
  }

  @Test
  void triangularPropertyDistributionIsBoundedAndHonorsItsMode() {
    UnitIntervalDistribution distribution = new UnitIntervalDistribution(0.1, 0.3, 0.8);

    assertEquals(0.1, distribution.sample(0.0));
    assertEquals(0.3, distribution.sample((0.3 - 0.1) / (0.8 - 0.1)), 1.0e-15);
    assertTrue(distribution.contains(distribution.sample(0.999_999)));
    assertEquals(0.4, new UnitIntervalDistribution(0.4, 0.4, 0.4).sample(0.75));
    assertThrows(IllegalArgumentException.class, () -> new UnitIntervalDistribution(0.4, 0.2, 0.8));
  }

  @Test
  void alterationRecipesSelectByProtolithFamilyAndRequireExactCoverage() {
    MaterialAssemblage felsic = assemblage("test:felsic");
    MaterialAssemblage mafic = assemblage("test:mafic");
    AlterationAssemblageRecipe first =
        new AlterationAssemblageRecipe(
            List.of(GeneticFamily.IGNEOUS, GeneticFamily.SEDIMENTARY), felsic);
    AlterationAssemblageRecipe second =
        new AlterationAssemblageRecipe(
            List.of(GeneticFamily.METAMORPHIC, GeneticFamily.HYDROTHERMAL, GeneticFamily.SURFICIAL),
            mafic);

    AlterationDefinition definition = alteration(List.of(first, second));

    assertSame(felsic, definition.targetAssemblage(GeneticFamily.IGNEOUS));
    assertSame(felsic, definition.targetAssemblage(GeneticFamily.SEDIMENTARY));
    assertSame(mafic, definition.targetAssemblage(GeneticFamily.METAMORPHIC));
    assertThrows(IllegalArgumentException.class, () -> alteration(List.of(first)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            alteration(
                List.of(
                    first,
                    second,
                    new AlterationAssemblageRecipe(List.of(GeneticFamily.IGNEOUS), mafic))));
    assertThrows(
        IllegalArgumentException.class, () -> alteration(List.of(first, second), Optional.empty()));
  }

  @Test
  void fluidStateRequiresBoundedIndependentTransportAxes() {
    ProcessFluidState state = fluidState();

    assertEquals(3, state.ligandCapacities().chloride());
    assertEquals(2, state.ligandCapacities().reducedSulfur());
    assertEquals(3, state.integratedFluxClass());
    assertThrows(IllegalArgumentException.class, () -> new LigandCapacities(4, 0, 0, 0));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ProcessFluidState(
                state.medium(),
                state.redox(),
                state.acidity(),
                state.salinity(),
                state.sulfurState(),
                state.ligandCapacities(),
                -1));
  }

  @Test
  void responseTextureIsRequiredExactlyForIsochemicalMetamorphism() {
    AlterationDefinition hornfels =
        new AlterationDefinition(
            Overprint.CONTACT_HORNFELS,
            MaterialProcessClass.ISOCHEMICAL_METAMORPHISM,
            Optional.empty(),
            0,
            List.of(),
            Optional.of(RockTexture.HORNFELSIC),
            MetamorphicFacies.HORNBLENDE_HORNFELS,
            MetamorphicPath.CONTACT_LOW_P,
            500.0,
            700.0,
            100.0,
            300.0,
            0.7,
            -0.08);

    assertEquals(RockTexture.HORNFELSIC, hornfels.responseTexture().orElseThrow());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AlterationDefinition(
                hornfels.overprint(),
                hornfels.processClass(),
                hornfels.fluidState(),
                hornfels.replacementPpm(),
                hornfels.targetRecipes(),
                Optional.empty(),
                hornfels.facies(),
                hornfels.path(),
                hornfels.minimumTemperatureCelsius(),
                hornfels.maximumTemperatureCelsius(),
                hornfels.minimumPressureMpa(),
                hornfels.maximumPressureMpa(),
                hornfels.porosityMultiplier(),
                hornfels.erodibilityDelta()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AlterationDefinition(
                Overprint.NONE,
                MaterialProcessClass.NONE,
                Optional.empty(),
                0,
                List.of(),
                Optional.of(RockTexture.HORNFELSIC),
                MetamorphicFacies.NONE,
                MetamorphicPath.NONE,
                0.0,
                0.0,
                0.0,
                0.0,
                1.0,
                0.0));
  }

  @Test
  void primaryMetamorphismIsRequiredOnlyForMetamorphicRockRecipes() {
    PrimaryMetamorphicDefinition metamorphism =
        new PrimaryMetamorphicDefinition(
            "test:shale",
            MetamorphicGrade.LOW,
            MetamorphicFacies.GREENSCHIST,
            MetamorphicPath.COLLISION_CLOCKWISE,
            250.0,
            450.0,
            200.0,
            600.0);
    UnitIntervalDistribution property = new UnitIntervalDistribution(0.1, 0.2, 0.3);
    MaterialAssemblage assemblage = assemblage("test:quartz");

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PrimaryMetamorphicDefinition(
                "test:shale",
                MetamorphicGrade.NONE,
                MetamorphicFacies.GREENSCHIST,
                MetamorphicPath.COLLISION_CLOCKWISE,
                250.0,
                450.0,
                200.0,
                600.0));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RockDefinition(
                "test:metamorphic",
                Lithology.GRANITIC_GNEISS,
                GeneticFamily.METAMORPHIC,
                RockTexture.FOLIATED_CRYSTALLINE,
                Optional.empty(),
                assemblage,
                0.0,
                List.of(),
                sedimentYield(),
                property,
                property,
                property));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RockDefinition(
                "test:igneous",
                Lithology.FELSIC_STOCK,
                GeneticFamily.IGNEOUS,
                RockTexture.PHANERITIC_CRYSTALLINE,
                Optional.of(metamorphism),
                assemblage,
                0.0,
                List.of(),
                sedimentYield(),
                property,
                property,
                property));
  }

  @Test
  void modalVariationAxesConserveMassAndRespectTheRockSpreadEnvelope() {
    ModalVariationAxis axis =
        new ModalVariationAxis(
            "quartz_feldspar_balance", Map.of("test:quartz", 50_000L, "test:feldspar", -50_000L));
    MaterialAssemblage central =
        new MaterialAssemblage(Map.of("test:quartz", 500_000L, "test:feldspar", 500_000L));

    RockDefinition rock = rock(central, 0.1, List.of(axis));

    assertEquals(
        0L,
        rock.modalVariationAxes().getFirst().loadingsPpm().values().stream()
            .mapToLong(Long::longValue)
            .sum());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ModalVariationAxis(
                "unbalanced", Map.of("test:quartz", 50_000L, "test:feldspar", -49_999L)));
    ModalVariationAxis excessive =
        new ModalVariationAxis(
            "excessive", Map.of("test:quartz", 50_001L, "test:feldspar", -50_001L));
    assertThrows(IllegalArgumentException.class, () -> rock(central, 0.1, List.of(excessive)));
    assertThrows(IllegalArgumentException.class, () -> rock(central, 0.1, List.of(axis, axis)));
  }

  @Test
  void bodySamplerFollowsAuthoredCorrelationWithoutLeakingModalMass() {
    MaterialAssemblage central =
        new MaterialAssemblage(
            Map.of(
                "test:quartz", 400_000L,
                "test:albite", 300_000L,
                "test:orthoclase", 300_000L));
    ModalVariationAxis axis =
        new ModalVariationAxis(
            "quartz_feldspar_balance",
            Map.of(
                "test:quartz", 60_000L,
                "test:albite", -30_000L,
                "test:orthoclase", -30_000L));
    RockDefinition rock = rock(central, 0.15, List.of(axis));
    BodyCompositionSampler sampler =
        new BodyCompositionSampler(
            new WorldIdentity(17L, "test-model", "test-digest", "test:profile"));
    boolean sawPositive = false;
    boolean sawNegative = false;

    for (int index = 1; index <= 128; index++) {
      MaterialAssemblage sampled = sampler.sample(rock, StableId.parse("%032x".formatted(index)));
      long quartzDelta = sampled.modesPpm().get("test:quartz") - 400_000L;
      long albiteDelta = sampled.modesPpm().get("test:albite") - 300_000L;
      long orthoclaseDelta = sampled.modesPpm().get("test:orthoclase") - 300_000L;

      assertEquals(0L, quartzDelta + albiteDelta + orthoclaseDelta);
      assertTrue(StrictMath.abs(albiteDelta - orthoclaseDelta) <= 1L);
      assertTrue(quartzDelta == 0L || Long.signum(quartzDelta) == -Long.signum(albiteDelta));
      assertTrue(StrictMath.abs(quartzDelta) <= 60_000L);
      sawPositive |= quartzDelta > 0L;
      sawNegative |= quartzDelta < 0L;
    }
    assertTrue(sawPositive && sawNegative);
  }

  @Test
  void weightedAssemblageBlendIsExactAndIndependentOfShareOrder() {
    MaterialAssemblage quartz = assemblage("test:quartz");
    MaterialAssemblage feldspar = assemblage("test:feldspar");
    MaterialAssemblage clay = assemblage("test:clay");
    List<MaterialAssemblage.Share> shares =
        List.of(
            new MaterialAssemblage.Share(quartz, 333_333L),
            new MaterialAssemblage.Share(feldspar, 333_333L),
            new MaterialAssemblage.Share(clay, 333_334L));

    MaterialAssemblage blended = MaterialAssemblage.weightedBlend(shares);

    assertEquals(
        Map.of("test:quartz", 333_333L, "test:feldspar", 333_333L, "test:clay", 333_334L),
        blended.modesPpm());
    assertEquals(
        blended,
        MaterialAssemblage.weightedBlend(List.of(shares.get(2), shares.get(0), shares.get(1))));
    MaterialAssemblage mixedA =
        new MaterialAssemblage(Map.of("test:quartz", 500_001L, "test:feldspar", 499_999L));
    MaterialAssemblage mixedB =
        new MaterialAssemblage(Map.of("test:quartz", 333_333L, "test:clay", 666_667L));
    MaterialAssemblage mixedC =
        new MaterialAssemblage(Map.of("test:feldspar", 250_001L, "test:clay", 749_999L));
    List<MaterialAssemblage.Share> roundedShares =
        List.of(
            new MaterialAssemblage.Share(mixedA, 333_333L),
            new MaterialAssemblage.Share(mixedB, 333_333L),
            new MaterialAssemblage.Share(mixedC, 333_334L));
    MaterialAssemblage rounded = MaterialAssemblage.weightedBlend(roundedShares);
    assertEquals(
        Map.of("test:quartz", 277_778L, "test:feldspar", 250_000L, "test:clay", 472_222L),
        rounded.modesPpm());
    assertEquals(
        rounded,
        MaterialAssemblage.weightedBlend(
            List.of(roundedShares.get(2), roundedShares.get(0), roundedShares.get(1))));
    assertEquals(
        MaterialAssemblage.blend(quartz, feldspar, 500_000L),
        MaterialAssemblage.weightedBlend(
            List.of(
                new MaterialAssemblage.Share(quartz, 500_000L),
                new MaterialAssemblage.Share(feldspar, 500_000L))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            MaterialAssemblage.weightedBlend(
                List.of(new MaterialAssemblage.Share(quartz, 999_999L))));
  }

  private static AlterationDefinition alteration(List<AlterationAssemblageRecipe> recipes) {
    return alteration(recipes, Optional.of(fluidState()));
  }

  private static AlterationDefinition alteration(
      List<AlterationAssemblageRecipe> recipes, Optional<ProcessFluidState> fluidState) {
    return new AlterationDefinition(
        Overprint.POTASSIC_ALTERATION,
        MaterialProcessClass.HYDROTHERMAL_METASOMATISM,
        fluidState,
        250_000,
        recipes,
        Optional.empty(),
        MetamorphicFacies.NONE,
        MetamorphicPath.NONE,
        300.0,
        500.0,
        50.0,
        150.0,
        1.0,
        0.0);
  }

  private static ProcessFluidState fluidState() {
    return new ProcessFluidState(
        FluidMedium.MAGMATIC_HYDROTHERMAL,
        RedoxClass.OXIDIZING,
        AcidityClass.NEAR_NEUTRAL,
        SalinityClass.CONCENTRATED_BRINE,
        SulfurState.REDUCED_SULFUR_BUFFERED,
        new LigandCapacities(3, 2, 1, 2),
        3);
  }

  private static RockDefinition rock(
      MaterialAssemblage central, double spread, List<ModalVariationAxis> axes) {
    UnitIntervalDistribution property = new UnitIntervalDistribution(0.1, 0.2, 0.3);
    return new RockDefinition(
        "test:rock",
        Lithology.GRANITIC_GNEISS,
        GeneticFamily.METAMORPHIC,
        RockTexture.FOLIATED_CRYSTALLINE,
        Optional.of(
            new PrimaryMetamorphicDefinition(
                "test:protolith",
                MetamorphicGrade.HIGH,
                MetamorphicFacies.AMPHIBOLITE,
                MetamorphicPath.COLLISION_CLOCKWISE,
                600.0,
                750.0,
                400.0,
                800.0)),
        central,
        spread,
        axes,
        sedimentYield(),
        property,
        property,
        property);
  }

  private static MaterialAssemblage assemblage(String mineral) {
    return new MaterialAssemblage(Map.of(mineral, MaterialAssemblage.SCALE));
  }

  private static SedimentGrainSize sedimentYield() {
    return new SedimentGrainSize(400_000L, 400_000L, 200_000L);
  }
}
