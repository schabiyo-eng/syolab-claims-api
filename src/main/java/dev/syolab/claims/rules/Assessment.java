package dev.syolab.claims.rules;

import dev.syolab.claims.domain.ClaimStatus;
import dev.syolab.claims.domain.Severity;
import dev.syolab.claims.domain.ValidationFinding;
import java.util.List;

public record Assessment(ClaimStatus status, List<ValidationFinding> findings) {

  public Assessment {
    findings = List.copyOf(findings);
  }

  public static Assessment from(List<ValidationFinding> findings) {
    boolean error = findings.stream().anyMatch(finding -> finding.severity() == Severity.ERROR);
    if (error) {
      return new Assessment(ClaimStatus.REJECTED, findings);
    }
    boolean warning = findings.stream().anyMatch(finding -> finding.severity() == Severity.WARNING);
    if (warning) {
      return new Assessment(ClaimStatus.FLAGGED, findings);
    }
    return new Assessment(ClaimStatus.ACCEPTED, findings);
  }
}
