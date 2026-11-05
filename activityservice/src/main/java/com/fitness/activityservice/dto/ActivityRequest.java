package com.fitness.activityservice.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
@Data
public class ActivityRequest {
    private String userId;
    private String type;   // free text typed by the user, e.g. "Evening jog"
    private Integer duration;
    private LocalDateTime startTime;
    private Map<String , Object> additionalMetrics;

}
