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

-- Voitures (le ref sera généré automatiquement par l'application)
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (5, 'VOI0001', 1);
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (7, 'VOI0002', 2);
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (4, 'VOI0003', 1);
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (5, 'VOI0004', 3);

-- Hotels
-- 1. Ajouter l'Aéroport dans la table hotel
INSERT INTO hotel (nom) VALUES ('Aeroport');
INSERT INTO parametre (nom, valeur) 
VALUES ('AEROPORT_HOTEL_ID', (SELECT id FROM hotel WHERE nom = 'Aeroport'));

INSERT INTO hotel (nom) VALUES ('Colbert');
INSERT INTO hotel (nom) VALUES ('Novotel');
INSERT INTO hotel (nom) VALUES ('Ibis');
INSERT INTO hotel (nom) VALUES ('Lokanga');
INSERT INTO hotel (nom) VALUES ('test'); 

-- Colbert (id=1) ↔ Aeroport (id=6) : 7 km
INSERT INTO distance (id_from, id_to, kilometer) VALUES (1, 6, 7.00);
-- Novotel (id=2) ↔ Aeroport (id=6) : 4 km
INSERT INTO distance (id_from, id_to, kilometer) VALUES (2, 6, 4.00);
-- Ibis (id=3) ↔ Aeroport (id=6) : 10 km
INSERT INTO distance (id_from, id_to, kilometer) VALUES (3, 6, 10.00);
-- Lokanga (id=4) ↔ Aeroport (id=6) : 6 km
INSERT INTO distance (id_from, id_to, kilometer) VALUES (4, 6, 6.00);
-- test (id=5) ↔ Aeroport (id=6) : 3 km
INSERT INTO distance (id_from, id_to, kilometer) VALUES (5, 6, 3.00);

