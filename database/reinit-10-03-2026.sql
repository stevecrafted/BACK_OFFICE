-- ========================================
-- SCRIPT DE RÉINITIALISATION COMPLÈTE
-- Date: 10-03-2026
-- ========================================
-- Modifications:
-- - Table Carburant supprimée, carburant directement dans Voiture (E/H/D)
-- - Table hotel remplacée par lieu (id, code, libelle, type)

-- ========================================
-- 1. SUPPRESSION DE TOUTES LES TABLES
-- ========================================
DROP TABLE IF EXISTS assignation CASCADE;
DROP TABLE IF EXISTS distance CASCADE;
DROP TABLE IF EXISTS reservations CASCADE;
DROP TABLE IF EXISTS Voiture CASCADE;
DROP TABLE IF EXISTS Carburant CASCADE;
DROP TABLE IF EXISTS hotel CASCADE;
DROP TABLE IF EXISTS lieu CASCADE;
DROP TABLE IF EXISTS token_validite CASCADE;
DROP TABLE IF EXISTS parametre CASCADE;

-- ========================================
-- 2. CRÉATION DES TABLES
-- ========================================

-- Table Voiture (carburant intégré: E=Essence, H=Hybride, D=Diesel)
CREATE TABLE Voiture(
   idVoiture SERIAL,
   Capacite INTEGER,
   ref_ VARCHAR(50),
   carburant CHAR(1) NOT NULL CHECK (carburant IN ('E', 'H', 'D')),
   PRIMARY KEY(idVoiture)
);

-- Table Lieu (remplace hotel)
CREATE TABLE lieu(
    id SERIAL,
    code VARCHAR(50) NOT NULL,
    libelle VARCHAR(250) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('hotel', 'aeroport')),
    PRIMARY KEY(id)
);

-- Table Distance
CREATE TABLE distance(
    id SERIAL,
    id_from INT NOT NULL,
    id_to INT NOT NULL,
    kilometer DECIMAL(10,2) NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY(id_from) REFERENCES lieu(id),
    FOREIGN KEY(id_to) REFERENCES lieu(id),
    CHECK (id_from < id_to)
);

-- Table Reservations
CREATE TABLE reservations(
    id SERIAL PRIMARY KEY,
    id_lieu INT NOT NULL,
    id_client VARCHAR(250),
    nbPassager INT NOT NULL,
    date_heure TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_lieu) REFERENCES lieu(id)
);

-- Table Assignation (lien entre Voiture et Reservation)
CREATE TABLE assignation(
    id SERIAL PRIMARY KEY,
    id_voiture INT NOT NULL,
    id_reservation INT NOT NULL,
    date_assignation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_voiture) REFERENCES Voiture(idVoiture),
    FOREIGN KEY (id_reservation) REFERENCES reservations(id),
    UNIQUE(id_reservation)
);

-- Table Parametre
CREATE TABLE parametre(
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) UNIQUE NOT NULL,
    valeur VARCHAR(250)
);

-- Table Token Validite
CREATE TABLE token_validite(
    id SERIAL PRIMARY KEY,
    token VARCHAR(250) UNIQUE NOT NULL,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_expiration TIMESTAMP NOT NULL
);

-- ========================================
-- 3. INSERTION DES DONNÉES DE BASE
-- ========================================

-- Voitures (carburant: E=Essence, H=Hybride, D=Diesel)
INSERT INTO Voiture (Capacite, ref_, carburant) VALUES (5, 'VOI0001', 'E');
INSERT INTO Voiture (Capacite, ref_, carburant) VALUES (7, 'VOI0002', 'D');
INSERT INTO Voiture (Capacite, ref_, carburant) VALUES (4, 'VOI0003', 'E');
INSERT INTO Voiture (Capacite, ref_, carburant) VALUES (5, 'VOI0004', 'H');

-- Lieux (1 aeroport + 4 hotels)
INSERT INTO lieu (code, libelle, type) VALUES ('AER', 'Aéroport Ivato', 'aeroport');
INSERT INTO lieu (code, libelle, type) VALUES ('COL', 'Colbert', 'hotel');
INSERT INTO lieu (code, libelle, type) VALUES ('NOV', 'Novotel', 'hotel');
INSERT INTO lieu (code, libelle, type) VALUES ('IBI', 'Ibis', 'hotel');
INSERT INTO lieu (code, libelle, type) VALUES ('LOK', 'Lokanga', 'hotel');

-- Distances entre lieux (id_from < id_to)
-- id=1: Aéroport, id=2: Colbert, id=3: Novotel, id=4: Ibis, id=5: Lokanga
INSERT INTO distance (id_from, id_to, kilometer) VALUES (1, 2, 5.00);  -- Aéroport <-> Colbert
INSERT INTO distance (id_from, id_to, kilometer) VALUES (1, 3, 9.00);  -- Aéroport <-> Novotel
INSERT INTO distance (id_from, id_to, kilometer) VALUES (1, 4, 8.00);  -- Aéroport <-> Ibis
INSERT INTO distance (id_from, id_to, kilometer) VALUES (1, 5, 3.00);  -- Aéroport <-> Lokanga
INSERT INTO distance (id_from, id_to, kilometer) VALUES (2, 3, 6.00);  -- Colbert <-> Novotel
INSERT INTO distance (id_from, id_to, kilometer) VALUES (2, 4, 12.00); -- Colbert <-> Ibis
INSERT INTO distance (id_from, id_to, kilometer) VALUES (2, 5, 8.00);  -- Colbert <-> Lokanga
INSERT INTO distance (id_from, id_to, kilometer) VALUES (3, 4, 4.00);  -- Novotel <-> Ibis
INSERT INTO distance (id_from, id_to, kilometer) VALUES (3, 5, 11.00); -- Novotel <-> Lokanga
INSERT INTO distance (id_from, id_to, kilometer) VALUES (4, 5, 9.00);  -- Ibis <-> Lokanga

-- Paramètres système
INSERT INTO parametre (nom, valeur) VALUES ('app_version', '2.0.0');
INSERT INTO parametre (nom, valeur) VALUES ('maintenance_mode', 'false');
INSERT INTO parametre (nom, valeur) VALUES ('max_reservations_per_day', '100');
INSERT INTO parametre (nom, valeur) VALUES ('VM', '60'); -- Vitesse Moyenne en km/h

-- ========================================
-- 4. DONNÉES DE TEST POUR LA SIMULATION
-- ========================================

-- Scénario 1: Même date_heure (même minute) - 2 réservations groupées
-- Ces 2 réservations doivent être traitées ensemble (même date, heure, minute)
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (2, 'CLIENT001', 4, '2026-03-15 10:30:00'); -- Colbert, 4 passagers

INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (3, 'CLIENT002', 2, '2026-03-15 10:30:00'); -- Novotel, 2 passagers (même minute)

-- Scénario 2: Même date_heure (même minute) - 3 réservations groupées
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (4, 'CLIENT003', 3, '2026-03-15 14:00:00'); -- Ibis, 3 passagers

INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (5, 'CLIENT004', 2, '2026-03-15 14:00:00'); -- Lokanga, 2 passagers

INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (2, 'CLIENT005', 1, '2026-03-15 14:00:00'); -- Colbert, 1 passager

-- Scénario 3: Réservation seule avec beaucoup de passagers (test capacité)
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (3, 'CLIENT006', 6, '2026-03-15 16:00:00'); -- Novotel, 6 passagers (seule VOI0002 a 7 places)

-- Scénario 4: Autre date pour tester le filtre par date
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (4, 'CLIENT007', 2, '2026-03-20 09:00:00'); -- Ibis, 2 passagers

INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (5, 'CLIENT008', 3, '2026-03-20 09:00:00'); -- Lokanga, 3 passagers

-- Scénario 5: Test priorité carburant (D > H > E)
-- Réservation avec 5 passagers -> VOI0001(5,E) et VOI0004(5,H) sont candidates
-- VOI0004(H) devrait être choisie (H > E)
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (2, 'CLIENT009', 5, '2026-03-22 11:00:00'); -- Colbert, 5 passagers

-- Scénario 6: Test dépassement capacité (aucune voiture ne peut)
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)
VALUES (3, 'CLIENT010', 10, '2026-03-25 08:00:00'); -- 10 passagers, max voiture = 7

-- ========================================
-- 5. VÉRIFICATION
-- ========================================
SELECT 'Voitures: ' || COUNT(*) FROM Voiture;
SELECT 'Lieux: ' || COUNT(*) FROM lieu;
SELECT 'Distances: ' || COUNT(*) FROM distance;
SELECT 'Réservations: ' || COUNT(*) FROM reservations;
SELECT 'Assignations: ' || COUNT(*) FROM assignation;
SELECT 'Paramètres: ' || COUNT(*) FROM parametre;

-- ========================================
-- FIN DU SCRIPT
-- ========================================
