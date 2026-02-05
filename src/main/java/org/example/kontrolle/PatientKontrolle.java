package org.example.kontrolle;

import org.example.crud.PatientCrud;
import org.example.model.Patient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

    public class PatientKontrolle {

        private final PatientCrud patientCrud;
        private final Validation validation;

        public PatientKontrolle() {
            this.patientCrud = new PatientCrud();
            this.validation = new Validation();
        }

        public List<Patient> getAll() {
            return patientCrud.findAll();
        }

        // optional: Suche in-memory (nutzt findAll)
        public List<Patient> search(String query) {
            String q = query == null ? "" : query.trim().toLowerCase();
            List<Patient> all = patientCrud.findAll();
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

        public List<Patient> sortByLastName(List<Patient> list) {
            List<Patient> copy = new ArrayList<>(list);
            copy.sort(Comparator.comparing((Patient p) -> safe(p.getLastName()))
                    .thenComparing(p -> safe(p.getFirstName())));
            return copy;
        }

        // entscheidet insert/update
        public void save(Patient p) {
            validation.validatePatient(p);
            if (p.getId() <= 0) patientCrud.insert(p);
            else patientCrud.update(p);
        }

        public void deleteById(int id) {
            if (id <= 0) throw new IllegalArgumentException("Ungültige ID: " + id);
            patientCrud.deleteById(id);
        }

        private boolean contains(String field, String q) {
            return field != null && field.toLowerCase().contains(q);
        }

        private String safe(String s) {
            return s == null ? "" : s.toLowerCase();
        }


}
