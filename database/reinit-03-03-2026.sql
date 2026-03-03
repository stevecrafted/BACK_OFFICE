-- ========================================
-- SCRIPT DE RÉINITIALISATION COMPLÈTE
-- Date: 03-03-2026
-- ========================================
-- Ce script supprime toutes les tables et les recrée avec les données de test

-- ========================================
-- 1. SUPPRESSION DE TOUTES LES TABLES
-- ========================================
DROP TABLE IF EXISTS assignation CASCADE;
DROP TABLE IF EXISTS distance CASCADE;
DROP TABLE IF EXISTS reservations CASCADE;
DROP TABLE IF EXISTS Voiture CASCADE;
DROP TABLE IF EXISTS Carburant CASCADE;
DROP TABLE IF EXISTS hotel CASCADE;
DROP TABLE IF EXISTS token_validite CASCADE;
DROP TABLE IF EXISTS parametre CASCADE;

-- ========================================
-- 2. CRÉATION DES TABLES
-- ========================================

-- Table Carburant
CREATE TABLE Carburant(
   idCarburant SERIAL,
   libelle VARCHAR(50),
   PRIMARY KEY(idCarburant)
);

-- Table Voiture
CREATE TABLE Voiture(
   idVoiture SERIAL,
   Capacite INTEGER,
   ref_ VARCHAR(50),
   idCarburant INTEGER NOT NULL,
   PRIMARY KEY(idVoiture),
   FOREIGN KEY(idCarburant) REFERENCES Carburant(idCarburant)
);

-- Table Hotel
CREATE TABLE hotel(
    id SERIAL,
    nom VARCHAR(250) NOT NULL,
    PRIMARY KEY(id)
);

-- Table Distance
CREATE TABLE distance(
    id SERIAL,
    id_from INT NOT NULL,
    id_to INT NOT NULL,
    kilometer DECIMAL(10,2) NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY(id_from) REFERENCES hotel(id),
    FOREIGN KEY(id_to) REFERENCES hotel(id),
    CHECK (id_from < id_to)
);

-- Table Reservations
CREATE TABLE reservations(
    id SERIAL PRIMARY KEY,
    id_hotel INT NOT NULL,
    id_client VARCHAR(250),
    nbPassager INT NOT NULL,
    date_heure TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_hotel) REFERENCES hotel(id)
);

-- Table Assignation (lien entre Voiture et Reservation)
CREATE TABLE assignation(
    id SERIAL PRIMARY KEY,
    id_voiture INT NOT NULL,
    id_reservation INT NOT NULL,
    date_assignation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_voiture) REFERENCES Voiture(idVoiture),
    FOREIGN KEY (id_reservation) REFERENCES reservations(id),
    UNIQUE(id_reservation) -- Une réservation ne peut être assignée qu'une seule fois
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
-- 3. INSERTION DES DONNÉES DE TEST
-- ========================================

-- Carburants
INSERT INTO Carburant (libelle) VALUES ('Essence');
INSERT INTO Carburant (libelle) VALUES ('Diesel');
INSERT INTO Carburant (libelle) VALUES ('Électrique');
INSERT INTO Carburant (libelle) VALUES ('Hybride');

-- Voitures (le ref sera généré automatiquement par l'application)
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (5, 'VOI0001', 1);
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (7, 'VOI0002', 2);
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (4, 'VOI0003', 1);
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (5, 'VOI0004', 3);

-- Hotels
INSERT INTO hotel (nom) VALUES ('Colbert');
INSERT INTO hotel (nom) VALUES ('Novotel');
INSERT INTO hotel (nom) VALUES ('Ibis');
INSERT INTO hotel (nom) VALUES ('Lokanga');
INSERT INTO hotel (nom) VALUES ('test');

-- Distances entre hotels
INSERT INTO distance (id_from, id_to, kilometer) VALUES (1, 2, 5.00);  -- TNR <-> MJN
INSERT INTO distance (id_from, id_to, kilometer) VALUES (1, 3, 9.00);  -- TNR <-> TLE
INSERT INTO distance (id_from, id_to, kilometer) VALUES (1, 4, 8.00);  -- TNR <-> FTU
INSERT INTO distance (id_from, id_to, kilometer) VALUES (1, 5, 3.00);  -- TNR <-> TMM
INSERT INTO distance (id_from, id_to, kilometer) VALUES (2, 3, 6.00);  -- MJN <-> TLE
INSERT INTO distance (id_from, id_to, kilometer) VALUES (2, 4, 12.00); -- MJN <-> FTU
INSERT INTO distance (id_from, id_to, kilometer) VALUES (2, 5, 8.00);  -- MJN <-> TMM
INSERT INTO distance (id_from, id_to, kilometer) VALUES (3, 4, 4.00);  -- TLE <-> FTU
INSERT INTO distance (id_from, id_to, kilometer) VALUES (3, 5, 11.00); -- TLE <-> TMM
INSERT INTO distance (id_from, id_to, kilometer) VALUES (4, 5, 9.00);  -- FTU <-> TMM

-- Réservations de test
INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (1, 'CLIENT001', 2, '2026-03-10 14:30:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (2, 'CLIENT002', 4, '2026-03-15 10:00:00');

INSERT INTO reservations (id_hotel, id_client, nbPassager, date_heure) 
VALUES (3, 'CLIENT003', 3, '2026-03-20 16:45:00');

-- Paramètres système
INSERT INTO parametre (nom, valeur) VALUES ('app_version', '1.0.0');
INSERT INTO parametre (nom, valeur) VALUES ('maintenance_mode', 'false');
INSERT INTO parametre (nom, valeur) VALUES ('max_reservations_per_day', '100');
INSERT INTO parametre (nom, valeur) VALUES ('VM', '60'); -- Vitesse Moyenne en km/h

-- ========================================
-- 4. VÉRIFICATION
-- ========================================
SELECT 'Carburants: ' || COUNT(*) FROM Carburant;
SELECT 'Voitures: ' || COUNT(*) FROM Voiture;
SELECT 'Hotels: ' || COUNT(*) FROM hotel;
SELECT 'Distances: ' || COUNT(*) FROM distance;
SELECT 'Réservations: ' || COUNT(*) FROM reservations;
SELECT 'Assignations: ' || COUNT(*) FROM assignation;
SELECT 'Paramètres: ' || COUNT(*) FROM parametre;

-- ========================================
-- FIN DU SCRIPT
-- ========================================
