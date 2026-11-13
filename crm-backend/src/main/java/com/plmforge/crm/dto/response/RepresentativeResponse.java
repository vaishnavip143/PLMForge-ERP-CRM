package com.plmforge.crm.dto.response;

import com.plmforge.crm.entity.User;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepresentativeResponse {
    private Long id;
    private String name;
    private String email;

    public static RepresentativeResponse from(User user) {
        return RepresentativeResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
