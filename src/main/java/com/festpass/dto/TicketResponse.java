package com.festpass.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TicketResponse {

    private Long ticketId;
    private String qrCode;
    private String qrCodeImageBase64;
    private boolean checkedIn;
    private LocalDateTime issuedAt;
    private LocalDateTime checkedInAt;

    private Long eventId;
    private String eventName;
    private BigDecimal ticketPrice;

    private Long attendeeId;
    private String attendeeName;
    private String attendeeEmail;
    private String attendeePhone;

    public TicketResponse() {
    }

    public TicketResponse(Long ticketId, String qrCode, String qrCodeImageBase64, boolean checkedIn,
                          LocalDateTime issuedAt, LocalDateTime checkedInAt,
                          Long eventId, String eventName, BigDecimal ticketPrice,
                          Long attendeeId, String attendeeName, String attendeeEmail, String attendeePhone) {
        this.ticketId = ticketId;
        this.qrCode = qrCode;
        this.qrCodeImageBase64 = qrCodeImageBase64;
        this.checkedIn = checkedIn;
        this.issuedAt = issuedAt;
        this.checkedInAt = checkedInAt;
        this.eventId = eventId;
        this.eventName = eventName;
        this.ticketPrice = ticketPrice;
        this.attendeeId = attendeeId;
        this.attendeeName = attendeeName;
        this.attendeeEmail = attendeeEmail;
        this.attendeePhone = attendeePhone;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getQrCodeImageBase64() {
        return qrCodeImageBase64;
    }

    public void setQrCodeImageBase64(String qrCodeImageBase64) {
        this.qrCodeImageBase64 = qrCodeImageBase64;
    }

    public boolean isCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(boolean checkedIn) {
        this.checkedIn = checkedIn;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(LocalDateTime checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public BigDecimal getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(BigDecimal ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public Long getAttendeeId() {
        return attendeeId;
    }

    public void setAttendeeId(Long attendeeId) {
        this.attendeeId = attendeeId;
    }

    public String getAttendeeName() {
        return attendeeName;
    }

    public void setAttendeeName(String attendeeName) {
        this.attendeeName = attendeeName;
    }

    public String getAttendeeEmail() {
        return attendeeEmail;
    }

    public void setAttendeeEmail(String attendeeEmail) {
        this.attendeeEmail = attendeeEmail;
    }

    public String getAttendeePhone() {
        return attendeePhone;
    }

    public void setAttendeePhone(String attendeePhone) {
        this.attendeePhone = attendeePhone;
    }
}
