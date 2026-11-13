package com.plmforge.crm.dto.response;

import com.plmforge.crm.entity.PotentialCustomer;
import com.plmforge.crm.enums.LeadStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PotentialCustomerResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String source;
    private LeadStatus status;
    private LocalDateTime createdAt;

    public static PotentialCustomerResponse from(PotentialCustomer lead) {
        return PotentialCustomerResponse.builder()
                .id(lead.getId())
                .name(lead.getName())
                .email(lead.getEmail())
                .phone(lead.getPhone())
                .source(lead.getSource())
                .status(lead.getStatus())
                .createdAt(lead.getCreatedAt())
                .build();
    }
}
