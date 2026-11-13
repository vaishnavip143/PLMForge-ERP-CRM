package com.plmforge.crm.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketIngestResponse {
    private TicketResponse ticket;
    private PotentialCustomerResponse potentialCustomer;
    private boolean notificationCreated;
    private String notificationMessage;
    private String message;
}
