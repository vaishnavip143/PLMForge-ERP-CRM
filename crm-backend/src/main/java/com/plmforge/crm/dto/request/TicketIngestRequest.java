package com.plmforge.crm.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketIngestRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    /** Optional: when provided, the backend first tries to resolve the existing customer by id. */
    @Positive(message = "Customer id must be a positive number")
    private Long customerId;

    @NotBlank(message = "Subject is required")
    @Size(max = 255, message = "Subject must not exceed 255 characters")
    private String subject;

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    /** Optional contact number. */
    @Pattern(regexp = "^[0-9+()\\- ]{7,20}$", message = "Phone must contain 7 to 20 digits or phone symbols")
    private String phone;
}
