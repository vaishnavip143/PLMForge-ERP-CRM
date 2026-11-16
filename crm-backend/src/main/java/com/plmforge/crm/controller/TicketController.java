package com.plmforge.crm.controller;

import com.plmforge.crm.dto.request.TicketIngestRequest;
import com.plmforge.crm.dto.response.ApiResponse;
import com.plmforge.crm.dto.response.TicketIngestResponse;
import com.plmforge.crm.dto.response.TicketResponse;
import com.plmforge.crm.security.UserPrincipal;
import com.plmforge.crm.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Query ingestion and ticket access")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/ingest")
    @Operation(summary = "Ingest a customer query (existing customer or new lead)")
    public ResponseEntity<ApiResponse<TicketIngestResponse>> ingest(@Valid @RequestBody TicketIngestRequest request,
                                                                    Authentication authentication) {
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
        String performedBy = authentication != null ? authentication.getName() : null;
        UserPrincipal principal = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            principal = userPrincipal;
        }
        TicketIngestResponse response = ticketService.ingest(request, performedBy, isAuthenticated, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, response.getMessage()));
    }

    @GetMapping("/my")
    @Operation(summary = "List the authenticated customer's own tickets")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> myTickets(Authentication authentication) {
        List<TicketResponse> tickets = ticketService.getMyTickets(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(tickets, "Your tickets retrieved"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one ticket (ownership and role rules apply)")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicket(@PathVariable Long id,
                                                                 @AuthenticationPrincipal UserPrincipal principal) {
        TicketResponse ticket = ticketService.getTicketForUser(id, principal);
        return ResponseEntity.ok(ApiResponse.ok(ticket, "Ticket retrieved"));
    }
}
