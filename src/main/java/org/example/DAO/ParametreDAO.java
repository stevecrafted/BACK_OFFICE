package org.example.DAO;

import org.example.Util.DatabaseConnection;

import java.sql.*;

public class ParametreDAO {

    public String getValeur(String nom) {
        String sql = "SELECT valeur FROM parametre WHERE nom = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nom);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("valeur");
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la lecture du paramètre " + nom + " : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public double getVM() {
        String valeur = getValeur("VM");
        if (valeur != null) {
            return Double.parseDouble(valeur);
        }
        return 60.0; // valeur par défaut
    }

    public int getTempsAttente() {
        String valeur = getValeur("temps_attente");
        if (valeur != null) {
            return Integer.parseInt(valeur);
        }
        return 30; // valeur par défaut en minutes
    }
}
