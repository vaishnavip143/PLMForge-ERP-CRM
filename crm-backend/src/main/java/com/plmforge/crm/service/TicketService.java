package com.plmforge.crm.service;

import com.plmforge.crm.dto.request.TicketIngestRequest;
import com.plmforge.crm.dto.request.UpdateTicketStatusRequest;
import com.plmforge.crm.dto.response.PotentialCustomerResponse;
import com.plmforge.crm.dto.response.TicketIngestResponse;
import com.plmforge.crm.dto.response.TicketResponse;
import com.plmforge.crm.entity.Customer;
import com.plmforge.crm.entity.Notification;
import com.plmforge.crm.entity.PotentialCustomer;
import com.plmforge.crm.entity.Ticket;
import com.plmforge.crm.entity.User;
import com.plmforge.crm.enums.CustomerType;
import com.plmforge.crm.enums.LeadStatus;
import com.plmforge.crm.enums.NotificationType;
import com.plmforge.crm.enums.Role;
import com.plmforge.crm.enums.TicketPriority;
import com.plmforge.crm.enums.TicketStatus;
import com.plmforge.crm.exception.BadRequestException;
import com.plmforge.crm.exception.ResourceNotFoundException;
import com.plmforge.crm.repository.CustomerRepository;
import com.plmforge.crm.repository.PotentialCustomerRepository;
import com.plmforge.crm.repository.TicketRepository;
import com.plmforge.crm.repository.UserRepository;
import com.plmforge.crm.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    // 24h TAT for every query - this is what the business agreed on, not a tuning knob.
    public static final int DEFAULT_TAT_HOURS = 24;

    // Less than two hours left on an active ticket counts as an SLA warning.
    public static final long SLA_WARNING_THRESHOLD_MINUTES = 120;

    private static final DateTimeFormatter TICKET_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TicketRepository ticketRepository;
    private final CustomerRepository customerRepository;
    private final PotentialCustomerRepository potentialCustomerRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public TicketService(TicketRepository ticketRepository,
                         CustomerRepository customerRepository,
                         PotentialCustomerRepository potentialCustomerRepository,
                         UserRepository userRepository,
                         NotificationService notificationService,
                         AuditService auditService) {
        this.ticketRepository = ticketRepository;
        this.customerRepository = customerRepository;
        this.potentialCustomerRepository = potentialCustomerRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public TicketIngestResponse ingest(TicketIngestRequest request, String performedBy) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String name = request.getName().trim();
        String phone = trimToNull(request.getPhone());
        var now = LocalDateTime.now();

        log.info("query came in from {} (customerId={}, subject=\"{}\")",
                email, request.getCustomerId(), request.getSubject());

        Customer knownCustomer = resolveExistingCustomer(request.getCustomerId(), email);

        if (knownCustomer != null) {
            log.info("{} is an existing customer (#{})", email, knownCustomer.getId());

            Ticket ticket = buildTicket(request, email, name, knownCustomer, null, now);
            ticket = ticketRepository.save(ticket);

            auditService.record(ticket.getId(), ticket.getTicketNumber(), "QUERY_INGESTED_EXISTING_CUSTOMER",
                    performedBy,
                    "Query linked to existing customer #" + knownCustomer.getId()
                            + ". 24h TAT assigned. Due at " + ticket.getDueAt());

            log.info("ticket {} saved for customer #{}, due {}",
                    ticket.getTicketNumber(), knownCustomer.getId(), ticket.getDueAt());

            return TicketIngestResponse.builder()
                    .ticket(mapToResponse(ticket))
                    .potentialCustomer(null)
                    .notificationCreated(false)
                    .notificationMessage(null)
                    .message("Query registered for existing customer. 24-hour TAT applied.")
                    .build();
        }

        // Unknown sender, so it becomes a lead. Same email again -> reuse the old lead row.
        PotentialCustomer lead = potentialCustomerRepository.findByEmailIgnoreCase(email).orElse(null);
        boolean isNewLead = false;

        if (lead == null) {
            lead = potentialCustomerRepository.save(PotentialCustomer.builder()
                    .name(name)
                    .email(email)
                    .phone(phone)
                    .source("INCOMING_QUERY")
                    .status(LeadStatus.NEW_LEAD)
                    .build());
            isNewLead = true;
            log.info("new lead captured: #{} {}", lead.getId(), email);
        } else {
            log.info("reusing lead #{} for {}", lead.getId(), email);
            if (lead.getPhone() == null && phone != null) {
                lead.setPhone(phone);
                potentialCustomerRepository.save(lead);
            }
        }

        Ticket ticket = buildTicket(request, email, name, null, lead, now);
        ticket = ticketRepository.save(ticket);

        String notificationMessage = String.format(
                "New lead query from %s <%s>. Potential customer #%d, ticket #%s, due at %s.",
                name, email, lead.getId(), ticket.getTicketNumber(), ticket.getDueAt());

        List<Notification> notifications =
                notificationService.notifyManagers(NotificationType.NEW_LEAD, notificationMessage, ticket);

        auditService.record(ticket.getId(), ticket.getTicketNumber(), "QUERY_INGESTED_POTENTIAL_CUSTOMER",
                performedBy,
                (isNewLead ? "New potential customer #" + lead.getId() + " created. "
                        : "Existing potential customer #" + lead.getId() + " reused. ")
                        + notifications.size() + " manager notification(s) created. 24h TAT assigned.");

        log.info("lead ticket {} created, {} manager(s) notified",
                ticket.getTicketNumber(), notifications.size());

        return TicketIngestResponse.builder()
                .ticket(mapToResponse(ticket))
                .potentialCustomer(PotentialCustomerResponse.from(lead))
                .notificationCreated(!notifications.isEmpty())
                .notificationMessage(notifications.isEmpty() ? null : notificationMessage)
                .message(isNewLead
                        ? "Unknown sender: potential customer created, ticket registered and Sales Manager notified."
                        : "Existing potential customer reused, new ticket registered and Sales Manager notified.")
                .build();
    }

    private Customer resolveExistingCustomer(Long customerId, String email) {
        if (customerId != null) {
            Customer byId = customerRepository.findById(customerId).orElse(null);
            if (byId != null) {
                return byId;
            }
            log.warn("customerId={} does not exist, falling back to the email lookup", customerId);
        }
        return customerRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    private Ticket buildTicket(TicketIngestRequest request,
                               String email,
                               String name,
                               Customer customer,
                               PotentialCustomer lead,
                               LocalDateTime now) {
        return Ticket.builder()
                .ticketNumber(generateUniqueTicketNumber())
                .senderEmail(email)
                .senderName(name)
                .subject(request.getSubject().trim())
                .description(request.getDescription().trim())
                .customerType(customer != null ? CustomerType.EXISTING_CUSTOMER : CustomerType.POTENTIAL_CUSTOMER)
                .customer(customer)
                .potentialCustomer(lead)
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.MEDIUM)
                .tatHours(DEFAULT_TAT_HOURS)
                .dueAt(now.plusHours(DEFAULT_TAT_HOURS))
                .nearSlaBreach(false)
                .slaBreached(false)
                .createdAt(now)
                .build();
    }

    private String generateUniqueTicketNumber() {
        String date = LocalDateTime.now().format(TICKET_DATE_FORMAT);
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = "TICK-" + date + "-" + (100000 + RANDOM.nextInt(900000));
            if (!ticketRepository.existsByTicketNumber(candidate)) {
                return candidate;
            }
        }
        return "TICK-" + date + "-" + System.nanoTime();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getMyTickets(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        var byId = new LinkedHashMap<Long, Ticket>();

        customerRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(customer ->
                ticketRepository.findByCustomer_IdOrderByCreatedAtDesc(customer.getId())
                        .forEach(ticket -> byId.put(ticket.getId(), ticket)));

        ticketRepository.findByPotentialCustomer_EmailIgnoreCaseOrderByCreatedAtDesc(normalizedEmail)
                .forEach(ticket -> byId.put(ticket.getId(), ticket));

        return byId.values().stream()
                .sorted(Comparator.comparing(Ticket::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketForUser(Long id, UserPrincipal principal) {
        Ticket ticket = findTicket(id);

        switch (principal.getRole()) {
            case ROLE_CUSTOMER -> {
                if (!isOwnedByCustomer(ticket, principal.getUsername())) {
                    log.warn("Customer {} attempted to access ticket {} owned by another customer",
                            principal.getUsername(), id);
                    throw new AccessDeniedException("That ticket belongs to another customer.");
                }
            }
            case ROLE_SALES_REPRESENTATIVE -> {
                if (ticket.getAssignedTo() == null
                        || !ticket.getAssignedTo().getId().equals(principal.getId())) {
                    log.warn("Representative {} attempted to access ticket {} assigned to someone else",
                            principal.getUsername(), id);
                    throw new AccessDeniedException("That ticket isn't assigned to you.");
                }
            }
            case ROLE_SALES_MANAGER -> {
                // managers see everything, no check needed
            }
        }

        return mapToResponse(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getAssignedTickets(Long representativeId) {
        return ticketRepository.findByAssignedTo_IdOrderByCreatedAtDesc(representativeId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Nothing calls this any more; the manager alert tables come from SlaService.
    // Left in place until someone confirms no report depends on it.
    @Transactional(readOnly = true)
    public List<TicketResponse> getActiveTickets() {
        return ticketRepository.findByStatusInOrderByDueAtAsc(
                        List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS))
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public TicketResponse updateStatus(Long id, UpdateTicketStatusRequest request, UserPrincipal principal) {
        Ticket ticket = findTicket(id);

        if (principal.getRole() == Role.ROLE_CUSTOMER) {
            throw new AccessDeniedException("Customers can't change ticket status.");
        }
        if (principal.getRole() == Role.ROLE_SALES_REPRESENTATIVE
                && (ticket.getAssignedTo() == null
                || !ticket.getAssignedTo().getId().equals(principal.getId()))) {
            throw new AccessDeniedException("That ticket isn't assigned to you.");
        }

        TicketStatus oldStatus = ticket.getStatus();
        TicketStatus newStatus = request.getStatus();

        ticket.setStatus(newStatus);
        if (request.getResolutionNotes() != null && !request.getResolutionNotes().isBlank()) {
            ticket.setResolutionNotes(request.getResolutionNotes().trim());
        }

        if (newStatus.isFinalStatus()) {
            if (ticket.getResolvedAt() == null) {
                ticket.setResolvedAt(LocalDateTime.now());
            }
        } else {
            ticket.setResolvedAt(null);
        }

        ticket = ticketRepository.save(ticket);

        log.info("{} moved {} from {} to {}",
                principal.getUsername(), ticket.getTicketNumber(), oldStatus, newStatus);

        auditService.record(ticket.getId(), ticket.getTicketNumber(), "TICKET_STATUS_CHANGED",
                principal.getUsername(),
                "Status changed from " + oldStatus + " to " + newStatus
                        + (ticket.getResolutionNotes() != null ? ". Notes: " + ticket.getResolutionNotes() : ""));

        return mapToResponse(ticket);
    }

    @Transactional
    public TicketResponse assignTicket(Long id, Long representativeId, String performedBy) {
        Ticket ticket = findTicket(id);

        User representative = userRepository.findById(representativeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No sales representative with id " + representativeId));

        if (representative.getRole() != Role.ROLE_SALES_REPRESENTATIVE) {
            throw new BadRequestException("That user isn't a sales representative.");
        }

        ticket.setAssignedTo(representative);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket = ticketRepository.save(ticket);

        log.info("ticket {} handed to {} (by {})",
                ticket.getTicketNumber(), representative.getEmail(), performedBy);

        auditService.record(ticket.getId(), ticket.getTicketNumber(), "TICKET_ASSIGNED", performedBy,
                "Assigned to " + representative.getName() + " <" + representative.getEmail() + ">");

        notificationService.notifyUser(representative, NotificationType.TICKET_ASSIGNED,
                "Ticket #" + ticket.getTicketNumber() + " was assigned to you by " + performedBy + ".",
                ticket);

        return mapToResponse(ticket);
    }

    public TicketResponse mapToResponse(Ticket ticket) {
        LocalDateTime now = LocalDateTime.now();
        Long remainingMinutes = null;
        String remainingTime;
        boolean slaAlert = false;
        boolean overdue = false;

        if (ticket.getStatus() != null && ticket.getStatus().isFinalStatus()) {
            remainingMinutes = 0L;
            remainingTime = "Resolved";
        } else if (ticket.getDueAt() == null) {
            remainingTime = "N/A";
        } else {
            remainingMinutes = Duration.between(now, ticket.getDueAt()).toMinutes();
            if (remainingMinutes <= 0) {
                overdue = true;
                remainingTime = "Overdue by " + formatDuration(Math.abs(remainingMinutes));
            } else {
                slaAlert = remainingMinutes < SLA_WARNING_THRESHOLD_MINUTES;
                remainingTime = formatDuration(remainingMinutes) + " remaining";
            }
        }

        Customer customer = ticket.getCustomer();
        PotentialCustomer lead = ticket.getPotentialCustomer();
        User assignedTo = ticket.getAssignedTo();

        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .customerType(ticket.getCustomerType())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .customerId(customer != null ? customer.getId() : null)
                .customerName(customer != null ? customer.getName() : null)
                .customerEmail(customer != null ? customer.getEmail() : null)
                .potentialCustomerId(lead != null ? lead.getId() : null)
                .potentialCustomerName(lead != null ? lead.getName() : null)
                .potentialCustomerEmail(lead != null ? lead.getEmail() : null)
                .potentialCustomerPhone(lead != null ? lead.getPhone() : null)
                .assignedToId(assignedTo != null ? assignedTo.getId() : null)
                .assignedToName(assignedTo != null ? assignedTo.getName() : null)
                .assignedToEmail(assignedTo != null ? assignedTo.getEmail() : null)
                .tatHours(ticket.getTatHours())
                .createdAt(ticket.getCreatedAt())
                .dueAt(ticket.getDueAt())
                .remainingMinutes(remainingMinutes)
                .remainingTime(remainingTime)
                .slaAlert(slaAlert)
                .overdue(overdue)
                .resolutionNotes(ticket.getResolutionNotes())
                .resolvedAt(ticket.getResolvedAt())
                .build();
    }

    private Ticket findTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No ticket with id " + id));
    }

    private boolean isOwnedByCustomer(Ticket ticket, String email) {
        if (ticket.getCustomer() != null
                && ticket.getCustomer().getEmail().equalsIgnoreCase(email)) {
            return true;
        }
        return ticket.getPotentialCustomer() != null
                && ticket.getPotentialCustomer().getEmail().equalsIgnoreCase(email);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String formatDuration(long totalMinutes) {
        return (totalMinutes / 60) + "h " + (totalMinutes % 60) + "m";
    }
}
