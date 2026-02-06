package org.example.crud;

import org.example.DB;
import org.example.model.Patient;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PatientCrud {

    private Patient mapRow(ResultSet rs) throws SQLException {
        Patient p = new Patient();
        p.setId(rs.getInt("id"));
        p.setFirstName(rs.getString("first_name"));
        p.setLastName(rs.getString("last_name"));

        Date bd = rs.getDate("birth_date");
        p.setBirthDate(bd == null ? null : bd.toLocalDate());

        p.setSvnr(rs.getString("svnr"));
        p.setPhone(rs.getString("phone"));
        p.setReason(rs.getString("reason")); // ✅ DAS war oft der Grund warum’s in der GUI leer war

        int st = rs.getInt("station_id");
        p.setStationId(rs.wasNull() ? null : st);

        return p;
    }

    public List<Patient> findAll() {
        String sql = """
                SELECT id, first_name, last_name, birth_date, svnr, phone, reason, station_id
                FROM patient
                ORDER BY last_name, first_name
                """;
        List<Patient> out = new ArrayList<>();

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) out.add(mapRow(rs));
            return out;

        } catch (Exception e) {
            throw new RuntimeException("Patienten konnten nicht geladen werden.", e);
        }
    }

    // ✅ schneller als: findAll() + in Java filtern
    public List<Patient> search(String query) {
        String q = (query == null) ? "" : query.trim();
        if (q.isEmpty()) return findAll();

        String like = "%" + q.toLowerCase() + "%";
        String sql = """
                SELECT id, first_name, last_name, birth_date, svnr, phone, reason, station_id
                FROM patient
                WHERE LOWER(first_name) LIKE ?
                   OR LOWER(last_name) LIKE ?
                   OR LOWER(svnr) LIKE ?
                   OR LOWER(phone) LIKE ?
                   OR LOWER(reason) LIKE ?
                ORDER BY last_name, first_name
                """;

        List<Patient> out = new ArrayList<>();
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (int i = 1; i <= 5; i++) ps.setString(i, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
            return out;

        } catch (Exception e) {
            throw new RuntimeException("Suche konnte nicht durchgeführt werden.", e);
        }
    }

    public void insert(Patient p) {
        String sql = """
                INSERT INTO patient (first_name, last_name, birth_date, svnr, phone, reason, station_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, p.getFirstName());
            ps.setString(2, p.getLastName());
            ps.setDate(3, p.getBirthDate() == null ? null : Date.valueOf(p.getBirthDate()));
            ps.setString(4, p.getSvnr());
            ps.setString(5, p.getPhone());
            ps.setString(6, p.getReason());
            if (p.getStationId() == null) ps.setNull(7, Types.INTEGER);
            else ps.setInt(7, p.getStationId());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getInt(1));
            }

        } catch (Exception e) {
            throw new RuntimeException("Patient konnte nicht angelegt werden.", e);
        }
    }

    public void update(Patient p) {
        String sql = """
                UPDATE patient
                SET first_name=?, last_name=?, birth_date=?, svnr=?, phone=?, reason=?, station_id=?
                WHERE id=?
                """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, p.getFirstName());
            ps.setString(2, p.getLastName());
            ps.setDate(3, p.getBirthDate() == null ? null : Date.valueOf(p.getBirthDate()));
            ps.setString(4, p.getSvnr());
            ps.setString(5, p.getPhone());
            ps.setString(6, p.getReason());
            if (p.getStationId() == null) ps.setNull(7, Types.INTEGER);
            else ps.setInt(7, p.getStationId());
            ps.setInt(8, p.getId());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Patient konnte nicht gespeichert werden.", e);
        }
    }

    public void deleteById(int id) {
        String sql = "DELETE FROM patient WHERE id=?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Patient konnte nicht gelöscht werden.", e);
        }
    }
}
