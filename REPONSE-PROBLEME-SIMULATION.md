# Réponse au Problème de Simulation

## Problème Identifié

Vous avez constaté que pour la date **2026-03-10**, il manque des réservations dans la simulation :
- **Attendu**: 9 réservations (ID 1-9)
- **Affiché**: Seulement 5 réservations assignées
- **Manquant**: Réservations #5 (Emma), #8 (Henry), #9 (Iris) et possiblement d'autres

## Cause Probable

Le problème vient très probablement d'un **manque de voitures disponibles** dans votre base de données.

### Analyse de votre situation actuelle

Avec les données de base (`reinit-03-03-2026.sql`), vous avez :
- **4 voitures** seulement
- **Capacité totale**: 21 places (5+7+4+5)

Pour le 2026-03-10, vous avez :
- **9 réservations** 
- **33 passagers** au total
- **3 vagues** de traitement

### Pourquoi certaines réservations ne sont pas assignées ?

L'algorithme traite les réservations par vagues :

1. **Vague 1 (08:00)**: 10 passagers → Utilise 2 voitures
2. **Vague 2 (10:30)**: 16 passagers → Essaie d'utiliser les voitures restantes
3. **Vague 3 (14:00)**: 7 passagers → **AUCUNE VOITURE DISPONIBLE** (toutes utilisées dans les vagues précédentes)

**Règle importante**: Une voiture utilisée dans une vague N ne peut plus être utilisée dans les vagues suivantes.

## Solutions

### Solution 1: Utiliser le Script de Test Complet (RECOMMANDÉ)

Ce script crée **8 voitures** (capacité totale: 43 places) :

```bash
psql -U postgres -d framework_test -f database/test-simulation-data.sql
```

**Résultat:**
- 8 voitures disponibles
- Toutes les 9 réservations du 2026-03-10 seront assignées
- 2 réservations non assignées pour le 2026-03-20 (test de capacité insuffisante)

### Solution 2: Ajouter Plus de Voitures Manuellement

Si vous voulez garder vos données actuelles, ajoutez des voitures :

```sql
-- Se connecter à la base
psql -U postgres -d framework_test

-- Ajouter 4 voitures supplémentaires
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (7, 'VOI0005', 2);  -- Diesel, 7 places
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (4, 'VOI0006', 1);  -- Essence, 4 places
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (5, 'VOI0007', 2);  -- Diesel, 5 places
INSERT INTO Voiture (Capacite, ref_, idCarburant) VALUES (6, 'VOI0008', 2);  -- Diesel, 6 places
```

### Solution 3: Utiliser les Scénarios Complets

Pour tester tous les cas d'usage :

```bash
psql -U postgres -d framework_test -f database/test-scenarios-complets.sql
```

**Contenu:**
- 5 scénarios de test différents
- 24 réservations au total
- Tests de tous les cas (multi-vagues, remplissage optimal, capacité insuffisante, etc.)

## Vérification

### Étape 1: Vérifier vos données actuelles

```bash
psql -U postgres -d framework_test -f database/check-data.sql
```

ou

```bash
psql -U postgres -d framework_test -f database/diagnostic-simulation.sql
```

### Étape 2: Compter vos voitures

```sql
SELECT COUNT(*), SUM(Capacite) FROM Voiture;
```

**Attendu:**
- Minimum 6 voitures pour le scénario 2026-03-10
- Capacité totale >= 33 places

### Étape 3: Relancer la simulation

1. Accéder à: `http://localhost:8084/assignations/simuler`
2. Saisir la date: `2026-03-10`
3. Cliquer sur "Lancer la Simulation"
4. Vérifier que les 9 réservations sont affichées

## Résultats Attendus (avec 8 voitures)

### Vague 1: 08:00
| Voiture | Réservations | Passagers | Places Restantes |
|---------|--------------|-----------|------------------|
| VOI0004 (5p Hybride) | #1 Alice (5p) | 5 | 0 |
| VOI0003 (4p Essence) | #2 Bob (3p) | 3 | 1 |
| VOI0006 (4p Essence) | #3 Charlie (2p) | 2 | 2 |

### Vague 2: 10:30
| Voiture | Réservations | Passagers | Places Restantes |
|---------|--------------|-----------|------------------|
| VOI0002 (7p Diesel) | #4 David (7p) | 7 | 0 |
| VOI0007 (5p Diesel) | #5 Emma (4p) | 4 | 1 |
| VOI0001 (5p Essence) | #6 Frank (3p) | 3 | 2 |
| VOI0008 (6p Diesel) | #7 Grace (2p) | 2 | 4 |

### Vague 3: 14:00
| Voiture | Réservations | Passagers | Places Restantes |
|---------|--------------|-----------|------------------|
| VOI0005 (7p Diesel) | #8 Henry (4p), #9 Iris (3p) | 7 | 0 |

**Total: 9 réservations assignées, 0 non assignée**

## Scripts Créés pour Vous

J'ai créé plusieurs scripts SQL pour vous aider :

1. **`database/test-simulation-data.sql`** - Données complètes (8 voitures, 12 réservations)
2. **`database/test-scenarios-complets.sql`** - 5 scénarios de test (24 réservations)
3. **`database/add-more-reservations.sql`** - Ajouter des réservations sans supprimer
4. **`database/check-data.sql`** - Vérifier l'état de la base
5. **`database/diagnostic-simulation.sql`** - Diagnostic complet et détaillé
6. **`database/README-SCRIPTS.md`** - Guide d'utilisation des scripts

## Commande Rapide

Pour résoudre immédiatement le problème :

```bash
# 1. Charger les données de test complètes
psql -U postgres -d framework_test -f database/test-simulation-data.sql

# 2. Vérifier
psql -U postgres -d framework_test -f database/check-data.sql

# 3. Tester la simulation
# Aller sur: http://localhost:8084/assignations/simuler
# Date: 2026-03-10
```

## Explication Technique

### Pourquoi les voitures sont "bloquées" entre vagues ?

C'est une règle métier de votre algorithme :

```java
// Dans SimulationAssignationService.java
Set<Integer> voituresUtilisees = new HashSet<>();

// Pour chaque vague
for (Map.Entry<Timestamp, List<Reservation>> entry : vagues.entrySet()) {
    // Voitures disponibles = toutes SAUF celles déjà utilisées
    List<Voiture> voituresDisponibles = toutesVoitures.stream()
            .filter(v -> !voituresUtilisees.contains(v.getIdVoiture()))
            .collect(Collectors.toList());
    
    // ... traitement ...
    
    // Marquer les voitures comme utilisées
    voituresUtilisees.add(assignation.getVoiture().getIdVoiture());
}
```

Cette logique simule le fait qu'une voiture partie en mission ne peut pas faire une autre mission le même jour.

## Support

Si le problème persiste après avoir exécuté `test-simulation-data.sql`, vérifiez :

1. La connexion à la base de données (`beans.xml`)
2. Les logs de la console Java
3. Que la date saisie est bien `2026-03-10` (format YYYY-MM-DD)
4. Qu'il n'y a pas d'erreurs de compilation

Pour un diagnostic complet :
```bash
psql -U postgres -d framework_test -f database/diagnostic-simulation.sql
```

