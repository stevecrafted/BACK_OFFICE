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
INSERT INTO Voiture (Capacite, ref_, carburant) VALUES (12, 'vehicule1', 'D');
INSERT INTO Voiture (Capacite, ref_, carburant) VALUES (12, 'vehicule2', 'E');

INSERT INTO Voiture (Capacite, ref_, carburant) VALUES (5, 'vehicule3', 'D');
INSERT INTO Voiture (Capacite, ref_, carburant) VALUES (5, 'vehicule4', 'E');

-- Lieux (1 aeroport + 4 hotels)
INSERT INTO lieu (code, libelle, type) VALUES ('AER', 'Aéroport Ivato', 'aeroport');
INSERT INTO lieu (code, libelle, type) VALUES ('HOT', 'hotel1', 'hotel');  
INSERT INTO lieu (code, libelle, type) VALUES ('HOT', 'hotel2', 'hotel');  

-- Distances entre lieux (id_from < id_to)
-- id=1: Aéroport, id=2: Colbert, id=3: Novotel, id=4: Ibis, id=5: Lokanga
INSERT INTO distance (id_from, id_to, kilometer) VALUES (1, 2, 5);
INSERT INTO distance (id_from, id_to, kilometer) VALUES (2, 3, 10);

INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)  VALUES (2, 'client1', 7, '2026-03-18 09:00:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)  VALUES (2, 'client2', 11, '2026-03-18 09:00:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)  VALUES (2, 'client3', 3, '2026-03-18 09:00:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)  VALUES (2, 'client4', 1, '2026-03-18 09:00:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)  VALUES (2, 'client5', 2, '2026-03-18 09:00:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)  VALUES (2, 'client6', 20, '2026-03-18 09:00:00');

-- Paramètres système
INSERT INTO parametre (nom, valeur) VALUES ('app_version', '2.0.0');
INSERT INTO parametre (nom, valeur) VALUES ('maintenance_mode', 'false');
INSERT INTO parametre (nom, valeur) VALUES ('max_reservations_per_day', '100');
INSERT INTO parametre (nom, valeur) VALUES ('VM', '60'); -- Vitesse Moyenne en km/h
INSERT INTO parametre (nom, valeur) VALUES ('temps_attente', '30'); -- Temps d'attente en minutes pour le groupement des vagues
 