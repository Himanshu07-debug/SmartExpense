package com.auth.auth_service.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequestDTO {
    @NotBlank(message = "Username cannot be blank")
    private String username;

    @NotBlank(message = "Password cannot be blank")
    private String password;

    private String firstName;
    private String lastName;
    private Long phoneNumber;
    private String email;
}