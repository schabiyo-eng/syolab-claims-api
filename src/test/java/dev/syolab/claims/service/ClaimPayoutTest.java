package dev.syolab.claims.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.syolab.claims.domain.Policy;
import dev.syolab.claims.domain.PolicyStatus;
import dev.syolab.claims.domain.ProductLine;
import dev.syolab.claims.rules.ClaimRuleEngine;
import dev.syolab.claims.store.ActivityLog;
import dev.syolab.claims.store.ClaimRepository;
import dev.syolab.claims.store.PolicyRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClaimPayoutTest {

  private final ClaimService service =
      new ClaimService(
          new ClaimRepository(),
          new PolicyRepository(),
          new ActivityLog(),
          new ClaimRuleEngine(),
          Clock.systemUTC());

  @Test
  void autoPaysClaimedAmountLessDeductibleWhenCoverageRemains() {
    assertThat(payout(ProductLine.AUTO, "1250.00", "500.00", "10000.00")).isEqualTo("750.00");
  }

  @Test
  void autoCapsAtRemainingCoverageBeforeTheDeductible() {
    assertThat(payout(ProductLine.AUTO, "1250.00", "500.00", "900.00")).isEqualTo("400.00");
  }

  @Test
  void autoFloorsAtZeroWhenTheCappedAmountIsBelowTheDeductible() {
    assertThat(payout(ProductLine.AUTO, "1250.00", "500.00", "400.00")).isEqualTo("0.00");
    assertThat(payout(ProductLine.AUTO, "300.00", "500.00", "10000.00")).isEqualTo("0.00");
  }

  @Test
  void autoPaysNothingWhenCoverageIsUsedUp() {
    assertThat(payout(ProductLine.AUTO, "1250.00", "0.00", "-50.00")).isEqualTo("0.00");
  }

  @Test
  void autoPayoutIsAlwaysAtScaleTwo() {
    BigDecimal result =
        service.calculatePayout(
            policy(ProductLine.AUTO, "0"), new BigDecimal("1250"), new BigDecimal("10000"));

    assertThat(result.scale()).isEqualTo(2);
    assertThat(result.toPlainString()).isEqualTo("1250.00");
  }

  private String payout(
      ProductLine line, String claimed, String deductible, String remainingCoverage) {
    return service
        .calculatePayout(
            policy(line, deductible), new BigDecimal(claimed), new BigDecimal(remainingCoverage))
        .toPlainString();
  }

  static Policy policy(ProductLine line, String deductible) {
    return new Policy(
        "POL-FRESH",
        "test-holder",
        "Test Holder",
        PolicyStatus.ACTIVE,
        line,
        new BigDecimal("15000.00"),
        new BigDecimal(deductible),
        LocalDate.parse("2025-01-01"),
        LocalDate.parse("2027-12-31"),
        List.of("police-report"));
  }
}
