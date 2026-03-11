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
    private HotelDAO hotelDAO = new HotelDAO();
    private ParametreDAO parametreDAO = new ParametreDAO();

    /**
     * Simule l'assignation des voitures pour une date donnée
     * SANS INSERTION EN BASE - Juste simulation
     */
    public ResultatSimulation simulerAssignation(Date dateSimulation) {
        ResultatSimulation resultat = new ResultatSimulation(dateSimulation);

        System.out.println("\n========================================");
        System.out.println(" SIMULATION D'ASSIGNATION");
        System.out.println("Date: " + dateSimulation);
        System.out.println("========================================\n");

        // Charger les paramètres 
        double vitesseMoyenne = parametreDAO.getVitesseMoyenne();
        int aeroportId = parametreDAO.getAeroportId();
        Hotel aeroport = hotelDAO.findById(aeroportId);
 
        if (aeroport == null) {
            System.out.println(" Aéroport non configuré (paramètre AEROPORT_HOTEL_ID manquant)");
            return resultat;
        }
        System.out.println("fahhh");

        System.out.println("Aéroport: " + aeroport.getNom() + " (ID: " + aeroportId + ")");
        System.out.println("Vitesse Moyenne: " + vitesseMoyenne + " km/h\n");

        // 1. Récupérer toutes les réservations de cette date 
        List<Reservation> reservations = reservationDAO.findByDate(dateSimulation);
        
        System.out.println(reservations.size());
        if (reservations.isEmpty()) {
            System.out.println(" Aucune réservation trouvée pour cette date");
            return resultat;
        }

        System.out.println(" " + reservations.size() + " réservation(s) trouvée(s)\n");

        // 2. Regrouper les réservations par vague (même heure)
        Map<Timestamp, List<Reservation>> vagues = regrouperParVague(reservations);
        resultat.setNbVagues(vagues.size());

        System.out.println(" " + vagues.size() + " vague(s) de traitement\n");

        // 3. Récupérer toutes les voitures disponibles
        List<Voiture> toutesVoitures = voitureDAO.findAll();
        Set<Integer> voituresUtilisees = new HashSet<>();
        Map<Integer, Timestamp> heuresRetourVoitures = new HashMap<>();

        // 4. Traiter chaque vague
        int numeroVague = 1;
        for (Map.Entry<Timestamp, List<Reservation>> entry : vagues.entrySet()) {
            Timestamp heureVague = entry.getKey();
            List<Reservation> reservationsVague = entry.getValue();

            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println(" VAGUE #" + numeroVague + " - " + heureVague);
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // Libérer les voitures revenues à l'aéroport avant cette vague
            voituresUtilisees.removeIf(idVoiture -> {
                Timestamp heureRetour = heuresRetourVoitures.get(idVoiture);
                if (heureRetour != null && !heureRetour.after(heureVague)) {
                    System.out.println("🔄 Voiture #" + idVoiture + " revenue à " + heureRetour + " → disponible");
                    return true;
                }
                return false;
            });

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
                System.out.println("\n Réservation #" + reservation.getId() + 
                                   " - " + reservation.getNbPassager() + " passagers - Hotel #" + 
                                   reservation.getIdHotel());

                SimulationAssignation assignation = trouverMeilleureVoiture(
                    reservation, 
                    voituresDisponibles, 
                    assignationsVague,
                    heureVague
                );

                if (assignation != null) {
                    System.out.println("    Assignée à Voiture #" + assignation.getVoiture().getIdVoiture() + 
                                       " (" + assignation.getVoiture().getRef() + ") - " +
                                       assignation.getPlacesRestantes() + " places restantes");
                } else {
                    System.out.println("    Aucune voiture disponible");
                    resultat.ajouterReservationNonAssignee(reservation);
                }
            }

            // Calculer l'itinéraire pour chaque assignation de cette vague
            for (SimulationAssignation assignation : assignationsVague.values()) {
                calculerItineraire(assignation, aeroport, vitesseMoyenne);
                resultat.ajouterAssignation(assignation);
                voituresUtilisees.add(assignation.getVoiture().getIdVoiture());
                heuresRetourVoitures.put(assignation.getVoiture().getIdVoiture(), assignation.getHeureRetour());
            }

            numeroVague++;
            System.out.println();
        }

        return resultat;
    }

    /**
     * Calcule l'itinéraire optimal pour une voiture avec ses réservations.
     * Algorithme du plus proche voisin :
     *   Aéroport → hôtel le plus proche → hôtel suivant le plus proche → ... → Aéroport
     * En cas d'égalité de distance : ordre alphabétique du nom de l'hôtel.
     */
    private void calculerItineraire(SimulationAssignation assignation, Hotel aeroport, double vitesseMoyenne) {
        // Récupérer les hôtels uniques des réservations
        Set<Integer> hotelIdsVus = new LinkedHashSet<>();
        for (Reservation r : assignation.getReservations()) {
            hotelIdsVus.add(r.getIdHotel());
        }

        // Charger les objets Hotel
        List<Hotel> hotelsAVisiter = new ArrayList<>();
        for (int hotelId : hotelIdsVus) {
            Hotel hotel = hotelDAO.findById(hotelId);
            if (hotel != null) {
                hotelsAVisiter.add(hotel);
            }
        }

        if (hotelsAVisiter.isEmpty()) {
            return;
        }

        // Algorithme du plus proche voisin depuis l'aéroport
        List<Hotel> ordreVisite = new ArrayList<>();
        List<Hotel> restants = new ArrayList<>(hotelsAVisiter);
        Hotel positionActuelle = aeroport;

        while (!restants.isEmpty()) {
            Hotel plusProche = trouverPlusProche(positionActuelle, restants);
            ordreVisite.add(plusProche);
            restants.remove(plusProche);
            positionActuelle = plusProche;
        }

        // Construire les étapes avec les horaires
        List<EtapeItineraire> itineraire = new ArrayList<>();
        double distanceTotale = 0;
        Timestamp heureActuelle = assignation.getHeureVague();
        assignation.setHeureDepart(heureActuelle);

        Hotel depart = aeroport;
        for (Hotel destination : ordreVisite) {
            Double distanceKm = distanceDAO.getDistance(depart.getId(), destination.getId());
            if (distanceKm == null) {
                distanceKm = 0.0;
            }

            Timestamp heureDepart = heureActuelle;
            // Temps en millisecondes = (distance / vitesse) * 3600 * 1000
            long tempsTrajetMs = (long) ((distanceKm / vitesseMoyenne) * 3600.0 * 1000.0);
            Timestamp heureArrivee = new Timestamp(heureDepart.getTime() + tempsTrajetMs);

            itineraire.add(new EtapeItineraire(depart, destination, distanceKm, heureDepart, heureArrivee));
            distanceTotale += distanceKm;

            heureActuelle = heureArrivee;
            depart = destination;
        }

        // Retour à l'aéroport
        Double distanceRetour = distanceDAO.getDistance(depart.getId(), aeroport.getId());
        if (distanceRetour == null) {
            distanceRetour = 0.0;
        }

        Timestamp heureDepartRetour = heureActuelle;
        long tempsRetourMs = (long) ((distanceRetour / vitesseMoyenne) * 3600.0 * 1000.0);
        Timestamp heureArriveeRetour = new Timestamp(heureDepartRetour.getTime() + tempsRetourMs);

        itineraire.add(new EtapeItineraire(depart, aeroport, distanceRetour, heureDepartRetour, heureArriveeRetour));
        distanceTotale += distanceRetour;

        assignation.setItineraire(itineraire);
        assignation.setDistanceTotale(distanceTotale);
        assignation.setHeureRetour(heureArriveeRetour);

        // Log
        System.out.println("\n   🗺️ Itinéraire pour Voiture " + assignation.getVoiture().getRef() + " :");
        for (EtapeItineraire etape : itineraire) {
            System.out.println("      " + etape);
        }
        System.out.println("      📏 Distance totale: " + String.format("%.2f", distanceTotale) + " km");
        System.out.println("      ⏰ Départ: " + assignation.getHeureDepart() + " → Retour: " + heureArriveeRetour);
    }

    /**
     * Trouve l'hôtel le plus proche de la position actuelle.
     * En cas d'égalité de distance : ordre alphabétique du nom.
     */
    private Hotel trouverPlusProche(Hotel positionActuelle, List<Hotel> candidats) {
        Hotel plusProche = null;
        double distanceMin = Double.MAX_VALUE;

        for (Hotel candidat : candidats) {
            Double distance = distanceDAO.getDistance(positionActuelle.getId(), candidat.getId());
            if (distance == null) {
                distance = Double.MAX_VALUE;
            }

            if (distance < distanceMin) {
                distanceMin = distance;
                plusProche = candidat;
            } else if (distance == distanceMin && plusProche != null) {
                // Égalité : ordre alphabétique
                if (candidat.getNom().compareToIgnoreCase(plusProche.getNom()) < 0) {
                    plusProche = candidat;
                }
            }
        }

        return plusProche;
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

        // 2. Chercher une nouvelle voiture (exclure celles déjà utilisées dans cette vague)
        List<Voiture> voituresCompatibles = voituresDisponibles.stream()
                .filter(v -> v.getCapacite() >= nbPassagers)
                .filter(v -> !assignationsVague.containsKey(v.getIdVoiture()))
                .collect(Collectors.toList());

        if (voituresCompatibles.isEmpty()) {
            System.out.println("   → Aucune voiture compatible disponible");
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
            System.out.println("   → Écart minimal: " + ecartMin + " | Priorité Diesel");
            voitureChoisie = voituresDiesel.size() == 1 ? 
                            voituresDiesel.get(0) : 
                            voituresDiesel.get(new Random().nextInt(voituresDiesel.size()));
        } else {
            System.out.println("   → Écart minimal: " + ecartMin + " | Choix parmi " + voituresOptimales.size() + " voiture(s)");
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
