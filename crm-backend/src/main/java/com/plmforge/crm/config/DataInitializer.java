package com.plmforge.crm.config;

import com.plmforge.crm.entity.Customer;
import com.plmforge.crm.entity.PotentialCustomer;
import com.plmforge.crm.entity.Ticket;
import com.plmforge.crm.entity.User;
import com.plmforge.crm.enums.*;
import com.plmforge.crm.repository.CustomerRepository;
import com.plmforge.crm.repository.PotentialCustomerRepository;
import com.plmforge.crm.repository.TicketRepository;
import com.plmforge.crm.repository.UserRepository;
import com.plmforge.crm.service.AuditService;
import com.plmforge.crm.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Creates development-only seed data when the database has no users.
 * Existing PostgreSQL data is never modified or duplicated.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String DEV_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PotentialCustomerRepository potentialCustomerRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public DataInitializer(UserRepository userRepository,
                           CustomerRepository customerRepository,
                           PotentialCustomerRepository potentialCustomerRepository,
                           TicketRepository ticketRepository,
                           PasswordEncoder passwordEncoder,
                           NotificationService notificationService,
                           AuditService auditService) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.potentialCustomerRepository = potentialCustomerRepository;
        this.ticketRepository = ticketRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already contains users. Seed data was not inserted.");
            return;
        }

        log.info("Empty database detected. Seeding development data...");

        String encodedPassword = passwordEncoder.encode(DEV_PASSWORD);

        User representative = userRepository.save(User.builder()
                .name("Alex Turner")
                .email("salesrep@erp.com")
                .password(encodedPassword)
                .role(Role.ROLE_SALES_REPRESENTATIVE)
                .build());

        userRepository.save(User.builder()
                .name("Sarah Connor")
                .email("manager@erp.com")
                .password(encodedPassword)
                .role(Role.ROLE_SALES_MANAGER)
                .build());

        userRepository.save(User.builder()
                .name("John Doe")
                .email("customer@erp.com")
                .password(encodedPassword)
                .role(Role.ROLE_CUSTOMER)
                .build());

        Customer john = customerRepository.save(Customer.builder()
                .name("John Doe")
                .email("customer@erp.com")
                .phone("+1-555-0101")
                .companyName("Global Logistics Inc.")
                .build());

        Customer acme = customerRepository.save(Customer.builder()
                .name("Alice Smith")
                .email("acme@acmecorp.com")
                .phone("+1-555-0102")
                .companyName("Acme Corporation")
                .build());

        Customer techCorp = customerRepository.save(Customer.builder()
                .name("David Miller")
                .email("techcorp@techcorp.io")
                .phone("+1-555-0103")
                .companyName("TechCorp Solutions")
                .build());

        PotentialCustomer lead = potentialCustomerRepository.save(PotentialCustomer.builder()
                .name("Michael Green")
                .email("prospect@startup.co")
                .phone("+1-555-0201")
                .source("INCOMING_QUERY")
                .status(LeadStatus.NEW_LEAD)
                .build());

        LocalDateTime now = LocalDateTime.now();

        Ticket existingCustomerTicket = ticketRepository.save(Ticket.builder()
                .ticketNumber("TICK-" + now.toLocalDate().toString().replace("-", "") + "-100001")
                .senderEmail(acme.getEmail())
                .senderName(acme.getName())
                .subject("Invoice discrepancy for October batch")
                .description("Invoice #8819 applied a 5% discount instead of the contracted 10%.")
                .customerType(CustomerType.EXISTING_CUSTOMER)
                .customer(acme)
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.HIGH)
                .tatHours(24)
                .dueAt(now.plusHours(18))
                .createdAt(now.minusHours(6))
                .assignedTo(representative)
                .build());

        Ticket nearSlaTicket = ticketRepository.save(Ticket.builder()
                .ticketNumber("TICK-" + now.toLocalDate().toString().replace("-", "") + "-100002")
                .senderEmail(techCorp.getEmail())
                .senderName(techCorp.getName())
                .subject("Warehouse scanner sync failure")
                .description("Scanners in warehouse zone B cannot synchronise with the ERP server.")
                .customerType(CustomerType.EXISTING_CUSTOMER)
                .customer(techCorp)
                .status(TicketStatus.IN_PROGRESS)
                .priority(TicketPriority.CRITICAL)
                .tatHours(24)
                .dueAt(now.plusMinutes(75))
                .nearSlaBreach(true)
                .createdAt(now.minusHours(22))
                .assignedTo(representative)
                .build());

        ticketRepository.save(Ticket.builder()
                .ticketNumber("TICK-" + now.toLocalDate().toString().replace("-", "") + "-100003")
                .senderEmail(john.getEmail())
                .senderName(john.getName())
                .subject("Request for API rate limit increase")
                .description("Please increase our shipment sync rate limit to 500 requests per minute.")
                .customerType(CustomerType.EXISTING_CUSTOMER)
                .customer(john)
                .status(TicketStatus.RESOLVED)
                .priority(TicketPriority.MEDIUM)
                .tatHours(24)
                .dueAt(now.minusHours(4))
                .createdAt(now.minusHours(28))
                .resolvedAt(now.minusHours(8))
                .resolutionNotes("Rate limit increased on the API gateway.")
                .assignedTo(representative)
                .build());

        Ticket leadTicket = ticketRepository.save(Ticket.builder()
                .ticketNumber("TICK-" + now.toLocalDate().toString().replace("-", "") + "-100004")
                .senderEmail(lead.getEmail())
                .senderName(lead.getName())
                .subject("Enterprise ERP demo and pricing")
                .description("We are evaluating ERP providers and would like a demo for 120 employees.")
                .customerType(CustomerType.POTENTIAL_CUSTOMER)
                .potentialCustomer(lead)
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.HIGH)
                .tatHours(24)
                .dueAt(now.plusHours(22))
                .createdAt(now.minusHours(2))
                .build());

        auditService.record(existingCustomerTicket.getId(), existingCustomerTicket.getTicketNumber(),
                "QUERY_INGESTED_EXISTING_CUSTOMER", acme.getEmail(),
                "Seed ticket for an existing customer with 24h TAT.");

        auditService.record(leadTicket.getId(), leadTicket.getTicketNumber(),
                "QUERY_INGESTED_POTENTIAL_CUSTOMER", lead.getEmail(),
                "Seed lead ticket. Sales Manager was notified.");

        notificationService.notifyManagers(NotificationType.NEW_LEAD,
                "New lead from " + lead.getEmail() + " (ticket #" + leadTicket.getTicketNumber() + ").",
                leadTicket);

        notificationService.notifyManagers(NotificationType.SLA_WARNING,
                "Ticket #" + nearSlaTicket.getTicketNumber() + " has less than 2 hours remaining.",
                nearSlaTicket);

        log.info("Development seed complete: 3 users, 3 customers, 1 lead, 4 tickets, 2 notifications.");
        log.info("Seed accounts: customer@erp.com, salesrep@erp.com, manager@erp.com");
    }
}
