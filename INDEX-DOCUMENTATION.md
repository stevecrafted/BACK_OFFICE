# 📚 Index de la Documentation

## 🚨 Problème Actuel

**Vous êtes ici parce que:** Des réservations manquent dans la simulation

**Solution rapide:** Voir `QUICK-FIX.md`

---

## 📖 Documentation Principale

### 1. Guide de Simulation
**Fichier:** `SIMULATION-GUIDE.md`

**Contenu:**
- Vue d'ensemble de la simulation
- Fonctionnalités
- Algorithme d'assignation
- Utilisation pas à pas
- API REST
- Structure des données
- Règles de gestion
- Exemples de résultats

**Quand le lire:** Pour comprendre comment fonctionne la simulation

---

### 2. Solution au Problème de Simulation
**Fichier:** `REPONSE-PROBLEME-SIMULATION.md`

**Contenu:**
- Problème identifié (réservations manquantes)
- Cause probable (manque de voitures)
- Solutions détaillées
- Vérification
- Résultats attendus
- Scripts créés
- Explication technique

**Quand le lire:** Quand des réservations ne s'affichent pas dans la simulation

---

### 3. Solution Rapide
**Fichier:** `QUICK-FIX.md`

**Contenu:**
- Solution en 1 commande
- Vérification rapide
- Résultat attendu

**Quand le lire:** Quand vous voulez résoudre le problème immédiatement

---

## 🗄️ Scripts SQL

### Guide des Scripts
**Fichier:** `database/README-SCRIPTS.md`

**Contenu:**
- Description de tous les scripts SQL
- Quand utiliser chaque script
- Workflow recommandé
- Commandes utiles
- Dépannage

**Scripts disponibles:**
1. `reinit-03-03-2026.sql` - Réinitialisation complète
2. `test-simulation-data.sql` - Données de test (8 voitures, 12 réservations)
3. `test-scenarios-complets.sql` - 5 scénarios de test (24 réservations)
4. `add-more-reservations.sql` - Ajouter des réservations
5. `check-data.sql` - Vérifier l'état de la base
6. `diagnostic-simulation.sql` - Diagnostic complet

---

## 🎯 Workflows Courants

### Workflow 1: Première Installation
```bash
# 1. Créer la structure
psql -U postgres -d framework_test -f database/reinit-03-03-2026.sql

# 2. Ajouter les données de test
psql -U postgres -d framework_test -f database/test-simulation-data.sql

# 3. Tester
http://localhost:8084/assignations/simuler
```

**Documentation:** `SIMULATION-GUIDE.md` section "Utilisation"

---

### Workflow 2: Résoudre le Problème de Réservations Manquantes
```bash
# 1. Solution rapide
psql -U postgres -d framework_test -f database/test-simulation-data.sql

# 2. Vérifier
psql -U postgres -d framework_test -f database/check-data.sql

# 3. Tester
http://localhost:8084/assignations/simuler
```

**Documentation:** `QUICK-FIX.md`

---

### Workflow 3: Tester Tous les Scénarios
```bash
# 1. Charger tous les scénarios
psql -U postgres -d framework_test -f database/test-scenarios-complets.sql

# 2. Tester chaque date
# - 2026-03-10: Multi-vagues
# - 2026-03-15: Remplissage optimal
# - 2026-03-20: Capacité insuffisante
# - 2026-03-25: Priorité diesel
# - 2026-03-30: Écart minimal
```

**Documentation:** `database/README-SCRIPTS.md` section "test-scenarios-complets.sql"

---

### Workflow 4: Diagnostic Complet
```bash
# Exécuter le diagnostic
psql -U postgres -d framework_test -f database/diagnostic-simulation.sql
```

**Documentation:** `REPONSE-PROBLEME-SIMULATION.md` section "Vérification"

---

## 🔧 Fichiers Techniques

### Backend (Java)

#### Services
- `src/main/java/org/example/Service/SimulationAssignationService.java`
  - Logique de simulation
  - Algorithme d'assignation par vagues
  - Remplissage optimal

#### Controllers
- `src/main/java/org/example/Controlleur/AssignationController.java`
  - Endpoints de simulation
  - Confirmation des assignations

#### Models
- `src/main/java/org/example/Model/SimulationAssignation.java`
  - Assignation temporaire (non enregistrée)
- `src/main/java/org/example/Model/ResultatSimulation.java`
  - Résultat complet de la simulation

### Frontend (JSP)

- `src/main/webapp/assignations/simulation.jsp`
  - Interface de simulation
  - Affichage des résultats par vague
  - Bouton de confirmation

- `src/main/webapp/assignations/liste.jsp`
  - Liste des assignations confirmées
  - Lien vers la simulation

---

## 📊 Structure des Données

### Tables Principales

```sql
-- Voitures
Voiture (idVoiture, Capacite, ref_, idCarburant)

-- Réservations
reservations (id, id_hotel, id_client, nbPassager, date_heure, idVoiture)

-- Assignations (confirmées)
assignation (id, id_voiture, id_reservation, date_assignation)

-- Hotels
hotel (id, nom)

-- Carburants
Carburant (idCarburant, libelle)
```

**Documentation:** `SIMULATION-GUIDE.md` section "Structure des Données"

---

## 🆘 Dépannage

### Problème: Aucune réservation trouvée
**Solution:** Exécuter `database/test-simulation-data.sql`
**Documentation:** `QUICK-FIX.md`

### Problème: Réservations non assignées
**Solution:** Ajouter plus de voitures ou exécuter `database/test-simulation-data.sql`
**Documentation:** `REPONSE-PROBLEME-SIMULATION.md`

### Problème: Erreur de compilation
**Solution:** Vérifier les imports et recompiler avec `mvn clean package`
**Documentation:** `SIMULATION-GUIDE.md` section "Dépannage"

### Problème: Comprendre pourquoi certaines réservations ne sont pas assignées
**Solution:** Exécuter `database/diagnostic-simulation.sql`
**Documentation:** `REPONSE-PROBLEME-SIMULATION.md` section "Explication Technique"

---

## 🎓 Concepts Clés

### Vague
Ensemble de réservations avec la même date/heure. Chaque vague est traitée indépendamment.

### Remplissage Optimal
Priorité donnée aux voitures déjà assignées avec places disponibles pour minimiser le nombre de voitures utilisées.

### Priorité Diesel
À capacité égale, les véhicules Diesel sont prioritaires.

### Écart Minimal
Choix de la voiture dont la capacité est la plus proche du nombre de passagers.

**Documentation:** `SIMULATION-GUIDE.md` section "Algorithme d'Assignation"

---

## 📞 Aide Rapide

| Besoin | Fichier | Commande |
|--------|---------|----------|
| Résoudre le problème maintenant | `QUICK-FIX.md` | `psql ... test-simulation-data.sql` |
| Comprendre la simulation | `SIMULATION-GUIDE.md` | - |
| Comprendre le problème | `REPONSE-PROBLEME-SIMULATION.md` | - |
| Guide des scripts SQL | `database/README-SCRIPTS.md` | - |
| Vérifier les données | - | `psql ... check-data.sql` |
| Diagnostic complet | - | `psql ... diagnostic-simulation.sql` |
| Tous les scénarios de test | - | `psql ... test-scenarios-complets.sql` |

---

## 📝 Notes

- Tous les scripts SQL sont dans le dossier `database/`
- La simulation n'enregistre rien en base (jusqu'à confirmation)
- Une voiture utilisée dans une vague ne peut plus être utilisée dans les vagues suivantes
- L'algorithme privilégie le remplissage optimal des voitures

---

**Dernière mise à jour:** 03-03-2026

