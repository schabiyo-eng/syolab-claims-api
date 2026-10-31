package dev.syolab.claims.domain;

import dev.syolab.claims.auth.Role;
import java.time.Instant;
import java.util.UUID;

public record DecisionChange(
    UUID claimId, Instant at, String actor, Role role, Decision from, Decision to, String note)
    implements Activity {

  @Override
  public ActivityAction action() {
    return ActivityAction.DECIDED;
  }
}
