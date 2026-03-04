-- Modification de la table hotel
-- Supprimer l'ancienne table si elle existe
DROP TABLE IF EXISTS hotel CASCADE;

-- Créer la nouvelle table hotel avec la structure souhaitée
CREATE TABLE hotel(
    id SERIAL,
    code VARCHAR(50) NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    PRIMARY KEY(id)
);

-- Insérer des données de test
INSERT INTO hotel (code, libelle) VALUES ('TNR', 'Ivato Aeroport');
INSERT INTO hotel (code, libelle) VALUES ('MJN', 'Mahajanga Centre');
INSERT INTO hotel (code, libelle) VALUES ('TLE', 'Toliara Beach');

-- ========================================
-- Table Distance
-- ========================================
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

-- Insérer des distances entre les hotels
-- TNR (1) <-> MJN (2): 550 km
INSERT INTO distance (id_from, id_to, kilometer) VALUES (1, 2, 550.00);

-- TNR (1) <-> TLE (3): 900 km
INSERT INTO distance (id_from, id_to, kilometer) VALUES (1, 3, 900.00);

-- MJN (2) <-> TLE (3): 650 km
INSERT INTO distance (id_from, id_to, kilometer) VALUES (2, 3, 650.00);

-- Note: Pas besoin d'insérer (2,1), (3,1), (3,2) car la distance est la même dans les deux sens
-- Pour récupérer la distance entre deux hotels A et B, utilisez:
-- SELECT kilometer FROM distance 
-- WHERE (id_from = A AND id_to = B) OR (id_from = B AND id_to = A);

