package dev.syolab.claims.service;

import dev.syolab.claims.domain.Claim;
import dev.syolab.claims.domain.ClaimStatus;
import dev.syolab.claims.domain.Policy;
import dev.syolab.claims.domain.ProductLine;
import dev.syolab.claims.rules.Assessment;
import dev.syolab.claims.rules.ClaimDraft;
import dev.syolab.claims.rules.ClaimRuleEngine;
import dev.syolab.claims.store.ClaimRepository;
import dev.syolab.claims.store.PolicyRepository;
import dev.syolab.claims.web.CreateClaimRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ClaimService {

  private final ClaimRepository claims;
  private final PolicyRepository policies;
  private final ClaimRuleEngine engine;
  private final Clock clock;

  public ClaimService(
      ClaimRepository claims, PolicyRepository policies, ClaimRuleEngine engine, Clock clock) {
    this.claims = claims;
    this.policies = policies;
    this.engine = engine;
    this.clock = clock;
  }

  public Claim submit(CreateClaimRequest request) {
    return submit(request, UUID.randomUUID(), null);
  }

  public Claim submit(CreateClaimRequest request, UUID id, Instant createdAt) {
    synchronized (claims) {
      validateShape(request);
      BigDecimal amount = MoneyParser.parse(request.amount());
      Policy policy = policies.findByNumber(request.policyNumber()).orElse(null);
      List<Claim> open =
          policy == null ? List.of() : claims.findConsumingCoverage(policy.policyNumber());
      ClaimDraft draft =
          new ClaimDraft(
              request.policyNumber(),
              request.claimantName().trim(),
              amount,
              request.incidentDate(),
              request.description().trim(),
              request.documentNames());
      Assessment assessment = engine.evaluate(policy, open, draft, LocalDate.now(clock));
      BigDecimal payout = null;
      if (assessment.status() == ClaimStatus.ACCEPTED) {
        BigDecimal consumed =
            open.stream().map(Claim::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        payout = calculatePayout(policy, draft.amount(), policy.coverageLimit().subtract(consumed));
      }
      Claim claim =
          new Claim(
              id,
              draft.policyNumber(),
              draft.claimantName(),
              draft.amount(),
              draft.incidentDate(),
              draft.description(),
              draft.documentNames(),
              assessment.status(),
              assessment.findings(),
              payout,
              createdAt == null ? Instant.now(clock) : createdAt);
      claims.save(claim);
      return claim;
    }
  }

  public BigDecimal calculatePayout(
      Policy policy, BigDecimal claimedAmount, BigDecimal remainingCoverage) {
    BigDecimal available = remainingCoverage.max(BigDecimal.ZERO);
    if (policy.productLine() == ProductLine.AUTO) {
      return claimedAmount
          .min(available)
          .subtract(policy.deductible())
          .max(BigDecimal.ZERO)
          .setScale(2, RoundingMode.HALF_EVEN);
    }
    BigDecimal net = claimedAmount.subtract(policy.deductible()).max(BigDecimal.ZERO);
    BigDecimal coveredShare = net.divide(claimedAmount, 0, RoundingMode.DOWN);
    return claimedAmount.min(available).multiply(coveredShare).setScale(2, RoundingMode.DOWN);
  }

  public Claim get(UUID id) {
    return claims.findById(id).orElseThrow(() -> new NotFoundException("Claim not found."));
  }

  public List<Claim> listNewestFirst() {
    return claims.findAllNewestFirst();
  }

  private static void validateShape(CreateClaimRequest request) {
    if (request.policyNumber() == null) {
      throw new BadRequestException("Policy number is required.");
    }
    if (request.claimantName() == null || request.claimantName().isBlank()) {
      throw new BadRequestException("Claimant name is required.");
    }
    if (request.description() == null || request.description().isBlank()) {
      throw new BadRequestException("Description is required.");
    }
    if (request.incidentDate() == null) {
      throw new BadRequestException("Incident date is required.");
    }
    if (request.documentNames() == null) {
      throw new BadRequestException("Document names are required.");
    }
    if (request.documentNames().stream().anyMatch(Objects::isNull)) {
      throw new BadRequestException("Document names are malformed.");
    }
  }
}
