package org.example.DAO;

import org.example.Util.DatabaseConnection;

import java.sql.*;

public class DistanceDAO {

    /**
     * Récupère la distance entre deux hotels
     * Gère automatiquement l'ordre (A->B ou B->A)
     */
    public Double getDistance(int idHotel1, int idHotel2) {
        if (idHotel1 == idHotel2) {
            return 0.0; // Même hotel
        }

        String sql = "SELECT kilometer FROM distance " +
                     "WHERE (id_from = ? AND id_to = ?) OR (id_from = ? AND id_to = ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Essayer dans les deux sens
            stmt.setInt(1, Math.min(idHotel1, idHotel2));
            stmt.setInt(2, Math.max(idHotel1, idHotel2));
            stmt.setInt(3, Math.min(idHotel1, idHotel2));
            stmt.setInt(4, Math.max(idHotel1, idHotel2));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("kilometer");
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche de distance : " + e.getMessage());
            e.printStackTrace();
        }

        return null; // Distance non trouvée
    }
}
