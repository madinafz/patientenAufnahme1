package org.example;


import javax.swing.SwingUtilities;

/**
 * Einstiegspunkt der Anwendung.
 * Startet die Swing-Oberfläche für die Patientenaufnahme. Der GUI-Start wird über SwingUI
 * in den Event-dispatch-Thread gelegt, damit Swing korrekt und thread-sicher initialisiert wird
 */
public class Main {

    /**
     * Startmethode der Anwendung.
     *
     * @param args Kommandozeilenargumente
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PatientenAufnahme().setVisible(true));

    }
}
