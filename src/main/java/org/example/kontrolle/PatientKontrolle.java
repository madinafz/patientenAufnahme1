package org.example.kontrolle;

import org.example.crud.PatientCrud;
import org.example.model.Patient;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PatientKontrolle {

    private final PatientCrud crud = new PatientCrud();
    private static final DateTimeFormatter SVNR_DATE = DateTimeFormatter.ofPattern("ddMMyy");

    public List<Patient> search(String query) {
        return crud.search(query);
    }

    public void validateOnly(Patient p) {
        checkPatient(p);
    }

    public void save(Patient p) {
        checkPatient(p);
        if (p.getId() <= 0) crud.insert(p);
        else crud.update(p);
    }

    public void delete(int id) {
        crud.deleteById(id);
    }

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

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private String firstUpperRestLower(String s) {
        if (s == null || s.isEmpty()) return "";
        String x = s.toLowerCase();
        return Character.toUpperCase(x.charAt(0)) + x.substring(1);
    }
}
