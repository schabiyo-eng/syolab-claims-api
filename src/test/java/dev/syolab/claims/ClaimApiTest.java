package dev.syolab.claims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.syolab.claims.domain.Policy;
import dev.syolab.claims.domain.PolicyStatus;
import dev.syolab.claims.seed.SeedData;
import dev.syolab.claims.store.PolicyRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ClaimApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SeedData seedData;
  @Autowired private PolicyRepository policies;

  @BeforeEach
  void resetSeed() {
    seedData.reset();
  }

  @Test
  void listsPoliciesInPolicyNumberOrder() throws Exception {
    JsonNode body = getJson("/api/policies");

    assertThat(body).hasSize(4);
    assertThat(body.get(0).get("policyNumber").asText()).isEqualTo("POL-1001");
    assertThat(body.get(0).get("holderName").asText()).isEqualTo("Amira Shah");
    assertThat(body.get(0).get("status").asText()).isEqualTo("ACTIVE");
    assertThat(body.get(0).get("coverageLimit").asText()).isEqualTo("10000.00");
    assertThat(body.get(0).get("requiredDocuments").get(0).asText()).isEqualTo("police-report");
    assertThat(body.get(0).get("requiredDocuments").get(1).asText()).isEqualTo("repair-estimate");
    assertThat(body.get(1).get("policyNumber").asText()).isEqualTo("POL-1002");
    assertThat(body.get(2).get("holderName").asText()).isEqualTo("Nora Blake");
    assertThat(body.get(2).get("status").asText()).isEqualTo("EXPIRED");
    assertThat(body.get(3).get("policyNumber").asText()).isEqualTo("POL-1004");
  }

  @Test
  void getsOnePolicy() throws Exception {
    JsonNode body = getJson("/api/policies/POL-1002");

    assertThat(body.get("holderName").asText()).isEqualTo("Luis Ortega");
    assertThat(body.get("coverageLimit").asText()).isEqualTo("50000.00");
    assertThat(body.get("requiredDocuments").get(0).asText()).isEqualTo("medical-bill");
  }

  @Test
  void unknownPolicyIsNotFound() throws Exception {
    assertError(getRaw("/api/policies/POL-404"), 404);
  }

  @Test
  void listsClaimsNewestFirstAndKeepsTheSeededDesk() throws Exception {
    JsonNode claims = getJson("/api/claims");

    assertThat(claims).hasSize(5);
    assertThat(claims.get(0).get("id").asText()).isEqualTo("c4444444-4444-4444-8444-444444444444");
    assertThat(claims.get(0).get("status").asText()).isEqualTo("REJECTED");
    assertThat(codes(claims.get(0))).contains("POLICY_INACTIVE");

    JsonNode chris = findClaimant(claims, "Chris Dalton");
    assertThat(chris.get("status").asText()).isEqualTo("ACCEPTED");
    assertThat(chris.get("amount").asText()).isEqualTo("10000.00");
    assertThat(chris.get("policyNumber").asText()).isEqualTo("POL-1004");

    BigDecimal amira = BigDecimal.ZERO;
    for (JsonNode claim : claims) {
      if ("Amira Shah".equals(claim.get("claimantName").asText())
          && "ACCEPTED".equals(claim.get("status").asText())) {
        amira = amira.add(new BigDecimal(claim.get("amount").asText()));
      }
    }
    assertThat(amira).isEqualByComparingTo("10000.00");

    JsonNode luis = findClaimant(claims, "Luis Ortega");
    assertThat(luis.get("status").asText()).isEqualTo("FLAGGED");
    assertThat(codes(luis)).contains("MISSING_DOCUMENTS");
  }

  @Test
  void getsOneClaim() throws Exception {
    JsonNode claim = getJson("/api/claims/c2222222-2222-4222-8222-222222222222");

    assertThat(claim.get("claimantName").asText()).isEqualTo("Amira Shah");
    assertThat(claim.get("amount").asText()).isEqualTo("4000.00");
    assertThat(claim.get("status").asText()).isEqualTo("ACCEPTED");
    assertThat(claim.get("createdAt").asText()).isEqualTo("2026-03-12T15:00:00Z");
  }

  @Test
  void unknownClaimIsNotFound() throws Exception {
    assertError(getRaw("/api/claims/00000000-0000-4000-8000-000000000099"), 404);
    assertError(getRaw("/api/claims/not-a-uuid"), 404);
  }

  @Test
  void createsAWellFormedClaim() throws Exception {
    JsonNode created =
        postClaim(
            claimJson("POL-1002", "\"100.00\"", "2026-06-01", "Follow up visit", "medical-bill"));

    assertThat(created.get("status").asText()).isEqualTo("ACCEPTED");
    assertThat(created.get("amount").asText()).isEqualTo("100.00");
    assertThat(created.get("findings")).isEmpty();

    JsonNode claims = getJson("/api/claims");
    assertThat(claims.get(0).get("id").asText()).isEqualTo(created.get("id").asText());
    JsonNode fetched = getJson("/api/claims/" + created.get("id").asText());
    assertThat(fetched.get("description").asText()).isEqualTo("Follow up visit");
  }

  @Test
  void rejectsWhenOpenClaimsTogetherExceedTheCoverageLimit() throws Exception {
    JsonNode created =
        postClaim(
            claimJson(
                "POL-1001",
                "\"1.00\"",
                "2026-06-01",
                "Another scratch",
                "police-report",
                "repair-estimate"));

    assertThat(created.get("status").asText()).isEqualTo("REJECTED");
    assertThat(codes(created)).contains("EXCEEDS_COVERAGE");
    assertThat(severities(created, "EXCEEDS_COVERAGE")).contains("ERROR");
  }

  @Test
  void rejectedClaimsDoNotConsumeCoverage() throws Exception {
    savePolicy("POL-FRESH", "10000.00");

    JsonNode first =
        postClaim(
            claimJson("POL-FRESH", "\"15000.00\"", "2026-06-01", "Large repair", "police-report"));
    assertThat(first.get("status").asText()).isEqualTo("REJECTED");
    assertThat(codes(first)).contains("EXCEEDS_COVERAGE");

    JsonNode second =
        postClaim(
            claimJson("POL-FRESH", "\"10000.00\"", "2026-06-02", "Full repair", "police-report"));
    assertThat(second.get("status").asText()).isEqualTo("ACCEPTED");
    assertThat(second.get("findings")).isEmpty();
  }

  @Test
  void acceptsDecimalAmountStringsAndNumbers() throws Exception {
    savePolicy("POL-WIDE", "100000.00", "medical-bill");

    assertThat(
            postClaim(claimJson("POL-WIDE", "\"10000\"", "2026-06-01", "Flat", "medical-bill"))
                .get("amount")
                .asText())
        .isEqualTo("10000.00");
    assertThat(
            postClaim(
                    claimJson("POL-WIDE", "\"10000.0\"", "2026-06-02", "One digit", "medical-bill"))
                .get("amount")
                .asText())
        .isEqualTo("10000.00");
    assertThat(
            postClaim(
                    claimJson(
                        "POL-WIDE", "\"10000.00\"", "2026-06-03", "Two digits", "medical-bill"))
                .get("amount")
                .asText())
        .isEqualTo("10000.00");
    assertThat(
            postClaim(claimJson("POL-WIDE", "10000", "2026-06-04", "Number", "medical-bill"))
                .get("amount")
                .asText())
        .isEqualTo("10000.00");
    assertThat(
            postClaim(claimJson("POL-WIDE", "10000.0", "2026-06-05", "Number one", "medical-bill"))
                .get("amount")
                .asText())
        .isEqualTo("10000.00");
    assertThat(
            postClaim(claimJson("POL-WIDE", "10000.00", "2026-06-06", "Number two", "medical-bill"))
                .get("amount")
                .asText())
        .isEqualTo("10000.00");
  }

  @Test
  void rejectsMalformedClaims() throws Exception {
    savePolicy("POL-WIDE", "100000.00");

    assertError(
        postRaw(claimJson("POL-WIDE", "\"10000.001\"", "2026-06-01", "Too fine", "police-report")),
        400);
    assertError(
        postRaw(claimJson("POL-WIDE", "\"abc\"", "2026-06-01", "Words", "police-report")), 400);
    assertError(
        postRaw(claimJson("POL-WIDE", "\"0.00\"", "2026-06-01", "Zero", "police-report")), 400);
    assertError(
        postRaw(claimJson("POL-WIDE", "\"-1.00\"", "2026-06-01", "Negative", "police-report")),
        400);
    assertError(
        postRaw(
            claimJson("POL-WIDE", "10000.001", "2026-06-01", "Too fine number", "police-report")),
        400);
    assertError(
        postRaw(
            """
{"policyNumber":"POL-WIDE","amount":"10.00","incidentDate":"2026-06-01","description":"No name","documentNames":["police-report"]}
"""),
        400);
    assertError(
        postRaw(
            claimJson("POL-WIDE", "\"10.00\"", "2026-06-01", "   ", "police-report")
                .replace("Test Holder", "   ")),
        400);
    assertError(postRaw("{"), 400);
    assertError(postRaw(""), 400);
  }

  @Test
  void unknownPolicyNumberIsStored() throws Exception {
    JsonNode created =
        postClaim(
            claimJson("POL-MISSING", "\"20.00\"", "2026-06-01", "No such policy", "police-report"));

    assertThat(created.get("status").asText()).isEqualTo("REJECTED");
    assertThat(codes(created)).containsExactly("POLICY_NOT_FOUND");
  }

  @Test
  void allowsTheDeskOrigin() throws Exception {
    mockMvc
        .perform(get("/api/policies").header("Origin", "http://localhost:5173"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
  }

  @Test
  void publishesInteractiveDocs() throws Exception {
    mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
  }

  private void savePolicy(String number, String limit, String... documents) {
    List<String> required = documents.length == 0 ? List.of("police-report") : List.of(documents);
    policies.save(
        new Policy(
            number,
            "Test Holder",
            PolicyStatus.ACTIVE,
            new BigDecimal(limit),
            LocalDate.parse("2025-01-01"),
            LocalDate.parse("2027-12-31"),
            required));
  }

  private String claimJson(
      String policy,
      String amountLiteral,
      String incidentDate,
      String description,
      String... documents) {
    StringBuilder docs = new StringBuilder();
    for (int i = 0; i < documents.length; i++) {
      if (i > 0) {
        docs.append(',');
      }
      docs.append('"').append(documents[i]).append('"');
    }
    return """
{"policyNumber":"%s","claimantName":"Test Holder","amount":%s,"incidentDate":"%s","description":"%s","documentNames":[%s]}
"""
        .formatted(policy, amountLiteral, incidentDate, description, docs);
  }

  private JsonNode postClaim(String json) throws Exception {
    MockHttpServletResponse response = postRaw(json);
    assertThat(response.getStatus()).isEqualTo(201);
    return objectMapper.readTree(response.getContentAsString());
  }

  private MockHttpServletResponse postRaw(String json) throws Exception {
    return mockMvc
        .perform(post("/api/claims").contentType(MediaType.APPLICATION_JSON).content(json))
        .andReturn()
        .getResponse();
  }

  private JsonNode getJson(String path) throws Exception {
    MockHttpServletResponse response = getRaw(path);
    assertThat(response.getStatus()).isEqualTo(200);
    return objectMapper.readTree(response.getContentAsString());
  }

  private MockHttpServletResponse getRaw(String path) throws Exception {
    return mockMvc.perform(get(path)).andReturn().getResponse();
  }

  private void assertError(MockHttpServletResponse response, int expectedStatus) throws Exception {
    assertThat(response.getStatus()).isEqualTo(expectedStatus);
    JsonNode error = objectMapper.readTree(response.getContentAsString());
    assertThat(error.get("message").asText()).isNotBlank();
    assertThat(error.size()).isEqualTo(1);
  }

  private JsonNode findClaimant(JsonNode claims, String name) {
    for (JsonNode claim : claims) {
      if (name.equals(claim.get("claimantName").asText())) {
        return claim;
      }
    }
    throw new AssertionError("Missing claimant " + name);
  }

  private static List<String> codes(JsonNode claim) {
    java.util.ArrayList<String> codes = new java.util.ArrayList<>();
    for (JsonNode finding : claim.get("findings")) {
      codes.add(finding.get("code").asText());
    }
    return codes;
  }

  private static List<String> severities(JsonNode claim, String code) {
    java.util.ArrayList<String> severities = new java.util.ArrayList<>();
    for (JsonNode finding : claim.get("findings")) {
      if (code.equals(finding.get("code").asText())) {
        severities.add(finding.get("severity").asText());
      }
    }
    return severities;
  }
}
