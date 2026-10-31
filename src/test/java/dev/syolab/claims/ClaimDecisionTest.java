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
class ClaimDecisionTest {

  private static final String C1111 = "c1111111-1111-4111-8111-111111111111";
  private static final String C2222 = "c2222222-2222-4222-8222-222222222222";
  private static final String C3333 = "c3333333-3333-4333-8333-333333333333";
  private static final String C4444 = "c4444444-4444-4444-8444-444444444444";
  private static final String C5555 = "c5555555-5555-4555-8555-555555555555";
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
  void seededAndNewClaimsStartPending() throws Exception {
    JsonNode seeded = getJson(dana, "/api/claims/" + C3333);
    assertThat(seeded.get("decision").asText()).isEqualTo("PENDING");
    assertThat(seeded.get("history")).isEmpty();

    JsonNode created =
        objectMapper.readTree(
            mockMvc
                .perform(
                    post("/api/claims")
                        .header(HttpHeaders.AUTHORIZATION, luis)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
{"policyNumber":"POL-1002","claimantName":"Luis Ortega","amount":"50.00","incidentDate":"2026-06-01","description":"New claim","documentNames":["medical-bill"]}
"""))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(created.get("decision").asText()).isEqualTo("PENDING");
    assertThat(created.get("history")).isEmpty();
  }

  @Test
  void adjusterApprovesAClaim() throws Exception {
    JsonNode body =
        decide(
            dana,
            C2222,
            """
            {"decision":"APPROVED"}
            """);
    assertThat(body.get("decision").asText()).isEqualTo("APPROVED");
    assertThat(body.get("history")).hasSize(1);
    JsonNode entry = body.get("history").get(0);
    assertThat(entry.get("at").asText()).isEqualTo(FixedClock.NOW.toString());
    assertThat(entry.get("actor").asText()).isEqualTo("dana");
    assertThat(entry.get("action").asText()).isEqualTo("DECIDED");
    assertThat(entry.get("fromDecision").asText()).isEqualTo("PENDING");
    assertThat(entry.get("toDecision").asText()).isEqualTo("APPROVED");
    assertThat(entry.get("note").isNull()).isTrue();

    JsonNode fetched = getJson(dana, "/api/claims/" + C2222);
    assertThat(fetched.get("decision").asText()).isEqualTo("APPROVED");
    assertThat(fetched.get("history")).hasSize(1);
  }

  @Test
  void denyAndInfoRequestNeedANote() throws Exception {
    assertOneFieldError(
        decideRaw(
            dana,
            C3333,
            """
            {"decision":"DENIED"}
            """),
        400);
    JsonNode pending = getJson(dana, "/api/claims/" + C3333);
    assertThat(pending.get("decision").asText()).isEqualTo("PENDING");
    assertThat(pending.get("history")).isEmpty();

    assertOneFieldError(
        decideRaw(
            dana,
            C3333,
            """
            {"decision":"INFO_REQUESTED","note":"   "}
            """),
        400);
    pending = getJson(dana, "/api/claims/" + C3333);
    assertThat(pending.get("decision").asText()).isEqualTo("PENDING");
    assertThat(pending.get("history")).isEmpty();
  }

  @Test
  void rejectsMissingUnknownAndPendingDecisions() throws Exception {
    String message = "Decision must be APPROVED, DENIED, or INFO_REQUESTED.";
    assertOneFieldError(decideRaw(dana, C3333, "{}"), 400, message);
    assertOneFieldError(
        decideRaw(
            dana,
            C3333,
            """
            {"decision":null}
            """),
        400,
        message);
    assertOneFieldError(
        decideRaw(
            dana,
            C3333,
            """
            {"decision":"MAYBE"}
            """),
        400,
        message);
    assertOneFieldError(
        decideRaw(
            dana,
            C3333,
            """
            {"decision":"PENDING","note":"x"}
            """),
        400,
        message);
    assertOneFieldError(decideRaw(dana, C3333, "{"), 400);
  }

  @Test
  void finalDecisionCannotChange() throws Exception {
    decide(
        dana,
        C2222,
        """
        {"decision":"APPROVED"}
        """);
    assertOneFieldError(
        decideRaw(
            dana,
            C2222,
            """
            {"decision":"APPROVED"}
            """),
        409);
    assertOneFieldError(
        decideRaw(
            dana,
            C2222,
            """
            {"decision":"DENIED","note":"Too late"}
            """),
        409);
    assertOneFieldError(
        decideRaw(
            dana,
            C2222,
            """
            {"decision":"INFO_REQUESTED","note":"Need more"}
            """),
        409);
    assertThat(getJson(dana, "/api/claims/" + C2222).get("history")).hasSize(1);

    decide(
        dana,
        C5555,
        """
        {"decision":"DENIED","note":"Denied"}
        """);
    assertOneFieldError(
        decideRaw(
            dana,
            C5555,
            """
            {"decision":"APPROVED"}
            """),
        409);
    assertThat(getJson(dana, "/api/claims/" + C5555).get("history")).hasSize(1);
  }

  @Test
  void infoRequestCanRepeatThenResolve() throws Exception {
    decide(
        dana,
        C3333,
        """
        {"decision":"INFO_REQUESTED","note":"First ask"}
        """);
    decide(
        dana,
        C3333,
        """
        {"decision":"INFO_REQUESTED","note":"Second ask"}
        """);
    JsonNode body =
        decide(
            dana,
            C3333,
            """
            {"decision":"APPROVED"}
            """);
    assertThat(body.get("decision").asText()).isEqualTo("APPROVED");
    JsonNode history = body.get("history");
    assertThat(history).hasSize(3);
    assertHistoryPair(history.get(0), "PENDING", "INFO_REQUESTED", "First ask");
    assertHistoryPair(history.get(1), "INFO_REQUESTED", "INFO_REQUESTED", "Second ask");
    assertHistoryPair(history.get(2), "INFO_REQUESTED", "APPROVED", null);
  }

  @Test
  void claimantCannotDecide() throws Exception {
    MockHttpServletResponse own =
        decideRaw(
            amira,
            C1111,
            """
            {"decision":"APPROVED"}
            """);
    MockHttpServletResponse other =
        decideRaw(
            amira,
            C3333,
            """
            {"decision":"APPROVED"}
            """);
    MockHttpServletResponse missing =
        decideRaw(
            amira,
            MISSING,
            """
            {"decision":"APPROVED"}
            """);
    assertThat(own.getStatus()).isEqualTo(403);
    assertThat(other.getStatus()).isEqualTo(403);
    assertThat(missing.getStatus()).isEqualTo(403);
    assertThat(own.getContentAsString()).isEqualTo(other.getContentAsString());
    assertThat(own.getContentAsString()).isEqualTo(missing.getContentAsString());
    assertOneFieldError(own, 403);
    assertThat(getJson(amira, "/api/claims/" + C1111).get("decision").asText())
        .isEqualTo("PENDING");
  }

  @Test
  void decidingAMissingClaimIsNotFound() throws Exception {
    assertOneFieldError(
        decideRaw(
            dana,
            MISSING,
            """
            {"decision":"APPROVED"}
            """),
        404);
    assertOneFieldError(
        decideRaw(
            dana,
            "not-a-uuid",
            """
            {"decision":"APPROVED"}
            """),
        404);
  }

  @Test
  void adjusterDecidesAValidationRejectedClaim() throws Exception {
    JsonNode body =
        decide(
            dana,
            C4444,
            """
            {"decision":"DENIED","note":"Policy inactive"}
            """);
    assertThat(body.get("status").asText()).isEqualTo("REJECTED");
    assertThat(body.get("findings").get(0).get("code").asText()).isEqualTo("POLICY_INACTIVE");
    assertThat(body.get("payout").isNull()).isTrue();
  }

  @Test
  void decisionLeavesStatusAndPayoutUnchanged() throws Exception {
    JsonNode body =
        decide(
            dana,
            C2222,
            """
            {"decision":"APPROVED"}
            """);
    assertThat(body.get("status").asText()).isEqualTo("ACCEPTED");
    assertThat(body.get("amount").asText()).isEqualTo("4000.00");
    assertThat(body.get("payout").asText()).isEqualTo("3750.00");
  }

  @Test
  void deniedClaimStillConsumesCoverage() throws Exception {
    decide(
        dana,
        C1111,
        """
        {"decision":"DENIED","note":"Not covered"}
        """);
    JsonNode created =
        objectMapper.readTree(
            mockMvc
                .perform(
                    post("/api/claims")
                        .header(HttpHeaders.AUTHORIZATION, amira)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
{"policyNumber":"POL-1001","claimantName":"Amira Shah","amount":"1.00","incidentDate":"2026-06-01","description":"Another scratch","documentNames":["police-report","repair-estimate"]}
"""))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(created.get("status").asText()).isEqualTo("REJECTED");
    assertThat(created.get("findings").get(0).get("code").asText()).isEqualTo("EXCEEDS_COVERAGE");
  }

  @Test
  void claimantSeesHistoryOnlyOnOwnClaims() throws Exception {
    decide(
        dana,
        C1111,
        """
        {"decision":"INFO_REQUESTED","note":"Send photos"}
        """);
    JsonNode amiraClaim = getJson(amira, "/api/claims/" + C1111);
    assertThat(amiraClaim.get("history")).hasSize(1);
    assertThat(amiraClaim.get("history").get(0).get("note").asText()).isEqualTo("Send photos");

    MockHttpServletResponse missing = getRaw(amira, "/api/claims/" + MISSING);
    MockHttpServletResponse other = getRaw(amira, "/api/claims/" + C3333);
    assertThat(other.getStatus()).isEqualTo(404);
    assertThat(other.getContentAsString()).isEqualTo(missing.getContentAsString());
  }

  @Test
  void listCarriesDecisionAndHistory() throws Exception {
    decide(
        dana,
        C3333,
        """
        {"decision":"INFO_REQUESTED","note":"Receipt please"}
        """);
    JsonNode claims = getJson(dana, "/api/claims");
    assertThat(claims).hasSize(6);
    int pending = 0;
    for (JsonNode claim : claims) {
      if (C3333.equals(claim.get("id").asText())) {
        assertThat(claim.get("decision").asText()).isEqualTo("INFO_REQUESTED");
        assertThat(claim.get("history")).hasSize(1);
      } else {
        assertThat(claim.get("decision").asText()).isEqualTo("PENDING");
        assertThat(claim.get("history")).isEmpty();
        pending++;
      }
    }
    assertThat(pending).isEqualTo(5);
  }

  private JsonNode decide(String token, String claimId, String json) throws Exception {
    MockHttpServletResponse response = decideRaw(token, claimId, json);
    assertThat(response.getStatus()).isEqualTo(200);
    return objectMapper.readTree(response.getContentAsString());
  }

  private MockHttpServletResponse decideRaw(String token, String claimId, String json)
      throws Exception {
    return mockMvc
        .perform(
            post("/api/claims/" + claimId + "/decision")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andReturn()
        .getResponse();
  }

  private JsonNode getJson(String token, String path) throws Exception {
    MockHttpServletResponse response = getRaw(token, path);
    assertThat(response.getStatus()).isEqualTo(200);
    return objectMapper.readTree(response.getContentAsString());
  }

  private MockHttpServletResponse getRaw(String token, String path) throws Exception {
    return mockMvc
        .perform(get(path).header(HttpHeaders.AUTHORIZATION, token))
        .andReturn()
        .getResponse();
  }

  private void assertOneFieldError(MockHttpServletResponse response, int status) throws Exception {
    assertThat(response.getStatus()).isEqualTo(status);
    JsonNode error = objectMapper.readTree(response.getContentAsString());
    assertThat(error.get("message").asText()).isNotBlank();
    assertThat(error.size()).isEqualTo(1);
  }

  private void assertOneFieldError(MockHttpServletResponse response, int status, String message)
      throws Exception {
    assertOneFieldError(response, status);
    assertThat(objectMapper.readTree(response.getContentAsString()).get("message").asText())
        .isEqualTo(message);
  }

  private static void assertHistoryPair(JsonNode entry, String from, String to, String note) {
    assertThat(entry.get("fromDecision").asText()).isEqualTo(from);
    assertThat(entry.get("toDecision").asText()).isEqualTo(to);
    if (note == null) {
      assertThat(entry.get("note").isNull()).isTrue();
    } else {
      assertThat(entry.get("note").asText()).isEqualTo(note);
    }
  }
}
