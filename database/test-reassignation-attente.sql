-- ========================================
-- TEST: RÉASSIGNATION PENDANT LE TEMPS D'ATTENTE
-- Date: 2026-03-21
-- ========================================
-- Objectif: Vérifier qu'une voiture diesel qui devient disponible
-- pendant la fenêtre d'attente remplace une voiture essence

-- Nettoyer les données existantes
DELETE FROM assignation;
DELETE FROM reservations;

-- ========================================
-- SCÉNARIO: Test GARANTI de réassignation
-- Date: 2026-03-21
-- ========================================
-- On force les conditions pour que la réassignation se produise:
-- - VOI0001 (5pl Essence) a fait 2 trajets aujourd'hui, DISPONIBLE
-- - VOI0004 (5pl Hybride) a fait 0 trajet et devient disponible pendant l'attente

-- Trajets existants pour VOI0001 (2 trajets ce matin)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (1, 'MATIN_V1_1', 3, '2026-03-21 06:00:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (2, 'MATIN_V1_2', 2, '2026-03-21 07:00:00');

INSERT INTO assignation (id_voiture, id_reservation, date_assignation)
VALUES (1, 1, '2026-03-21 05:45:00');

INSERT INTO assignation (id_voiture, id_reservation, date_assignation)
VALUES (1, 2, '2026-03-21 06:45:00');

-- VOI0004 est en trajet (termine vers 08:05)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (1, 'TRAJET_V4', 2, '2026-03-21 07:30:00'); -- Court trajet

INSERT INTO assignation (id_voiture, id_reservation, date_assignation)
VALUES (4, 3, '2026-03-21 07:20:00');

-- ========================================
-- Réservations à simuler (Fenêtre 08:00 - 08:30)
-- ========================================

-- 08:00: 5 passagers -> VOI0001 (seule 5pl dispo, mais 2 trajets)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (3, 'TestReassign1', 5, '2026-03-21 08:00:00');

-- 08:15: repousse le départ à 08:15
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (3, 'TestReassign2', 0, '2026-03-21 08:15:00');

-- ========================================
-- RÉSULTAT ATTENDU:
-- ========================================
-- 1. Assignation initiale à VOI0001 (5pl Essence, 2 trajets)
--    (VOI0004 en trajet à 08:00)
-- 2. Heure de départ = 08:15 (dernière résa de la vague)
-- 3. À ~08:05, VOI0004 (5pl Hybride, 1 trajet après) termine
-- 4. 08:05 < 08:15 donc candidate
-- 5. Comparaison:
--    - Écart: égalité (0)
--    - Trajets: VOI0001(2) vs VOI0004(1) -> VOI0004 meilleur!
-- => RÉASSIGNATION: VOI0001 -> VOI0004
-- Message attendu: "RÉASSIGNATION: Voiture #1 -> Voiture #4"

-- ========================================
-- VÉRIFICATIONS
-- ========================================
SELECT '=== VOITURES ===' as info;
SELECT idVoiture, ref_, Capacite, carburant FROM Voiture ORDER BY idVoiture;

SELECT '=== ASSIGNATIONS EXISTANTES (2026-03-21) ===' as info;
SELECT
    a.id_voiture,
    v.ref_,
    r.id_client,
    r.date_heure
FROM assignation a
JOIN reservations r ON a.id_reservation = r.id
JOIN Voiture v ON a.id_voiture = v.idVoiture
WHERE DATE(r.date_heure) = '2026-03-21'
ORDER BY r.date_heure;

SELECT '=== RÉSERVATIONS À SIMULER ===' as info;
SELECT
    r.id,
    r.id_client,
    r.nbPassager,
    r.date_heure
FROM reservations r
LEFT JOIN assignation a ON r.id = a.id_reservation
WHERE a.id IS NULL AND DATE(r.date_heure) = '2026-03-21'
ORDER BY r.date_heure;

-- ========================================
-- INSTRUCTIONS DE TEST
-- ========================================
-- 1. Exécuter ce script
-- 2. Simuler la date 2026-03-21
-- 3. Vérifier le message: "RÉASSIGNATION: Voiture #1 -> Voiture #4"
