package com.fitness.activityservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

// The message published to RabbitMQ: the saved activity plus the user's biometrics,
// so ai-service does not need to call any other service.
@Data
@Builder
public class ActivityEvent {
    private String id;
    private String userId;
    private String type;
    private Integer duration;
    private LocalDateTime startTime;
    private Map<String, Object> additionalMetrics;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Integer age;
    private Double weight;
    private Double height;
    private String gender;
}
