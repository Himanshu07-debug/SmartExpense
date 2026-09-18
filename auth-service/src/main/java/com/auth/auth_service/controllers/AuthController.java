package com.auth.auth_service.controllers;

import com.auth.auth_service.dtos.AuthRequestDTO;
import com.auth.auth_service.dtos.JwtResponseDTO;
import com.auth.auth_service.dtos.RefreshTokenRequestDTO;
import com.auth.auth_service.dtos.UserInfoDto;
import com.auth.auth_service.entities.RefreshToken;
import com.auth.auth_service.entities.UserInfo;
import com.auth.auth_service.producer.UserInfoProducer;
import com.auth.auth_service.repositories.UserRepository;
import com.auth.auth_service.services.JwtService;
import com.auth.auth_service.services.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/auth/v1")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserInfoProducer userInfoProducer;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody AuthRequestDTO request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username already taken!");
        }

        UserInfo user = UserInfo.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of("ROLE_USER"))
                .build();

        UserInfo savedUser = userRepository.save(user);

        // Kafka Event Publish karna UserService ke liye
        UserInfoDto event = UserInfoDto.builder()
                .userId(savedUser.getUserId())
                .username(savedUser.getUsername())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .build();

        userInfoProducer.sendEvent(event);

        return ResponseEntity.ok("User registered and event published successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponseDTO> login(@Valid @RequestBody AuthRequestDTO authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        );

        if (authentication.isAuthenticated()) {
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(authRequest.getUsername());
            String accessToken = jwtService.GenerateToken(authRequest.getUsername());

            // Database se actual user record nikal kar numeric user_id lein
            UserInfo user = userRepository.findByUsername(authRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + authRequest.getUsername()));

            return ResponseEntity.ok(JwtResponseDTO.builder()
                    .accessToken(accessToken)
                    .token(refreshToken.getToken())
                    .userId(user.getUserId()) // <--- User entity ka numeric ID (users table column: user_id)
                    .build());
        } else {
            throw new RuntimeException("Invalid user request");
        }
    }

    @PostMapping("/refreshToken")
    public ResponseEntity<JwtResponseDTO> refreshToken(@RequestBody RefreshTokenRequestDTO request) {
        return refreshTokenService.findByToken(request.getToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUserInfo)
                .map(userInfo -> {
                    String accessToken = jwtService.GenerateToken(userInfo.getUsername());
                    return ResponseEntity.ok(JwtResponseDTO.builder()
                            .accessToken(accessToken)
                            .token(request.getToken())
                            .build());
                }).orElseThrow(() -> new RuntimeException("Refresh Token not found in Database!"));
    }

    // Protected Test Endpoint
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Pong! Authentication Successful.");
    }
}
