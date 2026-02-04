package org.example.model;

import java.time.LocalDate;

public class Patient {

    private int id;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String svnr;
    private String phone;
    private String reason;
    private Integer stationId; // kann null sein

    public Patient() {}

    public Patient(int id, String firstName, String lastName, LocalDate birthDate,
                   String svnr, String phone, String reason, Integer stationId) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.svnr = svnr;
        this.phone = phone;
        this.reason = reason;
        this.stationId = stationId;
    }

    public Patient(String firstName, String lastName, LocalDate birthDate, String svnr) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.svnr = svnr;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getSvnr() { return svnr; }
    public void setSvnr(String svnr) { this.svnr = svnr; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getStationId() { return stationId; }
    public void setStationId(Integer stationId) { this.stationId = stationId; }

    @Override
    public String toString() {
        return id + ": " + lastName + ", " + firstName + " (" + birthDate + ")";
    }
}
