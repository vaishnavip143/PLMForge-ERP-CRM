package com.plmforge.crm.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentUserResponse {
    private Long userId;
    private String name;
    private String email;
    private String role;
    private boolean authenticated;
}
