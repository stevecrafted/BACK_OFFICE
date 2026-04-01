package org.example.Service;

import org.example.DAO.*;
import org.example.Model.*;
import org.example.Util.UtilSimulation;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.*;

public class SimulationAssignationService {

    private ReservationDAO reservationDAO = new ReservationDAO();
    private VoitureDAO voitureDAO = new VoitureDAO();
    private DistanceDAO distanceDAO = new DistanceDAO();
    private LieuDAO lieuDAO = new LieuDAO();
    private ParametreDAO parametreDAO = new ParametreDAO();
    private AssignationDAO assignationDAO = new AssignationDAO();
    private VoitureService voitureService = new VoitureService(); // SPRINT 8

    // Stocke les intervalles [debutFenetre, finFenetre] de chaque vague (clé =
    // cleVague)
    private Map<String, Timestamp[]> vagueIntervalles = new LinkedHashMap<>();

    /**
     * Simule l'assignation des voitures pour une date donnée
     * SANS INSERTION EN BASE - Juste simulation
     * Prend en compte les assignations déjà existantes en base
     */
    public ResultatSimulation simulerAssignation(Date dateSimulation) {
 
        // Apres on a les temps de depart, le temps d'arrivée et le temps d'attente.
        // Chaque traitement dependra du temps entre l'intervalle de chaque vague

        ResultatSimulation resultat = new ResultatSimulation(dateSimulation);
        vagueIntervalles.clear();

        // 1. Liste des reservations
        List<Reservation> reservations = reservationDAO.findByDate(dateSimulation);
        if (reservations.isEmpty()) {
            System.out.println("Aucune réservation trouvée pour cette date");
            return resultat;
        }

        // 2. Récupérer les assignations existantes en base
        List<Assignation> assignationsExistantes = assignationDAO.findAll();
        Set<Integer> reservationsDejaAssignees = new HashSet<>();

        // 3. Récupérer toutes les voitures et la vitesse moyenne
        List<Voiture> toutesVoitures = voitureDAO.findAll();
        double vitesseMoyenne = parametreDAO.getVM();
        Lieu aeroport = lieuDAO.findAeroport();

        // ---
        // Etape 1 : Identifier les réservations déjà assignées
        // Note: On considère qu'une réservation est "assignée" si elle apparaît dans une assignation confirmée
        for (Assignation assignation : assignationsExistantes) {
            for (ReservationAssignation ra : assignation.getReservationAssignations()) {
                reservationsDejaAssignees.add(ra.getIdReservation());
            }
        }

        // Filtrer pour ne garder que les réservations non assignées
        List<Reservation> reservationsATraiter = new ArrayList<>();
        for (Reservation r : reservations) {
            if (!reservationsDejaAssignees.contains(r.getId())) {
                reservationsATraiter.add(r);
            }
        }

        if (reservationsATraiter.isEmpty()) {
            System.out.println(" Toutes les réservations sont déjà assignées");
            return resultat;
        }

        // ---
        // Etape 2 : Trier les réservations par date et regrouper dynamiquement
        int tempsAttenteMinutes = parametreDAO.getTempsAttente();

        // Trier toutes les réservations par date croissante
        reservationsATraiter.sort((r1, r2) -> r1.getDateHeure().compareTo(r2.getDateHeure()));

        // Compteur de trajets par voiture (idVoiture -> nombre de trajets)
        Map<Integer, Integer> compteurTrajetsJour = new HashMap<>();

        // Heure de retour de chaque voiture (idVoiture -> heureRetour à l'aéroport)
        // Une voiture n'est disponible que si finFenetreVague >= heureRetour
        Map<Integer, Timestamp> voituresEnTrajet = new HashMap<>();

        // ---
        // Etape 3 : Traiter les vagues dynamiquement
        int numeroVague = 1;

        while (!reservationsATraiter.isEmpty()) {
            // La première réservation définit le début de la vague
            Timestamp premiereReservation = UtilSimulation.trouverPremiereReservationDuJour(reservationsATraiter).getDateHeure();
            
            // Calculer le moment optimal de départ de la vague en tenant compte des véhicules en trajet
            // Si des véhicules à grande capacité reviennent bientôt, on attend leur retour
            Timestamp debutVague = calculerDebutVagueOptimal(premiereReservation, toutesVoitures, voituresEnTrajet, 
                                                              tempsAttenteMinutes, dateSimulation);
            
            long debutVagueMs = UtilSimulation.tronquerAuxMinutes(debutVague);
            long finFenetreMaxMs = debutVagueMs + (tempsAttenteMinutes * 60L * 1000L);

            // Collecter toutes les réservations qui peuvent partir dans cette vague:
            // - Réservations dont l'heure <= fin de fenêtre (peuvent attendre)
            // - Réservations dont l'heure < debutVague (en attente depuis avant)
            List<Reservation> reservationsVague = new ArrayList<>();
            for (Reservation r : reservationsATraiter) {
                // Une réservation fait partie de cette vague si:
                // - Son heure est avant ou égale à la fin de la fenêtre ET
                // - Son heure est après ou égale au début de la première réservation
                if (r.getDateHeure().getTime() <= finFenetreMaxMs) {
                    reservationsVague.add(r);
                }
            }

            // Retirer les réservations de cette vague de la liste globale
            reservationsATraiter.removeAll(reservationsVague);

            // La fin de fenêtre de vague = debut + temps d'attente
            // Les voitures attendent jusqu'à cette heure avant de partir (sauf si pleines)
            Timestamp finFenetreVague = new Timestamp(finFenetreMaxMs);

            String cleVague = String.format("%tF %tH:%tM", debutVague, debutVague, debutVague);

            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println(" VAGUE #" + numeroVague + " - " + cleVague);
            System.out.println(" " + reservationsVague.size() + " réservation(s)");
            

            // Liste des assignations de cette vague (pour la réassignation)
            List<SimulationAssignation> assignationsVague = new ArrayList<>();
            
            // Copier les réservations pour manipulation sans modifier l'original
            // Utiliser une map pour tracker les passagers restants par réservation
            Map<Integer, Integer> passagersRestantsParReservation = new HashMap<>();
            for (Reservation r : reservationsVague) {
                passagersRestantsParReservation.put(r.getId(), r.getNbPassager());
            }

            // Traiter les réservations de cette vague
            // Algorithme: 
            // 1. Chercher la réservation avec le plus de passagers
            // 2. Trouver la meilleure voiture pour cette réservation
            // 3. Compléter avec d'autres réservations optimales
            // 4. Si passagers restants, chercher une autre voiture pour les restes
            
            while (true) {
                // Vérifier s'il reste des passagers à assigner
                int totalPassagersRestants = 0;
                for (Integer p : passagersRestantsParReservation.values()) {
                    totalPassagersRestants += p;
                }
                if (totalPassagersRestants == 0) break;
                
                // 1. Trouver la réservation avec le plus de passagers restants
                Reservation reservation = trouverReservationAvecPlusDePassagers(reservationsVague, passagersRestantsParReservation);
                if (reservation == null) break;
                
                int passagersRestants = passagersRestantsParReservation.getOrDefault(reservation.getId(), 0);
                if (passagersRestants <= 0) {
                    reservationsVague.remove(reservation);
                    continue;
                }
                
                // 2. Trouver la meilleure voiture pour ces passagers
                Reservation reservationPourVoiture = copierReservation(reservation, passagersRestants);
                Voiture voiture = UtilSimulation.trouverMeilleurVoiture(reservationPourVoiture, toutesVoitures,
                        compteurTrajetsJour, voituresEnTrajet, finFenetreVague, dateSimulation);
                
                // Si aucune voiture ne peut prendre tous les passagers, chercher la plus grande disponible
                if (voiture == null) {
                    voiture = trouverPlusGrandeVoitureDisponible(toutesVoitures, compteurTrajetsJour, 
                                                                  voituresEnTrajet, finFenetreVague, dateSimulation);
                    
                    if (voiture == null) {
                        System.out.println(" Aucune voiture disponible pour réservation #" + reservation.getId() + 
                            " (" + reservation.getIdClient() + ", " + passagersRestants + "p)");
                        break; // Plus de voitures disponibles pour cette vague
                    }
                    
                    System.out.println(" Split nécessaire: " + reservation.getIdClient() + 
                        " (" + passagersRestants + "p) → Voiture " + voiture.getRef() + 
                        " (" + voiture.getCapacite() + " places)");
                }
                
                // Créer l'assignation
                SimulationAssignation simulationAssignation = new SimulationAssignation(voiture, debutVague);
                simulationAssignation.setDebutVague(debutVague);
                
                // Assigner les passagers de cette réservation
                int passagersAPrendre = Math.min(passagersRestants, voiture.getCapacite());
                Reservation reservationAssignee = copierReservation(reservation, passagersAPrendre);
                simulationAssignation.ajouterReservation(reservationAssignee);
                
                // Mettre à jour les passagers restants
                int nouveauRestant = passagersRestants - passagersAPrendre;
                passagersRestantsParReservation.put(reservation.getId(), nouveauRestant);
                
                if (nouveauRestant <= 0) {
                    reservationsVague.remove(reservation);
                }
                
                System.out.println(" Vehicule " + voiture.getRef() + " (" + voiture.getCapacite() + " places) pour " + 
                    reservation.getIdClient() + " (" + passagersAPrendre + "/" + passagersRestants + " passagers)");
                
                // 3. Si places restantes, compléter avec d'autres réservations optimales
                if (simulationAssignation.getPlacesRestantes() > 0) {
                    remplissageOptimal(simulationAssignation, reservationsVague, passagersRestantsParReservation);
                }
                
                // Calculer la disponibilité réelle de la voiture
                Timestamp disponibiliteVoiture = UtilSimulation.combinerDateEtHeure(dateSimulation, voiture.getDisponibilite());
                if (voituresEnTrajet.containsKey(voiture.getIdVoiture())) {
                    Timestamp heureRetour = voituresEnTrajet.get(voiture.getIdVoiture());
                    if (heureRetour != null && heureRetour.after(disponibiliteVoiture)) {
                        disponibiliteVoiture = heureRetour;
                    }
                }
                
                // Calculer l'heure de départ
                Timestamp heureDepart;
                if (simulationAssignation.getPlacesRestantes() == 0) {
                    // Voiture pleine: part dès qu'elle est disponible ou à l'heure de début de vague
                    heureDepart = disponibiliteVoiture.after(debutVague) ? disponibiliteVoiture : debutVague;
                } else {
                    // Voiture non pleine: attend jusqu'à la fin de la fenêtre
                    heureDepart = disponibiliteVoiture.after(finFenetreVague) ? disponibiliteVoiture : finFenetreVague;
                }

                // Calculer l'itinéraire et les heures de trajet
                UtilSimulation.calculerItineraire(simulationAssignation, aeroport, heureDepart, finFenetreVague,
                        distanceDAO, lieuDAO, vitesseMoyenne);

                // Ajouter au résultat
                resultat.ajouterAssignation(simulationAssignation);
                assignationsVague.add(simulationAssignation);

                // Incrémenter le compteur de trajets pour cette voiture
                int trajetsActuels = compteurTrajetsJour.getOrDefault(voiture.getIdVoiture(), 0);
                compteurTrajetsJour.put(voiture.getIdVoiture(), trajetsActuels + 1);

                // Calculer l'heure de retour à l'aéroport et mettre à jour voituresEnTrajet
                setHeureRetour(simulationAssignation, voituresEnTrajet, voiture, aeroport, vitesseMoyenne);
            }

            // ---
            // Etape 4 : Réassignation pendant l'attente
            // Pour chaque assignation qui n'est pas encore partie, vérifier si une meilleure voiture est disponible
            for (SimulationAssignation assignation : assignationsVague) {
                // Ne réassigner que les voitures qui n'ont pas encore parti (non pleines, attendent finFenetreVague)
                if (assignation.getPlacesRestantes() == 0) {
                    // Voiture pleine, déjà partie
                    continue;
                }

                Voiture voitureActuelle = assignation.getVoiture();
                int nbPassagersTotal = assignation.getVoiture().getCapacite() - assignation.getPlacesRestantes();

                // Chercher une meilleure voiture parmi celles qui sont maintenant disponibles
                for (Voiture candidat : toutesVoitures) {
                    if (candidat.getIdVoiture() == voitureActuelle.getIdVoiture()) {
                        continue; // Même voiture
                    }

                    // Vérifier si le candidat est disponible
                    Timestamp disponibiliteReelle = UtilSimulation.combinerDateEtHeure(dateSimulation, candidat.getDisponibilite());
                    if (voituresEnTrajet.containsKey(candidat.getIdVoiture())) {
                        Timestamp heureRetour = voituresEnTrajet.get(candidat.getIdVoiture());
                        if (heureRetour != null && heureRetour.after(disponibiliteReelle)) {
                            disponibiliteReelle = heureRetour;
                        }
                    }

                    // Le candidat doit être disponible avant finFenetreVague
                    if (disponibiliteReelle.after(finFenetreVague)) {
                        continue;
                    }

                    // Le candidat doit avoir assez de places
                    if (candidat.getCapacite() < nbPassagersTotal) {
                        continue;
                    }

                    // Vérifier si le candidat est meilleur
                    if (UtilSimulation.estMeilleureVoiture(candidat, voitureActuelle, nbPassagersTotal, compteurTrajetsJour)) { 

                        // Libérer l'ancienne voiture
                        voituresEnTrajet.remove(voitureActuelle.getIdVoiture());
                        int trajetsActuels = compteurTrajetsJour.getOrDefault(voitureActuelle.getIdVoiture(), 0);
                        if (trajetsActuels > 0) {
                            compteurTrajetsJour.put(voitureActuelle.getIdVoiture(), trajetsActuels - 1);
                        }

                        // Réassigner à la nouvelle voiture
                        assignation.setVoiture(candidat);

                        // Recalculer l'itinéraire
                        UtilSimulation.calculerItineraire(assignation, aeroport, finFenetreVague, finFenetreVague,
                                distanceDAO, lieuDAO, vitesseMoyenne);

                        // Mettre à jour le compteur et l'heure de retour pour la nouvelle voiture
                        int trajetsNouveau = compteurTrajetsJour.getOrDefault(candidat.getIdVoiture(), 0);
                        compteurTrajetsJour.put(candidat.getIdVoiture(), trajetsNouveau + 1);
                        setHeureRetour(assignation, voituresEnTrajet, candidat, aeroport, vitesseMoyenne);

                        break; // Une seule réassignation par assignation
                    }
                }
            }

            System.out.println(" Fin fenêtre: " + finFenetreVague);
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            // ===== SPRINT 8: TRAITEMENT DES RETOURS DE VÉHICULES =====
            // Collecter les réservations non assignées de cette vague
            List<Reservation> reservationsNonAssignees = new ArrayList<>();
            for (Reservation r : reservationsVague) {
                int restants = passagersRestantsParReservation.getOrDefault(r.getId(), 0);
                if (restants > 0) {
                    Reservation copie = copierReservation(r, restants);
                    reservationsNonAssignees.add(copie);
                }
            }
            
            // Ajouter aussi les réservations en attente des vagues précédentes
            reservationsNonAssignees.addAll(reservationsATraiter);
            
            // S'il y a des réservations non assignées, tenter de les assigner aux véhicules qui reviennent
            if (!reservationsNonAssignees.isEmpty()) {
                // Déterminer la date/heure limite pour le traitement des retours
                Timestamp dateHeureProchainVague = null;
                if (!reservationsATraiter.isEmpty()) {
                    dateHeureProchainVague = reservationsATraiter.get(0).getDateHeure();
                } else {
                    // Si pas de prochaine vague, utiliser la fin de la journée
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(dateSimulation);
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    dateHeureProchainVague = new Timestamp(cal.getTimeInMillis());
                }
                
                System.out.println(" SPRINT 8: Traitement des retours de véhicules...");
                System.out.println(" Réservations non assignées: " + reservationsNonAssignees.size());
                
                // Appeler le traitement des retours de véhicules
                SimulationAssignation assignationRetour = traiterRetourVehicule(
                    reservationsNonAssignees, 
                    reservationsDejaAssignees,
                    finFenetreVague, 
                    dateHeureProchainVague,
                    dateSimulation, 
                    toutesVoitures, 
                    voituresEnTrajet,
                    compteurTrajetsJour, 
                    vitesseMoyenne, 
                    aeroport, 
                    tempsAttenteMinutes
                );
                
                // Si une assignation a été créée, l'ajouter au résultat
                if (assignationRetour != null) {
                    resultat.ajouterAssignation(assignationRetour);
                    System.out.println(" ✓ Assignation créée avec véhicule de retour: " + 
                        assignationRetour.getVoiture().getRef());
                    
                    // Retirer les réservations assignées de toutes les listes
                    for (Reservation r : assignationRetour.getReservations()) {
                        // Retirer de la liste locale
                        reservationsNonAssignees.removeIf(res -> res.getId() == r.getId());
                        
                        // Retirer de la liste globale des réservations à traiter
                        reservationsATraiter.removeIf(res -> res.getId() == r.getId());
                        
                        // Mettre à jour les passagers restants
                        int passagersAssignes = assignationRetour.getPassagersAssignes(r.getId());
                        int restants = passagersRestantsParReservation.getOrDefault(r.getId(), 0);
                        int nouveauRestant = Math.max(0, restants - passagersAssignes);
                        passagersRestantsParReservation.put(r.getId(), nouveauRestant);
                        
                        // Si des passagers restent, créer une nouvelle réservation pour eux
                        if (nouveauRestant > 0) {
                            Reservation copie = copierReservation(r, nouveauRestant);
                            // Mettre l'heure à la prochaine disponibilité
                            Timestamp prochaineDisponibilite = trouverProchaineDisponibilite(
                                toutesVoitures, voituresEnTrajet, finFenetreVague, dateSimulation);
                            if (prochaineDisponibilite != null && prochaineDisponibilite.after(copie.getDateHeure())) {
                                copie.setDateHeure(prochaineDisponibilite);
                            }
                            reservationsATraiter.add(copie);
                            System.out.println(" Reporter " + r.getIdClient() + " (" + nouveauRestant + " passagers) à " + copie.getDateHeure());
                        }
                    }
                    
                    // Mettre à jour voituresEnTrajet avec l'heure de retour du véhicule utilisé
                    setHeureRetour(assignationRetour, voituresEnTrajet, assignationRetour.getVoiture(), aeroport, vitesseMoyenne);
                } else {
                    System.out.println(" ✗ Aucun véhicule de retour disponible");
                }
            }
            // ===== FIN SPRINT 8 =====
            
            // Reporter les réservations avec des passagers restants à la vague suivante
            // Trouver la prochaine heure de disponibilité d'une voiture
            Timestamp prochaineDisponibilite = trouverProchaineDisponibilite(toutesVoitures, voituresEnTrajet, finFenetreVague, dateSimulation);
            
            for (Reservation r : reservationsVague) {
                int restants = passagersRestantsParReservation.getOrDefault(r.getId(), 0);
                if (restants > 0) {
                    // Créer une copie avec les passagers restants et une nouvelle heure
                    Reservation copie = copierReservation(r, restants);
                    // Mettre à jour l'heure à la prochaine disponibilité de voiture
                    if (prochaineDisponibilite != null && prochaineDisponibilite.after(copie.getDateHeure())) {
                        copie.setDateHeure(prochaineDisponibilite);
                    }
                    reservationsATraiter.add(copie);
                    System.out.println(" Reporter " + r.getIdClient() + " (" + restants + " passagers) à " + copie.getDateHeure());
                }
            }

            numeroVague++;
            System.out.println();
        }

        return resultat;
    }
    
    /**
     * Trouve la prochaine heure de disponibilité d'une voiture
     */
    private Timestamp trouverProchaineDisponibilite(List<Voiture> voitures, Map<Integer, Timestamp> voituresEnTrajet, 
                                                     Timestamp apres, java.sql.Date dateSimulation) {
        Timestamp prochaineDisponibilite = null;
        
        for (Voiture v : voitures) {
            // Calculer la disponibilité réelle de cette voiture
            Timestamp dispo = UtilSimulation.combinerDateEtHeure(dateSimulation, v.getDisponibilite());
            
            if (voituresEnTrajet.containsKey(v.getIdVoiture())) {
                Timestamp heureRetour = voituresEnTrajet.get(v.getIdVoiture());
                if (heureRetour != null && heureRetour.after(dispo)) {
                    dispo = heureRetour;
                }
            }
            
            // On cherche la première voiture disponible après 'apres'
            if (dispo.after(apres)) {
                if (prochaineDisponibilite == null || dispo.before(prochaineDisponibilite)) {
                    prochaineDisponibilite = dispo;
                }
            }
        }
        
        return prochaineDisponibilite;
    }
    
    /**
     * Calcule le début optimal de la vague en tenant compte des véhicules en trajet.
     * Si des véhicules à grande capacité reviennent bientôt après l'heure de la première réservation,
     * on attend leur retour pour optimiser le regroupement des passagers.
     */
    private Timestamp calculerDebutVagueOptimal(Timestamp premiereReservation, List<Voiture> voitures,
                                                  Map<Integer, Timestamp> voituresEnTrajet,
                                                  int tempsAttenteMinutes, java.sql.Date dateSimulation) {
        // Calculer la capacité totale des voitures actuellement disponibles
        int capaciteDisponible = 0;
        int capaciteTotale = 0;
        
        for (Voiture v : voitures) {
            capaciteTotale += v.getCapacite();
            
            Timestamp dispo = UtilSimulation.combinerDateEtHeure(dateSimulation, v.getDisponibilite());
            if (voituresEnTrajet.containsKey(v.getIdVoiture())) {
                Timestamp heureRetour = voituresEnTrajet.get(v.getIdVoiture());
                if (heureRetour != null && heureRetour.after(dispo)) {
                    dispo = heureRetour;
                }
            }
            
            // Si la voiture est disponible à l'heure de la première réservation
            if (!dispo.after(premiereReservation)) {
                capaciteDisponible += v.getCapacite();
            }
        }
        
        // Si moins de 50% de la capacité totale est disponible, vérifier si on peut attendre
        if (capaciteDisponible < capaciteTotale * 0.5) {
            // Trouver l'heure de retour du plus gros véhicule en trajet
            Timestamp prochainRetourOptimal = null;
            int meilleureCapacite = 0;
            
            for (Voiture v : voitures) {
                if (voituresEnTrajet.containsKey(v.getIdVoiture())) {
                    Timestamp heureRetour = voituresEnTrajet.get(v.getIdVoiture());
                    // Ne considérer que les véhicules qui reviennent dans les prochaines 2 heures
                    long deuxHeuresMs = 2 * 60 * 60 * 1000L;
                    if (heureRetour != null && heureRetour.after(premiereReservation) 
                        && heureRetour.getTime() - premiereReservation.getTime() < deuxHeuresMs) {
                        // Prioriser les gros véhicules
                        if (v.getCapacite() > meilleureCapacite) {
                            meilleureCapacite = v.getCapacite();
                            prochainRetourOptimal = heureRetour;
                        }
                    }
                }
            }
            
            // Si un gros véhicule revient bientôt, attendre son retour
            if (prochainRetourOptimal != null && meilleureCapacite >= 9) {
                return prochainRetourOptimal;
            }
        }
        
        return premiereReservation;
    } 
    
    public void setHeureRetour(SimulationAssignation simulationAssignation, Map<Integer, Timestamp> voituresEnTrajet, Voiture voiture, Lieu aeroport, double vitesseMoyenne) {
        // L'heure d'arrivée inclut déjà le retour à l'aéroport (calculé dans calculerItineraire)
        // Donc heureRetour = heureArrivee
        Timestamp heureArrivee = simulationAssignation.getDateHeureArrivee();
        if (heureArrivee != null) {
            voituresEnTrajet.put(voiture.getIdVoiture(), heureArrivee);
        }
    }
    
    /**
     * Trouve la réservation avec le plus de passagers restants
     */
    private Reservation trouverReservationAvecPlusDePassagers(List<Reservation> reservations, Map<Integer, Integer> passagersRestants) {
        Reservation max = null;
        int maxPassagers = 0;
        
        for (Reservation r : reservations) {
            int passagers = passagersRestants.getOrDefault(r.getId(), r.getNbPassager());
            if (passagers > maxPassagers) {
                maxPassagers = passagers;
                max = r;
            }
        }
        
        return max;
    }
    
    /**
     * Copie une réservation avec un nombre de passagers spécifique
     */
    private Reservation copierReservation(Reservation source, int nbPassagers) {
        Reservation copie = new Reservation();
        copie.setId(source.getId());
        copie.setIdLieu(source.getIdLieu());
        copie.setIdClient(source.getIdClient());
        copie.setDateHeure(source.getDateHeure());
        copie.setNbPassager(nbPassagers);
        return copie;
    }
    
    /**
     * Remplit les places restantes de l'assignation avec d'autres réservations de la vague
     * Priorise les réservations vers la même destination que celles déjà dans le véhicule
     */
    private void remplissageOptimal(SimulationAssignation assignation, List<Reservation> reservationsVague, 
                                     Map<Integer, Integer> passagersRestantsParReservation) {
        // Obtenir la/les destination(s) déjà dans le véhicule
        Set<Integer> destinationsActuelles = new HashSet<>();
        for (Reservation r : assignation.getReservations()) {
            destinationsActuelles.add(r.getIdLieu());
        }
        
        while (assignation.getPlacesRestantes() > 0 && !reservationsVague.isEmpty()) {
            // Chercher la meilleure réservation pour remplir les places restantes
            // Priorité 1: même destination, Priorité 2: meilleur écart
            Reservation meilleureReservation = null;
            int meilleurScore = Integer.MAX_VALUE;
            int placesRestantes = assignation.getPlacesRestantes();
            
            for (Reservation r : reservationsVague) {
                int passagers = passagersRestantsParReservation.getOrDefault(r.getId(), 0);
                if (passagers <= 0) continue;
                
                int ecart = Math.abs(passagers - placesRestantes);
                // Bonus de priorité pour la même destination (score plus bas = meilleur)
                int bonus = destinationsActuelles.contains(r.getIdLieu()) ? 0 : 1000;
                int score = ecart + bonus;
                
                if (score < meilleurScore) {
                    meilleurScore = score;
                    meilleureReservation = r;
                }
            }
            
            if (meilleureReservation == null) {
                break;
            }
            
            int passagersDisponibles = passagersRestantsParReservation.get(meilleureReservation.getId());
            int passagersAPrendre = Math.min(passagersDisponibles, placesRestantes);
            
            // Créer une copie de la réservation avec le bon nombre de passagers
            Reservation reservationAssignee = copierReservation(meilleureReservation, passagersAPrendre);
            assignation.ajouterReservation(reservationAssignee);
            
            // Mettre à jour les passagers restants
            int nouveauRestant = passagersDisponibles - passagersAPrendre;
            passagersRestantsParReservation.put(meilleureReservation.getId(), nouveauRestant);
            
            if (nouveauRestant <= 0) {
                reservationsVague.remove(meilleureReservation);
            }
        }
    }

    /**
     * Trouve la plus grande voiture disponible, même si elle ne peut pas prendre tous les passagers.
     * Utilisée pour forcer le split des réservations quand aucune voiture n'est assez grande.
     */
    private Voiture trouverPlusGrandeVoitureDisponible(List<Voiture> voitures, Map<Integer, Integer> compteurTrajetsJour,
                                                        Map<Integer, Timestamp> voituresEnTrajet, 
                                                        Timestamp finFenetreVague, java.sql.Date dateSimulation) {
        Voiture plusGrande = null;
        int capaciteMax = 0;
        
        for (Voiture v : voitures) {
            // Vérifier si la voiture est disponible
            Timestamp disponibiliteReelle = UtilSimulation.combinerDateEtHeure(dateSimulation, v.getDisponibilite());
            if (voituresEnTrajet.containsKey(v.getIdVoiture())) {
                Timestamp heureRetour = voituresEnTrajet.get(v.getIdVoiture());
                if (heureRetour != null && heureRetour.after(disponibiliteReelle)) {
                    disponibiliteReelle = heureRetour;
                }
            }
            
            // La voiture doit être disponible avant ou pendant la fenêtre
            if (disponibiliteReelle.after(finFenetreVague)) {
                continue;
            }
            
            // Chercher la plus grande capacité
            if (v.getCapacite() > capaciteMax) {
                capaciteMax = v.getCapacite();
                plusGrande = v;
            }
        }
        
        return plusGrande;
    }

    // ========================================
    // SPRINT 8: FEATURE 2 - Return Trip Assignment
    // ========================================
    
    /**
     * SPRINT 8 - Feature 2
     * Traite les retours de véhicules et assigne les réservations en attente.
     */
    public SimulationAssignation traiterRetourVehicule(
            List<Reservation> nonAssignees,
            Set<Integer> assignees,
            Timestamp dateHeureFin,
            Timestamp dateHeureProchain,
            Date dateSimulation,
            List<Voiture> toutesVoitures,
            Map<Integer, Timestamp> voituresEnTrajet,
            Map<Integer, Integer> compteurTrajetsJour,
            double vitesseMoyenne,
            Lieu aeroport,
            int tempsAttenteMinutes) {
        
        if (nonAssignees == null || nonAssignees.isEmpty()) {
            return null;
        }
        
        nonAssignees.sort((r1, r2) -> {
            int comparePassagers = Integer.compare(r2.getNbPassager(), r1.getNbPassager());
            if (comparePassagers != 0) return comparePassagers;
            return r1.getDateHeure().compareTo(r2.getDateHeure());
        });
        
        List<VoitureService.VoitureAvecCapacite> vehiculesDisponibles = 
            voitureService.getPremiersVehicules(voituresEnTrajet, dateHeureFin, dateHeureProchain);
        
        if (vehiculesDisponibles == null || vehiculesDisponibles.isEmpty()) {
            return null;
        }
        
        for (Reservation reservation : nonAssignees) {
            if (assignees.contains(reservation.getId())) continue;
            
            Voiture vehicule = voitureService.getRetourVehicule(
                vehiculesDisponibles, reservation.getNbPassager(), compteurTrajetsJour);
            
            if (vehicule == null) continue;
            
            Timestamp heureRetourVehicule = voituresEnTrajet.get(vehicule.getIdVoiture());
            if (heureRetourVehicule == null) continue;
            
            long finFenetreMs = heureRetourVehicule.getTime() + (tempsAttenteMinutes * 60L * 1000L);
            Timestamp finFenetreVague = new Timestamp(finFenetreMs);
            
            SimulationAssignation simulationAssignation = new SimulationAssignation(vehicule, heureRetourVehicule);
            simulationAssignation.setDebutVague(heureRetourVehicule);
            simulationAssignation.setFinFenetreVague(finFenetreVague);
            
            int passagersAPrendre = Math.min(reservation.getNbPassager(), vehicule.getCapacite());
            Reservation reservationAssignee = copierReservation(reservation, passagersAPrendre);
            simulationAssignation.ajouterReservation(reservationAssignee);
            assignees.add(reservation.getId());
            
            System.out.println(" [RETOUR] Véhicule " + vehicule.getRef() + " revient à " + 
                heureRetourVehicule + " → Prend " + reservation.getIdClient() + 
                " (" + passagersAPrendre + " passagers)");
            
            // Préparer la map des passagers restants pour remplissageOptimal
            // Inclure toutes les réservations non assignées (y compris celle qu'on vient d'assigner)
            Map<Integer, Integer> passagersRestants = new HashMap<>();
            for (Reservation r : nonAssignees) {
                if (r.getId() == reservation.getId()) {
                    // Pour la réservation qu'on vient d'assigner, mettre les passagers restants
                    int restants = reservation.getNbPassager() - passagersAPrendre;
                    if (restants > 0) {
                        passagersRestants.put(r.getId(), restants);
                    }
                } else if (!assignees.contains(r.getId())) {
                    // Pour les autres, mettre tous les passagers
                    passagersRestants.put(r.getId(), r.getNbPassager());
                }
            }
            
            // Remplir le véhicule avec d'autres réservations si places disponibles
            remplissageOptimal(simulationAssignation, nonAssignees, passagersRestants);
            
            for (Reservation r : simulationAssignation.getReservations()) {
                assignees.add(r.getId());
            }
            
            Timestamp heureDepart;
            if (simulationAssignation.getPlacesRestantes() == 0) {
                heureDepart = heureRetourVehicule;
                System.out.println(" [RETOUR] Véhicule PLEIN → Départ immédiat à " + heureDepart);
            } else {
                heureDepart = finFenetreVague;
                System.out.println(" [RETOUR] Véhicule pas plein (" + 
                    simulationAssignation.getPlacesRestantes() + " places vides) → Départ à " + heureDepart);
            }
            
            UtilSimulation.calculerItineraire(simulationAssignation, aeroport, heureDepart, 
                finFenetreVague, distanceDAO, lieuDAO, vitesseMoyenne);
            
            int trajetsActuels = compteurTrajetsJour.getOrDefault(vehicule.getIdVoiture(), 0);
            compteurTrajetsJour.put(vehicule.getIdVoiture(), trajetsActuels + 1);
            
            setHeureRetour(simulationAssignation, voituresEnTrajet, vehicule, aeroport, vitesseMoyenne);
            
            for (VoitureService.VoitureAvecCapacite v : vehiculesDisponibles) {
                if (v.voiture.getIdVoiture() == vehicule.getIdVoiture()) {
                    v.capaciteRestante = simulationAssignation.getPlacesRestantes();
                    break;
                }
            }
            
            return simulationAssignation;
        }
        
        return null;
    }
}
