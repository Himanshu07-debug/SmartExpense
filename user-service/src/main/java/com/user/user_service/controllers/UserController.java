package com.user.user_service.controllers;

import com.user.user_service.dtos.UserInfoDto;
import com.user.user_service.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/v1")
public class UserController {

    @Autowired
    private UserService userService;

    // GET: http://localhost:8081/user/v1/profile/{userId}
    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserInfoDto> getUserProfile(@PathVariable Long userId) {
        UserInfoDto response = userService.getUser(userId);
        return ResponseEntity.ok(response);
    }

    // PUT: http://localhost:8081/user/v1/profile/{userId}
    @PutMapping("/profile/{userId}")
    public ResponseEntity<UserInfoDto> updateUserProfile(
            @PathVariable Long userId,
            @RequestBody UserInfoDto requestDto) {
        UserInfoDto response = userService.updateProfile(userId, requestDto);
        return ResponseEntity.ok(response);
    }
}