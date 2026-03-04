-- ========================================
-- AJOUTER DES RÉSERVATIONS SUPPLÉMENTAIRES
-- Date: 03-03-2026
-- ========================================
-- Ce script ajoute des réservations aux données existantes
-- SANS supprimer les données actuelles

-- ========================================
-- RÉSERVATIONS POUR 2026-03-10
-- ========================================

-- Vague 08:00
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (1, 'Alice', 5, '2026-03-10 08:00:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (2, 'Bob', 3, '2026-03-10 08:00:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (3, 'Charlie', 2, '2026-03-10 08:00:00');

-- Vague 10:30
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (1, 'David', 7, '2026-03-10 10:30:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (2, 'Emma', 4, '2026-03-10 10:30:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (3, 'Frank', 3, '2026-03-10 10:30:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (4, 'Grace', 2, '2026-03-10 10:30:00');

-- Vague 14:00
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (1, 'Henry', 4, '2026-03-10 14:00:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (2, 'Iris', 3, '2026-03-10 14:00:00');

-- ========================================
-- RÉSERVATIONS POUR 2026-03-15
-- ========================================

-- Vague 09:00 (test remplissage optimal)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (1, 'Jack', 3, '2026-03-15 09:00:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (2, 'Kate', 2, '2026-03-15 09:00:00');

-- Vague 11:00
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (3, 'Leo', 4, '2026-03-15 11:00:00');

-- ========================================
-- RÉSERVATIONS POUR 2026-03-20
-- ========================================

-- Vague 16:00 (test capacité)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (1, 'Mike', 6, '2026-03-20 16:00:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (2, 'Nina', 5, '2026-03-20 16:00:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (3, 'Oscar', 4, '2026-03-20 16:00:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (4, 'Paula', 3, '2026-03-20 16:00:00');

-- ========================================
-- VÉRIFICATION
-- ========================================

SELECT '=== TOUTES LES RÉSERVATIONS ===' as info;
SELECT 
    id,
    id_client,
    id_hotel,
    nbPassager,
    date_heure
FROM reservations
ORDER BY date_heure, nbPassager DESC;

SELECT '=== RÉSUMÉ PAR DATE ===' as info;
SELECT 
    DATE(date_heure) as date,
    COUNT(*) as nb_reservations,
    SUM(nbPassager) as total_passagers
FROM reservations
GROUP BY DATE(date_heure)
ORDER BY DATE(date_heure);

SELECT '=== DÉTAIL 2026-03-10 ===' as info;
SELECT 
    date_heure,
    COUNT(*) as nb_res,
    SUM(nbPassager) as passagers,
    STRING_AGG(id_client, ', ' ORDER BY nbPassager DESC) as clients
FROM reservations
WHERE DATE(date_heure) = '2026-03-10'
GROUP BY date_heure
ORDER BY date_heure;

