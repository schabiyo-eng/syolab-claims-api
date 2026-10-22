package dev.syolab.claims.web;

import dev.syolab.claims.domain.Policy;
import dev.syolab.claims.domain.PolicyStatus;
import dev.syolab.claims.service.MoneyParser;
import java.time.LocalDate;
import java.util.List;

public record PolicyResponse(
    String policyNumber,
    String holderName,
    PolicyStatus status,
    String coverageLimit,
    LocalDate effectiveDate,
    LocalDate expirationDate,
    List<String> requiredDocuments) {

  public static PolicyResponse from(Policy policy) {
    return new PolicyResponse(
        policy.policyNumber(),
        policy.holderName(),
        policy.status(),
        MoneyParser.format(policy.coverageLimit()),
        policy.effectiveDate(),
        policy.expirationDate(),
        policy.requiredDocuments());
  }
}
