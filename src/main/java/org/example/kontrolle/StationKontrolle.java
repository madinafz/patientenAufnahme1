package org.example.kontrolle;

import org.example.model.Station;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StationKontrolle {
    public List<Station> getAllStations() {
        return new org.example.crud.StationCrud().findAll();
    }

    public Map<Integer, String> getStationMap() {
        Map<Integer, String> map = new HashMap<>();
        for (Station s : getAllStations()) {
            map.put(s.getRaum(), s.getName());
        }
        return map;
    }
}
