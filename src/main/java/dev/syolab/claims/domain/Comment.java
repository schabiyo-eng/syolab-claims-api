package dev.syolab.claims.domain;

import dev.syolab.claims.auth.Role;
import java.time.Instant;
import java.util.UUID;

public record Comment(UUID id, UUID claimId, Instant at, String actor, Role role, String body)
    implements Activity {

  @Override
  public ActivityAction action() {
    return ActivityAction.COMMENTED;
  }
}
