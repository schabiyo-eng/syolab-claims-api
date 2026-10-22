package dev.syolab.claims.store;

import dev.syolab.claims.domain.Policy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class PolicyRepository {

  private final ConcurrentHashMap<String, Policy> policies = new ConcurrentHashMap<>();

  public synchronized void clear() {
    policies.clear();
  }

  public synchronized void save(Policy policy) {
    policies.put(policy.policyNumber(), policy);
  }

  public synchronized Optional<Policy> findByNumber(String policyNumber) {
    return Optional.ofNullable(policies.get(policyNumber));
  }

  public synchronized List<Policy> findAllOrdered() {
    List<Policy> ordered = new ArrayList<>(policies.values());
    ordered.sort(Comparator.comparing(Policy::policyNumber));
    return List.copyOf(ordered);
  }
}
