package com.planit.domain.trip.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.itinerary")
public class ItineraryJobProperties {
    private long jobTtlSeconds = 86400;
    private String consumerGroup = "travel-service";
    private String consumerName = "local";
    private boolean streamEnabled = true;
}
