package org.example.DAO;

import org.example.Model.Assignation;
import org.example.Model.ReservationAssignation;
import org.example.Util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AssignationDAO {

    public boolean create(Assignation assignation) {
        String sqlAssignation = "INSERT INTO assignation (id_voiture, date_heure_depart, date_heure_arrivee) VALUES (?, ?, ?)";
        String sqlLien = "INSERT INTO reservation_assignation (id_assignation, id_reservation, ordre_itineraire) VALUES (?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sqlAssignation, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, assignation.getIdVoiture());
                stmt.setTimestamp(2, assignation.getDateHeureDepart());
                stmt.setTimestamp(3, assignation.getDateHeureArrivee());

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected <= 0) {
                    conn.rollback();
                    return false;
                }

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        assignation.setId(rs.getInt(1));
                    }
                }
            }

            if (assignation.getReservationAssignations() != null && !assignation.getReservationAssignations().isEmpty()) {
                try (PreparedStatement stmtLien = conn.prepareStatement(sqlLien)) {
                    for (ReservationAssignation ra : assignation.getReservationAssignations()) {
                        stmtLien.setInt(1, assignation.getId());
                        stmtLien.setInt(2, ra.getIdReservation());
                        stmtLien.setInt(3, ra.getOrdreItineraire());
                        stmtLien.addBatch();
                    }
                    stmtLien.executeBatch();
                }
            }

            conn.commit();
            System.out.println("✅ Assignation créée : Voiture " + assignation.getIdVoiture() + " | Départ " + assignation.getDateHeureDepart());
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            System.err.println("❌ Erreur lors de la création : " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public List<Assignation> findAll() {
        List<Assignation> assignations = new ArrayList<>();
        String sql = "SELECT * FROM assignation ORDER BY date_heure_depart, id";

        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Assignation assignation = mapResultSetToAssignation(rs);
                assignation.setReservationAssignations(findReservationAssignationsByAssignation(assignation.getId(), conn));
                assignations.add(assignation);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la lecture : " + e.getMessage());
            e.printStackTrace();
        }

        return assignations;
    }

    public List<Assignation> findByVoiture(int idVoiture) {
        List<Assignation> assignations = new ArrayList<>();
        String sql = "SELECT * FROM assignation WHERE id_voiture = ? ORDER BY date_heure_depart";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idVoiture);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Assignation assignation = mapResultSetToAssignation(rs);
                    assignation.setReservationAssignations(findReservationAssignationsByAssignation(assignation.getId(), conn));
                    assignations.add(assignation);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche : " + e.getMessage());
            e.printStackTrace();
        }

        return assignations;
    }

    public Assignation findByReservation(int idReservation) {
        String sql = "SELECT a.* FROM assignation a " +
            "JOIN reservation_assignation ra ON ra.id_assignation = a.id " +
            "WHERE ra.id_reservation = ? " +
            "ORDER BY a.date_heure_depart DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idReservation);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Assignation assignation = mapResultSetToAssignation(rs);
                    assignation.setReservationAssignations(findReservationAssignationsByAssignation(assignation.getId(), conn));
                    return assignation;
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public Assignation findById(int id) {
        String sql = "SELECT * FROM assignation WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Assignation assignation = mapResultSetToAssignation(rs);
                    assignation.setReservationAssignations(findReservationAssignationsByAssignation(assignation.getId(), conn));
                    return assignation;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche par id : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean delete(int id) {
        String sqlDeleteLiaison = "DELETE FROM reservation_assignation WHERE id_assignation = ?";
        String sqlDeleteAssignation = "DELETE FROM assignation WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmtLien = conn.prepareStatement(sqlDeleteLiaison)) {
                stmtLien.setInt(1, id);
                stmtLien.executeUpdate();
            }

            int rowsAffected;
            try (PreparedStatement stmtAssignation = conn.prepareStatement(sqlDeleteAssignation)) {
                stmtAssignation.setInt(1, id);
                rowsAffected = stmtAssignation.executeUpdate();
            }

            conn.commit();
            if (rowsAffected > 0) {
                System.out.println("✅ Assignation supprimée (ID: " + id + ")");
                return true;
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            System.err.println("❌ Erreur lors de la suppression : " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }

        return false;
    }

    private Assignation mapResultSetToAssignation(ResultSet rs) throws SQLException {
        Assignation assignation = new Assignation();
        assignation.setId(rs.getInt("id"));
        assignation.setIdVoiture(rs.getInt("id_voiture"));
        assignation.setDateHeureDepart(rs.getTimestamp("date_heure_depart"));
        assignation.setDateHeureArrivee(rs.getTimestamp("date_heure_arrivee"));
        return assignation;
    }

    private List<ReservationAssignation> findReservationAssignationsByAssignation(int idAssignation, Connection conn) throws SQLException {
        List<ReservationAssignation> result = new ArrayList<>();
        String sql = "SELECT * FROM reservation_assignation WHERE id_assignation = ? ORDER BY ordre_itineraire, id";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idAssignation);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ReservationAssignation ra = new ReservationAssignation();
                    ra.setId(rs.getInt("id"));
                    ra.setIdAssignation(rs.getInt("id_assignation"));
                    ra.setIdReservation(rs.getInt("id_reservation"));
                    ra.setOrdreItineraire(rs.getInt("ordre_itineraire"));
                    result.add(ra);
                }
            }
        }
        return result;
    }

    public List<Integer> findReservationIdsByAssignation(int idAssignation) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id_reservation FROM reservation_assignation WHERE id_assignation = ? ORDER BY ordre_itineraire, id";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idAssignation);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id_reservation"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findReservationIdsByAssignation : " + e.getMessage());
            e.printStackTrace();
        }
        return ids;
    }

    /**
     * SPRINT 6: Compte le nombre de trajets effectués par une voiture POUR UNE DATE DONNÉE
     * Un trajet = une assignation (Aéroport -> Hôtel -> Aéroport)
     * Le compteur se réinitialise chaque jour
     */
    public int countTrajetsParVoiture(int idVoiture, java.sql.Date dateSimulation) {
        String sql = "SELECT COUNT(*) as nb_trajets FROM assignation WHERE id_voiture = ? AND DATE(date_heure_depart) = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idVoiture);
            stmt.setDate(2, dateSimulation);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("nb_trajets");
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors du comptage des trajets : " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }
}
