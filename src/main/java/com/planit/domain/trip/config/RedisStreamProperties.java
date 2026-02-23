package com.planit.domain.trip.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.redis.stream")
public class RedisStreamProperties {
    private String aiJobsKey = "stream:ai-jobs";
    private String aiResultsKey = "stream:itinerary-results";
    private String aiJobsGroup = "ai-workers";
}
