package com.user.user_service.consumer;

import com.user.user_service.dtos.UserInfoDto;
import com.user.user_service.entities.UserInfo;
import com.user.user_service.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class UserConsumer {

    @Autowired
    private UserRepository userRepository;

    @KafkaListener(topics = "USER_EVENTS", groupId = "user-service-group-v2")
    public void consumeUserEvent(UserInfoDto event) {
        System.out.println("===> [UserService] Received Kafka Event for User: " + event.getUsername());

        UserInfo user = UserInfo.builder()
                .userId(event.getUserId())
                .username(event.getUsername())
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .email(event.getEmail())
                .phoneNumber(event.getPhoneNumber())
                .build();

        userRepository.save(user);
        System.out.println("===> [UserService] Successfully saved user profile to user_service_db!");
    }
}