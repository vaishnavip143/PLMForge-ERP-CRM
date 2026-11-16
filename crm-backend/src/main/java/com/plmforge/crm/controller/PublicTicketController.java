package com.plmforge.crm.controller;

import com.plmforge.crm.dto.request.TicketIngestRequest;
import com.plmforge.crm.dto.response.ApiResponse;
import com.plmforge.crm.dto.response.TicketIngestResponse;
import com.plmforge.crm.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public endpoint for query submission without authentication.
 * Unregistered visitors can submit queries using their name and email.
 * The backend resolves existing customers or creates potential customer leads.
 */
@RestController
@RequestMapping("/api/public")
@Tag(name = "Public query submission", description = "Submit queries without login (unregistered visitors)")
public class PublicTicketController {

    private final TicketService ticketService;

    public PublicTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/ingest")
    @Operation(summary = "Submit a query without authentication (public visitor)")
    public ResponseEntity<ApiResponse<TicketIngestResponse>> ingest(@Valid @RequestBody TicketIngestRequest request) {
        TicketIngestResponse response = ticketService.ingest(request, null, false, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, response.getMessage()));
    }
}