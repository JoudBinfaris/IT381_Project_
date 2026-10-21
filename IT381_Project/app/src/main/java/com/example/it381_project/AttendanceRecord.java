package com.example.it381_project;

import java.io.Serializable;

public class AttendanceRecord implements Serializable {
    private int id;
    private String participantName;
    private String eventName;
    private String dateTime;

    public AttendanceRecord() {
    }

    public AttendanceRecord(int id, String participantName, String eventName, String dateTime) {
        this.id = id;
        this.participantName = participantName;
        this.eventName = eventName;
        this.dateTime = dateTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }
}
