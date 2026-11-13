package com.plmforge.crm.dto.request;

import com.plmforge.crm.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTicketStatusRequest {

    @NotNull(message = "Status is required (OPEN, IN_PROGRESS, RESOLVED, CLOSED)")
    private TicketStatus status;

    @Size(max = 2000, message = "Resolution notes must not exceed 2000 characters")
    private String resolutionNotes;
}
