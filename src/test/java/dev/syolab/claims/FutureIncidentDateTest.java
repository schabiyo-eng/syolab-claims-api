package dev.syolab.claims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.syolab.claims.seed.SeedData;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FutureIncidentDateTest {

  @TestConfiguration
  static class FixedClockConfig {

    @Bean
    @Primary
    Clock fixedClock() {
      return Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC);
    }
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SeedData seedData;

  @BeforeEach
  void resetSeed() {
    seedData.reset();
  }

  @Test
  void flagsFutureIncidentDate() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/claims")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
{"policyNumber":"POL-1002","claimantName":"Luis Ortega","amount":"100.00","incidentDate":"2026-06-02","description":"Follow up visit","documentNames":["medical-bill"]}
"""))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode claim = objectMapper.readTree(body);

    assertThat(claim.get("status").asText()).isEqualTo("FLAGGED");
    assertThat(claim.get("findings")).hasSize(1);
    assertThat(claim.get("findings").get(0).get("code").asText()).isEqualTo("POSSIBLE_FRAUD");
    assertThat(claim.get("findings").get(0).get("severity").asText()).isEqualTo("WARNING");
  }
}
