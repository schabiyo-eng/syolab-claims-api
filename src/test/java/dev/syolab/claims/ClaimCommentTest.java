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
class ClaimCommentTest {

  private static final String C1111 = "c1111111-1111-4111-8111-111111111111";
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
  void claimantCommentsOnOwnClaim() throws Exception {
    JsonNode created =
        postComment(
            amira,
            C1111,
            """
            {"body":"  Receipt attached.  "}
            """);
    assertThat(created.size()).isEqualTo(5);
    assertThat(created.get("id").asText())
        .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    assertThat(created.get("at").asText()).isEqualTo(FixedClock.NOW.toString());
    assertThat(created.get("author").asText()).isEqualTo("amira");
    assertThat(created.get("role").asText()).isEqualTo("CLAIMANT");
    assertThat(created.get("body").asText()).isEqualTo("Receipt attached.");

    JsonNode listed = getComments(amira, C1111);
    assertThat(listed).hasSize(1);
    assertThat(listed.get(0).get("body").asText()).isEqualTo("Receipt attached.");
  }

  @Test
  void adjusterCommentsOnAnyClaim() throws Exception {
    JsonNode created =
        postComment(
            dana,
            C3333,
            """
            {"body":"Checking documents"}
            """);
    assertThat(created.get("author").asText()).isEqualTo("dana");
    assertThat(created.get("role").asText()).isEqualTo("ADJUSTER");
  }

  @Test
  void commentsListOldestFirst() throws Exception {
    postComment(
        dana,
        C3333,
        """
        {"body":"From dana"}
        """);
    postComment(
        luis,
        C3333,
        """
        {"body":"From luis"}
        """);
    JsonNode listed = getComments(luis, C3333);
    assertThat(listed).hasSize(2);
    assertThat(listed.get(0).get("author").asText()).isEqualTo("dana");
    assertThat(listed.get(1).get("author").asText()).isEqualTo("luis");
  }

  @Test
  void claimantCannotReadOrCommentOnAnotherClaimantsClaim() throws Exception {
    MockHttpServletResponse missing = getRaw(amira, "/api/claims/" + MISSING + "/comments");
    MockHttpServletResponse getOther = getRaw(amira, "/api/claims/" + C3333 + "/comments");
    assertThat(getOther.getStatus()).isEqualTo(404);
    assertThat(getOther.getContentAsString()).isEqualTo(missing.getContentAsString());

    MockHttpServletResponse postOther =
        postCommentRaw(
            amira,
            C3333,
            """
            {"body":"Nope"}
            """);
    assertThat(postOther.getStatus()).isEqualTo(404);
    assertThat(postOther.getContentAsString()).isEqualTo(missing.getContentAsString());

    JsonNode danaList = getComments(dana, C3333);
    assertThat(danaList).isEmpty();
  }

  @Test
  void commentOnMissingClaimIsNotFound() throws Exception {
    assertOneFieldError(getRaw(dana, "/api/claims/" + MISSING + "/comments"), 404);
    assertOneFieldError(
        postCommentRaw(
            dana,
            MISSING,
            """
            {"body":"Hi"}
            """),
        404);
    assertOneFieldError(getRaw(dana, "/api/claims/not-a-uuid/comments"), 404);
    assertOneFieldError(
        postCommentRaw(
            dana,
            "not-a-uuid",
            """
            {"body":"Hi"}
            """),
        404);
  }

  @Test
  void blankCommentBodyIsBadRequest() throws Exception {
    assertOneFieldError(postCommentRaw(dana, C3333, "{}"), 400);
    assertOneFieldError(
        postCommentRaw(
            dana,
            C3333,
            """
            {"body":""}
            """),
        400);
    assertOneFieldError(
        postCommentRaw(
            dana,
            C3333,
            """
            {"body":"   "}
            """),
        400);
    assertThat(getComments(dana, C3333)).isEmpty();
  }

  @Test
  void claimantRepliesToAnInfoRequest() throws Exception {
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

    postComment(
        luis,
        C3333,
        """
        {"body":"Here is the receipt"}
        """);

    JsonNode claim = getJson(luis, "/api/claims/" + C3333);
    assertThat(claim.get("decision").asText()).isEqualTo("INFO_REQUESTED");
    assertThat(claim.get("history")).hasSize(1);
  }

  private JsonNode postComment(String token, String claimId, String json) throws Exception {
    MockHttpServletResponse response = postCommentRaw(token, claimId, json);
    assertThat(response.getStatus()).isEqualTo(201);
    return objectMapper.readTree(response.getContentAsString());
  }

  private MockHttpServletResponse postCommentRaw(String token, String claimId, String json)
      throws Exception {
    return mockMvc
        .perform(
            post("/api/claims/" + claimId + "/comments")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andReturn()
        .getResponse();
  }

  private JsonNode getComments(String token, String claimId) throws Exception {
    MockHttpServletResponse response = getRaw(token, "/api/claims/" + claimId + "/comments");
    assertThat(response.getStatus()).isEqualTo(200);
    return objectMapper.readTree(response.getContentAsString());
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
}
