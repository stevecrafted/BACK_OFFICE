package org.example.DAO;

import org.example.Model.Voiture;
import org.example.Util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VoitureDAO {

    // CREATE
    public boolean create(Voiture voiture) {
        String sql = "INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, voiture.getCapacite());
            // Le ref sera mis à jour après avoir récupéré l'ID
            stmt.setString(2, "TEMP"); // Valeur temporaire
            stmt.setInt(3, voiture.getIdCarburant());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        voiture.setIdVoiture(generatedId);
                        
                        // Générer le ref basé sur l'ID: VOI0001, VOI0012, etc.
                        String ref = String.format("VOI%04d", generatedId);
                        voiture.setRef(ref);
                        
                        // Mettre à jour le ref dans la base
                        String updateSql = "UPDATE Voiture SET ref_ = ? WHERE idVoiture = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setString(1, ref);
                            updateStmt.setInt(2, generatedId);
                            updateStmt.executeUpdate();
                        }
                    }
                }
                System.out.println("✅ Voiture créée : " + voiture.getIdVoiture() + " - Ref: " + voiture.getRef());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la création : " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // READ ALL
    public List<Voiture> findAll() {
        List<Voiture> voitures = new ArrayList<>();
        String sql = "SELECT * FROM Voiture ORDER BY idVoiture";

        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Voiture voiture = mapResultSetToVoiture(rs);
                voitures.add(voiture);
            }

            System.out.println("✅ " + voitures.size() + " voiture(s) trouvée(s)");

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la lecture : " + e.getMessage());
            e.printStackTrace();
        }

        return voitures;
    }

    // READ BY ID
    public Voiture findById(int id) {
        String sql = "SELECT * FROM Voiture WHERE idVoiture = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Voiture voiture = mapResultSetToVoiture(rs);
                    System.out.println("✅ Voiture trouvée : " + voiture.getIdVoiture());
                    return voiture;
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // READ BY CARBURANT
    public List<Voiture> findByCarburant(int idCarburant) {
        List<Voiture> voitures = new ArrayList<>();
        String sql = "SELECT * FROM Voiture WHERE idCarburant = ? ORDER BY ref_";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idCarburant);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    voitures.add(mapResultSetToVoiture(rs));
                }
            }

            System.out.println("✅ " + voitures.size() + " voiture(s) trouvée(s) pour le carburant : " + idCarburant);

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche par carburant : " + e.getMessage());
            e.printStackTrace();
        }

        return voitures;
    }

    // READ BY REF
    public Voiture findByRef(String ref) {
        String sql = "SELECT * FROM Voiture WHERE ref_ = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ref);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Voiture voiture = mapResultSetToVoiture(rs);
                    System.out.println("✅ Voiture trouvée avec ref : " + ref);
                    return voiture;
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche par ref : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // UPDATE
    public boolean update(Voiture voiture) {
        String sql = "UPDATE Voiture SET Capacite = ?, ref_ = ?, idCarburant = ? WHERE idVoiture = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, voiture.getCapacite());
            stmt.setString(2, voiture.getRef());
            stmt.setInt(3, voiture.getIdCarburant());
            stmt.setInt(4, voiture.getIdVoiture());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Voiture mise à jour : " + voiture.getIdVoiture());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la mise à jour : " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // DELETE
    public boolean delete(int id) {
        String sql = "DELETE FROM Voiture WHERE idVoiture = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Voiture supprimée (ID: " + id + ")");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression : " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // MÉTHODE UTILITAIRE : Mapper ResultSet vers Voiture
    private Voiture mapResultSetToVoiture(ResultSet rs) throws SQLException {
        Voiture voiture = new Voiture();
        voiture.setIdVoiture(rs.getInt("idVoiture"));
        voiture.setCapacite(rs.getInt("Capacite"));
        voiture.setRef(rs.getString("ref_"));
        voiture.setIdCarburant(rs.getInt("idCarburant"));
        return voiture;
    }
}
