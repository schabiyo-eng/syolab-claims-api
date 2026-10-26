package dev.syolab.claims.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Policy(
    String policyNumber,
    String holderName,
    PolicyStatus status,
    ProductLine productLine,
    BigDecimal coverageLimit,
    BigDecimal deductible,
    LocalDate effectiveDate,
    LocalDate expirationDate,
    List<String> requiredDocuments) {

  public Policy {
    requiredDocuments = List.copyOf(requiredDocuments);
  }
}
