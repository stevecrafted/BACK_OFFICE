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

    /**
     * Simule l'assignation des voitures pour une date donnée
     * SANS INSERTION EN BASE - Juste simulation
     */
    public ResultatSimulation simulerAssignation(Date dateSimulation) {
        ResultatSimulation resultat = new ResultatSimulation(dateSimulation);

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

        // 2. Regrouper les réservations par vague (même heure)
        Map<Timestamp, List<Reservation>> vagues = regrouperParVague(reservations);
        resultat.setNbVagues(vagues.size());

        System.out.println("🌊 " + vagues.size() + " vague(s) de traitement\n");

        // 3. Récupérer toutes les voitures disponibles
        List<Voiture> toutesVoitures = voitureDAO.findAll();
        Set<Integer> voituresUtilisees = new HashSet<>();

        // 4. Traiter chaque vague
        int numeroVague = 1;
        for (Map.Entry<Timestamp, List<Reservation>> entry : vagues.entrySet()) {
            Timestamp heureVague = entry.getKey();
            List<Reservation> reservationsVague = entry.getValue();

            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("🌊 VAGUE #" + numeroVague + " - " + heureVague);
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // Trier par nombre de passagers décroissant
            reservationsVague.sort((r1, r2) -> Integer.compare(r2.getNbPassager(), r1.getNbPassager()));

            // Voitures disponibles pour cette vague (pas encore utilisées)
            List<Voiture> voituresDisponibles = toutesVoitures.stream()
                    .filter(v -> !voituresUtilisees.contains(v.getIdVoiture()))
                    .collect(Collectors.toList());

            // Assignations temporaires pour cette vague
            Map<Integer, SimulationAssignation> assignationsVague = new HashMap<>();

            // Traiter chaque réservation de la vague
            for (Reservation reservation : reservationsVague) {
                System.out.println("\n📌 Réservation #" + reservation.getId() + 
                                   " - " + reservation.getNbPassager() + " passagers - Hotel #" + 
                                   reservation.getIdHotel());

                SimulationAssignation assignation = trouverMeilleureVoiture(
                    reservation, 
                    voituresDisponibles, 
                    assignationsVague,
                    heureVague
                );

                if (assignation != null) {
                    System.out.println("   ✅ Assignée à Voiture #" + assignation.getVoiture().getIdVoiture() + 
                                       " (" + assignation.getVoiture().getRef() + ") - " +
                                       assignation.getPlacesRestantes() + " places restantes");
                } else {
                    System.out.println("   ❌ Aucune voiture disponible");
                    resultat.ajouterReservationNonAssignee(reservation);
                }
            }

            // Ajouter les assignations de cette vague au résultat
            for (SimulationAssignation assignation : assignationsVague.values()) {
                resultat.ajouterAssignation(assignation);
                voituresUtilisees.add(assignation.getVoiture().getIdVoiture());
            }

            numeroVague++;
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
     */
    private SimulationAssignation trouverMeilleureVoiture(
            Reservation reservation,
            List<Voiture> voituresDisponibles,
            Map<Integer, SimulationAssignation> assignationsVague,
            Timestamp heureVague) {

        int nbPassagers = reservation.getNbPassager();

        // 1. Priorité: Voitures déjà assignées avec places disponibles
        for (SimulationAssignation assignation : assignationsVague.values()) {
            if (assignation.peutAccueillir(nbPassagers)) {
                assignation.ajouterReservation(reservation);
                System.out.println("   → Ajoutée à voiture existante (remplissage optimal)");
                return assignation;
            }
        }

        // 2. Chercher une nouvelle voiture
        List<Voiture> voituresCompatibles = voituresDisponibles.stream()
                .filter(v -> v.getCapacite() >= nbPassagers)
                .collect(Collectors.toList());

        if (voituresCompatibles.isEmpty()) {
            return null;
        }

        // 3. Calculer l'écart pour chaque voiture
        Map<Voiture, Integer> ecarts = new HashMap<>();
        for (Voiture voiture : voituresCompatibles) {
            int ecart = Math.abs(voiture.getCapacite() - nbPassagers);
            ecarts.put(voiture, ecart);
        }

        // 4. Trouver l'écart minimum
        int ecartMin = Collections.min(ecarts.values());

        // 5. Filtrer les voitures avec l'écart minimum
        List<Voiture> voituresOptimales = voituresCompatibles.stream()
                .filter(v -> ecarts.get(v) == ecartMin)
                .collect(Collectors.toList());

        // 6. Si plusieurs voitures, privilégier diesel
        List<Voiture> voituresDiesel = voituresOptimales.stream()
                .filter(v -> v.getCarburant() != null && 
                            "Diesel".equalsIgnoreCase(v.getCarburant().getLibelle()))
                .collect(Collectors.toList());

        Voiture voitureChoisie;
        if (!voituresDiesel.isEmpty()) {
            System.out.println("   → Priorité Diesel appliquée");
            voitureChoisie = voituresDiesel.size() == 1 ? 
                            voituresDiesel.get(0) : 
                            voituresDiesel.get(new Random().nextInt(voituresDiesel.size()));
        } else {
            System.out.println("   → Choix parmi " + voituresOptimales.size() + " voiture(s) optimale(s)");
            voitureChoisie = voituresOptimales.size() == 1 ? 
                            voituresOptimales.get(0) : 
                            voituresOptimales.get(new Random().nextInt(voituresOptimales.size()));
        }

        // 7. Créer une nouvelle assignation
        SimulationAssignation assignation = new SimulationAssignation(voitureChoisie, heureVague);
        assignation.ajouterReservation(reservation);
        assignationsVague.put(voitureChoisie.getIdVoiture(), assignation);

        return assignation;
    }

    /**
     * Regroupe les réservations par vague (même heure)
     */
    private Map<Timestamp, List<Reservation>> regrouperParVague(List<Reservation> reservations) {
        Map<Timestamp, List<Reservation>> vagues = new TreeMap<>();

        for (Reservation reservation : reservations) {
            Timestamp heure = reservation.getDateHeure();
            vagues.computeIfAbsent(heure, k -> new ArrayList<>()).add(reservation);
        }

        return vagues;
    }
}
