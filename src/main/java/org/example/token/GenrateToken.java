package org.example.token;

import org.example.Util.DatabaseConnection;

import java.sql.*;
import java.util.UUID;

public class GenrateToken {

    /**
     * Récupère la durée de validité du token (en secondes) depuis la table parametre
     */
    private static int getDureeValiditeToken(Connection conn) throws SQLException {
        String sql = "SELECT valeur FROM parametre WHERE nom = 'DUREE_VALIDITE_TOKEN'";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return Integer.parseInt(rs.getString("valeur"));
            } else {
                throw new SQLException("Paramètre 'DUREE_VALIDITE_TOKEN' non trouvé dans la table parametre");
            }
        }
    }

    /**
     * Génère un token unique et l'insère dans la table token_validite
     */
    public static String genererEtInsererToken() {
        String token = UUID.randomUUID().toString();

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Récupérer la durée de validité en secondes
            int dureeEnSecondes = getDureeValiditeToken(conn);

            // 2. Calculer la date d'expiration
            Timestamp maintenant = new Timestamp(System.currentTimeMillis());
            Timestamp expiration = new Timestamp(maintenant.getTime() + (dureeEnSecondes * 1000L));

            // 3. Insérer le token dans la base
            String sql = "INSERT INTO token_validite (token, date_creation, date_expiration) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, token);
                stmt.setTimestamp(2, maintenant);
                stmt.setTimestamp(3, expiration);
                stmt.executeUpdate();
            }

            System.out.println("==========================================");
            System.out.println("✅ Token généré avec succès !");
            System.out.println("   Token       : " + token);
            System.out.println("   Créé le     : " + maintenant);
            System.out.println("   Expire le   : " + expiration);
            System.out.println("   Durée       : " + dureeEnSecondes + " secondes");
            System.out.println("==========================================");

            return token;

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la génération du token : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Vérifie si un token est valide (existe et non expiré)
     */
    public static boolean isTokenValide(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        String sql = "SELECT id FROM token_validite WHERE token = ? AND date_expiration > NOW()";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, token);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la vérification du token : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Point d'entrée - Génère un nouveau token à chaque exécution
     */
    public static void main(String[] args) {
        System.out.println("=== Génération d'un nouveau token ===");
        String token = genererEtInsererToken();

        if (token != null) {
            System.out.println("\n✅ Utilisez ce token dans le header 'Authorization' de vos requêtes API.");
            System.out.println("   Exemple : Authorization: " + token);
        } else {
            System.err.println("\n❌ Échec de la génération du token.");
            System.exit(1);
        }
    }
}
