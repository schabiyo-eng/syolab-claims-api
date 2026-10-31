package dev.syolab.claims.auth;

import dev.syolab.claims.domain.Policy;
import java.util.Objects;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The signed-in user a request acts for. Services decide claimant scope with canSee and nowhere
 * else.
 */
public record Caller(String username, Role role) {

  public Caller {
    Objects.requireNonNull(username);
    Objects.requireNonNull(role);
  }

  /** Reads sub and the role claim from a token the resource server already verified. */
  public static Caller from(Jwt jwt) {
    return new Caller(
        jwt.getSubject(), Role.valueOf(jwt.getClaimAsString(AuthController.ROLE_CLAIM)));
  }

  /** policy is null when the number matches no policy. Only adjusters see that case. */
  public boolean canSee(Policy policy) {
    return role == Role.ADJUSTER || (policy != null && policy.holderId().equals(username));
  }
}
