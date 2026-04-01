-- ========================================
-- SCRIPT DE TEST POUR SPRINT 8
-- Gestion des retours de véhicules
-- ========================================

-- Réinitialiser les données
DELETE FROM assignation;
DELETE FROM reservations;
DELETE FROM Voiture;

-- ========================================
-- VOITURES
-- ========================================
-- Seulement 2 voitures disponibles au début pour forcer l'attente
INSERT INTO Voiture (Capacite, ref_, carburant, disponibilite) VALUES 
(7, 'VOI0001', 'D', '08:00:00'),  -- Diesel, 7 places, disponible à 08:00
(10, 'VOI0002', 'D', '08:00:00'); -- Diesel, 10 places, disponible à 08:00

-- ========================================
-- SCÉNARIO SPRINT 8: Retour de véhicule
-- ========================================

-- VAGUE 1: 08:00 - Les 2 voitures partent
-- VOI0001 (7 places) prend Alice (5p) + Bob (2p) = 7/7 PLEIN
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (2, 'Alice', 5, '2026-03-16 08:00:00');

INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (2, 'Bob', 2, '2026-03-16 08:05:00');

-- VOI0002 (10 places) prend Charlie (8p) = 8/10
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (3, 'Charlie', 8, '2026-03-16 08:10:00');

-- RÉSERVATIONS EN ATTENTE (pas de voiture disponible - toutes parties)
-- David attend (10 passagers) - nécessite VOI0002 qui revient
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (2, 'David', 10, '2026-03-16 08:15:00');

-- Emma attend (5 passagers) - peut prendre VOI0001 qui revient
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (3, 'Emma', 5, '2026-03-16 08:20:00');

-- ========================================
-- RÉSULTAT ATTENDU:
-- ========================================
-- 08:00 - VAGUE 1:
--   * VOI0001 (7 places) part avec Alice (5p) + Bob (2p) = 7/7 PLEIN
--   * VOI0002 (10 places) part avec Charlie (8p) = 8/10
--   * David (10p) et Emma (5p) EN ATTENTE (pas de voitures disponibles)
--
-- ~08:45 - VOI0001 revient de l'hôtel 1 (trajet ~45 min)
--   * SPRINT 8 ACTIVÉ
--   * Fenêtre de regroupement: [08:45, 09:15] (30 min d'attente)
--   * David (10p) ne peut pas monter (VOI0001 = 7 places seulement)
--   * Emma (5p) peut monter dans VOI0001 (5/7)
--   * Attend jusqu'à 09:15 pour remplir
--   * Part à 09:15 avec Emma (2 places vides)
--
-- ~09:00 - VOI0002 revient de l'hôtel 2 (trajet ~60 min)
--   * SPRINT 8 ACTIVÉ
--   * Fenêtre de regroupement: [09:00, 09:30]
--   * Prend David (10p) = 10/10 PLEIN!
--   * Départ immédiat à 09:00 (véhicule plein)

-- ========================================
-- SCÉNARIO 2: Véhicule plein au retour (OPTIONNEL)
-- ========================================

-- Frank arrive pendant que VOI0001 attend (commenté par défaut)
-- INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
-- VALUES (2, 'Frank', 2, '2026-03-16 09:00:00');

-- RÉSULTAT ATTENDU avec Frank:
-- 08:45 - VOI0001 revient
--   * Prend Emma (5p) = 5/7
--   * Attend jusqu'à 09:15
-- 09:00 - Frank arrive
--   * VOI0001 prend Frank (2p) = 7/7 PLEIN!
--   * Départ immédiat à 09:00 (ne attend pas 09:15)

-- ========================================
-- VÉRIFICATION
-- ========================================
SELECT 'Voitures: ' || COUNT(*) FROM Voiture;
SELECT 'Réservations: ' || COUNT(*) FROM reservations;

SELECT * FROM Voiture ORDER BY idVoiture;
SELECT * FROM reservations ORDER BY date_heure;
