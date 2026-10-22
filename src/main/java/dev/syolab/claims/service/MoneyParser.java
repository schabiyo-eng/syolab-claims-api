package dev.syolab.claims.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

public final class MoneyParser {

  private static final Pattern DECIMAL = Pattern.compile("\\d+(\\.\\d+)?");

  private MoneyParser() {}

  public static BigDecimal parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new BadRequestException("Amount is required.");
    }
    String text = raw.trim();
    if (!DECIMAL.matcher(text).matches()) {
      throw new BadRequestException("Amount must be a decimal number.");
    }
    BigDecimal amount = new BigDecimal(text);
    if (amount.scale() > 2) {
      throw new BadRequestException("Amount must have at most two fraction digits.");
    }
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BadRequestException("Amount must be greater than zero.");
    }
    return amount.setScale(2, RoundingMode.UNNECESSARY);
  }

  public static String format(BigDecimal amount) {
    return amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
  }
}
