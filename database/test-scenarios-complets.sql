-- ========================================
-- SCÉNARIOS DE TEST COMPLETS
-- Date: 03-03-2026
-- ========================================
-- Ce script crée plusieurs scénarios de test pour valider tous les cas

-- Nettoyer les données existantes
DELETE FROM assignation;
DELETE FROM reservations;

-- ========================================
-- SCÉNARIO 1: 2026-03-10 - Test Multi-Vagues Complet
-- ========================================
-- Objectif: Tester 3 vagues avec remplissage optimal et priorité diesel

-- VAGUE 1: 08:00 (10 passagers total)
-- Devrait utiliser: VOI0002 (7 places Diesel) pour Alice(5p) + Charlie(2p)
--                   VOI0001 (5 places Essence) pour Bob(3p)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (1, 'Alice', 5, '2026-03-10 08:00:00');  -- Priorité 1 (plus de passagers)

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (2, 'Bob', 3, '2026-03-10 08:00:00');    -- Priorité 2

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (3, 'Charlie', 2, '2026-03-10 08:00:00'); -- Priorité 3 (remplissage de VOI0002)

-- VAGUE 2: 10:30 (16 passagers total)
-- Devrait utiliser: VOI0005 (7 places Diesel) pour David(7p)
--                   VOI0007 (5 places Diesel) pour Emma(4p)
--                   VOI0003 (4 places Essence) pour Frank(3p)
--                   VOI0006 (4 places Essence) pour Grace(2p)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (1, 'David', 7, '2026-03-10 10:30:00');   -- Priorité 1 (7 passagers)

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (2, 'Emma', 4, '2026-03-10 10:30:00');    -- Priorité 2

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (3, 'Frank', 3, '2026-03-10 10:30:00');   -- Priorité 3

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (4, 'Grace', 2, '2026-03-10 10:30:00');   -- Priorité 4

-- VAGUE 3: 14:00 (7 passagers total)
-- Devrait utiliser: VOI0008 (6 places Diesel) pour Henry(4p) + Iris(3p) OU
--                   VOI0004 (5 places Électrique) pour Henry(4p)
--                   VOI0006 ou autre pour Iris(3p)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (1, 'Henry', 4, '2026-03-10 14:00:00');   -- Priorité 1

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (2, 'Iris', 3, '2026-03-10 14:00:00');    -- Priorité 2

-- ========================================
-- SCÉNARIO 2: 2026-03-15 - Test Remplissage Optimal
-- ========================================
-- Objectif: Tester le remplissage optimal (2 réservations dans 1 voiture)

-- VAGUE 1: 09:00 (5 passagers = 1 voiture pleine)
-- Devrait utiliser: VOI0001 (5 places) pour Jack(3p) + Kate(2p)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (1, 'Jack', 3, '2026-03-15 09:00:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (2, 'Kate', 2, '2026-03-15 09:00:00');

-- VAGUE 2: 11:00 (4 passagers)
-- Devrait utiliser: VOI0002 (7 places Diesel) ou VOI0003 (4 places Essence)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (3, 'Leo', 4, '2026-03-15 11:00:00');

-- ========================================
-- SCÉNARIO 3: 2026-03-20 - Test Capacité Insuffisante
-- ========================================
-- Objectif: Tester le cas où il n'y a pas assez de voitures

-- VAGUE 1: 16:00 (Beaucoup de réservations)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (1, 'Mike', 7, '2026-03-20 16:00:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (2, 'Nina', 6, '2026-03-20 16:00:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (3, 'Oscar', 5, '2026-03-20 16:00:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (4, 'Paula', 5, '2026-03-20 16:00:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (1, 'Quinn', 4, '2026-03-20 16:00:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (2, 'Rita', 4, '2026-03-20 16:00:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (3, 'Sam', 3, '2026-03-20 16:00:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (4, 'Tina', 3, '2026-03-20 16:00:00');

-- VAGUE 2: 18:00 (Devrait avoir 0 voiture disponible)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (1, 'Uma', 2, '2026-03-20 18:00:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (2, 'Victor', 2, '2026-03-20 18:00:00');

-- ========================================
-- SCÉNARIO 4: 2026-03-25 - Test Priorité Diesel
-- ========================================
-- Objectif: Vérifier que les Diesel sont prioritaires

-- VAGUE 1: 10:00 (4 passagers - plusieurs voitures de 4-5 places disponibles)
-- Devrait choisir VOI0007 (5 places Diesel) plutôt que VOI0003 (4 places Essence)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (1, 'Walter', 4, '2026-03-25 10:00:00');

-- ========================================
-- SCÉNARIO 5: 2026-03-30 - Test Écart Minimal
-- ========================================
-- Objectif: Vérifier la minimisation de l'écart

-- VAGUE 1: 14:00 (3 passagers)
-- Devrait choisir VOI0003 (4 places, écart=1) plutôt que VOI0001 (5 places, écart=2)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (1, 'Xena', 3, '2026-03-30 14:00:00');

-- ========================================
-- VÉRIFICATIONS
-- ========================================

SELECT '=== RÉSUMÉ PAR DATE ===' as info;
SELECT 
    DATE(date_heure) as date,
    COUNT(*) as nb_reservations,
    SUM(nbPassager) as total_passagers
FROM reservations
GROUP BY DATE(date_heure)
ORDER BY DATE(date_heure);

SELECT '=== SCÉNARIO 1: 2026-03-10 (Multi-Vagues) ===' as info;
SELECT 
    date_heure as vague,
    COUNT(*) as nb_res,
    SUM(nbPassager) as passagers,
    STRING_AGG(id_client || '(' || nbPassager || 'p)', ', ' ORDER BY nbPassager DESC) as detail
FROM reservations
WHERE DATE(date_heure) = '2026-03-10'
GROUP BY date_heure
ORDER BY date_heure;

SELECT '=== SCÉNARIO 2: 2026-03-15 (Remplissage Optimal) ===' as info;
SELECT 
    date_heure as vague,
    COUNT(*) as nb_res,
    SUM(nbPassager) as passagers,
    STRING_AGG(id_client || '(' || nbPassager || 'p)', ', ' ORDER BY nbPassager DESC) as detail
FROM reservations
WHERE DATE(date_heure) = '2026-03-15'
GROUP BY date_heure
ORDER BY date_heure;

SELECT '=== SCÉNARIO 3: 2026-03-20 (Capacité Insuffisante) ===' as info;
SELECT 
    date_heure as vague,
    COUNT(*) as nb_res,
    SUM(nbPassager) as passagers,
    STRING_AGG(id_client || '(' || nbPassager || 'p)', ', ' ORDER BY nbPassager DESC) as detail
FROM reservations
WHERE DATE(date_heure) = '2026-03-20'
GROUP BY date_heure
ORDER BY date_heure;

SELECT '=== CAPACITÉ TOTALE DES VOITURES ===' as info;
SELECT 
    COUNT(*) as nb_voitures,
    SUM(Capacite) as capacite_totale,
    STRING_AGG(ref_ || '(' || Capacite || 'p)', ', ' ORDER BY idVoiture) as voitures
FROM Voiture;

-- ========================================
-- INSTRUCTIONS DE TEST
-- ========================================
-- 1. Exécutez: psql -U postgres -d framework_test -f database/test-scenarios-complets.sql
-- 2. Testez chaque scénario:
--    - 2026-03-10: Multi-vagues (3 vagues, 9 réservations)
--    - 2026-03-15: Remplissage optimal (2 vagues, 3 réservations)
--    - 2026-03-20: Capacité insuffisante (2 vagues, 10 réservations, certaines non assignées)
--    - 2026-03-25: Priorité diesel (1 vague, 1 réservation)
--    - 2026-03-30: Écart minimal (1 vague, 1 réservation)
-- ========================================

