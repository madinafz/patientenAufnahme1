package org.example;

import org.example.crud.PatientCrud;
import org.example.model.Patient;

import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        try {
            PatientCrud crud = new PatientCrud();

            int before = crud.findAll().size();
            System.out.println("Patienten vorher: " + before);

            // INSERT
            Patient p = new Patient("Test", "User", LocalDate.of(2005, 1, 1), "9999999999");
            p.setPhone("06601234567");
            p.setReason("Test");
            p.setStationId(null);
            crud.insert(p);

            var listAfterInsert = crud.findAll();
            System.out.println("Patienten nach INSERT: " + listAfterInsert.size());

            // UPDATE + DELETE testet man am einfachsten an “letztem” Datensatz:
            Patient last = listAfterInsert.get(listAfterInsert.size() - 1);
            last.setReason("Updated");
            crud.update(last);

            crud.deleteById(last.getId());
            System.out.println("Patient UPDATE+DELETE ok");

        } catch (Exception e) {
            System.out.println("Fehler:");
            e.printStackTrace();
        }
    }
}
