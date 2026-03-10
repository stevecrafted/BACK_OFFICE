package org.example.DAO;

import org.example.Util.DatabaseConnection;

import java.sql.*;

public class ParametreDAO {

    /**
     * Récupère la valeur d'un paramètre par son nom
     */
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
            System.err.println("❌ Erreur lors de la lecture du paramètre '" + nom + "' : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Récupère la vitesse moyenne (VM) en km/h
     */
    public double getVitesseMoyenne() {
        String valeur = getValeur("VM");
        if (valeur != null) {
            return Double.parseDouble(valeur);
        }
        return 60.0; // Valeur par défaut
    }

    /**
     * Récupère l'ID de l'hôtel servant d'aéroport (point de départ/arrivée)
     */
    public int getAeroportId() {
        String valeur = getValeur("AEROPORT_HOTEL_ID");
        if (valeur != null) {
            return Integer.parseInt(valeur);
        }
        return -1;
    }
}
