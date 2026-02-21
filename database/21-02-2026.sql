
CREATE TABLE Carburant(
   idCarburant VARCHAR(50) ,
   libelle VARCHAR(50) ,
   PRIMARY KEY(idCarburant)
);

CREATE TABLE Voiture(
   idVoiture SERIAL,
   Capacite INTEGER,
   ref_ VARCHAR(50) ,
   idCarburant VARCHAR(50)  NOT NULL,
   PRIMARY KEY(idVoiture),
   FOREIGN KEY(idCarburant) REFERENCES Carburant(idCarburant)
);
