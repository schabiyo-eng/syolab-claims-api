package dev.syolab.claims.domain;

import java.util.List;

public enum Decision {
  PENDING,
  APPROVED,
  DENIED,
  INFO_REQUESTED;

  public boolean isFinal() {
    return this == APPROVED || this == DENIED;
  }

  public boolean needsNote() {
    return this == DENIED || this == INFO_REQUESTED;
  }

  /** PENDING for an empty history, else the last entry's to(). */
  public static Decision current(List<DecisionChange> history) {
    if (history.isEmpty()) {
      return PENDING;
    }
    return history.get(history.size() - 1).to();
  }
}
