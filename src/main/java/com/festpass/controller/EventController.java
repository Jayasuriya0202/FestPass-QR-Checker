package com.festpass.controller;

import com.festpass.dto.CreateEventRequest;
import com.festpass.dto.EventAttendanceResponse;
import com.festpass.dto.EventResponse;
import com.festpass.dto.UpdateEventRequest;
import com.festpass.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Fest Events", description = "CRUD and attendance reporting operations for college fest events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @Operation(summary = "Create a new fest event", description = "Registers a new event with total ticket capacity and ticket pricing")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Event created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for request body")
    })
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        EventResponse created = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List all fest events", description = "Retrieves all registered fest events with current issued ticket counts")
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get fest event by ID", description = "Retrieves detailed information for a single event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event details found"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing fest event", description = "Updates event attributes safely. Rejects capacity reductions below already-issued tickets.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error in updated fields"),
            @ApiResponse(responseCode = "404", description = "Event not found"),
            @ApiResponse(responseCode = "409", description = "Capacity cannot be reduced below already issued tickets")
    })
    public ResponseEntity<EventResponse> updateEvent(@PathVariable Long id, @Valid @RequestBody UpdateEventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a fest event", description = "Deletes an event only if no tickets have been issued yet. Prevents orphan tickets.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Event deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Event not found"),
            @ApiResponse(responseCode = "409", description = "Cannot delete event with existing tickets")
    })
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/attendance")
    @Operation(summary = "Get event attendance and headcount", description = "Returns live checked-in headcount versus capacity and issued tickets")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attendance metrics retrieved"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<EventAttendanceResponse> getAttendance(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getAttendance(id));
    }
}
