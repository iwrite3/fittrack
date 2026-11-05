package com.fitness.activityservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ActivityPublisher {
    private final Publisher publisher;
    private final ObjectMapper mapper = new ObjectMapper();

    public ActivityPublisher(@Value("${GCP_PROJECT}") String project,
                             @Value("${PUBSUB_TOPIC:activity-events}") String topic) throws Exception {
        this.publisher = Publisher.newBuilder(TopicName.of(project, topic)).build();
    }

    public void publish(Object event) throws Exception {
        PubsubMessage msg = PubsubMessage.newBuilder()
                .setData(ByteString.copyFrom(mapper.writeValueAsBytes(event))).build();
        publisher.publish(msg).get();   // wait for server ack so failures surface
    }
}
