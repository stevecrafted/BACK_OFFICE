Parametre .conf 
    Vitesse Moyenne de toute les voitures (VM)
Assigantion de reservations pour des voiture automatiquement
    -Apres reservation
    -



CREATE TABLE Carburant(
   idCarburant ,
   libelle VARCHAR(50) ,
   PRIMARY KEY(idCarburant)
);
insert into Carburant (libelle) values ('Diesel');

CREATE TABLE Voiture(
   idVoiture SERIAL,
   Capacite INTEGER,
   ref_ VARCHAR(50) ,
   idCarburant INTEGER NOT NULL,
   PRIMARY KEY(idVoiture),
   FOREIGN KEY(idCarburant) REFERENCES Carburant(idCarburant)
);
CREATE TABLE
    parametre (
        id SERIAL PRIMARY KEY,
        nom VARCHAR(100) UNIQUE NOT NULL,
        valeur VARCHAR(250)
    );

CREATE TABLE
    token_validite (
        id SERIAL PRIMARY KEY,
        token VARCHAR(250) UNIQUE NOT NULL,
        date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        date_expiration TIMESTAMP NOT NULL
    );


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