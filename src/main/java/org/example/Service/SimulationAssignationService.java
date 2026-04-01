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

    // Stocke les intervalles [debutFenetre, finFenetre] de chaque vague (clé =
    // cleVague)
    private Map<String, Timestamp[]> vagueIntervalles = new LinkedHashMap<>();

    private static class CandidatAffectation {
        private final Voiture voiture;
        private final SimulationAssignation assignationExistante;
        private final int placesDisponibles;

        private CandidatAffectation(Voiture voiture, SimulationAssignation assignationExistante,
                int placesDisponibles) {
            this.voiture = voiture;
            this.assignationExistante = assignationExistante;
            this.placesDisponibles = placesDisponibles;
        }
    }

    /**
     * Candidat avec heure de disponibilité, utilisé pour la sélection des
     * non-assignés
     */
    private static class CandidatNonAssigne {
        final Voiture voiture;
        final long heureDispoMs;

        CandidatNonAssigne(Voiture voiture, long heureDispoMs) {
            this.voiture = voiture;
            this.heureDispoMs = heureDispoMs;
        }
    }

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

        // Compteur de trajets par voiture pour le jour même (base + simulation)
        Map<Integer, Integer> compteurTrajetsJour = new HashMap<>();

        // 3. Récupérer toutes les voitures et la vitesse moyenne
        List<Voiture> toutesVoitures = voitureDAO.findAll();
        double vitesseMoyenne = parametreDAO.getVM();
        Lieu aeroport = lieuDAO.findAeroport();

        // 4. Construire les assignations existantes et pré-remplir les intervalles
        // occupés
        Map<Integer, List<long[]>> intervallesOccupes = new HashMap<>();

        for (Assignation a : assignationsExistantes) {
            Voiture voiture = voitureDAO.findById(a.getIdVoiture());
            if (voiture == null)
                continue;

            if (a.getDateHeureDepart() != null && a.getDateHeureArrivee() != null) {
                intervallesOccupes.computeIfAbsent(a.getIdVoiture(), k -> new ArrayList<>())
                        .add(new long[] { a.getDateHeureDepart().getTime(), a.getDateHeureArrivee().getTime() });

                java.sql.Date dateDepartAssign = new java.sql.Date(a.getDateHeureDepart().getTime());
                if (dateDepartAssign.equals(dateSimulation)) {
                    compteurTrajetsJour.put(a.getIdVoiture(),
                            compteurTrajetsJour.getOrDefault(a.getIdVoiture(), 0) + 1);
                }
            }

            List<Reservation> reservationsDeLAssignation = new ArrayList<>();
            List<ReservationAssignation> liens = new ArrayList<>(a.getReservationAssignations());
            liens.sort(Comparator.comparingInt(ReservationAssignation::getOrdreItineraire));

            for (ReservationAssignation ra : liens) {
                Reservation r = reservationDAO.findById(ra.getIdReservation());
                if (r != null) {
                    reservationsDeLAssignation.add(r);
                    reservationsDejaAssignees.add(r.getId());
                }
            }

            if (!reservationsDeLAssignation.isEmpty()) {
                Timestamp heureVague = a.getDateHeureDepart() != null
                        ? a.getDateHeureDepart()
                        : reservationsDeLAssignation.get(0).getDateHeure();
                SimulationAssignation saExistante = new SimulationAssignation(voiture, heureVague);
                for (Reservation r : reservationsDeLAssignation) {
                    saExistante.ajouterReservation(r);
                }
                saExistante.setDateHeureDepart(a.getDateHeureDepart());
                saExistante.setDateHeureArrivee(a.getDateHeureArrivee());

                if (saExistante.getDateHeureDepart() == null || saExistante.getDateHeureArrivee() == null) {
                    calculerHeuresTrajet(saExistante, aeroport, vitesseMoyenne);
                }
                resultat.ajouterAssignationExistante(saExistante);
            }
        }

        // Filtrer les réservations : exclure celles déjà assignées
        List<Reservation> reservationsASimuler = new ArrayList<>();
        for (Reservation r : reservations) {
            if (!reservationsDejaAssignees.contains(r.getId())) {
                reservationsASimuler.add(r);
            }
        }

        System.out.println(
                "📌 " + reservationsDejaAssignees.size() + " réservation(s) déjà assignée(s) en base (exclues)");
        System.out.println("📋 " + reservationsASimuler.size() + " réservation(s) à simuler\n");
        System.out.println("🔒 " + intervallesOccupes.size() + " voiture(s) avec des intervalles occupés\n");

        if (reservationsASimuler.isEmpty()) {
            System.out.println("ℹ️ Toutes les réservations sont déjà assignées");
            return resultat;
        }

        // 5. Regrouper les réservations restantes par vague
        Map<String, List<Reservation>> vagues = regrouperParVague(reservationsASimuler);
        resultat.setNbVagues(vagues.size());
        List<String> vaguesTriees = new ArrayList<>(vagues.keySet());
        Collections.sort(vaguesTriees);

        // Liste des réservations non assignées à retraiter dans les vagues suivantes
        List<Reservation> reservationsNonAssignees = new ArrayList<>();

        int numeroVague = 1;
        for (String cleVague : vaguesTriees) {
            List<Reservation> reservationsVague = vagues.get(cleVague);
            Timestamp[] intervalleVague = vagueIntervalles.get(cleVague);

            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("🌊 VAGUE #" + numeroVague + " - " + cleVague);
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // ================================================================
            // ÉTAPE A : Traitement PRIORITAIRE des réservations non assignées
            // ================================================================
            if (!reservationsNonAssignees.isEmpty()) {
                System.out.println("\n🔴 PRIORITÉ: Traitement de " + reservationsNonAssignees.size()
                        + " réservation(s) non assignée(s)");

                // Trier par passagers décroissants
                reservationsNonAssignees.sort((r1, r2) -> Integer.compare(r2.getNbPassager(), r1.getNbPassager()));

                List<Reservation> nonAssigneesRestantes = new ArrayList<>();

                for (Reservation resNA : reservationsNonAssignees) {
                    // Trouver la meilleure voiture disponible (revenues ou nouvelles)
                    // Critère 1 : heure de disponibilité la plus tôt
                    // Critère 2 (ex-æquo) : écart capacité → trajets min → D > H > E
                    CandidatNonAssigne candidat = choisirCandidatPourNonAssigne(
                            resNA.getNbPassager(),
                            toutesVoitures,
                            intervallesOccupes,
                            compteurTrajetsJour,
                            dateSimulation);

                    if (candidat == null) {
                        System.out.println("   ❌ Aucune voiture disponible pour réservation #"
                                + resNA.getId() + " (" + resNA.getNbPassager() + " passagers)");
                        nonAssigneesRestantes.add(resNA);
                        continue;
                    }

                    Voiture voitureChoisie = candidat.voiture;
                    long heureDispoMs = candidat.heureDispoMs;

                    SimulationAssignation assignationNA = new SimulationAssignation(
                            voitureChoisie, new Timestamp(heureDispoMs));
                    assignationNA.ajouterReservation(resNA);

                    boolean estPleine = assignationNA.getPlacesRestantes() == 0;

                    Timestamp heureDepart;
                    if (estPleine) {
                        // Pleine → départ immédiat dès que la voiture est disponible
                        heureDepart = new Timestamp(heureDispoMs);
                        System.out.println("   ✅ Réservation #" + resNA.getId()
                                + " → Voiture #" + voitureChoisie.getIdVoiture()
                                + " (" + voitureChoisie.getRef() + ") | PLEINE → départ immédiat à " + heureDepart);
                    } else {
                        // Pas pleine → attend la fin de la fenêtre de vague
                        long finFenetreMs = intervalleVague != null
                                ? intervalleVague[1].getTime()
                                : heureDispoMs;
                        heureDepart = new Timestamp(finFenetreMs);
                        System.out.println("   ✅ Réservation #" + resNA.getId()
                                + " → Voiture #" + voitureChoisie.getIdVoiture()
                                + " (" + voitureChoisie.getRef() + ") | pas pleine → départ fin fenêtre à "
                                + heureDepart);
                    }

                    assignationNA.setDateHeureDepart(heureDepart);
                    if (intervalleVague != null) {
                        assignationNA.setDebutVague(intervalleVague[0]);
                        assignationNA.setFinFenetreVague(intervalleVague[1]);
                    }
                    calculerHeuresTrajet(assignationNA, aeroport, vitesseMoyenne);

                    long departMs = assignationNA.getDateHeureDepart().getTime();
                    long arriveeMs = assignationNA.getDateHeureArrivee().getTime();

                    // Vérifier chevauchement
                    boolean chevauchement = false;
                    List<long[]> intervals = intervallesOccupes.get(voitureChoisie.getIdVoiture());
                    if (intervals != null) {
                        for (long[] interval : intervals) {
                            if (departMs < interval[1] && interval[0] < arriveeMs) {
                                chevauchement = true;
                                break;
                            }
                        }
                    }

                    if (chevauchement) {
                        System.out.println("   ⚠️ Chevauchement détecté pour réservation #"
                                + resNA.getId() + " → reportée");
                        nonAssigneesRestantes.add(resNA);
                    } else {
                        resultat.ajouterAssignation(assignationNA);
                        intervallesOccupes.computeIfAbsent(voitureChoisie.getIdVoiture(), k -> new ArrayList<>())
                                .add(new long[] { departMs, arriveeMs });
                        compteurTrajetsJour.put(voitureChoisie.getIdVoiture(),
                                compteurTrajetsJour.getOrDefault(voitureChoisie.getIdVoiture(), 0) + 1);
                    }
                }

                reservationsNonAssignees = nonAssigneesRestantes;
            }

            // ================================================================
            // ÉTAPE B : Traitement normal de la vague
            // ================================================================

            // Heure de base de la vague = heure de la dernière réservation
            Timestamp heureVague = reservationsVague.stream()
                    .map(Reservation::getDateHeure)
                    .filter(Objects::nonNull)
                    .max(Timestamp::compareTo)
                    .orElse(Timestamp.valueOf(cleVague + ":00"));

            // Heure de départ commune
            Timestamp heureDepartCommune = calculerHeureDepartCommuneVague(
                    heureVague, intervalleVague, toutesVoitures, intervallesOccupes, dateSimulation);

            // Filtrer les voitures disponibles à l'heure de départ commune
            long heureVagueMs = heureDepartCommune.getTime();
            List<Voiture> voituresDisponibles = new ArrayList<>();
            for (Voiture v : toutesVoitures) {
                long dispoVoitureMs = getDisponibiliteMsPourDate(v, dateSimulation);
                if (heureVagueMs < dispoVoitureMs) {
                    System.out.println("   ⏳ Voiture #" + v.getIdVoiture() + " (" + v.getRef()
                            + ") indisponible avant " + new Timestamp(dispoVoitureMs));
                    continue;
                }
                List<long[]> intervals = intervallesOccupes.get(v.getIdVoiture());
                boolean disponible = true;
                if (intervals != null) {
                    for (long[] interval : intervals) {
                        if (heureVagueMs >= interval[0] && heureVagueMs < interval[1]) {
                            disponible = false;
                            System.out.println("   🚫 Voiture #" + v.getIdVoiture() + " (" + v.getRef()
                                    + ") en trajet de " + new Timestamp(interval[0])
                                    + " à " + new Timestamp(interval[1]));
                            break;
                        }
                    }
                }
                if (disponible)
                    voituresDisponibles.add(v);
            }

            // Assignations temporaires pour cette vague
            Map<Integer, SimulationAssignation> assignationsVague = new LinkedHashMap<>();

            // État des réservations (passagers restants)
            Map<Integer, Reservation> reservationsParId = new LinkedHashMap<>();
            Map<Integer, Integer> passagersRestantsParReservation = new HashMap<>();
            Set<Integer> idsReservationsPrioritaires = new HashSet<>();

            for (Reservation reservation : reservationsVague) {
                Reservation existe = reservationsParId.get(reservation.getId());
                if (existe == null) {
                    reservationsParId.put(reservation.getId(), reservation);
                    passagersRestantsParReservation.put(reservation.getId(), reservation.getNbPassager());
                } else {
                    int cumule = passagersRestantsParReservation.getOrDefault(reservation.getId(), 0)
                            + reservation.getNbPassager();
                    passagersRestantsParReservation.put(reservation.getId(), cumule);
                }
            }

            // Boucle principale d'assignation de la vague
            while (aDesPassagersRestants(passagersRestantsParReservation)) {
                Reservation reservationPrincipale = choisirReservationPrincipale(
                        reservationsParId, passagersRestantsParReservation, idsReservationsPrioritaires);
                if (reservationPrincipale == null)
                    break;

                int restantsPrincipale = passagersRestantsParReservation
                        .getOrDefault(reservationPrincipale.getId(), 0);
                System.out.println("\n📌 Réservation principale #" + reservationPrincipale.getId()
                        + " - " + restantsPrincipale + " passagers restants");

                boolean progressionPrincipale = false;

                while (passagersRestantsParReservation.getOrDefault(reservationPrincipale.getId(), 0) > 0) {
                    int demande = passagersRestantsParReservation
                            .getOrDefault(reservationPrincipale.getId(), 0);
                    CandidatAffectation candidat = choisirCandidatPourFraction(
                            demande, voituresDisponibles, assignationsVague, compteurTrajetsJour);

                    if (candidat == null || candidat.placesDisponibles <= 0)
                        break;

                    int nbAffectes = Math.min(demande, candidat.placesDisponibles);
                    Reservation fractionPrincipale = clonerReservationAvecPassagers(reservationPrincipale, nbAffectes);

                    SimulationAssignation cible = candidat.assignationExistante;
                    if (cible == null) {
                        cible = new SimulationAssignation(candidat.voiture, heureDepartCommune);
                        assignationsVague.put(candidat.voiture.getIdVoiture(), cible);
                        System.out.println("   → Nouvelle voiture sélectionnée: #"
                                + candidat.voiture.getIdVoiture()
                                + " (" + candidat.voiture.getRef() + ")");
                    }

                    cible.ajouterReservation(fractionPrincipale);
                    int nouveauRestant = demande - nbAffectes;
                    passagersRestantsParReservation.put(reservationPrincipale.getId(), nouveauRestant);
                    if (nouveauRestant > 0)
                        idsReservationsPrioritaires.add(reservationPrincipale.getId());
                    progressionPrincipale = true;

                    System.out.println("   ✅ " + nbAffectes + " passager(s) affecté(s) à Voiture #"
                            + cible.getVoiture().getIdVoiture()
                            + " | Restants: " + nouveauRestant);

                    remplirAssignationAvecReservationProche(
                            cible, reservationsParId,
                            passagersRestantsParReservation, idsReservationsPrioritaires);
                }

                if (!progressionPrincipale)
                    break;
            }

            // Reliquats → non assignés pour prochaine vague
            for (Map.Entry<Integer, Reservation> entry : reservationsParId.entrySet()) {
                int restants = passagersRestantsParReservation.getOrDefault(entry.getKey(), 0);
                if (restants > 0) {
                    reservationsNonAssignees.add(clonerReservationAvecPassagers(entry.getValue(), restants));
                    System.out.println("   ❌ " + restants + " passager(s) non assigné(s) → réservation #"
                            + entry.getKey());
                }
            }

            // Incrémenter compteur trajets
            for (SimulationAssignation sa : assignationsVague.values()) {
                int idVoiture = sa.getVoiture().getIdVoiture();
                compteurTrajetsJour.put(idVoiture, compteurTrajetsJour.getOrDefault(idVoiture, 0) + 1);
            }

            // Calculer les heures initiales
            for (SimulationAssignation sa : assignationsVague.values()) {
                if (intervalleVague != null) {
                    sa.setDebutVague(intervalleVague[0]);
                    sa.setFinFenetreVague(intervalleVague[1]);
                }
                sa.setDateHeureDepart(heureDepartCommune);
                calculerHeuresTrajet(sa, aeroport, vitesseMoyenne);
            }

            // Réassignation pendant fenêtre d'attente (logique existante)
            verifierReassignationPendantAttente(
                    assignationsVague, intervalleVague, toutesVoitures,
                    intervallesOccupes, compteurTrajetsJour, dateSimulation);

            // ================================================================
            // ÉTAPE C : Synchronisation des départs si la dernière voiture est pleine
            // ================================================================
            synchroniserDeparts(assignationsVague);

            // Recalculer les heures après réassignation et synchro
            for (SimulationAssignation sa : assignationsVague.values()) {
                calculerHeuresTrajet(sa, aeroport, vitesseMoyenne);
            }

            // Vérifier chevauchements et ajouter au résultat
            for (SimulationAssignation assignation : assignationsVague.values()) {
                long departMs = assignation.getDateHeureDepart().getTime();
                long arriveeMs = assignation.getDateHeureArrivee().getTime();
                int idVoiture = assignation.getVoiture().getIdVoiture();

                boolean chevauchement = false;
                List<long[]> intervals = intervallesOccupes.get(idVoiture);
                if (intervals != null) {
                    for (long[] interval : intervals) {
                        if (departMs < interval[1] && interval[0] < arriveeMs) {
                            chevauchement = true;
                            System.out.println("   ⚠️ Voiture #" + idVoiture
                                    + " chevauchement: [" + assignation.getDateHeureDepart()
                                    + " - " + assignation.getDateHeureArrivee() + "]");
                            break;
                        }
                    }
                }

                if (chevauchement) {
                    for (Reservation r : assignation.getReservations()) {
                        reservationsNonAssignees.add(r);
                    }
                } else {
                    resultat.ajouterAssignation(assignation);
                    intervallesOccupes.computeIfAbsent(idVoiture, k -> new ArrayList<>())
                            .add(new long[] { departMs, arriveeMs });
                }
            }

            numeroVague++;
            System.out.println();
        }

        // ================================================================
        // POST-TRAITEMENT : réservations non assignées après toutes les vagues
        // ================================================================
        if (!reservationsNonAssignees.isEmpty()) {
            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("🔁 POST-TRAITEMENT: " + reservationsNonAssignees.size()
                    + " réservation(s) non assignée(s) à retraiter");

            reservationsNonAssignees.sort((r1, r2) -> Integer.compare(r2.getNbPassager(), r1.getNbPassager()));

            List<Reservation> definitivementNonAssignees = new ArrayList<>();

            for (Reservation resNA : reservationsNonAssignees) {
                // Fractionner si nécessaire (ex : 7 passagers, max cap 5)
                int restants = resNA.getNbPassager();

                while (restants > 0) {
                    CandidatNonAssigne candidat = choisirCandidatPourNonAssigne(
                            restants, // essaie d'abord le total
                            toutesVoitures,
                            intervallesOccupes,
                            compteurTrajetsJour,
                            dateSimulation);

                    // Si aucune voiture ne peut prendre TOUS les passagers restants,
                    // chercher la voiture avec la plus grande capacité disponible
                    if (candidat == null) {
                        candidat = choisirCandidatPourNonAssigne(
                                1, // au moins 1 passager
                                toutesVoitures,
                                intervallesOccupes,
                                compteurTrajetsJour,
                                dateSimulation);
                    }

                    if (candidat == null) {
                        System.out.println("   ❌ Aucune voiture disponible pour "
                                + restants + " passager(s) restants de réservation #" + resNA.getId());
                        definitivementNonAssignees.add(
                                clonerReservationAvecPassagers(resNA, restants));
                        break;
                    }

                    Voiture voitureChoisie = candidat.voiture;
                    int nbAffectes = Math.min(restants, voitureChoisie.getCapacite());
                    Timestamp heureDepart = new Timestamp(candidat.heureDispoMs);

                    SimulationAssignation sa = new SimulationAssignation(voitureChoisie, heureDepart);
                    sa.ajouterReservation(clonerReservationAvecPassagers(resNA, nbAffectes));
                    sa.setDateHeureDepart(heureDepart);
                    calculerHeuresTrajet(sa, aeroport, vitesseMoyenne);

                    long departMs = sa.getDateHeureDepart().getTime();
                    long arriveeMs = sa.getDateHeureArrivee().getTime();

                    intervallesOccupes.computeIfAbsent(voitureChoisie.getIdVoiture(),
                            k -> new ArrayList<>())
                            .add(new long[] { departMs, arriveeMs });
                    compteurTrajetsJour.put(voitureChoisie.getIdVoiture(),
                            compteurTrajetsJour.getOrDefault(voitureChoisie.getIdVoiture(), 0) + 1);

                    resultat.ajouterAssignation(sa);
                    System.out.println("   ✅ " + nbAffectes + " passager(s) → Voiture #"
                            + voitureChoisie.getIdVoiture()
                            + " départ à " + heureDepart);

                    restants -= nbAffectes;
                }
            }

            reservationsNonAssignees = definitivementNonAssignees;
        }

        System.out.println("\n========================================");
        System.out.println("📊 RÉSUMÉ DE LA SIMULATION");
        System.out.println("========================================");
        System.out.println("✅ Réservations assignées: " + resultat.getTotalReservationsAssignees());
        System.out.println("❌ Réservations non assignées: " + resultat.getReservationsNonAssignees().size());
        System.out.println("🚗 Voitures utilisées: " + resultat.getAssignations().size());
        System.out.println("========================================\n");

        return resultat;
    }

    // ========================================================================
    // NOUVEAU : Choisir la meilleure voiture pour un non-assigné
    // Critère 1 : heure de disponibilité la plus tôt (revenue ou nouvelle)
    // Critère 2 (ex-æquo) : écart capacité min → trajets min → D > H > E
    // ========================================================================
    private CandidatNonAssigne choisirCandidatPourNonAssigne(
            int nbPassagers,
            List<Voiture> toutesVoitures,
            Map<Integer, List<long[]>> intervallesOccupes,
            Map<Integer, Integer> compteurTrajetsJour,
            Date dateSimulation) {

        List<CandidatNonAssigne> candidats = new ArrayList<>();

        for (Voiture v : toutesVoitures) {
            if (v.getCapacite() < nbPassagers)
                continue;

            // Heure à laquelle la voiture est libre (fin de son dernier trajet ou heure
            // initiale)
            long heureDispoTrajet = trouverHeureDisponibilite(v, intervallesOccupes, 0);
            long heureDispoInitiale = getDisponibiliteMsPourDate(v, dateSimulation);
            long heureDispo = Math.max(heureDispoTrajet, heureDispoInitiale);

            candidats.add(new CandidatNonAssigne(v, heureDispo));
        }

        if (candidats.isEmpty())
            return null;

        // Trier : heure dispo la plus tôt → écart capacité → trajets → carburant
        candidats.sort((c1, c2) -> {
            // 1. Heure de disponibilité (plus tôt = meilleur)
            int cmpDispo = Long.compare(c1.heureDispoMs, c2.heureDispoMs);
            if (cmpDispo != 0)
                return cmpDispo;

            // 2. Écart capacité (plus petit = meilleur)
            int ecart1 = c1.voiture.getCapacite() - nbPassagers;
            int ecart2 = c2.voiture.getCapacite() - nbPassagers;
            int cmpEcart = Integer.compare(ecart1, ecart2);
            if (cmpEcart != 0)
                return cmpEcart;

            // 3. Nombre de trajets du jour (moins = meilleur)
            int trajets1 = compteurTrajetsJour.getOrDefault(c1.voiture.getIdVoiture(), 0);
            int trajets2 = compteurTrajetsJour.getOrDefault(c2.voiture.getIdVoiture(), 0);
            int cmpTrajets = Integer.compare(trajets1, trajets2);
            if (cmpTrajets != 0)
                return cmpTrajets;

            // 4. Carburant (D > H > E)
            return Integer.compare(
                    getPrioriteCarburant(c2.voiture.getCarburant()),
                    getPrioriteCarburant(c1.voiture.getCarburant()));
        });

        return candidats.get(0);
    }

    // ========================================================================
    // NOUVEAU : Synchroniser les départs de la vague
    // Si la voiture dont la DERNIÈRE réservation est la plus tardive est PLEINE,
    // toutes les voitures de la vague partent à cette même heure.
    // ========================================================================
    private void synchroniserDeparts(Map<Integer, SimulationAssignation> assignationsVague) {
        if (assignationsVague.isEmpty())
            return;

        // Trouver la voiture dont la date_heure de la dernière réservation est la plus
        // tardive
        SimulationAssignation derniereVoiture = null;
        Timestamp derniereHeure = null;

        for (SimulationAssignation sa : assignationsVague.values()) {
            Timestamp maxHeure = sa.getReservations().stream()
                    .map(Reservation::getDateHeure)
                    .filter(Objects::nonNull)
                    .max(Timestamp::compareTo)
                    .orElse(null);

            if (maxHeure == null)
                continue;

            if (derniereHeure == null || maxHeure.after(derniereHeure)) {
                derniereHeure = maxHeure;
                derniereVoiture = sa;
            }
        }

        if (derniereVoiture == null || derniereHeure == null)
            return;

        // Vérifier si cette voiture est pleine
        boolean estPleine = derniereVoiture.getPlacesRestantes() == 0;

        if (estPleine) {
            System.out.println("   ⏱️ SYNCHRO VAGUE: Voiture #"
                    + derniereVoiture.getVoiture().getIdVoiture()
                    + " (dernière, pleine) → toutes les voitures partent à " + derniereHeure);

            // Toutes les voitures de la vague partent à l'heure de la dernière
            for (SimulationAssignation sa : assignationsVague.values()) {
                sa.setDateHeureDepart(derniereHeure);
            }
        }
    }

    // ========================================================================
    // Méthodes inchangées
    // ========================================================================

    private SimulationAssignation trouverMeilleureVoiture(
            Reservation reservation, List<Voiture> voituresDisponibles,
            Map<Integer, SimulationAssignation> assignationsVague,
            Timestamp heureVague, Map<Integer, Integer> compteurTrajetsJour) {

        int nbPassagers = reservation.getNbPassager();

        for (SimulationAssignation assignation : assignationsVague.values()) {
            if (assignation.peutAccueillir(nbPassagers)) {
                assignation.ajouterReservation(reservation);
                System.out.println("   → Ajoutée à voiture existante (remplissage optimal)");
                return assignation;
            }
        }

        List<Voiture> voituresCompatibles = voituresDisponibles.stream()
                .filter(v -> v.getCapacite() >= nbPassagers)
                .filter(v -> !assignationsVague.containsKey(v.getIdVoiture()))
                .collect(Collectors.toList());

        if (voituresCompatibles.isEmpty()) {
            System.out.println("   → Aucune voiture compatible disponible");
            return null;
        }

        int ecartMin = voituresCompatibles.stream()
                .mapToInt(v -> v.getCapacite() - nbPassagers).min().orElse(0);
        List<Voiture> voituresOptimales = voituresCompatibles.stream()
                .filter(v -> (v.getCapacite() - nbPassagers) == ecartMin)
                .collect(Collectors.toList());

        Map<Integer, Integer> nbTrajetsParVoiture = new HashMap<>();
        for (Voiture v : voituresOptimales) {
            nbTrajetsParVoiture.put(v.getIdVoiture(),
                    compteurTrajetsJour.getOrDefault(v.getIdVoiture(), 0));
        }

        int nbTrajetsMin = nbTrajetsParVoiture.values().stream()
                .min(Integer::compare).orElse(0);
        List<Voiture> voituresAvecMinTrajets = voituresOptimales.stream()
                .filter(v -> nbTrajetsParVoiture.get(v.getIdVoiture()) == nbTrajetsMin)
                .collect(Collectors.toList());

        Voiture voitureChoisie = choisirParCarburant(voituresAvecMinTrajets);
        SimulationAssignation assignation = new SimulationAssignation(voitureChoisie, heureVague);
        assignation.ajouterReservation(reservation);
        assignationsVague.put(voitureChoisie.getIdVoiture(), assignation);
        return assignation;
    }

    private CandidatAffectation choisirCandidatPourFraction(
            int nbPassagersRestants, List<Voiture> voituresDisponibles,
            Map<Integer, SimulationAssignation> assignationsVague,
            Map<Integer, Integer> compteurTrajetsJour) {

        List<CandidatAffectation> candidats = new ArrayList<>();

        for (SimulationAssignation sa : assignationsVague.values()) {
            if (sa.getPlacesRestantes() > 0) {
                candidats.add(new CandidatAffectation(sa.getVoiture(), sa, sa.getPlacesRestantes()));
            }
        }
        for (Voiture v : voituresDisponibles) {
            if (!assignationsVague.containsKey(v.getIdVoiture()) && v.getCapacite() > 0) {
                candidats.add(new CandidatAffectation(v, null, v.getCapacite()));
            }
        }

        if (candidats.isEmpty())
            return null;

        List<CandidatAffectation> superieurs = candidats.stream()
                .filter(c -> c.placesDisponibles >= nbPassagersRestants)
                .collect(Collectors.toList());

        List<CandidatAffectation> pool;
        if (!superieurs.isEmpty()) {
            int ecartMin = superieurs.stream()
                    .mapToInt(c -> c.placesDisponibles - nbPassagersRestants).min().orElse(0);
            pool = superieurs.stream()
                    .filter(c -> (c.placesDisponibles - nbPassagersRestants) == ecartMin)
                    .collect(Collectors.toList());
        } else {
            List<CandidatAffectation> inferieurs = candidats.stream()
                    .filter(c -> c.placesDisponibles < nbPassagersRestants)
                    .collect(Collectors.toList());
            if (inferieurs.isEmpty())
                return null;
            int placeMax = inferieurs.stream().mapToInt(c -> c.placesDisponibles).max().orElse(0);
            pool = inferieurs.stream()
                    .filter(c -> c.placesDisponibles == placeMax).collect(Collectors.toList());
        }

        int minTrajets = pool.stream()
                .mapToInt(c -> compteurTrajetsJour.getOrDefault(c.voiture.getIdVoiture(), 0)
                        + (assignationsVague.containsKey(c.voiture.getIdVoiture()) ? 1 : 0))
                .min().orElse(0);
        List<CandidatAffectation> apresTrajets = pool.stream()
                .filter(c -> (compteurTrajetsJour.getOrDefault(c.voiture.getIdVoiture(), 0)
                        + (assignationsVague.containsKey(c.voiture.getIdVoiture()) ? 1 : 0)) == minTrajets)
                .collect(Collectors.toList());

        int meilleurCarburant = apresTrajets.stream()
                .mapToInt(c -> getPrioriteCarburant(c.voiture.getCarburant())).max().orElse(0);
        List<CandidatAffectation> apresCarburant = apresTrajets.stream()
                .filter(c -> getPrioriteCarburant(c.voiture.getCarburant()) == meilleurCarburant)
                .collect(Collectors.toList());

        return apresCarburant.get(new Random().nextInt(apresCarburant.size()));
    }

    private Reservation clonerReservationAvecPassagers(Reservation source, int nbPassagers) {
        Reservation clone = new Reservation();
        clone.setId(source.getId());
        clone.setIdLieu(source.getIdLieu());
        clone.setIdClient(source.getIdClient());
        clone.setDateHeure(source.getDateHeure());
        clone.setNbPassager(nbPassagers);
        return clone;
    }

    private boolean aDesPassagersRestants(Map<Integer, Integer> passagersRestantsParReservation) {
        for (Integer restants : passagersRestantsParReservation.values()) {
            if (restants != null && restants > 0)
                return true;
        }
        return false;
    }

    private Reservation choisirReservationPrincipale(
            Map<Integer, Reservation> reservationsParId,
            Map<Integer, Integer> passagersRestantsParReservation,
            Set<Integer> idsReservationsPrioritaires) {

        List<Reservation> candidates = new ArrayList<>();
        for (Reservation r : reservationsParId.values()) {
            if (passagersRestantsParReservation.getOrDefault(r.getId(), 0) > 0) {
                candidates.add(r);
            }
        }
        if (candidates.isEmpty())
            return null;

        boolean existePrioritaire = candidates.stream()
                .anyMatch(r -> idsReservationsPrioritaires.contains(r.getId()));
        if (existePrioritaire) {
            candidates.removeIf(r -> !idsReservationsPrioritaires.contains(r.getId()));
        }

        candidates.sort((r1, r2) -> {
            int p1 = passagersRestantsParReservation.getOrDefault(r1.getId(), 0);
            int p2 = passagersRestantsParReservation.getOrDefault(r2.getId(), 0);
            int cmp = Integer.compare(p2, p1);
            if (cmp != 0)
                return cmp;
            Timestamp t1 = r1.getDateHeure(), t2 = r2.getDateHeure();
            if (t1 == null && t2 == null)
                return 0;
            if (t1 == null)
                return 1;
            if (t2 == null)
                return -1;
            return t1.compareTo(t2);
        });

        return candidates.get(0);
    }

    private void remplirAssignationAvecReservationProche(
            SimulationAssignation assignation,
            Map<Integer, Reservation> reservationsParId,
            Map<Integer, Integer> passagersRestantsParReservation,
            Set<Integer> idsReservationsPrioritaires) {

        while (assignation.getPlacesRestantes() > 0) {
            Reservation res = choisirReservationPourRemplissage(
                    assignation.getPlacesRestantes(), reservationsParId,
                    passagersRestantsParReservation, idsReservationsPrioritaires);
            if (res == null)
                break;

            int restants = passagersRestantsParReservation.getOrDefault(res.getId(), 0);
            if (restants <= 0)
                break;

            int nbAffectes = Math.min(restants, assignation.getPlacesRestantes());
            assignation.ajouterReservation(clonerReservationAvecPassagers(res, nbAffectes));
            passagersRestantsParReservation.put(res.getId(), restants - nbAffectes);

            System.out.println("   ↳ Remplissage: réservation #" + res.getId()
                    + " +" + nbAffectes + " passager(s), restants=" + (restants - nbAffectes)
                    + ", places voiture=" + assignation.getPlacesRestantes());
        }
    }

    private Reservation choisirReservationPourRemplissage(
            int placesRestantes,
            Map<Integer, Reservation> reservationsParId,
            Map<Integer, Integer> passagersRestantsParReservation,
            Set<Integer> idsReservationsPrioritaires) {

        List<Reservation> candidates = new ArrayList<>();
        for (Reservation r : reservationsParId.values()) {
            if (passagersRestantsParReservation.getOrDefault(r.getId(), 0) > 0)
                candidates.add(r);
        }
        if (candidates.isEmpty())
            return null;

        boolean existePrioritaire = candidates.stream()
                .anyMatch(r -> idsReservationsPrioritaires.contains(r.getId()));
        if (existePrioritaire)
            candidates.removeIf(r -> !idsReservationsPrioritaires.contains(r.getId()));

        List<Reservation> superieurs = new ArrayList<>(), inferieurs = new ArrayList<>();
        for (Reservation r : candidates) {
            int restants = passagersRestantsParReservation.getOrDefault(r.getId(), 0);
            if (restants >= placesRestantes)
                superieurs.add(r);
            else
                inferieurs.add(r);
        }

        List<Reservation> pool = !superieurs.isEmpty() ? superieurs : inferieurs;
        if (pool.isEmpty())
            return null;

        pool.sort((r1, r2) -> {
            int p1 = passagersRestantsParReservation.getOrDefault(r1.getId(), 0);
            int p2 = passagersRestantsParReservation.getOrDefault(r2.getId(), 0);
            int cmpEcart = Integer.compare(Math.abs(p1 - placesRestantes), Math.abs(p2 - placesRestantes));
            if (cmpEcart != 0)
                return cmpEcart;
            int cmpPass = Integer.compare(p2, p1);
            if (cmpPass != 0)
                return cmpPass;
            Timestamp t1 = r1.getDateHeure(), t2 = r2.getDateHeure();
            if (t1 == null && t2 == null)
                return 0;
            if (t1 == null)
                return 1;
            if (t2 == null)
                return -1;
            return t1.compareTo(t2);
        });

        return pool.get(0);
    }

    private boolean estMeilleureVoiture(Voiture nouvelleVoiture, Voiture voitureActuelle,
            int nbPassagers, Map<Integer, Integer> compteurTrajetsJour) {
        int ecartN = nouvelleVoiture.getCapacite() - nbPassagers;
        int ecartA = voitureActuelle.getCapacite() - nbPassagers;
        if (ecartN < ecartA)
            return true;
        if (ecartN > ecartA)
            return false;
        int trajN = compteurTrajetsJour.getOrDefault(nouvelleVoiture.getIdVoiture(), 0);
        int trajA = compteurTrajetsJour.getOrDefault(voitureActuelle.getIdVoiture(), 0);
        if (trajN < trajA)
            return true;
        if (trajN > trajA)
            return false;
        return getPrioriteCarburant(nouvelleVoiture.getCarburant()) > getPrioriteCarburant(
                voitureActuelle.getCarburant());
    }

    private int getPrioriteCarburant(String carburant) {
        if ("D".equals(carburant))
            return 3;
        if ("H".equals(carburant))
            return 2;
        if ("E".equals(carburant))
            return 1;
        return 0;
    }

    private void verifierReassignationPendantAttente(
            Map<Integer, SimulationAssignation> assignationsVague,
            Timestamp[] intervalleVague,
            List<Voiture> toutesVoitures,
            Map<Integer, List<long[]>> intervallesOccupes,
            Map<Integer, Integer> compteurTrajetsJour,
            Date dateSimulation) {

        if (intervalleVague == null || assignationsVague.isEmpty())
            return;

        long debutFenetreMs = intervalleVague[0].getTime();

        for (SimulationAssignation assignation : assignationsVague.values()) {
            Voiture voitureActuelle = assignation.getVoiture();
            long heureDepartMs = assignation.getDateHeureDepart().getTime();
            int nbPassagersTotal = assignation.getReservations().stream()
                    .mapToInt(Reservation::getNbPassager).sum();

            for (Voiture voitureCandidate : toutesVoitures) {
                if (voitureCandidate.getIdVoiture() == voitureActuelle.getIdVoiture())
                    continue;
                if (assignationsVague.containsKey(voitureCandidate.getIdVoiture()))
                    continue;
                if (voitureCandidate.getCapacite() < nbPassagersTotal)
                    continue;

                long heureDispoTrajetMs = trouverHeureDisponibilite(voitureCandidate, intervallesOccupes,
                        debutFenetreMs);
                long heureDispoInitialeMs = getDisponibiliteMsPourDate(voitureCandidate, dateSimulation);
                long heureDispoMs = Math.max(heureDispoTrajetMs, heureDispoInitialeMs);

                if (heureDispoMs > debutFenetreMs && heureDispoMs < heureDepartMs) {
                    if (estMeilleureVoiture(voitureCandidate, voitureActuelle, nbPassagersTotal, compteurTrajetsJour)) {
                        System.out.println("   🔄 RÉASSIGNATION: Voiture #" + voitureActuelle.getIdVoiture()
                                + " → Voiture #" + voitureCandidate.getIdVoiture()
                                + " disponible à " + new Timestamp(heureDispoMs));
                        compteurTrajetsJour.put(voitureActuelle.getIdVoiture(),
                                compteurTrajetsJour.getOrDefault(voitureActuelle.getIdVoiture(), 1) - 1);
                        compteurTrajetsJour.put(voitureCandidate.getIdVoiture(),
                                compteurTrajetsJour.getOrDefault(voitureCandidate.getIdVoiture(), 0) + 1);
                        assignation.setVoiture(voitureCandidate);
                        voitureActuelle = voitureCandidate;
                    }
                }
            }
        }
    }

    private long trouverHeureDisponibilite(
            Voiture voiture, Map<Integer, List<long[]>> intervallesOccupes, long debutFenetreMs) {
        List<long[]> intervals = intervallesOccupes.get(voiture.getIdVoiture());
        if (intervals == null || intervals.isEmpty())
            return 0;
        long heureDispoDerniere = 0;
        for (long[] interval : intervals) {
            if (interval[1] > debutFenetreMs && interval[1] > heureDispoDerniere) {
                heureDispoDerniere = interval[1];
            }
        }
        return heureDispoDerniere;
    }

    private Timestamp calculerHeureDepartCommuneVague(
            Timestamp heureVague, Timestamp[] intervalleVague,
            List<Voiture> toutesVoitures,
            Map<Integer, List<long[]>> intervallesOccupes,
            Date dateSimulation) {

        if (intervalleVague == null)
            return heureVague;

        long departInitialMs = heureVague.getTime();
        long finFenetreMs = intervalleVague[1].getTime();
        long retourLePlusTot = Long.MAX_VALUE;

        for (Voiture voiture : toutesVoitures) {
            long heureDispoTrajet = trouverHeureDisponibilite(voiture, intervallesOccupes, departInitialMs);
            long heureDispoInitiale = getDisponibiliteMsPourDate(voiture, dateSimulation);
            long heureDispo = Math.max(heureDispoTrajet, heureDispoInitiale);
            if (heureDispo > departInitialMs && heureDispo <= finFenetreMs) {
                if (heureDispo < retourLePlusTot)
                    retourLePlusTot = heureDispo;
            }
        }

        if (retourLePlusTot != Long.MAX_VALUE) {
            System.out.println("   ⏱️ Départ commun décalé à " + new Timestamp(retourLePlusTot)
                    + " (retour voiture pendant fenêtre)");
            return new Timestamp(retourLePlusTot);
        }
        return heureVague;
    }

    private long getDisponibiliteMsPourDate(Voiture voiture, Date dateSimulation) {
        if (voiture.getDisponibilite() == null) {
            return new Timestamp(dateSimulation.getTime()).getTime();
        }
        Calendar calDate = Calendar.getInstance();
        calDate.setTime(dateSimulation);
        Calendar calTime = Calendar.getInstance();
        calTime.setTime(voiture.getDisponibilite());
        calDate.set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY));
        calDate.set(Calendar.MINUTE, calTime.get(Calendar.MINUTE));
        calDate.set(Calendar.SECOND, calTime.get(Calendar.SECOND));
        calDate.set(Calendar.MILLISECOND, 0);
        return calDate.getTimeInMillis();
    }

    private Voiture choisirParCarburant(List<Voiture> voitures) {
        if (voitures.size() == 1)
            return voitures.get(0);
        for (String carb : new String[] { "D", "H", "E" }) {
            List<Voiture> filtered = voitures.stream()
                    .filter(v -> carb.equals(v.getCarburant())).collect(Collectors.toList());
            if (!filtered.isEmpty()) {
                return filtered.size() == 1 ? filtered.get(0)
                        : filtered.get(new Random().nextInt(filtered.size()));
            }
        }
        return voitures.get(new Random().nextInt(voitures.size()));
    }

    private void calculerHeuresTrajet(SimulationAssignation assignation, Lieu aeroport, double vitesseMoyenne) {
        Timestamp departReservation = assignation.getReservations().stream()
                .map(Reservation::getDateHeure)
                .max(Timestamp::compareTo)
                .orElse(assignation.getHeureVague());

        Timestamp departExistant = assignation.getDateHeureDepart();
        Timestamp depart = (departExistant != null && departExistant.after(departReservation))
                ? departExistant
                : departReservation;
        assignation.setDateHeureDepart(depart);

        if (aeroport == null) {
            assignation.setDateHeureArrivee(depart);
            return;
        }

        List<Integer> idLieux = assignation.getReservations().stream()
                .map(Reservation::getIdLieu).distinct().collect(Collectors.toList());
        if (idLieux.isEmpty()) {
            assignation.setDateHeureArrivee(depart);
            return;
        }

        List<Lieu> lieux = new ArrayList<>();
        for (int idLieu : idLieux) {
            Lieu lieu = lieuDAO.findById(idLieu);
            if (lieu != null)
                lieux.add(lieu);
        }

        int idAeroport = aeroport.getId();
        lieux.sort((l1, l2) -> {
            Double d1 = distanceDAO.getDistance(idAeroport, l1.getId());
            Double d2 = distanceDAO.getDistance(idAeroport, l2.getId());
            double dist1 = d1 != null ? d1 : Double.MAX_VALUE;
            double dist2 = d2 != null ? d2 : Double.MAX_VALUE;
            int cmp = Double.compare(dist1, dist2);
            if (cmp != 0)
                return cmp;
            return l1.getLibelle().compareToIgnoreCase(l2.getLibelle());
        });

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

        Double distRetour = distanceDAO.getDistance(positionCourante, idAeroport);
        double distRetourVal = distRetour != null ? distRetour : 0.0;
        double dureeRetour = (distRetourVal / vitesseMoyenne) * 60.0;
        itineraire.add(new EtapeItineraire(etapeNum, nomCourant, aeroport.getLibelle(), distRetourVal, dureeRetour));
        distanceTotale += distRetourVal;

        assignation.setItineraire(itineraire);

        long dureeMillis = (long) ((distanceTotale / vitesseMoyenne) * 3600 * 1000);
        Timestamp arrivee = new Timestamp(depart.getTime() + dureeMillis);
        assignation.setDateHeureArrivee(arrivee);

        System.out.println("   🕐 Départ: " + depart + " | Distance: " + distanceTotale
                + " km | Arrivée: " + arrivee);
    }

    private Map<String, List<Reservation>> regrouperParVague(List<Reservation> reservations) {
        int tempsAttenteMinutes = parametreDAO.getTempsAttente();
        long tempsAttenteMs = tempsAttenteMinutes * 60L * 1000L;

        List<Reservation> triees = new ArrayList<>(reservations);
        triees.sort((r1, r2) -> r1.getDateHeure().compareTo(r2.getDateHeure()));

        Map<String, List<Reservation>> vagues = new LinkedHashMap<>();
        int i = 0;

        while (i < triees.size()) {
            Timestamp debutFenetre = triees.get(i).getDateHeure();
            long debutMs = tronquerAuxMinutes(debutFenetre);
            long finFenetreMs = debutMs + tempsAttenteMs;

            List<Reservation> vagueReservations = new ArrayList<>();
            while (i < triees.size()) {
                Timestamp heureCourante = triees.get(i).getDateHeure();
                if (tronquerAuxMinutes(heureCourante) <= finFenetreMs) {
                    vagueReservations.add(triees.get(i));
                    i++;
                } else {
                    break;
                }
            }

            String cleVague = String.format("%tF %tH:%tM", debutFenetre, debutFenetre, debutFenetre);
            vagues.put(cleVague, vagueReservations);
            vagueIntervalles.put(cleVague,
                    new Timestamp[] { new Timestamp(debutMs), new Timestamp(finFenetreMs) });
        }

        return vagues;
    }

    private long tronquerAuxMinutes(Timestamp ts) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ts.getTime());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Classe interne pour stocker une voiture et son heure de disponibilité
     */
    private static class VoitureDisponibilite {
        Voiture voiture;
        long heureDispoMs;

        VoitureDisponibilite(Voiture voiture, long heureDispoMs) {
            this.voiture = voiture;
            this.heureDispoMs = heureDispoMs;
        }
    }
}