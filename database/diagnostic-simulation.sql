-- ========================================
-- DIAGNOSTIC DE LA SIMULATION
-- ========================================
-- Ce script aide à comprendre pourquoi certaines réservations
-- ne sont pas assignées dans la simulation

-- ========================================
-- 1. ÉTAT ACTUEL DE LA BASE
-- ========================================

SELECT '========================================' as separator;
SELECT '1. VOITURES DISPONIBLES' as section;
SELECT '========================================' as separator;

SELECT 
    v.idVoiture,
    v.ref_,
    v.Capacite,
    c.libelle as carburant
FROM Voiture v
LEFT JOIN Carburant c ON v.idCarburant = c.idCarburant
ORDER BY v.idVoiture;

SELECT 
    '   → Total: ' || COUNT(*) || ' voitures, ' || SUM(Capacite) || ' places' as resume
FROM Voiture;

-- ========================================
-- 2. RÉSERVATIONS PAR DATE
-- ========================================

SELECT '========================================' as separator;
SELECT '2. RÉSERVATIONS PAR DATE' as section;
SELECT '========================================' as separator;

SELECT 
    DATE(date_heure) as date,
    COUNT(*) as nb_reservations,
    SUM(nbPassager) as total_passagers
FROM reservations
GROUP BY DATE(date_heure)
ORDER BY DATE(date_heure);

-- ========================================
-- 3. ANALYSE DÉTAILLÉE: 2026-03-10
-- ========================================

SELECT '========================================' as separator;
SELECT '3. ANALYSE 2026-03-10' as section;
SELECT '========================================' as separator;

SELECT 
    date_heure as vague,
    COUNT(*) as nb_reservations,
    SUM(nbPassager) as total_passagers,
    STRING_AGG(
        id || ':' || id_client || '(' || nbPassager || 'p)', 
        ', ' 
        ORDER BY nbPassager DESC
    ) as detail
FROM reservations
WHERE DATE(date_heure) = '2026-03-10'
GROUP BY date_heure
ORDER BY date_heure;

SELECT '--- Détail par réservation ---' as info;

SELECT 
    id,
    id_client,
    id_hotel,
    nbPassager,
    date_heure,
    CASE 
        WHEN nbPassager <= 4 THEN 'Petite voiture (4 places)'
        WHEN nbPassager <= 5 THEN 'Moyenne voiture (5 places)'
        WHEN nbPassager <= 6 THEN 'Grande voiture (6 places)'
        ELSE 'Très grande voiture (7+ places)'
    END as voiture_necessaire
FROM reservations
WHERE DATE(date_heure) = '2026-03-10'
ORDER BY date_heure, nbPassager DESC;

-- ========================================
-- 4. SIMULATION MANUELLE VAGUE PAR VAGUE
-- ========================================

SELECT '========================================' as separator;
SELECT '4. SIMULATION MANUELLE' as section;
SELECT '========================================' as separator;

-- Vague 1: 08:00
SELECT '--- VAGUE 1: 08:00 ---' as info;
SELECT 
    'Réservations: ' || COUNT(*) || ' | Passagers: ' || SUM(nbPassager) as resume
FROM reservations
WHERE date_heure = '2026-03-10 08:00:00';

SELECT 
    id,
    id_client,
    nbPassager,
    'Priorité ' || ROW_NUMBER() OVER (ORDER BY nbPassager DESC) as priorite
FROM reservations
WHERE date_heure = '2026-03-10 08:00:00'
ORDER BY nbPassager DESC;

-- Vague 2: 10:30
SELECT '--- VAGUE 2: 10:30 ---' as info;
SELECT 
    'Réservations: ' || COUNT(*) || ' | Passagers: ' || SUM(nbPassager) as resume
FROM reservations
WHERE date_heure = '2026-03-10 10:30:00';

SELECT 
    id,
    id_client,
    nbPassager,
    'Priorité ' || ROW_NUMBER() OVER (ORDER BY nbPassager DESC) as priorite
FROM reservations
WHERE date_heure = '2026-03-10 10:30:00'
ORDER BY nbPassager DESC;

-- Vague 3: 14:00
SELECT '--- VAGUE 3: 14:00 ---' as info;
SELECT 
    'Réservations: ' || COUNT(*) || ' | Passagers: ' || SUM(nbPassager) as resume
FROM reservations
WHERE date_heure = '2026-03-10 14:00:00';

SELECT 
    id,
    id_client,
    nbPassager,
    'Priorité ' || ROW_NUMBER() OVER (ORDER BY nbPassager DESC) as priorite
FROM reservations
WHERE date_heure = '2026-03-10 14:00:00'
ORDER BY nbPassager DESC;

-- ========================================
-- 5. CALCUL DE CAPACITÉ PAR VAGUE
-- ========================================

SELECT '========================================' as separator;
SELECT '5. CAPACITÉ NÉCESSAIRE VS DISPONIBLE' as section;
SELECT '========================================' as separator;

WITH capacite_totale AS (
    SELECT SUM(Capacite) as total FROM Voiture
),
vagues AS (
    SELECT 
        date_heure,
        COUNT(*) as nb_res,
        SUM(nbPassager) as passagers_necessaires
    FROM reservations
    WHERE DATE(date_heure) = '2026-03-10'
    GROUP BY date_heure
)
SELECT 
    v.date_heure as vague,
    v.nb_res as reservations,
    v.passagers_necessaires,
    c.total as capacite_disponible,
    CASE 
        WHEN v.passagers_necessaires <= c.total THEN '✓ OK'
        ELSE '✗ INSUFFISANT'
    END as statut,
    c.total - v.passagers_necessaires as places_restantes
FROM vagues v, capacite_totale c
ORDER BY v.date_heure;

-- ========================================
-- 6. ASSIGNATIONS ACTUELLES
-- ========================================

SELECT '========================================' as separator;
SELECT '6. ASSIGNATIONS EXISTANTES' as section;
SELECT '========================================' as separator;

SELECT 
    a.id,
    a.id_voiture,
    v.ref_,
    a.id_reservation,
    r.id_client,
    r.nbPassager,
    a.date_assignation
FROM assignation a
LEFT JOIN Voiture v ON a.id_voiture = v.idVoiture
LEFT JOIN reservations r ON a.id_reservation = r.id
ORDER BY a.date_assignation DESC
LIMIT 20;

SELECT 
    'Total assignations: ' || COUNT(*) as resume
FROM assignation;

-- ========================================
-- 7. RÉSERVATIONS NON ASSIGNÉES
-- ========================================

SELECT '========================================' as separator;
SELECT '7. RÉSERVATIONS NON ASSIGNÉES' as section;
SELECT '========================================' as separator;

SELECT 
    r.id,
    r.id_client,
    r.id_hotel,
    r.nbPassager,
    r.date_heure
FROM reservations r
LEFT JOIN assignation a ON r.id = a.id_reservation
WHERE a.id IS NULL
ORDER BY r.date_heure, r.nbPassager DESC;

SELECT 
    'Total non assignées: ' || COUNT(*) as resume
FROM reservations r
LEFT JOIN assignation a ON r.id = a.id_reservation
WHERE a.id IS NULL;

-- ========================================
-- 8. RECOMMANDATIONS
-- ========================================

SELECT '========================================' as separator;
SELECT '8. RECOMMANDATIONS' as section;
SELECT '========================================' as separator;

WITH stats AS (
    SELECT 
        COUNT(*) as nb_voitures,
        SUM(Capacite) as capacite_totale,
        (SELECT COUNT(*) FROM reservations WHERE DATE(date_heure) = '2026-03-10') as nb_res_0310,
        (SELECT SUM(nbPassager) FROM reservations WHERE DATE(date_heure) = '2026-03-10') as pass_0310
    FROM Voiture
)
SELECT 
    CASE 
        WHEN capacite_totale < pass_0310 THEN 
            '⚠️  PROBLÈME: Capacité insuffisante (' || capacite_totale || ' places pour ' || pass_0310 || ' passagers)'
        WHEN nb_voitures < 4 THEN
            '⚠️  ATTENTION: Peu de voitures (' || nb_voitures || '). Exécuter test-simulation-data.sql'
        ELSE
            '✓ OK: Capacité suffisante (' || capacite_totale || ' places pour ' || pass_0310 || ' passagers)'
    END as diagnostic
FROM stats;

SELECT '========================================' as separator;
SELECT 'FIN DU DIAGNOSTIC' as section;
SELECT '========================================' as separator;

