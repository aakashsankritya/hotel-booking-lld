package com.rupeek.hotel.application.config;

import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings under {@code booking.*} in application.yml. */
@ConfigurationProperties("booking")
public record BookingProperties(Duration holdTtl, Duration sweepInterval, ZoneId zone) {
}
