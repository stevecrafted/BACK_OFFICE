-- Vérification des données actuelles

SELECT '=== VOITURES DISPONIBLES ===' as info;
SELECT idVoiture, Capacite, ref_, idCarburant FROM Voiture ORDER BY idVoiture;

SELECT '=== CARBURANTS ===' as info;
SELECT * FROM Carburant ORDER BY idCarburant;

SELECT '=== HOTELS ===' as info;
SELECT * FROM hotel ORDER BY id;

SELECT '=== RÉSERVATIONS 2026-03-10 ===' as info;
SELECT 
    id,
    id_client,
    id_hotel,
    nbPassager,
    date_heure
FROM reservations
WHERE DATE(date_heure) = '2026-03-10'
ORDER BY date_heure, nbPassager DESC;

SELECT '=== RÉSUMÉ PAR VAGUE ===' as info;
SELECT 
    date_heure as vague,
    COUNT(*) as nb_reservations,
    SUM(nbPassager) as total_passagers
FROM reservations
WHERE DATE(date_heure) = '2026-03-10'
GROUP BY date_heure
ORDER BY date_heure;

SELECT '=== CAPACITÉ TOTALE DES VOITURES ===' as info;
SELECT SUM(Capacite) as capacite_totale FROM Voiture;
