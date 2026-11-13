package com.plmforge.crm.controller;

import com.plmforge.crm.dto.request.AssignTicketRequest;
import com.plmforge.crm.dto.response.*;
import com.plmforge.crm.security.UserPrincipal;
import com.plmforge.crm.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager")
@Tag(name = "Sales manager", description = "Manager-only oversight, assignment and SLA alerts")
public class ManagerController {

    private final TicketService ticketService;
    private final CustomerService customerService;
    private final LeadService leadService;
    private final NotificationService notificationService;
    private final SlaService slaService;
    private final AuditService auditService;
    private final AuthService authService;

    public ManagerController(TicketService ticketService,
                             CustomerService customerService,
                             LeadService leadService,
                             NotificationService notificationService,
                             SlaService slaService,
                             AuditService auditService,
                             AuthService authService) {
        this.ticketService = ticketService;
        this.customerService = customerService;
        this.leadService = leadService;
        this.notificationService = notificationService;
        this.slaService = slaService;
        this.auditService = auditService;
        this.authService = authService;
    }

    @GetMapping("/tickets")
    @Operation(summary = "View all tickets")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> allTickets() {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getAllTickets(), "All tickets retrieved"));
    }

    @GetMapping("/leads")
    @Operation(summary = "View all potential customers / leads")
    public ResponseEntity<ApiResponse<List<PotentialCustomerResponse>>> allLeads() {
        return ResponseEntity.ok(ApiResponse.ok(leadService.getAllLeads(), "Leads retrieved"));
    }

    @GetMapping("/customers")
    @Operation(summary = "View all existing customers")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> allCustomers() {
        return ResponseEntity.ok(ApiResponse.ok(customerService.getAllCustomers(), "Customers retrieved"));
    }

    @GetMapping("/representatives")
    @Operation(summary = "List sales representatives available for assignment")
    public ResponseEntity<ApiResponse<List<RepresentativeResponse>>> representatives() {
        return ResponseEntity.ok(ApiResponse.ok(authService.getSalesRepresentatives(),
                "Sales representatives retrieved"));
    }

    @PostMapping("/tickets/{id}/assign")
    @Operation(summary = "Assign a ticket to a sales representative")
    public ResponseEntity<ApiResponse<TicketResponse>> assign(@PathVariable Long id,
                                                              @Valid @RequestBody AssignTicketRequest request,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        TicketResponse ticket = ticketService.assignTicket(id, request.getRepresentativeId(),
                principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(ticket, "Ticket assigned successfully"));
    }

    @GetMapping("/sla-alerts")
    @Operation(summary = "View tickets close to their SLA deadline and overdue tickets")
    public ResponseEntity<ApiResponse<SlaAlertsResponse>> slaAlerts() {
        return ResponseEntity.ok(ApiResponse.ok(slaService.getSlaAlerts(), "SLA alerts retrieved"));
    }

    @GetMapping("/notifications")
    @Operation(summary = "View the manager's notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> notifications(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.getNotifications(principal.getId()),
                "Manager notifications retrieved"));
    }

    @GetMapping("/tickets/{id}/audit")
    @Operation(summary = "View the audit trail of a ticket")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> ticketAudit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(auditService.getTicketAuditTrail(id),
                "Ticket audit trail retrieved"));
    }
}
