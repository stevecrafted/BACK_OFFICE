-- -- Créer la base de données
-- CREATE DATABASE framework_test;
-- -- Se connecter à la base
-- \c framework_test;
-- Créer la table utilisateurs

    
CREATE TABLE
    hotel (
        id SERIAL PRIMARY KEY,
        nom VARCHAR(250)
    );

CREATE TABLE
    reservations (
        id SERIAL PRIMARY KEY,
        id_hotel INT NOT NULL,
        id_client VARCHAR(250),
        nbPassager INT NOT NULL,
        date_heure TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (id_hotel) REFERENCES hotel (id)
    );

INSERT INTO hotel (nom) VALUES ('Hotel California');
INSERT INTO hotel (nom) VALUES ('Hotel Transylvania');