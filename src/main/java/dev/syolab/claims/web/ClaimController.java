package dev.syolab.claims.web;

import dev.syolab.claims.auth.Caller;
import dev.syolab.claims.domain.Claim;
import dev.syolab.claims.service.ClaimService;
import dev.syolab.claims.service.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

  private final ClaimService claims;

  public ClaimController(ClaimService claims) {
    this.claims = claims;
  }

  @GetMapping
  @Operation(operationId = "listClaims", summary = "List claims newest first")
  public List<ClaimResponse> list(@AuthenticationPrincipal Jwt jwt) {
    return claims.listNewestFirst(Caller.from(jwt)).stream().map(ClaimResponse::from).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(operationId = "createClaim", summary = "Submit a claim and run validation")
  public ClaimResponse create(
      @AuthenticationPrincipal Jwt jwt, @RequestBody CreateClaimRequest request) {
    return ClaimResponse.from(claims.submit(Caller.from(jwt), request));
  }

  @GetMapping("/{id}")
  @Operation(operationId = "getClaim", summary = "Get one claim")
  public ResponseEntity<ClaimResponse> get(
      @AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
    UUID claimId = parseId(id);
    Claim claim = claims.get(Caller.from(jwt), claimId);
    return ResponseEntity.ok(ClaimResponse.from(claim));
  }

  private static UUID parseId(String id) {
    try {
      return UUID.fromString(id);
    } catch (IllegalArgumentException exception) {
      throw new NotFoundException("Claim not found.");
    }
  }
}
