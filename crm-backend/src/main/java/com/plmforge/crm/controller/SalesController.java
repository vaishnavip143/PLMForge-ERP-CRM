package com.plmforge.crm.controller;

import com.plmforge.crm.dto.request.UpdateTicketStatusRequest;
import com.plmforge.crm.dto.response.ApiResponse;
import com.plmforge.crm.dto.response.TicketResponse;
import com.plmforge.crm.enums.Role;
import com.plmforge.crm.security.UserPrincipal;
import com.plmforge.crm.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
@Tag(name = "Sales representative", description = "Work on assigned tickets")
public class SalesController {

    private final TicketService ticketService;

    public SalesController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/tickets")
    @Operation(summary = "View tickets assigned to the authenticated representative")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> myAssignedTickets(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<TicketResponse> tickets = principal.getRole() == Role.ROLE_SALES_MANAGER
                ? ticketService.getAllTickets()
                : ticketService.getAssignedTickets(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(tickets, "Tickets retrieved"));
    }

    @PutMapping("/tickets/{id}/status")
    @Operation(summary = "Update the status / resolution of an assigned ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketResponse ticket = ticketService.updateStatus(id, request, principal);
        return ResponseEntity.ok(ApiResponse.ok(ticket, "Ticket status updated"));
    }
}
