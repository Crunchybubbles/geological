package io.github.crunchybubbles.geological.petrology;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sparse magma partition-response proxies for the tracked element/reservoir subset.
 *
 * <p>Values are normalized crystal-capture fractions keyed by sulfur-saturation state. They are
 * authored routing proxies with explicit review status, not measured partition coefficients or an
 * equilibrium calculation.
 */
public final class ElementPartitionResponseCatalog {
  public static final String VERSION = "phase9-alpha.5-proxy-v1";
  public static final String REVIEW_MANIFEST_VERSION = "phase9-alpha.7-review-manifest-v1";
  private static final String PROXY_EVIDENCE_ID = "geological:phase9/partition-proxy";
  private static final List<Response> RESPONSES = responses();
  private static final List<ReviewEvidence> REVIEW_EVIDENCE = compileReviewEvidence();

  private ElementPartitionResponseCatalog() {}

  /** Returns every sparse response in stable element/saturation order. */
  public static List<Response> all() {
    return RESPONSES;
  }

  /** Returns the source and redistribution dispositions needed before external review. */
  public static List<ReviewEvidence> reviewEvidence() {
    return REVIEW_EVIDENCE;
  }

  /** Returns the authored response for one element and sulfur-saturation state. */
  public static Response require(
      ChemicalElement element, MagmaDifferentiationState.SulfurSaturationHistory saturation) {
    if (element == null || saturation == null) {
      throw new IllegalArgumentException("partition response key is required");
    }
    return RESPONSES.stream()
        .filter(response -> response.element() == element)
        .filter(response -> response.sulfurSaturation() == saturation)
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "partition response is not authored for " + element + " / " + saturation));
  }

  private static List<Response> responses() {
    List<Response> result = new ArrayList<>();
    add(result, ChemicalElement.P, 100_000L, 100_000L, 100_000L);
    add(result, ChemicalElement.S, 100_000L, 350_000L, 650_000L);
    add(result, ChemicalElement.F, 50_000L, 50_000L, 50_000L);
    add(result, ChemicalElement.CL, 50_000L, 50_000L, 50_000L);
    add(result, ChemicalElement.K, 150_000L, 150_000L, 150_000L);
    add(result, ChemicalElement.CU, 100_000L, 450_000L, 700_000L);
    add(result, ChemicalElement.ZN, 80_000L, 350_000L, 600_000L);
    add(result, ChemicalElement.AU, 120_000L, 500_000L, 750_000L);
    add(result, ChemicalElement.LI, 30_000L, 30_000L, 30_000L);
    add(result, ChemicalElement.BE, 50_000L, 50_000L, 50_000L);
    add(result, ChemicalElement.B, 50_000L, 50_000L, 50_000L);
    add(result, ChemicalElement.RB, 100_000L, 100_000L, 100_000L);
    add(result, ChemicalElement.CS, 80_000L, 80_000L, 80_000L);
    add(result, ChemicalElement.NB, 70_000L, 70_000L, 70_000L);
    add(result, ChemicalElement.MO, 180_000L, 420_000L, 650_000L);
    add(result, ChemicalElement.AG, 100_000L, 350_000L, 600_000L);
    add(result, ChemicalElement.SN, 100_000L, 300_000L, 500_000L);
    add(result, ChemicalElement.TA, 70_000L, 70_000L, 70_000L);
    add(result, ChemicalElement.W, 120_000L, 350_000L, 550_000L);
    add(result, ChemicalElement.RE, 120_000L, 350_000L, 600_000L);
    add(result, ChemicalElement.PB, 100_000L, 350_000L, 600_000L);
    add(result, ChemicalElement.TH, 30_000L, 30_000L, 30_000L);
    add(result, ChemicalElement.U, 50_000L, 50_000L, 50_000L);
    return result.stream()
        .sorted(Comparator.comparing(Response::element).thenComparing(Response::sulfurSaturation))
        .toList();
  }

  private static List<ReviewEvidence> compileReviewEvidence() {
    return List.of(
        new ReviewEvidence(
            PROXY_EVIDENCE_ID,
            SourceKind.PROJECT_AUTHORED,
            URI.create("https://github.com/Crunchybubbles/geological"),
            LicenseDisposition.PROJECT_MIT,
            "All 69 response values are authored routing proxies in this repository; no third-party table is bundled."),
        new ReviewEvidence(
            "earthref:germ/kdd",
            SourceKind.EXTERNAL_CANDIDATE,
            URI.create("https://earthref.org/KDD-old/"),
            LicenseDisposition.REDISTRIBUTION_UNVERIFIED,
            "Candidate peer-reviewed partition source for scientific comparison; licensing and row-level selection require review before redistribution."));
  }

  private static void add(
      List<Response> target,
      ChemicalElement element,
      long undersaturated,
      long approachingSaturation,
      long saturated) {
    target.add(
        response(
            element,
            MagmaDifferentiationState.SulfurSaturationHistory.UNDERSATURATED,
            undersaturated));
    target.add(
        response(
            element,
            MagmaDifferentiationState.SulfurSaturationHistory.APPROACHING_SATURATION,
            approachingSaturation));
    target.add(
        response(element, MagmaDifferentiationState.SulfurSaturationHistory.SATURATED, saturated));
  }

  private static Response response(
      ChemicalElement element,
      MagmaDifferentiationState.SulfurSaturationHistory saturation,
      long crystalCapturePpm) {
    return new Response(
        element,
        saturation,
        crystalCapturePpm,
        250_000L,
        ReviewStatus.AUTHORED_PROXY,
        "project-authored sulfur-saturation capture proxy; external partition review required");
  }

  public record Response(
      ChemicalElement element,
      MagmaDifferentiationState.SulfurSaturationHistory sulfurSaturation,
      long crystalCapturePpm,
      long confidencePpm,
      ReviewStatus reviewStatus,
      String provenance) {
    public Response {
      if (element == null
          || sulfurSaturation == null
          || crystalCapturePpm < 0L
          || crystalCapturePpm > MaterialAssemblage.SCALE
          || confidencePpm < 0L
          || confidencePpm > MaterialAssemblage.SCALE
          || reviewStatus == null
          || provenance == null
          || provenance.isBlank()) {
        throw new IllegalArgumentException("partition response is incomplete or out of bounds");
      }
    }

    /** Identifies the review-manifest entry governing this authored response row. */
    public String evidenceId() {
      return PROXY_EVIDENCE_ID;
    }
  }

  public record ReviewEvidence(
      String evidenceId,
      SourceKind sourceKind,
      URI sourceUri,
      LicenseDisposition licenseDisposition,
      String scope) {
    public ReviewEvidence {
      if (evidenceId == null
          || evidenceId.isBlank()
          || sourceKind == null
          || sourceUri == null
          || !"https".equalsIgnoreCase(sourceUri.getScheme())
          || licenseDisposition == null
          || scope == null
          || scope.isBlank()) {
        throw new IllegalArgumentException("partition review evidence is incomplete or unsafe");
      }
    }
  }

  public enum SourceKind {
    PROJECT_AUTHORED,
    EXTERNAL_CANDIDATE
  }

  public enum LicenseDisposition {
    PROJECT_MIT,
    REDISTRIBUTION_UNVERIFIED
  }

  public enum ReviewStatus {
    AUTHORED_PROXY,
    EXTERNALLY_REVIEWED
  }
}
