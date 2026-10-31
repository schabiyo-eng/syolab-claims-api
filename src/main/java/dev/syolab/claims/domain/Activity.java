package dev.syolab.claims.domain;

import dev.syolab.claims.auth.Role;
import java.time.Instant;
import java.util.UUID;

public sealed interface Activity permits DecisionChange, Comment {
  UUID claimId();

  Instant at();

  String actor();

  Role role();

  ActivityAction action();
}
