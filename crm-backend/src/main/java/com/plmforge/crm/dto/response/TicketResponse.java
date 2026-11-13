package com.plmforge.crm.dto.response;

import com.plmforge.crm.enums.CustomerType;
import com.plmforge.crm.enums.TicketPriority;
import com.plmforge.crm.enums.TicketStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {
    private Long id;
    private String ticketNumber;
    private String subject;
    private String description;
    private CustomerType customerType;
    private TicketStatus status;
    private TicketPriority priority;

    private Long customerId;
    private String customerName;
    private String customerEmail;

    private Long potentialCustomerId;
    private String potentialCustomerName;
    private String potentialCustomerEmail;
    private String potentialCustomerPhone;

    private Long assignedToId;
    private String assignedToName;
    private String assignedToEmail;

    private Integer tatHours;
    private LocalDateTime createdAt;
    private LocalDateTime dueAt;
    private Long remainingMinutes;
    private String remainingTime;
    private boolean slaAlert;
    private boolean overdue;

    private String resolutionNotes;
    private LocalDateTime resolvedAt;
}
