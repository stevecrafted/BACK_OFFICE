package org.example.DAO;

import org.example.Model.Reservation;
import org.example.Util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    // CREATE
    public boolean create(Reservation reservation) {
        String sql = "INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, reservation.getIdHotel());
            stmt.setString(2, reservation.getIdClient());
            stmt.setInt(3, reservation.getNbPassager());

            if (reservation.getDateHeure() != null) {
                stmt.setTimestamp(4, reservation.getDateHeure());
            } else {
                stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            }

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        reservation.setId(rs.getInt(1));
                    }
                }
                System.out.println("✅ Réservation créée : " + reservation.getId());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la création : " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // READ ALL
    public List<Reservation> findAll() {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservations ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la lecture : " + e.getMessage());
            e.printStackTrace();
        }

        return reservations;
    }

    // READ BY ID
    public Reservation findById(int id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReservation(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // UPDATE
    public boolean update(Reservation reservation) {
        String sql = "UPDATE reservations SET id_hotel = ?, id_client = ?, nbPassager = ?, date_heure = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reservation.getIdHotel());
            stmt.setString(2, reservation.getIdClient());
            stmt.setInt(3, reservation.getNbPassager());
            stmt.setTimestamp(4, reservation.getDateHeure());
            stmt.setInt(5, reservation.getId());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Réservation mise à jour : " + reservation.getId());
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
        String sql = "DELETE FROM reservations WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Réservation supprimée (ID: " + id + ")");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression : " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // READ BY CLIENT
    public List<Reservation> findByClient(String idClient) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE id_client = ? ORDER BY date_heure DESC";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idClient);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }

            System.out.println("✅ " + reservations.size() + " réservation(s) trouvée(s) pour le client : " + idClient);

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche par client : " + e.getMessage());
            e.printStackTrace();
        }

        return reservations;
    }

    // READ BY HOTEL
    public List<Reservation> findByHotel(int idHotel) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE id_hotel = ? ORDER BY date_heure DESC";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idHotel);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }

            System.out.println("✅ " + reservations.size() + " réservation(s) trouvée(s) pour l'hotel ID : " + idHotel);

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche par hotel : " + e.getMessage());
            e.printStackTrace();
        }

        return reservations;
    }

    // READ BY DATE
    public List<Reservation> findByDate(java.sql.Date date) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE DATE(date_heure) = ? ORDER BY date_heure";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, date);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }

            System.out.println("✅ " + reservations.size() + " réservation(s) trouvée(s) pour la date : " + date);

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche par date : " + e.getMessage());
            e.printStackTrace();
        }

        return reservations;
    }

    // MÉTHODE UTILITAIRE
    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setId(rs.getInt("id"));
        reservation.setIdHotel(rs.getInt("id_hotel"));
        reservation.setIdClient(rs.getString("id_client"));
        reservation.setNbPassager(rs.getInt("nbPassager"));
        reservation.setDateHeure(rs.getTimestamp("date_heure"));
        int idVoiture = rs.getInt("idVoiture");
        reservation.setIdVoiture(rs.wasNull() ? null : idVoiture);
        return reservation;
    }

    // ASSIGNER UNE VOITURE A UNE RESERVATION
    public boolean assignVoiture(int idReservation, int idVoiture) {
        String sql = "UPDATE reservations SET idVoiture = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idVoiture);
            stmt.setInt(2, idReservation);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Voiture " + idVoiture + " assignée à la réservation " + idReservation);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'assignation de voiture : " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }
}
