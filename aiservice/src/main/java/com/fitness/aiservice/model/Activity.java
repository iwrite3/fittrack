package com.fitness.aiservice.model;


import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class Activity {

    private String id;
    private String userId;
private String type;
    private Integer duration;
    // User biometrics sent by activity-service in the event
    private Integer age;
    private Double weight;   // kg
    private Double height;   // cm
    private String gender;
    private LocalDateTime startTime;

    private Map<String , Object> additionalMetrics;
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
