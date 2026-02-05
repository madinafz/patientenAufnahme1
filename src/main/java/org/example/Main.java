package org.example;

import org.example.PatientenAufnahme;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PatientenAufnahme().setVisible(true));
    }
}
