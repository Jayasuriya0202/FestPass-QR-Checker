package com.festpass.controller;

import com.festpass.dto.CheckInRequest;
import com.festpass.dto.CheckInResponse;
import com.festpass.dto.IssueTicketRequest;
import com.festpass.dto.TicketResponse;
import com.festpass.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets & Check-in", description = "Endpoints for issuing tickets with QR codes and verifying check-in")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/issue")
    @Operation(summary = "Issue a ticket for an attendee", description = "Checks capacity atomically, generates unique QR value and QR image data URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ticket issued successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Target event not found"),
            @ApiResponse(responseCode = "409", description = "Event capacity has been reached")
    })
    public ResponseEntity<TicketResponse> issueTicket(@Valid @RequestBody IssueTicketRequest request) {
        TicketResponse response = ticketService.issueTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/check-in")
    @Operation(summary = "Validate ticket check-in via QR code", description = "Performs one-time check-in validation. Rejects unknown or already-used QR codes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ticket successfully validated and checked in"),
            @ApiResponse(responseCode = "400", description = "QR code parameter missing"),
            @ApiResponse(responseCode = "404", description = "Unknown QR code"),
            @ApiResponse(responseCode = "409", description = "Ticket already checked in")
    })
    public ResponseEntity<CheckInResponse> checkInTicket(@Valid @RequestBody CheckInRequest request) {
        CheckInResponse response = ticketService.checkInTicket(request.getQrCode());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket details by ID", description = "Retrieves ticket information including QR code and Base64 QR code image")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ticket found"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }
}
