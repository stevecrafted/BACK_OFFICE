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

    // Stocke les intervalles [debutFenetre, finFenetre] de chaque vague (clé = cleVague)
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
                reservationsDejaAssignees.add(assignationsExistantes.get(i).getReservationAssignations().get(j).getIdReservation());
            }

        }

        // Filtrer pour ne garder que les réservations non assignées
        List<Reservation> reservationsATraiter = new ArrayList<>();
        for (Reservation r : reservations) {
            if (!reservationsDejaAssignees.contains(r.getId())) {
                reservationsATraiter.add(r);
            }
        }

        // ----
        // Etape 2 : Traiter Reservation
        for (int i = 0; i < reservationsATraiter.size(); i++) {

            Reservation reservation = UtilSimulation.trouverReservationATraiter(reservationsATraiter);
            Voiture voiture = UtilSimulation.trouverMeilleurVoiture(reservation, toutesVoitures, null);

            SimulationAssignation simulationAssignation = new SimulationAssignation(voiture, reservation.getDateHeure());
            simulationAssignation.ajouterReservation(reservation);

            if ( simulationAssignation.getPlacesRestantes() > 0 ) {

                // Remplissage optimal : chercher une réservation qui correspond au reste de places
                while (simulationAssignation.getPlacesRestantes() > 0) {
                    Reservation reservationRemplissage = UtilSimulation.trouverReservationPourRemplissage(
                        reservationsATraiter,
                        simulationAssignation.getPlacesRestantes()
                    );

                    if (reservationRemplissage == null) {
                        break; // Plus de réservation qui peut rentrer
                    }

                    // Ajouter la réservation à l'assignation
                    simulationAssignation.ajouterReservation(reservationRemplissage);
                    reservationsATraiter.remove(reservationRemplissage);
                }
            }

            // Supp Reservation deja traitee
            reservationsATraiter.remove(reservation);

        }


        return resultat;
    }

}

