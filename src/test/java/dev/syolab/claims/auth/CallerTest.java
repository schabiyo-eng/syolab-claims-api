package dev.syolab.claims.auth;

import static org.assertj.core.api.Assertions.assertThat;

import dev.syolab.claims.domain.Policy;
import dev.syolab.claims.domain.PolicyStatus;
import dev.syolab.claims.domain.ProductLine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CallerTest {

  private static final Policy AMIRA_POLICY =
      new Policy(
          "POL-1001",
          "amira",
          "Amira Shah",
          PolicyStatus.ACTIVE,
          ProductLine.AUTO,
          new BigDecimal("10000.00"),
          BigDecimal.ZERO,
          LocalDate.parse("2025-01-01"),
          LocalDate.parse("2027-12-31"),
          List.of("police-report"));
  private static final Policy LUIS_POLICY =
      new Policy(
          "POL-1002",
          "luis",
          "Luis Ortega",
          PolicyStatus.ACTIVE,
          ProductLine.TRAVEL,
          new BigDecimal("50000.00"),
          BigDecimal.ZERO,
          LocalDate.parse("2025-01-01"),
          LocalDate.parse("2027-12-31"),
          List.of("medical-bill"));

  @Test
  void adjusterSeesAnyPolicyAndUnknownNumbers() {
    Caller dana = new Caller("dana", Role.ADJUSTER);

    assertThat(dana.canSee(LUIS_POLICY)).isTrue();
    assertThat(dana.canSee(null)).isTrue();
  }

  @Test
  void claimantSeesOnlyPoliciesTheyHold() {
    Caller amira = new Caller("amira", Role.CLAIMANT);

    assertThat(amira.canSee(AMIRA_POLICY)).isTrue();
    assertThat(amira.canSee(LUIS_POLICY)).isFalse();
  }

  @Test
  void claimantDoesNotSeeUnknownPolicy() {
    Caller amira = new Caller("amira", Role.CLAIMANT);

    assertThat(amira.canSee(null)).isFalse();
  }
}
