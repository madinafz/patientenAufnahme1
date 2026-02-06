package org.example.kontrolle;

import org.example.crud.PatientCrud;
import org.example.model.Patient;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PatientKontrolle {

    private final PatientCrud crud = new PatientCrud();
    private static final DateTimeFormatter SVNR_DATE = DateTimeFormatter.ofPattern("ddMMyy");

    public List<Patient> getAll() { return crud.findAll(); }
    public List<Patient> search(String query) { return crud.search(query); }

    public List<Patient> sortByName(List<Patient> list) {
        list.sort(Comparator.comparing((Patient p) -> safeLower(p.getLastName()))
                .thenComparing(p -> safeLower(p.getFirstName())));
        return list;
    }

    // ✅ NEU: nur validieren (ohne insert/update)
    public void validateOnly(Patient p) {
        checkPatient(p);
    }

    public void save(Patient p) {
        checkPatient(p);
        if (p.getId() <= 0) crud.insert(p);
        else crud.update(p);
    }

    public void delete(int id) {
        if (id <= 0) throw new IllegalArgumentException("Ungültige ID.");
        crud.deleteById(id);
    }

    private void checkPatient(Patient p) {
        if (p == null) throw new IllegalArgumentException("Patientendaten fehlen.");

        List<String> errors = new ArrayList<>();

        p.setFirstName(capitalize(p.getFirstName()));
        p.setLastName(capitalize(p.getLastName()));

        if (isBlank(p.getFirstName())) errors.add("Vorname fehlt.");
        if (isBlank(p.getLastName())) errors.add("Nachname fehlt.");
        if (p.getBirthDate() == null) errors.add("Geburtsdatum fehlt.");
        if (isBlank(p.getReason())) errors.add("Grund für Aufenthalt fehlt.");

        String svnr = p.getSvnr();
        if (isBlank(svnr) || !svnr.matches("\\d{10}")) {
            errors.add("SVNR muss genau 10 Ziffern haben.");
        } else if (p.getBirthDate() != null) {
            String expected = p.getBirthDate().format(SVNR_DATE);
            String last6 = svnr.substring(4);
            if (!last6.equals(expected)) {
                errors.add("SVNR ungültig: letzte 6 Ziffern müssen dem Geburtsdatum (TTMMJJ) entsprechen.");
            }
        }

        String phone = p.getPhone();
        if (isBlank(phone)) {
            errors.add("Telefonnummer fehlt (muss mit + beginnen, z.B. +436641234567).");
        } else if (!phone.matches("\\+\\d{9,12}")) { // 10–13 Zeichen gesamt
            errors.add("Telefonnummer ungültig: muss mit + beginnen und 10–13 Zeichen lang sein (nur Ziffern nach +).");
        }

        if (p.getStationId() == null) errors.add("Bitte eine Station auswählen.");

        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join("\n", errors));
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private String safeLower(String s) { return s == null ? "" : s.toLowerCase(); }

    private String capitalize(String s) {
        if (s == null) return "";
        s = s.trim().toLowerCase();
        if (s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
