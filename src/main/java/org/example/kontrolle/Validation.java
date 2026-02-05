package org.example.kontrolle;

import org.example.model.Patient;

import java.util.regex.Pattern;

    public class Validation {

        private static final Pattern SVNR_10_DIGITS = Pattern.compile("^\\d{10}$");
        private static final Pattern PHONE = Pattern.compile("^[0-9 +/()-]{6,20}$");

        public void validatePatient(Patient p) {
            if (p == null) throw new IllegalArgumentException("Patient ist null");

            if (isBlank(p.getFirstName())) throw new IllegalArgumentException("Vorname fehlt");
            if (isBlank(p.getLastName())) throw new IllegalArgumentException("Nachname fehlt");
            if (p.getBirthDate() == null) throw new IllegalArgumentException("Geburtsdatum fehlt");

            if (isBlank(p.getSvnr()) || !SVNR_10_DIGITS.matcher(p.getSvnr()).matches()) {
                throw new IllegalArgumentException("SVNR muss genau 10 Ziffern haben");
            }

            if (!isBlank(p.getPhone()) && !PHONE.matcher(p.getPhone()).matches()) {
                throw new IllegalArgumentException("Telefonnummer ist ungültig");
            }
        }

        private boolean isBlank(String s) {
            return s == null || s.trim().isEmpty();
        }


}
