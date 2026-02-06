package org.example.crud;

import org.example.DB;
import org.example.model.Station;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StationCrud {

    public List<Station> findAll() {
        // ✅ Raum wird als id „gemappt“, damit alter Code weiter funktioniert
        String sql = "SELECT Raum AS id, name, max_betten FROM station ORDER BY name";
        List<Station> out = new ArrayList<>();

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(new Station(
                        rs.getInt("id"),             // = Raum
                        rs.getString("name"),
                        rs.getInt("max_betten")
                ));
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("Stationen konnten nicht geladen werden.", e);
        }
    }
}
