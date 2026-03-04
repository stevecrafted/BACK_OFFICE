
CREATE TABLE Carburant(
   idCarburant SERIAL,
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