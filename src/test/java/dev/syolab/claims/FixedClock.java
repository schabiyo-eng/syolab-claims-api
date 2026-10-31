package dev.syolab.claims;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class FixedClock {

  public static final Instant NOW = Instant.parse("2026-06-10T09:00:00Z");

  @Bean
  @Primary
  Clock fixedClock() {
    return Clock.fixed(NOW, ZoneOffset.UTC);
  }
}
