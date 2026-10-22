package dev.syolab.claims.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record Claim(
    UUID id,
    String policyNumber,
    String claimantName,
    BigDecimal amount,
    LocalDate incidentDate,
    String description,
    List<String> documentNames,
    ClaimStatus status,
    List<ValidationFinding> findings,
    Instant createdAt) {

  public Claim {
    documentNames = List.copyOf(documentNames);
    findings = List.copyOf(findings);
  }
}
