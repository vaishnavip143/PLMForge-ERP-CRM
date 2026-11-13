package com.plmforge.crm.service;

import com.plmforge.crm.dto.response.NotificationResponse;
import com.plmforge.crm.entity.Notification;
import com.plmforge.crm.entity.Ticket;
import com.plmforge.crm.entity.User;
import com.plmforge.crm.enums.NotificationType;
import com.plmforge.crm.enums.Role;
import com.plmforge.crm.exception.ResourceNotFoundException;
import com.plmforge.crm.repository.NotificationRepository;
import com.plmforge.crm.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates one notification per Sales Manager. Duplicate notifications for
     * the same ticket + type + recipient are skipped.
     */
    @Transactional
    public List<Notification> notifyManagers(NotificationType type, String message, Ticket ticket) {
        List<User> managers = userRepository.findByRole(Role.ROLE_SALES_MANAGER);

        if (managers.isEmpty()) {
            log.warn("No SALES_MANAGER user exists; notification [{}] for ticket [{}] was not stored",
                    type, ticket != null ? ticket.getId() : null);
            return List.of();
        }

        List<Notification> created = new ArrayList<>();
        for (User manager : managers) {
            if (ticket != null && ticket.getId() != null
                    && notificationRepository.existsByRecipient_IdAndTypeAndRelatedTicket_Id(
                            manager.getId(), type, ticket.getId())) {
                log.debug("Skipping duplicate [{}] notification for manager {} and ticket {}",
                        type, manager.getId(), ticket.getId());
                continue;
            }
            created.add(save(manager, type, message, ticket));
        }
        return created;
    }

    @Transactional
    public Notification notifyUser(User recipient, NotificationType type, String message, Ticket ticket) {
        return save(recipient, type, message, ticket);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long recipientId) {
        return notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(recipientId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(Long recipientId) {
        return notificationRepository.findByRecipient_IdAndReadFalseOrderByCreatedAtDesc(recipientId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long recipientId) {
        Notification notification = notificationRepository.findByIdAndRecipient_Id(notificationId, recipientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found with id: " + notificationId));

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
            log.info("Notification {} marked as read by recipient {}", notificationId, recipientId);
        }
        return toResponse(notification);
    }

    private Notification save(User recipient, NotificationType type, String message, Ticket ticket) {
        Notification notification = notificationRepository.save(Notification.builder()
                .recipient(recipient)
                .type(type)
                .message(message)
                .relatedTicket(ticket)
                .read(false)
                .build());

        log.info("Notification created: id={}, type={}, recipient={}, ticket={}",
                notification.getId(), type, recipient.getEmail(),
                ticket != null ? ticket.getTicketNumber() : null);
        return notification;
    }

    private NotificationResponse toResponse(Notification notification) {
        Ticket ticket = notification.getRelatedTicket();
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .message(notification.getMessage())
                .ticketId(ticket != null ? ticket.getId() : null)
                .ticketNumber(ticket != null ? ticket.getTicketNumber() : null)
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
