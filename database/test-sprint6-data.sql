-- ========================================
-- DONNÉES DE TEST SPRINT 6
-- Date: 16-03-2026
-- ========================================
-- Test de la priorité au nombre de trajets et réutilisation des voitures

-- Nettoyer les données existantes
DELETE FROM assignation;
DELETE FROM reservations;

-- ========================================
-- SCÉNARIO 1: Test priorité nombre de trajets (JOUR MÊME)
-- Date: 2026-03-16
-- ========================================
-- Objectif: Vérifier que les voitures avec moins de trajets LE JOUR MÊME sont prioritaires
-- Le compteur de trajets se réinitialise chaque jour

-- Créer des assignations existantes pour le MÊME JOUR (2026-03-16)
-- VOI0001 (5 places, Essence) : 2 trajets aujourd'hui
-- VOI0002 (7 places, Diesel) : 0 trajet aujourd'hui
-- VOI0003 (4 places, Essence) : 1 trajet aujourd'hui
-- VOI0004 (5 places, Hybride) : 0 trajet aujourd'hui

-- Réservations déjà traitées ce matin (2026-03-16)
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure) VALUES (2, 'MATIN001', 3, '2026-03-16 06:00:00'); -- Colbert
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure) VALUES (3, 'MATIN002', 4, '2026-03-16 06:30:00'); -- Novotel
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure) VALUES (4, 'MATIN003', 2, '2026-03-16 07:00:00'); -- Ibis

-- Créer les assignations du matin (même jour)
-- VOI0001: 2 trajets (IDs 1, 2)
INSERT INTO assignation (id_voiture, id_reservation, date_assignation)
VALUES (1, 1, '2026-03-16 05:30:00');

INSERT INTO assignation (id_voiture, id_reservation, date_assignation)
VALUES (1, 2, '2026-03-16 06:00:00');

-- VOI0003: 1 trajet (ID 3)
INSERT INTO assignation (id_voiture, id_reservation, date_assignation)
VALUES (3, 3, '2026-03-16 06:30:00');

-- VOI0002: 0 trajet aujourd'hui
-- VOI0004: 0 trajet aujourd'hui

-- Créer aussi des assignations d'HIER (2026-03-15) pour montrer que ça ne compte pas
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (2, 'HIER001', 3, '2026-03-15 10:00:00');

INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (3, 'HIER002', 4, '2026-03-15 11:00:00');

INSERT INTO assignation (id_voiture, id_reservation, date_assignation)
VALUES (2, 4, '2026-03-15 09:30:00'); -- VOI0002 avait 1 trajet HIER (ne compte pas)

INSERT INTO assignation (id_voiture, id_reservation, date_assignation)
VALUES (4, 5, '2026-03-15 10:30:00'); -- VOI0004 avait 1 trajet HIER (ne compte pas)

-- ========================================
-- RÉSERVATIONS À SIMULER - 2026-03-16
-- ========================================

-- VAGUE 1: 08:00 (3 réservations)
-- Test: 5 passagers -> VOI0001(5,E,2 trajets aujourd'hui) vs VOI0004(5,H,0 trajet aujourd'hui)
-- Résultat attendu: VOI0004 (0 trajet aujourd'hui + Hybride > Essence)
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (2, 'Alice', 5, '2026-03-16 08:00:00'); -- Colbert, 5 passagers

-- Test: 4 passagers -> VOI0003(4,E,1 trajet aujourd'hui) seule candidate
-- Résultat attendu: VOI0003
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (3, 'Bob', 4, '2026-03-16 08:00:00'); -- Novotel, 4 passagers

-- Test: 3 passagers -> VOI0001(5,E,2 trajets) vs VOI0002(7,D,0 trajet) vs VOI0004(déjà utilisée)
-- Résultat attendu: VOI0002 (0 trajet + Diesel) OU VOI0001 selon écart
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (4, 'Charlie', 3, '2026-03-16 08:00:00'); -- Ibis, 3 passagers

-- VAGUE 2: 10:30 (4 réservations)
-- Test réutilisation: Les voitures de la vague 1 peuvent être réutilisées si disponibles
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (5, 'David', 7, '2026-03-16 10:30:00'); -- Lokanga, 7 passagers -> VOI0002 (1 trajet)

INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (2, 'Emma', 4, '2026-03-16 10:30:00'); -- Colbert, 4 passagers

INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (3, 'Frank', 3, '2026-03-16 10:30:00'); -- Novotel, 3 passagers

INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (4, 'Grace', 2, '2026-03-16 10:30:00'); -- Ibis, 2 passagers

-- VAGUE 3: 14:00 (2 réservations)
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (2, 'Henry', 4, '2026-03-16 14:00:00'); -- Colbert, 4 passagers

INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (3, 'Iris', 3, '2026-03-16 14:00:00'); -- Novotel, 3 passagers

-- ========================================
-- SCÉNARIO 2: Test temps d'attente (vagues)
-- Date: 2026-03-17
-- ========================================
-- Objectif: Vérifier le regroupement par vague avec temps_attente = 30 minutes

-- Réservations espacées de moins de 30 minutes -> même vague
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (2, 'Jack', 3, '2026-03-17 09:00:00'); -- Début fenêtre

INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (3, 'Kate', 2, '2026-03-17 09:15:00'); -- +15 min (dans la fenêtre)

INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (4, 'Leo', 4, '2026-03-17 09:29:00'); -- +29 min (dans la fenêtre)

-- Réservation après 30 minutes -> nouvelle vague
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (5, 'Mike', 3, '2026-03-17 09:31:00'); -- +31 min (nouvelle vague)

-- ========================================
-- SCÉNARIO 3: Test priorité carburant
-- Date: 2026-03-18
-- ========================================
-- Objectif: D > H > E

-- 5 passagers -> VOI0001(5,E) vs VOI0004(5,H)
-- Résultat attendu: VOI0004 (H > E)
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (2, 'Nina', 5, '2026-03-18 10:00:00');

-- 7 passagers -> VOI0002(7,D) seule candidate
-- Résultat attendu: VOI0002 (Diesel)
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (3, 'Oscar', 7, '2026-03-18 11:00:00');

-- ========================================
-- SCÉNARIO 4: Test capacité insuffisante
-- Date: 2026-03-19
-- ========================================

-- 10 passagers -> aucune voiture (max = 7)
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (2, 'Paula', 10, '2026-03-19 08:00:00');

-- ========================================
-- SCÉNARIO 5: Test remplissage optimal
-- Date: 2026-03-20
-- ========================================
-- Objectif: Vérifier que les réservations sont ajoutées aux voitures existantes

-- Même vague, 3 réservations qui peuvent partager des voitures
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (2, 'Quinn', 3, '2026-03-20 09:00:00'); -- 3 passagers

INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (3, 'Rita', 2, '2026-03-20 09:00:00'); -- 2 passagers (peut remplir VOI0001 ou VOI0004)

INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (4, 'Sam', 1, '2026-03-20 09:00:00'); -- 1 passager (remplissage)

-- ========================================
-- VÉRIFICATIONS
-- ========================================

SELECT '=== HISTORIQUE DES TRAJETS (JOUR MÊME: 2026-03-16) ===' as info;
SELECT 
    v.idVoiture,
    v.ref_,
    v.Capacite,
    v.carburant,
    COUNT(CASE WHEN DATE(r.date_heure) = '2026-03-16' THEN 1 END) as trajets_aujourdhui,
    COUNT(a.id) as trajets_total
FROM Voiture v
LEFT JOIN assignation a ON v.idVoiture = a.id_voiture
LEFT JOIN reservations r ON a.id_reservation = r.id
GROUP BY v.idVoiture, v.ref_, v.Capacite, v.carburant
ORDER BY v.idVoiture;

SELECT '=== RÉSERVATIONS PAR DATE ===' as info;
SELECT 
    DATE(date_heure) as date,
    COUNT(*) as nb_reservations,
    SUM(nbPassager) as total_passagers
FROM reservations
WHERE id > 6 -- Exclure l'historique
GROUP BY DATE(date_heure)
ORDER BY DATE(date_heure);

SELECT '=== SCÉNARIO 1: 2026-03-16 (Test trajets) ===' as info;
SELECT 
    TO_CHAR(date_heure, 'HH24:MI') as heure,
    COUNT(*) as nb_res,
    SUM(nbPassager) as passagers,
    STRING_AGG(id_client || '(' || nbPassager || 'p)', ', ' ORDER BY nbPassager DESC) as detail
FROM reservations
WHERE DATE(date_heure) = '2026-03-16' AND id > 6
GROUP BY TO_CHAR(date_heure, 'HH24:MI')
ORDER BY heure;

SELECT '=== SCÉNARIO 2: 2026-03-17 (Test vagues) ===' as info;
SELECT 
    id_client,
    nbPassager,
    TO_CHAR(date_heure, 'HH24:MI:SS') as heure_exacte
FROM reservations
WHERE DATE(date_heure) = '2026-03-17'
ORDER BY date_heure;

-- ========================================
-- INSTRUCTIONS DE TEST
-- ========================================
-- 1. Exécutez: psql -U postgres -d framework_test -f database/reinit-10-03-2026-srint4.sql
-- 2. Exécutez: psql -U postgres -d framework_test -f database/test-sprint6-data.sql
-- 3. Testez chaque scénario:
--    - 2026-03-16: Priorité nombre de trajets (VOI0004 devrait être choisie pour Alice)
--    - 2026-03-17: Regroupement par vague (3 réservations en 1 vague, 1 en vague 2)
--    - 2026-03-18: Priorité carburant (H > E, D seul)
--    - 2026-03-19: Capacité insuffisante (non assignée)
--    - 2026-03-20: Remplissage optimal (3 réservations, 1-2 voitures)
-- ========================================

