Parametre .conf 
    Vitesse Moyenne de toute les voitures (VM)
Assigantion de reservations pour des voiture automatiquement
    -Apres reservation
    -On prend la Date   de la reservation 
    -l'ordre de traitement des reservations doit etre decroissante par rapport au nbPassager des reservations(on priorise les reservations avec le plus de Passagers et ainsi de suite )
Regle de gestion
    -Les clients dans une reservations  ne peuvent etre separer => -Pour une voiture on verifie le nombre de place dispo
                                                                   -OK Si nb place dispo > nbPassager de la reservation a assigner 
    -Si plusieurs Voitures remplie cette condition : -On prend la voiture dont le trajet est plus rapide par rapport aux destinations (hotel) de reservations (si existe) deja assignée au vehicule 
    -Si plusieurs on va prioriser les diesel 
    -Si plusieurs on fait random




CREATE TABLE Carburant(
   idCarburant ,
   libelle VARCHAR(50) ,
   PRIMARY KEY(idCarburant)
);

insert into Carburant (libelle) values ('Diesel');
insert into Carburant (libelle) values ('Essence');
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

DROP TABLE IF EXISTS distance CASCADE;

CREATE TABLE distance(
    id SERIAL,
    id_from INT NOT NULL,
    id_to INT NOT NULL,
    kilometer DECIMAL(10,2) NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY(id_from) REFERENCES hotel(id),
    FOREIGN KEY(id_to) REFERENCES hotel(id),
    -- Contrainte pour éviter les doublons (A->B et B->A)
    -- On s'assure que id_from < id_to
    CHECK (id_from < id_to)
);


CREATE TABLE hotel(
    id SERIAL,
    code VARCHAR(50) NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    PRIMARY KEY(id)
);
test Pull Request


Assignation 
    L'assignation doit se presenter sous forme de simmulation d'abord (pas d'insertion de données juste afficher sur tableau)
    On saisie un date puis : 
    -On regroupe tte les resevation de cette date 
    -On regroupe les traitement par vague c'est adire : 1 vague = essemble de traitement de  reservations dont la date heure de reservation sont egales
    Pour chaque vague :
    -Traitement 1 reservation 1 
    -Chaque traitement 
        -Prioriser le Reservation avec plus passager 
        Deroulement traitement (chercher voiture compatible):
            -Prioriser le remplissage de voiture  (si une voiture deja assigné mais place dispo >0 )
                -Si nbr place dispo voiture >= reservatiuon.nbr Passager donc on assigne la reservation a  la voiture 
                -Si non ou si pas encore de voiture qui possede deja des passager on cherche d'autre voiture par rapport a ces regle de gestion :
                    -nbr place dispo >= nbr passager et prioriser la voiture dans l'ecart |(nbrplaceDispo-nbrpassager)| soit le plus minim que possible
                    -Si tous nbr dispo sont egaux :
                        - On privilegie les diesel 
                            Si pas de diesel ou tout est diesel on fais random
        puis on continue le traitement par la reservation suivant 
    a la fin d'un vague de traitement
        - on ne doit plus considerer les voitures assigné pour cette vague lors de la prochaine vague de traitement 
        


