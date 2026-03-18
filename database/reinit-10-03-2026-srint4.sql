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

-- Voitures (carburant: E=Essence,  D=Diesel)
INSERT INTO Voiture (Capacite, ref_, carburant) VALUES (5, 'VOI0001', 'E');
INSERT INTO Voiture (Capacite, ref_, carburant) VALUES (5, 'VOI0002', 'D');

INSERT INTO Voiture (Capacite, ref_, carburant) VALUES (5, 'VOI0003', 'E');
INSERT INTO Voiture (Capacite, ref_, carburant) VALUES (5, 'VOI0004', 'D');

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
INSERT INTO parametre (nom, valeur) VALUES ('temps_attente', '30'); -- Temps d'attente en minutes pour le groupement des vagues
 