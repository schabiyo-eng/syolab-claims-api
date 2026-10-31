package dev.syolab.claims.domain;

import java.util.List;

public record ClaimFile(Claim claim, List<DecisionChange> history) {
  public ClaimFile {
    history = List.copyOf(history);
  }

  /** A claim that was just submitted has no decisions yet. */
  public static ClaimFile submitted(Claim claim) {
    return new ClaimFile(claim, List.of());
  }

  public Decision decision() {
    return Decision.current(history);
  }
}
