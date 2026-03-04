-- ========================================
-- DONNÉES DE TEST POUR LA SIMULATION
-- Date: 03-03-2026
-- ========================================
-- Ce script ajoute des réservations de test pour tester la simulation par vagues

-- Supprimer les anciennes données de test
DELETE FROM assignation;
DELETE FROM reservations;
DELETE FROM Voiture;
DELETE FROM Carburant;
DELETE FROM hotel;

-- ========================================
-- RECRÉER LES DONNÉES DE BASE
-- ========================================

-- Carburants
INSERT INTO Carburant (libelle) VALUES ('Essence');
INSERT INTO Carburant (libelle) VALUES ('Diesel');
INSERT INTO Carburant (libelle) VALUES ('Électrique');
INSERT INTO Carburant (libelle) VALUES ('Hybride');

-- Voitures (AUGMENTÉ pour supporter plus de réservations)
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (5, 'VOI0001', 1);  -- Essence, 5 places
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (7, 'VOI0002', 2);  -- Diesel, 7 places
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (4, 'VOI0003', 1);  -- Essence, 4 places
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (5, 'VOI0004', 3);  -- Électrique, 5 places
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (7, 'VOI0005', 2);  -- Diesel, 7 places
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (4, 'VOI0006', 1);  -- Essence, 4 places
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (5, 'VOI0007', 2);  -- Diesel, 5 places
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (6, 'VOI0008', 2);  -- Diesel, 6 places

-- Hotels
INSERT INTO hotel (nom) VALUES ('Colbert');
INSERT INTO hotel (nom) VALUES ('Novotel');
INSERT INTO hotel (nom) VALUES ('Ibis');
INSERT INTO hotel (nom) VALUES ('Lokanga');

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
SELECT '=== VOITURES DISPONIBLES ===' as info;
SELECT idVoiture, Capacite, ref_, idCarburant FROM Voiture ORDER BY idVoiture;

SELECT '=== CAPACITÉ TOTALE ===' as info;
SELECT 
    COUNT(*) as nb_voitures,
    SUM(Capacite) as capacite_totale
FROM Voiture;

SELECT '=== RÉSERVATIONS PAR DATE ===' as info;

SELECT 
    DATE(date_heure) as date,
    date_heure as heure,
    COUNT(*) as nb_reservations,
    SUM(nbPassager) as total_passagers
FROM reservations
GROUP BY DATE(date_heure), date_heure
ORDER BY date_heure;

SELECT '=== DÉTAIL DES RÉSERVATIONS 2026-03-10 ===' as info;

SELECT 
    id,
    id_client,
    id_hotel,
    nbPassager,
    date_heure
FROM reservations
WHERE DATE(date_heure) = '2026-03-10'
ORDER BY date_heure, nbPassager DESC;

SELECT '=== ANALYSE PAR VAGUE (2026-03-10) ===' as info;

SELECT 
    date_heure as vague,
    COUNT(*) as nb_reservations,
    SUM(nbPassager) as total_passagers,
    STRING_AGG(id_client || '(' || nbPassager || 'p)', ', ' ORDER BY nbPassager DESC) as clients
FROM reservations
WHERE DATE(date_heure) = '2026-03-10'
GROUP BY date_heure
ORDER BY date_heure;

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

