package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLOutput;

/**
 * Hilfsklasse für den Datenbankzugriff.
 * <p>
 * Diese Klasse kapselt die Verbindungsdaten (URL, Benutzer, Passwort) und stellt
 * eine zentrale Methode bereit, um eine JDBC-Verbindung zur Datenbank aufzubauen.
 * </p>
 */
public class DB {

    /**
     * JDBC-URL zur MySQL-Datenbank inkl. benötigter Parameter.
     */
    private static final String URL =
            "jdbc:mysql://10.25.2.145:3306/24abfa?useSSL=false&serverTimezone=UTC";

    /**
     * Benutzername für den Datenbanklogin.
     */
    private static final String USER = "24fama";

    /**
     * Passwort für den Datenbanklogin.
     */
    private static final String PASS = "geb24";

    /**
     * Erstellt und liefert eine neue Verbindung zur Datenbank.
     * <p>
     * Die Verbindung wird über {@link DriverManager#getConnection(String, String, String)}
     * aufgebaut. Das Schließen der Verbindung (z.B. in try-with-resources) ist Aufgabe
     * der aufrufenden Stelle.
     * </p>
     *
     * @return neue {@link Connection} zur Datenbank
     * @throws SQLException wenn keine Verbindung aufgebaut werden kann
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }



}
