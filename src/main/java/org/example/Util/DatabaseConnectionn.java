package org.example.Util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnectionn {

    static {
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("✅ Driver PostgreSQL chargé");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Driver PostgreSQL non trouvé");
        }
    }

    public static Connection getConnection() throws SQLException {
        // FORCER l'URL Docker
        // String url = "jdbc:postgresql://postgres:5432/framework_test";
        // String user = "postgres";
        // String password = "steve";

        // postgresql://postgres:[YOUR-PASSWORD]@db.ibighlolbxphofypqspr.supabase.co:5432/postgres
        // MrNaina-Tomobil
        // ZxBMqWw1kZ0YPo8L

        // String dbUrl =
        // "jdbc:postgresql://db.ibighlolbxphofypqspr.supabase.co:5432/postgres?user=postgres&password=ZxBMqWw1kZ0YPo8L&sslmode=require";
        String url = "jdbc:postgresql://postgres:5432/framework_test";
        String user = "postgres";
        String password = "postgres";

        // Raha tsy misy url io dbUrl io dia lasa local ho azy ny db
        //String dbUrl = "postgresql://postgres.ibighlolbxphofypqspr:ZxBMqWw1kZ0YPo8L@aws-1-eu-west-1.pooler.supabase.com:6543/postgres";
        String dbUrl = "";

        if (dbUrl != null && !dbUrl.isEmpty()) {
            url = dbUrl;
            user = null;
            password = null;
        } else {
            System.out.println(" CONNEXION LOCALE : ");
            url = "jdbc:postgresql://localhost:5432/framework_test";
            user = "postgres";
            password = "steve";
            System.out.println("URL: " + url);
        }
        System.out.println("==========================================");

        System.out.println("==========================================");
        System.out.println(" CONNEXION :");
        System.out.println("URL: " + url);
        System.out.println("User: " + user);
        System.out.println("==========================================");

        try {
            if (user == null) {
                // Connexion avec URL complète (Supabase)
                return DriverManager.getConnection(url);
            } else {
                // Connexion locale
                return DriverManager.getConnection(url, user, password);
            }
        } catch (SQLException e) {
            System.err.println("❌ ÉCHEC CONNEXION: " + e.getMessage());
            throw e;
        }
    }
}
