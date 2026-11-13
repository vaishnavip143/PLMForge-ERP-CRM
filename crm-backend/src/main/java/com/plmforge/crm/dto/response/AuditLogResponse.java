package com.plmforge.crm.dto.response;

import com.plmforge.crm.entity.AuditLog;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private Long id;
    private Long ticketId;
    private String ticketNumber;
    private String action;
    private String performedBy;
    private String details;
    private LocalDateTime createdAt;

    public static AuditLogResponse from(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .ticketId(log.getTicketId())
                .ticketNumber(log.getTicketNumber())
                .action(log.getAction())
                .performedBy(log.getPerformedBy())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
