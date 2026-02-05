package org.example.kontrolle;

import org.example.crud.PatientCrud;
import org.example.model.Patient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PatientKontrolle {

    private final PatientCrud crud = new PatientCrud();

    public List<Patient> getAll() {
        return crud.findAll();
    }

    // Speichern: id <= 0 => insert, sonst update
    public void save(Patient p) {
        checkPatient(p);
        if (p.getId() <= 0) crud.insert(p);
        else crud.update(p);
    }

    public void delete(int id) {
        if (id <= 0) throw new IllegalArgumentException("Ungültige ID");
        crud.deleteById(id);
    }

    // Suche ohne trim(): nur toLowerCase()
    public List<Patient> search(String query) {
        String q = (query == null) ? "" : query.toLowerCase();

        List<Patient> all = crud.findAll();
        if (q.isEmpty()) return all;

        List<Patient> out = new ArrayList<>();
        for (Patient p : all) {
            if (contains(p.getFirstName(), q) ||
                    contains(p.getLastName(), q) ||
                    contains(p.getSvnr(), q) ||
                    contains(p.getPhone(), q) ||
                    contains(p.getReason(), q)) {
                out.add(p);
            }
        }
        return out;
    }

    // Sortieren: Nachname, dann Vorname
    public List<Patient> sortByName(List<Patient> list) {
        List<Patient> copy = new ArrayList<>(list);
        copy.sort(Comparator
                .comparing((Patient p) -> safe(p.getLastName()))
                .thenComparing(p -> safe(p.getFirstName())));
        return copy;
    }

    // =========================
    // Validation
    // =========================
    private void checkPatient(Patient p) {
        if (p == null) throw new IllegalArgumentException("Patient ist null");

        if (isBlank(p.getFirstName())) throw new IllegalArgumentException("Vorname fehlt");
        if (isBlank(p.getLastName())) throw new IllegalArgumentException("Nachname fehlt");
        if (p.getBirthDate() == null) throw new IllegalArgumentException("Geburtsdatum fehlt");

        // --- SVNR: 10 Ziffern + letzte 6 Ziffern müssen TTMMJJ vom Geburtsdatum sein ---
        String svnr = p.getSvnr();
        if (isBlank(svnr) || !svnr.matches("\\d{10}")) {
            throw new IllegalArgumentException("SVNR muss 10 Ziffern haben");
        }

        // letzte 6 Ziffern (Index 4..9)
        String svnrDatePart = svnr.substring(4);

        // Geburtsdatum -> TTMMJJ
        String birthPart = String.format("%02d%02d%02d",
                p.getBirthDate().getDayOfMonth(),
                p.getBirthDate().getMonthValue(),
                p.getBirthDate().getYear() % 100
        );

        if (!svnrDatePart.equals(birthPart)) {
            throw new IllegalArgumentException("SVNR stimmt nicht mit dem Geburtsdatum überein (TTMMJJ)");
        }

        // --- Telefon: muss mit 0 ODER mit +Vorwahl beginnen ---
        String phone = p.getPhone();
        if (!isBlank(phone) &&
                !phone.matches("^(0[0-9][0-9 +/()\\-]{4,18}|\\+[1-9][0-9]{1,3}[0-9 +/()\\-]{4,18})$")) {
            throw new IllegalArgumentException("Telefonnummer muss mit 0 oder mit internationaler Vorwahl (+..) beginnen");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isEmpty(); // ohne trim()
    }

    private boolean contains(String field, String q) {
        return field != null && field.toLowerCase().contains(q);
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase();
    }
}