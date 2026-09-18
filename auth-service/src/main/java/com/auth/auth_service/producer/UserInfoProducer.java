package com.auth.auth_service.producer;

import com.auth.auth_service.dtos.UserInfoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserInfoProducer {

    private static final String TOPIC = "USER_EVENTS";

    @Autowired
    private KafkaTemplate<String, UserInfoDto> kafkaTemplate;

    public void sendEvent(UserInfoDto event) {
        kafkaTemplate.send(TOPIC, event.getUsername(), event);
        System.out.println("--> [AuthService] Kafka Event Published for: " + event.getUsername());
    }
}
