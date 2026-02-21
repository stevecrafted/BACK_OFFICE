package org.example.DAO;

import org.example.Model.Carburant;
import org.example.Util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CarburantDAO {

    // CREATE
    public boolean create(Carburant carburant) {
        String sql = "INSERT INTO Carburant (libelle) VALUES (?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, carburant.getLibelle());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        carburant.setIdCarburant(rs.getInt(1));
                    }
                }
                System.out.println("✅ Carburant créé : " + carburant.getIdCarburant());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la création : " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // READ ALL
    public List<Carburant> findAll() {
        List<Carburant> carburants = new ArrayList<>();
        String sql = "SELECT * FROM Carburant ORDER BY libelle";

        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Carburant carburant = mapResultSetToCarburant(rs);
                carburants.add(carburant);
            }

            System.out.println("✅ " + carburants.size() + " carburant(s) trouvé(s)");

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la lecture : " + e.getMessage());
            e.printStackTrace();
        }

        return carburants;
    }

    // READ BY ID
    public Carburant findById(int id) {
        String sql = "SELECT * FROM Carburant WHERE idCarburant = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Carburant carburant = mapResultSetToCarburant(rs);
                    System.out.println("✅ Carburant trouvé : " + carburant.getIdCarburant());
                    return carburant;
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // UPDATE
    public boolean update(Carburant carburant) {
        String sql = "UPDATE Carburant SET libelle = ? WHERE idCarburant = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, carburant.getLibelle());
            stmt.setInt(2, carburant.getIdCarburant());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Carburant mis à jour : " + carburant.getIdCarburant());
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
        String sql = "DELETE FROM Carburant WHERE idCarburant = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Carburant supprimé (ID: " + id + ")");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression : " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // MÉTHODE UTILITAIRE : Mapper ResultSet vers Carburant
    private Carburant mapResultSetToCarburant(ResultSet rs) throws SQLException {
        Carburant carburant = new Carburant();
        carburant.setIdCarburant(rs.getInt("idCarburant"));
        carburant.setLibelle(rs.getString("libelle"));
        return carburant;
    }
}
