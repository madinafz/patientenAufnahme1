package org.example.kontrolle;

import org.example.crud.PatientCrud;
import org.example.model.Patient;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Kontroll- und Validierungslogik für Patienten.
 * Diese Klasse liegt zwischen UI/Anwendung und Datenbankzugriff und übernimmt:
 * Validierung der Eingaben, Normalisierung von Textfeldern sowie das weiterleiten
 * der CRUDoperationen
 */
public class PatientKontrolle {

    /**
     * Zum Zugreifen für Datenbanken operationen rund um Patienten.
     */
    private final PatientCrud crud = new PatientCrud();

    /**
     * Formatiert für die letzten 6 Ziffern der SVNR anhand des Geburtsdatums
     */
    private static final DateTimeFormatter SVNR_DATE = DateTimeFormatter.ofPattern("ddMMyy");

    /**
     * führt Suche nach Patienten durch.
     * eigentliche Suche wird an PatientCrud weitergegeben.
     *
     * @param query Suchbegriff
     * @return Liste der gefundenen Patienten
     */
    public List<Patient> search(String query) {

        return crud.search(query);
    }

    /**
     * Prüft einen Patienten nur auf Gültigkeit, ohne zu speichern
     * Die Methode ist praktisch, wenn man vor dem Speichern in der UI
     * nur Fehlerliste sehen möchte
     *
     * @param p Patient-Objekt, dass geprüft werden soll
     * @throws IllegalArgumentException wenn Validierungsfehler auftreten
     */
    public void validateOnly(Patient p) {

        checkPatient(p);
    }

    /**
     * Speichert einen Patienten in der Datenbank
     * Vor dem Speichern wird checkPatient(Patient) aufgerufe
     * wenn ID noch nicht gesetzt ist (<= 0), wird Insert gemacht,
     * sonst ein Update
     *
     * @param p Patient-Objekt mit den zu speichernden Daten
     * @throws IllegalArgumentException wenn Validierungsfehler auftreten
     */
    public void save(Patient p) {
        checkPatient(p);
        if (p.getId() <= 0) crud.insert(p);
        else crud.update(p);
    }

    /**
     * Löscht einen Patienten wegen ID
     *
     * @param id ID vom zu löschenden Patienten
     */
    public void delete(int id) {

        crud.deleteById(id);
    }

    /**
     * Validiert und normalisiert die Patientendaten
     * Es werden Pflichtfelder geprüft, einige Textfelder normalisiert (1.ster Buchstabe groß,
     * Rest klein) und auch die SVNR sowie Telefonnummer anhand einfacher Regeln geprüft
     * Bei Fehlern werden alle Meldungen gesammelt und zrk.gegeben
     *
     * @param p Patient-Objekt, das geprüft werden soll
     * @throws IllegalArgumentException wenn der Patient null ist oder Validierungsfehler auftreten
     */
    private void checkPatient(Patient p) {
        if (p == null) throw new IllegalArgumentException("Patientendaten fehlen.");

        List<String> errors = new ArrayList<>();

        p.setFirstName(firstUpperRestLower(p.getFirstName()));
        p.setLastName(firstUpperRestLower(p.getLastName()));
        p.setReason(firstUpperRestLower(p.getReason()));
        p.setAddress(firstUpperRestLower(p.getAddress()));

        if (isEmpty(p.getFirstName())) errors.add("Vorname fehlt.");
        if (isEmpty(p.getLastName())) errors.add("Nachname fehlt.");
        if (p.getBirthDate() == null) errors.add("Geburtsdatum fehlt.");
        if (isEmpty(p.getSvnr())) errors.add("SVNR fehlt.");
        if (isEmpty(p.getPhone())) errors.add("Telefonnummer fehlt.");
        if (isEmpty(p.getReason())) errors.add("Grund für Aufenthalt fehlt.");
        if (isEmpty(p.getAddress())) errors.add("Die Adresse fehlt – bitte eintragen.");
        if (p.getStationId() == null) errors.add("Bitte eine Station auswählen.");

        String svnr = p.getSvnr();
        if (!isEmpty(svnr) && !svnr.matches("\\d{10}")) {
            errors.add("SVNR muss genau 10 Ziffern haben.");
        } else if (!isEmpty(svnr) && p.getBirthDate() != null) {
            String expected = p.getBirthDate().format(SVNR_DATE);
            String last6 = svnr.substring(4);
            if (!last6.equals(expected)) {
                errors.add("SVNR ungültig: letzte 6 Ziffern müssen dem Geburtsdatum (TTMMJJ) entsprechen.");
            }
        }

        String phone = p.getPhone();
        if (!isEmpty(phone) && !phone.matches("\\+\\d{9,12}")) {
            errors.add("Telefonnummer ungültig: muss mit + beginnen und 10–13 Zeichen lang sein.");
        }

        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join("\n", errors));
    }

    /**
     * ürüft, ob ein String leer ist
     * @param s zu prüfender String
     * @return, wenn leer, entweder true oder false
     */
    private boolean isEmpty(String s) {

        return s == null || s.isEmpty();
    }

    /**
     * Schreibt einen Text so, dass der 1.ste Buchstabe groß ist und der Rest klein.
     * Wenn der String nicht gespeichert oder leer ist, wird ein leerer String zurückgegeben.
     * @param s Eingabetext
     * @return normalisierte Schreibweise
     */
    private String firstUpperRestLower(String s) {
        if (s == null || s.isEmpty()) return "";
        String x = s.toLowerCase();
        return Character.toUpperCase(x.charAt(0)) + x.substring(1);
    }
}
