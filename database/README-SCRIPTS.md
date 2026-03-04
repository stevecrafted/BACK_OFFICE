# Scripts SQL - Guide d'Utilisation

## Scripts Disponibles

### 1. `reinit-03-03-2026.sql` - Réinitialisation Complète
**Usage:** Réinitialiser toute la base de données
```bash
psql -U postgres -d framework_test -f database/reinit-03-03-2026.sql
```

**Contenu:**
- Supprime toutes les tables
- Recrée la structure complète
- Insère les données de base (4 voitures, 5 hotels, 3 réservations)
- Insère les paramètres système

**Quand l'utiliser:**
- Première installation
- Reset complet de la base
- Retour à l'état initial

---

### 2. `test-simulation-data.sql` - Données de Test pour Simulation
**Usage:** Créer des données de test complètes
```bash
psql -U postgres -d framework_test -f database/test-simulation-data.sql
```

**Contenu:**
- Supprime les anciennes données de test
- Recrée carburants, voitures (8 voitures), hotels
- Insère 12 réservations pour tester la simulation
- 2 scénarios principaux: 2026-03-10 et 2026-03-15

**Quand l'utiliser:**
- Tester la simulation avec des données propres
- Avoir 8 voitures disponibles (capacité totale: 43 places)

---

### 3. `test-scenarios-complets.sql` - Tous les Scénarios de Test
**Usage:** Créer 5 scénarios de test différents
```bash
psql -U postgres -d framework_test -f database/test-scenarios-complets.sql
```

**Contenu:**
- Supprime uniquement assignations et réservations
- Garde les voitures et hotels existants
- Crée 5 scénarios de test:
  1. **2026-03-10**: Multi-vagues (3 vagues, 9 réservations)
  2. **2026-03-15**: Remplissage optimal (2 vagues, 3 réservations)
  3. **2026-03-20**: Capacité insuffisante (2 vagues, 10 réservations)
  4. **2026-03-25**: Priorité diesel (1 vague, 1 réservation)
  5. **2026-03-30**: Écart minimal (1 vague, 1 réservation)

**Quand l'utiliser:**
- Tester tous les cas d'usage de la simulation
- Valider l'algorithme d'assignation
- Tests de régression

---

### 4. `add-more-reservations.sql` - Ajouter des Réservations
**Usage:** Ajouter des réservations sans supprimer les données existantes
```bash
psql -U postgres -d framework_test -f database/add-more-reservations.sql
```

**Contenu:**
- N'efface RIEN
- Ajoute 16 nouvelles réservations
- 3 dates: 2026-03-10, 2026-03-15, 2026-03-20

**Quand l'utiliser:**
- Ajouter des données de test aux données existantes
- Compléter les réservations actuelles
- Ne pas perdre les données déjà créées

---

### 5. `check-data.sql` - Vérification des Données
**Usage:** Vérifier l'état actuel de la base
```bash
psql -U postgres -d framework_test -f database/check-data.sql
```

**Contenu:**
- Affiche les voitures disponibles
- Affiche les carburants
- Affiche les hotels
- Affiche les réservations du 2026-03-10
- Calcule la capacité totale

**Quand l'utiliser:**
- Vérifier les données avant un test
- Diagnostiquer un problème
- Voir l'état actuel de la base

---

## Workflow Recommandé

### Pour Démarrer (Première Fois)
```bash
# 1. Créer la structure complète
psql -U postgres -d framework_test -f database/reinit-03-03-2026.sql

# 2. Ajouter les données de test pour simulation
psql -U postgres -d framework_test -f database/test-simulation-data.sql

# 3. Vérifier
psql -U postgres -d framework_test -f database/check-data.sql
```

### Pour Tester la Simulation
```bash
# Option A: Données simples (2 scénarios)
psql -U postgres -d framework_test -f database/test-simulation-data.sql

# Option B: Tous les scénarios (5 scénarios)
psql -U postgres -d framework_test -f database/test-scenarios-complets.sql
```

### Pour Ajouter des Données
```bash
# Ajouter sans supprimer
psql -U postgres -d framework_test -f database/add-more-reservations.sql
```

### Pour Vérifier
```bash
# Voir l'état actuel
psql -U postgres -d framework_test -f database/check-data.sql
```

### Pour Reset Complet
```bash
# Tout supprimer et recréer
psql -U postgres -d framework_test -f database/reinit-03-03-2026.sql
```

---

## Résumé des Données

### Après `reinit-03-03-2026.sql`
- 4 carburants
- 4 voitures (capacité: 21 places)
- 5 hotels
- 10 distances
- 3 réservations
- 4 paramètres

### Après `test-simulation-data.sql`
- 4 carburants
- 8 voitures (capacité: 43 places)
- 4 hotels
- 0 distances
- 12 réservations (2 dates)

### Après `test-scenarios-complets.sql`
- Garde les voitures et hotels existants
- 0 assignations
- 24 réservations (5 dates différentes)

---

## Commandes Utiles

### Se connecter à la base
```bash
psql -U postgres -d framework_test
```

### Voir toutes les réservations
```sql
SELECT * FROM reservations ORDER BY date_heure;
```

### Voir les réservations d'une date
```sql
SELECT * FROM reservations WHERE DATE(date_heure) = '2026-03-10';
```

### Compter les réservations par date
```sql
SELECT DATE(date_heure), COUNT(*), SUM(nbPassager) 
FROM reservations 
GROUP BY DATE(date_heure) 
ORDER BY DATE(date_heure);
```

### Voir les voitures disponibles
```sql
SELECT v.*, c.libelle as carburant 
FROM Voiture v 
JOIN Carburant c ON v.idCarburant = c.idCarburant 
ORDER BY v.idVoiture;
```

### Supprimer toutes les réservations
```sql
DELETE FROM assignation;
DELETE FROM reservations;
```

---

## Dépannage

### Problème: "Aucune réservation trouvée"
**Solution:** Exécuter `test-simulation-data.sql` ou `add-more-reservations.sql`

### Problème: "Réservations non assignées"
**Solution:** Pas assez de voitures. Exécuter `test-simulation-data.sql` pour avoir 8 voitures

### Problème: "Table n'existe pas"
**Solution:** Exécuter `reinit-03-03-2026.sql` pour créer la structure

### Problème: "Données en double"
**Solution:** Utiliser `reinit-03-03-2026.sql` pour reset complet

