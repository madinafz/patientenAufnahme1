package org.example.model;

/**
 * Modelklasse für eine Station
 * <p>
 * Eine Station wird hier über die Raumnummer, den Namen und die maximale Bettenanzahl beschrieben.
 * Die Klasse ist bewusst immutable aufgebaut (nur final Felder) und wird typischerweise für
 * Auswahl- oder Anzeigezwecke verwendet.
 * </p>
 */
public class Station {
    private final int raum;
    private final String name;
    private final int maxBetten;

    /**
     * Erstellt eine neue Station mit allen notwendigen Angaben.
     *
     * @param raum Raumnummer der Station
     * @param name Name/Bezeichnung der Station
     * @param maxBetten maximale Anzahl an Betten in dieser Station
     */
    public Station(int raum, String name, int maxBetten) {
        this.raum = raum;
        this.name = name;
        this.maxBetten = maxBetten;
    }

    /**
     * Gibt die Raumnummer der Station zurück.
     *
     * @return Raumnummer
     */
    public int getRaum() { return raum; }

    /**
     * Gibt den Namen der Station zurück.
     *
     * @return Stationsname
     */
    public String getName() { return name; }

    /**
     * Gibt die maximale Bettenanzahl der Station zurück.
     *
     * @return maximale Bettenanzahl
     */
    public int getMaxBetten() { return maxBetten; }

    /**
     * Gibt die Station als String zurück.
     * <p>
     * Für die Anzeige wird hier nur der Name zurückgegeben, damit z.B. ComboBoxen
     * oder Listen direkt den Stationsnamen anzeigen.
     * </p>
     *
     * @return Stationsname
     */
    @Override
    public String toString() {
        return name;
    }
}
