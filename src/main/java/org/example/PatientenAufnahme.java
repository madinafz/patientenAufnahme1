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

/**
 * Hauptfenster für die Patientenaufnahme.
 * Man kann suchen, anzeigen, anlegen, bearbeiten und löschen.
 */
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
    private List<Station> stations = new ArrayList<>();
    private Map<Integer, String> stationMap = Map.of();

    private SwingWorker<List<Patient>, Void> loadWorker;
    private String pendingQuery = null;

    private boolean firstLoadDone = false;
    private String prefillFirst = "";
    private String prefillLast = "";

    /**
     * Baut das Fenster auf und lädt die Liste.
     */
    public PatientenAufnahme() {
        setTitle("Patientenaufnahme");
        setContentPane(panel1);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        if (taDetails != null) taDetails.setEditable(false);

        suchenButton.addActionListener(e -> loadTable(tfSearch.getText(), true));
        tfSearch.addActionListener(e -> loadTable(tfSearch.getText(), true));
        refresh.addActionListener(e -> {
            tfSearch.setText("");
            loadTable("", false);
        });

        anlegenButton.addActionListener(e -> createPatient());
        bearbeitenButton.addActionListener(e -> editSelectedPatient());
        löschenButton.addActionListener(e -> deleteSelectedPatient());

        tblPatients.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            Patient p = getSelectedPatientSilent();
            if (p != null) showDetails(p);
        });

        loadTable("", false);
    }

    /**
     * Lädt Patienten im Hintergrund und füllt die Tabelle.
     * Wenn gerade geladen wird, wird die Suche gemerkt.
     */
    private void loadTable(String query, boolean userInitiated) {
        String q = query == null ? "" : query;

        if (loadWorker != null && !loadWorker.isDone()) {
            pendingQuery = q;
            showInfo("Daten werden geladen …\nSuche kommt danach: \"" + q + "\"");
            return;
        }

        pendingQuery = null;
        setActionsEnabled(false);
        showInfo("Daten werden geladen …");

        loadWorker = new SwingWorker<>() {
            @Override
            protected List<Patient> doInBackground() {
                ensureStationsLoaded();
                return pk.search(q);
            }

            @Override
            protected void done() {
                try {
                    currentPatients = get();
                    fillTable(currentPatients);
                    clearInfo();

                    if (currentPatients.isEmpty() && firstLoadDone && userInitiated && !q.isEmpty()) {
                        showNotFoundWithCreate(q);
                    }
                    firstLoadDone = true;

                } catch (Exception ex) {
                    showDbError("Daten konnten nicht geladen werden.", unwrap(ex));
                } finally {
                    setActionsEnabled(true);
                    runPendingQuery();
                }
            }
        };

        loadWorker.execute();
    }

    /**
     * Lädt Stationen und die Map, wenn sie noch fehlen.
     */
    private void ensureStationsLoaded() {
        if (stations == null || stations.isEmpty()) stations = sk.getAllStations();
        if (stationMap == null || stationMap.isEmpty()) stationMap = sk.getStationMap();
    }

    /**
     * Führt eine gemerkte Suche aus (nach dem Laden).
     */
    private void runPendingQuery() {
        if (pendingQuery == null) return;
        String next = pendingQuery;
        pendingQuery = null;
        loadTable(next, true);
    }

    /**
     * Zeigt “Nicht gefunden” und bietet “Neu anlegen” an.
     */
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

        if (choice != 0) return;

        String[] parts = q.trim().split("\\s+", 2);
        prefillFirst = parts.length > 0 ? parts[0] : "";
        prefillLast = parts.length > 1 ? parts[1] : "";
        createPatient();
    }

    /**
     * Schreibt alle Patienten in die Tabelle.
     */
    private void fillTable(List<Patient> patients) {
        String[] cols = {"Patient-ID", "Raum", "Nachname", "Vorname", "Geburtsdatum", "SVNR", "Telefon", "Adresse", "Station", "Grund"};
        Object[][] rows = new Object[patients.size()][cols.length];

        for (int i = 0; i < patients.size(); i++) {
            Patient p = patients.get(i);
            Integer raum = p.getStationId();

            rows[i][0] = p.getId();
            rows[i][1] = raum == null ? "" : raum;
            rows[i][2] = p.getLastName();
            rows[i][3] = p.getFirstName();
            rows[i][4] = p.getBirthDate();
            rows[i][5] = p.getSvnr();
            rows[i][6] = p.getPhone();
            rows[i][7] = p.getAddress();
            rows[i][8] = raum == null ? "" : stationMap.getOrDefault(raum, "");
            rows[i][9] = p.getReason();
        }

        tblPatients.setModel(new DefaultTableModel(rows, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        });
    }

    /**
     * Zeigt die Details vom Patienten rechts an.
     */
    private void showDetails(Patient p) {
        Integer raum = p.getStationId();

        taDetails.setText(
                "Patient-ID: " + p.getId() + "\n" +
                        "Raum: " + (raum == null ? "" : raum) + "\n" +
                        "Vorname: " + safe(p.getFirstName()) + "\n" +
                        "Nachname: " + safe(p.getLastName()) + "\n" +
                        "Geburtsdatum: " + p.getBirthDate() + "\n" +
                        "SVNR: " + safe(p.getSvnr()) + "\n" +
                        "Telefon: " + safe(p.getPhone()) + "\n" +
                        "Adresse: " + safe(p.getAddress()) + "\n" +
                        "Station: " + (raum == null ? "" : stationMap.getOrDefault(raum, "")) + "\n" +
                        "Grund: " + safe(p.getReason())
        );
    }

    /**
     * Öffnet den Dialog und legt einen Patienten an.
     */
    private void createPatient() {
        Patient p = showPatientDialogLoop(null);
        prefillFirst = "";
        prefillLast = "";
        if (p == null) return;

        runDbAction(
                "Patient wird angelegt …",
                () -> pk.save(p),
                () -> {
                    JOptionPane.showMessageDialog(this, "Patient wurde erfolgreich angelegt.", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
                    showInfo("Bitte kurz warten – Liste wird aktualisiert …");
                    tfSearch.setText("");
                    loadTable("", false);
                }
        );
    }

    /**
     * Öffnet den Dialog und speichert Änderungen.
     */
    private void editSelectedPatient() {
        Patient old = getSelectedPatientOrWarn();
        if (old == null) return;

        Patient updated = showPatientDialogLoop(old);
        if (updated == null) return;

        updated.setId(old.getId());

        runDbAction(
                "Änderungen werden gespeichert …",
                () -> pk.save(updated),
                () -> {
                    JOptionPane.showMessageDialog(this, "Änderungen wurden gespeichert.", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
                    loadTable(tfSearch.getText(), true);
                }
        );
    }

    /**
     * Löscht den ausgewählten Patienten nach Bestätigung.
     */
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

        runDbAction(
                "Patient wird gelöscht …",
                () -> pk.delete(p.getId()),
                () -> {
                    JOptionPane.showMessageDialog(this, "Patient wurde gelöscht.", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
                    loadTable(tfSearch.getText(), true);
                }
        );
    }

    /**
     * Gibt den ausgewählten Patienten zurück.
     * Wenn keiner gewählt ist, kommt eine Meldung.
     */
    private Patient getSelectedPatientOrWarn() {
        Patient p = getSelectedPatientSilent();
        if (p != null) return p;

        JOptionPane.showMessageDialog(
                this,
                "Bitte zuerst einen Patienten in der Tabelle auswählen.",
                "Hinweis",
                JOptionPane.INFORMATION_MESSAGE
        );
        return null;
    }

    /**
     * Gibt den ausgewählten Patienten zurück.
     * Kein Popup, wenn nichts gewählt ist.
     */
    private Patient getSelectedPatientSilent() {
        int row = tblPatients.getSelectedRow();
        if (row < 0 || row >= currentPatients.size()) return null;
        return currentPatients.get(row);
    }

    /**
     * Führt DB-Arbeit im Hintergrund aus.
     * Zeigt bei Fehlern eine einfache Meldung.
     */
    private void runDbAction(String infoText, Runnable dbWork, Runnable onSuccess) {
        setActionsEnabled(false);
        showInfo(infoText);

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() { dbWork.run(); return null; }

            @Override protected void done() {
                try {
                    get();
                    onSuccess.run();
                } catch (Exception ex) {
                    Throwable cause = unwrap(ex);
                    if (cause instanceof IllegalArgumentException) {
                        showInfo("Eingabe passt noch nicht – bitte korrigieren.");
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

    /**
     * Zeigt den Dialog zum Anlegen/Bearbeiten.
     * Wiederholt sich, bis es passt oder Abbruch.
     */
    private Patient showPatientDialogLoop(Patient existing) {
        ensureStationsLoaded();

        JTextField tfFirst = new JTextField(existing != null ? safe(existing.getFirstName()) : safe(prefillFirst));
        JTextField tfLast  = new JTextField(existing != null ? safe(existing.getLastName())  : safe(prefillLast));
        JFormattedTextField tfBirth = createBirthField(existing);

        JTextField tfSvnr = new JTextField(existing == null ? "" : safe(existing.getSvnr()));
        ((AbstractDocument) tfSvnr.getDocument()).setDocumentFilter(new SvnrDocumentFilter());

        String startPhone = (existing != null && existing.getPhone() != null && !existing.getPhone().isEmpty()) ? existing.getPhone() : "+";
        JTextField tfPhone = new JTextField(startPhone);
        ((AbstractDocument) tfPhone.getDocument()).setDocumentFilter(new PlusDigitsFilter(13));

        JTextField tfReason = new JTextField(existing == null ? "" : safe(existing.getReason()));
        JTextField tfAddress = new JTextField(existing == null ? "" : safe(existing.getAddress()));

        JComboBox<Station> cbStation = buildStationCombo(existing);

        JPanel p = new JPanel(new java.awt.GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Vorname:")); p.add(tfFirst);
        p.add(new JLabel("Nachname:")); p.add(tfLast);
        p.add(new JLabel("Geburtsdatum (YYYY-MM-DD):")); p.add(tfBirth);
        p.add(new JLabel("SVNR (10 Ziffern):")); p.add(tfSvnr);
        p.add(new JLabel("Telefon:")); p.add(tfPhone);
        p.add(new JLabel("Adresse:")); p.add(tfAddress);
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
                out.setBirthDate(parseBirthSafe(tfBirth.getText()));
                out.setSvnr(tfSvnr.getText());
                out.setPhone(tfPhone.getText());
                out.setAddress(tfAddress.getText());
                out.setReason(tfReason.getText());

                Station sel = (Station) cbStation.getSelectedItem();
                if (sel != null) out.setStationId(sel.getRaum());

                pk.validateOnly(out);
                return out;

            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Bitte korrigieren:\n" + ex.getMessage(), "Eingabe fehlerhaft", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Eingaben sind nicht korrekt.", "Eingabe fehlerhaft", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * Baut die Stations-Combobox und wählt bei Bearbeiten vor.
     */
    private JComboBox<Station> buildStationCombo(Patient existing) {
        JComboBox<Station> cb = new JComboBox<>();
        for (Station s : stations) {
            if (s != null && s.getName() != null && !"test".equalsIgnoreCase(s.getName())) cb.addItem(s);
        }

        if (existing != null && existing.getStationId() != null) {
            for (int i = 0; i < cb.getItemCount(); i++) {
                if (cb.getItemAt(i).getRaum() == existing.getStationId()) {
                    cb.setSelectedIndex(i);
                    break;
                }
            }
        }
        return cb;
    }

    /**
     * Liest das Datum aus dem Feld.
     * Gibt null zurück, wenn es nicht passt.
     */
    private LocalDate parseBirthSafe(String s) {
        if (s == null) return null;
        if (s.isEmpty() || s.contains("_")) return null;
        try { return LocalDate.parse(s); } catch (Exception e) { return null; }
    }

    /**
     * Baut das Datumsfeld mit Maske (YYYY-MM-DD).
     */
    private JFormattedTextField createBirthField(Patient existing) {
        String val = (existing == null || existing.getBirthDate() == null)
                ? "2000-01-01"
                : existing.getBirthDate().toString();

        try {
            MaskFormatter mf = new MaskFormatter("####-##-##");
            mf.setPlaceholderCharacter('_');
            JFormattedTextField tf = new JFormattedTextField(mf);
            tf.setText(val);
            return tf;
        } catch (ParseException e) {
            JFormattedTextField tf = new JFormattedTextField();
            tf.setText(val);
            return tf;
        }
    }

    /**
     * Holt die eigentliche Ursache aus einer Exception.
     */
    private Throwable unwrap(Exception ex) {
        if (ex instanceof ExecutionException && ex.getCause() != null) return ex.getCause();
        return ex;
    }

    /**
     * Zeigt eine DB-Fehlermeldung.
     */
    private void showDbError(String userText, Throwable ex) {
        JOptionPane.showMessageDialog(this, userText + "\n\nBitte Datenbank-Verbindung prüfen.", "Datenbank-Problem", JOptionPane.ERROR_MESSAGE);
        showInfo("Datenbank-Problem – Verbindung prüfen.");
    }

    /**
     * Zeigt Text im Detailfeld an.
     */
    private void showInfo(String text) { if (taDetails != null) taDetails.setText(text); }

    /**
     * Leert das Detailfeld.
     */
    private void clearInfo() { if (taDetails != null) taDetails.setText(""); }

    /**
     * Aktiviert oder deaktiviert Buttons.
     */
    private void setActionsEnabled(boolean enabled) {
        tfSearch.setEnabled(true);
        suchenButton.setEnabled(enabled);
        refresh.setEnabled(enabled);
        anlegenButton.setEnabled(enabled);
        bearbeitenButton.setEnabled(enabled);
        löschenButton.setEnabled(enabled);
    }

    /**
     * Gibt nie null zurück (für Anzeige).
     */
    private String safe(String s) { return s == null ? "" : s; }

    /**
     * Filter: SVNR nur Ziffern, max 10.
     */
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

    /**
     * Filter: Telefon erlaubt "+", dann Ziffern.
     * Maximal-Länge wird geprüft.
     */
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
