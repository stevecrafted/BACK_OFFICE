DELETE FROM assignation;
DELETE FROM reservations;
-- Vague 08:00
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure) VALUES (2, 'CLI-001', 5, '2026-03-10 08:00:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure) VALUES (2, 'CLI-002', 5, '2026-03-10 08:00:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure) VALUES (2, 'CLI-003', 5, '2026-03-10 08:00:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure) VALUES (2, 'CLI-004', 5, '2026-03-10 08:00:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure) VALUES (3, 'CLI-004', 5, '2026-03-10 08:00:00');