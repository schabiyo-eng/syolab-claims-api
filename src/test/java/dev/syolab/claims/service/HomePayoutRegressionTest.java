package dev.syolab.claims.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.syolab.claims.domain.ProductLine;
import dev.syolab.claims.rules.ClaimRuleEngine;
import dev.syolab.claims.store.ActivityLog;
import dev.syolab.claims.store.ClaimRepository;
import dev.syolab.claims.store.PolicyRepository;
import java.math.BigDecimal;
import java.time.Clock;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("payout-regression")
class HomePayoutRegressionTest {

  private final ClaimService service =
      new ClaimService(
          new ClaimRepository(),
          new PolicyRepository(),
          new ActivityLog(),
          new ClaimRuleEngine(),
          Clock.systemUTC());

  @Test
  void homePaysClaimedAmountLessDeductibleWhenCoverageRemains() {
    BigDecimal result =
        service.calculatePayout(
            ClaimPayoutTest.policy(ProductLine.HOME, "500.00"),
            new BigDecimal("1250.00"),
            new BigDecimal("15000.00"));

    assertThat(result.toPlainString()).isEqualTo("750.00");
  }
}
