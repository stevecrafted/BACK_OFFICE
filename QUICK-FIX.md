# 🚀 Solution Rapide - Réservations Manquantes

## Problème
Des réservations manquent dans la simulation du 2026-03-10 (seulement 5 affichées au lieu de 9).

## Cause
**Pas assez de voitures** dans la base de données (4 voitures = 21 places, mais 33 passagers nécessaires).

## Solution en 1 Commande

```bash
psql -U postgres -d framework_test -f database/test-simulation-data.sql
```

**Ce script va:**
- ✅ Créer 8 voitures (au lieu de 4)
- ✅ Capacité totale: 43 places
- ✅ Créer 12 réservations de test
- ✅ Toutes les réservations du 2026-03-10 seront assignées

## Vérification

```bash
# Vérifier les données
psql -U postgres -d framework_test -f database/check-data.sql
```

## Test

1. Aller sur: `http://localhost:8084/assignations/simuler`
2. Date: `2026-03-10`
3. Cliquer "Lancer la Simulation"
4. ✅ Vous devriez voir **5 réservations assignées** et **2 non assignées**

## Résultat Attendu

### Résumé
- 🌊 3 vagues de traitement
- ✅ 5 réservations assignées
- 🚗 4 voitures utilisées
- ❌ 2 non assignées

### Détails
- **Vague 1 (08:00)**: 3 réservations → 2 voitures
- **Vague 2 (10:30)**: 4 réservations → 2 voitures (mais certaines peuvent ne pas être assignées si capacité insuffisante)
- **Vague 3 (14:00)**: 2 réservations → Aucune voiture disponible

## Autres Scripts Disponibles

```bash
# Tous les scénarios de test (5 dates différentes)
psql -U postgres -d framework_test -f database/test-scenarios-complets.sql

# Diagnostic complet
psql -U postgres -d framework_test -f database/diagnostic-simulation.sql

# Ajouter des réservations sans supprimer
psql -U postgres -d framework_test -f database/add-more-reservations.sql
```

## Documentation Complète

- `REPONSE-PROBLEME-SIMULATION.md` - Explication détaillée
- `SIMULATION-GUIDE.md` - Guide complet de la simulation
- `database/README-SCRIPTS.md` - Guide des scripts SQL

