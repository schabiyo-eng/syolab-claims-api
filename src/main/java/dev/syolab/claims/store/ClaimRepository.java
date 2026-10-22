package dev.syolab.claims.store;

import dev.syolab.claims.domain.Claim;
import dev.syolab.claims.domain.ClaimStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ClaimRepository {

  private final List<Claim> claims = new ArrayList<>();

  public synchronized void clear() {
    claims.clear();
  }

  public synchronized void save(Claim claim) {
    claims.add(claim);
  }

  public synchronized Optional<Claim> findById(UUID id) {
    return claims.stream().filter(claim -> claim.id().equals(id)).findFirst();
  }

  public synchronized List<Claim> findAllNewestFirst() {
    return claims.stream()
        .sorted(
            Comparator.comparing(Claim::createdAt)
                .reversed()
                .thenComparing(claim -> claim.id().toString()))
        .toList();
  }

  public synchronized List<Claim> findConsumingCoverage(String policyNumber) {
    return claims.stream()
        .filter(claim -> claim.policyNumber().equals(policyNumber))
        .filter(claim -> claim.status() != ClaimStatus.REJECTED)
        .toList();
  }
}
