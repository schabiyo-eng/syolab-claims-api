package dev.syolab.claims;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OpenApiContractTest {

  @Test
  void committedOpenApiDeclaresRequiredOperationIds() throws Exception {
    String contract = Files.readString(Path.of("openapi/claims-api.yaml"));

    assertThat(contract).contains("operationId: listPolicies");
    assertThat(contract).contains("operationId: getPolicy");
    assertThat(contract).contains("operationId: listClaims");
    assertThat(contract).contains("operationId: createClaim");
    assertThat(contract).contains("operationId: getClaim");
    assertThat(contract).contains("operationId: login");
    assertThat(contract).contains("operationId: getCurrentUser");
  }
}
