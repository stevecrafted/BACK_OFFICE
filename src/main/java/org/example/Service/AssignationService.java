package org.example.Service;

import org.example.DAO.*;
import org.example.Model.*;

import java.util.*;
import java.util.stream.Collectors;

public class AssignationService {

    private ReservationDAO reservationDAO = new ReservationDAO();
    private VoitureDAO voitureDAO = new VoitureDAO();
    private AssignationDAO assignationDAO = new AssignationDAO();
    private DistanceDAO distanceDAO = new DistanceDAO();

    /**
     * Assigne automatiquement les voitures aux réservations non assignées
     */
    public List<Assignation> assignerVoituresAutomatiquement() {
        List<Assignation> nouvellesAssignations = new ArrayList<>();

        List<Reservation> reservationsNonAssignees = getReservationsNonAssignees();
        reservationsNonAssignees.sort((r1, r2) -> Integer.compare(r2.getNbPassager(), r1.getNbPassager()));

        System.out.println("========================================");
        System.out.println("ASSIGNATION AUTOMATIQUE");
        System.out.println(reservationsNonAssignees.size() + " réservation(s) à assigner");
        System.out.println("========================================");

        for (Reservation reservation : reservationsNonAssignees) {
            System.out.println("\n--- Traitement Réservation #" + reservation.getId() + 
                               " (" + reservation.getNbPassager() + " passagers, Lieu #" + 
                               reservation.getIdLieu() + ") ---");

            Voiture meilleureVoiture = trouverMeilleureVoiture(reservation);

            if (meilleureVoiture != null) {
                Assignation assignation = new Assignation(meilleureVoiture.getIdVoiture(), reservation.getId());
                
                if (assignationDAO.create(assignation)) {
                    nouvellesAssignations.add(assignation);
                    System.out.println("✅ Assignée à Voiture #" + meilleureVoiture.getIdVoiture() + 
                                       " (" + meilleureVoiture.getRef() + ")");
                }
            } else {
                System.out.println("❌ Aucune voiture disponible pour cette réservation");
            }
        }

        System.out.println("\n========================================");
        System.out.println("RÉSULTAT: " + nouvellesAssignations.size() + " assignation(s) créée(s)");
        System.out.println("========================================");

        return nouvellesAssignations;
    }

    private Voiture trouverMeilleureVoiture(Reservation reservation) {
        List<Voiture> voitures = voitureDAO.findAll();
        List<Voiture> voituresEligibles = new ArrayList<>();

        for (Voiture voiture : voitures) {
            int placesDisponibles = calculerPlacesDisponibles(voiture);
            
            if (placesDisponibles >= reservation.getNbPassager()) {
                voituresEligibles.add(voiture);
            }
        }

        if (voituresEligibles.isEmpty()) return null;
        if (voituresEligibles.size() == 1) return voituresEligibles.get(0);

        // Trouver l'écart minimum
        int nbPassagers = reservation.getNbPassager();
        int ecartMin = voituresEligibles.stream()
                .mapToInt(v -> v.getCapacite() - nbPassagers)
                .min().orElse(0);

        List<Voiture> voituresOptimales = voituresEligibles.stream()
                .filter(v -> (v.getCapacite() - nbPassagers) == ecartMin)
                .collect(Collectors.toList());

        if (voituresOptimales.size() == 1) return voituresOptimales.get(0);

        // Priorité D > H > E
        List<Voiture> voituresDiesel = voituresOptimales.stream()
                .filter(v -> "D".equals(v.getCarburant()))
                .collect(Collectors.toList());
        if (!voituresDiesel.isEmpty()) return voituresDiesel.get(0);

        List<Voiture> voituresHybride = voituresOptimales.stream()
                .filter(v -> "H".equals(v.getCarburant()))
                .collect(Collectors.toList());
        if (!voituresHybride.isEmpty()) return voituresHybride.get(0);

        return voituresOptimales.get(new Random().nextInt(voituresOptimales.size()));
    }

    private int calculerPlacesDisponibles(Voiture voiture) {
        List<Assignation> assignations = assignationDAO.findByVoiture(voiture.getIdVoiture());
        int placesOccupees = 0;
        for (Assignation assignation : assignations) {
            Reservation res = reservationDAO.findById(assignation.getIdReservation());
            if (res != null) {
                placesOccupees += res.getNbPassager();
            }
        }
        return voiture.getCapacite() - placesOccupees;
    }

    private List<Reservation> getReservationsNonAssignees() {
        List<Reservation> toutesReservations = reservationDAO.findAll();
        List<Reservation> nonAssignees = new ArrayList<>();
        
        for (Reservation reservation : toutesReservations) {
            Assignation assignation = assignationDAO.findByReservation(reservation.getId());
            if (assignation == null) {
                nonAssignees.add(reservation);
            }
        }
        
        return nonAssignees;
    }
}
