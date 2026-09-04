package io.github.crunchybubbles.geological.petrology;

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
  private static final List<Response> RESPONSES = responses();

  private ElementPartitionResponseCatalog() {}

  /** Returns every sparse response in stable element/saturation order. */
  public static List<Response> all() {
    return RESPONSES;
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
  }

  public enum ReviewStatus {
    AUTHORED_PROXY,
    EXTERNALLY_REVIEWED
  }
}
