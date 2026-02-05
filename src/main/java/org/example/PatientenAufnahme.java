package org.example;

import org.example.kontrolle.PatientKontrolle;
import org.example.kontrolle.StationKontrolle;
import org.example.model.Patient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PatientenAufnahme extends JFrame {

    // ===== Diese Felder werden vom .form (GUI Designer) befüllt =====
    private JPanel panel1;
    private JTextField tfSearch;
    private JButton suchenButton;
    private JButton refresh;
    private JTable tblPatients;
    private JTextArea taDetails;

    private JButton anlegenButton;
    private JButton bearbeitenButton;
    private JButton löschenButton;

    // (Wenn du ein JLabel "Patient" im Form hast: ok, aber wird hier nicht gebraucht)
    private JLabel Patient;

    // ===== Logik-Schicht =====
    private final PatientKontrolle pk = new PatientKontrolle();
    private final StationKontrolle sk = new StationKontrolle();

    private List<Patient> currentPatients = new ArrayList<>();
    private Map<Integer, String> stationMap = Map.of();

    public PatientenAufnahme() {
        setTitle("Patientenaufnahme");
        setContentPane(panel1);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        if (taDetails != null) taDetails.setEditable(false);

        // Suche / Refresh
        suchenButton.addActionListener(e -> loadTable(tfSearch.getText()));
        refresh.addActionListener(e -> {
            tfSearch.setText("");
            loadTable("");
        });
        tfSearch.addActionListener(e -> loadTable(tfSearch.getText())); // Enter

        // CRUD Buttons
        if (anlegenButton != null) anlegenButton.addActionListener(e -> createPatient());
        if (bearbeitenButton != null) bearbeitenButton.addActionListener(e -> editSelectedPatient());
        if (löschenButton != null) löschenButton.addActionListener(e -> deleteSelectedPatient());

        // Klick auf Tabelle -> Details rechts
        tblPatients.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = tblPatients.getSelectedRow();
            if (row < 0 || row >= currentPatients.size()) return;
            showDetails(currentPatients.get(row));
        });

        loadTable("");
    }

    private void loadTable(String query) {
        suchenButton.setEnabled(false);
        refresh.setEnabled(false);

        if (anlegenButton != null) anlegenButton.setEnabled(false);
        if (bearbeitenButton != null) bearbeitenButton.setEnabled(false);
        if (löschenButton != null) löschenButton.setEnabled(false);

        new SwingWorker<List<Patient>, Void>() {
            @Override
            protected List<Patient> doInBackground() {
                stationMap = sk.getStationMap();
                List<Patient> list = pk.search(query);
                return pk.sortByName(list);
            }

            @Override
            protected void done() {
                try {
                    currentPatients = get();
                    fillTable(currentPatients);
                    if (taDetails != null) taDetails.setText("");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            PatientenAufnahme.this,
                            "Fehler beim Laden: " + ex.getMessage(),
                            "Fehler",
                            JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    suchenButton.setEnabled(true);
                    refresh.setEnabled(true);

                    if (anlegenButton != null) anlegenButton.setEnabled(true);
                    if (bearbeitenButton != null) bearbeitenButton.setEnabled(true);
                    if (löschenButton != null) löschenButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void fillTable(List<Patient> patients) {
        String[] cols = {"ID", "Nachname", "Vorname", "Geburtsdatum", "SVNR", "Station"};
        Object[][] rows = new Object[patients.size()][cols.length];

        for (int i = 0; i < patients.size(); i++) {
            Patient p = patients.get(i);

            rows[i][0] = p.getId();
            rows[i][1] = p.getLastName();
            rows[i][2] = p.getFirstName();
            rows[i][3] = (p.getBirthDate() == null ? "" : p.getBirthDate().toString());
            rows[i][4] = p.getSvnr();

            String stationName = "";
            if (p.getStationId() != null) {
                stationName = stationMap.getOrDefault(p.getStationId(), "");
            }
            rows[i][5] = stationName;
        }

        tblPatients.setModel(new DefaultTableModel(rows, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
    }

    private void showDetails(Patient p) {
        String stationName = "";
        if (p.getStationId() != null) {
            stationName = stationMap.getOrDefault(p.getStationId(), "");
        }

        taDetails.setText(
                "ID: " + p.getId() + "\n" +
                        "Vorname: " + safe(p.getFirstName()) + "\n" +
                        "Nachname: " + safe(p.getLastName()) + "\n" +
                        "Geburtsdatum: " + (p.getBirthDate() == null ? "" : p.getBirthDate()) + "\n" +
                        "SVNR: " + safe(p.getSvnr()) + "\n" +
                        "Telefon: " + safe(p.getPhone()) + "\n" +
                        "Grund: " + safe(p.getReason()) + "\n" +
                        "Station: " + stationName + "\n"
        );
    }

    // =========================
    // CRUD: Anlegen/Bearbeiten/Löschen
    // =========================

    private void createPatient() {
        Patient p = showPatientDialog(null);
        if (p == null) return;

        try {
            pk.save(p); // insert (id <= 0)
            JOptionPane.showMessageDialog(this, "Patient wurde angelegt.");
            loadTable("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Fehler: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editSelectedPatient() {
        int row = tblPatients.getSelectedRow();
        if (row < 0 || row >= currentPatients.size()) {
            JOptionPane.showMessageDialog(this, "Bitte zuerst einen Patienten auswählen.");
            return;
        }

        Patient selected = currentPatients.get(row);
        Patient updated = showPatientDialog(selected);
        if (updated == null) return;

        // ID übernehmen => UPDATE
        updated.setId(selected.getId());

        try {
            pk.save(updated); // update (id > 0)
            JOptionPane.showMessageDialog(this, "Patient wurde bearbeitet.");
            loadTable("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Fehler: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedPatient() {
        int row = tblPatients.getSelectedRow();
        if (row < 0 || row >= currentPatients.size()) {
            JOptionPane.showMessageDialog(this, "Bitte zuerst einen Patienten auswählen.");
            return;
        }

        Patient selected = currentPatients.get(row);

        int ok = JOptionPane.showConfirmDialog(
                this,
                "Wirklich löschen?\n" + safe(selected.getLastName()) + " " + safe(selected.getFirstName()),
                "Löschen",
                JOptionPane.YES_NO_OPTION
        );
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            pk.delete(selected.getId());
            JOptionPane.showMessageDialog(this, "Patient wurde gelöscht.");
            loadTable("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Fehler: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Dialog für Neu + Bearbeiten
    private Patient showPatientDialog(Patient existing) {
        JTextField tfFirst = new JTextField(existing == null ? "" : safe(existing.getFirstName()));
        JTextField tfLast  = new JTextField(existing == null ? "" : safe(existing.getLastName()));
        JTextField tfBirth = new JTextField(existing == null || existing.getBirthDate() == null
                ? "2000-01-01" : existing.getBirthDate().toString());
        JTextField tfSvnr  = new JTextField(existing == null ? "" : safe(existing.getSvnr()));
        JTextField tfPhone = new JTextField(existing == null ? "" : safe(existing.getPhone()));
        JTextField tfReason= new JTextField(existing == null ? "" : safe(existing.getReason()));
        JTextField tfStationId = new JTextField(existing == null || existing.getStationId() == null
                ? "" : String.valueOf(existing.getStationId()));

        JPanel p = new JPanel(new java.awt.GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Vorname:")); p.add(tfFirst);
        p.add(new JLabel("Nachname:")); p.add(tfLast);
        p.add(new JLabel("Geburtsdatum (YYYY-MM-DD):")); p.add(tfBirth);
        p.add(new JLabel("SVNR (10 Ziffern):")); p.add(tfSvnr);
        p.add(new JLabel("Telefon:")); p.add(tfPhone);
        p.add(new JLabel("Grund:")); p.add(tfReason);
        p.add(new JLabel("Station ID:")); p.add(tfStationId);

        int res = JOptionPane.showConfirmDialog(
                this, p,
                existing == null ? "Patient anlegen" : "Patient bearbeiten",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (res != JOptionPane.OK_OPTION) return null;

        try {
            Patient out = new Patient();
            out.setFirstName(tfFirst.getText());
            out.setLastName(tfLast.getText());
            out.setBirthDate(LocalDate.parse(tfBirth.getText()));
            out.setSvnr(tfSvnr.getText());
            out.setPhone(tfPhone.getText());
            out.setReason(tfReason.getText());

            String st = tfStationId.getText();
            if (st != null && !st.isEmpty()) out.setStationId(Integer.parseInt(st));

            return out;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Eingabe falsch: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
