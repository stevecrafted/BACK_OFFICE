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

        for (Assignation a : assignationsExistantes) {
            reservationsDejaAssignees.add(a.getIdReservation());
            voitureReservationsExistantes
                .computeIfAbsent(a.getIdVoiture(), k -> new ArrayList<>())
                .add(reservationDAO.findById(a.getIdReservation()));
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

        int numeroVague = 1;
        for (String cleVague : vaguesTriees) {
            List<Reservation> reservationsVague = vagues.get(cleVague);

            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("🌊 VAGUE #" + numeroVague + " - " + cleVague);
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

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

            // Récupérer l'intervalle de la vague pour l'affichage
            Timestamp[] intervalleVague = vagueIntervalles.get(cleVague);

            // Calculer les heures d'arrivée pour chaque assignation de cette vague
            // Puis vérifier que l'intervalle [départ, arrivée] ne chevauche aucun intervalle existant
            for (SimulationAssignation assignation : assignationsVague.values()) {
                if (intervalleVague != null) {
                    assignation.setDebutVague(intervalleVague[0]);
                    assignation.setFinFenetreVague(intervalleVague[1]);
                }
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
                    // Déplacer toutes les réservations vers non-assignées
                    for (Reservation r : assignation.getReservations()) {
                        resultat.ajouterReservationNonAssignee(r);
                    }
                } else {
                    resultat.ajouterAssignation(assignation);
                    // Enregistrer le nouvel intervalle occupé
                    intervallesOccupes.computeIfAbsent(idVoitureAssignee, k -> new ArrayList<>())
                        .add(new long[]{departMs, arriveeMs});
                }
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

        // 5. Priorité carburant: D (Diesel) > H (Hybride) > E (Essence)
        Voiture voitureChoisie = choisirParCarburant(voituresOptimales);

        System.out.println("   → Écart minimal: " + ecartMin + " | Carburant: " + voitureChoisie.getCarburantLibelle());

        // 6. Créer une nouvelle assignation
        SimulationAssignation assignation = new SimulationAssignation(voitureChoisie, heureVague);
        assignation.ajouterReservation(reservation);
        assignationsVague.put(voitureChoisie.getIdVoiture(), assignation);

        return assignation;
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

        List<Voiture> hybride = voitures.stream().filter(v -> "H".equals(v.getCarburant())).collect(Collectors.toList());
        if (!hybride.isEmpty()) {
            return hybride.size() == 1 ? hybride.get(0) : hybride.get(new Random().nextInt(hybride.size()));
        }

        List<Voiture> essence = voitures.stream().filter(v -> "E".equals(v.getCarburant())).collect(Collectors.toList());
        if (!essence.isEmpty()) {
            return essence.size() == 1 ? essence.get(0) : essence.get(new Random().nextInt(essence.size()));
        }

        return voitures.get(new Random().nextInt(voitures.size()));
    }

    /**
     * Calcule les heures de départ et d'arrivée pour une assignation
     * - Départ = date_heure de la réservation
     * - Route: aéroport -> lieu le plus proche -> lieu suivant -> ... -> dernier lieu -> retour aéroport
     * - Si distances similaires, ordre alphabétique du lieu
     */
    private void calculerHeuresTrajet(SimulationAssignation assignation, Lieu aeroport, double vitesseMoyenne) {
        // L'heure de départ est celle de la vague (= date_heure des réservations)
        Timestamp depart = assignation.getHeureVague();
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
     * Regroupe les réservations par vague selon le temps d'attente.
     * - La première réservation de la journée ouvre une fenêtre [t, t + temps_attente]
     * - Toutes les réservations dans cette fenêtre font partie de la même vague
     * - L'heure de départ de la vague = heure de la dernière réservation dans la fenêtre
     * - La vague suivante commence à la première réservation APRÈS la fin de la fenêtre (strictement après)
     * La clé de chaque vague est le timestamp de la dernière réservation (= heure de départ)
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
            // La première réservation ouvre la fenêtre
            Timestamp debutFenetre = triees.get(i).getDateHeure();
            // Tronquer aux minutes (ignorer les secondes)
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

            // La clé de la vague = heure de la dernière réservation (= heure de départ de la vague)
            String cleVague = String.format("%tF %tH:%tM", derniereHeure, derniereHeure, derniereHeure);
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
