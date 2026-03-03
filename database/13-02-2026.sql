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