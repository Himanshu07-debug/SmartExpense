package com.user.user_service.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfo {

    @Id
    @Column(name = "user_id")
    private Long userId; // Yeh ID AuthService se aayegi, auto-increment nahi hogi taaki dono services me same user ID rahe

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone_number")
    private Long phoneNumber;

    @Column(nullable = false)
    private String email;
}