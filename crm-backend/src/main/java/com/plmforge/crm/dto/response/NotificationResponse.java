package com.plmforge.crm.dto.response;

import com.plmforge.crm.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String message;
    private Long ticketId;
    private String ticketNumber;
    private boolean read;
    private LocalDateTime createdAt;
}
