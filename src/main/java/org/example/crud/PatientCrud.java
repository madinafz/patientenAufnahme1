package org.example.crud;

import org.example.DB;
import org.example.model.Patient;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD-Klasse für den Zugriff auf die Patiententabelle.
 * <p>
 * Diese Klasse kapselt alle grundlegenden Datenbank-Operationen für {@link Patient}:
 * Laden, Suchen, Einfügen, Aktualisieren und Löschen. Die Daten werden dabei zwischen
 * {@link ResultSet} und dem {@link Patient}-Objekt gemappt.
 * </p>
 */
public class PatientCrud {

    /**
     * Erstellt aus der aktuellen Zeile im {@link ResultSet} ein {@link Patient}-Objekt.
     * <p>
     * Dabei werden die Spaltenwerte aus dem ResultSet gelesen und in ein neues Patient-Objekt
     * übernommen. Optional-Werte wie {@code birth_date} und {@code station_id} werden sauber
     * auf {@code null} gesetzt, falls sie in der DB leer sind.
     * </p>
     *
     * @param rs ResultSet, das bereits auf einer gültigen Zeile steht
     * @return gemapptes Patient-Objekt
     * @throws SQLException wenn beim Zugriff auf das ResultSet ein Fehler auftritt
     */
    private Patient mapRow(ResultSet rs) throws SQLException {

        Patient p = new Patient();
        p.setId(rs.getInt("id"));
        p.setFirstName(rs.getString("first_name"));
        p.setLastName(rs.getString("last_name"));

        Date bd = rs.getDate("birth_date");
        p.setBirthDate(bd == null ? null : bd.toLocalDate());

        p.setSvnr(rs.getString("svnr"));
        p.setPhone(rs.getString("phone"));
        p.setAddress(rs.getString("address"));
        p.setReason(rs.getString("reason"));

        int st = rs.getInt("station_id");
        p.setStationId(rs.wasNull() ? null : st);

        return p;
    }

    /**
     * Lädt alle Patienten aus der Datenbank.
     * <p>
     * Die Liste wird nach Nachname und Vorname sortiert zurückgegeben.
     * </p>
     *
     * @return Liste aller Patienten
     * @throws RuntimeException wenn die Patienten nicht geladen werden können
     */
    public List<Patient> findAll() {
        String sql = """
                SELECT id, first_name, last_name, birth_date, svnr, phone, address, reason, station_id
                FROM patient
                ORDER BY last_name, first_name
                """;
        List<Patient> out = new ArrayList<>();

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) out.add(mapRow(rs));
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("Patienten konnten nicht geladen werden.", e);
        }
    }

    /**
     * Sucht Patienten anhand eines Suchbegriffs.
     * <p>
     * Es wird in mehreren Feldern gesucht (Vorname, Nachname, SVNR, Telefon, Adresse, Grund).
     * Der Suchbegriff wird case-insensitive verarbeitet (über {@code LOWER(...)}).
     * Wenn der Suchbegriff leer ist, wird {@link #findAll()} verwendet.
     * </p>
     *
     * @param q Suchbegriff (kann {@code null} oder leer sein)
     * @return Trefferliste der passenden Patienten
     * @throws RuntimeException wenn die Suche nicht durchgeführt werden kann
     */
    public List<Patient> search(String q) {
        String query = (q == null) ? "" : q.toLowerCase();
        if (query.isEmpty()) return findAll();

        String like = "%" + query + "%";
        String sql = """
                SELECT id, first_name, last_name, birth_date, svnr, phone, address, reason, station_id
                FROM patient
                WHERE LOWER(first_name) LIKE ?
                   OR LOWER(last_name) LIKE ?
                   OR svnr LIKE ?
                   OR phone LIKE ?
                   OR LOWER(address) LIKE ?
                   OR LOWER(reason) LIKE ?
                ORDER BY last_name, first_name
                """;

        List<Patient> out = new ArrayList<>();
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setString(5, like);
            ps.setString(6, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("Suche konnte nicht durchgeführt werden.", e);
        }
    }

    /**
     * Legt einen neuen Patienten in der Datenbank an.
     * <p>
     * Nach dem Insert wird die generierte ID aus den Generated Keys gelesen und in das
     * übergebene {@link Patient}-Objekt zurückgeschrieben.
     * </p>
     *
     * @param p Patient-Objekt mit den zu speichernden Daten
     * @throws RuntimeException wenn der Patient nicht angelegt werden kann
     */
    public void insert(Patient p) {
        String sql = """
                INSERT INTO patient (first_name, last_name, birth_date, svnr, phone, address, reason, station_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, p.getFirstName());
            ps.setString(2, p.getLastName());
            ps.setDate(3, p.getBirthDate() == null ? null : Date.valueOf(p.getBirthDate()));
            ps.setString(4, p.getSvnr());
            ps.setString(5, p.getPhone());
            ps.setString(6, p.getAddress());
            ps.setString(7, p.getReason());

            if (p.getStationId() == null) ps.setNull(8, Types.INTEGER);
            else ps.setInt(8, p.getStationId());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getInt(1));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Patient konnte nicht angelegt werden.", e);
        }
    }

    /**
     * Aktualisiert einen bestehenden Patienten in der Datenbank.
     * <p>
     * Es wird anhand der {@code id} aktualisiert. Felder wie {@code birth_date} oder
     * {@code station_id} werden bei Bedarf auf {@code null} gesetzt, wenn sie im Objekt
     * nicht befüllt sind.
     * </p>
     *
     * @param p Patient-Objekt mit aktualisierten Daten (inkl. gesetzter ID)
     * @throws RuntimeException wenn der Patient nicht gespeichert werden kann
     */
    public void update(Patient p) {
        String sql = """
                UPDATE patient
                SET first_name=?, last_name=?, birth_date=?, svnr=?, phone=?, address=?, reason=?, station_id=?
                WHERE id=?
                """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, p.getFirstName());
            ps.setString(2, p.getLastName());
            ps.setDate(3, p.getBirthDate() == null ? null : Date.valueOf(p.getBirthDate()));
            ps.setString(4, p.getSvnr());
            ps.setString(5, p.getPhone());
            ps.setString(6, p.getAddress());
            ps.setString(7, p.getReason());

            if (p.getStationId() == null) ps.setNull(8, Types.INTEGER);
            else ps.setInt(8, p.getStationId());

            ps.setInt(9, p.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Patient konnte nicht gespeichert werden.", e);
        }
    }

    /**
     * Löscht einen Patienten anhand seiner ID.
     *
     * @param id ID des zu löschenden Patienten
     * @throws RuntimeException wenn der Patient nicht gelöscht werden kann
     */
    public void deleteById(int id) {
        String sql = "DELETE FROM patient WHERE id=?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Patient konnte nicht gelöscht werden.", e);
        }
    }
}
