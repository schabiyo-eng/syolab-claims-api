package dev.syolab.claims.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.syolab.claims.domain.Claim;
import dev.syolab.claims.domain.ClaimStatus;
import dev.syolab.claims.domain.FindingCode;
import dev.syolab.claims.domain.Policy;
import dev.syolab.claims.domain.PolicyStatus;
import dev.syolab.claims.domain.ProductLine;
import dev.syolab.claims.domain.Severity;
import dev.syolab.claims.domain.ValidationFinding;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClaimRuleEngineTest {

  private final ClaimRuleEngine engine = new ClaimRuleEngine();
  private final LocalDate today = LocalDate.parse("2026-10-22");

  @Test
  void acceptsClaimExactlyAtCoverageLimit() {
    Assessment result =
        engine.evaluate(
            freshPolicy("10000.00"),
            List.of(),
            draft("10000.00", "2026-06-01", "Parking lot dent", "police-report"),
            today);

    assertThat(result.status()).isEqualTo(ClaimStatus.ACCEPTED);
    assertThat(result.findings()).noneMatch(finding -> finding.severity() == Severity.ERROR);
    assertThat(result.findings())
        .noneMatch(finding -> finding.code() == FindingCode.EXCEEDS_COVERAGE);
  }

  @Test
  void oneCentOverTheCoverageLimitIsRejected() {
    Assessment result =
        engine.evaluate(
            freshPolicy("10000.00"),
            List.of(),
            draft("10000.01", "2026-06-01", "Parking lot dent", "police-report"),
            today);

    assertThat(result.status()).isEqualTo(ClaimStatus.REJECTED);
    assertThat(codes(result)).contains(FindingCode.EXCEEDS_COVERAGE);
  }

  @Test
  void inclusiveTermDatesStayActive() {
    Policy policy = freshPolicy("10000.00");
    Assessment onEffective =
        engine.evaluate(
            policy, List.of(), draft("10.00", "2025-01-01", "Scratch", "police-report"), today);
    Assessment onExpiration =
        engine.evaluate(
            policy,
            List.of(),
            draft("10.00", "2027-12-31", "Scratch", "police-report"),
            LocalDate.parse("2027-12-31"));

    assertThat(onEffective.status()).isEqualTo(ClaimStatus.ACCEPTED);
    assertThat(onExpiration.status()).isEqualTo(ClaimStatus.ACCEPTED);
  }

  @Test
  void rejectsInactivePolicy() {
    Policy expired =
        new Policy(
            "POL-OLD",
            "test-holder",
            "Test Holder",
            PolicyStatus.EXPIRED,
            ProductLine.AUTO,
            new BigDecimal("20000.00"),
            BigDecimal.ZERO,
            LocalDate.parse("2025-01-01"),
            LocalDate.parse("2027-12-31"),
            List.of("police-report"));
    Assessment result =
        engine.evaluate(
            expired, List.of(), draft("10.00", "2026-06-01", "Scratch", "police-report"), today);

    assertThat(result.status()).isEqualTo(ClaimStatus.REJECTED);
    assertThat(codes(result)).containsExactly(FindingCode.POLICY_INACTIVE);
  }

  @Test
  void incidentOutsideTheTermIsInactive() {
    Assessment before =
        engine.evaluate(
            freshPolicy("10000.00"),
            List.of(),
            draft("10.00", "2024-12-31", "Scratch", "police-report"),
            today);
    Assessment after =
        engine.evaluate(
            freshPolicy("10000.00"),
            List.of(),
            draft("10.00", "2028-01-01", "Scratch", "police-report"),
            LocalDate.parse("2028-01-01"));

    assertThat(codes(before)).containsExactly(FindingCode.POLICY_INACTIVE);
    assertThat(codes(after)).containsExactly(FindingCode.POLICY_INACTIVE);
  }

  @Test
  void inactivePolicySkipsCoverageAndStillChecksDocumentsAndFraud() {
    Policy expired =
        new Policy(
            "POL-OLD",
            "test-holder",
            "Test Holder",
            PolicyStatus.EXPIRED,
            ProductLine.AUTO,
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            LocalDate.parse("2023-01-01"),
            LocalDate.parse("2024-12-31"),
            List.of("police-report"));
    Assessment result =
        engine.evaluate(
            expired,
            List.of(),
            draft("30000.00", "2026-06-01", "Total loss of the windshield", "photo"),
            today);

    assertThat(result.status()).isEqualTo(ClaimStatus.REJECTED);
    assertThat(codes(result))
        .containsExactly(
            FindingCode.POLICY_INACTIVE, FindingCode.MISSING_DOCUMENTS, FindingCode.POSSIBLE_FRAUD);
    assertThat(codes(result)).doesNotContain(FindingCode.EXCEEDS_COVERAGE);
  }

  @Test
  void storesUnknownPolicyAsNotFoundOnly() {
    Assessment result =
        engine.evaluate(
            null, List.of(), draft("30000.00", "2026-12-01", "Total loss", "police-report"), today);

    assertThat(result.status()).isEqualTo(ClaimStatus.REJECTED);
    assertThat(codes(result)).containsExactly(FindingCode.POLICY_NOT_FOUND);
    assertThat(result.findings().get(0).severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void flagsMissingDocumentsWhenCoverageIsFine() {
    Assessment result =
        engine.evaluate(
            freshPolicy("10000.00", "police-report", "repair-estimate"),
            List.of(),
            draft("25.00", "2026-06-01", "Scratch", "Police-Report"),
            today);

    assertThat(result.status()).isEqualTo(ClaimStatus.FLAGGED);
    assertThat(codes(result)).containsExactly(FindingCode.MISSING_DOCUMENTS);
    assertThat(result.findings().get(0).severity()).isEqualTo(Severity.WARNING);
  }

  @Test
  void matchingDocumentNamesAreCaseInsensitive() {
    Assessment result =
        engine.evaluate(
            freshPolicy("10000.00", "police-report"),
            List.of(),
            draft("25.00", "2026-06-01", "Scratch", "Police-Report"),
            today);

    assertThat(result.status()).isEqualTo(ClaimStatus.ACCEPTED);
    assertThat(result.findings()).isEmpty();
  }

  @Test
  void flagsTotalLossWording() {
    Assessment result =
        engine.evaluate(
            freshPolicy("100000.00"),
            List.of(),
            draft("25.00", "2026-06-01", "The car is a TOTAL LOSS", "police-report"),
            today);

    assertThat(result.status()).isEqualTo(ClaimStatus.FLAGGED);
    assertThat(codes(result)).containsExactly(FindingCode.POSSIBLE_FRAUD);
  }

  @Test
  void flagsHighAmount() {
    Assessment atThreshold =
        engine.evaluate(
            freshPolicy("100000.00"),
            List.of(),
            draft("25000.00", "2026-06-01", "Repair", "police-report"),
            today);
    Assessment below =
        engine.evaluate(
            freshPolicy("100000.00"),
            List.of(),
            draft("24999.99", "2026-06-01", "Repair", "police-report"),
            today);

    assertThat(atThreshold.status()).isEqualTo(ClaimStatus.FLAGGED);
    assertThat(codes(atThreshold)).containsExactly(FindingCode.POSSIBLE_FRAUD);
    assertThat(below.status()).isEqualTo(ClaimStatus.ACCEPTED);
    assertThat(below.findings()).isEmpty();
  }

  @Test
  void futureIncidentDateAddsFraudWarning() {
    Assessment result =
        engine.evaluate(
            freshPolicy("100000.00"),
            List.of(),
            draft("25.00", "2026-10-23", "Repair", "police-report"),
            today);

    assertThat(result.status()).isEqualTo(ClaimStatus.FLAGGED);
    assertThat(codes(result)).containsExactly(FindingCode.POSSIBLE_FRAUD);
  }

  @Test
  void rejectsClaimantWhoIsNotThePolicyholder() {
    ClaimDraft other =
        new ClaimDraft(
            "POL-FRESH",
            "Someone Else",
            new BigDecimal("25.00"),
            LocalDate.parse("2026-06-01"),
            "Scratch",
            List.of("police-report"));

    Assessment result = engine.evaluate(freshPolicy("10000.00"), List.of(), other, today);

    assertThat(result.status()).isEqualTo(ClaimStatus.REJECTED);
    assertThat(codes(result)).containsExactly(FindingCode.CLAIMANT_NOT_POLICYHOLDER);
    assertThat(result.findings().get(0).severity()).isEqualTo(Severity.ERROR);
  }

  @Test
  void claimantMatchingThePolicyholderStaysAccepted() {
    ClaimDraft differentCase =
        new ClaimDraft(
            "POL-FRESH",
            "test holder",
            new BigDecimal("25.00"),
            LocalDate.parse("2026-06-01"),
            "Scratch",
            List.of("police-report"));

    Assessment exact =
        engine.evaluate(
            freshPolicy("10000.00"),
            List.of(),
            draft("25.00", "2026-06-01", "Scratch", "police-report"),
            today);
    Assessment caseOnly = engine.evaluate(freshPolicy("10000.00"), List.of(), differentCase, today);

    assertThat(exact.status()).isEqualTo(ClaimStatus.ACCEPTED);
    assertThat(exact.findings()).isEmpty();
    assertThat(caseOnly.status()).isEqualTo(ClaimStatus.ACCEPTED);
    assertThat(caseOnly.findings()).isEmpty();
  }

  @Test
  void openClaimsAreIncludedInTheCoverageSum() {
    Claim existing =
        new Claim(
            UUID.randomUUID(),
            "POL-FRESH",
            "Holder",
            new BigDecimal("6000.00"),
            LocalDate.parse("2026-02-01"),
            "Earlier",
            List.of("police-report"),
            ClaimStatus.ACCEPTED,
            List.of(),
            null,
            Instant.parse("2026-02-01T15:00:00Z"));
    Assessment result =
        engine.evaluate(
            freshPolicy("10000.00"),
            List.of(existing),
            draft("4000.01", "2026-06-01", "Repair", "police-report"),
            today);

    assertThat(result.status()).isEqualTo(ClaimStatus.REJECTED);
    assertThat(codes(result)).contains(FindingCode.EXCEEDS_COVERAGE);
  }

  private static Policy freshPolicy(String limit, String... documents) {
    List<String> required = documents.length == 0 ? List.of("police-report") : List.of(documents);
    return new Policy(
        "POL-FRESH",
        "test-holder",
        "Test Holder",
        PolicyStatus.ACTIVE,
        ProductLine.AUTO,
        new BigDecimal(limit),
        BigDecimal.ZERO,
        LocalDate.parse("2025-01-01"),
        LocalDate.parse("2027-12-31"),
        required);
  }

  private static ClaimDraft draft(
      String amount, String incidentDate, String description, String... documents) {
    return new ClaimDraft(
        "POL-FRESH",
        "Test Holder",
        new BigDecimal(amount),
        LocalDate.parse(incidentDate),
        description,
        List.of(documents));
  }

  private static List<FindingCode> codes(Assessment assessment) {
    return assessment.findings().stream().map(ValidationFinding::code).toList();
  }
}
