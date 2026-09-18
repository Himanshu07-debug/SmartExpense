package com.user.user_service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserInfoDto {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("phone_number")
    private Long phoneNumber;

    @JsonProperty("email")
    private String email;
}