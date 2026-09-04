package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.ProvinceProofIds;
import io.github.crunchybubbles.geological.atlas.RiftArcGeometry;
import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.determinism.WorldIdentity;
import io.github.crunchybubbles.geological.model.AgeKey;
import io.github.crunchybubbles.geological.model.Bounds2D;
import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.model.Point3;
import io.github.crunchybubbles.geological.trace.ProvenanceStep;
import java.util.List;
import java.util.Map;

/** Phase 0 causal proofs for porphyry Cu, VMS, and source-linked placer Au. */
public final class MineralSystemProofs {
  public static final String PORPHYRY_MODEL = "geological:porphyry_cu_au_phase0";
  public static final String VMS_MODEL = "geological:vms_phase0";
  public static final String PLACER_MODEL = "geological:alluvial_placer_au_phase0";
  public static final String LCT_MODEL = "geological:lct_pegmatite_phase3";
  public static final String BIF_MODEL = "geological:bif_phase3";
  public static final String EVAPORITE_MODEL = "geological:evaporite_potash_phase3";

  public List<MineralSystemDecision> compile(Province province) {
    return List.of(
        province.grammar().formsPorphyry()
            ? formedPorphyry(province)
            : barrenPrimaryPorphyry(province),
        rejectedPorphyry(province),
        province.grammar().formsVms() ? formedVms(province) : barrenPrimaryVms(province),
        rejectedVms(province),
        province.grammar().formsPlacer() ? formedPlacer(province) : rejectedPrimaryPlacer(province),
        rejectedPlacer(province));
  }

  /** Returns the linked porphyry topology for the province's primary candidate. */
  public PorphyrySystemState porphyryState(Province province) {
    if (province == null) {
      throw new IllegalArgumentException("province is required");
    }
    MineralSystemDecision primary =
        compile(province).stream()
            .filter(
                decision -> decision.candidateId().equals(province.proofIds().porphyrySystemId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("porphyry proof is missing"));
    return PorphyrySystemState.proofFor(province, primary);
  }

  /** Returns the linked VMS basin/lens/feeder topology for the province's primary candidate. */
  public VmsSystemState vmsState(Province province) {
    if (province == null) {
      throw new IllegalArgumentException("province is required");
    }
    MineralSystemDecision primary =
        compile(province).stream()
            .filter(decision -> decision.candidateId().equals(province.proofIds().vmsSystemId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("VMS proof is missing"));
    return VmsSystemState.proofFor(province, primary);
  }

  /** Returns the evolved-lineage LCT pegmatite child-body state for this province. */
  public LctPegmatiteState lctPegmatiteState(
      Province province, io.github.crunchybubbles.geological.determinism.WorldIdentity identity) {
    if (province == null || identity == null) {
      throw new IllegalArgumentException("province and world identity are required");
    }
    return LctPegmatiteState.proofFor(province, identity);
  }

  /** Returns the basin-bound BIF sheet state for this province. */
  public BifSystemState bifState(
      Province province, io.github.crunchybubbles.geological.determinism.WorldIdentity identity) {
    if (province == null || identity == null) {
      throw new IllegalArgumentException("province and world identity are required");
    }
    return BifSystemState.proofFor(province, identity);
  }

  /** Returns the restricted-basin evaporite and potash sequence for this province. */
  public EvaporitePotashState evaporitePotashState(
      Province province, io.github.crunchybubbles.geological.determinism.WorldIdentity identity) {
    if (province == null || identity == null) {
      throw new IllegalArgumentException("province and world identity are required");
    }
    return EvaporitePotashState.proofFor(province, identity);
  }

  /** Returns the source-linked placer transport and trap state for this province. */
  public PlacerSystemState placerState(Province province) {
    if (province == null) {
      throw new IllegalArgumentException("province is required");
    }
    MineralSystemDecision primary =
        compile(province).stream()
            .filter(decision -> decision.candidateId().equals(province.proofIds().placerSystemId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("placer proof is missing"));
    return PlacerSystemState.proofFor(province, primary);
  }

  /** Returns the richer porphyry fluid-phase and metal-distribution refinement. */
  public PorphyryFluidMetalState porphyryFluidMetalState(Province province) {
    if (province == null) {
      throw new IllegalArgumentException("province is required");
    }
    MineralSystemDecision primary =
        compile(province).stream()
            .filter(
                decision -> decision.candidateId().equals(province.proofIds().porphyrySystemId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("porphyry proof is missing"));
    return PorphyryFluidMetalState.proofFor(province, primary);
  }

  /** Returns the primary-Cu-dependent oxidation, water-table, and supergene profile state. */
  public SupergeneCopperState supergeneCopperState(Province province, WorldIdentity identity) {
    if (province == null || identity == null) {
      throw new IllegalArgumentException("province and world identity are required");
    }
    MineralSystemDecision primary =
        compile(province).stream()
            .filter(
                decision -> decision.candidateId().equals(province.proofIds().porphyrySystemId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("porphyry proof is missing"));
    return SupergeneCopperState.proofFor(province, primary, identity);
  }

  /**
   * Returns source-specific empirical-distribution and invariant reports for the six primary
   * models.
   */
  public List<MineralSystemValidationReport> validationReports(
      Province province, WorldIdentity identity) {
    if (province == null || identity == null) {
      throw new IllegalArgumentException("province and world identity are required");
    }
    List<MineralSystemValidationReport> reports =
        new java.util.ArrayList<>(
            compile(province).stream()
                .filter(
                    decision ->
                        decision.candidateId().equals(province.proofIds().porphyrySystemId())
                            || decision.candidateId().equals(province.proofIds().vmsSystemId())
                            || decision.candidateId().equals(province.proofIds().placerSystemId()))
                .map(decision -> MineralSystemValidationReport.from(province, decision))
                .toList());
    LctPegmatiteState lct = lctPegmatiteState(province, identity);
    BifSystemState bif = bifState(province, identity);
    EvaporitePotashState evaporite = evaporitePotashState(province, identity);
    reports.add(
        MineralSystemValidationReport.fromState(
            lct.childBodyId(),
            LCT_MODEL,
            lct.status(),
            lct.sourceBudgetFixedUnits(),
            lct.childAllocationFixedUnits(),
            lct.failedGate()));
    reports.add(
        MineralSystemValidationReport.fromState(
            bif.sheetId(),
            BIF_MODEL,
            bif.status(),
            bif.sourceBudgetFixedUnits(),
            bif.sheetAllocationFixedUnits(),
            bif.failedGate()));
    reports.add(
        MineralSystemValidationReport.fromState(
            evaporite.systemId(),
            EVAPORITE_MODEL,
            evaporite.status(),
            evaporite.soluteSourceBudgetFixedUnits(),
            Math.addExact(
                Math.addExact(
                    evaporite.sulfateAllocationFixedUnits(),
                    evaporite.haliteAllocationFixedUnits()),
                evaporite.potashAllocationFixedUnits()),
            evaporite.failedGate()));
    return List.copyOf(reports);
  }

  private MineralSystemDecision barrenPrimaryPorphyry(Province province) {
    ProvinceProofIds ids = province.proofIds();
    return new MineralSystemDecision(
        ids.porphyrySystemId(),
        PORPHYRY_MODEL,
        FormationStatus.BARREN_SYSTEM,
        null,
        List.of(
            pass(
                "driver",
                "The multi-pulse stock supplies heat.",
                province.geometry().plutonPulses().getLast().id()),
            fail(
                "source",
                "The dry arc lineage retains too little exsolved volatile and accessible metal to form a deposit.")),
        List.of(
            step(
                0,
                "barren_magma_evaluation",
                "Emplacement and contact metamorphism occur, but the volatile-source gate fails before mineralization.",
                List.of(ids.magmaLineageId()),
                List.of(ids.porphyrySystemId()))),
        null);
  }

  private MineralSystemDecision barrenPrimaryVms(Province province) {
    ProvinceProofIds ids = province.proofIds();
    return new MineralSystemDecision(
        ids.vmsSystemId(),
        VMS_MODEL,
        FormationStatus.BARREN_SYSTEM,
        null,
        List.of(
            pass("medium", "Marine basin water is present.", province.geometry().basin().id()),
            fail(
                "driver",
                "No coeval subseafloor volcanic heat pulse sustains hydrothermal convection.")),
        List.of(
            step(
                0,
                "barren_basin_evaluation",
                "The basin contains permissive strata but lacks the coeval driver required for a VMS system.",
                List.of(province.geometry().basin().packageId()),
                List.of(ids.vmsSystemId()))),
        null);
  }

  private MineralSystemDecision rejectedPrimaryPlacer(Province province) {
    ProvinceProofIds ids = province.proofIds();
    boolean hasPrimarySource = province.grammar().formsPorphyry();
    String failedGate = hasPrimarySource ? "exposure" : "upstream_source";
    String explanation =
        hasPrimarySource
            ? "The primary porphyry remains too deeply buried to debit a weathering-release budget."
            : "No formed upstream primary gold deposit can supply the catchment.";
    return new MineralSystemDecision(
        ids.placerSystemId(),
        PLACER_MODEL,
        FormationStatus.REJECTED,
        null,
        List.of(
            pass(
                "transport",
                "A connected trunk and hydraulic traps transport ordinary sediment.",
                ids.placerSystemId()),
            fail(failedGate, explanation)),
        List.of(
            step(
                0,
                "source_link_rejection",
                "The geomorphic trap is retained, but no allowable source debit reaches it.",
                hasPrimarySource ? List.of(ids.porphyryDepositId()) : List.of(),
                List.of(ids.placerSystemId()))),
        null);
  }

  private MineralSystemDecision formedPorphyry(Province province) {
    ProvinceProofIds ids = province.proofIds();
    RiftArcGeometry geometry = province.geometry();
    Point3 worldCenter = worldPoint(province, geometry.porphyryCenter());
    DepositDescriptor deposit =
        deposit(
            ids.porphyryDepositId(),
            ids.porphyrySystemId(),
            DepositType.PORPHYRY_CU_AU,
            worldCenter,
            155.0,
            new AgeKey(92.0, 0),
            List.of(ids.magmaLineageId(), geometry.plutonPulses().getLast().id()),
            0.78);
    List<GateEvidence> gates =
        List.of(
            pass(
                "driver",
                "The shallow multi-pulse felsic stock supplies heat and pressure.",
                geometry.plutonPulses().getLast().id()),
            pass(
                "source",
                "Residual arc magma supplies Cu and Au to an exsolved fluid budget.",
                ids.magmaLineageId()),
            pass(
                "medium",
                "A magmatic-hydrothermal fluid pulse is explicitly present.",
                ids.porphyrySystemId()),
            pass(
                "pathway",
                "The inherited finite fault and intrusion-top fractures focus flow.",
                geometry.fault().id()),
            pass(
                "trap",
                "Cooling and wall-rock reaction occur above the felsic stock.",
                geometry.aureoleId()),
            pass(
                "preservation",
                "Later uplift partly exposes but does not erase the stockwork.",
                ids.upliftId()));
    List<ProvenanceStep> trace =
        List.of(
            step(
                0,
                "arc_magma_generation",
                "Basement and mantle-derived arc melt establish a fertile magma lineage.",
                List.of(geometry.basementId()),
                List.of(ids.magmaLineageId())),
            step(
                1,
                "multi_pulse_emplacement",
                "Three independently identified pulses differentiate toward a shallow felsic stock.",
                List.of(ids.magmaLineageId()),
                geometry.plutonPulses().stream().map(RiftArcGeometry.PlutonPulse::id).toList()),
            step(
                2,
                "fluid_exsolution_and_transport",
                "Residual fluid carries Cu-Au through stockwork and inherited fault permeability.",
                List.of(ids.magmaLineageId(), geometry.fault().id()),
                List.of(ids.porphyrySystemId())),
            step(
                3,
                "cooling_and_reaction_trap",
                "Cooling and host reaction deposit a zoned stockwork inside the contact footprint.",
                List.of(ids.porphyrySystemId(), geometry.aureoleId()),
                List.of(ids.porphyryDepositId())),
            step(
                4,
                "exhumation",
                "Uplift exposes part of the source for later weathering and placer transport.",
                List.of(ids.porphyryDepositId()),
                List.of(ids.upliftId())));
    FixedPointLedger ledger =
        new FixedPointLedger(
            "Cu",
            "phase0_fixed_units",
            1_000_000,
            Map.of(
                "deposit", 105_000L, "diffuse_halo_and_loss", 45_000L, "retained_magma", 850_000L));
    return new MineralSystemDecision(
        ids.porphyrySystemId(),
        PORPHYRY_MODEL,
        FormationStatus.FORMED,
        deposit,
        gates,
        trace,
        ledger);
  }

  private MineralSystemDecision rejectedPorphyry(Province province) {
    ProvinceProofIds ids = province.proofIds();
    return new MineralSystemDecision(
        ids.rejectedPorphyryCandidateId(),
        PORPHYRY_MODEL,
        FormationStatus.BARREN_SYSTEM,
        null,
        List.of(
            pass(
                "driver",
                "A distal pulse supplies heat.",
                province.geometry().plutonPulses().getFirst().id()),
            pass(
                "source",
                "The magma lineage contains an accessible residual metal budget.",
                ids.magmaLineageId()),
            fail(
                "pathway",
                "No connected stockwork, permeable contact, or inherited fault reaches this host volume.")),
        List.of(
            step(
                0,
                "rule_rejection",
                "The candidate remains a barren hydrothermal system because source-to-trap connectivity fails.",
                List.of(ids.magmaLineageId()),
                List.of(ids.rejectedPorphyryCandidateId()))),
        null);
  }

  private MineralSystemDecision formedVms(Province province) {
    ProvinceProofIds ids = province.proofIds();
    RiftArcGeometry geometry = province.geometry();
    Point3 worldCenter = worldPoint(province, geometry.vmsCenter());
    DepositDescriptor deposit =
        deposit(
            ids.vmsDepositId(),
            ids.vmsSystemId(),
            DepositType.VOLCANOGENIC_MASSIVE_SULFIDE,
            worldCenter,
            112.0,
            new AgeKey(241.0, 0),
            List.of(geometry.basin().packageId(), geometry.fault().id()),
            0.69);
    List<GateEvidence> gates =
        List.of(
            pass(
                "driver",
                "A coeval subseafloor volcanic heat source drives convection.",
                ids.vmsSystemId()),
            pass(
                "source",
                "Hydrothermal fluid leaches Cu-Zn-Fe-S from the permeable volcanic pile.",
                geometry.basin().packageId()),
            pass(
                "medium",
                "Seawater-dominated hydrothermal fluid is available in the marine rift basin.",
                geometry.basin().id()),
            pass(
                "pathway",
                "The synvolcanic inherited fault connects recharge to the seafloor.",
                geometry.fault().id()),
            pass(
                "trap",
                "Cooling and mixing occur at the active depositional horizon.",
                geometry.basin().packageId()),
            pass(
                "preservation",
                "The lens is buried, folded, faulted, and only partly exposed rather than destroyed.",
                ids.upliftId()));
    List<ProvenanceStep> trace =
        List.of(
            step(
                0,
                "rift_basin_deposition",
                "Submarine volcaniclastics create a permeable, metal-bearing source pile.",
                List.of(geometry.basin().id()),
                List.of(geometry.basin().packageId())),
            step(
                1,
                "seawater_convection",
                "Heat and the inherited fault connect recharge, leaching, and discharge.",
                List.of(geometry.basin().packageId(), geometry.fault().id()),
                List.of(ids.vmsSystemId())),
            step(
                2,
                "seafloor_precipitation",
                "Cooling and mixing form a stratiform lens with a chloritic feeder.",
                List.of(ids.vmsSystemId()),
                List.of(ids.vmsDepositId())),
            step(
                3,
                "younger_deformation",
                "The later finite fold and fault displace the older lens in event order.",
                List.of(ids.vmsDepositId()),
                List.of(geometry.fold().id(), geometry.fault().id())));
    FixedPointLedger ledger =
        new FixedPointLedger(
            "Cu-Zn",
            "phase0_fixed_units",
            800_000,
            Map.of(
                "deposit",
                92_000L,
                "diffuse_alteration_and_loss",
                88_000L,
                "retained_volcanic_pile",
                620_000L));
    return new MineralSystemDecision(
        ids.vmsSystemId(), VMS_MODEL, FormationStatus.FORMED, deposit, gates, trace, ledger);
  }

  private MineralSystemDecision rejectedVms(Province province) {
    ProvinceProofIds ids = province.proofIds();
    return new MineralSystemDecision(
        ids.rejectedVmsCandidateId(),
        VMS_MODEL,
        FormationStatus.REJECTED,
        null,
        List.of(
            pass("driver", "A heat source exists below the younger sandstone.", ids.vmsSystemId()),
            fail(
                "synvolcanic_horizon",
                "The candidate is above the submarine volcanic depositional horizon and is not coeval with it.")),
        List.of(
            step(
                0,
                "rule_rejection",
                "The rule rejects a tempting sulfide lens that lacks the required synsedimentary age and horizon.",
                List.of(ids.vmsSystemId()),
                List.of(ids.rejectedVmsCandidateId()))),
        null);
  }

  private MineralSystemDecision formedPlacer(Province province) {
    ProvinceProofIds ids = province.proofIds();
    RiftArcGeometry geometry = province.geometry();
    Point2 center2 = province.frame().toWorld(geometry.placerCenter());
    Point3 center = new Point3(center2.x(), 0.0, center2.z());
    DepositDescriptor deposit =
        deposit(
            ids.placerDepositId(),
            ids.placerSystemId(),
            DepositType.ALLUVIAL_PLACER_AU,
            center,
            74.0,
            new AgeKey(0.1, 0),
            List.of(ids.porphyryDepositId()),
            0.61);
    List<GateEvidence> gates =
        List.of(
            pass(
                "driver",
                "Gravity and trunk-stream energy transport released dense particles.",
                ids.placerSystemId()),
            pass(
                "source",
                "The partly exposed porphyry contains durable gold upstream.",
                ids.porphyryDepositId()),
            pass(
                "medium",
                "Water and sediment move through the Overworld catchment profile.",
                ids.weatheringId()),
            pass(
                "pathway",
                "The analytic catchment-owned trunk is connected and points downstream.",
                ids.placerSystemId()),
            pass(
                "trap",
                "A gradient/curvature break supplies a bounded hydraulic trap.",
                ids.placerDepositId()),
            pass(
                "preservation",
                "The young channel-lag body remains inside the active alluvial corridor.",
                ids.placerDepositId()));
    List<ProvenanceStep> trace =
        List.of(
            step(
                0,
                "primary_gold_hosting",
                "The porphyry stockwork retains a small Au-bearing host fraction.",
                List.of(ids.magmaLineageId()),
                List.of(ids.porphyryDepositId())),
            step(
                1,
                "weathering_release",
                "Exhumation and weathering debit the accessible primary source.",
                List.of(ids.porphyryDepositId(), ids.upliftId()),
                List.of(ids.weatheringId())),
            step(
                2,
                "drainage_transport",
                "The connected trunk carries released durable gold downstream with explicit loss.",
                List.of(ids.weatheringId()),
                List.of(ids.placerSystemId())),
            step(
                3,
                "hydraulic_sorting",
                "A downstream trap credits only the amount debited from the exposed source budget.",
                List.of(ids.placerSystemId(), ids.porphyryDepositId()),
                List.of(ids.placerDepositId())));
    FixedPointLedger ledger =
        new FixedPointLedger(
            "Au",
            "phase0_fixed_units",
            100_000,
            Map.of(
                "placer_trap",
                20_000L,
                "retained_primary_source",
                72_000L,
                "transport_and_dilution_loss",
                8_000L));
    return new MineralSystemDecision(
        ids.placerSystemId(), PLACER_MODEL, FormationStatus.FORMED, deposit, gates, trace, ledger);
  }

  private MineralSystemDecision rejectedPlacer(Province province) {
    ProvinceProofIds ids = province.proofIds();
    return new MineralSystemDecision(
        ids.rejectedPlacerCandidateId(),
        PLACER_MODEL,
        FormationStatus.REJECTED,
        null,
        List.of(
            pass(
                "trap",
                "A local bar-shaped hydraulic trap is present.",
                ids.rejectedPlacerCandidateId()),
            fail(
                "upstream_source",
                "The candidate lies upstream of the exposed porphyry source and cannot receive its released gold.")),
        List.of(
            step(
                0,
                "rule_rejection",
                "A plausible-looking bar is rejected because its drainage ancestry contains no gold-bearing source.",
                List.of(ids.porphyryDepositId()),
                List.of(ids.rejectedPlacerCandidateId()))),
        null);
  }

  private static DepositDescriptor deposit(
      StableId depositId,
      StableId systemId,
      DepositType type,
      Point3 center,
      double radius,
      AgeKey age,
      List<StableId> sources,
      double intensity) {
    return new DepositDescriptor(
        depositId,
        systemId,
        type,
        center,
        new Bounds2D(
            center.x() - radius, center.z() - radius, center.x() + radius, center.z() + radius),
        age,
        sources,
        intensity);
  }

  private static Point3 worldPoint(Province province, Point3 local) {
    Point2 world = province.frame().toWorld(new Point2(local.x(), local.z()));
    return new Point3(world.x(), local.y(), world.z());
  }

  private static GateEvidence pass(String gate, String explanation, StableId... ids) {
    return new GateEvidence(gate, GateStatus.PASS, explanation, List.of(ids));
  }

  private static GateEvidence fail(String gate, String explanation) {
    return new GateEvidence(gate, GateStatus.FAIL, explanation, List.of());
  }

  private static ProvenanceStep step(
      int order,
      String process,
      String explanation,
      List<StableId> inputs,
      List<StableId> outputs) {
    return new ProvenanceStep(order, process, explanation, inputs, outputs);
  }
}
