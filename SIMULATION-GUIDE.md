# Guide de Simulation d'Assignation

## Vue d'ensemble

Le système de simulation d'assignation permet de planifier l'attribution des voitures aux réservations de manière optimale, en traitant les réservations par "vagues" (groupes de réservations avec la même heure).

## Fonctionnalités

### 1. Simulation (Sans Enregistrement)
- Visualisation des assignations avant de les confirmer
- Aucune modification en base de données
- Permet de tester différents scénarios

### 2. Traitement par Vagues
- **Vague** = Ensemble de réservations avec la même date/heure
- Chaque vague est traitée indépendamment
- Les voitures utilisées dans une vague ne sont plus disponibles pour les vagues suivantes

### 3. Algorithme d'Assignation

#### Priorités de traitement:
1. **Tri par nombre de passagers** (décroissant)
   - Les réservations avec le plus de passagers sont traitées en premier

2. **Remplissage optimal**
   - Priorité aux voitures déjà assignées avec places disponibles
   - Minimise le nombre de voitures utilisées

3. **Sélection de voiture**
   - Capacité >= nombre de passagers
   - Minimise l'écart |capacité - passagers|
   - Priorité aux véhicules Diesel
   - Choix aléatoire si plusieurs options équivalentes

## Utilisation

### Étape 1: Accéder à la Simulation
```
URL: http://localhost:8084/assignations/simuler
```

### Étape 2: Sélectionner une Date
- Saisir la date au format: YYYY-MM-DD
- Exemple: 2026-03-10

### Étape 3: Lancer la Simulation
- Cliquer sur "🚀 Lancer la Simulation"
- Le système affiche:
  - Nombre de vagues
  - Réservations assignées
  - Voitures utilisées
  - Réservations non assignées (si capacité insuffisante)

### Étape 4: Analyser les Résultats
- Vérifier les assignations par vague
- Voir le détail: voiture, réservations, passagers, places restantes
- Identifier les réservations non assignées

### Étape 5: Confirmer (Optionnel)
- Cliquer sur "✔️ Confirmer et Enregistrer"
- Les assignations sont enregistrées en base de données
- Les réservations sont mises à jour avec l'ID de la voiture

## Données de Test

### Option 1: Script Complet (Recommandé)
Recrée toutes les données (voitures, hotels, réservations) :
```bash
psql -U postgres -d framework_test -f database/test-simulation-data.sql
```

### Option 2: Scénarios Complets
Crée 5 scénarios de test différents :
```bash
psql -U postgres -d framework_test -f database/test-scenarios-complets.sql
```

### Option 3: Ajouter des Réservations
Ajoute des réservations aux données existantes (sans supprimer) :
```bash
psql -U postgres -d framework_test -f database/add-more-reservations.sql
```

### Voitures Disponibles (après test-simulation-data.sql)
- VOI0001: 5 places, Essence
- VOI0002: 7 places, Diesel
- VOI0003: 4 places, Essence
- VOI0004: 5 places, Électrique
- VOI0005: 7 places, Diesel
- VOI0006: 4 places, Essence
- VOI0007: 5 places, Diesel
- VOI0008: 6 places, Diesel

**Capacité totale: 43 places**

### Scénarios disponibles:

#### Scénario 1: 2026-03-10 (Multi-Vagues)
- 3 vagues (08:00, 10:30, 14:00)
- 9 réservations au total
- 33 passagers total
- Test du traitement multi-vagues
- **Vague 1 (08:00)**: Alice(5p), Bob(3p), Charlie(2p) = 10 passagers
- **Vague 2 (10:30)**: David(7p), Emma(4p), Frank(3p), Grace(2p) = 16 passagers
- **Vague 3 (14:00)**: Henry(4p), Iris(3p) = 7 passagers

#### Scénario 2: 2026-03-15 (Remplissage Optimal)
- 2 vagues (09:00, 11:00)
- 3 réservations
- Test du remplissage optimal (2 réservations dans 1 voiture)
- **Vague 1 (09:00)**: Jack(3p) + Kate(2p) = 5 passagers → 1 voiture de 5 places
- **Vague 2 (11:00)**: Leo(4p)

#### Scénario 3: 2026-03-20 (Capacité Insuffisante)
- 2 vagues (16:00, 18:00)
- 10 réservations
- Test avec capacité insuffisante (certaines réservations non assignées)
- **Vague 1 (16:00)**: 8 réservations, 37 passagers
- **Vague 2 (18:00)**: 2 réservations, 4 passagers (probablement non assignées)

#### Scénario 4: 2026-03-25 (Priorité Diesel)
- 1 vague (10:00)
- 1 réservation de 4 passagers
- Vérifie que le Diesel est prioritaire

#### Scénario 5: 2026-03-30 (Écart Minimal)
- 1 vague (14:00)
- 1 réservation de 3 passagers
- Vérifie la minimisation de l'écart (voiture 4 places plutôt que 5)

## API REST

### Lancer une simulation (JSON)
```
GET /api/assignations/simuler?date=2026-03-10
```

**Réponse:**
```json
{
  "dateSimulation": "2026-03-10",
  "nbVagues": 3,
  "assignations": [...],
  "reservationsNonAssignees": [...],
  "totalReservationsAssignees": 9
}
```

## Structure des Données

### ResultatSimulation
- `dateSimulation`: Date de la simulation
- `nbVagues`: Nombre de vagues traitées
- `assignations`: Liste des SimulationAssignation
- `reservationsNonAssignees`: Réservations sans voiture
- `totalReservationsAssignees`: Nombre total de réservations assignées

### SimulationAssignation
- `voiture`: Objet Voiture assignée
- `reservations`: Liste des Reservation dans cette voiture
- `placesRestantes`: Nombre de places disponibles
- `heureVague`: Timestamp de la vague

## Règles de Gestion

1. **Une réservation = Une voiture**
   - Chaque réservation est assignée à une seule voiture

2. **Capacité respectée**
   - Nombre de passagers ≤ Capacité de la voiture

3. **Voitures réutilisables dans une vague**
   - Si places disponibles, on remplit la voiture

4. **Voitures bloquées entre vagues**
   - Une voiture utilisée dans vague N n'est plus disponible pour vague N+1

5. **Priorité Diesel**
   - À capacité égale, on privilégie les véhicules Diesel

## Exemples de Résultats

### Vague 1: 08:00
| Voiture | Réf | Capacité | Réservations | Passagers | Places Restantes |
|---------|-----|----------|--------------|-----------|------------------|
| #2 | VOI0002 | 7 | #1 (5p), #3 (2p) | 7 | 0 |
| #1 | VOI0001 | 5 | #2 (3p) | 3 | 2 |

### Vague 2: 10:30
| Voiture | Réf | Capacité | Réservations | Passagers | Places Restantes |
|---------|-----|----------|--------------|-----------|------------------|
| #3 | VOI0003 | 4 | #4 (4p) | 4 | 0 |
| #4 | VOI0004 | 5 | #5 (3p), #7 (2p) | 5 | 0 |

## Dépannage

### Aucune réservation trouvée
- Vérifier que des réservations existent pour la date sélectionnée
- Exécuter le script de test: `database/test-simulation-data.sql`

### Réservations non assignées
- Vérifier la capacité des voitures disponibles
- Ajouter plus de voitures dans la base
- Répartir les réservations sur plusieurs vagues

### Erreur de compilation
- Vérifier que tous les imports sont présents
- Recompiler: `mvn clean package`

## Fichiers Concernés

### Backend
- `SimulationAssignationService.java` - Logique de simulation
- `AssignationController.java` - Endpoints web
- `SimulationAssignation.java` - Modèle d'assignation temporaire
- `ResultatSimulation.java` - Résultat de simulation

### Frontend
- `assignations/simulation.jsp` - Interface de simulation
- `assignations/liste.jsp` - Liste des assignations confirmées

### Base de données
- `database/reinit-03-03-2026.sql` - Structure complète
- `database/test-simulation-data.sql` - Données de test

## Support

Pour toute question ou problème, vérifier:
1. Les logs de la console (System.out)
2. Les erreurs de compilation
3. La connexion à la base de données
4. Les données de test

