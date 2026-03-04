-- Vérification rapide
SELECT 'VOITURES:' as info, COUNT(*) as nombre, SUM(Capacite) as capacite_totale FROM Voiture;
SELECT 'RESERVATIONS 2026-03-10:' as info, COUNT(*) as nombre, SUM(nbPassager) as total_passagers 
FROM reservations WHERE DATE(date_heure) = '2026-03-10';

SELECT '=== VOITURES DÉTAIL ===' as separator;
SELECT idVoiture, ref_, Capacite FROM Voiture ORDER BY idVoiture;

SELECT '=== RÉSERVATIONS 2026-03-10 ===' as separator;
SELECT id, id_client, nbPassager, date_heure 
FROM reservations 
WHERE DATE(date_heure) = '2026-03-10'
ORDER BY date_heure, nbPassager DESC;
