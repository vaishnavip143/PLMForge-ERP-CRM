package com.plmforge.crm.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String tokenType;
    private long expiresIn;
    private Long userId;
    private String name;
    private String email;
    private String role;
}
