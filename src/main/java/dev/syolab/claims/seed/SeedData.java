package dev.syolab.claims.seed;

import dev.syolab.claims.domain.Policy;
import dev.syolab.claims.domain.PolicyStatus;
import dev.syolab.claims.service.ClaimService;
import dev.syolab.claims.store.ClaimRepository;
import dev.syolab.claims.store.PolicyRepository;
import dev.syolab.claims.web.CreateClaimRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SeedData implements ApplicationRunner {

  private final PolicyRepository policies;
  private final ClaimRepository claims;
  private final ClaimService claimService;

  public SeedData(PolicyRepository policies, ClaimRepository claims, ClaimService claimService) {
    this.policies = policies;
    this.claims = claims;
    this.claimService = claimService;
  }

  @Override
  public void run(ApplicationArguments args) {
    load();
  }

  public void reset() {
    synchronized (claims) {
      claims.clear();
      policies.clear();
      load();
    }
  }

  public void load() {
    policies.save(
        policy(
            "POL-1001",
            "Amira Shah",
            PolicyStatus.ACTIVE,
            "10000.00",
            "2025-01-01",
            "2027-12-31",
            List.of("police-report", "repair-estimate")));
    policies.save(
        policy(
            "POL-1002",
            "Luis Ortega",
            PolicyStatus.ACTIVE,
            "50000.00",
            "2025-01-01",
            "2027-12-31",
            List.of("medical-bill")));
    policies.save(
        policy(
            "POL-1003",
            "Nora Blake",
            PolicyStatus.EXPIRED,
            "20000.00",
            "2023-01-01",
            "2024-12-31",
            List.of("police-report")));
    policies.save(
        policy(
            "POL-1004",
            "Chris Dalton",
            PolicyStatus.ACTIVE,
            "10000.00",
            "2025-01-01",
            "2027-12-31",
            List.of("police-report")));

    store(
        "c5555555-5555-4555-8555-555555555555",
        "POL-1004",
        "Chris Dalton",
        "10000.00",
        "2026-01-20",
        "Parking lot dent",
        List.of("police-report"),
        "2026-01-20T15:00:00Z");
    store(
        "c1111111-1111-4111-8111-111111111111",
        "POL-1001",
        "Amira Shah",
        "6000.00",
        "2026-02-01",
        "Rear bumper scrape",
        List.of("police-report", "repair-estimate"),
        "2026-02-01T15:00:00Z");
    store(
        "c2222222-2222-4222-8222-222222222222",
        "POL-1001",
        "Amira Shah",
        "4000.00",
        "2026-03-12",
        "Side mirror cracked",
        List.of("police-report", "repair-estimate"),
        "2026-03-12T15:00:00Z");
    store(
        "c3333333-3333-4333-8333-333333333333",
        "POL-1002",
        "Luis Ortega",
        "1200.00",
        "2026-04-02",
        "Clinic visit",
        List.of(),
        "2026-04-02T15:00:00Z");
    store(
        "c4444444-4444-4444-8444-444444444444",
        "POL-1003",
        "Nora Blake",
        "900.00",
        "2026-05-01",
        "Windshield chip",
        List.of("police-report"),
        "2026-05-01T15:00:00Z");
  }

  private static Policy policy(
      String number,
      String holder,
      PolicyStatus status,
      String limit,
      String effective,
      String expiration,
      List<String> documents) {
    return new Policy(
        number,
        holder,
        status,
        new BigDecimal(limit),
        LocalDate.parse(effective),
        LocalDate.parse(expiration),
        documents);
  }

  private void store(
      String id,
      String policyNumber,
      String claimant,
      String amount,
      String incidentDate,
      String description,
      List<String> documents,
      String createdAt) {
    claimService.submit(
        new CreateClaimRequest(
            policyNumber, claimant, amount, LocalDate.parse(incidentDate), description, documents),
        UUID.fromString(id),
        Instant.parse(createdAt));
  }
}
