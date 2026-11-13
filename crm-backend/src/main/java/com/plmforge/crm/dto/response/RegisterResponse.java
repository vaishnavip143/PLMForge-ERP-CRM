package com.plmforge.crm.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponse {
    private Long userId;
    private String name;
    private String email;
    private String role;
    private LocalDateTime createdAt;
}
