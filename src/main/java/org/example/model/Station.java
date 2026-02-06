package org.example.model;

public class Station {
    private final int raum;
    private final String name;
    private final int maxBetten;

    public Station(int raum, String name, int maxBetten) {
        this.raum = raum;
        this.name = name;
        this.maxBetten = maxBetten;
    }

    public int getRaum() { return raum; }
    public String getName() { return name; }
    public int getMaxBetten() { return maxBetten; }

    @Override
    public String toString() {
        return name;
    }
}
