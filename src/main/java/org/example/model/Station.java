package org.example.model;

public class Station {
    // In DB heißt das Feld jetzt "Raum"
    private final int raum;
    private final String name;
    private final int maxBetten;

    public Station(int raum, String name, int maxBetten) {
        this.raum = raum;
        this.name = name;
        this.maxBetten = maxBetten;
    }

    // ✅ damit euer restlicher Code (getId) NICHT überall geändert werden muss:
    public int getId() { return raum; }          // id == raum (Alias)
    public int getRaum() { return raum; }        // optional, falls ihr es explizit wollt
    public String getName() { return name; }
    public int getMaxBetten() { return maxBetten; }

    @Override
    public String toString() {
        // ✅ Dropdown zeigt jetzt "Raum - Name" (statt nur Name oder nur Zahl)
        return raum + " - " + name;
    }
}
