-- ========================================
-- TEST: Split Optimization
-- Cas: R1=5p, R2=11p avec V1=4p, V2=10p
-- ========================================

DELETE FROM assignation;
DELETE FROM reservations;
DELETE FROM Voiture;

-- ========================================
-- VOITURES
-- ========================================
INSERT INTO Voiture (Capacite, ref_, carburant, disponibilite) VALUES 
(4, 'V1', 'D', '08:00:00'),   -- 4 places
(10, 'V2', 'D', '08:00:00');  -- 10 places

-- ========================================
-- RÉSERVATIONS
-- ========================================
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (2, 'R1', 5, '2026-03-17 08:00:00');

INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (2, 'R2', 11, '2026-03-17 08:00:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (2, 'R3', 1, '2026-03-17 12:00:00');

-- ========================================
-- RÉSULTAT ATTENDU (APRÈS MODIFICATION):
-- ========================================
-- VAGUE #1 - 08:00:
--   1. R2 (11p) cherche voiture → V2 (10p) trouvée
--      → V2 prend R2 (10/11p) - Split: 1p restant
--   
--   2. R1 (5p) cherche voiture → Aucune voiture assez grande
--      → Cherche plus grande disponible → V1 (4p) trouvée
--      → V1 prend R1 (4/5p) - Split: 1p restant
--   
--   3. R2 (1p restant) cherche voiture → Aucune disponible
--   4. R1 (1p restant) cherche voiture → Aucune disponible
--
-- Résultat:
--   ✓ V2 part avec R2 (10p)
--   ✓ V1 part avec R1 (4p)
--   ✗ R2 (1p) en attente
--   ✗ R1 (1p) en attente
--
-- SPRINT 8 - Quand V1 et V2 reviennent:
--   → Prennent R2 (1p) et R1 (1p)

-- ========================================
-- VÉRIFICATION
-- ========================================
SELECT 'Voitures: ' || COUNT(*) FROM Voiture;
SELECT 'Réservations: ' || COUNT(*) FROM reservations;

SELECT idVoiture, ref_, Capacite, disponibilite FROM Voiture ORDER BY idVoiture;
SELECT id, id_client, nbPassager, date_heure FROM reservations ORDER BY date_heure;
