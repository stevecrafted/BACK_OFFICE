package org.example.Service;

import org.example.DAO.*;
import org.example.Model.*;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class SimulationAssignationService {

    private ReservationDAO reservationDAO = new ReservationDAO();
    private VoitureDAO voitureDAO = new VoitureDAO();
    private DistanceDAO distanceDAO = new DistanceDAO();
    private LieuDAO lieuDAO = new LieuDAO();
    private ParametreDAO parametreDAO = new ParametreDAO();
    private AssignationDAO assignationDAO = new AssignationDAO();

    // Stocke les intervalles [debutFenetre, finFenetre] de chaque vague (clé = cleVague)
    private Map<String, Timestamp[]> vagueIntervalles = new LinkedHashMap<>();

    /**
     * Simule l'assignation des voitures pour une date donnée
     * SANS INSERTION EN BASE - Juste simulation
     * Prend en compte les assignations déjà existantes en base
     */
    public ResultatSimulation simulerAssignation(Date dateSimulation) {
        ResultatSimulation resultat = new ResultatSimulation(dateSimulation);
        vagueIntervalles.clear();

        System.out.println("\n========================================");
        System.out.println("🎯 SIMULATION D'ASSIGNATION");
        System.out.println("Date: " + dateSimulation);
        System.out.println("========================================\n");

        // 1. Récupérer toutes les réservations de cette date
        List<Reservation> reservations = reservationDAO.findByDate(dateSimulation);
        
        if (reservations.isEmpty()) {
            System.out.println("❌ Aucune réservation trouvée pour cette date");
            return resultat;
        }

        System.out.println("📋 " + reservations.size() + " réservation(s) trouvée(s)\n");

        // 2. Récupérer les assignations existantes en base et exclure ces réservations
        List<Assignation> assignationsExistantes = assignationDAO.findAll();
        Set<Integer> reservationsDejaAssignees = new HashSet<>();
        // Map: idVoiture -> liste de réservations déjà assignées à cette voiture
        Map<Integer, List<Reservation>> voitureReservationsExistantes = new HashMap<>();

        // Compteur de trajets par voiture pour le jour même (base + simulation)
        Map<Integer, Integer> compteurTrajetsJour = new HashMap<>();

        for (Assignation a : assignationsExistantes) {
            reservationsDejaAssignees.add(a.getIdReservation());
            Reservation r = reservationDAO.findById(a.getIdReservation());
            if (r != null) {
                voitureReservationsExistantes
                    .computeIfAbsent(a.getIdVoiture(), k -> new ArrayList<>())
                    .add(r);
                
                // Compter les trajets du jour même en base
                if (r.getDateHeure() != null) {
                    java.sql.Date dateRes = new java.sql.Date(r.getDateHeure().getTime());
                    if (dateRes.equals(dateSimulation)) {
                        compteurTrajetsJour.put(a.getIdVoiture(), 
                            compteurTrajetsJour.getOrDefault(a.getIdVoiture(), 0) + 1);
                    }
                }
            }
        }

        // Filtrer les réservations : exclure celles déjà assignées
        List<Reservation> reservationsASimuler = new ArrayList<>();
        for (Reservation r : reservations) {
            if (!reservationsDejaAssignees.contains(r.getId())) {
                reservationsASimuler.add(r);
            }
        }

        System.out.println("📌 " + reservationsDejaAssignees.size() + " réservation(s) déjà assignée(s) en base (exclues)");
        System.out.println("📋 " + reservationsASimuler.size() + " réservation(s) à simuler\n");

        // 3. Récupérer toutes les voitures et la vitesse moyenne
        List<Voiture> toutesVoitures = voitureDAO.findAll();
        double vitesseMoyenne = parametreDAO.getVM();
        Lieu aeroport = lieuDAO.findAeroport();

        // 4. Construire les assignations existantes pour affichage et pré-remplir les intervalles occupés
        // Chaque voiture a une liste d'intervalles [departMs, arriveeMs] pendant lesquels elle est occupée
        Map<Integer, List<long[]>> intervallesOccupes = new HashMap<>();

        for (Map.Entry<Integer, List<Reservation>> entry : voitureReservationsExistantes.entrySet()) {
            int idVoiture = entry.getKey();
            List<Reservation> resExistantes = entry.getValue();
            // Filtrer les réservations nulles (au cas où findById retourne null)
            resExistantes.removeIf(r -> r == null);
            if (resExistantes.isEmpty()) continue;

            Voiture voiture = voitureDAO.findById(idVoiture);
            if (voiture == null) continue;

            // Regrouper par vague (même minute)
            Map<String, List<Reservation>> vaguesExistantes = new TreeMap<>();
            for (Reservation r : resExistantes) {
                if (r.getDateHeure() != null) {
                    Timestamp heure = r.getDateHeure();
                    String cleVague = String.format("%tF %tH:%tM", heure, heure, heure);
                    vaguesExistantes.computeIfAbsent(cleVague, k -> new ArrayList<>()).add(r);
                }
            }

            for (List<Reservation> vagueRes : vaguesExistantes.values()) {
                Timestamp heureVague = vagueRes.get(0).getDateHeure();
                SimulationAssignation saExistante = new SimulationAssignation(voiture, heureVague);
                for (Reservation r : vagueRes) {
                    saExistante.ajouterReservation(r);
                }
                calculerHeuresTrajet(saExistante, aeroport, vitesseMoyenne);
                resultat.ajouterAssignationExistante(saExistante);

                // Enregistrer l'intervalle occupé [départ, arrivée]
                intervallesOccupes.computeIfAbsent(idVoiture, k -> new ArrayList<>())
                    .add(new long[]{saExistante.getDateHeureDepart().getTime(), saExistante.getDateHeureArrivee().getTime()});
            }
        }

        System.out.println("🔒 " + intervallesOccupes.size() + " voiture(s) avec des intervalles occupés\n");

        // 5. Regrouper les réservations restantes par vague
        if (reservationsASimuler.isEmpty()) {
            System.out.println("ℹ️ Toutes les réservations sont déjà assignées");
            return resultat;
        }

        Map<String, List<Reservation>> vagues = regrouperParVague(reservationsASimuler);
        resultat.setNbVagues(vagues.size());
        List<String> vaguesTriees = new ArrayList<>(vagues.keySet());
        Collections.sort(vaguesTriees);

        // Liste des réservations non assignées à retraiter dans les vagues suivantes
        List<Reservation> reservationsNonAssignees = new ArrayList<>();

        int numeroVague = 1;
        for (String cleVague : vaguesTriees) {
            List<Reservation> reservationsVague = vagues.get(cleVague);

            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("🌊 VAGUE #" + numeroVague + " - " + cleVague);
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // Ajouter les réservations non assignées des vagues précédentes pour retraitement
            if (!reservationsNonAssignees.isEmpty()) {
                System.out.println("🔄 Retraitement de " + reservationsNonAssignees.size() + 
                                   " réservation(s) non assignée(s) des vagues précédentes");
                reservationsVague.addAll(reservationsNonAssignees);
                reservationsNonAssignees.clear();
            }

            // Trier par nombre de passagers décroissant
            reservationsVague.sort((r1, r2) -> Integer.compare(r2.getNbPassager(), r1.getNbPassager()));

            // Heure de la vague = heure de la dernière réservation (déjà calculée par regrouperParVague)
            // On utilise la clé de la vague qui correspond à la dernière réservation
            Timestamp heureVague = Timestamp.valueOf(cleVague + ":00");

            // Filtrer les voitures disponibles : celles qui ne sont pas en trajet à l'heure de la vague
            long heureVagueMs = heureVague.getTime();
            List<Voiture> voituresDisponibles = new ArrayList<>();
            for (Voiture v : toutesVoitures) {
                List<long[]> intervals = intervallesOccupes.get(v.getIdVoiture());
                boolean disponible = true;
                if (intervals != null) {
                    for (long[] interval : intervals) {
                        // La voiture est occupée si l'heure de la vague tombe dans un intervalle [départ, arrivée[
                        if (heureVagueMs >= interval[0] && heureVagueMs < interval[1]) {
                            disponible = false;
                            System.out.println("   🚫 Voiture #" + v.getIdVoiture() + " (" + v.getRef() + 
                                               ") en trajet de " + new Timestamp(interval[0]) + " à " + new Timestamp(interval[1]));
                            break;
                        }
                    }
                }
                if (disponible) {
                    voituresDisponibles.add(v);
                }
            }

            // Assignations temporaires pour cette vague
            Map<Integer, SimulationAssignation> assignationsVague = new LinkedHashMap<>();

            // Traiter chaque réservation de la vague
            for (Reservation reservation : reservationsVague) {
                System.out.println("\n📌 Réservation #" + reservation.getId() + 
                                   " - " + reservation.getNbPassager() + " passagers - Lieu #" + 
                                   reservation.getIdLieu());

                SimulationAssignation assignation = trouverMeilleureVoiture(
                    reservation, 
                    voituresDisponibles, 
                    assignationsVague,
                    heureVague,
                    compteurTrajetsJour
                );

                if (assignation != null) {
                    System.out.println("   ✅ Assignée à Voiture #" + assignation.getVoiture().getIdVoiture() + 
                                       " (" + assignation.getVoiture().getRef() + ") - " +
                                       assignation.getPlacesRestantes() + " places restantes");
                } else {
                    System.out.println("   ❌ Aucune voiture disponible - sera retraitée dans la vague suivante");
                    reservationsNonAssignees.add(reservation);
                }
            }

            // Incrémenter le compteur de trajets pour chaque voiture utilisée dans cette vague
            for (SimulationAssignation assignation : assignationsVague.values()) {
                int idVoiture = assignation.getVoiture().getIdVoiture();
                compteurTrajetsJour.put(idVoiture, compteurTrajetsJour.getOrDefault(idVoiture, 0) + 1);
            }

            // Récupérer l'intervalle de la vague pour l'affichage
            Timestamp[] intervalleVague = vagueIntervalles.get(cleVague);

            // ÉTAPE 1: Calculer les heures de trajet initiales pour chaque assignation
            for (SimulationAssignation assignation : assignationsVague.values()) {
                if (intervalleVague != null) {
                    assignation.setDebutVague(intervalleVague[0]);
                    assignation.setFinFenetreVague(intervalleVague[1]);
                }
                calculerHeuresTrajet(assignation, aeroport, vitesseMoyenne);
            }

            // ÉTAPE 2: Vérifier si des réassignations sont possibles pendant la fenêtre d'attente
            verifierReassignationPendantAttente(
                assignationsVague,
                intervalleVague,
                toutesVoitures,
                intervallesOccupes,
                compteurTrajetsJour
            );

            // ÉTAPE 2.5: Traiter les réservations non assignées avec les voitures qui reviennent
            // Créer une copie temporaire des intervalles occupés avec les assignations de cette vague
            Map<Integer, List<long[]>> intervallesTempAvecVague = new HashMap<>();
            for (Map.Entry<Integer, List<long[]>> entry : intervallesOccupes.entrySet()) {
                intervallesTempAvecVague.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            for (SimulationAssignation sa : assignationsVague.values()) {
                long depMs = sa.getDateHeureDepart().getTime();
                long arrMs = sa.getDateHeureArrivee().getTime();
                intervallesTempAvecVague.computeIfAbsent(sa.getVoiture().getIdVoiture(), k -> new ArrayList<>())
                    .add(new long[]{depMs, arrMs});
            }

            // Liste pour stocker les assignations différées (trajets de retour)
            List<SimulationAssignation> assignationsDifferees = new ArrayList<>();

            // Tenter d'assigner les réservations non assignées aux voitures qui reviennent
            List<Reservation> reservationsTraitees = traiterReservationsNonAssigneesAvecVoituresRetour(
                reservationsNonAssignees,
                intervalleVague,
                toutesVoitures,
                intervallesTempAvecVague,
                assignationsDifferees,  // Passer la liste des assignations différées
                compteurTrajetsJour,
                aeroport,
                vitesseMoyenne
            );
            reservationsNonAssignees.removeAll(reservationsTraitees);

            // ÉTAPE 3: Recalculer les heures de trajet (si réassignation) et vérifier les chevauchements
            for (SimulationAssignation assignation : assignationsVague.values()) {
                // Recalculer les heures (au cas où la voiture a changé)
                calculerHeuresTrajet(assignation, aeroport, vitesseMoyenne);

                long departMs = assignation.getDateHeureDepart().getTime();
                long arriveeMs = assignation.getDateHeureArrivee().getTime();
                int idVoitureAssignee = assignation.getVoiture().getIdVoiture();

                // Vérifier chevauchement avec les intervalles existants
                boolean chevauchement = false;
                List<long[]> intervals = intervallesOccupes.get(idVoitureAssignee);
                if (intervals != null) {
                    for (long[] interval : intervals) {
                        // Deux intervalles [A,B] et [C,D] se chevauchent si A < D && C < B
                        if (departMs < interval[1] && interval[0] < arriveeMs) {
                            chevauchement = true;
                            System.out.println("   ⚠️ Voiture #" + idVoitureAssignee + 
                                " chevauchement détecté: trajet [" + assignation.getDateHeureDepart() + 
                                " - " + assignation.getDateHeureArrivee() + 
                                "] chevauche [" + new Timestamp(interval[0]) + " - " + new Timestamp(interval[1]) + "]");
                            break;
                        }
                    }
                }

                if (chevauchement) {
                    // Déplacer toutes les réservations vers non-assignées pour retraitement
                    for (Reservation r : assignation.getReservations()) {
                        reservationsNonAssignees.add(r);
                    }
                } else {
                    resultat.ajouterAssignation(assignation);
                    // Enregistrer le nouvel intervalle occupé
                    intervallesOccupes.computeIfAbsent(idVoitureAssignee, k -> new ArrayList<>())
                        .add(new long[]{departMs, arriveeMs});
                }
            }

            // ÉTAPE 3.5: Ajouter les assignations différées (trajets de retour) au résultat
            for (SimulationAssignation assignationDifferee : assignationsDifferees) {
                long departMs = assignationDifferee.getDateHeureDepart().getTime();
                long arriveeMs = assignationDifferee.getDateHeureArrivee().getTime();
                int idVoitureAssignee = assignationDifferee.getVoiture().getIdVoiture();

                // Vérifier chevauchement avec les intervalles existants
                boolean chevauchement = false;
                List<long[]> intervals = intervallesOccupes.get(idVoitureAssignee);
                if (intervals != null) {
                    for (long[] interval : intervals) {
                        if (departMs < interval[1] && interval[0] < arriveeMs) {
                            chevauchement = true;
                            System.out.println("   ⚠️ Voiture #" + idVoitureAssignee +
                                " chevauchement détecté (différé): trajet [" + assignationDifferee.getDateHeureDepart() +
                                " - " + assignationDifferee.getDateHeureArrivee() +
                                "] chevauche [" + new Timestamp(interval[0]) + " - " + new Timestamp(interval[1]) + "]");
                            break;
                        }
                    }
                }

                if (chevauchement) {
                    for (Reservation r : assignationDifferee.getReservations()) {
                        reservationsNonAssignees.add(r);
                    }
                } else {
                    resultat.ajouterAssignation(assignationDifferee);
                    intervallesOccupes.computeIfAbsent(idVoitureAssignee, k -> new ArrayList<>())
                        .add(new long[]{departMs, arriveeMs});
                }
            }

            numeroVague++;
            System.out.println();
        }

        // Traiter les réservations qui restent non assignées après toutes les vagues
        if (!reservationsNonAssignees.isEmpty()) {
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("❌ " + reservationsNonAssignees.size() + 
                               " réservation(s) définitivement non assignée(s)");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            for (Reservation r : reservationsNonAssignees) {
                System.out.println("   • Réservation #" + r.getId() + " - " + 
                                   r.getIdClient() + " (" + r.getNbPassager() + " passagers)");
                resultat.ajouterReservationNonAssignee(r);
            }
            System.out.println();
        }

        System.out.println("========================================");
        System.out.println("📊 RÉSUMÉ DE LA SIMULATION");
        System.out.println("========================================");
        System.out.println("✅ Réservations assignées: " + resultat.getTotalReservationsAssignees());
        System.out.println("❌ Réservations non assignées: " + resultat.getReservationsNonAssignees().size());
        System.out.println("🚗 Voitures utilisées: " + resultat.getAssignations().size());
        System.out.println("========================================\n");

        return resultat;
    }

    /**
     * Trouve la meilleure voiture pour une réservation
     * Sprint 6: Priorité au nombre de trajets (min)
     */
    private SimulationAssignation trouverMeilleureVoiture(
            Reservation reservation,
            List<Voiture> voituresDisponibles,
            Map<Integer, SimulationAssignation> assignationsVague,
            Timestamp heureVague,
            Map<Integer, Integer> compteurTrajetsJour) {

        int nbPassagers = reservation.getNbPassager();

        // 1. D'abord, vérifier si une voiture déjà assignée dans cette vague a encore de la place
        // On cherche parmi les assignations existantes par ordre de création
        for (SimulationAssignation assignation : assignationsVague.values()) {
            if (assignation.peutAccueillir(nbPassagers)) {
                assignation.ajouterReservation(reservation);
                System.out.println("   → Ajoutée à voiture existante (remplissage optimal)");
                return assignation;
            }
        }

        // 2. Chercher une nouvelle voiture parmi celles non utilisées dans cette vague
        List<Voiture> voituresCompatibles = voituresDisponibles.stream()
                .filter(v -> v.getCapacite() >= nbPassagers)
                .filter(v -> !assignationsVague.containsKey(v.getIdVoiture()))
                .collect(Collectors.toList());

        if (voituresCompatibles.isEmpty()) {
            System.out.println("   → Aucune voiture compatible disponible");
            return null;
        }

        // 3. Calculer l'écart pour chaque voiture (capacité - nbPassagers)
        int ecartMin = voituresCompatibles.stream()
                .mapToInt(v -> v.getCapacite() - nbPassagers)
                .min().orElse(0);

        // 4. Filtrer les voitures avec l'écart minimum
        List<Voiture> voituresOptimales = voituresCompatibles.stream()
                .filter(v -> (v.getCapacite() - nbPassagers) == ecartMin)
                .collect(Collectors.toList());

        // 5. SPRINT 6: Priorité au nombre de trajets (minimum) POUR LE JOUR MÊME
        // Utiliser le compteur passé en paramètre (base + simulation)
        Map<Integer, Integer> nbTrajetsParVoiture = new HashMap<>();
        for (Voiture v : voituresOptimales) {
            int nbTrajets = compteurTrajetsJour.getOrDefault(v.getIdVoiture(), 0);
            nbTrajetsParVoiture.put(v.getIdVoiture(), nbTrajets);
        }

        // Trouver le nombre minimum de trajets
        int nbTrajetsMin = nbTrajetsParVoiture.values().stream()
                .min(Integer::compare)
                .orElse(0);

        // Filtrer les voitures avec le nombre minimum de trajets
        List<Voiture> voituresAvecMinTrajets = voituresOptimales.stream()
                .filter(v -> nbTrajetsParVoiture.get(v.getIdVoiture()) == nbTrajetsMin)
                .collect(Collectors.toList());

        System.out.println("   → Écart minimal: " + ecartMin + " | Trajets min: " + nbTrajetsMin + 
                           " | Candidats: " + voituresAvecMinTrajets.size());

        // 6. Priorité carburant: D (Diesel) > H (Hybride) > E (Essence)
        Voiture voitureChoisie = choisirParCarburant(voituresAvecMinTrajets);

        System.out.println("   → Voiture choisie: #" + voitureChoisie.getIdVoiture() + 
                           " (" + voitureChoisie.getRef() + ") | Carburant: " + voitureChoisie.getCarburantLibelle() +
                           " | Trajets: " + nbTrajetsParVoiture.get(voitureChoisie.getIdVoiture()));

        // 7. Créer une nouvelle assignation
        SimulationAssignation assignation = new SimulationAssignation(voitureChoisie, heureVague);
        assignation.ajouterReservation(reservation);
        assignationsVague.put(voitureChoisie.getIdVoiture(), assignation);

        return assignation;
    }

    /**
     * Compare deux voitures pour déterminer si la nouvelle est meilleure que l'actuelle
     * Critères (dans l'ordre): écart capacité → nombre trajets → carburant (D > H > E)
     *
     * @return true si nouvelleVoiture est strictement meilleure que voitureActuelle
     */
    private boolean estMeilleureVoiture(Voiture nouvelleVoiture, Voiture voitureActuelle,
            int nbPassagers, Map<Integer, Integer> compteurTrajetsJour) {

        // 1. Comparer l'écart de capacité
        int ecartNouvelle = nouvelleVoiture.getCapacite() - nbPassagers;
        int ecartActuelle = voitureActuelle.getCapacite() - nbPassagers;

        if (ecartNouvelle < ecartActuelle) return true;
        if (ecartNouvelle > ecartActuelle) return false;

        // 2. Comparer le nombre de trajets (moins = mieux)
        int trajetsNouvelle = compteurTrajetsJour.getOrDefault(nouvelleVoiture.getIdVoiture(), 0);
        int trajetsActuelle = compteurTrajetsJour.getOrDefault(voitureActuelle.getIdVoiture(), 0);

        if (trajetsNouvelle < trajetsActuelle) return true;
        if (trajetsNouvelle > trajetsActuelle) return false;

        // 3. Comparer le carburant (D > H > E)
        int prioriteNouvelle = getPrioriteCarburant(nouvelleVoiture.getCarburant());
        int prioriteActuelle = getPrioriteCarburant(voitureActuelle.getCarburant());

        return prioriteNouvelle > prioriteActuelle;
    }

    /**
     * Retourne la priorité du carburant (plus c'est haut, mieux c'est)
     */
    private int getPrioriteCarburant(String carburant) {
        if ("D".equals(carburant)) return 3;  // Diesel = priorité max 
        if ("E".equals(carburant)) return 2;  // Essence
        return 0;
    }

    /**
     * Vérifie si des réassignations sont possibles pendant la fenêtre d'attente.
     *
     * Si une meilleure voiture devient disponible pendant la fenêtre d'attente
     * (avant l'heure de départ d'une assignation), on réassigne.
     *
     * @param assignationsVague      Les assignations de la vague actuelle
     * @param intervalleVague        [debutFenetre, finFenetre] de la vague
     * @param toutesVoitures         Toutes les voitures du système
     * @param intervallesOccupes     Intervalles occupés par voiture
     * @param compteurTrajetsJour    Compteur de trajets par voiture pour le jour
     * @param assignationsVagueIds   IDs des voitures déjà utilisées dans la vague
     */
    private void verifierReassignationPendantAttente(
            Map<Integer, SimulationAssignation> assignationsVague,
            Timestamp[] intervalleVague,
            List<Voiture> toutesVoitures,
            Map<Integer, List<long[]>> intervallesOccupes,
            Map<Integer, Integer> compteurTrajetsJour) {

        if (intervalleVague == null || assignationsVague.isEmpty()) {
            return;
        }

        long debutFenetreMs = intervalleVague[0].getTime();
        long finFenetreMs = intervalleVague[1].getTime();

        // Pour chaque assignation de la vague
        for (SimulationAssignation assignation : assignationsVague.values()) {
            Voiture voitureActuelle = assignation.getVoiture();
            long heureDepartMs = assignation.getDateHeureDepart().getTime();

            // Calculer le nombre total de passagers de cette assignation
            int nbPassagersTotal = assignation.getReservations().stream()
                    .mapToInt(Reservation::getNbPassager)
                    .sum();

            // Chercher les voitures qui deviennent disponibles dans [debutFenetre, heureDepart[
            for (Voiture voitureCandidate : toutesVoitures) {
                // Ignorer la voiture actuelle
                if (voitureCandidate.getIdVoiture() == voitureActuelle.getIdVoiture()) {
                    continue;
                }

                // Ignorer les voitures déjà utilisées dans cette vague
                if (assignationsVague.containsKey(voitureCandidate.getIdVoiture())) {
                    continue;
                }

                // Vérifier la capacité
                if (voitureCandidate.getCapacite() < nbPassagersTotal) {
                    continue;
                }

                // Trouver le moment où cette voiture devient disponible
                long heureDispoMs = trouverHeureDisponibilite(voitureCandidate, intervallesOccupes, debutFenetreMs);

                // La voiture doit devenir disponible APRÈS le début de la fenêtre
                // et AVANT l'heure de départ de l'assignation
                if (heureDispoMs > debutFenetreMs && heureDispoMs < heureDepartMs) {
                    // Vérifier si cette voiture est meilleure
                    if (estMeilleureVoiture(voitureCandidate, voitureActuelle, nbPassagersTotal, compteurTrajetsJour)) {
                        System.out.println("   🔄 RÉASSIGNATION: Voiture #" + voitureActuelle.getIdVoiture() +
                                " (" + voitureActuelle.getRef() + ", " + voitureActuelle.getCarburantLibelle() +
                                ") → Voiture #" + voitureCandidate.getIdVoiture() +
                                " (" + voitureCandidate.getRef() + ", " + voitureCandidate.getCarburantLibelle() +
                                ") disponible à " + new Timestamp(heureDispoMs));

                        // Mettre à jour le compteur de trajets
                        compteurTrajetsJour.put(voitureActuelle.getIdVoiture(),
                                compteurTrajetsJour.getOrDefault(voitureActuelle.getIdVoiture(), 1) - 1);
                        compteurTrajetsJour.put(voitureCandidate.getIdVoiture(),
                                compteurTrajetsJour.getOrDefault(voitureCandidate.getIdVoiture(), 0) + 1);

                        // Réassigner
                        assignation.setVoiture(voitureCandidate);
                        voitureActuelle = voitureCandidate;
                    }
                }
            }
        }
    }

    /**
     * Trouve l'heure à laquelle une voiture devient disponible (fin de son dernier intervalle occupé)
     * Si la voiture n'a pas d'intervalle chevauchant avec la fenêtre, retourne 0 (disponible immédiatement)
     */
    private long trouverHeureDisponibilite(Voiture voiture, Map<Integer, List<long[]>> intervallesOccupes, long debutFenetreMs) {
        List<long[]> intervals = intervallesOccupes.get(voiture.getIdVoiture());
        if (intervals == null || intervals.isEmpty()) {
            return 0; // Disponible depuis le début
        }

        long heureDispoDerniere = 0;
        for (long[] interval : intervals) {
            // Si l'intervalle se termine APRÈS le début de la fenêtre
            if (interval[1] > debutFenetreMs) {
                // La voiture devient disponible à la fin de cet intervalle
                if (interval[1] > heureDispoDerniere) {
                    heureDispoDerniere = interval[1];
                }
            }
        }

        return heureDispoDerniere;
    }

    /**
     * Traite les réservations non assignées en vérifiant si des voitures reviennent dans la fenêtre d'attente.
     *
     * @return Liste des réservations qui ont pu être assignées
     */
    private List<Reservation> traiterReservationsNonAssigneesAvecVoituresRetour(
            List<Reservation> reservationsNonAssignees,
            Timestamp[] intervalleVague,
            List<Voiture> toutesVoitures,
            Map<Integer, List<long[]>> intervallesOccupes,
            List<SimulationAssignation> assignationsDifferees,  // Liste pour stocker les nouvelles assignations
            Map<Integer, Integer> compteurTrajetsJour,
            Lieu aeroport,
            double vitesseMoyenne) {

        List<Reservation> reservationsTraitees = new ArrayList<>();

        if (intervalleVague == null || reservationsNonAssignees.isEmpty()) {
            return reservationsTraitees;
        }

        long debutFenetreMs = intervalleVague[0].getTime();
        long finFenetreMs = intervalleVague[1].getTime();

        // Trier les réservations par nombre de passagers décroissant
        List<Reservation> reservationsATraiter = new ArrayList<>(reservationsNonAssignees);
        reservationsATraiter.sort((r1, r2) -> Integer.compare(r2.getNbPassager(), r1.getNbPassager()));

        for (Reservation reservation : reservationsATraiter) {
            int nbPassagers = reservation.getNbPassager();

            // Chercher les voitures qui reviennent dans la fenêtre d'attente
            List<VoitureDisponibilite> voituresQuiReviennent = new ArrayList<>();

            for (Voiture voiture : toutesVoitures) {
                // Ignorer les voitures sans capacité suffisante
                if (voiture.getCapacite() < nbPassagers) {
                    continue;
                }

                long heureDispoMs = trouverHeureDisponibilite(voiture, intervallesOccupes, debutFenetreMs);

                // La voiture doit revenir APRÈS le début de la fenêtre et AVANT la fin
                if (heureDispoMs > debutFenetreMs && heureDispoMs <= finFenetreMs) {
                    voituresQuiReviennent.add(new VoitureDisponibilite(voiture, heureDispoMs));
                }
            }

            if (voituresQuiReviennent.isEmpty()) {
                continue;
            }

            // Trier par: heure de dispo croissante, puis critères habituels
            voituresQuiReviennent.sort((v1, v2) -> {
                // D'abord par heure de disponibilité
                int cmpHeure = Long.compare(v1.heureDispoMs, v2.heureDispoMs);
                if (cmpHeure != 0) return cmpHeure;

                // Puis par écart capacité
                int ecart1 = v1.voiture.getCapacite() - nbPassagers;
                int ecart2 = v2.voiture.getCapacite() - nbPassagers;
                int cmpEcart = Integer.compare(ecart1, ecart2);
                if (cmpEcart != 0) return cmpEcart;

                // Puis par nombre de trajets
                int trajets1 = compteurTrajetsJour.getOrDefault(v1.voiture.getIdVoiture(), 0);
                int trajets2 = compteurTrajetsJour.getOrDefault(v2.voiture.getIdVoiture(), 0);
                int cmpTrajets = Integer.compare(trajets1, trajets2);
                if (cmpTrajets != 0) return cmpTrajets;

                // Puis par carburant (D > H > E)
                return Integer.compare(getPrioriteCarburant(v2.voiture.getCarburant()),
                                       getPrioriteCarburant(v1.voiture.getCarburant()));
            });

            // Prendre la meilleure voiture
            VoitureDisponibilite meilleure = voituresQuiReviennent.get(0);
            Voiture voitureChoisie = meilleure.voiture;
            Timestamp heureDepart = new Timestamp(meilleure.heureDispoMs);

            System.out.println("\n   🔄 ASSIGNATION DIFFÉRÉE: Réservation #" + reservation.getId() +
                    " (" + reservation.getIdClient() + ") → Voiture #" + voitureChoisie.getIdVoiture() +
                    " (" + voitureChoisie.getRef() + ", " + voitureChoisie.getCarburantLibelle() +
                    ") disponible à " + heureDepart);

            // Créer une NOUVELLE assignation pour ce trajet différé (trajet de retour)
            SimulationAssignation nouvelleAssignation = new SimulationAssignation(voitureChoisie, heureDepart);
            nouvelleAssignation.setDateHeureDepart(heureDepart); // Définir l'heure de départ = retour voiture
            nouvelleAssignation.ajouterReservation(reservation);
            nouvelleAssignation.setDebutVague(intervalleVague[0]);
            nouvelleAssignation.setFinFenetreVague(intervalleVague[1]);
            assignationsDifferees.add(nouvelleAssignation);  // Ajouter à la liste des assignations différées

            // Calculer les heures de trajet
            calculerHeuresTrajet(nouvelleAssignation, aeroport, vitesseMoyenne);

            // Mettre à jour les intervalles occupés
            long departMs = nouvelleAssignation.getDateHeureDepart().getTime();
            long arriveeMs = nouvelleAssignation.getDateHeureArrivee().getTime();
            intervallesOccupes.computeIfAbsent(voitureChoisie.getIdVoiture(), k -> new ArrayList<>())
                .add(new long[]{departMs, arriveeMs});

            // Incrémenter le compteur de trajets
            compteurTrajetsJour.put(voitureChoisie.getIdVoiture(),
                compteurTrajetsJour.getOrDefault(voitureChoisie.getIdVoiture(), 0) + 1);

            reservationsTraitees.add(reservation);
        }

        return reservationsTraitees;
    }

    /**
     * Classe interne pour stocker une voiture et son heure de disponibilité
     */
    private static class VoitureDisponibilite {
        Voiture voiture;
        long heureDispoMs;

        VoitureDisponibilite(Voiture voiture, long heureDispoMs) {
            this.voiture = voiture;
            this.heureDispoMs = heureDispoMs;
        }
    }

    /**
     * Choisit la voiture selon la priorité carburant: D > H > E, puis random
     */
    private Voiture choisirParCarburant(List<Voiture> voitures) {
        if (voitures.size() == 1) return voitures.get(0);

        // Priorité D > H > E
        List<Voiture> diesel = voitures.stream().filter(v -> "D".equals(v.getCarburant())).collect(Collectors.toList());
        if (!diesel.isEmpty()) {
            return diesel.size() == 1 ? diesel.get(0) : diesel.get(new Random().nextInt(diesel.size()));
        }

        // List<Voiture> hybride = voitures.stream().filter(v -> "H".equals(v.getCarburant())).collect(Collectors.toList());
        // if (!hybride.isEmpty()) {
        //     return hybride.size() == 1 ? hybride.get(0) : hybride.get(new Random().nextInt(hybride.size()));
        // }

        List<Voiture> essence = voitures.stream().filter(v -> "E".equals(v.getCarburant())).collect(Collectors.toList());
        if (!essence.isEmpty()) {
            return essence.size() == 1 ? essence.get(0) : essence.get(new Random().nextInt(essence.size()));
        }

        return voitures.get(new Random().nextInt(voitures.size()));
    }

    /**
     * Calcule les heures de départ et d'arrivée pour une assignation
     * - Départ = MAX(date_heure de la DERNIÈRE réservation, heure de retour de la voiture si déjà définie)
     * - Route: aéroport -> lieu le plus proche -> lieu suivant -> ... -> dernier lieu -> retour aéroport
     * - Si distances similaires, ordre alphabétique du lieu
     */
    private void calculerHeuresTrajet(SimulationAssignation assignation, Lieu aeroport, double vitesseMoyenne) {
        // SPRINT 6: L'heure de départ = date_heure de la DERNIÈRE réservation de cette voiture
        Timestamp departReservation = assignation.getReservations().stream()
                .map(Reservation::getDateHeure)
                .max(Timestamp::compareTo)
                .orElse(assignation.getHeureVague());

        // Si une heure de départ est déjà définie (ex: voiture qui revient), prendre le MAX
        Timestamp departExistant = assignation.getDateHeureDepart();
        Timestamp depart = (departExistant != null && departExistant.after(departReservation))
                ? departExistant
                : departReservation;

        assignation.setDateHeureDepart(depart);

        if (aeroport == null) {
            assignation.setDateHeureArrivee(depart);
            return;
        }

        // Récupérer tous les lieux distincts des réservations
        List<Integer> idLieux = assignation.getReservations().stream()
                .map(Reservation::getIdLieu)
                .distinct()
                .collect(Collectors.toList());

        if (idLieux.isEmpty()) {
            assignation.setDateHeureArrivee(depart);
            return;
        }

        // Récupérer les objets Lieu
        List<Lieu> lieux = new ArrayList<>();
        for (int idLieu : idLieux) {
            Lieu lieu = lieuDAO.findById(idLieu);
            if (lieu != null) {
                lieux.add(lieu);
            }
        }

        // Trier les lieux par distance à l'aéroport (plus proche en premier)
        // Si distance égale, ordre alphabétique du libellé
        int idAeroport = aeroport.getId();
        lieux.sort((l1, l2) -> {
            Double d1 = distanceDAO.getDistance(idAeroport, l1.getId());
            Double d2 = distanceDAO.getDistance(idAeroport, l2.getId());
            double dist1 = d1 != null ? d1 : Double.MAX_VALUE;
            double dist2 = d2 != null ? d2 : Double.MAX_VALUE;
            int cmp = Double.compare(dist1, dist2);
            if (cmp != 0) return cmp;
            return l1.getLibelle().compareToIgnoreCase(l2.getLibelle());
        });

        // Construire l'itinéraire étape par étape
        List<EtapeItineraire> itineraire = new ArrayList<>();
        double distanceTotale = 0.0;
        int positionCourante = idAeroport;
        String nomCourant = aeroport.getLibelle();
        int etapeNum = 1;

        for (Lieu lieu : lieux) {
            Double dist = distanceDAO.getDistance(positionCourante, lieu.getId());
            double distEtape = dist != null ? dist : 0.0;
            double dureeMinutes = (distEtape / vitesseMoyenne) * 60.0;
            
            itineraire.add(new EtapeItineraire(etapeNum, nomCourant, lieu.getLibelle(), distEtape, dureeMinutes));
            
            distanceTotale += distEtape;
            positionCourante = lieu.getId();
            nomCourant = lieu.getLibelle();
            etapeNum++;
        }

        // Retour à l'aéroport depuis le dernier lieu
        Double distRetour = distanceDAO.getDistance(positionCourante, idAeroport);
        double distRetourVal = distRetour != null ? distRetour : 0.0;
        double dureeRetour = (distRetourVal / vitesseMoyenne) * 60.0;
        itineraire.add(new EtapeItineraire(etapeNum, nomCourant, aeroport.getLibelle(), distRetourVal, dureeRetour));
        distanceTotale += distRetourVal;

        assignation.setItineraire(itineraire);

        // Calculer la durée totale en millisecondes
        double dureeHeures = distanceTotale / vitesseMoyenne;
        long dureeMillis = (long) (dureeHeures * 3600 * 1000);

        Timestamp arrivee = new Timestamp(depart.getTime() + dureeMillis);
        assignation.setDateHeureArrivee(arrivee);

        System.out.println("   🕐 Départ: " + depart + " | Distance: " + distanceTotale + " km | Arrivée: " + arrivee);
    }

    /**
     * Regroupe les réservations par vague selon le temps d'attente (SPRINT 6).
     * 
     * LOGIQUE:
     * - Vague commence à la première réservation non traitée
     * - Fenêtre de traitement = [première_réservation, première_réservation + temps_attente]
     * - Toutes les réservations dans cette fenêtre font partie de la même vague
     * - La vague suivante commence à la première réservation APRÈS la fenêtre
     * 
     * IMPORTANT: Chaque voiture part à l'heure de SA dernière réservation (calculé dans calculerHeuresTrajet)
     * 
     * Exemple:
     * - MATIN001: 06:00 → Ouvre fenêtre [06:00, 06:30]
     * - MATIN002: 06:30 → Dans la fenêtre, même vague
     * - MATIN003: 07:00 → Hors fenêtre, nouvelle vague
     * 
     * Si VOI0004 a MATIN001 (06:00) + MATIN003 (07:00), elle part à 07:00
     */
    private Map<String, List<Reservation>> regrouperParVague(List<Reservation> reservations) {
        int tempsAttenteMinutes = parametreDAO.getTempsAttente();
        long tempsAttenteMs = tempsAttenteMinutes * 60L * 1000L;

        // Trier toutes les réservations par date_heure croissante
        List<Reservation> triees = new ArrayList<>(reservations);
        triees.sort((r1, r2) -> r1.getDateHeure().compareTo(r2.getDateHeure()));

        Map<String, List<Reservation>> vagues = new LinkedHashMap<>();
        int i = 0;

        while (i < triees.size()) {
            // La première réservation ouvre la fenêtre de traitement
            Timestamp debutFenetre = triees.get(i).getDateHeure();
            long debutMs = tronquerAuxMinutes(debutFenetre);
            long finFenetreMs = debutMs + tempsAttenteMs;

            // Collecter toutes les réservations dans [debutMs, finFenetreMs] (inclus)
            List<Reservation> vagueReservations = new ArrayList<>();
            Timestamp derniereHeure = debutFenetre;

            while (i < triees.size()) {
                Timestamp heureCourante = triees.get(i).getDateHeure();
                long heureCourtanteMs = tronquerAuxMinutes(heureCourante);

                if (heureCourtanteMs <= finFenetreMs) {
                    vagueReservations.add(triees.get(i));
                    if (heureCourante.after(derniereHeure)) {
                        derniereHeure = heureCourante;
                    }
                    i++;
                } else {
                    break;
                }
            }

            // La clé de la vague = fenêtre de traitement (pour affichage)
            String cleVague = String.format("%tF %tH:%tM", debutFenetre, debutFenetre, debutFenetre);
            vagues.put(cleVague, vagueReservations);

            // Stocker l'intervalle de la vague [début fenêtre, fin fenêtre]
            Timestamp tsDebut = new Timestamp(debutMs);
            Timestamp tsFin = new Timestamp(finFenetreMs);
            vagueIntervalles.put(cleVague, new Timestamp[]{tsDebut, tsFin});
        }

        return vagues;
    }

    /**
     * Tronque un Timestamp aux minutes (met les secondes et millisecondes à 0)
     */
    private long tronquerAuxMinutes(Timestamp ts) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ts.getTime());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
