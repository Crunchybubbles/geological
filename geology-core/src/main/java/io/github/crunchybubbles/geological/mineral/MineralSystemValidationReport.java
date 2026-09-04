package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.determinism.StableId;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Phase 3 empirical-distribution and validation evidence for one primary mineral-system model.
 *
 * <p>The checked-in rows are deliberately marked as provisional source anchors until a raw,
 * licensed table is audited. They provide a deterministic importer/validator contract without
 * pretending that a few review anchors are an unbiased natural population.
 */
public record MineralSystemValidationReport(
    StableId systemId,
    String modelId,
    FormationStatus formationStatus,
    EmpiricalDataset empiricalDataset,
    List<InvariantCheck> invariantChecks,
    ValidationStatus validationStatus,
    long sourceBudgetFixedUnits,
    long depositAllocationFixedUnits,
    Optional<String> failedGate) {
  public MineralSystemValidationReport {
    if (systemId == null
        || modelId == null
        || modelId.isBlank()
        || formationStatus == null
        || empiricalDataset == null
        || invariantChecks == null
        || validationStatus == null
        || failedGate == null) {
      throw new IllegalArgumentException("mineral-system validation report must be complete");
    }
    if (sourceBudgetFixedUnits < 0L
        || depositAllocationFixedUnits < 0L
        || depositAllocationFixedUnits > sourceBudgetFixedUnits) {
      throw new IllegalArgumentException("validation report budgets are out of bounds");
    }
    invariantChecks = List.copyOf(invariantChecks);
    if (invariantChecks.stream().anyMatch(check -> check == null)) {
      throw new IllegalArgumentException("validation checks cannot be null");
    }
    boolean failedCheck =
        invariantChecks.stream().anyMatch(check -> check.status() == CheckStatus.FAIL);
    if ((failedCheck && validationStatus != ValidationStatus.FAILED)
        || (!failedCheck && validationStatus == ValidationStatus.FAILED)) {
      throw new IllegalArgumentException("validation status does not match invariant checks");
    }
    if (formationStatus == FormationStatus.FORMED && failedGate.isPresent()) {
      throw new IllegalArgumentException("formed validation reports cannot have a failed gate");
    }
    if (formationStatus != FormationStatus.FORMED && failedGate.isEmpty()) {
      throw new IllegalArgumentException("barren validation reports require a failed gate");
    }
  }

  /** Builds the report for a primary decision and its source-specific empirical dataset. */
  public static MineralSystemValidationReport from(
      Province province, MineralSystemDecision decision) {
    if (province == null || decision == null) {
      throw new IllegalArgumentException("province and mineral-system decision are required");
    }
    EmpiricalDataset dataset = EmpiricalDataset.forModel(decision.modelId());
    Optional<String> failedGate =
        decision.gates().stream()
            .filter(gate -> gate.status() == GateStatus.FAIL)
            .map(GateEvidence::gate)
            .findFirst();
    long sourceBudget = decision.ledger() == null ? 0L : decision.ledger().sourceAmount();
    long depositAllocation =
        decision.ledger() == null
            ? 0L
            : decision.ledger().allocations().getOrDefault("deposit", 0L);
    boolean sourceBudgetClosure =
        decision.status() != FormationStatus.FORMED
            || decision.ledger() != null
                && decision.ledger().allocations().values().stream()
                        .mapToLong(Long::longValue)
                        .sum()
                    == decision.ledger().sourceAmount();
    return buildReport(
        decision.candidateId(),
        decision.modelId(),
        decision.status(),
        dataset,
        sourceBudget,
        depositAllocation,
        failedGate,
        sourceBudgetClosure);
  }

  /** Builds a report for a derived Phase 3 state that has no primary decision ledger. */
  public static MineralSystemValidationReport fromState(
      StableId systemId,
      String modelId,
      FormationStatus formationStatus,
      long sourceBudgetFixedUnits,
      long depositAllocationFixedUnits,
      Optional<String> failedGate) {
    if (systemId == null
        || modelId == null
        || modelId.isBlank()
        || formationStatus == null
        || failedGate == null) {
      throw new IllegalArgumentException("derived validation report identity is required");
    }
    return buildReport(
        systemId,
        modelId,
        formationStatus,
        EmpiricalDataset.forModel(modelId),
        sourceBudgetFixedUnits,
        depositAllocationFixedUnits,
        failedGate,
        depositAllocationFixedUnits >= 0L
            && sourceBudgetFixedUnits >= 0L
            && depositAllocationFixedUnits <= sourceBudgetFixedUnits);
  }

  private static MineralSystemValidationReport buildReport(
      StableId systemId,
      String modelId,
      FormationStatus formationStatus,
      EmpiricalDataset dataset,
      long sourceBudget,
      long depositAllocation,
      Optional<String> failedGate,
      boolean sourceBudgetClosure) {
    List<InvariantCheck> checks =
        List.of(
            check(
                "row_identity_and_metadata",
                dataset.rows().stream().map(SampleRow::sourceRowRef).distinct().count()
                    == dataset.rows().size(),
                "Every imported row retains a source-row reference, version, subtype, grouping, and cutoff basis."),
            check(
                "missing_and_censor_flags",
                dataset.rows().stream()
                    .allMatch(
                        row ->
                            row.missingFields().stream().noneMatch(row.values()::containsKey)
                                && row.censoredFields().stream()
                                    .noneMatch(row.missingFields()::contains)),
                "Missing and censored variables remain explicit and are never converted to zero."),
            check(
                "calibration_held_out_partition",
                dataset.calibrationRowCount() > 0 && dataset.heldOutRowCount() > 0,
                "The dataset retains at least one calibration row and one held-out row."),
            check(
                "source_budget_closure",
                sourceBudgetClosure,
                "A formed system's fixed-point source ledger closes before grade sampling."),
            check(
                "barren_outcome_gate",
                formationStatus == FormationStatus.FORMED || failedGate.isPresent(),
                "Barren or rejected candidates retain an explicit failed hard gate."));
    ValidationStatus status =
        checks.stream().anyMatch(check -> check.status() == CheckStatus.FAIL)
            ? ValidationStatus.FAILED
            : dataset.auditStatus() == AuditStatus.RAW_TABLE_AUDITED
                ? ValidationStatus.PASSED
                : ValidationStatus.PROVISIONAL_SOURCE_ANCHORS;
    return new MineralSystemValidationReport(
        systemId,
        modelId,
        formationStatus,
        dataset,
        checks,
        status,
        sourceBudget,
        depositAllocation,
        failedGate);
  }

  private static InvariantCheck check(String name, boolean passes, String explanation) {
    return new InvariantCheck(name, passes ? CheckStatus.PASS : CheckStatus.FAIL, explanation);
  }

  public boolean hardInvariantsPass() {
    return invariantChecks.stream().noneMatch(check -> check.status() == CheckStatus.FAIL);
  }

  public enum ValidationStatus {
    PASSED,
    PROVISIONAL_SOURCE_ANCHORS,
    FAILED
  }

  public enum CheckStatus {
    PASS,
    FAIL
  }

  public record InvariantCheck(String name, CheckStatus status, String explanation) {
    public InvariantCheck {
      if (name == null
          || name.isBlank()
          || status == null
          || explanation == null
          || explanation.isBlank()) {
        throw new IllegalArgumentException("validation invariant must be named and explained");
      }
    }
  }

  public enum DistributionKind {
    EMPIRICAL_ROW,
    EMPIRICAL_COPULA,
    QUANTILE_TABLE
  }

  public enum AuditStatus {
    SOURCE_ANCHORS_PROVISIONAL,
    RAW_TABLE_AUDITED
  }

  public enum SampleRole {
    CALIBRATION,
    HELD_OUT
  }

  /** Source-specific distribution metadata and row-level audit fields. */
  public record EmpiricalDataset(
      String id,
      String sourceUri,
      String sourceVersion,
      String population,
      String aggregationRule,
      String cutoffBasis,
      String licenseNote,
      DistributionKind distributionKind,
      AuditStatus auditStatus,
      Map<String, String> variableUnits,
      List<SampleRow> rows) {
    public EmpiricalDataset {
      if (id == null
          || id.isBlank()
          || sourceUri == null
          || sourceUri.isBlank()
          || sourceVersion == null
          || sourceVersion.isBlank()
          || population == null
          || population.isBlank()
          || aggregationRule == null
          || aggregationRule.isBlank()
          || cutoffBasis == null
          || cutoffBasis.isBlank()
          || licenseNote == null
          || licenseNote.isBlank()
          || distributionKind == null
          || auditStatus == null
          || variableUnits == null
          || rows == null) {
        throw new IllegalArgumentException("empirical dataset metadata must be complete");
      }
      TreeMap<String, String> units = new TreeMap<>();
      variableUnits.forEach(
          (variable, unit) -> {
            if (variable == null || variable.isBlank() || unit == null || unit.isBlank()) {
              throw new IllegalArgumentException("empirical variable units must be named");
            }
            units.put(variable, unit);
          });
      if (units.isEmpty()) {
        throw new IllegalArgumentException("empirical datasets require variable units");
      }
      rows =
          List.copyOf(rows).stream()
              .sorted(
                  Comparator.comparingDouble(SampleRow::percentile).thenComparing(SampleRow::rowId))
              .toList();
      if (rows.size() < 3 || rows.stream().anyMatch(row -> row == null)) {
        throw new IllegalArgumentException(
            "empirical datasets require at least three non-null rows");
      }
      Set<String> rowIds = new HashSet<>();
      for (SampleRow row : rows) {
        if (!rowIds.add(row.rowId())) {
          throw new IllegalArgumentException("empirical row IDs must be unique");
        }
        if (!row.sourceVersion().equals(sourceVersion)
            || !row.aggregationRule().equals(aggregationRule)
            || !row.cutoffBasis().equals(cutoffBasis)) {
          throw new IllegalArgumentException("row audit metadata must match dataset metadata");
        }
        if (!row.values().keySet().stream().allMatch(units::containsKey)
            || !row.missingFields().stream().allMatch(units::containsKey)
            || !row.censoredFields().stream().allMatch(units::containsKey)) {
          throw new IllegalArgumentException("empirical row variables must be declared");
        }
      }
      variableUnits = Map.copyOf(units);
      long calibration = rows.stream().filter(row -> row.role() == SampleRole.CALIBRATION).count();
      long heldOut = rows.stream().filter(row -> row.role() == SampleRole.HELD_OUT).count();
      if (calibration == 0 || heldOut == 0) {
        throw new IllegalArgumentException(
            "empirical datasets require calibration and held-out rows");
      }
    }

    public int calibrationRowCount() {
      return (int) rows.stream().filter(row -> row.role() == SampleRole.CALIBRATION).count();
    }

    public int heldOutRowCount() {
      return (int) rows.stream().filter(row -> row.role() == SampleRole.HELD_OUT).count();
    }

    public boolean sourceAuditComplete() {
      return auditStatus == AuditStatus.RAW_TABLE_AUDITED;
    }

    /** Returns the source-specific dataset selected by a Phase 3 model ID. */
    public static EmpiricalDataset forModel(String modelId) {
      if (MineralSystemProofs.PORPHYRY_MODEL.equals(modelId)) {
        return porphyry();
      }
      if (MineralSystemProofs.VMS_MODEL.equals(modelId)) {
        return vms();
      }
      if (MineralSystemProofs.PLACER_MODEL.equals(modelId)) {
        return placer();
      }
      if (MineralSystemProofs.LCT_MODEL.equals(modelId)) {
        return lctPegmatite();
      }
      if (MineralSystemProofs.BIF_MODEL.equals(modelId)) {
        return bif();
      }
      if (MineralSystemProofs.EVAPORITE_MODEL.equals(modelId)) {
        return evaporite();
      }
      throw new IllegalArgumentException("no empirical dataset is registered for " + modelId);
    }

    private static EmpiricalDataset porphyry() {
      String version = "USGS-SIR-2010-5070-B";
      String aggregation = "aggregate_related_mineralization_within_2_km";
      String cutoff = "production_plus_reserves_resources_lowest_reported_cutoff";
      return dataset(
          "usgs:sir2010_5070b_cu_mo",
          "https://pubs.usgs.gov/sir/2010/5070/b/",
          version,
          "porphyry_cu_calc_alkaline_deposits",
          aggregation,
          cutoff,
          Map.of("tonnage", "Mt", "cu_grade", "mass_fraction", "mo_grade", "mass_fraction"),
          List.of(
              row(
                  "porphyry-q10",
                  "calc_alkaline",
                  0.10,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 100.0, "cu_grade", 0.0022, "mo_grade", 0.00002),
                  Set.of(),
                  Set.of()),
              row(
                  "porphyry-q25",
                  "calc_alkaline",
                  0.25,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 250.0, "cu_grade", 0.0032, "mo_grade", 0.000035),
                  Set.of(),
                  Set.of()),
              row(
                  "porphyry-q50",
                  "calc_alkaline",
                  0.50,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 500.0, "cu_grade", 0.0044, "mo_grade", 0.00006),
                  Set.of(),
                  Set.of()),
              row(
                  "porphyry-q75",
                  "calc_alkaline",
                  0.75,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 1000.0, "cu_grade", 0.0060, "mo_grade", 0.00010),
                  Set.of(),
                  Set.of()),
              row(
                  "porphyry-q90",
                  "calc_alkaline",
                  0.90,
                  SampleRole.HELD_OUT,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 2000.0, "cu_grade", 0.0090, "mo_grade", 0.00018),
                  Set.of(),
                  Set.of("mo_grade"))));
    }

    private static EmpiricalDataset vms() {
      String version = "USGS-OF-2009-1034";
      String aggregation = "group_records_within_500_m";
      String cutoff = "source_model_resource_basis";
      return dataset(
          "usgs:vms_subtype_selected",
          "https://pubs.usgs.gov/of/2009/1034/",
          version,
          "vms_subtype_selected",
          aggregation,
          cutoff,
          Map.of("tonnage", "Mt", "cu_grade", "mass_fraction", "zn_grade", "mass_fraction"),
          List.of(
              row(
                  "vms-q10",
                  "mafic_bimodal",
                  0.10,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 0.2, "cu_grade", 0.006, "zn_grade", 0.015),
                  Set.of(),
                  Set.of()),
              row(
                  "vms-q25",
                  "mafic_bimodal",
                  0.25,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 0.8, "cu_grade", 0.010, "zn_grade", 0.025),
                  Set.of(),
                  Set.of()),
              row(
                  "vms-q50",
                  "mafic_bimodal",
                  0.50,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 2.5, "cu_grade", 0.015, "zn_grade", 0.045),
                  Set.of(),
                  Set.of()),
              row(
                  "vms-q75",
                  "mafic_bimodal",
                  0.75,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 8.0, "cu_grade", 0.022, "zn_grade", 0.070),
                  Set.of(),
                  Set.of()),
              row(
                  "vms-q90",
                  "mafic_bimodal",
                  0.90,
                  SampleRole.HELD_OUT,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 20.0, "cu_grade", 0.035, "zn_grade", 0.110),
                  Set.of(),
                  Set.of("tonnage"))));
    }

    private static EmpiricalDataset placer() {
      String version = "USGS-BUL-1693-placer-model";
      String aggregation = "group_records_within_1_6_km";
      String cutoff = "placer_model_resource_basis";
      return dataset(
          "usgs:model_39a_appropriate_subpopulation",
          "https://pubs.usgs.gov/bul/b1693/html/bull6945.htm",
          version,
          "alluvial_placer_gold_appropriate_subpopulation",
          aggregation,
          cutoff,
          Map.of("tonnage", "Mt", "au_grade", "g_per_tonne"),
          List.of(
              row(
                  "placer-q10",
                  "alluvial",
                  0.10,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 0.001, "au_grade", 0.20),
                  Set.of(),
                  Set.of()),
              row(
                  "placer-q25",
                  "alluvial",
                  0.25,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 0.005, "au_grade", 0.45),
                  Set.of(),
                  Set.of()),
              row(
                  "placer-q50",
                  "alluvial",
                  0.50,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 0.02, "au_grade", 1.10),
                  Set.of(),
                  Set.of()),
              row(
                  "placer-q75",
                  "alluvial",
                  0.75,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 0.08, "au_grade", 2.60),
                  Set.of(),
                  Set.of()),
              row(
                  "placer-q90",
                  "alluvial",
                  0.90,
                  SampleRole.HELD_OUT,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 0.30, "au_grade", 6.0),
                  Set.of(),
                  Set.of("au_grade"))));
    }

    private static EmpiricalDataset lctPegmatite() {
      String version = "USGS-2026-LCT-global";
      String aggregation = "reported_deposit_definition_no_cross_body_merge";
      String cutoff = "lithium_tonnage_grade_resource_basis";
      return dataset(
          "usgs:2026_lct_global",
          "https://pubs.usgs.gov/publication/70276446",
          version,
          "lithium_cesium_tantalum_pegmatites_global",
          aggregation,
          cutoff,
          Map.of("tonnage", "Mt", "li2o_grade", "mass_fraction", "ta2o5_grade", "mass_fraction"),
          List.of(
              row(
                  "lct-q10",
                  "lct_rare_element",
                  0.10,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 0.02, "li2o_grade", 0.015, "ta2o5_grade", 0.0001),
                  Set.of(),
                  Set.of()),
              row(
                  "lct-q25",
                  "lct_rare_element",
                  0.25,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 0.08, "li2o_grade", 0.025, "ta2o5_grade", 0.0003),
                  Set.of(),
                  Set.of()),
              row(
                  "lct-q50",
                  "lct_rare_element",
                  0.50,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 0.30, "li2o_grade", 0.040, "ta2o5_grade", 0.0007),
                  Set.of(),
                  Set.of()),
              row(
                  "lct-q75",
                  "lct_rare_element",
                  0.75,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 1.20, "li2o_grade", 0.065, "ta2o5_grade", 0.0015),
                  Set.of(),
                  Set.of()),
              row(
                  "lct-q90",
                  "lct_rare_element",
                  0.90,
                  SampleRole.HELD_OUT,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 4.0, "li2o_grade", 0.100),
                  Set.of("ta2o5_grade"),
                  Set.of())));
    }

    private static EmpiricalDataset bif() {
      String version = "USGS-BUL-1693-BIF-Superior-Algoma";
      String aggregation = "formation_scale_resource_prior_no_mine_cutoff";
      String cutoff = "broad_ore_resource_prior_not_economic_cutoff";
      return dataset(
          "usgs:bif_superior_algoma",
          "https://pubs.usgs.gov/bul/b1693/html/bull1tut.htm",
          version,
          "banded_iron_formation_superior_algoma_broad_prior",
          aggregation,
          cutoff,
          Map.of("tonnage", "Mt", "fe_grade", "mass_fraction"),
          List.of(
              row(
                  "bif-q10",
                  "algoma",
                  0.10,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 50.0, "fe_grade", 0.25),
                  Set.of(),
                  Set.of()),
              row(
                  "bif-q25",
                  "algoma",
                  0.25,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 150.0, "fe_grade", 0.30),
                  Set.of(),
                  Set.of()),
              row(
                  "bif-q50",
                  "superior",
                  0.50,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 500.0, "fe_grade", 0.35),
                  Set.of(),
                  Set.of()),
              row(
                  "bif-q75",
                  "superior",
                  0.75,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 1500.0, "fe_grade", 0.42),
                  Set.of(),
                  Set.of()),
              row(
                  "bif-q90",
                  "superior",
                  0.90,
                  SampleRole.HELD_OUT,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 5000.0, "fe_grade", 0.55),
                  Set.of(),
                  Set.of("fe_grade"))));
    }

    private static EmpiricalDataset evaporite() {
      String version = "USGS-SIR-2010-5090-S";
      String aggregation = "basin_member_definition_preserved";
      String cutoff = "original_resource_basis";
      return dataset(
          "usgs:sir2010_5090s_global_potash",
          "https://pubs.usgs.gov/publication/sir20105090S",
          version,
          "global_potash_deposits_member_aware",
          aggregation,
          cutoff,
          Map.of("tonnage", "Mt", "k2o_grade", "mass_fraction", "bed_depth", "m"),
          List.of(
              row(
                  "potash-q10",
                  "sylvinite",
                  0.10,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 10.0, "k2o_grade", 0.12, "bed_depth", 250.0),
                  Set.of(),
                  Set.of()),
              row(
                  "potash-q25",
                  "sylvinite",
                  0.25,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 40.0, "k2o_grade", 0.18, "bed_depth", 450.0),
                  Set.of(),
                  Set.of()),
              row(
                  "potash-q50",
                  "sylvinite",
                  0.50,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 120.0, "k2o_grade", 0.24, "bed_depth", 700.0),
                  Set.of(),
                  Set.of()),
              row(
                  "potash-q75",
                  "carnallite",
                  0.75,
                  SampleRole.CALIBRATION,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 350.0, "k2o_grade", 0.30, "bed_depth", 1100.0),
                  Set.of(),
                  Set.of()),
              row(
                  "potash-q90",
                  "carnallite",
                  0.90,
                  SampleRole.HELD_OUT,
                  version,
                  aggregation,
                  cutoff,
                  Map.of("tonnage", 900.0, "k2o_grade", 0.38, "bed_depth", 1600.0),
                  Set.of(),
                  Set.of("bed_depth"))));
    }

    private static EmpiricalDataset dataset(
        String id,
        String sourceUri,
        String sourceVersion,
        String population,
        String aggregationRule,
        String cutoffBasis,
        Map<String, String> variableUnits,
        List<SampleRow> rows) {
      return new EmpiricalDataset(
          id,
          sourceUri,
          sourceVersion,
          population,
          aggregationRule,
          cutoffBasis,
          "Source-specific rows/anchors retain citation and bias notes; redistribution and raw-table licensing require audit.",
          DistributionKind.QUANTILE_TABLE,
          AuditStatus.SOURCE_ANCHORS_PROVISIONAL,
          variableUnits,
          rows);
    }

    private static SampleRow row(
        String rowId,
        String subtype,
        double percentile,
        SampleRole role,
        String sourceVersion,
        String aggregationRule,
        String cutoffBasis,
        Map<String, Double> values,
        Set<String> missingFields,
        Set<String> censoredFields) {
      return new SampleRow(
          rowId,
          subtype,
          percentile,
          role,
          rowId,
          sourceVersion,
          aggregationRule,
          cutoffBasis,
          "published_resource_or_reserve_basis",
          values,
          missingFields,
          censoredFields);
    }
  }

  /** One source row or quantile anchor with the required bias/censoring audit fields. */
  public record SampleRow(
      String rowId,
      String subtype,
      double percentile,
      SampleRole role,
      String sourceRowRef,
      String sourceVersion,
      String aggregationRule,
      String cutoffBasis,
      String resourceBasis,
      Map<String, Double> values,
      Set<String> missingFields,
      Set<String> censoredFields) {
    public SampleRow {
      if (rowId == null
          || rowId.isBlank()
          || subtype == null
          || subtype.isBlank()
          || !Double.isFinite(percentile)
          || percentile < 0.0
          || percentile > 1.0
          || role == null
          || sourceRowRef == null
          || sourceRowRef.isBlank()
          || sourceVersion == null
          || sourceVersion.isBlank()
          || aggregationRule == null
          || aggregationRule.isBlank()
          || cutoffBasis == null
          || cutoffBasis.isBlank()
          || resourceBasis == null
          || resourceBasis.isBlank()
          || values == null
          || missingFields == null
          || censoredFields == null) {
        throw new IllegalArgumentException("empirical row audit fields must be complete");
      }
      TreeMap<String, Double> copied = new TreeMap<>();
      values.forEach(
          (variable, value) -> {
            if (variable == null
                || variable.isBlank()
                || value == null
                || !Double.isFinite(value)
                || value < 0.0) {
              throw new IllegalArgumentException(
                  "empirical row values must be finite and non-negative");
            }
            copied.put(variable, value);
          });
      values = Map.copyOf(copied);
      missingFields = Set.copyOf(missingFields);
      censoredFields = Set.copyOf(censoredFields);
      if (missingFields.stream().anyMatch(variable -> variable == null || variable.isBlank())
          || censoredFields.stream().anyMatch(variable -> variable == null || variable.isBlank())) {
        throw new IllegalArgumentException("empirical row flags must name variables");
      }
      if (missingFields.stream().anyMatch(values::containsKey)
          || missingFields.stream().anyMatch(censoredFields::contains)) {
        throw new IllegalArgumentException(
            "missing empirical values cannot also be present or censored");
      }
      if (values.isEmpty() && missingFields.isEmpty()) {
        throw new IllegalArgumentException(
            "empirical rows require a value or explicit missing field");
      }
    }
  }
}
