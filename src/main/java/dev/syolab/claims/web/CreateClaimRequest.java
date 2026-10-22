package dev.syolab.claims.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDate;
import java.util.List;

public final class CreateClaimRequest {

  private final String policyNumber;
  private final String claimantName;
  private final String amount;
  private final LocalDate incidentDate;
  private final String description;
  private final List<String> documentNames;

  @JsonCreator
  public CreateClaimRequest(
      @JsonProperty("policyNumber") String policyNumber,
      @JsonProperty("claimantName") String claimantName,
      @JsonProperty("amount") @JsonDeserialize(using = AmountTextDeserializer.class) String amount,
      @JsonProperty("incidentDate") LocalDate incidentDate,
      @JsonProperty("description") String description,
      @JsonProperty("documentNames") List<String> documentNames) {
    this.policyNumber = policyNumber;
    this.claimantName = claimantName;
    this.amount = amount;
    this.incidentDate = incidentDate;
    this.description = description;
    this.documentNames = documentNames == null ? null : List.copyOf(documentNames);
  }

  public String policyNumber() {
    return policyNumber;
  }

  public String claimantName() {
    return claimantName;
  }

  public String amount() {
    return amount;
  }

  public LocalDate incidentDate() {
    return incidentDate;
  }

  public String description() {
    return description;
  }

  public List<String> documentNames() {
    return documentNames;
  }
}
