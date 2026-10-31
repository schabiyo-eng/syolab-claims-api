package dev.syolab.claims.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.syolab.claims.seed.SeedData;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SeedData seedData;
  @Autowired private JwtEncoder jwtEncoder;

  @BeforeEach
  void resetSeed() {
    seedData.reset();
  }

  @Test
  void adjusterSignsIn() throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"username":"dana","password":"dana-demo"}
                        """))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(200);
    JsonNode body = objectMapper.readTree(response.getContentAsString());
    assertThat(body.get("token").asText()).isNotBlank();
    assertThat(body.get("username").asText()).isEqualTo("dana");
    assertThat(body.get("role").asText()).isEqualTo("ADJUSTER");
    assertThat(body.get("displayName").asText()).isEqualTo("Dana Whitfield");
  }

  @Test
  void wrongPasswordIsUnauthorized() throws Exception {
    assertOneFieldError(
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"username":"dana","password":"wrong"}
                        """))
            .andReturn()
            .getResponse(),
        401);
  }

  @Test
  void unknownUserIsUnauthorized() throws Exception {
    assertOneFieldError(
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"username":"nobody","password":"x"}
                        """))
            .andReturn()
            .getResponse(),
        401);
  }

  @Test
  void meReturnsTheClaimant() throws Exception {
    mockMvc
        .perform(
            get("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, Logins.bearer(mockMvc, Role.CLAIMANT)))
        .andExpect(status().isOk())
        .andExpect(
            result -> {
              JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
              assertThat(body.get("username").asText()).isEqualTo("amira");
              assertThat(body.get("role").asText()).isEqualTo("CLAIMANT");
              assertThat(body.get("displayName").asText()).isEqualTo("Amira Shah");
            });
  }

  @Test
  void protectedEndpointNeedsAToken() throws Exception {
    MockHttpServletResponse response =
        mockMvc.perform(get("/api/claims")).andReturn().getResponse();

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).startsWith("Bearer");
    assertOneFieldError(response, 401);
    JsonNode error = objectMapper.readTree(response.getContentAsString());
    assertThat(error.get("message").asText()).isEqualTo("Sign in to continue.");
  }

  @Test
  void tamperedTokenIsUnauthorized() throws Exception {
    String token = Logins.bearer(mockMvc, Role.ADJUSTER).substring("Bearer ".length());
    int inSignature = token.length() - 10;
    char flipped = token.charAt(inSignature) == 'a' ? 'b' : 'a';
    String bad = token.substring(0, inSignature) + flipped + token.substring(inSignature + 1);

    mockMvc
        .perform(get("/api/claims").header(HttpHeaders.AUTHORIZATION, "Bearer " + bad))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void expiredTokenIsUnauthorized() throws Exception {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject("dana")
            .claim("role", "ADJUSTER")
            .claim("name", "Dana Whitfield")
            .issuedAt(now.minus(Duration.ofHours(9)))
            .expiresAt(now.minus(Duration.ofHours(1)))
            .build();
    String token =
        jwtEncoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();

    mockMvc
        .perform(get("/api/claims").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void protectedEndpointAcceptsAToken() throws Exception {
    mockMvc
        .perform(
            get("/api/claims")
                .header(HttpHeaders.AUTHORIZATION, Logins.bearer(mockMvc, Role.ADJUSTER)))
        .andExpect(status().isOk())
        .andExpect(
            result -> {
              JsonNode claims = objectMapper.readTree(result.getResponse().getContentAsString());
              assertThat(claims).hasSize(6);
            });
  }

  @Test
  void adjusterCannotFileClaims() throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/api/claims")
                    .header(HttpHeaders.AUTHORIZATION, Logins.bearer(mockMvc, Role.ADJUSTER))
                    .header("Origin", "http://localhost:5173")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
{"policyNumber":"POL-1001","claimantName":"Amira Shah","amount":"10.00","incidentDate":"2026-06-01","description":"Scratch","documentNames":["police-report","repair-estimate"]}
"""))
            .andReturn()
            .getResponse();

    assertOneFieldError(response, 403);
    assertThat(response.getHeader("Access-Control-Allow-Origin"))
        .isEqualTo("http://localhost:5173");
    mockMvc
        .perform(
            get("/api/claims")
                .header(HttpHeaders.AUTHORIZATION, Logins.bearer(mockMvc, Role.ADJUSTER)))
        .andExpect(
            result ->
                assertThat(objectMapper.readTree(result.getResponse().getContentAsString()))
                    .hasSize(6));
  }

  @Test
  void deskPreflightNeedsNoToken() throws Exception {
    mockMvc
        .perform(
            options("/api/claims")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
        .andExpect(
            result ->
                assertThat(result.getResponse().getHeader("Access-Control-Allow-Headers"))
                    .containsIgnoringCase("authorization"));
  }

  @Test
  void unauthorizedResponseIsReadableByTheDesk() throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(get("/api/claims").header(HttpHeaders.ORIGIN, "http://127.0.0.1:5173"))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getHeader("Access-Control-Allow-Origin"))
        .isEqualTo("http://127.0.0.1:5173");
  }

  @Test
  void docsNeedNoToken() throws Exception {
    mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
    mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
  }

  @Test
  void everyClaimantIsASeededHolder() throws Exception {
    for (DemoUsers.DemoUser user : DemoUsers.ALL) {
      if (user.role() != Role.CLAIMANT) {
        continue;
      }
      MockHttpServletResponse response =
          mockMvc
              .perform(
                  get("/api/policies")
                      .header(HttpHeaders.AUTHORIZATION, Logins.bearer(mockMvc, user.username())))
              .andReturn()
              .getResponse();
      assertThat(response.getStatus()).isEqualTo(200);
      JsonNode policies = objectMapper.readTree(response.getContentAsString());
      assertThat(policies).isNotEmpty();
      for (JsonNode policy : policies) {
        assertThat(policy.get("holderName").asText()).isEqualTo(user.displayName());
      }
    }
  }

  @Test
  void readmeListsEveryDemoUser() throws Exception {
    String readme = Files.readString(Path.of("README.md"));
    for (DemoUsers.DemoUser user : DemoUsers.ALL) {
      assertThat(readme).contains(user.username());
      assertThat(readme).contains(user.password());
    }
  }

  private void assertOneFieldError(MockHttpServletResponse response, int expectedStatus)
      throws Exception {
    assertThat(response.getStatus()).isEqualTo(expectedStatus);
    JsonNode error = objectMapper.readTree(response.getContentAsString());
    assertThat(error.get("message").asText()).isNotBlank();
    assertThat(error.size()).isEqualTo(1);
  }
}
