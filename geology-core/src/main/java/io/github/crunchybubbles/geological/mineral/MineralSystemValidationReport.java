package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.determinism.StableId;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeMap;

/**
 * Phase 3 empirical-distribution and validation evidence for one primary mineral-system model.
 *
 * <p>Checked-in raw-table imports distinguish complete source releases from audited subsets;
 * neither claims to be an unbiased natural population. Every row retains the provenance and
 * limitation metadata needed for deterministic review.
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
    StatisticalValidation statistical = StatisticalValidation.from(dataset);
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
                "held_out_quantile_comparison",
                statistical.quantileComparisons().size()
                    == dataset.heldOutRowCount() * dataset.variableUnits().size(),
                "Each held-out row and declared variable receives a deterministic quantile comparison."),
            check(
                "held_out_covariance_summary",
                statistical.covarianceSummaries().size()
                    == dataset.variableUnits().size() * (dataset.variableUnits().size() - 1) / 2,
                "Every declared variable pair receives a calibration covariance and held-out availability summary."),
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
                : dataset.auditStatus() == AuditStatus.RAW_TABLE_AUDITED_SUBSET
                    ? ValidationStatus.AUDITED_SUBSET
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

  /** Returns deterministic quantile and covariance evidence for the dataset partitions. */
  public StatisticalValidation statisticalValidation() {
    return StatisticalValidation.from(empiricalDataset);
  }

  public enum ValidationStatus {
    PASSED,
    AUDITED_SUBSET,
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
    RAW_TABLE_AUDITED_SUBSET,
    RAW_TABLE_AUDITED
  }

  public enum SampleRole {
    CALIBRATION,
    HELD_OUT
  }

  public enum StatisticalStatus {
    COMPLETE,
    AUDITED_SUBSET,
    PROVISIONAL_ANCHORS,
    INSUFFICIENT_DATA
  }

  public enum ComparisonStatus {
    AVAILABLE,
    MISSING,
    CENSORED,
    NO_CALIBRATION_DATA,
    INSUFFICIENT_ROWS
  }

  /** Quantile and covariance projections used to audit the calibration/held-out split. */
  public record StatisticalValidation(
      List<QuantileComparison> quantileComparisons,
      List<CovarianceSummary> covarianceSummaries,
      StatisticalStatus status,
      String limitation) {
    public StatisticalValidation {
      if (quantileComparisons == null
          || covarianceSummaries == null
          || status == null
          || limitation == null
          || limitation.isBlank()) {
        throw new IllegalArgumentException("statistical validation evidence must be complete");
      }
      quantileComparisons =
          List.copyOf(quantileComparisons).stream()
              .sorted(
                  Comparator.comparing(QuantileComparison::heldOutRowId)
                      .thenComparing(QuantileComparison::variable))
              .toList();
      covarianceSummaries =
          List.copyOf(covarianceSummaries).stream()
              .sorted(
                  Comparator.comparing(CovarianceSummary::firstVariable)
                      .thenComparing(CovarianceSummary::secondVariable))
              .toList();
      if (quantileComparisons.stream().anyMatch(comparison -> comparison == null)
          || covarianceSummaries.stream().anyMatch(summary -> summary == null)) {
        throw new IllegalArgumentException("statistical validation entries cannot be null");
      }
    }

    private static StatisticalValidation from(EmpiricalDataset dataset) {
      List<SampleRow> calibration =
          dataset.rows().stream().filter(row -> row.role() == SampleRole.CALIBRATION).toList();
      List<SampleRow> heldOut =
          dataset.rows().stream().filter(row -> row.role() == SampleRole.HELD_OUT).toList();
      List<String> variables = dataset.variableUnits().keySet().stream().sorted().toList();
      List<QuantileComparison> quantiles =
          heldOut.stream()
              .flatMap(
                  row ->
                      variables.stream()
                          .map(variable -> quantileComparison(row, variable, calibration)))
              .toList();
      List<CovarianceSummary> covariance =
          java.util.stream.IntStream.range(0, variables.size())
              .boxed()
              .flatMap(
                  firstIndex ->
                      java.util.stream.IntStream.range(firstIndex + 1, variables.size())
                          .mapToObj(
                              secondIndex ->
                                  covarianceSummary(
                                      variables.get(firstIndex),
                                      variables.get(secondIndex),
                                      calibration,
                                      heldOut)))
              .toList();
      StatisticalStatus status =
          dataset.auditStatus() == AuditStatus.RAW_TABLE_AUDITED
              ? StatisticalStatus.COMPLETE
              : dataset.auditStatus() == AuditStatus.RAW_TABLE_AUDITED_SUBSET
                  ? StatisticalStatus.AUDITED_SUBSET
                  : heldOut.isEmpty() || calibration.isEmpty()
                      ? StatisticalStatus.INSUFFICIENT_DATA
                      : StatisticalStatus.PROVISIONAL_ANCHORS;
      String limitation =
          status == StatisticalStatus.COMPLETE
              ? "Source rows are audited; held-out metrics are computed from the declared partitions."
              : status == StatisticalStatus.AUDITED_SUBSET
                  ? "Metrics are computed from the checked-in audited subset; full-population redistribution and coverage remain outstanding."
                  : "Anchor metrics are deterministic review evidence only; raw-table audit and redistribution approval remain outstanding.";
      return new StatisticalValidation(quantiles, covariance, status, limitation);
    }

    private static QuantileComparison quantileComparison(
        SampleRow heldOut, String variable, List<SampleRow> calibration) {
      boolean missing = heldOut.missingFields().contains(variable);
      boolean censored = heldOut.censoredFields().contains(variable);
      OptionalDouble observed =
          heldOut.values().containsKey(variable)
              ? OptionalDouble.of(heldOut.values().get(variable))
              : OptionalDouble.empty();
      List<SampleRow> usable =
          calibration.stream()
              .filter(row -> row.values().containsKey(variable))
              .filter(row -> !row.missingFields().contains(variable))
              .filter(row -> !row.censoredFields().contains(variable))
              .sorted(Comparator.comparingDouble(SampleRow::percentile))
              .toList();
      if (usable.isEmpty()) {
        return new QuantileComparison(
            variable,
            heldOut.rowId(),
            heldOut.percentile(),
            observed,
            OptionalDouble.empty(),
            OptionalDouble.empty(),
            missing
                ? ComparisonStatus.MISSING
                : censored ? ComparisonStatus.CENSORED : ComparisonStatus.NO_CALIBRATION_DATA);
      }
      double predicted = interpolate(usable, variable, heldOut.percentile());
      OptionalDouble error =
          !observed.isPresent() || missing || censored
              ? OptionalDouble.empty()
              : OptionalDouble.of(log10AbsoluteError(observed.getAsDouble(), predicted));
      return new QuantileComparison(
          variable,
          heldOut.rowId(),
          heldOut.percentile(),
          observed,
          OptionalDouble.of(predicted),
          error,
          missing
              ? ComparisonStatus.MISSING
              : censored ? ComparisonStatus.CENSORED : ComparisonStatus.AVAILABLE);
    }

    private static double interpolate(List<SampleRow> rows, String variable, double percentile) {
      if (rows.size() == 1 || percentile <= rows.getFirst().percentile()) {
        return rows.getFirst().values().get(variable);
      }
      if (percentile >= rows.getLast().percentile()) {
        return rows.getLast().values().get(variable);
      }
      for (int index = 1; index < rows.size(); index++) {
        SampleRow lower = rows.get(index - 1);
        SampleRow upper = rows.get(index);
        if (percentile <= upper.percentile()) {
          double fraction =
              (percentile - lower.percentile()) / (upper.percentile() - lower.percentile());
          return lower.values().get(variable)
              + fraction * (upper.values().get(variable) - lower.values().get(variable));
        }
      }
      return rows.getLast().values().get(variable);
    }

    private static double log10AbsoluteError(double observed, double predicted) {
      double floor = 1.0e-12;
      return Math.abs(
          Math.log10(Math.max(observed, floor)) - Math.log10(Math.max(predicted, floor)));
    }

    private static CovarianceSummary covarianceSummary(
        String firstVariable,
        String secondVariable,
        List<SampleRow> calibration,
        List<SampleRow> heldOut) {
      List<double[]> calibrationPairs = pairs(calibration, firstVariable, secondVariable);
      OptionalDouble covariance = covariance(calibrationPairs, false);
      OptionalDouble correlation = covariance(calibrationPairs, true);
      List<double[]> heldOutPairs = pairs(heldOut, firstVariable, secondVariable);
      ComparisonStatus heldOutStatus =
          heldOutPairs.size() >= 2
              ? ComparisonStatus.AVAILABLE
              : heldOut.stream()
                      .anyMatch(
                          row ->
                              row.missingFields().contains(firstVariable)
                                  || row.missingFields().contains(secondVariable))
                  ? ComparisonStatus.MISSING
                  : heldOut.stream()
                          .anyMatch(
                              row ->
                                  row.censoredFields().contains(firstVariable)
                                      || row.censoredFields().contains(secondVariable))
                      ? ComparisonStatus.CENSORED
                      : ComparisonStatus.INSUFFICIENT_ROWS;
      return new CovarianceSummary(
          firstVariable,
          secondVariable,
          calibrationPairs.size(),
          covariance,
          correlation,
          heldOutPairs.size(),
          heldOutStatus);
    }

    private static List<double[]> pairs(
        List<SampleRow> rows, String firstVariable, String secondVariable) {
      return rows.stream()
          .filter(
              row ->
                  row.values().containsKey(firstVariable)
                      && row.values().containsKey(secondVariable))
          .filter(
              row ->
                  !row.missingFields().contains(firstVariable)
                      && !row.missingFields().contains(secondVariable))
          .filter(
              row ->
                  !row.censoredFields().contains(firstVariable)
                      && !row.censoredFields().contains(secondVariable))
          .map(
              row ->
                  new double[] {row.values().get(firstVariable), row.values().get(secondVariable)})
          .toList();
    }

    private static OptionalDouble covariance(List<double[]> pairs, boolean correlation) {
      if (pairs.size() < 2) {
        return OptionalDouble.empty();
      }
      double firstMean = pairs.stream().mapToDouble(pair -> pair[0]).average().orElseThrow();
      double secondMean = pairs.stream().mapToDouble(pair -> pair[1]).average().orElseThrow();
      double numerator =
          pairs.stream().mapToDouble(pair -> (pair[0] - firstMean) * (pair[1] - secondMean)).sum();
      double firstVariance =
          pairs.stream().mapToDouble(pair -> Math.pow(pair[0] - firstMean, 2.0)).sum();
      double secondVariance =
          pairs.stream().mapToDouble(pair -> Math.pow(pair[1] - secondMean, 2.0)).sum();
      if (correlation) {
        if (firstVariance == 0.0 || secondVariance == 0.0) {
          return OptionalDouble.empty();
        }
        return OptionalDouble.of(numerator / Math.sqrt(firstVariance * secondVariance));
      }
      return OptionalDouble.of(numerator / pairs.size());
    }
  }

  public record QuantileComparison(
      String variable,
      String heldOutRowId,
      double targetPercentile,
      OptionalDouble observedValue,
      OptionalDouble predictedValue,
      OptionalDouble absoluteLog10Error,
      ComparisonStatus status) {
    public QuantileComparison {
      if (variable == null
          || variable.isBlank()
          || heldOutRowId == null
          || heldOutRowId.isBlank()
          || !Double.isFinite(targetPercentile)
          || targetPercentile < 0.0
          || targetPercentile > 1.0
          || observedValue == null
          || predictedValue == null
          || absoluteLog10Error == null
          || status == null) {
        throw new IllegalArgumentException("quantile comparison must be complete");
      }
      validateOptional(observedValue, "observed value");
      validateOptional(predictedValue, "predicted value");
      validateOptional(absoluteLog10Error, "absolute log error");
    }
  }

  public record CovarianceSummary(
      String firstVariable,
      String secondVariable,
      int calibrationPairCount,
      OptionalDouble calibrationCovariance,
      OptionalDouble calibrationCorrelation,
      int heldOutPairCount,
      ComparisonStatus heldOutStatus) {
    public CovarianceSummary {
      if (firstVariable == null
          || firstVariable.isBlank()
          || secondVariable == null
          || secondVariable.isBlank()
          || firstVariable.compareTo(secondVariable) >= 0
          || calibrationPairCount < 0
          || calibrationCovariance == null
          || calibrationCorrelation == null
          || heldOutPairCount < 0
          || heldOutStatus == null) {
        throw new IllegalArgumentException("covariance summary must be complete and ordered");
      }
      validateOptional(calibrationCovariance, "calibration covariance");
      validateOptional(calibrationCorrelation, "calibration correlation");
    }
  }

  private static void validateOptional(OptionalDouble value, String label) {
    if (value.isPresent() && !Double.isFinite(value.getAsDouble())) {
      throw new IllegalArgumentException(label + " must be finite when present");
    }
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

    public boolean sourceAuditSubsetComplete() {
      return auditStatus == AuditStatus.RAW_TABLE_AUDITED_SUBSET
          || auditStatus == AuditStatus.RAW_TABLE_AUDITED;
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
        return evaporitePotash();
      }
      throw new IllegalArgumentException("no empirical dataset is registered for " + modelId);
    }

    private static EmpiricalDataset porphyry() {
      String version = "USGS-OFR-2008-1155-v1.0";
      String aggregation = "aggregate_related_mineralization_within_2_km";
      String cutoff = "production_plus_reserves_resources_lowest_reported_cutoff";
      return readPorphyrySubset(version, aggregation, cutoff);
    }

    private static EmpiricalDataset readPorphyrySubset(
        String version, String aggregation, String cutoff) {
      String resourcePath = "/data/geological/empirical/porphyry_cu_subset.tsv";
      var resource = EmpiricalDataset.class.getResourceAsStream(resourcePath);
      if (resource == null) {
        throw new IllegalStateException("missing empirical source resource " + resourcePath);
      }
      List<SampleRow> rows = new ArrayList<>();
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
          lineNumber++;
          String trimmed = line.trim();
          if (trimmed.isEmpty()
              || trimmed.startsWith("#")
              || trimmed.startsWith("source_row_ref")) {
            continue;
          }
          String[] fields = line.split("\\|", -1);
          if (fields.length != 7) {
            throw new IllegalStateException(
                "porphyry source row " + lineNumber + " has " + fields.length + " fields");
          }
          String sourceRowRef = fields[0].trim();
          String rowId = "porphyry-deposit-" + sourceRowRef;
          String subtype = fields[1].trim();
          SampleRole role = SampleRole.valueOf(fields[2].trim());
          double percentile = parseSourceNumber(fields[3], lineNumber, "percentile");
          double tonnage = parseSourceNumber(fields[4], lineNumber, "tonnage");
          double copperPercent = parseSourceNumber(fields[5], lineNumber, "cu_grade_pct");
          double molybdenumPercent = parseSourceNumber(fields[6], lineNumber, "mo_grade_pct");
          if (percentile <= 0.0 || percentile > 1.0) {
            throw new IllegalStateException(
                "porphyry percentile is outside (0,1] at row " + lineNumber);
          }
          rows.add(
              new SampleRow(
                  rowId,
                  subtype,
                  percentile,
                  role,
                  "DepositID=" + sourceRowRef,
                  version,
                  aggregation,
                  cutoff,
                  "production_plus_reserves_resources_source_row;percent_grades_converted_to_mass_fraction",
                  Map.of(
                      "tonnage", tonnage,
                      "cu_grade", copperPercent / 100.0,
                      "mo_grade", molybdenumPercent / 100.0),
                  Set.of(),
                  Set.of()));
        }
      } catch (IOException | IllegalArgumentException exception) {
        throw new IllegalStateException("invalid porphyry empirical source subset", exception);
      }
      if (rows.size() < 10) {
        throw new IllegalStateException("porphyry empirical source subset is unexpectedly small");
      }
      return new EmpiricalDataset(
          "usgs:ofr20081155_porphyry_cu_audited_subset",
          "https://pubs.usgs.gov/of/2008/1155/data/",
          version,
          "porphyry_cu_calc_alkaline_deposits_complete_tonnage_cu_mo_audited_subset",
          aggregation,
          cutoff,
          "USGS Open-File Report 2008-1155 source subset; source rows are checked in for reproducible review. Cu/Mo percentages are converted to mass fractions; the subset is not the full redistributable population.",
          DistributionKind.EMPIRICAL_ROW,
          AuditStatus.RAW_TABLE_AUDITED_SUBSET,
          Map.of("tonnage", "Mt", "cu_grade", "mass_fraction", "mo_grade", "mass_fraction"),
          rows);
    }

    private static double parseSourceNumber(String value, int lineNumber, String field) {
      try {
        double parsed = Double.parseDouble(value.trim());
        if (!Double.isFinite(parsed) || parsed <= 0.0) {
          throw new IllegalArgumentException(field + " must be positive");
        }
        return parsed;
      } catch (NumberFormatException exception) {
        throw new IllegalArgumentException(
            "invalid " + field + " at porphyry source row " + lineNumber, exception);
      }
    }

    private static EmpiricalDataset vms() {
      String version = "USGS-OF-2009-1034-v1.0";
      String aggregation = "group_records_within_500_m";
      String cutoff = "source_model_resource_basis";
      return readVmsSubset(version, aggregation, cutoff);
    }

    private static EmpiricalDataset readVmsSubset(
        String version, String aggregation, String cutoff) {
      String resourcePath = "/data/geological/empirical/vms_subset.tsv";
      var resource = EmpiricalDataset.class.getResourceAsStream(resourcePath);
      if (resource == null) {
        throw new IllegalStateException("missing empirical source resource " + resourcePath);
      }
      List<SampleRow> rows = new ArrayList<>();
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
          lineNumber++;
          String trimmed = line.trim();
          if (trimmed.isEmpty()
              || trimmed.startsWith("#")
              || trimmed.startsWith("source_row_ref")) {
            continue;
          }
          String[] fields = line.split("\\|", -1);
          if (fields.length != 8) {
            throw new IllegalStateException(
                "VMS source row " + lineNumber + " has " + fields.length + " fields");
          }
          String sourceRowRef = fields[0].trim();
          String depositName = fields[1].trim();
          String rowId = "vms-deposit-" + sourceRowRef.replace(':', '-') + "-" + depositName;
          String subtype = fields[2].trim();
          SampleRole role = SampleRole.valueOf(fields[3].trim());
          double percentile = parseSourceNumber(fields[4], lineNumber, "percentile");
          double tonnage = parseSourceNumber(fields[5], lineNumber, "tonnage");
          double copperPercent = parseSourceNumber(fields[6], lineNumber, "cu_grade_pct");
          double zincPercent = parseSourceNumber(fields[7], lineNumber, "zn_grade_pct");
          if (percentile <= 0.0 || percentile > 1.0 || depositName.isBlank()) {
            throw new IllegalStateException("invalid VMS source identity at row " + lineNumber);
          }
          rows.add(
              new SampleRow(
                  rowId,
                  subtype,
                  percentile,
                  role,
                  sourceRowRef,
                  version,
                  aggregation,
                  cutoff,
                  "source_model_resource_row;percent_grades_converted_to_mass_fraction",
                  Map.of(
                      "tonnage", tonnage,
                      "cu_grade", copperPercent / 100.0,
                      "zn_grade", zincPercent / 100.0),
                  Set.of(),
                  Set.of()));
        }
      } catch (IOException | IllegalArgumentException exception) {
        throw new IllegalStateException("invalid VMS empirical source subset", exception);
      }
      if (rows.size() < 10) {
        throw new IllegalStateException("VMS empirical source subset is unexpectedly small");
      }
      return new EmpiricalDataset(
          "usgs:of20091034_vms_cu_zn_audited_subset",
          "https://pubs.usgs.gov/of/2009/1034/of2009-1034_data.zip",
          version,
          "vms_global_deposits_positive_tonnage_cu_zn_audited_subset",
          aggregation,
          cutoff,
          "USGS Open-File Report 2009-1034 VMS data-package subset; source row references and subtype names are checked in for reproducible review. Cu/Zn percentages are converted to mass fractions; zero/unknown source rows are excluded under the declared population rule.",
          DistributionKind.EMPIRICAL_ROW,
          AuditStatus.RAW_TABLE_AUDITED_SUBSET,
          Map.of("tonnage", "Mt", "cu_grade", "mass_fraction", "zn_grade", "mass_fraction"),
          rows);
    }

    private static EmpiricalDataset placer() {
      String version = "USGS-OFR-1993-0280-v1.0";
      String aggregation = "source_deposit_or_district_as_published";
      String cutoff = "production_plus_reserves_resources_lowest_reported_cutoff";
      return readPlacerSubset(version, aggregation, cutoff);
    }

    private static EmpiricalDataset readPlacerSubset(
        String version, String aggregation, String cutoff) {
      String resourcePath = "/data/geological/empirical/placer_subset.tsv";
      var resource = EmpiricalDataset.class.getResourceAsStream(resourcePath);
      if (resource == null) {
        throw new IllegalStateException("missing empirical source resource " + resourcePath);
      }
      List<SampleRow> rows = new ArrayList<>();
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
          lineNumber++;
          String trimmed = line.trim();
          if (trimmed.isEmpty()
              || trimmed.startsWith("#")
              || trimmed.startsWith("source_row_ref")) {
            continue;
          }
          String[] fields = line.split("\\|", -1);
          if (fields.length != 12) {
            throw new IllegalStateException(
                "placer source row " + lineNumber + " has " + fields.length + " fields");
          }
          String sourceRowRef = fields[0].trim();
          String depositName = fields[1].trim();
          String countryCode = fields[2].trim();
          String district = fields[3].trim();
          SampleRole role = SampleRole.valueOf(fields[4].trim());
          double percentile = parseSourceNumber(fields[5], lineNumber, "percentile");
          double tonnage = parseSourceNumber(fields[6], lineNumber, "tonnage_mt");
          double platinumPpb = parseSourceNumber(fields[7], lineNumber, "pt_grade_ppb");
          double goldGramsPerTonne = parseSourceNumber(fields[8], lineNumber, "au_grade_gpt");
          String osmiumPpb = fields[9].trim();
          String iridiumPpb = fields[10].trim();
          String palladiumPpb = fields[11].trim();
          if (sourceRowRef.isBlank()
              || depositName.isBlank()
              || countryCode.isBlank()
              || percentile <= 0.0
              || percentile > 1.0
              || osmiumPpb.isBlank()
              || iridiumPpb.isBlank()
              || palladiumPpb.isBlank()) {
            throw new IllegalStateException("invalid placer source identity at row " + lineNumber);
          }
          String sourceRow =
              "OFR-93-0280.pdf:"
                  + sourceRowRef
                  + ";Name="
                  + depositName
                  + ";Country="
                  + countryCode
                  + ";District="
                  + district;
          String resourceBasis =
              "Tonnes/10^6_to_Mt;Pt_ppb_to_g_per_tonne;Au_g_per_tonne;Os_ppb="
                  + osmiumPpb
                  + ";Ir_ppb="
                  + iridiumPpb
                  + ";Pd_ppb="
                  + palladiumPpb
                  + ";source_deposit_or_district_as_published";
          rows.add(
              new SampleRow(
                  "placer-deposit-" + sourceRowRef.replace(':', '-') + "-" + depositName,
                  "placer_pt_au",
                  percentile,
                  role,
                  sourceRow,
                  version,
                  aggregation,
                  cutoff,
                  resourceBasis,
                  Map.of(
                      "tonnage", tonnage,
                      "pt_grade", platinumPpb / 1_000.0,
                      "au_grade", goldGramsPerTonne),
                  Set.of(),
                  Set.of()));
        }
      } catch (IOException | IllegalArgumentException exception) {
        throw new IllegalStateException("invalid placer empirical source subset", exception);
      }
      if (rows.size() < 10) {
        throw new IllegalStateException("placer empirical source subset is unexpectedly small");
      }
      return new EmpiricalDataset(
          "usgs:of93280_placer_pt_au_audited_subset",
          "https://pubs.usgs.gov/of/1993/ofr-93-0280/of93-0280.pdf",
          version,
          "placer_pt_au_deposits_positive_tonnage_pt_au_audited_subset",
          aggregation,
          cutoff,
          "USGS Open-File Report 93-0280 Placer Pt-Au table subset; source page references, country/district labels, and Os/Ir/Pd companion fields are retained in each row's resource basis. Pt ppb is converted to g/t, Au remains g/t, and the subset is not the full population.",
          DistributionKind.EMPIRICAL_ROW,
          AuditStatus.RAW_TABLE_AUDITED_SUBSET,
          Map.of("tonnage", "Mt", "pt_grade", "g_per_tonne", "au_grade", "g_per_tonne"),
          rows);
    }

    private static EmpiricalDataset lctPegmatite() {
      String version = "USGS-2026-LCT-v2.0";
      String aggregation = "reported_deposit_definition_no_cross_body_merge";
      String cutoff = "lithium_tonnage_grade_resource_basis";
      return readLctSubset(version, aggregation, cutoff);
    }

    private static EmpiricalDataset readLctSubset(
        String version, String aggregation, String cutoff) {
      String resourcePath = "/data/geological/empirical/lct_subset.tsv";
      var resource = EmpiricalDataset.class.getResourceAsStream(resourcePath);
      if (resource == null) {
        throw new IllegalStateException("missing empirical source resource " + resourcePath);
      }
      List<SampleRow> rows = new ArrayList<>();
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
          lineNumber++;
          String trimmed = line.trim();
          if (trimmed.isEmpty()
              || trimmed.startsWith("#")
              || trimmed.startsWith("source_row_ref")) {
            continue;
          }
          String[] fields = line.split("\\|", -1);
          if (fields.length != 9) {
            throw new IllegalStateException(
                "LCT source row " + lineNumber + " has " + fields.length + " fields");
          }
          String sourceRowRef = fields[0].trim();
          String depositName = fields[1].trim();
          String subtype = fields[2].trim();
          SampleRole role = SampleRole.valueOf(fields[3].trim());
          double percentile = parseSourceNumber(fields[4], lineNumber, "percentile");
          double oreTonnageTonnes = parseSourceNumber(fields[5], lineNumber, "ore_tonnage_tonnes");
          double lithiumPercent = parseSourceNumber(fields[6], lineNumber, "li2o_pct");
          String tantalumField = fields[7].trim();
          String cutoffField = fields[8].trim();
          if (depositName.isBlank() || percentile <= 0.0 || percentile > 1.0) {
            throw new IllegalStateException("invalid LCT source identity at row " + lineNumber);
          }
          Map<String, Double> values = new TreeMap<>();
          values.put("tonnage", oreTonnageTonnes / 1_000_000.0);
          values.put("li2o_grade", lithiumPercent / 100.0);
          Set<String> missing = Set.of();
          if (tantalumField.isBlank()) {
            missing = Set.of("ta2o5_grade");
          } else {
            values.put(
                "ta2o5_grade",
                parseSourceNumber(tantalumField, lineNumber, "ta2o5_ppm") / 1_000_000.0);
          }
          String resourceBasis =
              "source_resource_row;ore_tonnage_tonnes_to_Mt;Li2O_percent_to_mass_fraction;Ta2O5_ppm_to_mass_fraction;cutoff_percent="
                  + (cutoffField.isBlank() ? "not_reported" : cutoffField);
          rows.add(
              new SampleRow(
                  "lct-deposit-" + sourceRowRef + "-" + depositName,
                  subtype,
                  percentile,
                  role,
                  "LiCsRb_peg_GT_Deposits.csv:ID=" + sourceRowRef,
                  version,
                  aggregation,
                  cutoff,
                  resourceBasis,
                  values,
                  missing,
                  Set.of()));
        }
      } catch (IOException | IllegalArgumentException exception) {
        throw new IllegalStateException("invalid LCT empirical source subset", exception);
      }
      if (rows.size() < 10) {
        throw new IllegalStateException("LCT empirical source subset is unexpectedly small");
      }
      return new EmpiricalDataset(
          "usgs:2026_lct_global_audited_subset",
          "https://data.usgs.gov/datacatalog/data/USGS%3A66db3cb7d34eef5af66d9306",
          version,
          "lithium_dominated_pegmatite_resources_positive_tonnage_li2o_audited_subset",
          aggregation,
          cutoff,
          "USGS 2026 v2.0 Li-Cs-Rb pegmatite data release subset; source IDs, subtype mineral labels, cutoffs, and explicit Ta missingness are checked in for reproducible review. Unit conversions are retained in each row's resource basis; the subset is not the full population.",
          DistributionKind.EMPIRICAL_ROW,
          AuditStatus.RAW_TABLE_AUDITED_SUBSET,
          Map.of("tonnage", "Mt", "li2o_grade", "mass_fraction", "ta2o5_grade", "mass_fraction"),
          rows);
    }

    private static EmpiricalDataset bif() {
      String version = "USGS-OFR-1993-0280-v1.0";
      String aggregation = "source_deposit_unit_as_published";
      String cutoff = "production_plus_reserves_resources_lowest_reported_cutoff";
      return readBifFull(version, aggregation, cutoff);
    }

    private static EmpiricalDataset readBifFull(String version, String aggregation, String cutoff) {
      String resourcePath = "/data/geological/empirical/bif_full.tsv";
      var resource = EmpiricalDataset.class.getResourceAsStream(resourcePath);
      if (resource == null) {
        throw new IllegalStateException("missing empirical source resource " + resourcePath);
      }
      List<SampleRow> rows = new ArrayList<>();
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
          lineNumber++;
          String trimmed = line.trim();
          if (trimmed.isEmpty()
              || trimmed.startsWith("#")
              || trimmed.startsWith("source_row_ref")) {
            continue;
          }
          String[] fields = line.split("\\|", -1);
          if (fields.length != 8) {
            throw new IllegalStateException(
                "BIF source row " + lineNumber + " has " + fields.length + " fields");
          }
          String sourceRowRef = fields[0].trim();
          String depositName = fields[1].trim();
          String countryCode = fields[2].trim();
          SampleRole role = SampleRole.valueOf(fields[3].trim());
          double percentile = parseSourceNumber(fields[4], lineNumber, "percentile");
          double tonnage = parseSourceNumber(fields[5], lineNumber, "tonnage_mt");
          double ironPercent = parseSourceNumber(fields[6], lineNumber, "fe_grade_pct");
          String phosphorusField = fields[7].trim();
          if (sourceRowRef.isBlank()
              || depositName.isBlank()
              || countryCode.isBlank()
              || percentile <= 0.0
              || percentile > 1.0) {
            throw new IllegalStateException("invalid BIF source identity at row " + lineNumber);
          }
          TreeMap<String, Double> values = new TreeMap<>();
          values.put("tonnage", tonnage);
          values.put("fe_grade", ironPercent / 100.0);
          Set<String> missing = Set.of();
          if (phosphorusField.isBlank()) {
            missing = Set.of("p_grade");
          } else {
            values.put(
                "p_grade",
                parseSourceNonNegativeNumber(phosphorusField, lineNumber, "p_grade_pct") / 100.0);
          }
          rows.add(
              new SampleRow(
                  "bif-deposit-" + sourceRowRef.replace(':', '-') + "-" + depositName,
                  "superior_algoma_combined",
                  percentile,
                  role,
                  "OFR-93-0280.pdf:"
                      + sourceRowRef
                      + ";Name="
                      + depositName
                      + ";Country="
                      + countryCode,
                  version,
                  aggregation,
                  cutoff,
                  "Tonnes/10^6_to_Mt;Fe_percent_to_mass_fraction;P_percent_to_mass_fraction;source_combines_Superior_and_Algoma;country_code="
                      + countryCode,
                  values,
                  missing,
                  Set.of()));
        }
      } catch (IOException | IllegalArgumentException exception) {
        throw new IllegalStateException("invalid BIF empirical source table", exception);
      }
      if (rows.size() != 66) {
        throw new IllegalStateException(
            "BIF empirical source table must contain the complete 66-row report table");
      }
      return new EmpiricalDataset(
          "usgs:of93280_superior_algoma_fe_audited_full",
          "https://pubs.usgs.gov/of/1993/ofr-93-0280/of93-0280.pdf",
          version,
          "superior_algoma_bif_deposits_complete_66_row_table_positive_tonnage_fe_audited",
          aggregation,
          cutoff,
          "USGS Open-File Report 93-0280 Superior-Algoma Fe table; all 66 published rows, source page references, country codes, Fe/P grades, and the combined-model rule are checked in for reproducible review. Fe/P percentages are converted to mass fractions; blank P values remain missing. The complete historical table is audited as a source release, not asserted to be an unbiased natural population.",
          DistributionKind.EMPIRICAL_ROW,
          AuditStatus.RAW_TABLE_AUDITED,
          Map.of("tonnage", "Mt", "fe_grade", "mass_fraction", "p_grade", "mass_fraction"),
          rows);
    }

    private static double parseSourceNonNegativeNumber(String value, int lineNumber, String field) {
      try {
        double parsed = Double.parseDouble(value.trim());
        if (!Double.isFinite(parsed) || parsed < 0.0) {
          throw new IllegalArgumentException(field + " must be non-negative");
        }
        return parsed;
      } catch (NumberFormatException exception) {
        throw new IllegalArgumentException(
            "invalid " + field + " at BIF source row " + lineNumber, exception);
      }
    }

    private static EmpiricalDataset evaporitePotash() {
      String version = "USGS-SIR-2010-5090-S-v1.0";
      String aggregation = "deposit_row_definition_preserved";
      String cutoff = "positive_rr_ore_and_rr_k2o_resource_basis";
      return readPotashSubset(version, aggregation, cutoff);
    }

    private static EmpiricalDataset readPotashSubset(
        String version, String aggregation, String cutoff) {
      String resourcePath = "/data/geological/empirical/potash_subset.tsv";
      var resource = EmpiricalDataset.class.getResourceAsStream(resourcePath);
      if (resource == null) {
        throw new IllegalStateException("missing empirical source resource " + resourcePath);
      }
      List<SampleRow> rows = new ArrayList<>();
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
          lineNumber++;
          String trimmed = line.trim();
          if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("source_id")) {
            continue;
          }
          String[] fields = line.split("\\|", -1);
          if (fields.length != 13) {
            throw new IllegalStateException(
                "potash source row " + lineNumber + " has " + fields.length + " fields");
          }
          String sourceId = fields[0].trim();
          String depositName = fields[1].trim();
          String subtype = fields[2].trim();
          SampleRole role = SampleRole.valueOf(fields[3].trim());
          double percentile = parseSourceNumber(fields[4], lineNumber, "percentile");
          double tonnage = parseSourceNumber(fields[5], lineNumber, "rr_ore_mt");
          double k2oPercent = parseSourceNumber(fields[6], lineNumber, "rr_k2o_pct");
          double bedDepth = parseSourceNumber(fields[7], lineNumber, "bed_depth_m");
          String basin = fields[8].trim();
          String minerals = fields[9].trim();
          String unit = fields[10].trim();
          String depthSourceText = fields[11].trim();
          String resourceStatus = fields[12].trim();
          if (sourceId.isBlank()
              || depositName.isBlank()
              || subtype.isBlank()
              || basin.isBlank()
              || minerals.isBlank()
              || unit.isBlank()
              || depthSourceText.isBlank()
              || resourceStatus.isBlank()
              || percentile <= 0.0
              || percentile > 1.0) {
            throw new IllegalStateException("invalid potash source identity at row " + lineNumber);
          }
          String sourceRowRef =
              "PotashDeposits.xlsx:ID="
                  + sourceId
                  + ";SITE="
                  + depositName
                  + ";BASIN="
                  + basin
                  + ";UNIT="
                  + unit;
          String resourceBasis =
              "RR_ORE_MT_to_Mt;RR_K2O_PCT_to_mass_fraction;K_DEPTH_M_first_numeric_bound="
                  + depthSourceText
                  + ";K_MINERALS="
                  + minerals
                  + ";P_STATUS="
                  + resourceStatus;
          rows.add(
              new SampleRow(
                  "potash-deposit-" + sourceId + "-" + depositName,
                  subtype,
                  percentile,
                  role,
                  sourceRowRef,
                  version,
                  aggregation,
                  cutoff,
                  resourceBasis,
                  Map.of(
                      "tonnage", tonnage,
                      "k2o_grade", k2oPercent / 100.0,
                      "bed_depth", bedDepth),
                  Set.of(),
                  Set.of()));
        }
      } catch (IOException | IllegalArgumentException exception) {
        throw new IllegalStateException("invalid potash empirical source subset", exception);
      }
      if (rows.size() < 10) {
        throw new IllegalStateException("potash empirical source subset is unexpectedly small");
      }
      return new EmpiricalDataset(
          "usgs:sir20105090s_global_potash_audited_subset",
          "https://pubs.usgs.gov/sir/2010/5090/s/PotashXL.zip",
          version,
          "global_potash_deposits_positive_rr_ore_k2o_audited_subset",
          aggregation,
          cutoff,
          "USGS Scientific Investigations Report 2010-5090-S PotashXL deposit subset; source IDs, deposit names, basin/member metadata, resource statuses, and raw depth text are checked in for reproducible review. K2O percentages are converted to mass fractions and ranged depth text uses its first numeric bound; the subset is not the full population.",
          DistributionKind.EMPIRICAL_ROW,
          AuditStatus.RAW_TABLE_AUDITED_SUBSET,
          Map.of("tonnage", "Mt", "k2o_grade", "mass_fraction", "bed_depth", "m"),
          rows);
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
