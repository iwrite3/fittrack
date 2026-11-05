package com.fitness.activityservice.dto;

import lombok.Data;

// The part of the user-service response that activity-service needs.
@Data
public class UserProfile {
    private Integer age;
    private Double weight;   // kg
    private Double height;   // cm
    private String gender;
}
