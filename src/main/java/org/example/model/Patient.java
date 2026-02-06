package org.example.model;

import java.time.LocalDate;

/**
 * Modelklasse für einen Patienten.
 * <p>
 * Diese Klasse repräsentiert einen Datensatz aus der Patiententabelle und enthält
 * alle wichtigen Stammdaten wie Name, Geburtsdatum, SVNR, Kontaktinformationen,
 * Aufenthaltsgrund und die zugewiesene Station.
 * </p>
 */
public class Patient {
    private int id;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String svnr;
    private String phone;
    private String address;
    private String reason;
    private Integer stationId;

    /**
     * Gibt die eindeutige ID des Patienten zurück.
     *
     * @return Patienten-ID
     */
    public int getId() { return id; }

    /**
     * Setzt die eindeutige ID des Patienten.
     *
     * @param id Patienten-ID
     */
    public void setId(int id) { this.id = id; }

    /**
     * Gibt den Vornamen des Patienten zurück.
     *
     * @return Vorname
     */
    public String getFirstName() { return firstName; }

    /**
     * Setzt den Vornamen des Patienten.
     *
     * @param firstName Vorname
     */
    public void setFirstName(String firstName) { this.firstName = firstName; }

    /**
     * Gibt den Nachnamen des Patienten zurück.
     *
     * @return Nachname
     */
    public String getLastName() { return lastName; }

    /**
     * Setzt den Nachnamen des Patienten.
     *
     * @param lastName Nachname
     */
    public void setLastName(String lastName) { this.lastName = lastName; }

    /**
     * Gibt das Geburtsdatum des Patienten zurück.
     *
     * @return Geburtsdatum
     */
    public LocalDate getBirthDate() { return birthDate; }

    /**
     * Setzt das Geburtsdatum des Patienten.
     *
     * @param birthDate Geburtsdatum
     */
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    /**
     * Gibt die SVNR des Patienten zurück.
     *
     * @return Sozialversicherungsnummer
     */
    public String getSvnr() { return svnr; }

    /**
     * Setzt die SVNR des Patienten.
     *
     * @param svnr Sozialversicherungsnummer
     */
    public void setSvnr(String svnr) { this.svnr = svnr; }

    /**
     * Gibt die Telefonnummer des Patienten zurück.
     *
     * @return Telefonnummer
     */
    public String getPhone() { return phone; }

    /**
     * Setzt die Telefonnummer des Patienten.
     *
     * @param phone Telefonnummer
     */
    public void setPhone(String phone) { this.phone = phone; }

    /**
     * Gibt die Adresse des Patienten zurück.
     *
     * @return Adresse
     */
    public String getAddress() { return address; }

    /**
     * Setzt die Adresse des Patienten.
     *
     * @param address Adresse
     */
    public void setAddress(String address) { this.address = address; }

    /**
     * Gibt den Grund für den Aufenthalt zurück.
     *
     * @return Aufenthaltsgrund
     */
    public String getReason() { return reason; }

    /**
     * Setzt den Grund für den Aufenthalt.
     *
     * @param reason Aufenthaltsgrund
     */
    public void setReason(String reason) { this.reason = reason; }

    /**
     * Gibt die ID der zugewiesenen Station zurück.
     * <p>
     * Kann {@code null} sein, wenn noch keine Station ausgewählt wurde.
     * </p>
     *
     * @return Stations-ID oder {@code null}
     */
    public Integer getStationId() { return stationId; }

    /**
     * Setzt die ID der zugewiesenen Station.
     *
     * @param stationId Stations-ID (kann {@code null} sein)
     */
    public void setStationId(Integer stationId) { this.stationId = stationId; }
}
