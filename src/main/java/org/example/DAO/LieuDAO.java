package org.example.DAO;

import org.example.Model.Lieu;
import org.example.Util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LieuDAO {

    // CREATE
    public boolean create(Lieu lieu) {
        String sql = "INSERT INTO lieu (code, libelle, type) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, lieu.getCode());
            stmt.setString(2, lieu.getLibelle());
            stmt.setString(3, lieu.getType());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        lieu.setId(rs.getInt(1));
                    }
                }
                System.out.println("✅ Lieu créé : " + lieu.getLibelle());
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la création : " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // READ ALL
    public List<Lieu> findAll() {
        List<Lieu> lieux = new ArrayList<>();
        String sql = "SELECT * FROM lieu ORDER BY id";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                lieux.add(mapResultSetToLieu(rs));
            }
            
            System.out.println("✅ " + lieux.size() + " lieu(x) trouvé(s)");
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la lecture : " + e.getMessage());
            e.printStackTrace();
        }
        
        return lieux;
    }

    // READ BY ID
    public Lieu findById(int id) {
        String sql = "SELECT * FROM lieu WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLieu(rs);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche : " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    // FIND AEROPORT
    public Lieu findAeroport() {
        String sql = "SELECT * FROM lieu WHERE type = 'aeroport' LIMIT 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return mapResultSetToLieu(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche de l'aéroport : " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    // FIND BY TYPE
    public List<Lieu> findByType(String type) {
        List<Lieu> lieux = new ArrayList<>();
        String sql = "SELECT * FROM lieu WHERE type = ? ORDER BY libelle";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, type);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lieux.add(mapResultSetToLieu(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la recherche par type : " + e.getMessage());
            e.printStackTrace();
        }
        
        return lieux;
    }

    // UPDATE
    public boolean update(Lieu lieu) {
        String sql = "UPDATE lieu SET code = ?, libelle = ?, type = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, lieu.getCode());
            stmt.setString(2, lieu.getLibelle());
            stmt.setString(3, lieu.getType());
            stmt.setInt(4, lieu.getId());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✅ Lieu mis à jour : " + lieu.getLibelle());
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
        String sql = "DELETE FROM lieu WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✅ Lieu supprimé (ID: " + id + ")");
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression : " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    // MÉTHODE UTILITAIRE
    private Lieu mapResultSetToLieu(ResultSet rs) throws SQLException {
        Lieu lieu = new Lieu();
        lieu.setId(rs.getInt("id"));
        lieu.setCode(rs.getString("code"));
        lieu.setLibelle(rs.getString("libelle"));
        lieu.setType(rs.getString("type"));
        return lieu;
    }
}
