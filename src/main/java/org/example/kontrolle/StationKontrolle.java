package org.example.kontrolle;

import org.example.model.Station;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kontrollklasse für Stationen.
 * <p>
 * Diese Klasse stellt einfache Methoden bereit, um Stationsdaten zu laden und sie
 * in einer Form aufzubereiten, die in der UI leicht verwendet werden kann (z.B. als Map).
 * </p>
 */
public class StationKontrolle {

    /**
     * Lädt alle Stationen aus der Datenbank.
     * <p>
     * Der Datenbankzugriff wird an die CRUD-Klasse delegiert.
     * </p>
     *
     * @return Liste aller Stationen
     */
    public List<Station> getAllStations() {
        return new org.example.crud.StationCrud().findAll();
    }

    /**
     * Erstellt eine Map aus Stationsraum und Stationsname.
     * <p>
     * Der Schlüssel ist die Raumnummer ({@code raum}), der Wert ist der Name der Station.
     * Das ist praktisch für Dropdowns oder Auswahlfelder, wo man eine ID/Nummer anzeigen
     * oder speichern möchte.
     * </p>
     *
     * @return Map mit Raum als Key und Stationsname als Value
     */
    public Map<Integer, String> getStationMap() {
        Map<Integer, String> map = new HashMap<>();
        for (Station s : getAllStations()) {
            map.put(s.getRaum(), s.getName());
        }
        return map;
    }
}
