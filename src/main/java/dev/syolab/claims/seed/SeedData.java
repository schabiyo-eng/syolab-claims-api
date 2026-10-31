package dev.syolab.claims.seed;

import dev.syolab.claims.domain.Policy;
import dev.syolab.claims.domain.PolicyStatus;
import dev.syolab.claims.domain.ProductLine;
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
            "amira",
            "Amira Shah",
            PolicyStatus.ACTIVE,
            ProductLine.AUTO,
            "10000.00",
            "250.00",
            "2025-01-01",
            "2027-12-31",
            List.of("police-report", "repair-estimate")));
    policies.save(
        policy(
            "POL-1002",
            "luis",
            "Luis Ortega",
            PolicyStatus.ACTIVE,
            ProductLine.TRAVEL,
            "50000.00",
            "0.00",
            "2025-01-01",
            "2027-12-31",
            List.of("medical-bill")));
    policies.save(
        policy(
            "POL-1003",
            "nora",
            "Nora Blake",
            PolicyStatus.EXPIRED,
            ProductLine.AUTO,
            "20000.00",
            "250.00",
            "2023-01-01",
            "2024-12-31",
            List.of("police-report")));
    policies.save(
        policy(
            "POL-1004",
            "chris",
            "Chris Dalton",
            PolicyStatus.ACTIVE,
            ProductLine.AUTO,
            "10000.00",
            "500.00",
            "2025-01-01",
            "2027-12-31",
            List.of("police-report")));
    policies.save(
        policy(
            "POL-1005",
            "grace",
            "Grace Okafor",
            PolicyStatus.ACTIVE,
            ProductLine.HOME,
            "15000.00",
            "500.00",
            "2025-01-01",
            "2027-12-31",
            List.of("contractor-estimate")));

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
        "c6666666-6666-4666-8666-666666666666",
        "POL-1005",
        "Grace Okafor",
        "1250.00",
        "2026-04-18",
        "Kitchen pipe leak",
        List.of("contractor-estimate"),
        "2026-04-18T15:00:00Z");
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
      String holderId,
      String holder,
      PolicyStatus status,
      ProductLine productLine,
      String limit,
      String deductible,
      String effective,
      String expiration,
      List<String> documents) {
    return new Policy(
        number,
        holderId,
        holder,
        status,
        productLine,
        new BigDecimal(limit),
        new BigDecimal(deductible),
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
