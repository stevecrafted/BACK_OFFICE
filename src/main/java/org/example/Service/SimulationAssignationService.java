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

        // 2. Récupérer les assignations existantes en base et exclure ces réservations
        List<Assignation> assignationsExistantes = assignationDAO.findAll();
        Set<Integer> reservationsDejaAssignees = new HashSet<>();

        // 3. Récupérer toutes les voitures et la vitesse moyenne
        List<Voiture> toutesVoitures = voitureDAO.findAll();
        double vitesseMoyenne = parametreDAO.getVM();
        Lieu aeroport = lieuDAO.findAeroport();

        // ---
        // Etape 1 : Exclure les reservations deja assigné en base
        // Collecter les IDs des réservations déjà assignées
        for (int i = 0; i < assignationsExistantes.size(); i++) {

            for (int j = 0; j < assignationsExistantes.get(i).getReservationAssignations().size(); j++) {
                reservationsDejaAssignees
                        .add(assignationsExistantes.get(i).getReservationAssignations().get(j).getIdReservation());
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
            Timestamp debutVague = reservationsATraiter.get(0).getDateHeure();
            long debutVagueMs = UtilSimulation.tronquerAuxMinutes(debutVague);
            long finFenetreMaxMs = debutVagueMs + (tempsAttenteMinutes * 60L * 1000L);

            // Collecter toutes les réservations dans la fenêtre [debut, finFenetreMax]
            List<Reservation> reservationsVague = new ArrayList<>();
            for (Reservation r : reservationsATraiter) {
                if (r.getDateHeure().getTime() <= finFenetreMaxMs) {
                    reservationsVague.add(r);
                }
            }

            // Retirer les réservations de cette vague de la liste globale
            reservationsATraiter.removeAll(reservationsVague);

            // Calculer la fin réelle de la vague = min(finFenetreMax, dernièreReservation)
            Timestamp derniereReservation = reservationsVague.get(reservationsVague.size() - 1).getDateHeure();
            long finFenetreMs = Math.min(finFenetreMaxMs, derniereReservation.getTime());
            Timestamp finFenetreVague = new Timestamp(finFenetreMs);

            String cleVague = String.format("%tF %tH:%tM", debutVague, debutVague, debutVague);

            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println(" VAGUE #" + numeroVague + " - " + cleVague);
            System.out.println(" " + reservationsVague.size() + " réservation(s)");
            System.out.println(" Fin fenêtre: " + finFenetreVague);
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // Liste des assignations de cette vague (pour la réassignation)
            List<SimulationAssignation> assignationsVague = new ArrayList<>();

            // Traiter les réservations de cette vague
            while (!reservationsVague.isEmpty()) {
                // Trouver la réservation avec le plus de passagers
                Reservation reservation = UtilSimulation.trouverReservationATraiter(reservationsVague);

                if (reservation == null) {
                    break;
                }

                // Trouver la meilleure voiture parmi toutes les voitures
                // trouverMeilleurVoiture filtre celles qui ne sont pas disponibles via voituresEnTrajet
                Voiture voiture = UtilSimulation.trouverMeilleurVoiture(reservation, toutesVoitures,
                        compteurTrajetsJour, voituresEnTrajet, finFenetreVague);

                if (voiture == null) {
                    System.out.println(" Aucune voiture disponible pour réservation #" + reservation.getId());
                    reservationsVague.remove(reservation);
                    continue;
                }

                // Créer l'assignation
                SimulationAssignation simulationAssignation = new SimulationAssignation(voiture,
                        reservation.getDateHeure());
                simulationAssignation.setDebutVague(debutVague);
                simulationAssignation.ajouterReservation(reservation);
                reservationsVague.remove(reservation);

                // Assignation optimal des places pour cette vague
                UtilSimulation.AssignationOptimal(simulationAssignation, reservationsVague);

                // Calculer l'heure de départ:
                // - Si voiture pleine → part immédiatement (dernière réservation ajoutée)
                // - Sinon → attend jusqu'à finFenetreVague
                Timestamp heureDepart;
                if (simulationAssignation.getPlacesRestantes() == 0) {
                    // Voiture pleine: part à l'heure de la dernière réservation
                    List<Reservation> reservationsAssignees = simulationAssignation.getReservations();
                    heureDepart = reservationsAssignees.get(reservationsAssignees.size() - 1).getDateHeure();
                } else {
                    // Voiture non pleine: attend jusqu'à la fin de la fenêtre
                    heureDepart = finFenetreVague;
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
                    Timestamp disponibiliteReelle = Timestamp.valueOf(candidat.getDisponibilite().toString());
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

            numeroVague++;
            System.out.println();
        }

        return resultat;
    }

    public void setHeureRetour(SimulationAssignation simulationAssignation, Map<Integer, Timestamp> voituresEnTrajet, Voiture voiture, Lieu aeroport, double vitesseMoyenne) {
        // heureRetour = heureArrivee + temps retour (dernier hôtel → aéroport)
        Timestamp heureArrivee = simulationAssignation.getDateHeureArrivee();
        if (heureArrivee != null && !simulationAssignation.getReservations().isEmpty()) {
            // Récupérer le dernier hôtel de l'itinéraire
            List<Reservation> reservationss = simulationAssignation.getReservations();
            int dernierHotelId = reservationss.get(reservationss.size() - 1).getIdLieu();

            // Calculer la distance retour (dernier hôtel → aéroport)
            Double distanceRetour = distanceDAO.getDistance(dernierHotelId, aeroport.getId());
            if (distanceRetour == null)
                distanceRetour = 0.0;
            distanceRetour = distanceRetour * 2; // Aller retour donc * 2

            // Durée retour en minutes
            double dureeRetourMinutes = (distanceRetour / vitesseMoyenne) * 60.0;
            long dureeRetourMs = (long) (dureeRetourMinutes * 60 * 1000);

            Timestamp heureRetour = new Timestamp(heureArrivee.getTime() + dureeRetourMs);
            voituresEnTrajet.put(voiture.getIdVoiture(), heureRetour);
        }
    }

}
