package dev.syolab.claims.web;

import dev.syolab.claims.auth.Caller;
import dev.syolab.claims.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

  private final PolicyService policies;

  public PolicyController(PolicyService policies) {
    this.policies = policies;
  }

  @GetMapping
  @Operation(operationId = "listPolicies", summary = "List policies")
  public List<PolicyResponse> list(@AuthenticationPrincipal Jwt jwt) {
    return policies.list(Caller.from(jwt)).stream().map(PolicyResponse::from).toList();
  }

  @GetMapping("/{policyNumber}")
  @Operation(operationId = "getPolicy", summary = "Get one policy")
  public PolicyResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable String policyNumber) {
    return PolicyResponse.from(policies.get(Caller.from(jwt), policyNumber));
  }
}
