package com.festpass.service;

import com.festpass.dto.CreateEventRequest;
import com.festpass.dto.EventAttendanceResponse;
import com.festpass.dto.EventResponse;
import com.festpass.dto.UpdateEventRequest;
import com.festpass.entity.FestEvent;
import com.festpass.exception.ConflictException;
import com.festpass.exception.ResourceNotFoundException;
import com.festpass.repository.FestEventRepository;
import com.festpass.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final FestEventRepository eventRepository;
    private final TicketRepository ticketRepository;

    public EventService(FestEventRepository eventRepository, TicketRepository ticketRepository) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        FestEvent event = new FestEvent(
                request.getName(),
                request.getDescription(),
                request.getVenue(),
                request.getEventDate(),
                request.getCapacity(),
                request.getTicketPrice()
        );

        FestEvent saved = eventRepository.save(event);
        logger.info("Created new fest event: '{}' (ID: {}) with capacity {}", saved.getName(), saved.getId(), saved.getCapacity());
        return mapToResponse(saved, 0);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(event -> {
                    long issuedCount = ticketRepository.countByEventId(event.getId());
                    return mapToResponse(event, issuedCount);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        FestEvent event = findEventOrThrow(id);
        long issuedCount = ticketRepository.countByEventId(event.getId());
        return mapToResponse(event, issuedCount);
    }

    @Transactional
    public EventResponse updateEvent(Long id, UpdateEventRequest request) {
        FestEvent event = findEventOrThrow(id);
        long issuedCount = ticketRepository.countByEventId(id);

        // Safe capacity update rule: Cannot reduce capacity below tickets already issued
        if (request.getCapacity() != null) {
            if (request.getCapacity() < issuedCount) {
                throw new ConflictException(String.format(
                        "Cannot reduce event capacity to %d because %d tickets have already been issued",
                        request.getCapacity(), issuedCount
                ));
            }
            event.setCapacity(request.getCapacity());
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            event.setName(request.getName());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getVenue() != null && !request.getVenue().isBlank()) {
            event.setVenue(request.getVenue());
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        if (request.getTicketPrice() != null) {
            event.setTicketPrice(request.getTicketPrice());
        }

        FestEvent updated = eventRepository.save(event);
        logger.info("Updated fest event ID: {}", updated.getId());
        return mapToResponse(updated, issuedCount);
    }

    @Transactional
    public void deleteEvent(Long id) {
        FestEvent event = findEventOrThrow(id);

        // Safe delete rule: Prevent deleting an event if tickets have been issued
        if (ticketRepository.existsByEventId(id)) {
            long issuedCount = ticketRepository.countByEventId(id);
            throw new ConflictException(String.format(
                    "Cannot delete event '%s' (ID: %d) because %d ticket(s) have already been issued for it. Deletion would compromise ticketing audit history.",
                    event.getName(), id, issuedCount
            ));
        }

        eventRepository.delete(event);
        logger.info("Deleted fest event ID: {}", id);
    }

    @Transactional(readOnly = true)
    public EventAttendanceResponse getAttendance(Long id) {
        FestEvent event = findEventOrThrow(id);
        long issuedCount = ticketRepository.countByEventId(id);
        long checkedInHeadcount = ticketRepository.countByEventIdAndCheckedInTrue(id);

        return new EventAttendanceResponse(
                event.getId(),
                event.getName(),
                event.getCapacity(),
                issuedCount,
                checkedInHeadcount
        );
    }

    public FestEvent findEventOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fest event with ID " + id + " not found"));
    }

    private EventResponse mapToResponse(FestEvent event, long issuedTicketsCount) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getVenue(),
                event.getEventDate(),
                event.getCapacity(),
                event.getTicketPrice(),
                issuedTicketsCount,
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
