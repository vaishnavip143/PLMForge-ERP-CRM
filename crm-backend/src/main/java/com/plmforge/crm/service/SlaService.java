package com.plmforge.crm.service;

import com.plmforge.crm.dto.response.SlaAlertsResponse;
import com.plmforge.crm.dto.response.TicketResponse;
import com.plmforge.crm.entity.Ticket;
import com.plmforge.crm.enums.NotificationType;
import com.plmforge.crm.enums.TicketStatus;
import com.plmforge.crm.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SLA monitoring.
 *
 * Rule: an active ticket (OPEN / IN_PROGRESS) with more than 0 and less than
 * 2 hours remaining is an SLA warning. Tickets past their due date are overdue.
 * Resolved and closed tickets are never flagged.
 */
@Service
public class SlaService {

    private static final Logger log = LoggerFactory.getLogger(SlaService.class);

    private static final List<TicketStatus> ACTIVE_STATUSES =
            List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);

    private final TicketRepository ticketRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final TicketService ticketService;

    public SlaService(TicketRepository ticketRepository,
                      NotificationService notificationService,
                      AuditService auditService,
                      TicketService ticketService) {
        this.ticketRepository = ticketRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.ticketService = ticketService;
    }

    @Scheduled(fixedDelayString = "${app.sla.scan-rate-ms:60000}",
            initialDelayString = "${app.sla.scan-rate-ms:60000}")
    public void scheduledSlaScan() {
        try {
            SlaScanResult result = scanTickets();
            if (result.warningsRaised() + result.breachesRaised() > 0) {
                log.info("SLA scan completed: {} warning(s), {} breach(es)",
                        result.warningsRaised(), result.breachesRaised());
            }
        } catch (Exception exception) {
            log.error("SLA scan failed", exception);
        }
    }

    /**
     * Scans active tickets, flags SLA warnings / breaches and notifies managers.
     * Also callable from tests and from the manager scan endpoint.
     */
    @Transactional
    public SlaScanResult scanTickets() {
        List<Ticket> activeTickets = ticketRepository.findByStatusInOrderByDueAtAsc(ACTIVE_STATUSES);
        LocalDateTime now = LocalDateTime.now();
        int warningsRaised = 0;
        int breachesRaised = 0;

        for (Ticket ticket : activeTickets) {
            // Defensive guard: resolved and closed tickets are never SLA-flagged.
            if (ticket.getStatus() == null || ticket.getStatus().isFinalStatus()
                    || ticket.getDueAt() == null) {
                continue;
            }

            long remainingMinutes = Duration.between(now, ticket.getDueAt()).toMinutes();

            if (remainingMinutes <= 0) {
                if (!ticket.isSlaBreached()) {
                    ticket.setSlaBreached(true);
                    ticket.setNearSlaBreach(true);
                    ticketRepository.save(ticket);
                    breachesRaised++;

                    log.warn("SLA BREACH: ticket={}, customer={}, overdueByMinutes={}",
                            ticket.getTicketNumber(), ticket.getSenderEmail(), Math.abs(remainingMinutes));

                    notificationService.notifyManagers(NotificationType.SLA_BREACH,
                            "Ticket #" + ticket.getTicketNumber() + " breached its 24-hour SLA ("
                                    + ticket.getSenderEmail() + "). Overdue by "
                                    + Math.abs(remainingMinutes) + " minutes.",
                            ticket);

                    auditService.record(ticket.getId(), ticket.getTicketNumber(), "SLA_BREACH_FLAGGED",
                            "SYSTEM_SLA_MONITOR",
                            "Ticket is overdue by " + Math.abs(remainingMinutes) + " minutes.");
                }
            } else if (remainingMinutes < TicketService.SLA_WARNING_THRESHOLD_MINUTES) {
                if (!ticket.isNearSlaBreach()) {
                    ticket.setNearSlaBreach(true);
                    ticketRepository.save(ticket);
                    warningsRaised++;

                    log.warn("SLA WARNING: ticket={}, customer={}, remainingMinutes={}",
                            ticket.getTicketNumber(), ticket.getSenderEmail(), remainingMinutes);

                    notificationService.notifyManagers(NotificationType.SLA_WARNING,
                            "Ticket #" + ticket.getTicketNumber() + " has less than 2 hours remaining ("
                                    + remainingMinutes + " minutes) before its SLA deadline.",
                            ticket);

                    auditService.record(ticket.getId(), ticket.getTicketNumber(), "SLA_WARNING_FLAGGED",
                            "SYSTEM_SLA_MONITOR",
                            "Less than 2 hours remaining (" + remainingMinutes + " minutes).");
                }
            }
        }

        return new SlaScanResult(warningsRaised, breachesRaised, warningsRaised + breachesRaised);
    }

    @Transactional(readOnly = true)
    public SlaAlertsResponse getSlaAlerts() {
        LocalDateTime now = LocalDateTime.now();
        List<Ticket> activeTickets = ticketRepository.findByStatusInOrderByDueAtAsc(ACTIVE_STATUSES);

        List<TicketResponse> warnings = activeTickets.stream()
                .filter(ticket -> ticket.getDueAt() != null)
                .filter(ticket -> {
                    long minutes = Duration.between(now, ticket.getDueAt()).toMinutes();
                    return minutes > 0 && minutes < TicketService.SLA_WARNING_THRESHOLD_MINUTES;
                })
                .map(ticketService::mapToResponse)
                .toList();

        List<TicketResponse> overdue = activeTickets.stream()
                .filter(ticket -> ticket.getDueAt() != null)
                .filter(ticket -> Duration.between(now, ticket.getDueAt()).toMinutes() <= 0)
                .map(ticketService::mapToResponse)
                .toList();

        return SlaAlertsResponse.builder()
                .warningTickets(warnings)
                .overdueTickets(overdue)
                .warningCount(warnings.size())
                .overdueCount(overdue.size())
                .generatedAt(now)
                .build();
    }

    public record SlaScanResult(int warningsRaised, int breachesRaised, int totalFlagged) {
    }
}
