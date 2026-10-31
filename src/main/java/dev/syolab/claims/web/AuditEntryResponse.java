package dev.syolab.claims.web;

import dev.syolab.claims.auth.Role;
import dev.syolab.claims.domain.Activity;
import dev.syolab.claims.domain.ActivityAction;
import dev.syolab.claims.domain.Comment;
import dev.syolab.claims.domain.Decision;
import dev.syolab.claims.domain.DecisionChange;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record AuditEntryResponse(
    long sequence,
    Instant at,
    String actor,
    Role role,
    ActivityAction action,
    String claimId,
    Decision fromDecision,
    Decision toDecision,
    String commentId) {

  /** sequence is position + 1, so it never needs storing. */
  public static List<AuditEntryResponse> list(List<Activity> log) {
    List<AuditEntryResponse> entries = new ArrayList<>();
    for (int i = 0; i < log.size(); i++) {
      entries.add(from(log.get(i), i + 1L));
    }
    return entries;
  }

  private static AuditEntryResponse from(Activity activity, long sequence) {
    if (activity instanceof DecisionChange change) {
      return new AuditEntryResponse(
          sequence,
          change.at(),
          change.actor(),
          change.role(),
          change.action(),
          change.claimId().toString(),
          change.from(),
          change.to(),
          null);
    }
    Comment comment = (Comment) activity;
    return new AuditEntryResponse(
        sequence,
        comment.at(),
        comment.actor(),
        comment.role(),
        comment.action(),
        comment.claimId().toString(),
        null,
        null,
        comment.id().toString());
  }
}
