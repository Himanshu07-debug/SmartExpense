package com.auth.auth_service.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequestDTO {
    @NotBlank(message = "Refresh token cannot be blank")
    private String token;
}