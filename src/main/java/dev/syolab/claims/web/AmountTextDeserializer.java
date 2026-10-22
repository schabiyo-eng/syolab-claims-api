package dev.syolab.claims.web;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;

public class AmountTextDeserializer extends JsonDeserializer<String> {

  @Override
  public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    JsonToken token = parser.currentToken();
    if (token == JsonToken.VALUE_STRING || token.isNumeric()) {
      return parser.getText();
    }
    throw InvalidFormatException.from(
        parser, "Amount must be a decimal string or number", parser.getText(), String.class);
  }
}
