package org.example;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBtest {

    public static void main(String[] args) {

        String url = "jdbc:mysql://10.25.2.145:3306/24abfa?useSSL=false&serverTimezone=UTC";
        String user = "24fama";
        String pass = "geb24";

        try {
            Connection con = DriverManager.getConnection(url, user, pass);
            System.out.println("Verbindung zur Datenbank funktioniert");
            con.close();
        } catch (Exception e) {
            System.out.println("Fehler: " + e.getMessage());
        }
    }
}
