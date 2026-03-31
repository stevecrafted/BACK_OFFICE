
DROP TABLE IF EXISTS Distance CASCADE;
DROP TABLE IF EXISTS Lieu_hotel CASCADE; 
DROP TABLE IF EXISTS hotel CASCADE; 
DROP TABLE IF EXISTS hotel Voyages; 

-- ========== CREATION DES TABLES ==========
 
-- Table Lieu_hotel (lieux / localisations)
CREATE TABLE Lieu_hotel(
    idLieuHotel SERIAL PRIMARY KEY,
    code VARCHAR(10) UNIQUE NOT NULL,
    libelle VARCHAR(250) NOT NULL
);
 
-- Table Hotel (liée à un lieu)
CREATE TABLE hotel (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(250) NOT NULL,
    id_lieu_hotel INT NOT NULL, --niampy anito colonne ito
    FOREIGN KEY (id_lieu_hotel) REFERENCES Lieu_hotel(idLieuHotel)
);
 
-- Table Distance (entre lieux, unidirectionnelle : pas besoin de to -> from)
CREATE TABLE Distance(
    idDistance SERIAL PRIMARY KEY,
    idDepart INTEGER NOT NULL,
    idArrive INTEGER NOT NULL,
    kilometer DOUBLE PRECISION NOT NULL,
    FOREIGN KEY(idDepart) REFERENCES Lieu_hotel(idLieuHotel),
    FOREIGN KEY(idArrive) REFERENCES Lieu_hotel(idLieuHotel),
    UNIQUE(idDepart, idArrive)
);

-- Table Voyages (planification véhicule <-> réservation)
CREATE TABLE Voyages(
    idVoyage SERIAL PRIMARY KEY, 
    idReservation INTEGER NOT NULL,
    date_heure_depart TIMESTAMP,
    date_heure_arrivee TIMESTAMP,
    FOREIGN KEY(idReservation) REFERENCES reservations(id)
);

    ALTER TABLE reservations 
    ADD COLUMN idVoiture INT;
    ALTER TABLE reservations
    ADD CONSTRAINT fk_voiture
    FOREIGN KEY (idVoiture)
    REFERENCES voiture(idVoiture);

-- ========== INSERTION DES DONNEES ==========

-- Carburants
INSERT INTO Carburant (libelle) VALUES ('Diesel');
INSERT INTO Carburant (libelle) VALUES ('Essence');

-- Lieux
INSERT INTO Lieu_hotel (code, libelle) VALUES ('TNR', 'Ivato Aeroport');        -- id=1 (BASE)
INSERT INTO Lieu_hotel (code, libelle) VALUES ('ANT', 'Antananarivo Centre');    -- id=2
INSERT INTO Lieu_hotel (code, libelle) VALUES ('AMB', 'Ambatobe');               -- id=3
INSERT INTO Lieu_hotel (code, libelle) VALUES ('ANK', 'Ankorondrano');           -- id=4
INSERT INTO Lieu_hotel (code, libelle) VALUES ('IVT', 'Ivato Ville');            -- id=5

-- Hotels (liés aux lieux)
INSERT INTO hotel (nom, id_lieu_hotel) VALUES ('Hotel California', 2);      -- Antananarivo Centre
INSERT INTO hotel (nom, id_lieu_hotel) VALUES ('Hotel Transylvania', 3);    -- Ambatobe
INSERT INTO hotel (nom, id_lieu_hotel) VALUES ('Hotel Royal Palace', 4);    -- Ankorondrano
INSERT INTO hotel (nom, id_lieu_hotel) VALUES ('Hotel Ivato', 5);           -- Ivato Ville

-- Voitures (2 x 5 places Essence + 1 x 7 places Diesel + 1 x 3 places Diesel)
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (5, 'VOI0001', 2);  -- 5 places, Essence
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (5, 'VOI0002', 2);  -- 5 places, Essence
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (7, 'VOI0003', 1);  -- 7 places, Diesel
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (3, 'VOI0004', 1);  -- 3 places, Diesel

-- Distances depuis l'aeroport (TNR, id=1) vers les hotels
-- Unidirectionnel : pas besoin de to -> from
INSERT INTO Distance (idDepart, idArrive, kilometer) VALUES (1, 2, 20);   -- TNR -> ANT: 20 km
INSERT INTO Distance (idDepart, idArrive, kilometer) VALUES (1, 3, 15);   -- TNR -> AMB: 15 km
INSERT INTO Distance (idDepart, idArrive, kilometer) VALUES (1, 4, 18);   -- TNR -> ANK: 18 km
INSERT INTO Distance (idDepart, idArrive, kilometer) VALUES (1, 5, 5);    -- TNR -> IVT: 5 km
