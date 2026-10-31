package dev.syolab.claims.rules;

import dev.syolab.claims.domain.Claim;
import dev.syolab.claims.domain.FindingCode;
import dev.syolab.claims.domain.Policy;
import dev.syolab.claims.domain.PolicyStatus;
import dev.syolab.claims.domain.Severity;
import dev.syolab.claims.domain.ValidationFinding;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ClaimRuleEngine {

  static final BigDecimal FRAUD_AMOUNT = new BigDecimal("25000.00");

  public Assessment evaluate(
      Policy policy, List<Claim> openClaims, ClaimDraft draft, LocalDate today) {
    if (policy == null) {
      return Assessment.from(
          List.of(
              finding(
                  FindingCode.POLICY_NOT_FOUND,
                  Severity.ERROR,
                  "No policy matches this policy number.")));
    }

    List<ValidationFinding> findings = new ArrayList<>();
    boolean inTerm =
        !draft.incidentDate().isBefore(policy.effectiveDate())
            && !draft.incidentDate().isAfter(policy.expirationDate());
    if (policy.status() != PolicyStatus.ACTIVE || !inTerm) {
      findings.add(
          finding(
              FindingCode.POLICY_INACTIVE,
              Severity.ERROR,
              "The policy is not active for this incident date."));
    } else if (exceedsCoverage(policy, openClaims, draft)) {
      findings.add(
          finding(
              FindingCode.EXCEEDS_COVERAGE,
              Severity.ERROR,
              "Open claims exceed the policy coverage limit."));
    }

    if (!isPolicyholder(policy, draft)) {
      findings.add(
          finding(
              FindingCode.CLAIMANT_NOT_POLICYHOLDER,
              Severity.ERROR,
              "The claimant is not the policyholder on this policy."));
    }
    if (missingDocuments(policy, draft)) {
      findings.add(
          finding(
              FindingCode.MISSING_DOCUMENTS, Severity.WARNING, "A required document is missing."));
    }
    if (possibleFraud(draft, today)) {
      findings.add(
          finding(
              FindingCode.POSSIBLE_FRAUD, Severity.WARNING, "This claim needs a fraud review."));
    }
    return Assessment.from(findings);
  }

  private static boolean exceedsCoverage(Policy policy, List<Claim> openClaims, ClaimDraft draft) {
    BigDecimal open =
        openClaims.stream().map(Claim::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    return open.add(draft.amount()).compareTo(policy.coverageLimit()) > 0;
  }

  private static boolean isPolicyholder(Policy policy, ClaimDraft draft) {
    return draft.claimantName().trim().equalsIgnoreCase(policy.holderName().trim());
  }

  private static boolean missingDocuments(Policy policy, ClaimDraft draft) {
    Set<String> provided =
        draft.documentNames().stream()
            .map(name -> name.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
    return policy.requiredDocuments().stream()
        .anyMatch(required -> !provided.contains(required.toLowerCase(Locale.ROOT)));
  }

  private static boolean possibleFraud(ClaimDraft draft, LocalDate today) {
    if (draft.amount().compareTo(FRAUD_AMOUNT) >= 0) {
      return true;
    }
    if (draft.description().toLowerCase(Locale.ROOT).contains("total loss")) {
      return true;
    }
    return draft.incidentDate().isAfter(today);
  }

  private static ValidationFinding finding(FindingCode code, Severity severity, String message) {
    return new ValidationFinding(code, severity, message);
  }
}
