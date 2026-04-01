@echo off
echo ========================================
echo TEST: Split Optimization
echo ========================================
echo.
echo Scenario:
echo - R1 = 5 passagers a 08:00
echo - R2 = 11 passagers a 08:00
echo - V1 = 4 places
echo - V2 = 10 places
echo.
echo Resultat attendu:
echo - V2 prend R2 (10/11p) - Split
echo - V1 prend R1 (4/5p) - Split
echo - R2 (1p) et R1 (1p) en attente
echo.

set PGPASSWORD=postgres
psql -U postgres -d gde -h localhost -p 5432 -f database/test-split-optimization.sql

echo.
echo ========================================
echo Donnees chargees!
echo Utilisez la date: 2026-03-17
echo ========================================

pause
