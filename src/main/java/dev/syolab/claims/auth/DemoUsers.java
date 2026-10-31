package dev.syolab.claims.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

/** The demo sign-ins. README lists the same rows, and AuthApiTest fails when they drift. */
final class DemoUsers {

  record DemoUser(String username, String password, Role role, String displayName) {}

  static final List<DemoUser> ALL =
      List.of(
          new DemoUser("amira", "amira-demo", Role.CLAIMANT, "Amira Shah"),
          new DemoUser("luis", "luis-demo", Role.CLAIMANT, "Luis Ortega"),
          new DemoUser("dana", "dana-demo", Role.ADJUSTER, "Dana Whitfield"));

  private DemoUsers() {}

  /** Empty for unknown users, wrong passwords, or nulls. Passwords compare in constant time. */
  static Optional<DemoUser> authenticate(String username, String password) {
    if (username == null || password == null) {
      return Optional.empty();
    }
    Optional<DemoUser> match =
        ALL.stream().filter(user -> user.username().equals(username)).findFirst();
    if (match.isEmpty()) {
      return Optional.empty();
    }
    DemoUser user = match.get();
    byte[] expected = user.password().getBytes(StandardCharsets.UTF_8);
    byte[] actual = password.getBytes(StandardCharsets.UTF_8);
    if (!MessageDigest.isEqual(expected, actual)) {
      return Optional.empty();
    }
    return Optional.of(user);
  }
}
