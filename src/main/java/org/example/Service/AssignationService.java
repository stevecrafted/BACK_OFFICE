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
    private CarburantDAO carburantDAO = new CarburantDAO();

    /**
     * Assigne automatiquement les voitures aux réservations non assignées
     * selon les règles de gestion
     */
    public List<Assignation> assignerVoituresAutomatiquement() {
        List<Assignation> nouvellesAssignations = new ArrayList<>();

        // 1. Récupérer toutes les réservations non assignées
        List<Reservation> reservationsNonAssignees = getReservationsNonAssignees();

        // 2. Trier par nbPassager décroissant (priorité aux plus grandes réservations)
        reservationsNonAssignees.sort((r1, r2) -> Integer.compare(r2.getNbPassager(), r1.getNbPassager()));

        System.out.println("========================================");
        System.out.println("ASSIGNATION AUTOMATIQUE");
        System.out.println(reservationsNonAssignees.size() + " réservation(s) à assigner");
        System.out.println("========================================");

        // 3. Pour chaque réservation, trouver la meilleure voiture
        for (Reservation reservation : reservationsNonAssignees) {
            System.out.println("\n--- Traitement Réservation #" + reservation.getId() + 
                               " (" + reservation.getNbPassager() + " passagers, Hotel #" + 
                               reservation.getIdHotel() + ") ---");

            Voiture meilleureVoiture = trouverMeilleureVoiture(reservation);

            if (meilleureVoiture != null) {
                // Créer l'assignation
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

    /**
     * Trouve la meilleure voiture pour une réservation selon les règles
     */
    private Voiture trouverMeilleureVoiture(Reservation reservation) {
        List<Voiture> voitures = voitureDAO.findAll();
        List<Voiture> voituresEligibles = new ArrayList<>();

        // 1. Filtrer les voitures avec assez de places
        for (Voiture voiture : voitures) {
            int placesDisponibles = calculerPlacesDisponibles(voiture);
            
            if (placesDisponibles >= reservation.getNbPassager()) {
                voituresEligibles.add(voiture);
                System.out.println("  Voiture #" + voiture.getIdVoiture() + " éligible (" + 
                                   placesDisponibles + " places dispo)");
            }
        }

        if (voituresEligibles.isEmpty()) {
            return null;
        }

        // 2. Si une seule voiture éligible, la retourner
        if (voituresEligibles.size() == 1) {
            return voituresEligibles.get(0);
        }

        // 3. Calculer le temps de trajet pour chaque voiture
        Map<Voiture, Double> tempsTrajet = new HashMap<>();
        
        for (Voiture voiture : voituresEligibles) {
            double temps = calculerTempsTrajetTotal(voiture, reservation);
            tempsTrajet.put(voiture, temps);
            System.out.println("  Voiture #" + voiture.getIdVoiture() + " - Temps trajet: " + 
                               String.format("%.2f", temps) + " heures");
        }

        // 4. Trouver le temps minimum
        double tempsMin = Collections.min(tempsTrajet.values());
        
        // 5. Filtrer les voitures avec le temps minimum
        List<Voiture> voituresOptimales = voituresEligibles.stream()
                .filter(v -> Math.abs(tempsTrajet.get(v) - tempsMin) < 0.01)
                .collect(Collectors.toList());

        if (voituresOptimales.size() == 1) {
            return voituresOptimales.get(0);
        }

        // 6. Prioriser les diesel
        List<Voiture> voituresDiesel = filtrerParCarburant(voituresOptimales, "Diesel");
        
        if (!voituresDiesel.isEmpty()) {
            System.out.println("  Priorité Diesel appliquée");
            if (voituresDiesel.size() == 1) {
                return voituresDiesel.get(0);
            }
            voituresOptimales = voituresDiesel;
        }

        // 7. Choix aléatoire parmi les voitures restantes
        System.out.println("  Choix aléatoire parmi " + voituresOptimales.size() + " voiture(s)");
        Random random = new Random();
        return voituresOptimales.get(random.nextInt(voituresOptimales.size()));
    }

    /**
     * Calcule le nombre de places disponibles dans une voiture
     */
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

    /**
     * Calcule le temps de trajet total pour une voiture avec une nouvelle réservation
     */
    private double calculerTempsTrajetTotal(Voiture voiture, Reservation nouvelleReservation) {
        List<Assignation> assignations = assignationDAO.findByVoiture(voiture.getIdVoiture());
        
        // Récupérer tous les hotels de destination
        List<Integer> hotelsDestination = new ArrayList<>();
        
        for (Assignation assignation : assignations) {
            Reservation res = reservationDAO.findById(assignation.getIdReservation());
            if (res != null) {
                hotelsDestination.add(res.getIdHotel());
            }
        }
        
        // Ajouter le nouvel hotel
        hotelsDestination.add(nouvelleReservation.getIdHotel());
        
        // Si un seul hotel, pas de trajet
        if (hotelsDestination.size() == 1) {
            return 0.0;
        }
        
        // Calculer la distance totale (trajet optimal simplifié)
        double distanceTotale = calculerDistanceTotale(hotelsDestination);
        
        // Temps = Distance / Vitesse Moyenne (VM = 60 km/h par défaut)
        double vitesseMoyenne = 60.0; // TODO: récupérer depuis parametre
        
        return distanceTotale / vitesseMoyenne;
    }

    /**
     * Calcule la distance totale pour visiter tous les hotels
     * (Algorithme simplifié: somme des distances entre hotels consécutifs)
     */
    private double calculerDistanceTotale(List<Integer> hotels) {
        if (hotels.size() <= 1) {
            return 0.0;
        }
        
        double distanceTotale = 0.0;
        
        // Calculer toutes les distances entre paires d'hotels
        for (int i = 0; i < hotels.size(); i++) {
            for (int j = i + 1; j < hotels.size(); j++) {
                Double distance = distanceDAO.getDistance(hotels.get(i), hotels.get(j));
                if (distance != null) {
                    distanceTotale += distance;
                }
            }
        }
        
        return distanceTotale;
    }

    /**
     * Filtre les voitures par type de carburant
     */
    private List<Voiture> filtrerParCarburant(List<Voiture> voitures, String libelleCarburant) {
        return voitures.stream()
                .filter(v -> {
                    Carburant carburant = v.getCarburant();
                    return carburant != null && carburant.getLibelle().equalsIgnoreCase(libelleCarburant);
                })
                .collect(Collectors.toList());
    }

    /**
     * Récupère toutes les réservations non assignées
     */
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
