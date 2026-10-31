package dev.syolab.claims.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record Policy(
    String policyNumber,
    String holderId,
    String holderName,
    PolicyStatus status,
    ProductLine productLine,
    BigDecimal coverageLimit,
    BigDecimal deductible,
    LocalDate effectiveDate,
    LocalDate expirationDate,
    List<String> requiredDocuments) {

  public Policy {
    Objects.requireNonNull(holderId);
    requiredDocuments = List.copyOf(requiredDocuments);
  }
}
