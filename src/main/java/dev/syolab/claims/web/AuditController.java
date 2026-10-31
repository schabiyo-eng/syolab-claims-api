package dev.syolab.claims.web;

import dev.syolab.claims.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

  private final ClaimService claims;

  public AuditController(ClaimService claims) {
    this.claims = claims;
  }

  @GetMapping
  @Operation(operationId = "listAudit", summary = "List the append-only audit log")
  public List<AuditEntryResponse> list() {
    return AuditEntryResponse.list(claims.audit());
  }
}
