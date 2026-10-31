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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ClaimantScopeTest {

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
  void claimantListsOnlyClaimsOnPoliciesTheyHold() throws Exception {
    JsonNode amiraClaims = getJson("/api/claims", amira);
    assertThat(claimIds(amiraClaims))
        .containsExactlyInAnyOrder(
            "c1111111-1111-4111-8111-111111111111", "c2222222-2222-4222-8222-222222222222");

    JsonNode luisClaims = getJson("/api/claims", luis);
    assertThat(claimIds(luisClaims)).containsExactly("c3333333-3333-4333-8333-333333333333");
  }

  @Test
  void claimantReadsTheirOwnClaim() throws Exception {
    mockMvc
        .perform(
            get("/api/claims/c2222222-2222-4222-8222-222222222222")
                .header(HttpHeaders.AUTHORIZATION, amira))
        .andExpect(status().isOk());
  }

  @Test
  void claimantCannotReadAnotherClaimantsClaim() throws Exception {
    MockHttpServletResponse missing =
        getRaw("/api/claims/00000000-0000-4000-8000-000000000099", amira);
    MockHttpServletResponse hidden =
        getRaw("/api/claims/c3333333-3333-4333-8333-333333333333", amira);

    assertThat(hidden.getStatus()).isEqualTo(404);
    assertThat(hidden.getContentAsString()).isEqualTo(missing.getContentAsString());
  }

  @Test
  void claimantListsOnlyTheirPolicies() throws Exception {
    JsonNode amiraPolicies = getJson("/api/policies", amira);
    assertThat(policyNumbers(amiraPolicies)).containsExactly("POL-1001");

    JsonNode luisPolicies = getJson("/api/policies", luis);
    assertThat(policyNumbers(luisPolicies)).containsExactly("POL-1002");
  }

  @Test
  void claimantCannotReadAnotherClaimantsPolicy() throws Exception {
    MockHttpServletResponse missing = getRaw("/api/policies/POL-404", amira);
    MockHttpServletResponse hidden = getRaw("/api/policies/POL-1002", amira);

    assertThat(hidden.getStatus()).isEqualTo(404);
    assertThat(hidden.getContentAsString()).isEqualTo(missing.getContentAsString());
  }

  @Test
  void claimantCannotFileOnAnotherClaimantsPolicy() throws Exception {
    MockHttpServletResponse missingPolicy =
        postRaw(
            amira,
            """
{"policyNumber":"POL-MISSING","claimantName":"Amira Shah","amount":"10.00","incidentDate":"2026-06-01","description":"Scratch","documentNames":["medical-bill"]}
""");
    MockHttpServletResponse hiddenPolicy =
        postRaw(
            amira,
            """
{"policyNumber":"POL-1002","claimantName":"Amira Shah","amount":"10.00","incidentDate":"2026-06-01","description":"Scratch","documentNames":["medical-bill"]}
""");

    assertThat(hiddenPolicy.getStatus()).isEqualTo(404);
    assertThat(hiddenPolicy.getContentAsString()).isEqualTo(missingPolicy.getContentAsString());

    JsonNode deskClaims = getJson("/api/claims", dana);
    assertThat(deskClaims).hasSize(6);
  }

  @Test
  void claimantFilingOnUnknownPolicyIsNotFound() throws Exception {
    MockHttpServletResponse response =
        postRaw(
            amira,
            """
{"policyNumber":"POL-MISSING","claimantName":"Amira Shah","amount":"10.00","incidentDate":"2026-06-01","description":"Scratch","documentNames":["medical-bill"]}
""");

    assertThat(response.getStatus()).isEqualTo(404);
    JsonNode error = objectMapper.readTree(response.getContentAsString());
    assertThat(error.get("message").asText()).isNotBlank();
    assertThat(error.size()).isEqualTo(1);

    JsonNode deskClaims = getJson("/api/claims", dana);
    assertThat(deskClaims).hasSize(6);
  }

  @Test
  void newClaimIsVisibleOnlyToItsHolderAndTheDesk() throws Exception {
    MockHttpServletResponse created =
        postRaw(
            luis,
            """
{"policyNumber":"POL-1002","claimantName":"Luis Ortega","amount":"50.00","incidentDate":"2026-06-01","description":"New visit","documentNames":["medical-bill"]}
""");
    assertThat(created.getStatus()).isEqualTo(201);
    JsonNode claim = objectMapper.readTree(created.getContentAsString());
    String id = claim.get("id").asText();

    mockMvc
        .perform(get("/api/claims/" + id).header(HttpHeaders.AUTHORIZATION, luis))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/claims/" + id).header(HttpHeaders.AUTHORIZATION, dana))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/claims/" + id).header(HttpHeaders.AUTHORIZATION, amira))
        .andExpect(status().isNotFound());

    JsonNode amiraClaims = getJson("/api/claims", amira);
    assertThat(claimIds(amiraClaims))
        .doesNotContain(id)
        .containsExactlyInAnyOrder(
            "c1111111-1111-4111-8111-111111111111", "c2222222-2222-4222-8222-222222222222");
  }

  @Test
  void adjusterReadsEveryClaimantsClaimsAndPolicies() throws Exception {
    mockMvc
        .perform(
            get("/api/claims/c1111111-1111-4111-8111-111111111111")
                .header(HttpHeaders.AUTHORIZATION, dana))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            get("/api/claims/c3333333-3333-4333-8333-333333333333")
                .header(HttpHeaders.AUTHORIZATION, dana))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/policies/POL-1001").header(HttpHeaders.AUTHORIZATION, dana))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/policies/POL-1002").header(HttpHeaders.AUTHORIZATION, dana))
        .andExpect(status().isOk());
  }

  private JsonNode getJson(String path, String token) throws Exception {
    MockHttpServletResponse response = getRaw(path, token);
    assertThat(response.getStatus()).isEqualTo(200);
    return objectMapper.readTree(response.getContentAsString());
  }

  private MockHttpServletResponse getRaw(String path, String token) throws Exception {
    return mockMvc
        .perform(get(path).header(HttpHeaders.AUTHORIZATION, token))
        .andReturn()
        .getResponse();
  }

  private MockHttpServletResponse postRaw(String token, String json) throws Exception {
    return mockMvc
        .perform(
            post("/api/claims")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andReturn()
        .getResponse();
  }

  private static List<String> claimIds(JsonNode claims) {
    ArrayList<String> ids = new ArrayList<>();
    for (JsonNode claim : claims) {
      ids.add(claim.get("id").asText());
    }
    return ids;
  }

  private static List<String> policyNumbers(JsonNode policies) {
    ArrayList<String> numbers = new ArrayList<>();
    for (JsonNode policy : policies) {
      numbers.add(policy.get("policyNumber").asText());
    }
    return numbers;
  }
}
