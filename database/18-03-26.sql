-- ========================================
-- SCRIPT DE RÉINITIALISATION COMPLÈTE
-- Date: 18-03-2026
-- ========================================

-- ========================================
-- 1. SUPPRESSION DE TOUTES LES TABLES
-- ========================================
DROP TABLE IF EXISTS assignation CASCADE;
DROP TABLE IF EXISTS reservation_assignation CASCADE;
DROP TABLE IF EXISTS distance CASCADE;
DROP TABLE IF EXISTS reservations CASCADE;
DROP TABLE IF EXISTS Voiture CASCADE;
DROP TABLE IF EXISTS Carburant CASCADE;
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
    disponibilite TIME NOT NULL DEFAULT '00:00:00',
   PRIMARY KEY(idVoiture)
);

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
    date_heure_depart TIMESTAMP,
    date_heure_arrivee TIMESTAMP,
    FOREIGN KEY (id_voiture) REFERENCES Voiture(idVoiture)
);

-- Table de liaison assignation <-> reservations (ordre d'itinéraire)
CREATE TABLE reservation_assignation(
    id SERIAL PRIMARY KEY,
    id_assignation INT NOT NULL,
    id_reservation INT NOT NULL,
    ordre_itineraire INT NOT NULL,
    FOREIGN KEY (id_assignation) REFERENCES assignation(id),
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
INSERT INTO Voiture (Capacite, ref_, carburant, disponibilite) VALUES (5, 'vehicule1', 'D', '09:00:00');
INSERT INTO Voiture (Capacite, ref_, carburant, disponibilite) VALUES (5, 'vehicule2', 'E', '09:00:00');
INSERT INTO Voiture (Capacite, ref_, carburant, disponibilite) VALUES (12, 'vehicule3', 'D', '08:00:00');
INSERT INTO Voiture (Capacite, ref_, carburant, disponibilite) VALUES (9, 'vehicule4', 'D', '09:00:00');
INSERT INTO Voiture (Capacite, ref_, carburant, disponibilite) VALUES (12, 'vehicule5', 'E', '13:00:00');

INSERT INTO lieu (code, libelle, type) VALUES ('AER', 'Aéroport Ivato', 'aeroport');
INSERT INTO lieu (code, libelle, type) VALUES ('HOT', 'hotel1', 'hotel');  
INSERT INTO lieu (code, libelle, type) VALUES ('HOT', 'hotel2', 'hotel');  

INSERT INTO distance (id_from, id_to, kilometer) VALUES (1, 2, 90);
INSERT INTO distance (id_from, id_to, kilometer) VALUES (1, 3, 35);
INSERT INTO distance (id_from, id_to, kilometer) VALUES (2, 3, 60);

INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)  VALUES (2, 'client1', 7, '2026-03-19 09:00:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)  VALUES (3, 'client2', 20, '2026-03-19 08:00:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)  VALUES (2, 'client3', 3, '2026-03-19 09:10:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)  VALUES (2, 'client4', 10, '2026-03-19 09:15:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)  VALUES (2, 'client5', 5, '2026-03-19 09:20:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure)  VALUES (2, 'client6', 12, '2026-03-19 13:30:00');

INSERT INTO parametre (nom, valeur) VALUES ('app_version', '2.0.0');
INSERT INTO parametre (nom, valeur) VALUES ('maintenance_mode', 'false');
INSERT INTO parametre (nom, valeur) VALUES ('max_reservations_per_day', '100');
INSERT INTO parametre (nom, valeur) VALUES ('VM', '50'); -- Vitesse Moyenne en km/h
INSERT INTO parametre (nom, valeur) VALUES ('temps_attente', '30'); -- Temps d'attente en minutes pour le groupement des vagues

-- INSERT INTO assignation (id_voiture, date_heure_depart, date_heure_arrivee) VALUES (1, '2026-03-18 00:00:00', '2026-03-18 09:05:00');

-- ========================================
-- 4. DONNEES DE TEST - REGLES NA / RETOUR VOITURE
-- Date cible de test: 2026-03-20
-- ========================================

-- Vehicules supplementaires pour les scenarios de priorite NA
INSERT INTO Voiture (Capacite, ref_, carburant, disponibilite) VALUES (7, 'scenario_v1', 'D', '09:00:00');
INSERT INTO Voiture (Capacite, ref_, carburant, disponibilite) VALUES (4, 'scenario_v2', 'E', '09:00:00');
INSERT INTO Voiture (Capacite, ref_, carburant, disponibilite) VALUES (6, 'scenario_v3', 'D', '09:30:00');
INSERT INTO Voiture (Capacite, ref_, carburant, disponibilite) VALUES (4, 'scenario_v4', 'H', '11:20:00');

-- Reservations de test (scenes 1/2/3 demandees)
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure) VALUES (2, 'SCN_R1', 7, '2026-03-20 09:00:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure) VALUES (3, 'SCN_R2', 5, '2026-03-20 09:10:00');

INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure) VALUES (2, 'SCN_R3', 5, '2026-03-20 10:20:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure) VALUES (3, 'SCN_R4', 2, '2026-03-20 10:10:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure) VALUES (2, 'SCN_R5', 4, '2026-03-20 10:10:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure) VALUES (3, 'SCN_R6', 7, '2026-03-20 10:30:00');

INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure) VALUES (2, 'SCN_R7', 5, '2026-03-20 11:20:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure) VALUES (3, 'SCN_R8', 2, '2026-03-20 11:20:00');
INSERT INTO reservations (id_lieu, id_client, nbPassager, date_heure) VALUES (2, 'SCN_R9', 3, '2026-03-20 11:30:00');

-- Intervalles occupes existants en base pour simuler les retours voiture
-- scenario_v1 et scenario_v2 reviennent a 10:10
INSERT INTO assignation (id_voiture, date_heure_depart, date_heure_arrivee)
SELECT idVoiture, '2026-03-20 09:20:00', '2026-03-20 10:10:00' FROM Voiture WHERE ref_ = 'scenario_v1';

INSERT INTO assignation (id_voiture, date_heure_depart, date_heure_arrivee)
SELECT idVoiture, '2026-03-20 09:30:00', '2026-03-20 10:10:00' FROM Voiture WHERE ref_ = 'scenario_v2';

-- scenario_v3 revient a 11:20 pour la derniere scene
INSERT INTO assignation (id_voiture, date_heure_depart, date_heure_arrivee)
SELECT idVoiture, '2026-03-20 10:40:00', '2026-03-20 11:20:00' FROM Voiture WHERE ref_ = 'scenario_v3';

-- Parametre recommande pour reproduire exactement les scenes
-- UPDATE parametre SET valeur = '60' WHERE nom = 'temps_attente';