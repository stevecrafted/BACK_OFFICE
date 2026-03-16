# Guide de Test Sprint 6

## Nouvelles Fonctionnalités

### 1. Priorité au nombre de trajets (minimum) - PAR JOUR
Les voitures avec le moins de trajets effectués **LE JOUR MÊME** sont prioritaires.
**Important:** Le compteur de trajets se réinitialise à 0 chaque jour.

### 2. Réutilisation des voitures entre vagues
Les voitures peuvent être utilisées dans plusieurs vagues si elles sont disponibles (pas en trajet).

## Installation

### 1. Initialiser la base de données

```bash
psql -U postgres -d framework_test -f database/reinit-10-03-2026-srint4.sql
```

### 2. Charger les données de test

```bash
psql -U postgres -d framework_test -f database/test-sprint6-data.sql
```

### 3. Compiler et déployer

```bash
mvn clean package
.\deploy.bat
```

## Scénarios de Test

### Scénario 1: Priorité au nombre de trajets (2026-03-16)

**Trajets du jour (2026-03-16 matin, déjà effectués):**
- VOI0001 (5 places, Essence): 2 trajets aujourd'hui
- VOI0002 (7 places, Diesel): 0 trajet aujourd'hui ⭐
- VOI0003 (4 places, Essence): 1 trajet aujourd'hui
- VOI0004 (5 places, Hybride): 0 trajet aujourd'hui ⭐

**Note:** VOI0002 et VOI0004 avaient des trajets HIER (2026-03-15), mais ça ne compte pas car le compteur se réinitialise chaque jour.

**Vague 1 (08:00):**

| Réservation | Passagers | Voitures candidates | Résultat attendu |
|-------------|-----------|---------------------|------------------|
| Alice | 5 | VOI0001(5,E,2t) vs VOI0004(5,H,0t) | VOI0004 (0 trajet + H>E) |
| Bob | 4 | VOI0003(4,E,1t) | VOI0003 (seule candidate) |
| Charlie | 3 | VOI0001(5,E,2t) vs VOI0002(7,D,0t) | VOI0002 (0 trajet + Diesel) OU VOI0001 selon écart |

**Vague 2 (10:30):**
- David (7p) → VOI0002 (1 trajet, seule 7 places)
- Emma, Frank, Grace → Voitures disponibles après vague 1

**Vague 3 (14:00):**
- Henry, Iris → Voitures disponibles après vague 2

**Test:**
```
URL: http://localhost:8084/assignations/simuler
Date: 2026-03-16
```

**Vérification:**
- Alice doit être assignée à VOI0004 (pas VOI0001)
- Les logs doivent afficher: "Trajets min: 0"

---

### Scénario 2: Regroupement par vague (2026-03-17)

**Objectif:** Vérifier le regroupement avec temps_attente = 30 minutes

**Réservations:**
- 09:00:00 - Jack (3p)
- 09:15:00 - Kate (2p) → +15 min
- 09:29:00 - Leo (4p) → +29 min
- 09:31:00 - Mike (3p) → +31 min (nouvelle vague)

**Résultat attendu:**
- **Vague 1:** Jack, Kate, Leo (fenêtre 09:00 - 09:30)
- **Vague 2:** Mike (après 09:30)

**Test:**
```
URL: http://localhost:8084/assignations/simuler
Date: 2026-03-17
```

**Vérification:**
- 2 vagues affichées
- Vague 1: 3 réservations
- Vague 2: 1 réservation

---

### Scénario 3: Priorité carburant (2026-03-18)

**Objectif:** Vérifier D > H > E

**Réservations:**
- 10:00 - Nina (5p) → VOI0001(E) vs VOI0004(H)
- 11:00 - Oscar (7p) → VOI0002(D) seule

**Résultat attendu:**
- Nina → VOI0004 (H > E)
- Oscar → VOI0002 (Diesel)

**Test:**
```
URL: http://localhost:8084/assignations/simuler
Date: 2026-03-18
```

**Vérification:**
- Nina assignée à VOI0004
- Oscar assigné à VOI0002

---

### Scénario 4: Capacité insuffisante (2026-03-19)

**Objectif:** Vérifier la gestion des réservations non assignables

**Réservations:**
- 08:00 - Paula (10p) → Aucune voiture (max = 7)

**Résultat attendu:**
- Paula dans "Réservations non assignées"

**Test:**
```
URL: http://localhost:8084/assignations/simuler
Date: 2026-03-19
```

**Vérification:**
- 0 réservation assignée
- 1 réservation non assignée (Paula)

---

### Scénario 5: Remplissage optimal (2026-03-20)

**Objectif:** Vérifier le remplissage des voitures

**Réservations (même vague 09:00):**
- Quinn (3p)
- Rita (2p)
- Sam (1p)

**Résultat attendu:**
- Quinn → Nouvelle voiture (ex: VOI0004, 5 places)
- Rita → Remplissage VOI0004 (3+2=5, pleine)
- Sam → Nouvelle voiture (ex: VOI0003, 4 places)

**Test:**
```
URL: http://localhost:8084/assignations/simuler
Date: 2026-03-20
```

**Vérification:**
- 2 voitures utilisées (pas 3)
- Remplissage optimal affiché dans les logs

---

## Ordre de Priorité Complet (Sprint 6)

1. ✅ Remplissage des voitures déjà assignées dans la vague
2. ✅ Écart minimal |capacité - passagers|
3. ✅ **Nombre de trajets minimum** (nouveau)
4. ✅ Priorité carburant: D > H > E
5. ✅ Choix aléatoire si égalité

## Vérification des Trajets (Par Jour)

Pour voir le nombre de trajets par voiture POUR UNE DATE DONNÉE :

```sql
SELECT 
    v.idVoiture,
    v.ref_,
    v.carburant,
    COUNT(CASE WHEN DATE(r.date_heure) = '2026-03-16' THEN 1 END) as trajets_aujourdhui,
    COUNT(a.id) as trajets_total
FROM Voiture v
LEFT JOIN assignation a ON v.idVoiture = a.id_voiture
LEFT JOIN reservations r ON a.id_reservation = r.id
GROUP BY v.idVoiture, v.ref_, v.carburant
ORDER BY trajets_aujourdhui, v.idVoiture;
```

**Important:** Seul `trajets_aujourdhui` est utilisé pour la priorité, pas `trajets_total`.

## Logs à Surveiller

Dans la console Java, vous devriez voir :

```
📌 Réservation #X - Y passagers - Lieu #Z
   → Écart minimal: N | Trajets min: M | Candidats: K
   → Voiture choisie: #ID (REF) | Carburant: C | Trajets: M
   ✅ Assignée à Voiture #ID (REF) - P places restantes
```

## Confirmation des Assignations

Après la simulation, cliquez sur "✔️ Confirmer et Enregistrer" pour :
- Enregistrer les assignations en base
- Incrémenter le compteur de trajets
- Mettre à jour les réservations

## Dépannage

### Problème: Les trajets ne sont pas comptés

**Solution:** Vérifier que la méthode `countTrajetsParVoiture()` est bien appelée :

```java
int nbTrajets = assignationDAO.countTrajetsParVoiture(v.getIdVoiture());
```

### Problème: Mauvaise voiture choisie

**Vérifier:**
1. Le nombre de trajets de chaque voiture
2. L'écart de capacité
3. Le type de carburant
4. Les logs de la console

### Problème: Voitures non réutilisées entre vagues

**Cause:** Les intervalles de trajet se chevauchent

**Solution:** Vérifier les heures de départ/arrivée dans les logs

## Résumé des Changements Sprint 6

### Code modifié:
- `SimulationAssignationService.java` : Ajout priorité trajets
- `AssignationDAO.java` : Méthode `countTrajetsParVoiture()`

### Base de données:
- Table `lieu` remplace `hotel`
- Colonne `carburant` dans `Voiture` (E/H/D)
- Paramètre `temps_attente` pour les vagues

### Fonctionnalités:
- ✅ Priorité au nombre de trajets
- ✅ Réutilisation des voitures entre vagues
- ✅ Gestion des intervalles occupés
- ✅ Calcul des itinéraires

