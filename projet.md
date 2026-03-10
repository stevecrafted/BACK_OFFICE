# Projet BACK_OFFICE - Gestion de Flotte & Réservations

## Description Générale

Application web Java de gestion de flotte de véhicules et de réservations hôtelières. Le système permet d'assigner automatiquement des voitures à des réservations en fonction de règles métier précises (capacité, distance, type de carburant). Le projet utilise un framework MVC custom (spring-init-framework) avec Tomcat embarqué et PostgreSQL.

**Repository :** stevecrafted/BACK_OFFICE  
**Branche courante :** Staging/sprint3/listeVoiture-assignationVoiture  
**Branche par défaut :** main

---

## Stack Technique

| Composant   | Technologie                              |
|-------------|------------------------------------------|
| Langage     | Java 8+                                  |
| Framework   | Spring-init-framework (MVC custom)       |
| Serveur     | Tomcat 9.0.93 embarqué                   |
| Base de données | PostgreSQL 15                        |
| Vues        | JSP                                      |
| API         | JSON (Jackson 2.17)                      |
| Build       | Maven                                    |
| Conteneur   | Docker / Docker Compose                  |
| Packaging   | JAR exécutable (maven-shade-plugin)      |

---

## Structure du Projet

```
TEST/
├── pom.xml                          # Configuration Maven
├── docker-compose.yml               # PostgreSQL + App
├── Dockerfile                       # Build multi-stage
├── deploy.bat                       # Script de déploiement local
├── beans.xml                        # Config DataSource (connexion DB)
├── database/                        # Scripts SQL
│   ├── reinit-03-03-2026.sql        # Script de réinitialisation complète
│   ├── test-simulation-lancerSimulation.sql  # Données de test simulation
│   └── ...
├── src/main/java/org/example/
│   ├── Controlleur/                 # Contrôleurs MVC + API JSON
│   │   ├── AssignationController.java
│   │   ├── CarburantController.java
│   │   ├── HotelController.java
│   │   ├── ReservationController.java
│   │   └── VoitureController.java
│   ├── DAO/                         # Data Access Objects (JDBC + PostgreSQL)
│   │   ├── AssignationDAO.java
│   │   ├── CarburantDAO.java
│   │   ├── DistanceDAO.java
│   │   ├── HotelDAO.java
│   │   ├── ReservationDAO.java
│   │   └── VoitureDAO.java
│   ├── DTO/                         # Data Transfer Objects
│   │   ├── ReservationDTO.java
│   │   └── VoitureDTO.java
│   ├── Model/                       # Entités métier
│   │   ├── Assignation.java
│   │   ├── Carburant.java
│   │   ├── Hotel.java
│   │   ├── Reservation.java
│   │   ├── ResultatSimulation.java
│   │   ├── SimulationAssignation.java
│   │   └── Voiture.java
│   ├── Service/                     # Logique métier
│   │   ├── AssignationService.java
│   │   ├── ReservationService.java
│   │   ├── SimulationAssignationService.java
│   │   └── VoitureService.java
│   ├── token/                       # Gestion JWT / Token
│   └── Util/                        # Utilitaires
│       └── DatabaseConnection.java
├── src/main/webapp/                 # Vues JSP
│   ├── assignations/
│   │   ├── liste.jsp                # Liste des assignations
│   │   └── simulation.jsp           # Formulaire + résultat simulation
│   ├── reservations/
│   │   ├── form.jsp                 # Formulaire réservation
│   │   └── liste.jsp                # Liste des réservations
│   ├── voitures/
│   │   ├── details.jsp              # Détails d'une voiture
│   │   ├── form.jsp                 # Formulaire voiture
│   │   └── liste.jsp                # Liste des voitures
│   └── WEB-INF/
│       ├── web.xml
│       └── spring/servlet-context.xml
└── TODO/                            # Suivi des sprints
    ├── sprint1
    ├── sprint2
    └── sprint3
```

---

## Schéma de la Base de Données

```
┌─────────────┐     ┌─────────────┐     ┌──────────────┐
│  Carburant   │     │   Voiture    │     │ Assignation  │
├─────────────┤     ├─────────────┤     ├──────────────┤
│ idCarburant  │◄────│ idCarburant  │     │ id           │
│ libelle      │     │ idVoiture    │◄────│ id_voiture   │
└─────────────┘     │ Capacite     │     │ id_reservation│──►┐
                    │ ref_         │     │ date_assignation│  │
                    └─────────────┘     └──────────────┘    │
                                                             │
┌─────────────┐     ┌──────────────┐                        │
│    Hotel     │     │ Reservations │◄───────────────────────┘
├─────────────┤     ├──────────────┤
│ id           │◄────│ id_hotel     │
│ nom          │     │ id           │
└──────┬───┬──┘     │ id_client    │
       │   │        │ nbPassager   │
       │   │        │ date_heure   │
       │   │        └──────────────┘
       │   │
  ┌────┘   └────┐
  │  Distance   │    ┌──────────────┐
  ├─────────────┤    │  Parametre   │
  │ id_from     │    ├──────────────┤
  │ id_to       │    │ nom          │
  │ kilometer   │    │ valeur       │
  └─────────────┘    └──────────────┘

  ┌──────────────────┐
  │ token_validite   │
  ├──────────────────┤
  │ token            │
  │ date_creation    │
  │ date_expiration  │
  └──────────────────┘
```

### Tables

| Table           | Description                                      |
|-----------------|--------------------------------------------------|
| Carburant       | Types de carburant (Essence, Diesel, Électrique, Hybride) |
| Voiture         | Flotte de véhicules avec capacité et référence   |
| Hotel           | Hôtels / destinations de service                 |
| Distance        | Distances (km) entre paires d'hôtels             |
| Reservations    | Réservations clients (hôtel, nb passagers, date)  |
| Assignation     | Lien 1:1 entre une voiture et une réservation     |
| Parametre       | Config système (vitesse moyenne, version, etc.)   |
| token_validite  | Tokens d'authentification avec expiration          |

---

## Annotations Custom du Framework

| Annotation             | Description                                |
|------------------------|--------------------------------------------|
| `@AnnotationContoller` | Déclare une classe comme contrôleur MVC    |
| `@GetMapping("/url")`  | Route HTTP GET                             |
| `@PostMapping("/url")` | Route HTTP POST                            |
| `@Json`                | Retourne la réponse au format JSON         |
| `@AnnotationRequestParam("name")` | Injection de paramètre HTTP     |

L'entité `ModelView` sert de conteneur pour la vue (chemin JSP) et les attributs à transmettre.

---

## Endpoints (Routes)

### Pages Web (JSP)

| Méthode | URL                         | Description                        |
|---------|-----------------------------|------------------------------------|
| GET     | /voitures                   | Liste des voitures (+ filtre date) |
| GET     | /voitures/nouveau           | Formulaire création voiture        |
| POST    | /voitures/create            | Créer une voiture                  |
| GET     | /voitures/edit?id=X         | Formulaire modification voiture    |
| POST    | /voitures/update            | Modifier une voiture               |
| GET     | /reservations               | Liste des réservations             |
| GET     | /reservations/nouveau       | Formulaire création réservation    |
| POST    | /reservations/create        | Créer une réservation              |
| POST    | /reservations/delete        | Supprimer une réservation          |
| GET     | /assignations               | Liste des assignations             |
| POST    | /assignations/auto          | Lancer assignation automatique     |
| POST    | /assignations/delete        | Supprimer une assignation          |
| GET     | /assignations/simuler       | Formulaire de simulation           |
| POST    | /assignations/simuler       | **Lancer la simulation**           |
| POST    | /assignations/confirmer     | Confirmer et enregistrer simulation|
| GET     | /hotels                     | Liste des hôtels                   |
| GET     | /carburants                 | Liste des carburants               |
| POST    | /carburants/create          | Créer un carburant                 |
| POST    | /carburants/delete          | Supprimer un carburant             |

### API JSON

| Méthode | URL                                  | Description                     |
|---------|--------------------------------------|---------------------------------|
| GET     | /api/voitures                        | Liste voitures (VoitureDTO)     |
| GET     | /api/voitures/carburant?id=X         | Voitures par type carburant     |
| GET     | /api/voitures/date?dateDepart=X      | Voitures par date départ        |
| GET     | /api/reservations                    | Liste réservations (DTO)        |
| GET     | /api/assignations                    | Liste assignations détaillées   |
| POST    | /api/assignations/auto               | Assignation auto (JSON)         |
| GET     | /api/assignations/simuler?date=X     | Simulation assignation (JSON)   |
| GET     | /api/carburants                      | Liste carburants                |

---

## Logique Métier Principale

### Simulation d'Assignation (`lancerSimulation`)

La simulation est le coeur du Sprint 3. Elle assigne virtuellement des voitures aux réservations d'une date donnée **sans écrire en base de données**.

#### Algorithme

1. **Récupérer** toutes les réservations de la date sélectionnée
2. **Regrouper** par timestamp identique → "vagues" (waves)
3. **Pour chaque vague**, traiter les réservations triées par `nbPassager` décroissant :
   - **Priorité 1** : Remplir une voiture déjà assignée dans cette vague si elle a encore de la place
   - **Priorité 2** : Trouver une nouvelle voiture parmi celles non utilisées :
     a. Filtrer les voitures avec `capacité ≥ nbPassagers`
     b. Calculer l'écart = `|capacité - nbPassagers|`
     c. Choisir la voiture avec l'écart minimum (remplissage optimal)
     d. En cas d'égalité : **priorité Diesel**
     e. Si encore égalité : choix aléatoire
4. Les voitures utilisées dans une vague ne sont **plus disponibles** pour les vagues suivantes
5. **Retour** : `ResultatSimulation` contenant les assignations et les réservations non assignées

#### Confirmation

Après simulation, l'utilisateur peut confirmer pour écrire les assignations en base (`POST /assignations/confirmer`).

### Assignation Automatique (Service direct)

L'`AssignationService.assignerVoituresAutomatiquement()` fonctionne différemment :
- Récupère les réservations non encore assignées
- Prend en compte le temps de trajet (distance / vitesse moyenne)
- Insère directement en base

---

## Configuration

### Connexion Base de Données (beans.xml)

```xml
<bean id="dataSource">
    <property name="url" value="jdbc:postgresql://localhost:5432/framework_test"/>
    <property name="username" value="postgres"/>
    <property name="password" value="steve"/>
</bean>
```

### Docker Compose

```yaml
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: framework_test
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: steve
    ports: ["5432:5432"]
  app:
    ports: ["8080:8080"]
    depends_on: postgres
```

### Paramètres Système (table `parametre`)

| Nom                       | Valeur | Description                  |
|---------------------------|--------|------------------------------|
| VM                        | 60     | Vitesse moyenne (km/h)       |
| app_version               | 1.0.0  | Version de l'application     |
| maintenance_mode          | false  | Mode maintenance             |
| max_reservations_per_day  | 100    | Max réservations/jour        |

---

## Déploiement

### Local (deploy.bat)

```batch
# 1. Installer le framework JAR
mvn install:install-file -Dfile=src/main/webapp/WEB-INF/lib/spring-init-framework.jar ...

# 2. Compiler
mvn clean package

# 3. Lancer
java -jar target/spring-init-test-1.0.0.jar
```

### Docker

```bash
docker-compose up --build
```

---

## Données de Test

Le script `database/reinit-03-03-2026.sql` contient la réinitialisation complète de la base.  
Le script `database/test-simulation-lancerSimulation.sql` contient les scénarios de test pour la simulation.

### Voitures disponibles

| ID | Ref     | Capacité | Carburant   |
|----|---------|----------|-------------|
| 1  | VOI0001 | 5        | Essence     |
| 2  | VOI0002 | 7        | Diesel      |
| 3  | VOI0003 | 4        | Essence     |
| 4  | VOI0004 | 5        | Électrique  |

### Hôtels

| ID | Nom     |
|----|---------|
| 1  | Colbert |
| 2  | Novotel |
| 3  | Ibis    |
| 4  | Lokanga |
| 5  | test    |

### Scénarios de test simulation

| Date       | Nb réservations | Vagues | Cas testé                                    |
|------------|-----------------|--------|----------------------------------------------|
| 2026-03-12 | 6               | 3      | Multi-vagues, remplissage optimal            |
| 2026-03-15 | 5               | 1      | Surcharge (24 passagers > 21 places)         |
| 2026-03-18 | 6               | 2      | Petites réservations, remplissage optimal    |
| 2026-03-20 | 2               | 1      | Passagers > capacité max (8 places)          |
| 2026-03-22 | 1               | 1      | Cas simple (1 seule réservation)             |
| 2026-03-25 | 3               | 1      | Priorité Diesel, écart minimal               |
| 2026-03-30 | 0               | 0      | Aucune réservation (cas vide)                |

---

## Équipe

| Sprint | Team Lead | Membres                            |
|--------|-----------|------------------------------------|
| 1      | Zou       | Salohy (Front), Steven (API JSON)  |
| 2      | Steven    | Zou (CRUD Voiture), Salohy (Sécurité URL) |
| 3      | -         | Assignation voitures + Simulation  |

---

## Sprints

- **Sprint 1** : Mise en place initiale, affichage, filtres, API JSON
- **Sprint 2** : CRUD voiture, protection des URLs par token
- **Sprint 3** : Module d'assignation automatique et simulation d'assignation par date
