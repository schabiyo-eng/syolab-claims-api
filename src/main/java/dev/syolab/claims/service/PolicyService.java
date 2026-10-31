package dev.syolab.claims.service;

import dev.syolab.claims.auth.Caller;
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

  public List<Policy> list(Caller caller) {
    return policies.findAllOrdered().stream().filter(caller::canSee).toList();
  }

  public Policy get(Caller caller, String policyNumber) {
    return policies
        .findByNumber(policyNumber)
        .filter(caller::canSee)
        .orElseThrow(() -> new NotFoundException("Policy not found."));
  }
}
