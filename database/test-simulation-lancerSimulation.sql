-- Supprimer les reservations existantes et assignations pour repartir proprement
DELETE FROM assignation;
DELETE FROM reservations;

-- ========================================================
-- 2026-03-12 
-- ========================================================
-- === 08:00 === 
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) VALUES (2, 'CLI-A02', 4, '2026-03-12 08:00:00');
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) VALUES (3, 'CLI-A03', 1, '2026-03-12 08:00:00');
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) VALUES (3, 'CLI-HAHAHA4', 1, '2026-03-12 08:16:00');
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) VALUES (4, 'CLI-HAHAHA5', 1, '2026-03-12 08:15:00');
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) VALUES (4, 'CLI-HAHA1', 12, '2026-03-12 08:20:00');
