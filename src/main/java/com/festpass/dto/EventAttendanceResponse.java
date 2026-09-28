package com.festpass.dto;

public class EventAttendanceResponse {

    private Long eventId;
    private String eventName;
    private Integer capacity;
    private long issuedTickets;
    private long checkedInHeadcount;
    private long remainingCapacity;
    private double attendanceRatePercentage;

    public EventAttendanceResponse() {
    }

    public EventAttendanceResponse(Long eventId, String eventName, Integer capacity, long issuedTickets, long checkedInHeadcount) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.capacity = capacity;
        this.issuedTickets = issuedTickets;
        this.checkedInHeadcount = checkedInHeadcount;
        this.remainingCapacity = Math.max(0, capacity - issuedTickets);
        this.attendanceRatePercentage = (capacity != null && capacity > 0)
                ? Math.round(((double) checkedInHeadcount / capacity) * 1000.0) / 10.0
                : 0.0;
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

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public long getIssuedTickets() {
        return issuedTickets;
    }

    public void setIssuedTickets(long issuedTickets) {
        this.issuedTickets = issuedTickets;
    }

    public long getCheckedInHeadcount() {
        return checkedInHeadcount;
    }

    public void setCheckedInHeadcount(long checkedInHeadcount) {
        this.checkedInHeadcount = checkedInHeadcount;
    }

    public long getRemainingCapacity() {
        return remainingCapacity;
    }

    public void setRemainingCapacity(long remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }

    public double getAttendanceRatePercentage() {
        return attendanceRatePercentage;
    }

    public void setAttendanceRatePercentage(double attendanceRatePercentage) {
        this.attendanceRatePercentage = attendanceRatePercentage;
    }
}
