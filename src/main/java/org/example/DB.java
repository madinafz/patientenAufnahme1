package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLOutput;

public class DB {

       private static final String URL =
            "jdbc:mysql://10.25.2.145:3306/24abfa?useSSL=false&serverTimezone=UTC";

        private static final String USER = "24fama";
        private static final String PASS = "geb24";

        public static Connection getConnection() throws SQLException {
            return DriverManager.getConnection(URL, USER, PASS);
        }



}
