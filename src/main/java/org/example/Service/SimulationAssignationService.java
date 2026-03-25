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

              // List de tous les Reservations
        // Filtrer les reservations qui ne sont pas encore assigné en base.

        // Pour chaque reservation obtenir celui qui a le plus de place
            // Pour cet reservation :
                // Obtenir la voiture qui correspond aux nommbre de reservation (place)
                    // Trouver meilleure voiture
                        /*
                            -- Nombre de trajet pour ce jour
                            -- Diesel / Essence
                        */

            // Pour la prochaine itération, la priorité est de completer cet voiture par la reservation qui correpond le plus au reste de place.
                    // Ex : Reste place pour la voiture = 3
                    /*
                        - Il y a une reservation R1 de 2 place, r2 de 1 place, r3 de 3 place,
                        on prend R3 et on met dans la voiture.

                        - Il y a une reservation r1 de 4 place et R5 de 5 place,
                        on prend r1 car elle est plus proche de 3

                        Donc on choisi toujours celui qui est plus proche du reste de place
                    */
        

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
        // Etape 2 : Regrouper les réservations par vague
        int tempsAttenteMinutes = parametreDAO.getTempsAttente();
        Map<String, List<Reservation>> vagues = UtilSimulation.regrouperParVague(reservationsATraiter,
                tempsAttenteMinutes);

        // Compteur de trajets par voiture (idVoiture -> nombre de trajets)
        Map<Integer, Integer> compteurTrajetsJour = new HashMap<>();

        // Heure de retour de chaque voiture (idVoiture -> heureRetour à l'aéroport)
        // Une voiture n'est disponible que si finFenetreVague >= heureRetour
        Map<Integer, Timestamp> voituresEnTrajet = new HashMap<>();

        // ---
        // Etape 3 : Traiter chaque vague
        int numeroVague = 1;
        for (Map.Entry<String, List<Reservation>> entry : vagues.entrySet()) {
            String cleVague = entry.getKey();
            List<Reservation> reservationsVague = new ArrayList<>(entry.getValue());

            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println(" VAGUE #" + numeroVague + " - " + cleVague);
            System.out.println(" " + reservationsVague.size() + " réservation(s)");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // Calculer la fenêtre de cette vague
            Timestamp debutVague = reservationsVague.get(0).getDateHeure();
            long finFenetreMs = UtilSimulation.tronquerAuxMinutes(debutVague) + (tempsAttenteMinutes * 60L * 1000L);
            Timestamp finFenetreVague = new Timestamp(finFenetreMs);

            // Liste des voitures disponibles pour cette vague
            // Exclure les voitures encore en trajet (heureRetour > finFenetreVague)
            List<Voiture> voituresDisponiblesVague = new ArrayList<>();
            for (Voiture v : toutesVoitures) {
                Timestamp heureRetour = voituresEnTrajet.get(v.getIdVoiture());
                // Disponible si pas en trajet OU si retournée avant la fin de la fenêtre
                if (heureRetour == null || !heureRetour.after(finFenetreVague)) {
                    voituresDisponiblesVague.add(v);
                }
            }

            // Traiter les réservations de cette vague
            while (!reservationsVague.isEmpty()) {
                // Trouver la réservation avec le plus de passagers
                Reservation reservation = UtilSimulation.trouverReservationATraiter(reservationsVague);

                if (reservation == null) {
                    break;
                }

                // Trouver la meilleure voiture parmi celles encore disponibles
                Voiture voiture = UtilSimulation.trouverMeilleurVoiture(reservation, voituresDisponiblesVague,
                        compteurTrajetsJour);

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

                // Retirer la voiture de la liste des disponibles (elle est maintenant utilisée
                // dans cette vague)
                voituresDisponiblesVague.remove(voiture);

                // Assignation optimal des places pour cette vague
                UtilSimulation.AssignationOptimal(simulationAssignation, reservationsVague);

                // Calculer l'itinéraire et les heures de trajet
                UtilSimulation.calculerItineraire(simulationAssignation, aeroport, finFenetreVague,
                        distanceDAO, lieuDAO, vitesseMoyenne);

                // Ajouter au résultat
                resultat.ajouterAssignation(simulationAssignation);

                // Incrémenter le compteur de trajets pour cette voiture
                int trajetsActuels = compteurTrajetsJour.getOrDefault(voiture.getIdVoiture(), 0);
                compteurTrajetsJour.put(voiture.getIdVoiture(), trajetsActuels + 1);

                // Calculer l'heure de retour à l'aéroport
                setHeureRetour(simulationAssignation, voituresEnTrajet, voiture, aeroport, vitesseMoyenne);
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
