package io.github.crunchybubbles.geological.mineral;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.trace.ProvenanceStep;
import java.util.List;

/** Formed, barren, or rejected outcome with deterministic rule evidence. */
public record MineralSystemDecision(
    StableId candidateId,
    String modelId,
    FormationStatus status,
    DepositDescriptor deposit,
    List<GateEvidence> gates,
    List<ProvenanceStep> provenance,
    FixedPointLedger ledger) {
  public MineralSystemDecision {
    if (candidateId == null || modelId == null || modelId.isBlank() || status == null) {
      throw new IllegalArgumentException("mineral-system decision identity is required");
    }
    gates = List.copyOf(gates);
    provenance = List.copyOf(provenance);
    if (status == FormationStatus.FORMED && deposit == null) {
      throw new IllegalArgumentException("formed systems require a deposit descriptor");
    }
    if (status != FormationStatus.FORMED && deposit != null) {
      throw new IllegalArgumentException("non-formed systems cannot have a deposit descriptor");
    }
    boolean hasFailedGate = gates.stream().anyMatch(gate -> gate.status() == GateStatus.FAIL);
    if (status == FormationStatus.FORMED && hasFailedGate) {
      throw new IllegalArgumentException("formed systems cannot contain a failed hard gate");
    }
    if (status != FormationStatus.FORMED && !hasFailedGate) {
      throw new IllegalArgumentException("non-formed systems require an explicit failed gate");
    }
  }
}
