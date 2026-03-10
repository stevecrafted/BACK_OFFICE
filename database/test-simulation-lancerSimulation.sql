-- ========================================================
-- DONNeES DE TEST POUR lancerSimulation
-- ========================================================
-- Ce script ajoute des reservations realistes pour tester
-- la simulation d'assignation de voitures.
--
-- Prerequis : executer reinit-03-03-2026.sql d'abord
--
-- Rappel des voitures existantes :
--   VOI0001 : 5 places | Essence
--   VOI0002 : 7 places | Diesel
--   VOI0003 : 4 places | Essence
--   VOI0004 : 5 places | electrique
--
-- Rappel des hôtels existants :
--   1 = Colbert | 2 = Novotel | 3 = Ibis | 4 = Lokanga | 5 = test
-- ========================================================

-- Supprimer les reservations existantes et assignations pour repartir proprement
DELETE FROM assignation;
DELETE FROM reservations;

-- ========================================================
-- SCeNARIO A : DATE 2026-03-12
-- Test complet multi-vagues avec des cas varies
-- ========================================================

-- === VAGUE 1 : 08:00 (3 reservations a la même heure) ===
-- Teste : regroupement par vague, tri par nbPassager decroissant,
--         remplissage optimal, priorite Diesel

-- R1 : 6 passagers → devrait prendre VOI0002 (7 places, Diesel, seule a pouvoir accueillir 6)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (1, 'CLI-A01', 6, '2026-03-12 08:00:00');

-- R2 : 4 passagers → devrait prendre VOI0001 ou VOI0004 (5 places chacune), ou VOI0003 (4 places, ecart=0)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (2, 'CLI-A02', 4, '2026-03-12 08:00:00');

-- R3 : 1 passager → pourrait être ajoute a une voiture existante si places restantes (remplissage optimal)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (3, 'CLI-A03', 1, '2026-03-12 08:00:00');

-- === VAGUE 2 : 10:30 (2 reservations) ===
-- Les voitures de la vague 1 ne sont PAS reutilisees ici (voituresUtilisees)

-- R4 : 3 passagers
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (4, 'CLI-A04', 3, '2026-03-12 10:30:00');

-- R5 : 2 passagers → pourrait être ajoute a la même voiture que R4 si places restantes
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (1, 'CLI-A05', 2, '2026-03-12 10:30:00');

-- === VAGUE 3 : 14:00 (1 reservation) ===
-- Teste le cas où il ne reste potentiellement plus de voitures

-- R6 : 3 passagers
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (2, 'CLI-A06', 3, '2026-03-12 14:00:00');


-- ========================================================
-- SCeNARIO B : DATE 2026-03-15
-- Test de surcharge : trop de passagers, pas assez de voitures
-- ========================================================

-- === VAGUE UNIQUE : 09:00 (5 reservations, capacite totale = 21 places) ===
-- Capacite vehicules = 5 + 7 + 4 + 5 = 21 places au total

-- R7 : 7 passagers → VOI0002 (seule avec 7 places)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (1, 'CLI-B01', 7, '2026-03-15 09:00:00');

-- R8 : 5 passagers → VOI0001 ou VOI0004 (5 places)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (2, 'CLI-B02', 5, '2026-03-15 09:00:00');

-- R9 : 5 passagers → la dernière voiture de 5 places
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (3, 'CLI-B03', 5, '2026-03-15 09:00:00');

-- R10 : 4 passagers → VOI0003 (4 places, ecart=0)
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (4, 'CLI-B04', 4, '2026-03-15 09:00:00');

-- R11 : 3 passagers → AUCUNE voiture dispo → reservation non assignee !
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (1, 'CLI-B05', 3, '2026-03-15 09:00:00');


-- ========================================================
-- SCeNARIO C : DATE 2026-03-18
-- Test remplissage optimal : plusieurs petites reservations
-- dans une même voiture
-- ========================================================

-- === VAGUE 1 : 07:00 (4 petites reservations au même moment) ===

-- R12 : 2 passagers
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (1, 'CLI-C01', 2, '2026-03-18 07:00:00');

-- R13 : 2 passagers → devrait être ajoute a la même voiture que R12
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (2, 'CLI-C02', 2, '2026-03-18 07:00:00');

-- R14 : 1 passager → ajoute a une voiture existante ou nouvelle
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (3, 'CLI-C03', 1, '2026-03-18 07:00:00');

-- R15 : 1 passager → ajoute a une voiture existante
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (4, 'CLI-C04', 1, '2026-03-18 07:00:00');

-- === VAGUE 2 : 15:00 ===

-- R16 : 3 passagers
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (2, 'CLI-C05', 3, '2026-03-18 15:00:00');

-- R17 : 2 passagers
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (3, 'CLI-C06', 2, '2026-03-18 15:00:00');


-- ========================================================
-- SCeNARIO D : DATE 2026-03-20
-- Test passagers > capacite max → reservation impossible
-- ========================================================

-- R18 : 8 passagers → AUCUNE voiture n'a 8 places → non assignee
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (1, 'CLI-D01', 8, '2026-03-20 10:00:00');

-- R19 : 2 passagers → assignation normale a côte d'un echec
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (2, 'CLI-D02', 2, '2026-03-20 10:00:00');


-- ========================================================
-- SCeNARIO E : DATE 2026-03-22
-- Test vague unique simple (1 seule reservation)
-- ========================================================

-- R20 : 3 passagers → cas simple pour verifier le fonctionnement basique
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (4, 'CLI-E01', 3, '2026-03-22 11:00:00');


-- ========================================================
-- SCeNARIO F : DATE 2026-03-25
-- Test priorite Diesel : 2 voitures avec même ecart
-- ========================================================

-- R21 : 5 passagers → VOI0001 (Essence, 5 places) vs VOI0004 (electrique, 5 places)
--        vs VOI0002 (Diesel, 7 places, ecart=2)
--        → ecart min = 0 pour VOI0001 et VOI0004 → pas de diesel, choix aleatoire
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (1, 'CLI-F01', 5, '2026-03-25 09:00:00');

-- R22 : 6 passagers → VOI0002 (Diesel, 7 places, ecart=1) est la seule option
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (2, 'CLI-F02', 6, '2026-03-25 09:00:00');

-- R23 : 4 passagers → VOI0003 (Essence, 4 places, ecart=0) ou reste
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure)
VALUES (3, 'CLI-F03', 4, '2026-03-25 09:00:00');
 