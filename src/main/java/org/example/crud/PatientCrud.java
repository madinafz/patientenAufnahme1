package org.example.crud;

import org.example.model.Patient;
import org.example.DB;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class PatientCrud {

    public List<Patient> findAll() {
        String sql = """
                SELECT id, first_name, last_name, birth_date, svnr, phone, reason, station_id
                FROM patient
                ORDER BY id
                """;

        List<Patient> list = new ArrayList<>();

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Patient p = new Patient();
                p.setId(rs.getInt("id"));
                p.setFirstName(rs.getString("first_name"));
                p.setLastName(rs.getString("last_name"));

                Date bd = rs.getDate("birth_date");
                p.setBirthDate(bd == null ? null : bd.toLocalDate());

                p.setSvnr(rs.getString("svnr"));
                p.setPhone(rs.getString("phone"));
                p.setReason(rs.getString("reason"));

                int st = rs.getInt("station_id");
                p.setStationId(rs.wasNull() ? null : st);

                list.add(p);
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("DB-Fehler: Patienten konnten nicht geladen werden", e);
        }
    }

    public void insert(Patient p) {
        String sql = """
                INSERT INTO patient (first_name, last_name, birth_date, svnr, phone, reason, station_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, p.getFirstName());
            ps.setString(2, p.getLastName());

            if (p.getBirthDate() == null) ps.setNull(3, Types.DATE);
            else ps.setDate(3, Date.valueOf(p.getBirthDate()));

            ps.setString(4, p.getSvnr());
            ps.setString(5, p.getPhone());
            ps.setString(6, p.getReason());

            if (p.getStationId() == null) ps.setNull(7, Types.INTEGER);
            else ps.setInt(7, p.getStationId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("DB-Fehler: Patient konnte nicht eingefügt werden", e);
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

            if (p.getBirthDate() == null) ps.setNull(3, Types.DATE);
            else ps.setDate(3, Date.valueOf(p.getBirthDate()));

            ps.setString(4, p.getSvnr());
            ps.setString(5, p.getPhone());
            ps.setString(6, p.getReason());

            if (p.getStationId() == null) ps.setNull(7, Types.INTEGER);
            else ps.setInt(7, p.getStationId());

            ps.setInt(8, p.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("DB-Fehler: Patient konnte nicht aktualisiert werden", e);
        }
    }

    public void deleteById(int id) {
        String sql = "DELETE FROM patient WHERE id=?";

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("DB-Fehler: Patient konnte nicht gelöscht werden", e);
        }
    }
}
