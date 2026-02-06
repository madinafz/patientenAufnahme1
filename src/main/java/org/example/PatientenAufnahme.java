package org.example;

import org.example.kontrolle.PatientKontrolle;
import org.example.kontrolle.StationKontrolle;
import org.example.model.Patient;
import org.example.model.Station;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class PatientenAufnahme extends JFrame {

    private JPanel panel1;
    private JTextField tfSearch;
    private JButton suchenButton;
    private JButton refresh;
    private JTable tblPatients;
    private JTextArea taDetails;
    private JButton anlegenButton;
    private JButton bearbeitenButton;
    private JButton löschenButton;
    private JLabel Patient;

    private final PatientKontrolle pk = new PatientKontrolle();
    private final StationKontrolle sk = new StationKontrolle();

    private List<Patient> currentPatients = new ArrayList<>();
    private Map<Integer, String> stationMap = Map.of();
    private List<Station> stations = new ArrayList<>();

    private SwingWorker<List<Patient>, Void> loadWorker;
    private String pendingQuery = null;
    private boolean firstLoadDone = false;

    private String prefillFirst = "";
    private String prefillLast = "";

    public PatientenAufnahme() {
        setTitle("Patientenaufnahme");
        setContentPane(panel1);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        if (taDetails != null) taDetails.setEditable(false);

        suchenButton.addActionListener(e -> loadTable(tfSearch.getText(), true));
        refresh.addActionListener(e -> { tfSearch.setText(""); loadTable("", false); });
        tfSearch.addActionListener(e -> loadTable(tfSearch.getText(), true));

        anlegenButton.addActionListener(e -> createPatient());
        bearbeitenButton.addActionListener(e -> editSelectedPatient());
        löschenButton.addActionListener(e -> deleteSelectedPatient());

        tblPatients.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = tblPatients.getSelectedRow();
            if (row < 0 || row >= currentPatients.size()) return;
            showDetails(currentPatients.get(row));
        });

        loadTable("", false);
    }

    private void loadTable(String query, boolean userInitiated) {
        String q = (query == null) ? "" : query.trim();

        if (loadWorker != null && !loadWorker.isDone()) {
            pendingQuery = q;
            showInfo("Daten werden geladen ...\nSuche wird danach ausgeführt: \"" + q + "\"");
            return;
        }

        pendingQuery = null;
        setActionsEnabled(false);

        String cur = (taDetails == null ? "" : taDetails.getText());
        if (cur == null || cur.isBlank() || !cur.startsWith("Bitte kurz warten")) {
            showInfo("Daten werden geladen ...");
        }

        loadWorker = new SwingWorker<>() {
            @Override protected List<Patient> doInBackground() {
                if (stations == null || stations.isEmpty()) {
                    stations = sk.getAllStations();
                    stationMap = sk.getStationMap();
                } else if (stationMap == null || stationMap.isEmpty()) {
                    stationMap = sk.getStationMap();
                }
                return pk.search(q);
            }

            @Override protected void done() {
                try {
                    currentPatients = get();
                    fillTable(currentPatients);
                    clearInfo();

                    boolean isEmpty = currentPatients.isEmpty();
                    if (isEmpty && firstLoadDone && userInitiated && !q.isBlank()) {
                        showNotFoundWithCreate(q);
                    }
                    firstLoadDone = true;

                } catch (Exception ex) {
                    showDbError("Daten konnten nicht geladen werden.", unwrap(ex));
                } finally {
                    setActionsEnabled(true);
                    if (pendingQuery != null) {
                        String next = pendingQuery;
                        pendingQuery = null;
                        loadTable(next, true);
                    }
                }
            }
        };
        loadWorker.execute();
    }

    private void showNotFoundWithCreate(String q) {
        Object[] options = {"Neu anlegen", "OK"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Patient konnte nicht gefunden werden.\n\nSuchbegriff: " + q,
                "Nicht gefunden",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) {
            String[] parts = q.trim().split("\\s+", 2);
            prefillFirst = parts.length > 0 ? parts[0] : "";
            prefillLast  = parts.length > 1 ? parts[1] : "";
            createPatient();
        }
    }

    private void fillTable(List<Patient> patients) {
        String[] cols = {"ID", "Nachname", "Vorname", "Geburtsdatum", "SVNR", "Station", "Grund"};
        Object[][] rows = new Object[patients.size()][cols.length];

        for (int i = 0; i < patients.size(); i++) {
            Patient p = patients.get(i);
            rows[i][0] = p.getId();
            rows[i][1] = p.getLastName();
            rows[i][2] = p.getFirstName();
            rows[i][3] = p.getBirthDate();
            rows[i][4] = p.getSvnr();
            rows[i][5] = (p.getStationId() == null) ? "" : stationMap.getOrDefault(p.getStationId(), "");
            rows[i][6] = p.getReason();
        }

        tblPatients.setModel(new DefaultTableModel(rows, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        });
    }

    private void showDetails(Patient p) {
        taDetails.setText(
                "ID: " + p.getId() + "\n" +
                        "Vorname: " + safe(p.getFirstName()) + "\n" +
                        "Nachname: " + safe(p.getLastName()) + "\n" +
                        "Geburtsdatum: " + p.getBirthDate() + "\n" +
                        "SVNR: " + safe(p.getSvnr()) + "\n" +
                        "Telefon: " + safe(p.getPhone()) + "\n" +
                        "Grund: " + safe(p.getReason()) + "\n" +
                        "Station: " + (p.getStationId() == null ? "" : stationMap.getOrDefault(p.getStationId(), ""))
        );
    }

    private void createPatient() {
        Patient p = showPatientDialogLoop(null);   // ✅ bleibt offen bis korrekt
        prefillFirst = ""; prefillLast = "";
        if (p == null) return;

        runDbAction("Patient wird angelegt ...",
                () -> pk.save(p),
                () -> {
                    showInfo("Erfolg: Patient wurde erfolgreich angelegt.");
                    JOptionPane.showMessageDialog(this, "Patient wurde erfolgreich angelegt.", "Erfolg", JOptionPane.INFORMATION_MESSAGE);

                    showInfo("Bitte kurz warten – Liste wird aktualisiert ...");
                    tfSearch.setText("");
                    loadTable("", false);
                });
    }

    private void editSelectedPatient() {
        Patient old = getSelectedPatientOrWarn();
        if (old == null) return;

        Patient updated = showPatientDialogLoop(old);
        if (updated == null) return;

        updated.setId(old.getId());

        runDbAction("Patient wird gespeichert ...",
                () -> pk.save(updated),
                () -> {
                    JOptionPane.showMessageDialog(this, "Änderungen wurden erfolgreich gespeichert.", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
                    loadTable(tfSearch.getText(), true);
                });
    }

    private void deleteSelectedPatient() {
        Patient p = getSelectedPatientOrWarn();
        if (p == null) return;

        int ok = JOptionPane.showConfirmDialog(
                this,
                "Patient wirklich löschen?\n" + safe(p.getLastName()) + " " + safe(p.getFirstName()),
                "Löschen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (ok != JOptionPane.YES_OPTION) return;

        runDbAction("Patient wird gelöscht ...",
                () -> pk.delete(p.getId()),
                () -> {
                    JOptionPane.showMessageDialog(this, "Patient wurde gelöscht.", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
                    loadTable(tfSearch.getText(), true);
                });
    }

    private Patient getSelectedPatientOrWarn() {
        int row = tblPatients.getSelectedRow();
        if (row < 0 || row >= currentPatients.size()) {
            JOptionPane.showMessageDialog(this, "Bitte zuerst einen Patienten in der Tabelle auswählen.", "Hinweis", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return currentPatients.get(row);
    }

    private void runDbAction(String infoText, Runnable dbWork, Runnable onSuccess) {
        setActionsEnabled(false);
        showInfo(infoText);

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                dbWork.run();
                return null;
            }

            @Override protected void done() {
                try {
                    get();
                    onSuccess.run();
                } catch (Exception ex) {
                    Throwable cause = unwrap(ex);
                    if (cause instanceof IllegalArgumentException) {
                        // ✅ Eingabe-Fehler: Info-Feld nicht „patient wird angelegt…“ stehen lassen
                        showInfo("Eingabe fehlerhaft – bitte korrigieren.");
                        JOptionPane.showMessageDialog(
                                PatientenAufnahme.this,
                                "Bitte prüfen:\n" + cause.getMessage(),
                                "Eingabe fehlerhaft",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        showDbError("Aktion konnte nicht durchgeführt werden.", cause);
                    }
                } finally {
                    setActionsEnabled(true);
                }
            }
        }.execute();
    }

    // ✅ Dialog bleibt offen bis ok (Validierung ohne DB)
    private Patient showPatientDialogLoop(Patient existing) {
        if (stations == null || stations.isEmpty()) {
            stations = sk.getAllStations();
            stationMap = sk.getStationMap();
        }

        // Felder einmal anlegen (bleiben bei Fehler erhalten)
        JTextField tfFirst = new JTextField(existing != null ? safe(existing.getFirstName()) : safe(prefillFirst));
        JTextField tfLast  = new JTextField(existing != null ? safe(existing.getLastName())  : safe(prefillLast));

        JFormattedTextField tfBirth = createBirthField(existing);

        JTextField tfSvnr  = new JTextField(existing == null ? "" : safe(existing.getSvnr()));
        ((AbstractDocument) tfSvnr.getDocument()).setDocumentFilter(new SvnrDocumentFilter());

        String startPhone = (existing != null && existing.getPhone() != null && !existing.getPhone().isBlank())
                ? existing.getPhone() : "+";
        JTextField tfPhone = new JTextField(startPhone);
        ((AbstractDocument) tfPhone.getDocument()).setDocumentFilter(new PlusDigitsFilter(13));

        JTextField tfReason = new JTextField(existing == null ? "" : safe(existing.getReason()));

        JComboBox<Station> cbStation = new JComboBox<>();
        for (Station s : stations) {
            if (s != null && s.getName() != null && !"test".equalsIgnoreCase(s.getName())) cbStation.addItem(s);
        }

        if (existing != null && existing.getStationId() != null) {
            for (int i = 0; i < cbStation.getItemCount(); i++) {
                if (cbStation.getItemAt(i).getId() == existing.getStationId()) {
                    cbStation.setSelectedIndex(i);
                    break;
                }
            }
        }

        JPanel p = new JPanel(new java.awt.GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Vorname:")); p.add(tfFirst);
        p.add(new JLabel("Nachname:")); p.add(tfLast);
        p.add(new JLabel("Geburtsdatum (YYYY-MM-DD):")); p.add(tfBirth);
        p.add(new JLabel("SVNR (10 Ziffern):")); p.add(tfSvnr);
        p.add(new JLabel("Telefon (beginnt mit +):")); p.add(tfPhone);
        p.add(new JLabel("Grund:")); p.add(tfReason);
        p.add(new JLabel("Station:")); p.add(cbStation);

        while (true) {
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
                Station sel = (Station) cbStation.getSelectedItem();
                if (sel != null) out.setStationId(sel.getId());

                // ✅ nur prüfen (keine DB), damit der Dialog offen bleiben kann
                pk.validateOnly(out);

                return out; // ✅ alles ok -> Dialog schließen

            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this,
                        "Bitte korrigieren:\n" + ex.getMessage(),
                        "Eingabe fehlerhaft",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Eingaben sind nicht korrekt. Bitte nochmals prüfen.",
                        "Eingabe fehlerhaft",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private JFormattedTextField createBirthField(Patient existing) {
        try {
            MaskFormatter mf = new MaskFormatter("####-##-##");
            mf.setPlaceholderCharacter('_');
            JFormattedTextField tf = new JFormattedTextField(mf);

            String val = (existing == null || existing.getBirthDate() == null)
                    ? "2000-01-01"
                    : existing.getBirthDate().toString();
            tf.setText(val);
            return tf;
        } catch (ParseException e) {
            JFormattedTextField tf = new JFormattedTextField();
            tf.setText(existing == null || existing.getBirthDate() == null ? "2000-01-01" : existing.getBirthDate().toString());
            return tf;
        }
    }

    private Throwable unwrap(Exception ex) {
        if (ex instanceof ExecutionException && ex.getCause() != null) return ex.getCause();
        return ex;
    }

    private void showDbError(String userText, Throwable ex) {
        JOptionPane.showMessageDialog(this,
                userText + "\n\nBitte Datenbank-Verbindung prüfen und erneut versuchen.",
                "Datenbank-Problem",
                JOptionPane.ERROR_MESSAGE);
        showInfo("Datenbank-Problem – Verbindung prüfen.");
    }

    private void showInfo(String text) { if (taDetails != null) taDetails.setText(text); }
    private void clearInfo() { if (taDetails != null) taDetails.setText(""); }

    private void setActionsEnabled(boolean enabled) {
        tfSearch.setEnabled(true);
        suchenButton.setEnabled(enabled);
        refresh.setEnabled(enabled);
        anlegenButton.setEnabled(enabled);
        bearbeitenButton.setEnabled(enabled);
        löschenButton.setEnabled(enabled);
    }

    private String safe(String s) { return s == null ? "" : s; }

    private static class SvnrDocumentFilter extends DocumentFilter {
        @Override public void insertString(FilterBypass fb, int o, String s, AttributeSet a) throws BadLocationException {
            if (s != null && s.matches("\\d+") && fb.getDocument().getLength() + s.length() <= 10) super.insertString(fb, o, s, a);
            else java.awt.Toolkit.getDefaultToolkit().beep();
        }
        @Override public void replace(FilterBypass fb, int o, int l, String s, AttributeSet a) throws BadLocationException {
            if (s != null && s.matches("\\d+") && fb.getDocument().getLength() - l + s.length() <= 10) super.replace(fb, o, l, s, a);
            else java.awt.Toolkit.getDefaultToolkit().beep();
        }
    }

    private static class PlusDigitsFilter extends DocumentFilter {
        private final int maxLen;
        PlusDigitsFilter(int maxLen) { this.maxLen = maxLen; }

        @Override public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
            if (text == null) return;
            String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
            String next = new StringBuilder(cur).insert(offset, text).toString();
            if (isValid(next)) super.insertString(fb, offset, text, attr);
            else java.awt.Toolkit.getDefaultToolkit().beep();
        }

        @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null) return;
            String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
            String next = new StringBuilder(cur).replace(offset, offset + length, text).toString();
            if (isValid(next)) super.replace(fb, offset, length, text, attrs);
            else java.awt.Toolkit.getDefaultToolkit().beep();
        }

        private boolean isValid(String s) {
            if (s.length() > maxLen) return false;
            return s.isEmpty() || s.equals("+") || s.matches("\\+\\d*");
        }
    }
}
