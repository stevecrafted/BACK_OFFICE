package org.example.DAO;

import org.example.Model.Hotel;
import org.example.Util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HotelDAO {

    // CREATE
    public boolean create(Hotel hotel) {
        String sql = "INSERT INTO hotel (nom) VALUES (?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, hotel.getNom());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        hotel.setId(rs.getInt(1));
                    }
                }
                System.out.println("✅ Hotel créé : " + hotel.getNom());
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la création : " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // READ ALL
    public List<Hotel> findAll() {
        List<Hotel> hotels = new ArrayList<>();
        String sql = "SELECT * FROM hotel ORDER BY id";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Hotel hotel = mapResultSetToHotel(rs);
                hotels.add(hotel);
            }
            
            System.out.println("✅ " + hotels.size() + " hotel(s) trouvé(s)");
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la lecture : " + e.getMessage());
            e.printStackTrace();
        }
        
        return hotels;
    }

    // READ BY ID
    public Hotel findById(int id) {
        String sql = "SELECT * FROM hotel WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Hotel hotel = mapResultSetToHotel(rs);
                    System.out.println("✅ Hotel trouvé : " + hotel.getNom());
                    return hotel;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche : " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    // UPDATE
    public boolean update(Hotel hotel) {
        String sql = "UPDATE hotel SET nom = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, hotel.getNom());
            stmt.setInt(2, hotel.getId());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✅ Hotel mis à jour : " + hotel.getNom());
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
        String sql = "DELETE FROM hotel WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✅ Hotel supprimé (ID: " + id + ")");
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression : " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    // MÉTHODE UTILITAIRE : Mapper ResultSet vers Hotel
    private Hotel mapResultSetToHotel(ResultSet rs) throws SQLException {
        Hotel hotel = new Hotel();
        hotel.setId(rs.getInt("id"));
        hotel.setNom(rs.getString("nom"));
        return hotel;
    }
}