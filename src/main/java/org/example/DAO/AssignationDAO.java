package org.example.DAO;

import org.example.Model.Assignation;
import org.example.Util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AssignationDAO {

    // CREATE
    public boolean create(Assignation assignation) {
        String sql = "INSERT INTO assignation (id_voiture, id_reservation) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, assignation.getIdVoiture());
            stmt.setInt(2, assignation.getIdReservation());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        assignation.setId(rs.getInt(1));
                    }
                }
                System.out.println("✅ Assignation créée : Voiture " + assignation.getIdVoiture() + 
                                   " -> Reservation " + assignation.getIdReservation());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la création : " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // READ ALL
    public List<Assignation> findAll() {
        List<Assignation> assignations = new ArrayList<>();
        String sql = "SELECT * FROM assignation ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                assignations.add(mapResultSetToAssignation(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la lecture : " + e.getMessage());
            e.printStackTrace();
        }

        return assignations;
    }

    // READ BY VOITURE
    public List<Assignation> findByVoiture(int idVoiture) {
        List<Assignation> assignations = new ArrayList<>();
        String sql = "SELECT * FROM assignation WHERE id_voiture = ? ORDER BY date_assignation";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idVoiture);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    assignations.add(mapResultSetToAssignation(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche : " + e.getMessage());
            e.printStackTrace();
        }

        return assignations;
    }

    // READ BY RESERVATION
    public Assignation findByReservation(int idReservation) {
        String sql = "SELECT * FROM assignation WHERE id_reservation = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idReservation);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAssignation(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // DELETE
    public boolean delete(int id) {
        String sql = "DELETE FROM assignation WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Assignation supprimée (ID: " + id + ")");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression : " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // MÉTHODE UTILITAIRE
    private Assignation mapResultSetToAssignation(ResultSet rs) throws SQLException {
        Assignation assignation = new Assignation();
        assignation.setId(rs.getInt("id"));
        assignation.setIdVoiture(rs.getInt("id_voiture"));
        assignation.setIdReservation(rs.getInt("id_reservation"));
        assignation.setDateAssignation(rs.getTimestamp("date_assignation"));
        return assignation;
    }
}
