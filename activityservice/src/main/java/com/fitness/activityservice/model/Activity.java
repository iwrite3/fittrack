package com.fitness.activityservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Builder;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.util.Map;
import java.time.LocalDateTime;
@Document(collection = "activities")
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class Activity {
    @Id
    private String id;
    private String userId;
    private String type;   // free-text activity name
    private Integer duration;
    private LocalDateTime startTime;
    @Field("metrics")
    private Map<String , Object> additionalMetrics;
    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
