package dev.syolab.claims.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

public final class Logins {

  private Logins() {}

  /**
   * Signs in through POST /api/auth/login as the named demo user. Returns "Bearer &lt;token&gt;".
   */
  public static String bearer(MockMvc mockMvc, String username) throws Exception {
    DemoUsers.DemoUser user =
        DemoUsers.ALL.stream()
            .filter(demo -> demo.username().equals(username))
            .findFirst()
            .orElseThrow();
    String body =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"username":"%s","password":"%s"}
                        """
                            .formatted(user.username(), user.password())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode json = new ObjectMapper().readTree(body);
    return "Bearer " + json.get("token").asText();
  }

  /**
   * Signs in through POST /api/auth/login as the first demo user with this role. Returns "Bearer
   * &lt;token&gt;".
   */
  public static String bearer(MockMvc mockMvc, Role role) throws Exception {
    DemoUsers.DemoUser user =
        DemoUsers.ALL.stream().filter(demo -> demo.role() == role).findFirst().orElseThrow();
    return bearer(mockMvc, user.username());
  }
}
