package dev.syolab.claims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.syolab.claims.auth.Logins;
import dev.syolab.claims.auth.Role;
import dev.syolab.claims.seed.SeedData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClock.class)
class AuditLogTest {

  private static final String C2222 = "c2222222-2222-4222-8222-222222222222";
  private static final String C3333 = "c3333333-3333-4333-8333-333333333333";
  private static final String MISSING = "00000000-0000-4000-8000-000000000099";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SeedData seedData;

  private String amira;
  private String luis;
  private String dana;

  @BeforeEach
  void resetSeed() throws Exception {
    seedData.reset();
    amira = Logins.bearer(mockMvc, "amira");
    luis = Logins.bearer(mockMvc, "luis");
    dana = Logins.bearer(mockMvc, Role.ADJUSTER);
  }

  @Test
  void seedWritesNoAudit() throws Exception {
    JsonNode audit = getAudit(dana);
    assertThat(audit).isEmpty();
  }

  @Test
  void auditRecordsDecisionsAndCommentsInOrder() throws Exception {
    mockMvc
        .perform(
            post("/api/claims/" + C3333 + "/decision")
                .header(HttpHeaders.AUTHORIZATION, dana)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"decision":"INFO_REQUESTED","note":"Need receipt"}
                    """))
        .andExpect(status().isOk());

    JsonNode comment =
        objectMapper.readTree(
            mockMvc
                .perform(
                    post("/api/claims/" + C3333 + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, luis)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {"body":"Receipt attached"}
                            """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());

    mockMvc
        .perform(
            post("/api/claims/" + C3333 + "/decision")
                .header(HttpHeaders.AUTHORIZATION, dana)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"decision":"APPROVED","note":"Looks good"}
                    """))
        .andExpect(status().isOk());

    JsonNode audit = getAudit(dana);
    assertThat(audit).hasSize(3);
    assertEntry(audit.get(0), 1, "DECIDED", "dana", "ADJUSTER", C3333);
    assertThat(audit.get(0).get("fromDecision").asText()).isEqualTo("PENDING");
    assertThat(audit.get(0).get("toDecision").asText()).isEqualTo("INFO_REQUESTED");
    assertThat(audit.get(0).get("commentId").isNull()).isTrue();

    assertEntry(audit.get(1), 2, "COMMENTED", "luis", "CLAIMANT", C3333);
    assertThat(audit.get(1).get("fromDecision").isNull()).isTrue();
    assertThat(audit.get(1).get("toDecision").isNull()).isTrue();
    assertThat(audit.get(1).get("commentId").asText()).isEqualTo(comment.get("id").asText());

    assertEntry(audit.get(2), 3, "DECIDED", "dana", "ADJUSTER", C3333);
    assertThat(audit.get(2).get("fromDecision").asText()).isEqualTo("INFO_REQUESTED");
    assertThat(audit.get(2).get("toDecision").asText()).isEqualTo("APPROVED");
    assertThat(audit.get(2).get("commentId").isNull()).isTrue();

    for (JsonNode entry : audit) {
      assertThat(entry.get("at").asText()).isEqualTo(FixedClock.NOW.toString());
    }
  }

  @Test
  void failedRequestsAreNotAudited() throws Exception {
    mockMvc
        .perform(
            post("/api/claims/" + C3333 + "/decision")
                .header(HttpHeaders.AUTHORIZATION, dana)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"decision":"DENIED"}
                    """))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/claims/" + C3333 + "/decision")
                .header(HttpHeaders.AUTHORIZATION, amira)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"decision":"APPROVED"}
                    """))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/claims/" + MISSING + "/decision")
                .header(HttpHeaders.AUTHORIZATION, dana)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"decision":"APPROVED"}
                    """))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            post("/api/claims/" + C2222 + "/decision")
                .header(HttpHeaders.AUTHORIZATION, dana)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"decision":"APPROVED"}
                    """))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/claims/" + C2222 + "/decision")
                .header(HttpHeaders.AUTHORIZATION, dana)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"decision":"DENIED","note":"Too late"}
                    """))
        .andExpect(status().isConflict());

    JsonNode audit = getAudit(dana);
    assertThat(audit).hasSize(1);
    assertThat(audit.get(0).get("action").asText()).isEqualTo("DECIDED");
    assertThat(audit.get(0).get("claimId").asText()).isEqualTo(C2222);
  }

  @Test
  void claimantCannotReadAudit() throws Exception {
    assertOneFieldError(getAuditRaw(amira), 403);
  }

  @Test
  void auditNeedsAToken() throws Exception {
    MockHttpServletResponse response = mockMvc.perform(get("/api/audit")).andReturn().getResponse();
    assertThat(response.getStatus()).isEqualTo(401);
  }

  private JsonNode getAudit(String token) throws Exception {
    MockHttpServletResponse response = getAuditRaw(token);
    assertThat(response.getStatus()).isEqualTo(200);
    return objectMapper.readTree(response.getContentAsString());
  }

  private MockHttpServletResponse getAuditRaw(String token) throws Exception {
    return mockMvc
        .perform(get("/api/audit").header(HttpHeaders.AUTHORIZATION, token))
        .andReturn()
        .getResponse();
  }

  private void assertOneFieldError(MockHttpServletResponse response, int status) throws Exception {
    assertThat(response.getStatus()).isEqualTo(status);
    JsonNode error = objectMapper.readTree(response.getContentAsString());
    assertThat(error.get("message").asText()).isNotBlank();
    assertThat(error.size()).isEqualTo(1);
  }

  private static void assertEntry(
      JsonNode entry, long sequence, String action, String actor, String role, String claimId) {
    assertThat(entry.get("sequence").asLong()).isEqualTo(sequence);
    assertThat(entry.get("action").asText()).isEqualTo(action);
    assertThat(entry.get("actor").asText()).isEqualTo(actor);
    assertThat(entry.get("role").asText()).isEqualTo(role);
    assertThat(entry.get("claimId").asText()).isEqualTo(claimId);
  }
}
