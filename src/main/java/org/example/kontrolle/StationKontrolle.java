package org.example.kontrolle;

import org.example.crud.StationCrud;
import org.example.model.Station;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StationKontrolle {

    private final StationCrud crud = new StationCrud();

    public List<Station> getAll() {
        return crud.findAll();
    }
    System.out.println("Hell");

    // Optional: station_id -> station_name (für GUI)
    public Map<Integer, String> getStationMap() {
        List<Station> stations = crud.findAll();
        Map<Integer, String> map = new HashMap<>();
        for (Station s : stations) {
            map.put(s.getId(), s.getName());
        }
        return map;
    }

    // Optional: Suche ohne trim(): nur toLowerCase()
    public List<Station> search(String query) {
        String q = (query == null) ? "" : query.toLowerCase();

        List<Station> all = crud.findAll();
        if (q.isEmpty()) return all;

        List<Station> out = new java.util.ArrayList<>();
        for (Station s : all) {
            String name = s.getName();
            if (name != null && name.toLowerCase().contains(q)) out.add(s);
        }
        return out;
    }
}
