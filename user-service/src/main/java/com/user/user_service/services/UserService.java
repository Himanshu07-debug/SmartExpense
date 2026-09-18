package com.user.user_service.services;

import com.user.user_service.dtos.UserInfoDto;
import com.user.user_service.entities.UserInfo;
import com.user.user_service.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // 1. Fetch user profile by userId
    public UserInfoDto getUser(Long userId) {
        UserInfo user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        return UserInfoDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    // 2. Update user profile
    public UserInfoDto updateProfile(Long userId, UserInfoDto requestDto) {
        UserInfo user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Profile fields update
        if (requestDto.getFirstName() != null) user.setFirstName(requestDto.getFirstName());
        if (requestDto.getLastName() != null) user.setLastName(requestDto.getLastName());
        if (requestDto.getPhoneNumber() != null) user.setPhoneNumber(requestDto.getPhoneNumber());
        if (requestDto.getEmail() != null) user.setEmail(requestDto.getEmail());

        UserInfo updatedUser = userRepository.save(user);

        return UserInfoDto.builder()
                .userId(updatedUser.getUserId())
                .username(updatedUser.getUsername())
                .firstName(updatedUser.getFirstName())
                .lastName(updatedUser.getLastName())
                .email(updatedUser.getEmail())
                .phoneNumber(updatedUser.getPhoneNumber())
                .build();
    }
}