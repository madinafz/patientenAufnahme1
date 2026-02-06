package org.example;

import org.example.PatientenAufnahme;

import javax.swing.SwingUtilities;

/**
 * Einstiegspunkt der Anwendung.
 * <p>
 * Startet die Swing-Oberfläche für die Patientenaufnahme. Der GUI-Start wird über
 * {@link SwingUtilities#invokeLater(Runnable)} in den Event-Dispatch-Thread gelegt,
 * damit Swing korrekt und thread-sicher initialisiert wird.
 * </p>
 */
public class Main {

    /**
     * Startmethode der Anwendung.
     *
     * @param args Kommandozeilenargumente (werden hier nicht verwendet)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PatientenAufnahme().setVisible(true));

    }
}
