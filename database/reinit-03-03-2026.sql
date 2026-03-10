\c postgres
DROP DATABASE gde;
CREATE DATABASE gde;
\c gde

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
INSERT INTO Carburant (libelle) VALUES ('Essence');      -- id=1
INSERT INTO Carburant (libelle) VALUES ('Diesel');       -- id=2 

-- Voitures
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (5, 'VOI0001', 1);  -- id=1, Essence
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (7, 'VOI0002', 2);  -- id=2, Diesel
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (4, 'VOI0003', 1);  -- id=3, Essence
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (5, 'VOI0004', 2);  -- id=4, Électrique

-- Hotels 
INSERT INTO hotel (nom) VALUES ('Aeroport');  -- id=1
INSERT INTO hotel (nom) VALUES ('Colbert');   -- id=2
INSERT INTO hotel (nom) VALUES ('Novotel');   -- id=3
INSERT INTO hotel (nom) VALUES ('Ibis');      -- id=4
INSERT INTO hotel (nom) VALUES ('Lokanga');   -- id=5
INSERT INTO hotel (nom) VALUES ('test');      -- id=6

-- Paramètres système
INSERT INTO parametre (nom, valeur) VALUES ('AEROPORT_HOTEL_ID', (SELECT id FROM hotel WHERE nom = 'Aeroport'));
INSERT INTO parametre (nom, valeur) VALUES ('VM', '60');
INSERT INTO parametre (nom, valeur) VALUES ('app_version', '1.0.0');

-- ========================================
-- Distances depuis l'Aéroport (id=1)
-- ========================================
INSERT INTO distance (id_from, id_to, kilometer) VALUES (1, 2, 7.00);   -- Aeroport ↔ Colbert
INSERT INTO distance (id_from, id_to, kilometer) VALUES (1, 3, 4.00);   -- Aeroport ↔ Novotel
INSERT INTO distance (id_from, id_to, kilometer) VALUES (1, 4, 10.00);  -- Aeroport ↔ Ibis
INSERT INTO distance (id_from, id_to, kilometer) VALUES (1, 5, 6.00);   -- Aeroport ↔ Lokanga
INSERT INTO distance (id_from, id_to, kilometer) VALUES (1, 6, 3.00);   -- Aeroport ↔ test

-- ========================================
-- Distances entre hôtels
-- ========================================
INSERT INTO distance (id_from, id_to, kilometer) VALUES (2, 3, 5.00);   -- Colbert ↔ Novotel
INSERT INTO distance (id_from, id_to, kilometer) VALUES (2, 4, 9.00);   -- Colbert ↔ Ibis
INSERT INTO distance (id_from, id_to, kilometer) VALUES (2, 5, 8.00);   -- Colbert ↔ Lokanga
INSERT INTO distance (id_from, id_to, kilometer) VALUES (2, 6, 3.00);   -- Colbert ↔ test
INSERT INTO distance (id_from, id_to, kilometer) VALUES (3, 4, 6.00);   -- Novotel ↔ Ibis
INSERT INTO distance (id_from, id_to, kilometer) VALUES (3, 5, 12.00);  -- Novotel ↔ Lokanga
INSERT INTO distance (id_from, id_to, kilometer) VALUES (3, 6, 8.00);   -- Novotel ↔ test
INSERT INTO distance (id_from, id_to, kilometer) VALUES (4, 5, 4.00);   -- Ibis ↔ Lokanga
INSERT INTO distance (id_from, id_to, kilometer) VALUES (4, 6, 11.00);  -- Ibis ↔ test
INSERT INTO distance (id_from, id_to, kilometer) VALUES (5, 6, 9.00);   -- Lokanga ↔ test

-- ========================================
-- 4. VÉRIFICATION
-- ========================================
SELECT 'Carburants: ' || COUNT(*) FROM Carburant;
SELECT 'Voitures: ' || COUNT(*) FROM Voiture;
SELECT 'Hotels: ' || COUNT(*) FROM hotel;
SELECT 'Distances: ' || COUNT(*) FROM distance;
SELECT 'Paramètres: ' || COUNT(*) FROM parametre;

