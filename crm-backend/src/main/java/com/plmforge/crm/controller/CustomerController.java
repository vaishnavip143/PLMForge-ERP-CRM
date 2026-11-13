package com.plmforge.crm.controller;

import com.plmforge.crm.dto.response.ApiResponse;
import com.plmforge.crm.dto.response.CustomerResponse;
import com.plmforge.crm.dto.response.TicketResponse;
import com.plmforge.crm.service.CustomerService;
import com.plmforge.crm.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customer self-service", description = "Profile and own tickets for the CUSTOMER role")
public class CustomerController {

    private final CustomerService customerService;
    private final TicketService ticketService;

    public CustomerController(CustomerService customerService, TicketService ticketService) {
        this.customerService = customerService;
        this.ticketService = ticketService;
    }

    @GetMapping("/my-profile")
    @Operation(summary = "Get the authenticated customer's profile")
    public ResponseEntity<ApiResponse<CustomerResponse>> myProfile(Authentication authentication) {
        CustomerResponse profile = customerService.getMyProfile(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(profile, "Customer profile retrieved"));
    }

    @GetMapping("/my-tickets")
    @Operation(summary = "Get the authenticated customer's tickets with TAT details")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> myTickets(Authentication authentication) {
        List<TicketResponse> tickets = ticketService.getMyTickets(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(tickets, "Customer tickets retrieved"));
    }
}
