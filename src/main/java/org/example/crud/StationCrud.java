package org.example.crud;

import org.example.model.Station;
import org.example.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StationCrud {

        public List<Station> findAll() {
            String sql = "SELECT id, name, max_betten FROM station ORDER BY id";
            List<Station> list = new ArrayList<>();

            try (Connection con = DB.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    list.add(new Station(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("max_betten")
                    ));
                }
                return list;

            } catch (SQLException e) {
                throw new RuntimeException("DB-Fehler: Stationen konnten nicht geladen werden", e);
            }
        }


}
