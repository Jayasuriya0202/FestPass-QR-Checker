package com.festpass.dto;

import java.time.LocalDateTime;

public class CheckInResponse {

    private String message;
    private Long ticketId;
    private String qrCode;
    private LocalDateTime checkedInAt;
    private Long eventId;
    private String eventName;
    private String attendeeName;
    private long checkedInHeadcount;
    private Integer eventCapacity;

    public CheckInResponse() {
    }

    public CheckInResponse(String message, Long ticketId, String qrCode, LocalDateTime checkedInAt,
                           Long eventId, String eventName, String attendeeName,
                           long checkedInHeadcount, Integer eventCapacity) {
        this.message = message;
        this.ticketId = ticketId;
        this.qrCode = qrCode;
        this.checkedInAt = checkedInAt;
        this.eventId = eventId;
        this.eventName = eventName;
        this.attendeeName = attendeeName;
        this.checkedInHeadcount = checkedInHeadcount;
        this.eventCapacity = eventCapacity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public String getAttendeeName() {
        return attendeeName;
    }

    public void setAttendeeName(String attendeeName) {
        this.attendeeName = attendeeName;
    }

    public long getCheckedInHeadcount() {
        return checkedInHeadcount;
    }

    public void setCheckedInHeadcount(long checkedInHeadcount) {
        this.checkedInHeadcount = checkedInHeadcount;
    }

    public Integer getEventCapacity() {
        return eventCapacity;
    }

    public void setEventCapacity(Integer eventCapacity) {
        this.eventCapacity = eventCapacity;
    }
}
