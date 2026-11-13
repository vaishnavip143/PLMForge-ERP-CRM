package com.plmforge.crm.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaAlertsResponse {
    private List<TicketResponse> warningTickets;
    private List<TicketResponse> overdueTickets;
    private int warningCount;
    private int overdueCount;
    private LocalDateTime generatedAt;
}
