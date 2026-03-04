-- ========================================
-- DONNÉES DE TEST POUR LA SIMULATION
-- Date: 03-03-2026
-- ========================================
-- Ce script ajoute des réservations de test pour tester la simulation par vagues

-- Supprimer les anciennes réservations de test
DELETE FROM assignation;
DELETE FROM reservations;

-- ========================================
-- SCÉNARIO DE TEST: 2026-03-10
-- ========================================
-- 3 vagues de réservations avec différentes heures

-- VAGUE 1: 08:00 (3 réservations)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (1, 'Alice', 5, '2026-03-10 08:00:00');  -- 5 passagers (priorité haute)

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (2, 'Bob', 3, '2026-03-10 08:00:00');    -- 3 passagers

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (3, 'Charlie', 2, '2026-03-10 08:00:00'); -- 2 passagers

-- VAGUE 2: 10:30 (4 réservations)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (1, 'David', 7, '2026-03-10 10:30:00');   -- 7 passagers (priorité haute)

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (2, 'Emma', 4, '2026-03-10 10:30:00');    -- 4 passagers

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (3, 'Frank', 3, '2026-03-10 10:30:00');   -- 3 passagers

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (4, 'Grace', 2, '2026-03-10 10:30:00');   -- 2 passagers

-- VAGUE 3: 14:00 (2 réservations)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (1, 'Henry', 4, '2026-03-10 14:00:00');   -- 4 passagers

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (2, 'Iris', 3, '2026-03-10 14:00:00');    -- 3 passagers

-- ========================================
-- SCÉNARIO DE TEST: 2026-03-15
-- ========================================
-- 2 vagues avec remplissage optimal

-- VAGUE 1: 09:00 (2 réservations qui peuvent partager une voiture)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (1, 'Jack', 3, '2026-03-15 09:00:00');    -- 3 passagers

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (2, 'Kate', 2, '2026-03-15 09:00:00');    -- 2 passagers (total 5 = voiture pleine)

-- VAGUE 2: 11:00 (1 réservation)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (3, 'Leo', 4, '2026-03-15 11:00:00');     -- 4 passagers

-- ========================================
-- VÉRIFICATION
-- ========================================
SELECT '=== RÉSERVATIONS PAR DATE ===' as info;

SELECT 
    DATE(date_heure) as date,
    date_heure as heure,
    COUNT(*) as nb_reservations,
    SUM(nbPassager) as total_passagers
FROM reservations
GROUP BY DATE(date_heure), date_heure
ORDER BY date_heure;

SELECT '=== DÉTAIL DES RÉSERVATIONS ===' as info;

SELECT 
    id,
    id_client,
    id_hotel,
    nbPassager,
    date_heure
FROM reservations
ORDER BY date_heure, nbPassager DESC;

-- ========================================
-- INSTRUCTIONS
-- ========================================
-- Pour tester la simulation:
-- 1. Exécutez ce script pour créer les données de test
-- 2. Accédez à: http://localhost:8084/assignations/simuler
-- 3. Saisissez la date: 2026-03-10 ou 2026-03-15
-- 4. Cliquez sur "Lancer la Simulation"
-- 5. Vérifiez que les vagues sont bien séparées
-- 6. Vérifiez le remplissage optimal des voitures
-- 7. Cliquez sur "Confirmer et Enregistrer" pour sauvegarder
-- ========================================

