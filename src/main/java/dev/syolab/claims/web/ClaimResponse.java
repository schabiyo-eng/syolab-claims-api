package dev.syolab.claims.web;

import dev.syolab.claims.domain.ActivityAction;
import dev.syolab.claims.domain.ClaimFile;
import dev.syolab.claims.domain.ClaimStatus;
import dev.syolab.claims.domain.Decision;
import dev.syolab.claims.domain.DecisionChange;
import dev.syolab.claims.domain.ValidationFinding;
import dev.syolab.claims.service.MoneyParser;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ClaimResponse(
    String id,
    String policyNumber,
    String claimantName,
    String amount,
    LocalDate incidentDate,
    String description,
    List<String> documentNames,
    ClaimStatus status,
    List<ValidationFinding> findings,
    String payout,
    Instant createdAt,
    Decision decision,
    List<HistoryEntryResponse> history) {

  public static ClaimResponse from(ClaimFile file) {
    var claim = file.claim();
    return new ClaimResponse(
        claim.id().toString(),
        claim.policyNumber(),
        claim.claimantName(),
        MoneyParser.format(claim.amount()),
        claim.incidentDate(),
        claim.description(),
        claim.documentNames(),
        claim.status(),
        claim.findings(),
        claim.payout() == null ? null : MoneyParser.format(claim.payout()),
        claim.createdAt(),
        file.decision(),
        file.history().stream().map(HistoryEntryResponse::from).toList());
  }
}

record HistoryEntryResponse(
    Instant at,
    String actor,
    ActivityAction action,
    Decision fromDecision,
    Decision toDecision,
    String note) {

  static HistoryEntryResponse from(DecisionChange change) {
    return new HistoryEntryResponse(
        change.at(), change.actor(), change.action(), change.from(), change.to(), change.note());
  }
}
