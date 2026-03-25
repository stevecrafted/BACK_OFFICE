package org.example.Util;

import org.example.DAO.DistanceDAO;
import org.example.DAO.LieuDAO;
import org.example.Model.*;

import java.sql.Timestamp;
import java.util.*;

public class UtilSimulation {

    /**
     * Trouve la réservation avec le plus grand nombre de passagers
     */
    public static Reservation trouverReservationATraiter(List<Reservation> reservationsATraiter) {
        if (reservationsATraiter == null || reservationsATraiter.isEmpty()) {
            return null;
        }

        Reservation reservationMax = reservationsATraiter.get(0);
        for (int i = 1; i < reservationsATraiter.size(); i++) {
            if (reservationsATraiter.get(i).getNbPassager() > reservationMax.getNbPassager()) {
                reservationMax = reservationsATraiter.get(i);
            }
        }
        return reservationMax;
    }

    /**
     * Trouver meilleur voiture pour cet reservation
     * 1. Séparer en 3 listes: capacité > nbPassagers, capacité == nbPassagers,
     * capacité < nbPassagers
     * 2. Priorité: égal > (sup + inf par écart)
     * 3. Écart minimal, puis nombre de trajets min, puis D > E
     */
    public static Voiture trouverMeilleurVoiture(Reservation reservation, List<Voiture> voituresDisponibles,
            Map<Integer, Integer> compteurTrajetsJour, Map<Integer, Timestamp> voituresEnTrajet,
            Timestamp finFenetreVague) {

        if (voituresDisponibles == null || voituresDisponibles.isEmpty()) {
            return null;
        }

        int nbPassagers = reservation.getNbPassager();

        List<Voiture> listeVoitureSupCapacite = new ArrayList<>(); // capacité > nbPassagers
        List<Voiture> listeVoitureEgalCapacite = new ArrayList<>(); // capacité == nbPassagers
        List<Voiture> listeVoitureInfCapacite = new ArrayList<>(); // capacité < nbPassagers

        // Séparer les voitures en 3 listes
        for (Voiture voiture : voituresDisponibles) {

            // Calculer la disponibilité réelle (max entre disponibilité originale et heure de retour)
            Timestamp disponibiliteReelle = Timestamp.valueOf(voiture.getDisponibilite().toString());
            if (voituresEnTrajet != null && voituresEnTrajet.containsKey(voiture.getIdVoiture())) {
                Timestamp heureRetour = voituresEnTrajet.get(voiture.getIdVoiture());
                if (heureRetour != null && heureRetour.after(disponibiliteReelle)) {
                    disponibiliteReelle = heureRetour;
                }
            }

            // Vérifier si la voiture est disponible avant la fin de la fenêtre de vague
            if (disponibiliteReelle.after(finFenetreVague)) {
                continue;
            }

            if (voiture.getCapacite() == nbPassagers) {
                listeVoitureEgalCapacite.add(voiture);
            } else if (voiture.getCapacite() > nbPassagers) {
                listeVoitureSupCapacite.add(voiture);
            } else {
                listeVoitureInfCapacite.add(voiture);
            }
        }

        // Priorité ny egal
        if (listeVoitureEgalCapacite.size() > 0 && listeVoitureEgalCapacite.size() == 1) {
            return listeVoitureEgalCapacite.get(0);
        } else if (listeVoitureEgalCapacite.size() > 1) {
            Voiture voiture = trouverVoitureCarburantNbrTrajet(listeVoitureEgalCapacite, compteurTrajetsJour,
                    reservation.getNbPassager());
            return voiture;
        }

        // Traiter sup et inf ensemble (pas de priorité entre eux)
        // Fusionner les deux listes
        List<Voiture> listeCandidats = new ArrayList<>();
        listeCandidats.addAll(listeVoitureSupCapacite);
        listeCandidats.addAll(listeVoitureInfCapacite);

        if (listeCandidats.isEmpty()) {
            return null;
        }

        if (listeCandidats.size() == 1) {
            return listeCandidats.get(0);
        }

        // Calculer l'écart pour chaque voiture et trouver l'écart minimal
        int ecartMin = Integer.MAX_VALUE;
        for (Voiture v : listeCandidats) {
            int ecart = Math.abs(v.getCapacite() - nbPassagers);
            if (ecart < ecartMin) {
                ecartMin = ecart;
            }
        }

        // Filtrer les voitures avec l'écart minimal
        List<Voiture> voituresEcartMin = new ArrayList<>();
        for (Voiture v : listeCandidats) {
            if (Math.abs(v.getCapacite() - nbPassagers) == ecartMin) {
                voituresEcartMin.add(v);
            }
        }

        if (voituresEcartMin.size() == 1) {
            return voituresEcartMin.get(0);
        }

        // Si égalité d'écart, regarder le carburant et nombre de trajets
        return trouverVoitureCarburantNbrTrajet(voituresEcartMin, compteurTrajetsJour, nbPassagers);
    }

    /**
     * Départager les voitures par nombre de trajets puis carburant (D > E)
     */
    public static Voiture trouverVoitureCarburantNbrTrajet(List<Voiture> listeCandidats,
            Map<Integer, Integer> compteurTrajetsJour, Integer nbPassagers) {
        // Trouver l'écart minimal
        int ecartMin = Integer.MAX_VALUE;
        for (Voiture v : listeCandidats) {
            int ecart = Math.abs(v.getCapacite() - nbPassagers);
            if (ecart < ecartMin) {
                ecartMin = ecart;
            }
        }

        // Filtrer les voitures avec l'écart minimal
        List<Voiture> voituresEcartMin = new ArrayList<>();
        for (Voiture v : listeCandidats) {
            if (Math.abs(v.getCapacite() - nbPassagers) == ecartMin) {
                voituresEcartMin.add(v);
            }
        }

        if (voituresEcartMin.size() == 1) {
            return voituresEcartMin.get(0);
        }

        // Trouver le nombre de trajets minimum
        int trajetsMin = Integer.MAX_VALUE;
        for (Voiture v : voituresEcartMin) {
            int trajets = compteurTrajetsJour != null ? compteurTrajetsJour.getOrDefault(v.getIdVoiture(), 0) : 0;
            if (trajets < trajetsMin) {
                trajetsMin = trajets;
            }
        }

        // Filtrer les voitures avec le nombre de trajets minimum
        List<Voiture> voituresTrajetsMin = new ArrayList<>();
        for (Voiture v : voituresEcartMin) {
            int trajets = compteurTrajetsJour != null ? compteurTrajetsJour.getOrDefault(v.getIdVoiture(), 0) : 0;
            if (trajets == trajetsMin) {
                voituresTrajetsMin.add(v);
            }
        }

        if (voituresTrajetsMin.size() == 1) {
            return voituresTrajetsMin.get(0);
        }

        // Priorité carburant: D > E (pas de H)
        for (Voiture v : voituresTrajetsMin) {
            if ("D".equals(v.getCarburant())) {
                return v;
            }
        }
        for (Voiture v : voituresTrajetsMin) {
            if ("E".equals(v.getCarburant())) {
                return v;
            }
        }

        // Retourner la première si aucun critère ne départage
        return voituresTrajetsMin.isEmpty() ? null : voituresTrajetsMin.get(0);
    }

    /**
     * Trouve la réservation qui correspond le mieux au reste de places dans la
     * voiture
     * 1. D'abord chercher les égaux (nbPassagers == placesRestantes)
     * 2. Sinon, regarder l'écart minimal (sup ou inf)
     *
     * @param reservationsATraiter Liste des réservations disponibles
     * @param placesRestantes      Nombre de places restantes dans la voiture
     * @return La réservation avec l'écart minimal, ou null si liste vide
     */
    public static Reservation trouverReservationPourRemplissage(List<Reservation> reservationsATraiter,
            int placesRestantes) {
        if (reservationsATraiter == null || reservationsATraiter.isEmpty() || placesRestantes <= 0) {
            return null;
        }

        List<Reservation> listeReservationEgal = new ArrayList<>();
        List<Reservation> listeReservationSup = new ArrayList<>(); // nbPassagers > placesRestantes
        List<Reservation> listeReservationInf = new ArrayList<>(); // nbPassagers < placesRestantes

        // Séparer en 3 listes
        for (Reservation r : reservationsATraiter) {
            if (r.getNbPassager() == placesRestantes) {
                listeReservationEgal.add(r);
            } else if (r.getNbPassager() > placesRestantes) {
                listeReservationSup.add(r);
            } else {
                listeReservationInf.add(r);
            }
        }

        // Priorité aux égaux
        if (!listeReservationEgal.isEmpty()) {
            return listeReservationEgal.get(0);
        }

        // Fusionner sup et inf, puis trouver l'écart minimal
        List<Reservation> listeCandidats = new ArrayList<>();
        listeCandidats.addAll(listeReservationSup);
        listeCandidats.addAll(listeReservationInf);

        if (listeCandidats.isEmpty()) {
            return null;
        }

        if (listeCandidats.size() == 1) {
            return listeCandidats.get(0);
        }

        // Trouver l'écart minimal
        Reservation meilleureReservation = null;
        int ecartMin = Integer.MAX_VALUE;

        for (Reservation r : listeCandidats) {
            int ecart = Math.abs(r.getNbPassager() - placesRestantes);
            if (ecart < ecartMin) {
                ecartMin = ecart;
                meilleureReservation = r;
            }
        }

        return meilleureReservation;
    }

    /**
     * Regroupe les réservations par vague selon le temps d'attente.
     *
     * Une vague = fenêtre de temps [première_réservation, première_réservation +
     * temps_attente]
     * Toutes les réservations dans cette fenêtre font partie de la même vague.
     *
     * @param reservations        Liste des réservations à regrouper
     * @param tempsAttenteMinutes Temps d'attente en minutes (ex: 30)
     * @return Map avec clé = "YYYY-MM-DD HH:MM" et valeur = liste des réservations
     *         de cette vague
     */
    public static Map<String, List<Reservation>> regrouperParVague(List<Reservation> reservations,
            int tempsAttenteMinutes) {
        Map<String, List<Reservation>> vagues = new LinkedHashMap<>();

        if (reservations == null || reservations.isEmpty()) {
            return vagues;
        }

        long tempsAttenteMs = tempsAttenteMinutes * 60L * 1000L;

        // Trier toutes les réservations par date_heure croissante
        List<Reservation> triees = new ArrayList<>(reservations);
        triees.sort((r1, r2) -> r1.getDateHeure().compareTo(r2.getDateHeure()));

        int i = 0;

        while (i < triees.size()) {
            // La première réservation ouvre la fenêtre de traitement
            Timestamp debutFenetre = triees.get(i).getDateHeure();
            long debutMs = tronquerAuxMinutes(debutFenetre);
            long finFenetreMs = debutMs + tempsAttenteMs;

            // Collecter toutes les réservations dans [debutMs, finFenetreMs]
            List<Reservation> vagueReservations = new ArrayList<>();

            while (i < triees.size()) {
                Timestamp heureCourante = triees.get(i).getDateHeure();
                long heureCourtanteMs = tronquerAuxMinutes(heureCourante);

                if (heureCourtanteMs <= finFenetreMs) {
                    vagueReservations.add(triees.get(i));
                    i++;
                } else {
                    break;
                }
            }

            // La clé de la vague = début de la fenêtre (pour affichage)
            String cleVague = String.format("%tF %tH:%tM", debutFenetre, debutFenetre, debutFenetre);
            vagues.put(cleVague, vagueReservations);
        }

        return vagues;
    }

    /**
     * Tronque un Timestamp aux minutes (met les secondes et millisecondes à 0)
     */
    public static long tronquerAuxMinutes(Timestamp ts) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ts.getTime());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Compare deux voitures pour déterminer laquelle est meilleure pour un nombre de passagers donné.
     * Critères de comparaison (dans l'ordre):
     * 1. Écart minimal avec le nombre de passagers
     * 2. Nombre de trajets minimal
     * 3. Carburant: D > E
     *
     * @return true si nouvelleVoiture est meilleure que voitureActuelle
     */
    public static boolean estMeilleureVoiture(Voiture nouvelleVoiture, Voiture voitureActuelle,
            int nbPassagers, Map<Integer, Integer> compteurTrajetsJour) {

        int ecartNouvelle = Math.abs(nouvelleVoiture.getCapacite() - nbPassagers);
        int ecartActuelle = Math.abs(voitureActuelle.getCapacite() - nbPassagers);

        // 1. Comparer l'écart
        if (ecartNouvelle < ecartActuelle) {
            return true;
        }
        if (ecartNouvelle > ecartActuelle) {
            return false;
        }

        // Écart égal → comparer nombre de trajets
        int trajetsNouvelle = compteurTrajetsJour != null ? compteurTrajetsJour.getOrDefault(nouvelleVoiture.getIdVoiture(), 0) : 0;
        int trajetsActuelle = compteurTrajetsJour != null ? compteurTrajetsJour.getOrDefault(voitureActuelle.getIdVoiture(), 0) : 0;

        if (trajetsNouvelle < trajetsActuelle) {
            return true;
        }
        if (trajetsNouvelle > trajetsActuelle) {
            return false;
        }

        // Trajets égaux → comparer carburant (D > E)
        if ("D".equals(nouvelleVoiture.getCarburant()) && !"D".equals(voitureActuelle.getCarburant())) {
            return true;
        }

        return false;
    }

    public static void AssignationOptimal(SimulationAssignation simulationAssignation, List<Reservation> reservationsVague) {
        // Remplissage optimal : chercher des réservations qui correspondent au reste de
        // places
        while (simulationAssignation.getPlacesRestantes() > 0 && !reservationsVague.isEmpty()) {
            Reservation reservationRemplissage = UtilSimulation.trouverReservationPourRemplissage(
                    reservationsVague,
                    simulationAssignation.getPlacesRestantes());

            if (reservationRemplissage == null) {
                break;
            }

            // Vérifier que la réservation peut rentrer
            if (reservationRemplissage.getNbPassager() > simulationAssignation.getPlacesRestantes()) {
                break;
            }

            simulationAssignation.ajouterReservation(reservationRemplissage);
            reservationsVague.remove(reservationRemplissage);
        }
    }

    /**
     * Calcule l'itinéraire, l'heure de départ et l'heure d'arrivée pour une assignation.
     *
     * - Heure de départ = heureDepart (passée en paramètre)
     * - Itinéraire = Aéroport → Hôtel1 → Hôtel2 → ...
     * - Heure d'arrivée = heure de départ + durée totale
     *
     * @param assignation       L'assignation à traiter
     * @param aeroport          Le lieu de départ (aéroport)
     * @param heureDepart       L'heure de départ de la voiture
     * @param finFenetreVague   L'heure de fin de la fenêtre de vague (pour info)
     * @param distanceDAO       DAO pour récupérer les distances
     * @param lieuDAO           DAO pour récupérer les lieux
     * @param vitesseMoyenneKmH Vitesse moyenne en km/h
     */
    public static void calculerItineraire(SimulationAssignation assignation, Lieu aeroport,
            Timestamp heureDepart, Timestamp finFenetreVague, DistanceDAO distanceDAO, LieuDAO lieuDAO, double vitesseMoyenneKmH) {

        if (assignation == null || assignation.getReservations().isEmpty()) {
            return;
        }

        List<EtapeItineraire> itineraire = new ArrayList<>();
        int lieuDepartId = aeroport.getId();
        String lieuDepartNom = aeroport.getLibelle();
        int ordre = 1;

        // Récupérer tous les hôtels des réservations (sans doublons, garder l'ordre)
        List<Integer> hotelsIds = new ArrayList<>();
        for (Reservation r : assignation.getReservations()) {
            if (!hotelsIds.contains(r.getIdLieu())) {
                hotelsIds.add(r.getIdLieu());
            }
        }

        // Construire l'itinéraire: Aéroport → Hôtel1 → Hôtel2 → ...
        for (Integer hotelId : hotelsIds) {
            Lieu lieuArrivee = lieuDAO.findById(hotelId);
            if (lieuArrivee == null) {
                continue;
            }

            // Récupérer la distance
            Double distanceKm = distanceDAO.getDistance(lieuDepartId, hotelId);
            if (distanceKm == null) {
                distanceKm = 0.0;
            }

            // Calculer la durée en minutes: distance (km) / vitesse (km/h) * 60 (min)
            double dureeMinutes = (distanceKm / vitesseMoyenneKmH) * 60.0;

            // Créer l'étape
            EtapeItineraire etape = new EtapeItineraire(ordre, lieuDepartNom, lieuArrivee.getLibelle(), distanceKm, dureeMinutes);
            itineraire.add(etape);

            // Le prochain départ est l'arrivée actuelle
            lieuDepartId = hotelId;
            lieuDepartNom = lieuArrivee.getLibelle();
            ordre++;
        }

        // Mettre à jour l'assignation
        assignation.setItineraire(itineraire);
        assignation.setFinFenetreVague(finFenetreVague);
        assignation.setDateHeureDepart(heureDepart);

        // Calculer l'heure d'arrivée
        double dureeTotaleMinutes = assignation.getDureeTotaleMinutes();
        long dureeTotaleMs = (long) (dureeTotaleMinutes * 60 * 1000);
        Timestamp heureArrivee = new Timestamp(heureDepart.getTime() + dureeTotaleMs);
        assignation.setDateHeureArrivee(heureArrivee);
    }
}
