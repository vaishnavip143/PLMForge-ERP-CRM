package com.plmforge.crm.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignTicketRequest {

    @NotNull(message = "Representative id is required")
    @Positive(message = "Representative id must be a positive number")
    private Long representativeId;
}
