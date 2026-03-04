-- ========================================
-- AJOUTER 4 VOITURES SUPPLÉMENTAIRES
-- ========================================
-- Ce script ajoute 4 voitures SANS supprimer les données existantes

-- Vérifier combien de voitures existent déjà
SELECT 'Voitures actuelles:' as info, COUNT(*) as nombre FROM Voiture;

-- Ajouter 4 nouvelles voitures
INSERT INTO Voiture (Capacite, ref_, idCarburant) 
VALUES (7, 'VOI0005', 2);  -- Diesel, 7 places

INSERT INTO Voiture (Capacite, ref_, idCarburant) 
VALUES (4, 'VOI0006', 1);  -- Essence, 4 places

INSERT INTO Voiture (Capacite, ref_, idCarburant) 
VALUES (5, 'VOI0007', 2);  -- Diesel, 5 places

INSERT INTO Voiture (Capacite, ref_, idCarburant) 
VALUES (6, 'VOI0008', 2);  -- Diesel, 6 places

-- Vérification
SELECT 'Voitures après ajout:' as info, COUNT(*) as nombre, SUM(Capacite) as capacite_totale FROM Voiture;

SELECT '=== LISTE DES VOITURES ===' as separator;
SELECT idVoiture, ref_, Capacite, idCarburant FROM Voiture ORDER BY idVoiture;

SELECT '✅ 4 voitures ajoutées avec succès!' as resultat;
SELECT 'Relancez la simulation sur http://localhost:8084/assignations/simuler' as action;
