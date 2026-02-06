package org.example.kontrolle;

import org.example.crud.PatientCrud;
import org.example.model.Patient;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Kontroll- und Validierungslogik für Patienten.
 * <p>
 * Diese Klasse liegt zwischen UI/Anwendung und Datenbankzugriff und übernimmt:
 * Validierung der Eingaben, Normalisierung von Textfeldern sowie das Weiterleiten
 * der CRUD-Operationen an {@link PatientCrud}.
 * </p>
 */
public class PatientKontrolle {

    /**
     * Zugriffsschicht für Datenbankoperationen rund um Patienten.
     */
    private final PatientCrud crud = new PatientCrud();

    /**
     * Formatierer für die letzten 6 Stellen der SVNR anhand des Geburtsdatums (TTMMJJ).
     */
    private static final DateTimeFormatter SVNR_DATE = DateTimeFormatter.ofPattern("ddMMyy");

    /**
     * Führt eine Suche nach Patienten durch.
     * <p>
     * Die eigentliche Suche wird an {@link PatientCrud#search(String)} delegiert.
     * </p>
     *
     * @param query Suchbegriff (kann leer oder {@code null} sein)
     * @return Liste der gefundenen Patienten
     */
    public List<Patient> search(String query) {
        return crud.search(query);
    }

    /**
     * Prüft einen Patienten nur auf Gültigkeit, ohne ihn zu speichern.
     * <p>
     * Diese Methode ist z.B. praktisch, wenn man vor dem Speichern in der UI
     * nur die Fehlerliste sehen möchte.
     * </p>
     *
     * @param p Patient-Objekt, das geprüft werden soll
     * @throws IllegalArgumentException wenn Validierungsfehler auftreten
     */
    public void validateOnly(Patient p) {
        checkPatient(p);
    }

    /**
     * Speichert einen Patienten in der Datenbank.
     * <p>
     * Vor dem Speichern wird {@link #checkPatient(Patient)} aufgerufen.
     * Wenn die ID noch nicht gesetzt ist (<= 0), wird ein Insert gemacht,
     * ansonsten ein Update.
     * </p>
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
     * Löscht einen Patienten anhand seiner ID.
     *
     * @param id ID des zu löschenden Patienten
     */
    public void delete(int id) {
        crud.deleteById(id);
    }

    /**
     * Validiert und normalisiert die Patientendaten.
     * <p>
     * Es werden Pflichtfelder geprüft, einige Textfelder normalisiert (erster Buchstabe groß,
     * Rest klein) und zusätzlich die SVNR sowie Telefonnummer anhand einfacher Regeln geprüft.
     * Bei Fehlern werden alle Meldungen gesammelt und als {@link IllegalArgumentException}
     * zurückgegeben.
     * </p>
     *
     * @param p Patient-Objekt, das geprüft werden soll
     * @throws IllegalArgumentException wenn der Patient {@code null} ist oder Validierungsfehler auftreten
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
     * Prüft, ob ein String leer ist (inklusive {@code null}).
     *
     * @param s zu prüfender String
     * @return {@code true}, wenn {@code null} oder leer, sonst {@code false}
     */
    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Normalisiert einen Text so, dass der erste Buchstabe groß ist und der Rest klein.
     * <p>
     * Wenn der String {@code null} oder leer ist, wird ein leerer String zurückgegeben.
     * </p>
     *
     * @param s Eingabetext
     * @return normalisierte Schreibweise
     */
    private String firstUpperRestLower(String s) {
        if (s == null || s.isEmpty()) return "";
        String x = s.toLowerCase();
        return Character.toUpperCase(x.charAt(0)) + x.substring(1);
    }
}
