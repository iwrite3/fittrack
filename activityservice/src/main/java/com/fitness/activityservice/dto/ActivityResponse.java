package com.fitness.activityservice.dto;

import lombok.Data;


import java.time.LocalDateTime;
import java.util.Map;
@Data
public class ActivityResponse {

    private String id;
    private String userId;
    private String type;
    private Integer duration;
    private LocalDateTime startTime;

    private Map<String , Object> additionalMetrics;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
