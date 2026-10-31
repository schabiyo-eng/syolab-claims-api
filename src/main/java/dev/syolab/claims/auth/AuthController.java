package dev.syolab.claims.auth;

import dev.syolab.claims.web.ErrorMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private static final Duration TOKEN_TTL = Duration.ofHours(8);
  private static final String ROLE_CLAIM = "role";
  private static final String NAME_CLAIM = "name";

  private final JwtEncoder tokens;

  public AuthController(JwtEncoder tokens) {
    this.tokens = tokens;
  }

  public record LoginRequest(String username, String password) {}

  public record LoginResponse(String token, String username, Role role, String displayName) {}

  public record CurrentUserResponse(String username, Role role, String displayName) {
    static CurrentUserResponse from(Jwt jwt) {
      return new CurrentUserResponse(
          jwt.getSubject(),
          Role.valueOf(jwt.getClaimAsString(ROLE_CLAIM)),
          jwt.getClaimAsString(NAME_CLAIM));
    }
  }

  @PostMapping("/login")
  @Operation(operationId = "login", summary = "Sign in with a demo user")
  @SecurityRequirements
  public LoginResponse login(@RequestBody LoginRequest request) {
    DemoUsers.DemoUser user =
        DemoUsers.authenticate(request.username(), request.password())
            .orElseThrow(() -> new BadCredentialsException("bad credentials"));
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(user.username())
            .claim(ROLE_CLAIM, user.role().name())
            .claim(NAME_CLAIM, user.displayName())
            .issuedAt(now)
            .expiresAt(now.plus(TOKEN_TTL))
            .build();
    String token =
        tokens
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    return new LoginResponse(token, user.username(), user.role(), user.displayName());
  }

  @GetMapping("/me")
  @Operation(operationId = "getCurrentUser", summary = "Who the bearer token belongs to")
  public CurrentUserResponse me(@AuthenticationPrincipal Jwt jwt) {
    return CurrentUserResponse.from(jwt);
  }

  @ExceptionHandler(BadCredentialsException.class)
  ResponseEntity<ErrorMessage> badCredentials() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new ErrorMessage("Username or password is incorrect."));
  }
}
