package org.example.Util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    static {
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("✅ Driver PostgreSQL chargé");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Driver PostgreSQL non trouvé");
            throw new RuntimeException("Driver PostgreSQL requis", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        // Configuration pour base de données LOCALE
        String host = "localhost";
        String port = "5432"; // Port PostgreSQL par défaut
        String database = "framework_test"; // Remplacez par le nom de votre base
        String user = "postgres"; // Votre utilisateur local
        String password = "postgres"; // Votre mot de passe local
        
        // Construction de l'URL
        String url = String.format(
            "jdbc:postgresql://%s:%s/%s",
            host, port, database
        );

        System.out.println("==========================================");
        System.out.println(" TENTATIVE DE CONNEXION À BASE LOCALE:");
        System.out.println("URL: " + url);
        System.out.println("User: " + user);
        System.out.println("Host: " + host);
        System.out.println("Port: " + port);
        System.out.println("==========================================");

        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("✅ CONNEXION RÉUSSIE À LA BASE LOCALE!");
            
            return conn;
        } catch (SQLException e) {
            System.err.println("❌ ÉCHEC CONNEXION À LA BASE LOCALE: " + e.getMessage());
            System.err.println("Détails de l'erreur:");
            System.err.println("1. Code d'erreur: " + e.getErrorCode());
            System.err.println("2. État SQL: " + e.getSQLState());
            
            System.err.println("\nVérifiez que:");
            System.err.println("1. PostgreSQL est démarré sur votre machine");
            System.err.println("2. Le nom de la base de données est correct");
            System.err.println("3. L'utilisateur et le mot de passe sont corrects");
            System.err.println("4. Le port 5432 n'est pas bloqué");
            System.err.println();
            throw e;
        }
    }
   
}