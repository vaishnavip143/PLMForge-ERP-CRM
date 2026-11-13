package com.plmforge.crm.entity;

import com.plmforge.crm.enums.CustomerType;
import com.plmforge.crm.enums.TicketPriority;
import com.plmforge.crm.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A customer query / support ticket.
 *
 * The table keeps its existing name ("query_tickets") and the due date column
 * ("sla_deadline") so the local PostgreSQL data stays valid. The API exposes
 * the field as "dueAt" as required by the assignment.
 */
@Entity
@Table(name = "query_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 50)
    private String ticketNumber;

    @Column(name = "sender_email", nullable = false, length = 150)
    private String senderEmail;

    @Column(name = "sender_name", length = 150)
    private String senderName;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 50)
    private CustomerType customerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "potential_customer_id")
    private PotentialCustomer potentialCustomer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Column(name = "tat_hours", nullable = false)
    @Builder.Default
    private Integer tatHours = 24;

    @Column(name = "sla_deadline", nullable = false)
    private LocalDateTime dueAt;

    /** True when the "less than 2 hours remaining" SLA warning was raised. */
    @Column(name = "is_near_sla_breach", nullable = false)
    @Builder.Default
    private boolean nearSlaBreach = false;

    /** True when the ticket passed its due date while still active. */
    @Column(name = "is_sla_breached", nullable = false)
    @Builder.Default
    private boolean slaBreached = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (tatHours == null) {
            tatHours = 24;
        }
        if (dueAt == null) {
            dueAt = createdAt.plusHours(tatHours);
        }
        if (status == null) {
            status = TicketStatus.OPEN;
        }
        if (priority == null) {
            priority = TicketPriority.MEDIUM;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
