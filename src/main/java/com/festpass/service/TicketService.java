package com.festpass.service;

import com.festpass.dto.CheckInResponse;
import com.festpass.dto.IssueTicketRequest;
import com.festpass.dto.TicketResponse;
import com.festpass.entity.Attendee;
import com.festpass.entity.FestEvent;
import com.festpass.entity.Ticket;
import com.festpass.exception.CapacityExceededException;
import com.festpass.exception.ResourceNotFoundException;
import com.festpass.exception.TicketAlreadyCheckedInException;
import com.festpass.repository.AttendeeRepository;
import com.festpass.repository.FestEventRepository;
import com.festpass.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;
    private final FestEventRepository eventRepository;
    private final AttendeeRepository attendeeRepository;
    private final QrCodeGeneratorService qrCodeGeneratorService;

    public TicketService(TicketRepository ticketRepository,
                         FestEventRepository eventRepository,
                         AttendeeRepository attendeeRepository,
                         QrCodeGeneratorService qrCodeGeneratorService) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.attendeeRepository = attendeeRepository;
        this.qrCodeGeneratorService = qrCodeGeneratorService;
    }

    /**
     * Issues a ticket for an attendee at a specific fest event.
     * Enforces the capacity check and ticket creation within a single transaction.
     */
    @Transactional
    public TicketResponse issueTicket(IssueTicketRequest request) {
        // 1. Fetch Event
        FestEvent event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Cannot issue ticket: Event with ID " + request.getEventId() + " not found"));

        // 2. Capacity Check: Verify issued tickets against maximum capacity before saving
        long currentIssuedCount = ticketRepository.countByEventId(event.getId());
        if (currentIssuedCount >= event.getCapacity()) {
            logger.warn("Ticket issuance rejected: Event '{}' (ID: {}) has reached maximum capacity ({}/{})",
                    event.getName(), event.getId(), currentIssuedCount, event.getCapacity());
            throw new CapacityExceededException(String.format(
                    "Event '%s' has reached maximum capacity of %d tickets. No more tickets can be issued.",
                    event.getName(), event.getCapacity()
            ));
        }

        // 3. Find or register Attendee by email
        Attendee attendee = attendeeRepository.findByEmail(request.getAttendeeEmail())
                .map(existing -> {
                    // Update name or phone if provided
                    if (request.getAttendeeName() != null && !request.getAttendeeName().isBlank()) {
                        existing.setName(request.getAttendeeName());
                    }
                    if (request.getAttendeePhone() != null && !request.getAttendeePhone().isBlank()) {
                        existing.setPhone(request.getAttendeePhone());
                    }
                    return attendeeRepository.save(existing);
                })
                .orElseGet(() -> {
                    Attendee newAttendee = new Attendee(
                            request.getAttendeeName(),
                            request.getAttendeeEmail(),
                            request.getAttendeePhone()
                    );
                    return attendeeRepository.save(newAttendee);
                });

        // 4. Generate unique, opaque QR code string (unguessable UUID format)
        String uniqueQrCode = "FP-" + UUID.randomUUID().toString();

        // 5. Create and persist Ticket
        Ticket ticket = new Ticket(uniqueQrCode, event, attendee);
        Ticket savedTicket = ticketRepository.save(ticket);

        // 6. Generate QR image data URL
        String qrImageBase64 = qrCodeGeneratorService.generateQrCodeDataUrl(uniqueQrCode);

        // Optional status notification log
        logger.info("NOTIFICATION: Ticket issued successfully [Ticket ID: {}, QR: {}] for Attendee '{}' (<{}>) at Event '{}' (Issued: {}/{})",
                savedTicket.getId(), uniqueQrCode, attendee.getName(), attendee.getEmail(), event.getName(), currentIssuedCount + 1, event.getCapacity());

        return mapToTicketResponse(savedTicket, qrImageBase64);
    }

    /**
     * Validates and checks in a ticket via its unique QR code.
     * Enforces one-time check-in and updates headcount.
     */
    @Transactional
    public CheckInResponse checkInTicket(String qrCode) {
        if (qrCode == null || qrCode.isBlank()) {
            throw new ResourceNotFoundException("A valid QR code must be provided for check-in");
        }

        // 1. Validate QR code exists
        Ticket ticket = ticketRepository.findByQrCode(qrCode.trim())
                .orElseThrow(() -> {
                    logger.warn("Check-in failed: Unknown QR code '{}'", qrCode);
                    return new ResourceNotFoundException("Check-in rejected: Unknown QR code '" + qrCode + "'");
                });

        // 2. Reject already-used tickets
        if (ticket.isCheckedIn()) {
            logger.warn("Check-in rejected: Ticket ID {} with QR '{}' was already checked in at {}",
                    ticket.getId(), qrCode, ticket.getCheckedInAt());
            throw new TicketAlreadyCheckedInException(String.format(
                    "Ticket with QR '%s' was already checked in on %s",
                    qrCode, ticket.getCheckedInAt()
            ));
        }

        // 3. Mark ticket as checked-in
        LocalDateTime checkInTimestamp = LocalDateTime.now();
        ticket.setCheckedIn(true);
        ticket.setCheckedInAt(checkInTimestamp);
        ticketRepository.save(ticket);

        FestEvent event = ticket.getEvent();
        Attendee attendee = ticket.getAttendee();

        // 4. Calculate updated headcount
        long updatedHeadcount = ticketRepository.countByEventIdAndCheckedInTrue(event.getId());

        // Optional status notification log
        logger.info("NOTIFICATION: Ticket checked in [Ticket ID: {}, QR: {}] for Attendee '{}' at Event '{}'. Current Headcount: {}/{}",
                ticket.getId(), qrCode, attendee.getName(), event.getName(), updatedHeadcount, event.getCapacity());

        return new CheckInResponse(
                "Check-in successful! Welcome to " + event.getName(),
                ticket.getId(),
                ticket.getQrCode(),
                checkInTimestamp,
                event.getId(),
                event.getName(),
                attendee.getName(),
                updatedHeadcount,
                event.getCapacity()
        );
    }

    /**
     * Retrieve ticket details by ID.
     */
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with ID " + id + " not found"));
        String qrImageBase64 = qrCodeGeneratorService.generateQrCodeDataUrl(ticket.getQrCode());
        return mapToTicketResponse(ticket, qrImageBase64);
    }

    private TicketResponse mapToTicketResponse(Ticket ticket, String qrImageBase64) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getQrCode(),
                qrImageBase64,
                ticket.isCheckedIn(),
                ticket.getIssuedAt(),
                ticket.getCheckedInAt(),
                ticket.getEvent().getId(),
                ticket.getEvent().getName(),
                ticket.getEvent().getTicketPrice(),
                ticket.getAttendee().getId(),
                ticket.getAttendee().getName(),
                ticket.getAttendee().getEmail(),
                ticket.getAttendee().getPhone()
        );
    }
}
