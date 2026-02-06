package org.example.kontrolle;

import org.example.crud.StationCrud;
import org.example.model.Station;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StationKontrolle {
    private final StationCrud crud = new StationCrud();

    public List<Station> getAllStations() {
        return crud.findAll();
    }

    // ✅ Map: station_id (bei euch jetzt Raum-Wert) -> "Raum - Name"
    public Map<Integer, String> getStationMap() {
        Map<Integer, String> map = new HashMap<>();
        for (Station s : getAllStations()) {
            map.put(s.getId(), s.getRaum() + " - " + s.getName());
        }
        return map;
    }
}
