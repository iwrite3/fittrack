package com.fitness.aiservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.service.ActivityMessageListener; // <- or call your existing processing service
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Base64;
import java.util.Map;

/** Pub/Sub push endpoint. 2xx = ack; any exception -> 5xx -> Pub/Sub retries, then dead-letters. */
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ActivityMessageListener processor; // adapt: whatever method currently handles one ActivityEvent

    public TaskController(ActivityMessageListener processor) { this.processor = processor; }

    @PostMapping("/process")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Void> process(@RequestBody Map<String, Object> envelope) throws Exception {
        Map<String, Object> message = (Map<String, Object>) envelope.get("message");
        byte[] json = Base64.getDecoder().decode((String) message.get("data"));
        Activity activity = mapper.readValue(json, Activity.class);
        processor.processActivity(activity);   // must finish before Cloud Run request timeout
        return ResponseEntity.noContent().build();
    }
}
