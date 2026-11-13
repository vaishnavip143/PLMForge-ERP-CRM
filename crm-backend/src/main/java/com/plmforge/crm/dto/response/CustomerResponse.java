package com.plmforge.crm.dto.response;

import com.plmforge.crm.entity.Customer;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String companyName;
    private LocalDateTime createdAt;

    public static CustomerResponse from(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .companyName(customer.getCompanyName())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}
