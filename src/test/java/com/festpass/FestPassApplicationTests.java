package com.festpass;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.festpass.dto.CheckInRequest;
import com.festpass.dto.CreateEventRequest;
import com.festpass.dto.IssueTicketRequest;
import com.festpass.dto.UpdateEventRequest;
import com.festpass.repository.AttendeeRepository;
import com.festpass.repository.FestEventRepository;
import com.festpass.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class FestPassApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FestEventRepository eventRepository;

    @Autowired
    private AttendeeRepository attendeeRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        attendeeRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    @DisplayName("Case 1: Create an event with valid capacity and ticket price")
    void testCreateEvent_ValidInput_Returns201() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                "Spring TechFest 2026",
                "Annual National College Cultural & Tech Festival",
                "Main Auditorium, Block C",
                LocalDateTime.now().plusDays(10),
                100,
                new BigDecimal("250.00")
        );

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Spring TechFest 2026")))
                .andExpect(jsonPath("$.capacity", is(100)))
                .andExpect(jsonPath("$.ticketPrice", is(250.00)))
                .andExpect(jsonPath("$.issuedTicketsCount", is(0)));
    }

    @Test
    @DisplayName("Case 2: Reject invalid event input (missing name, zero capacity, negative price)")
    void testCreateEvent_InvalidInput_Returns400() throws Exception {
        // Missing name, capacity <= 0, negative ticket price
        CreateEventRequest request = new CreateEventRequest(
                "",
                "Description",
                "",
                LocalDateTime.now().plusDays(1),
                0,
                new BigDecimal("-50.00")
        );

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.name", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.venue", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.capacity", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.ticketPrice", notNullValue()));
    }

    @Test
    @DisplayName("Case 3: Issue a ticket successfully and show its unique QR value and image")
    void testIssueTicket_Success_Returns201AndQrCode() throws Exception {
        Long eventId = createTestEvent("Hackathon 2026", 50, new BigDecimal("150.00"));

        IssueTicketRequest request = new IssueTicketRequest(
                eventId,
                "Alice Smith",
                "alice.smith@college.edu",
                "+91 9876543210"
        );

        mockMvc.perform(post("/api/tickets/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketId", notNullValue()))
                .andExpect(jsonPath("$.qrCode", startsWith("FP-")))
                .andExpect(jsonPath("$.qrCodeImageBase64", startsWith("data:image/png;base64,")))
                .andExpect(jsonPath("$.checkedIn", is(false)))
                .andExpect(jsonPath("$.attendeeName", is("Alice Smith")))
                .andExpect(jsonPath("$.attendeeEmail", is("alice.smith@college.edu")));
    }

    @Test
    @DisplayName("Case 4: Reject ticket issuance when event capacity is reached (409 Conflict)")
    void testIssueTicket_CapacityFull_Returns409() throws Exception {
        // Create an event with capacity = 1
        Long eventId = createTestEvent("Robotics Workshop", 1, new BigDecimal("100.00"));

        // Issue 1st ticket (should succeed)
        IssueTicketRequest first = new IssueTicketRequest(eventId, "Bob", "bob@college.edu", "111");
        mockMvc.perform(post("/api/tickets/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        // Issue 2nd ticket (capacity reached -> should reject with 409 Conflict)
        IssueTicketRequest second = new IssueTicketRequest(eventId, "Charlie", "charlie@college.edu", "222");
        mockMvc.perform(post("/api/tickets/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Capacity Exceeded")))
                .andExpect(jsonPath("$.message", containsString("reached maximum capacity")));
    }

    @Test
    @DisplayName("Case 5: Check in a valid QR once and confirm the headcount increases")
    void testCheckIn_ValidQr_Returns200AndIncreasesHeadcount() throws Exception {
        Long eventId = createTestEvent("EDM Night", 200, new BigDecimal("500.00"));
        String qrCode = issueTicketAndGetQr(eventId, "David", "david@college.edu");

        CheckInRequest checkInRequest = new CheckInRequest(qrCode);

        mockMvc.perform(post("/api/tickets/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Check-in successful")))
                .andExpect(jsonPath("$.qrCode", is(qrCode)))
                .andExpect(jsonPath("$.checkedInHeadcount", is(1)))
                .andExpect(jsonPath("$.eventCapacity", is(200)));
    }

    @Test
    @DisplayName("Case 6: Reject the same QR on a second check-in (409 Conflict)")
    void testCheckIn_DuplicateQr_Returns409() throws Exception {
        Long eventId = createTestEvent("Drama Fest", 100, new BigDecimal("100.00"));
        String qrCode = issueTicketAndGetQr(eventId, "Emma", "emma@college.edu");

        CheckInRequest checkInRequest = new CheckInRequest(qrCode);

        // First check-in -> 200 OK
        mockMvc.perform(post("/api/tickets/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInRequest)))
                .andExpect(status().isOk());

        // Second check-in -> 409 Conflict
        mockMvc.perform(post("/api/tickets/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Ticket Already Checked In")))
                .andExpect(jsonPath("$.message", containsString("already checked in")));
    }

    @Test
    @DisplayName("Case 7: Reject an unknown QR (404 Not Found)")
    void testCheckIn_UnknownQr_Returns404() throws Exception {
        CheckInRequest checkInRequest = new CheckInRequest("FP-NON-EXISTENT-QR-TOKEN");

        mockMvc.perform(post("/api/tickets/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Unknown QR code")));
    }

    @Test
    @DisplayName("Case 8: Show headcount and capacity for the event (distinct headcount vs issued)")
    void testGetAttendance_ReturnsCorrectHeadcountAndCapacity() throws Exception {
        Long eventId = createTestEvent("Coding Relay", 50, new BigDecimal("50.00"));

        // Issue 3 tickets
        String qr1 = issueTicketAndGetQr(eventId, "User 1", "u1@college.edu");
        issueTicketAndGetQr(eventId, "User 2", "u2@college.edu");
        issueTicketAndGetQr(eventId, "User 3", "u3@college.edu");

        // Check in only 1 ticket
        mockMvc.perform(post("/api/tickets/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckInRequest(qr1))))
                .andExpect(status().isOk());

        // Verify attendance metrics: issued=3, headcount=1, capacity=50, remainingCapacity=47
        mockMvc.perform(get("/api/events/" + eventId + "/attendance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId", is(eventId.intValue())))
                .andExpect(jsonPath("$.capacity", is(50)))
                .andExpect(jsonPath("$.issuedTickets", is(3)))
                .andExpect(jsonPath("$.checkedInHeadcount", is(1)))
                .andExpect(jsonPath("$.remainingCapacity", is(47)))
                .andExpect(jsonPath("$.attendanceRatePercentage", is(2.0)));
    }

    @Test
    @DisplayName("Case 9: Missing event or ticket ID returns clear not-found error (404)")
    void testGetMissingEvent_Returns404() throws Exception {
        mockMvc.perform(get("/api/events/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Fest event with ID 999999 not found")));
    }

    @Test
    @DisplayName("Case 10a: Safe update - Prevent reducing capacity below issued tickets (409)")
    void testSafeEventUpdate_CapacityBelowIssued_Returns409() throws Exception {
        Long eventId = createTestEvent("Battle of the Bands", 10, new BigDecimal("200.00"));

        // Issue 3 tickets
        issueTicketAndGetQr(eventId, "Fan 1", "f1@college.edu");
        issueTicketAndGetQr(eventId, "Fan 2", "f2@college.edu");
        issueTicketAndGetQr(eventId, "Fan 3", "f3@college.edu");

        // Attempt to reduce capacity to 2 (which is < 3 issued)
        UpdateEventRequest updateReq = new UpdateEventRequest();
        updateReq.setCapacity(2);

        mockMvc.perform(put("/api/events/" + eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Cannot reduce event capacity to 2 because 3 tickets have already been issued")));
    }

    @Test
    @DisplayName("Case 10b: Safe delete - Prevent deleting event with issued tickets (409)")
    void testSafeEventDelete_WithIssuedTickets_Returns409() throws Exception {
        Long eventId = createTestEvent("Quiz Competition", 20, new BigDecimal("0.00"));
        issueTicketAndGetQr(eventId, "Quizzer", "quiz@college.edu");

        mockMvc.perform(delete("/api/events/" + eventId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Cannot delete event")));
    }

    @Test
    @DisplayName("Case 10c: Safe delete - Allows deleting empty event with zero tickets (204)")
    void testSafeEventDelete_WithoutTickets_Returns204() throws Exception {
        Long eventId = createTestEvent("Cancelled Meetup", 20, new BigDecimal("0.00"));

        mockMvc.perform(delete("/api/events/" + eventId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/events/" + eventId))
                .andExpect(status().isNotFound());
    }

    // --- Helper Methods ---

    private Long createTestEvent(String name, int capacity, BigDecimal price) throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                name,
                "Test Event Description",
                "College Campus",
                LocalDateTime.now().plusDays(5),
                capacity,
                price
        );

        MvcResult result = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseJson).get("id").asLong();
    }

    private String issueTicketAndGetQr(Long eventId, String name, String email) throws Exception {
        IssueTicketRequest request = new IssueTicketRequest(eventId, name, email, "9999999999");
        MvcResult result = mockMvc.perform(post("/api/tickets/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseJson).get("qrCode").asText();
    }
}
