package dev.syolab.claims.web;

import dev.syolab.claims.domain.Claim;
import dev.syolab.claims.domain.ClaimStatus;
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
    Instant createdAt) {

  public static ClaimResponse from(Claim claim) {
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
        claim.createdAt());
  }
}
