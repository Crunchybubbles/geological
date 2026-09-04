package io.github.crunchybubbles.geological;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.ProvinceGrammar;
import io.github.crunchybubbles.geological.mineral.BifSystemState;
import io.github.crunchybubbles.geological.mineral.EvaporitePotashState;
import io.github.crunchybubbles.geological.mineral.FormationStatus;
import io.github.crunchybubbles.geological.mineral.GateStatus;
import io.github.crunchybubbles.geological.mineral.LctPegmatiteState;
import io.github.crunchybubbles.geological.mineral.MineralSystemDecision;
import io.github.crunchybubbles.geological.mineral.MineralSystemProofs;
import io.github.crunchybubbles.geological.mineral.MineralSystemValidationReport;
import io.github.crunchybubbles.geological.mineral.PlacerSystemState;
import io.github.crunchybubbles.geological.mineral.PorphyryFluidMetalState;
import io.github.crunchybubbles.geological.mineral.PorphyrySystemState;
import io.github.crunchybubbles.geological.mineral.SupergeneCopperState;
import io.github.crunchybubbles.geological.mineral.VmsSystemState;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.query.GeologyQueryEngine;
import io.github.crunchybubbles.geological.query.Phase0World;
import io.github.crunchybubbles.geological.query.Phase1World;
import java.util.List;
import org.junit.jupiter.api.Test;

class MineralSystemProofTest {
  @Test
  void proofCatalogContainsFormedAndExplicitlyRejectedOutcomes() {
    GeologyQueryEngine query = Phase0World.create(8_675_309L);
    Province province = query.atlas().provinceAt(new Point2(0.0, 0.0));
    List<MineralSystemDecision> decisions = query.mineralDecisions(province);
    assertEquals(6, decisions.size());
    assertEquals(
        3, decisions.stream().filter(result -> result.status() == FormationStatus.FORMED).count());
    assertEquals(
        3, decisions.stream().filter(result -> result.status() != FormationStatus.FORMED).count());

    decisions.stream()
        .filter(result -> result.status() == FormationStatus.FORMED)
        .forEach(
            result -> {
              assertTrue(
                  result.gates().stream().allMatch(gate -> gate.status() == GateStatus.PASS));
              assertFalse(result.provenance().isEmpty());
              assertEquals(
                  result.ledger().sourceAmount(),
                  result.ledger().allocations().values().stream().mapToLong(Long::longValue).sum());
            });
    decisions.stream()
        .filter(result -> result.status() != FormationStatus.FORMED)
        .forEach(
            result ->
                assertTrue(
                    result.gates().stream().anyMatch(gate -> gate.status() == GateStatus.FAIL)));
  }

  @Test
  void placerNamesTheFormedPorphyryAsItsUpstreamSource() {
    GeologyQueryEngine query = Phase0World.create(26L);
    Province province = query.atlas().provinceAt(new Point2(0.0, 0.0));
    List<MineralSystemDecision> decisions = query.mineralDecisions(province);
    MineralSystemDecision porphyry = formed(decisions, MineralSystemProofs.PORPHYRY_MODEL);
    MineralSystemDecision placer = formed(decisions, MineralSystemProofs.PLACER_MODEL);
    assertTrue(placer.deposit().sourceIds().contains(porphyry.deposit().id()));
    assertTrue(
        placer.provenance().stream()
            .anyMatch(step -> step.inputIds().contains(porphyry.deposit().id())));
  }

  @Test
  void formedPorphyryPublishesLinkedTopologyAndAlterationZoning() {
    GeologyQueryEngine query = Phase0World.create(8_675_309L);
    Province province = query.atlas().provinceAt(new Point2(0.0, 0.0));
    PorphyrySystemState state = query.porphyrySystemState(province);

    assertEquals(FormationStatus.FORMED, state.status());
    assertEquals(province.geometry().plutonPulses().getLast().id(), state.intrusionId());
    assertEquals(
        PorphyrySystemState.FluidSourceClass.MAGMATIC_HYDROTHERMAL, state.fluidSourceClass());
    assertEquals(PorphyrySystemState.StockworkClass.CONNECTED_STOCKWORK, state.stockworkClass());
    assertEquals(3, state.alterationZones().size());
    Point3 center = state.localCenter();
    assertEquals(
        PorphyrySystemState.AlterationZoneKind.POTASSIC_CORE,
        state.zoneAt(center).orElseThrow().kind());
    assertEquals(
        PorphyrySystemState.AlterationZoneKind.PHYLLIC_INTERMEDIATE,
        state.zoneAt(new Point3(center.x() + 90.0, center.y(), center.z())).orElseThrow().kind());
    assertEquals(
        PorphyrySystemState.AlterationZoneKind.PROPYLITIC_DISTAL,
        state.zoneAt(new Point3(center.x() + 170.0, center.y(), center.z())).orElseThrow().kind());
    assertTrue(state.zoneAt(new Point3(center.x() + 250.0, center.y(), center.z())).isEmpty());
    assertEquals(1_000_000L, state.sourceBudgetFixedUnits());
    assertEquals(105_000L, state.depositAllocationFixedUnits());
    assertTrue(state.failedGate().isEmpty());
  }

  @Test
  void barrenPorphyryPublishesDisconnectedTopologyWithFailedGate() {
    GeologyQueryEngine query = Phase1World.create(8_675_309L);
    Province province =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC);
    PorphyrySystemState state = query.porphyrySystemState(province);

    assertEquals(FormationStatus.BARREN_SYSTEM, state.status());
    assertEquals(PorphyrySystemState.StockworkClass.DISCONNECTED_STOCKWORK, state.stockworkClass());
    assertTrue(state.alterationZones().isEmpty());
    assertEquals("source", state.failedGate().orElseThrow());
    assertEquals(0L, state.depositAllocationFixedUnits());
  }

  @Test
  void formedVmsPublishesSynvolcanicLensAndFeederGeometry() {
    GeologyQueryEngine query = Phase0World.create(8_675_309L);
    Province province = query.atlas().provinceAt(new Point2(0.0, 0.0));
    VmsSystemState state = query.vmsSystemState(province);

    assertEquals(FormationStatus.FORMED, state.status());
    assertEquals(province.geometry().basin().id(), state.basinId());
    assertEquals(
        VmsSystemState.FluidSourceClass.SEAWATER_DOMINATED_HYDROTHERMAL, state.fluidSourceClass());
    assertEquals(VmsSystemState.GeometryClass.STRATIFORM_LENS_WITH_FEEDER, state.geometryClass());
    assertEquals(241.0, state.seafloorAge().ageMa());
    Point3 center = state.localCenter();
    assertEquals(
        VmsSystemState.VmsZone.STRATIFORM_MASSIVE_SULFIDE_LENS, state.zoneAt(center).orElseThrow());
    assertEquals(
        VmsSystemState.VmsZone.CHLORITIC_FEEDER,
        state.zoneAt(new Point3(center.x(), center.y() - 50.0, center.z())).orElseThrow());
    assertTrue(state.zoneAt(new Point3(center.x() + 150.0, center.y(), center.z())).isEmpty());
    assertEquals(800_000L, state.sourceBudgetFixedUnits());
    assertEquals(92_000L, state.depositAllocationFixedUnits());
    assertTrue(state.failedGate().isEmpty());
  }

  @Test
  void barrenVmsRetainsBasinContextButRejectsMissingSynvolcanicDriver() {
    GeologyQueryEngine query = Phase1World.create(8_675_309L);
    Province province =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC);
    VmsSystemState state = query.vmsSystemState(province);

    assertEquals(FormationStatus.BARREN_SYSTEM, state.status());
    assertEquals(VmsSystemState.GeometryClass.NO_LENS, state.geometryClass());
    assertEquals(VmsSystemState.FluidSourceClass.NO_COEVAL_FLUID, state.fluidSourceClass());
    assertEquals("driver", state.failedGate().orElseThrow());
    assertTrue(state.zoneAt(state.localCenter()).isEmpty());
  }

  @Test
  void evolvedLineagePublishesLctPegmatiteChildBodyAndInternalZones() {
    GeologyQueryEngine query = Phase0World.create(8_675_309L);
    Province province = query.atlas().provinceAt(new Point2(0.0, 0.0));
    LctPegmatiteState state = query.lctPegmatiteState(province);

    assertEquals(FormationStatus.FORMED, state.status());
    assertEquals(province.geometry().plutonPulses().getLast().id(), state.parentIntrusionId());
    assertEquals(LctPegmatiteState.FertilityClass.LCT_RARE_ELEMENT, state.fertilityClass());
    assertEquals(
        LctPegmatiteState.EmplacementClass.APICAL_FRACTURE_DIKE_SWARM, state.emplacementClass());
    assertEquals(
        LctPegmatiteState.FluidSourceClass.RESIDUAL_VOLATILE_MELT, state.fluidSourceClass());
    assertEquals(0.85, state.differentiationProgress(), 1.0e-12);
    assertEquals(3, state.internalZones().size());
    Point3 center = state.localCenter();
    assertEquals(
        LctPegmatiteState.ZoneClass.QUARTZ_CORE, state.zoneAt(center).orElseThrow().kind());
    assertEquals(
        LctPegmatiteState.ZoneClass.INTERMEDIATE,
        state.zoneAt(new Point3(center.x() + 20.0, center.y(), center.z())).orElseThrow().kind());
    assertEquals(
        LctPegmatiteState.ZoneClass.WALL,
        state.zoneAt(new Point3(center.x() + 30.0, center.y(), center.z())).orElseThrow().kind());
    assertTrue(state.zoneAt(new Point3(center.x() + 50.0, center.y(), center.z())).isEmpty());
    assertTrue(state.childAllocationFixedUnits() <= state.sourceBudgetFixedUnits());
    assertTrue(state.failedGate().isEmpty());
  }

  @Test
  void barrenLineageCannotManufactureLctPegmatiteFertility() {
    GeologyQueryEngine query = Phase1World.create(8_675_309L);
    Province province =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC);
    LctPegmatiteState state = query.lctPegmatiteState(province);

    assertEquals(FormationStatus.BARREN_SYSTEM, state.status());
    assertEquals(LctPegmatiteState.FertilityClass.UNRESOLVED_FERTILITY, state.fertilityClass());
    assertEquals("lineage", state.failedGate().orElseThrow());
    assertTrue(state.internalZones().isEmpty());
    assertEquals(0L, state.childAllocationFixedUnits());
  }

  @Test
  void formedBifPublishesAncientRedoxBoundedStratiformSheet() {
    GeologyQueryEngine query = Phase0World.create(8_675_309L);
    Province province = query.atlas().provinceAt(new Point2(0.0, 0.0));
    BifSystemState state = query.bifSystemState(province);

    assertEquals(FormationStatus.FORMED, state.status());
    assertEquals(BifSystemState.BifType.ALGOMA_TYPE, state.type());
    assertEquals(BifSystemState.GeometryClass.BANDED_STRATIFORM_SHEET, state.geometryClass());
    assertEquals(2_500.0, state.formationAge().ageMa());
    assertEquals(
        io.github.crunchybubbles.geological.petrology.RedoxClass.REDUCING, state.oceanRedoxClass());
    assertTrue(state.contains(state.localCenter()));
    assertTrue(state.sheetAllocationFixedUnits() <= state.sourceBudgetFixedUnits());
    assertTrue(state.failedGate().isEmpty());
  }

  @Test
  void barrenBifRetainsCandidateIdentityWithoutInventingAgedOceanChemistry() {
    GeologyQueryEngine query = Phase1World.create(8_675_309L);
    Province province =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC);
    BifSystemState state = query.bifSystemState(province);

    assertEquals(FormationStatus.BARREN_SYSTEM, state.status());
    assertEquals(BifSystemState.BifType.UNRESOLVED, state.type());
    assertEquals(BifSystemState.GeometryClass.NO_SHEET, state.geometryClass());
    assertEquals("volcano_sedimentary_basin", state.failedGate().orElseThrow());
    assertFalse(state.contains(state.localCenter()));
  }

  @Test
  void formedEvaporitePublishesRestrictedRefloodedBrineSequence() {
    GeologyQueryEngine query = Phase0World.create(8_675_309L);
    Province province = query.atlas().provinceAt(new Point2(0.0, 0.0));
    EvaporitePotashState state = query.evaporitePotashState(province);

    assertEquals(FormationStatus.FORMED, state.status());
    assertEquals(province.geometry().basin().id(), state.basinId());
    assertEquals(
        EvaporitePotashState.BasinSetting.RESTRICTED_MARINE_EMBAYMENT, state.basinSetting());
    assertEquals(EvaporitePotashState.RestrictionClass.LIMITED_OUTFLOW, state.restrictionClass());
    assertEquals(
        EvaporitePotashState.SoluteSourceClass.REPLENISHED_SEAWATER, state.soluteSourceClass());
    assertEquals(
        EvaporitePotashState.BrineEvolutionClass.REFLOODING_HALITE_TO_POTASH,
        state.brineEvolutionClass());
    assertEquals(248.0, state.formationAge().ageMa());
    assertEquals(3, state.concentrationEpisodes());
    assertEquals(3, state.brineSequence().size());
    assertEquals(
        List.of(
            EvaporitePotashState.StageKind.MARGINAL_SULFATE,
            EvaporitePotashState.StageKind.BASIN_CENTER_HALITE,
            EvaporitePotashState.StageKind.LATE_POTASH),
        state.brineSequence().stream().map(EvaporitePotashState.BrineStage::kind).toList());

    Point3 center = state.localCenter();
    assertTrue(state.contains(center));
    assertEquals(
        EvaporitePotashState.StageKind.BASIN_CENTER_HALITE,
        state.zoneAt(center).orElseThrow().kind());
    assertEquals(
        EvaporitePotashState.StageKind.LATE_POTASH,
        state
            .zoneAt(
                new Point3(
                    center.x(), center.y() + 0.42 * state.sequenceThicknessBlocks(), center.z()))
            .orElseThrow()
            .kind());
    assertEquals(
        EvaporitePotashState.StageKind.MARGINAL_SULFATE,
        state
            .zoneAt(
                new Point3(
                    center.x() + 0.9 * state.halfLengthBlocks(),
                    center.y() - 0.4 * state.sequenceThicknessBlocks(),
                    center.z()))
            .orElseThrow()
            .kind());
    assertTrue(
        state
            .zoneAt(new Point3(center.x() + 1.1 * state.halfLengthBlocks(), center.y(), center.z()))
            .isEmpty());
    assertTrue(
        state.sulfateAllocationFixedUnits()
                + state.haliteAllocationFixedUnits()
                + state.potashAllocationFixedUnits()
            <= state.soluteSourceBudgetFixedUnits());
    assertTrue(state.failedGate().isEmpty());
  }

  @Test
  void barrenProvinceCannotManufactureRestrictedEvaporiteSequence() {
    GeologyQueryEngine query = Phase1World.create(8_675_309L);
    Province province =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC);
    EvaporitePotashState state = query.evaporitePotashState(province);

    assertEquals(FormationStatus.BARREN_SYSTEM, state.status());
    assertEquals(EvaporitePotashState.BasinSetting.NO_RESTRICTED_BASIN, state.basinSetting());
    assertEquals(EvaporitePotashState.RestrictionClass.OPEN_OUTFLOW, state.restrictionClass());
    assertEquals(
        EvaporitePotashState.SoluteSourceClass.NO_SOLUTE_REPLENISHMENT, state.soluteSourceClass());
    assertEquals(EvaporitePotashState.BrineEvolutionClass.UNRESOLVED, state.brineEvolutionClass());
    assertEquals("restriction", state.failedGate().orElseThrow());
    assertTrue(state.brineSequence().isEmpty());
    assertEquals(0L, state.soluteSourceBudgetFixedUnits());
    assertTrue(state.zoneAt(state.localCenter()).isEmpty());
  }

  @Test
  void formedPlacerPublishesExposedSourceDrainageAndSortingBudget() {
    GeologyQueryEngine query = Phase0World.create(8_675_309L);
    Province province = query.atlas().provinceAt(new Point2(0.0, 0.0));
    PlacerSystemState state = query.placerSystemState(province);

    assertEquals(FormationStatus.FORMED, state.status());
    assertEquals(province.proofIds().porphyryDepositId(), state.sourceDepositId());
    assertEquals(province.proofIds().weatheringId(), state.weatheringProcessId());
    assertEquals(province.proofIds().placerDepositId(), state.trapId());
    assertEquals(
        PlacerSystemState.SourceExposureClass.PARTLY_EXPOSED_PRIMARY, state.sourceExposureClass());
    assertEquals(
        PlacerSystemState.TransportClass.CONNECTED_WATER_CATCHMENT, state.transportClass());
    assertEquals(PlacerSystemState.TrapClass.HYDRAULIC_GRADIENT_BREAK, state.trapClass());
    assertEquals(
        PlacerSystemState.SortingClass.DENSE_MINERAL_HYDRAULIC_SORTING, state.sortingClass());
    assertEquals(1.0, state.hydraulicTrapScore());
    assertTrue(state.sourceDistanceBlocks() > 0.0);
    assertTrue(state.contains(state.trapCenter()));
    assertEquals(
        PlacerSystemState.PlacerZone.HYDRAULIC_TRAP,
        state.zoneAt(state.trapCenter()).orElseThrow());
    assertTrue(
        state
            .zoneAt(
                new Point2(
                    state.trapCenter().x() + 1.1 * state.trapHalfWidthBlocks(),
                    state.trapCenter().z()))
            .isEmpty());
    assertEquals(100_000L, state.sourceBudgetFixedUnits());
    assertEquals(28_000L, state.releasedSourceBudgetFixedUnits());
    assertEquals(8_000L, state.transportLossFixedUnits());
    assertEquals(20_000L, state.depositAllocationFixedUnits());
    assertEquals(72_000L, state.retainedSourceBudgetFixedUnits());
    assertTrue(state.failedGate().isEmpty());
  }

  @Test
  void barrenPlacerRetainsHydraulicCandidateWithoutAnUpstreamSource() {
    GeologyQueryEngine query = Phase1World.create(8_675_309L);
    Province province =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC);
    PlacerSystemState state = query.placerSystemState(province);

    assertEquals(FormationStatus.REJECTED, state.status());
    assertEquals(
        PlacerSystemState.SourceExposureClass.NO_EXPOSED_SOURCE, state.sourceExposureClass());
    assertEquals(
        PlacerSystemState.TransportClass.NO_SOURCE_LINKED_TRANSPORT, state.transportClass());
    assertEquals(PlacerSystemState.TrapClass.NO_ALLOWABLE_TRAP, state.trapClass());
    assertEquals(PlacerSystemState.SortingClass.UNRESOLVED_SORTING, state.sortingClass());
    assertEquals("upstream_source", state.failedGate().orElseThrow());
    assertEquals(0L, state.sourceBudgetFixedUnits());
    assertEquals(0.0, state.hydraulicTrapScore());
    assertTrue(state.zoneAt(state.trapCenter()).isEmpty());
  }

  @Test
  void formedPorphyryPublishesFluidPhaseAndMetalDistributionZones() {
    GeologyQueryEngine query = Phase0World.create(8_675_309L);
    Province province = query.atlas().provinceAt(new Point2(0.0, 0.0));
    PorphyryFluidMetalState state = query.porphyryFluidMetalState(province);

    assertEquals(FormationStatus.FORMED, state.status());
    assertEquals(province.proofIds().magmaLineageId(), state.sourceReservoirId());
    assertEquals(province.proofIds().porphyrySystemId(), state.fluidPathId());
    assertEquals(
        1_000_000L,
        state.sourceMetalFractionsPpm().values().stream().mapToLong(Long::longValue).sum());
    assertEquals(3, state.fluidPulses().size());
    assertEquals(3, state.metalDistributions().size());
    Point3 center = state.localCenter();
    assertEquals(
        PorphyryFluidMetalState.FluidPhaseClass.MAGMATIC_BRINE,
        state.fluidAt(center).orElseThrow().phase());
    assertEquals(
        PorphyryFluidMetalState.FluidPhaseClass.VAPOR_RICH_SEPARATED,
        state.fluidAt(new Point3(center.x() + 90.0, center.y(), center.z())).orElseThrow().phase());
    assertEquals(
        PorphyryFluidMetalState.FluidPhaseClass.METEORIC_MIXTURE,
        state
            .fluidAt(new Point3(center.x() + 170.0, center.y(), center.z()))
            .orElseThrow()
            .phase());
    assertEquals(
        620_000L,
        state
            .metalAt(center)
            .orElseThrow()
            .abundancePpm()
            .get(io.github.crunchybubbles.geological.petrology.ChemicalElement.CU));
    assertEquals(
        300_000L,
        state
            .metalAt(new Point3(center.x() + 170.0, center.y(), center.z()))
            .orElseThrow()
            .abundancePpm()
            .get(io.github.crunchybubbles.geological.petrology.ChemicalElement.CU));
    assertEquals(1_000_000L, state.sourceBudgetFixedUnits());
    assertEquals(105_000L, state.depositAllocationFixedUnits());
    assertEquals(
        state.depositAllocationFixedUnits(),
        state.metalDistributions().stream()
            .mapToLong(PorphyryFluidMetalState.MetalDistribution::allocationFixedUnits)
            .sum());
    assertTrue(state.failedGate().isEmpty());
  }

  @Test
  void barrenPorphyryDoesNotPublishInventedFluidOrMetalDistributions() {
    GeologyQueryEngine query = Phase1World.create(8_675_309L);
    Province province =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC);
    PorphyryFluidMetalState state = query.porphyryFluidMetalState(province);

    assertEquals(FormationStatus.BARREN_SYSTEM, state.status());
    assertEquals("source", state.failedGate().orElseThrow());
    assertTrue(state.sourceMetalFractionsPpm().isEmpty());
    assertTrue(state.fluidPulses().isEmpty());
    assertTrue(state.metalDistributions().isEmpty());
    assertTrue(state.fluidAt(state.localCenter()).isEmpty());
    assertTrue(state.metalAt(state.localCenter()).isEmpty());
  }

  @Test
  void formedPorphyryPublishesGatedSupergeneProfileAndClosedCopperDebit() {
    GeologyQueryEngine query = Phase0World.create(8_675_309L);
    Province province = query.atlas().provinceAt(new Point2(0.0, 0.0));
    SupergeneCopperState state = query.supergeneCopperState(province);

    assertEquals(FormationStatus.FORMED, state.status());
    assertEquals(province.proofIds().porphyrySystemId(), state.systemId());
    assertEquals(province.proofIds().porphyryDepositId(), state.primaryDepositId());
    assertEquals(province.proofIds().weatheringId(), state.weatheringProcessId());
    assertEquals(
        SupergeneCopperState.SourceClass.LEACHABLE_PRIMARY_CU_SULFIDE, state.sourceClass());
    assertEquals(
        SupergeneCopperState.OxidationClass.OXIDIZING_VADOSE_PROFILE, state.oxidationClass());
    assertEquals(
        SupergeneCopperState.WaterTableClass.STABLE_PALEO_WATER_TABLE, state.waterTableClass());
    assertEquals(SupergeneCopperState.TrapClass.REDUCING_SULFIDE_TRAP, state.trapClass());
    assertEquals(
        SupergeneCopperState.PreservationClass.PARTLY_PRESERVED_PROFILE, state.preservationClass());
    assertEquals(0.8, state.formationAge().ageMa());
    assertEquals(
        List.of(
            SupergeneCopperState.HorizonKind.LEACHED_CAP,
            SupergeneCopperState.HorizonKind.OXIDIZED_COPPER,
            SupergeneCopperState.HorizonKind.SUPERGENE_SULFIDE),
        state.horizons().stream().map(SupergeneCopperState.Horizon::kind).toList());

    Point3 center = state.localCenter();
    assertTrue(state.contains(center));
    assertEquals(
        SupergeneCopperState.HorizonKind.SUPERGENE_SULFIDE,
        state.zoneAt(center).orElseThrow().kind());
    assertEquals(
        SupergeneCopperState.HorizonKind.LEACHED_CAP,
        state
            .zoneAt(
                new Point3(
                    center.x(), center.y() + 0.45 * state.profileThicknessBlocks(), center.z()))
            .orElseThrow()
            .kind());
    assertEquals(
        SupergeneCopperState.HorizonKind.OXIDIZED_COPPER,
        state
            .zoneAt(
                new Point3(
                    center.x(), center.y() + 0.25 * state.profileThicknessBlocks(), center.z()))
            .orElseThrow()
            .kind());
    assertTrue(
        state
            .zoneAt(
                new Point3(
                    center.x() + 1.1 * state.blanketHalfWidthBlocks(), center.y(), center.z()))
            .isEmpty());
    assertEquals(105_000L, state.sourceBudgetFixedUnits());
    assertEquals(40_000L, state.leachableCopperFixedUnits());
    assertEquals(24_000L, state.supergeneAllocationFixedUnits());
    assertEquals(16_000L, state.oxidizedAndDissolvedLossFixedUnits());
    assertEquals(65_000L, state.retainedHypogeneFixedUnits());
    assertTrue(state.failedGate().isEmpty());
  }

  @Test
  void buriedPorphyryCannotFormSupergeneWithoutExposureOrPreservation() {
    GeologyQueryEngine query = Phase1World.create(8_675_309L);
    Province province =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.BURIED_FERTILE_RIFT_TO_ARC);
    SupergeneCopperState state = query.supergeneCopperState(province);

    assertEquals(FormationStatus.BARREN_SYSTEM, state.status());
    assertEquals("exposure", state.failedGate().orElseThrow());
    assertEquals(
        SupergeneCopperState.PreservationClass.ERODED_OR_BURIED_PROFILE, state.preservationClass());
    assertTrue(state.horizons().isEmpty());
    assertEquals(0L, state.sourceBudgetFixedUnits());
    assertTrue(state.zoneAt(state.localCenter()).isEmpty());
  }

  @Test
  void dryProvinceCannotInventAPrimaryCopperSupergeneSource() {
    GeologyQueryEngine query = Phase1World.create(8_675_309L);
    Province province =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC);
    SupergeneCopperState state = query.supergeneCopperState(province);

    assertEquals(FormationStatus.BARREN_SYSTEM, state.status());
    assertEquals("primary_cu_source", state.failedGate().orElseThrow());
    assertEquals(SupergeneCopperState.SourceClass.NO_PRIMARY_CU_SULFIDE, state.sourceClass());
    assertEquals(0L, state.supergeneAllocationFixedUnits());
  }

  @Test
  void primaryMineralModelsPublishSourceAuditedDistributionAndHeldOutReports() {
    GeologyQueryEngine query = Phase0World.create(8_675_309L);
    Province province = query.atlas().provinceAt(new Point2(0.0, 0.0));
    List<MineralSystemValidationReport> reports = query.mineralSystemValidationReports(province);

    assertEquals(6, reports.size());
    assertEquals(
        List.of(
            MineralSystemProofs.PORPHYRY_MODEL,
            MineralSystemProofs.VMS_MODEL,
            MineralSystemProofs.PLACER_MODEL,
            MineralSystemProofs.LCT_MODEL,
            MineralSystemProofs.BIF_MODEL,
            MineralSystemProofs.EVAPORITE_MODEL),
        reports.stream().map(MineralSystemValidationReport::modelId).toList());
    assertTrue(reports.stream().allMatch(MineralSystemValidationReport::hardInvariantsPass));
    assertEquals(
        MineralSystemValidationReport.ValidationStatus.AUDITED_SUBSET,
        reports.stream()
            .filter(report -> report.modelId().equals(MineralSystemProofs.PORPHYRY_MODEL))
            .findFirst()
            .orElseThrow()
            .validationStatus());
    assertTrue(
        reports.stream()
            .filter(report -> !report.modelId().equals(MineralSystemProofs.PORPHYRY_MODEL))
            .allMatch(
                report ->
                    report.validationStatus()
                        == MineralSystemValidationReport.ValidationStatus
                            .PROVISIONAL_SOURCE_ANCHORS));
    for (MineralSystemValidationReport report : reports) {
      boolean auditedPorphyry = report.modelId().equals(MineralSystemProofs.PORPHYRY_MODEL);
      assertEquals(auditedPorphyry ? 14 : 5, report.empiricalDataset().rows().size());
      assertEquals(auditedPorphyry ? 10 : 4, report.empiricalDataset().calibrationRowCount());
      assertEquals(auditedPorphyry ? 4 : 1, report.empiricalDataset().heldOutRowCount());
      assertEquals(
          auditedPorphyry
              ? MineralSystemValidationReport.AuditStatus.RAW_TABLE_AUDITED_SUBSET
              : MineralSystemValidationReport.AuditStatus.SOURCE_ANCHORS_PROVISIONAL,
          report.empiricalDataset().auditStatus());
      if (auditedPorphyry) {
        assertEquals(
            "https://pubs.usgs.gov/of/2008/1155/data/", report.empiricalDataset().sourceUri());
        assertEquals("USGS-OFR-2008-1155-v1.0", report.empiricalDataset().sourceVersion());
        assertEquals(
            MineralSystemValidationReport.DistributionKind.EMPIRICAL_ROW,
            report.empiricalDataset().distributionKind());
        assertTrue(report.empiricalDataset().sourceAuditSubsetComplete());
        MineralSystemValidationReport.SampleRow aguaRica =
            report.empiricalDataset().rows().stream()
                .filter(row -> row.sourceRowRef().equals("DepositID=323"))
                .findFirst()
                .orElseThrow();
        assertEquals(0.0042, aguaRica.values().get("cu_grade"), 1.0e-12);
        assertEquals(0.00033, aguaRica.values().get("mo_grade"), 1.0e-12);
      }
      assertTrue(
          report.empiricalDataset().rows().stream()
              .allMatch(row -> !row.sourceRowRef().isBlank() && !row.sourceVersion().isBlank()));
      assertTrue(
          report.invariantChecks().stream()
              .anyMatch(check -> check.name().equals("missing_and_censor_flags")));
      MineralSystemValidationReport.StatisticalValidation statistical =
          report.statisticalValidation();
      assertEquals(
          report.empiricalDataset().heldOutRowCount()
              * report.empiricalDataset().variableUnits().size(),
          statistical.quantileComparisons().size());
      assertEquals(
          report.empiricalDataset().variableUnits().size()
              * (report.empiricalDataset().variableUnits().size() - 1)
              / 2,
          statistical.covarianceSummaries().size());
      assertEquals(
          auditedPorphyry
              ? MineralSystemValidationReport.StatisticalStatus.AUDITED_SUBSET
              : MineralSystemValidationReport.StatisticalStatus.PROVISIONAL_ANCHORS,
          statistical.status());
      assertTrue(
          statistical.quantileComparisons().stream()
              .allMatch(comparison -> comparison.predictedValue().isPresent()));
      assertTrue(
          statistical.covarianceSummaries().stream()
              .allMatch(summary -> summary.calibrationPairCount() >= 2));
    }
  }

  @Test
  void barrenPrimaryReportsRetainFailedGatesWithoutAllocatingEmpiricalMass() {
    GeologyQueryEngine query = Phase1World.create(8_675_309L);
    Province province =
        Phase1TestSupport.provinceWithGrammar(query, ProvinceGrammar.BARREN_DRY_RIFT_TO_ARC);
    List<MineralSystemValidationReport> reports = query.mineralSystemValidationReports(province);

    assertEquals(6, reports.size());
    assertTrue(
        reports.stream().allMatch(report -> report.formationStatus() != FormationStatus.FORMED));
    assertTrue(reports.stream().allMatch(report -> report.failedGate().isPresent()));
    assertTrue(reports.stream().allMatch(report -> report.sourceBudgetFixedUnits() == 0L));
    assertTrue(reports.stream().allMatch(MineralSystemValidationReport::hardInvariantsPass));
  }

  private static MineralSystemDecision formed(
      List<MineralSystemDecision> decisions, String modelId) {
    return decisions.stream()
        .filter(result -> result.modelId().equals(modelId))
        .filter(result -> result.status() == FormationStatus.FORMED)
        .findFirst()
        .orElseThrow();
  }
}
