package dev.syolab.claims.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ClaimDraft(
    String policyNumber,
    String claimantName,
    BigDecimal amount,
    LocalDate incidentDate,
    String description,
    List<String> documentNames) {

  public ClaimDraft {
    documentNames = List.copyOf(documentNames);
  }
}
