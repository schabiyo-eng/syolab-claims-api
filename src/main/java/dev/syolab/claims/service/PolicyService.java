package dev.syolab.claims.service;

import dev.syolab.claims.domain.Policy;
import dev.syolab.claims.store.PolicyRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PolicyService {

  private final PolicyRepository policies;

  public PolicyService(PolicyRepository policies) {
    this.policies = policies;
  }

  public List<Policy> list() {
    return policies.findAllOrdered();
  }

  public Policy get(String policyNumber) {
    return policies
        .findByNumber(policyNumber)
        .orElseThrow(() -> new NotFoundException("Policy not found."));
  }
}
