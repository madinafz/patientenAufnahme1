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
 * Swing-GUI für die Patientenaufnahme.
 * <p>
 * Diese Klasse stellt das Hauptfenster der Anwendung dar. Sie ermöglicht:
 * Patienten suchen/aktualisieren, Details anzeigen, neue Patienten anlegen,
 * bestehende bearbeiten und löschen. Datenbankzugriffe werden dabei über
 * {@link PatientKontrolle} und {@link StationKontrolle} gekapselt und laufen
 * im Hintergrund via {@link SwingWorker}, damit die UI nicht blockiert.
 * </p>
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

    /**
     * Kontrollschicht für Patientenoperationen inkl. Validierung und Speichern.
     */
    private final PatientKontrolle pk = new PatientKontrolle();

    /**
     * Kontrollschicht für Stationsdaten (z.B. für Combobox/Anzeige).
     */
    private final StationKontrolle sk = new StationKontrolle();

    /**
     * Aktuell geladene Patientenliste, passend zur Tabelle.
     */
    private List<Patient> currentPatients = new ArrayList<>();

    /**
     * Map für schnelle Raum-/Stationsnamen-Auflösung: Raum -> Stationsname.
     */
    private Map<Integer, String> stationMap = Map.of();

    /**
     * Liste aller Stationen (für Auswahl im Dialog).
     */
    private List<Station> stations = new ArrayList<>();

    /**
     * Worker zum Laden der Patientendaten, damit die UI nicht einfriert.
     */
    private SwingWorker<List<Patient>, Void> loadWorker;

    /**
     * Merkt sich eine Suche, die während eines laufenden Loads eingegeben wurde.
     */
    private String pendingQuery = null;

    /**
     * Wird verwendet, um „Nicht gefunden“-Logik erst ab dem zweiten Laden sinnvoll zu zeigen.
     */
    private boolean firstLoadDone = false;

    /**
     * Vorbelegung für „Neu anlegen“, wenn ein Suchbegriff aus Vor- und Nachname bestand.
     */
    private String prefillFirst = "";

    /**
     * Vorbelegung für „Neu anlegen“, wenn ein Suchbegriff aus Vor- und Nachname bestand.
     */
    private String prefillLast = "";

    /**
     * Erstellt das Hauptfenster, initialisiert Listener und lädt initial die Tabelle.
     */
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

    /**
     * Lädt Patienten (und bei Bedarf Stationen) im Hintergrund und füllt danach die Tabelle.
     * <p>
     * Wenn gerade bereits ein Ladeprozess läuft, wird die gewünschte Suche als {@code pendingQuery}
     * gespeichert und anschließend ausgeführt.
     * </p>
     *
     * @param query Suchbegriff
     * @param userInitiated {@code true}, wenn die Suche vom User ausgelöst wurde (für UI-Feedback)
     */
    private void loadTable(String query, boolean userInitiated) {
        String q = (query == null) ? "" : query;

        if (loadWorker != null && !loadWorker.isDone()) {
            pendingQuery = q;
            showInfo("Daten werden geladen ...\nSuche wird danach ausgeführt: \"" + q + "\"");
            return;
        }

        pendingQuery = null;
        setActionsEnabled(false);

        String cur = (taDetails == null ? "" : taDetails.getText());
        if (cur == null || cur.isEmpty() || !cur.startsWith("Bitte kurz warten")) {
            showInfo("Daten werden geladen ...");
        }

        loadWorker = new SwingWorker<>() {
            @Override
            protected List<Patient> doInBackground() {
                if (stations == null || stations.isEmpty()) {
                    stations = sk.getAllStations();
                    stationMap = sk.getStationMap();
                } else if (stationMap == null || stationMap.isEmpty()) {
                    stationMap = sk.getStationMap();
                }
                return pk.search(q);
            }

            @Override
            protected void done() {
                try {
                    currentPatients = get();
                    fillTable(currentPatients);
                    clearInfo();

                    boolean isEmpty = currentPatients.isEmpty();
                    if (isEmpty && firstLoadDone && userInitiated && !q.isEmpty()) {
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

    /**
     * Zeigt eine „Nicht gefunden“-Meldung und bietet optional an, den Patienten neu anzulegen.
     * <p>
     * Wenn „Neu anlegen“ gewählt wird, wird versucht den Suchbegriff in Vor-/Nachname zu splitten
     * und als Vorbelegung im Dialog zu verwenden.
     * </p>
     *
     * @param q Suchbegriff, der keine Treffer geliefert hat
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

        if (choice == 0) {
            String[] parts = q.split("\\s+", 2);
            prefillFirst = parts.length > 0 ? parts[0] : "";
            prefillLast = parts.length > 1 ? parts[1] : "";
            createPatient();
        }
    }

    /**
     * Befüllt die Patiententabelle mit den gegebenen Datensätzen.
     *
     * @param patients Patientenliste, die angezeigt werden soll
     */
    private void fillTable(List<Patient> patients) {
        String[] cols = {"Patient-ID", "Raum", "Nachname", "Vorname", "Geburtsdatum", "SVNR", "Telefon", "Adresse", "Station", "Grund"};
        Object[][] rows = new Object[patients.size()][cols.length];

        for (int i = 0; i < patients.size(); i++) {
            Patient p = patients.get(i);
            rows[i][0] = p.getId();
            rows[i][1] = (p.getStationId() == null) ? "" : p.getStationId();
            rows[i][2] = p.getLastName();
            rows[i][3] = p.getFirstName();
            rows[i][4] = p.getBirthDate();
            rows[i][5] = p.getSvnr();
            rows[i][6] = p.getPhone();
            rows[i][7] = p.getAddress();
            rows[i][8] = (p.getStationId() == null) ? "" : stationMap.getOrDefault(p.getStationId(), "");
            rows[i][9] = p.getReason();
        }

        tblPatients.setModel(new DefaultTableModel(rows, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        });
    }

    /**
     * Zeigt die Details eines Patienten im Detailbereich an.
     *
     * @param p Patient, dessen Daten angezeigt werden sollen
     */
    private void showDetails(Patient p) {
        taDetails.setText(
                "Patient-ID: " + p.getId() + "\n" +
                        "Raum: " + (p.getStationId() == null ? "" : p.getStationId()) + "\n" +
                        "Vorname: " + safe(p.getFirstName()) + "\n" +
                        "Nachname: " + safe(p.getLastName()) + "\n" +
                        "Geburtsdatum: " + p.getBirthDate() + "\n" +
                        "SVNR: " + safe(p.getSvnr()) + "\n" +
                        "Telefon: " + safe(p.getPhone()) + "\n" +
                        "Adresse: " + safe(p.getAddress()) + "\n" +
                        "Station: " + (p.getStationId() == null ? "" : stationMap.getOrDefault(p.getStationId(), "")) + "\n" +
                        "Grund: " + safe(p.getReason())
        );
    }

    /**
     * Startet den Dialog zum Anlegen eines neuen Patienten und speichert ihn danach.
     * <p>
     * Nach erfolgreichem Speichern wird die Liste neu geladen.
     * </p>
     */
    private void createPatient() {
        Patient p = showPatientDialogLoop(null);
        prefillFirst = "";
        prefillLast = "";
        if (p == null) return;

        runDbAction(
                "Patient wird angelegt ...",
                () -> pk.save(p),
                () -> {
                    showInfo("Erfolg: Patient wurde erfolgreich angelegt.");
                    JOptionPane.showMessageDialog(this, "Patient wurde erfolgreich angelegt.", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
                    showInfo("Bitte kurz warten – Liste wird aktualisiert ...");
                    tfSearch.setText("");
                    loadTable("", false);
                }
        );
    }

    /**
     * Öffnet den Bearbeiten-Dialog für den aktuell ausgewählten Patienten und speichert die Änderungen.
     */
    private void editSelectedPatient() {
        Patient old = getSelectedPatientOrWarn();
        if (old == null) return;

        Patient updated = showPatientDialogLoop(old);
        if (updated == null) return;

        updated.setId(old.getId());

        runDbAction(
                "Patient wird gespeichert ...",
                () -> pk.save(updated),
                () -> {
                    JOptionPane.showMessageDialog(this, "Änderungen wurden erfolgreich gespeichert.", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
                    loadTable(tfSearch.getText(), true);
                }
        );
    }

    /**
     * Löscht den aktuell ausgewählten Patienten nach Bestätigung durch den User.
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
                "Patient wird gelöscht ...",
                () -> pk.delete(p.getId()),
                () -> {
                    JOptionPane.showMessageDialog(this, "Patient wurde gelöscht.", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
                    loadTable(tfSearch.getText(), true);
                }
        );
    }

    /**
     * Holt den aktuell ausgewählten Patienten aus der Tabelle oder zeigt eine Hinweis-Meldung.
     *
     * @return ausgewählter Patient oder {@code null}, wenn nichts ausgewählt wurde
     */
    private Patient getSelectedPatientOrWarn() {
        int row = tblPatients.getSelectedRow();
        if (row < 0 || row >= currentPatients.size()) {
            JOptionPane.showMessageDialog(this, "Bitte zuerst einen Patienten in der Tabelle auswählen.", "Hinweis", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return currentPatients.get(row);
    }

    /**
     * Führt eine Datenbankaktion im Hintergrund aus und verarbeitet danach das Ergebnis.
     * <p>
     * Fehler werden je nach Typ als Eingabefehler (Validierung) oder als Datenbankproblem
     * behandelt. Während der Aktion werden Buttons deaktiviert, damit keine Mehrfachklicks passieren.
     * </p>
     *
     * @param infoText Text, der während der Aktion angezeigt wird
     * @param dbWork   eigentliche Arbeit (Insert/Update/Delete)
     * @param onSuccess Callback nach erfolgreicher Ausführung
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

    /**
     * Öffnet den Eingabedialog zum Anlegen/Bearbeiten und bleibt so lange in einer Schleife,
     * bis gültige Eingaben gemacht wurden oder der User abbricht.
     *
     * @param existing bestehender Patient (bei Bearbeiten) oder {@code null} (bei Neu anlegen)
     * @return validierter Patient oder {@code null} bei Abbruch
     */
    private Patient showPatientDialogLoop(Patient existing) {
        if (stations == null || stations.isEmpty()) {
            stations = sk.getAllStations();
            stationMap = sk.getStationMap();
        }

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

        JComboBox<Station> cbStation = new JComboBox<>();
        for (Station s : stations) {
            if (s != null && s.getName() != null && !"test".equalsIgnoreCase(s.getName())) cbStation.addItem(s);
        }

        if (existing != null && existing.getStationId() != null) {
            for (int i = 0; i < cbStation.getItemCount(); i++) {
                if (cbStation.getItemAt(i).getRaum() == existing.getStationId()) {
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
     * Parst ein Geburtsdatum aus dem Textfeld.
     * <p>
     * Gibt {@code null} zurück, wenn der Text leer ist, Platzhalter enthält oder nicht geparst werden kann.
     * </p>
     *
     * @param s Text aus dem Eingabefeld
     * @return {@link LocalDate} oder {@code null}, wenn ungültig
     */
    private LocalDate parseBirthSafe(String s) {
        if (s == null) return null;
        if (s.isEmpty() || s.contains("_")) return null;
        try { return LocalDate.parse(s); } catch (Exception e) { return null; }
    }

    /**
     * Erstellt ein formatiertes Eingabefeld für das Geburtsdatum (YYYY-MM-DD).
     * <p>
     * Es wird eine Maskierung verwendet, damit das Format eingehalten wird.
     * Als Default wird bei Neuanlage ein Standarddatum gesetzt.
     * </p>
     *
     * @param existing bestehender Patient (kann {@code null} sein)
     * @return formatiertes Textfeld für das Geburtsdatum
     */
    private JFormattedTextField createBirthField(Patient existing) {
        try {
            MaskFormatter mf = new MaskFormatter("####-##-##");
            mf.setPlaceholderCharacter('_');
            JFormattedTextField tf = new JFormattedTextField(mf);
            String val = (existing == null || existing.getBirthDate() == null) ? "2000-01-01" : existing.getBirthDate().toString();
            tf.setText(val);
            return tf;
        } catch (ParseException e) {
            JFormattedTextField tf = new JFormattedTextField();
            tf.setText(existing == null || existing.getBirthDate() == null ? "2000-01-01" : existing.getBirthDate().toString());
            return tf;
        }
    }

    /**
     * Holt die eigentliche Ursache aus einer Exception (z.B. aus {@link ExecutionException}).
     *
     * @param ex Exception aus einem Worker
     * @return Ursache, falls vorhanden, sonst die Exception selbst
     */
    private Throwable unwrap(Exception ex) {
        if (ex instanceof ExecutionException && ex.getCause() != null) return ex.getCause();
        return ex;
    }

    /**
     * Zeigt eine standardisierte Datenbank-Fehlermeldung an.
     *
     * @param userText Text für den Benutzer
     * @param ex       technische Ursache (wird nicht direkt angezeigt, aber für Logging/Debugging relevant)
     */
    private void showDbError(String userText, Throwable ex) {
        JOptionPane.showMessageDialog(this, userText + "\n\nBitte Datenbank-Verbindung prüfen.", "Datenbank-Problem", JOptionPane.ERROR_MESSAGE);
        showInfo("Datenbank-Problem – Verbindung prüfen.");
    }

    /**
     * Zeigt einen Info-Text im Detailbereich an.
     *
     * @param text Text, der angezeigt werden soll
     */
    private void showInfo(String text) { if (taDetails != null) taDetails.setText(text); }

    /**
     * Leert den Info-/Detailbereich.
     */
    private void clearInfo() { if (taDetails != null) taDetails.setText(""); }

    /**
     * Aktiviert oder deaktiviert UI-Aktionen während laufender Lade- oder DB-Operationen.
     *
     * @param enabled {@code true} wenn Buttons klickbar sein sollen
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
     * Liefert einen sicheren String zurück, damit in der UI keine {@code null}-Werte angezeigt werden.
     *
     * @param s Eingabestring
     * @return leerer String bei {@code null}, sonst der String selbst
     */
    private String safe(String s) { return s == null ? "" : s; }

    /**
     * Filtert die Eingabe für SVNR so, dass nur Ziffern erlaubt sind und maximal 10 Zeichen möglich sind.
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
     * Filtert die Eingabe für Telefonnummern, damit nur ein führendes '+' und danach nur Ziffern möglich sind.
     * Zusätzlich wird eine maximale Länge erzwungen.
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

        /**
         * Prüft, ob der gegebene String eine gültige Teil-Eingabe für eine Telefonnummer ist.
         *
         * @param s aktueller Inhalt nach der geplanten Änderung
         * @return {@code true}, wenn gültig, sonst {@code false}
         */
        private boolean isValid(String s) {
            if (s.length() > maxLen) return false;
            return s.isEmpty() || s.equals("+") || s.matches("\\+\\d*");
        }
    }
}
