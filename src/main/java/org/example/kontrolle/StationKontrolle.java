package org.example.kontrolle;

import org.example.crud.StationCrud;
import org.example.model.Station;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

    public class StationKontrolle {

        private final StationCrud stationCrud = new StationCrud();

        public List<Station> getAllStations() {
            return stationCrud.findAll();
        }

        // station_id -> stationsname
        public Map<Integer, String> getStationMap() {
            List<Station> stations = stationCrud.findAll();
            Map<Integer, String> map = new HashMap<>();

            for (Station s : stations) {
                map.put(s.getId(), s.getName());
            }
            return map;
        }


}
