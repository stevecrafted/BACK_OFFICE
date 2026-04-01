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

    private static class CandidatAffectation {
        private final Voiture voiture;
        private final SimulationAssignation assignationExistante;
        private final int placesDisponibles;

        private CandidatAffectation(Voiture voiture, SimulationAssignation assignationExistante, int placesDisponibles) {
            this.voiture = voiture;
            this.assignationExistante = assignationExistante;
            this.placesDisponibles = placesDisponibles;
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

        // 4. Construire les assignations existantes pour affichage et pré-remplir les intervalles occupés
        // Chaque voiture a une liste d'intervalles [departMs, arriveeMs] pendant lesquels elle est occupée
        Map<Integer, List<long[]>> intervallesOccupes = new HashMap<>();

        for (Assignation a : assignationsExistantes) {
            Voiture voiture = voitureDAO.findById(a.getIdVoiture());
            if (voiture == null) {
                continue;
            }

            // Toujours considérer l'intervalle d'assignation (même sans réservations liées)
            if (a.getDateHeureDepart() != null && a.getDateHeureArrivee() != null) {
                intervallesOccupes.computeIfAbsent(a.getIdVoiture(), k -> new ArrayList<>())
                        .add(new long[]{a.getDateHeureDepart().getTime(), a.getDateHeureArrivee().getTime()});

                java.sql.Date dateDepartAssign = new java.sql.Date(a.getDateHeureDepart().getTime());
                if (dateDepartAssign.equals(dateSimulation)) {
                    compteurTrajetsJour.put(a.getIdVoiture(), compteurTrajetsJour.getOrDefault(a.getIdVoiture(), 0) + 1);
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
                    // fallback uniquement si données historiques incomplètes
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

        System.out.println("📌 " + reservationsDejaAssignees.size() + " réservation(s) déjà assignée(s) en base (exclues)");
        System.out.println("📋 " + reservationsASimuler.size() + " réservation(s) à simuler\n");

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

        // Liste des réservations non assignées à retraiter dans les vagues suivantes
        List<Reservation> reservationsNonAssignees = new ArrayList<>();

        int numeroVague = 1;
        for (String cleVague : vaguesTriees) {
            List<Reservation> reservationsVague = vagues.get(cleVague);

            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("🌊 VAGUE #" + numeroVague + " - " + cleVague);
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // Ajouter les réservations non assignées des vagues précédentes pour retraitement
            Set<Integer> idsReservationsPrioritaires = new HashSet<>();
            if (!reservationsNonAssignees.isEmpty()) {
                System.out.println("🔄 Retraitement de " + reservationsNonAssignees.size() + 
                                   " réservation(s) non assignée(s) des vagues précédentes");
                for (Reservation reservationNonAssignee : reservationsNonAssignees) {
                    idsReservationsPrioritaires.add(reservationNonAssignee.getId());
                }
                reservationsVague.addAll(reservationsNonAssignees);
                reservationsNonAssignees.clear();
            }

            // Trier les NA en priorité, puis nombre de passagers décroissant, puis date_heure croissante
            reservationsVague.sort((r1, r2) -> {
                boolean p1 = idsReservationsPrioritaires.contains(r1.getId());
                boolean p2 = idsReservationsPrioritaires.contains(r2.getId());
                if (p1 != p2) {
                    return p1 ? -1 : 1;
                }
                int cmpPassagers = Integer.compare(r2.getNbPassager(), r1.getNbPassager());
                if (cmpPassagers != 0) return cmpPassagers;
                Timestamp t1 = r1.getDateHeure();
                Timestamp t2 = r2.getDateHeure();
                if (t1 == null && t2 == null) return 0;
                if (t1 == null) return 1;
                if (t2 == null) return -1;
                return t1.compareTo(t2);
            });

                // Heure de base du départ = heure de la dernière réservation prise dans cette vague.
                Timestamp heureVague = reservationsVague.stream()
                    .map(Reservation::getDateHeure)
                    .filter(Objects::nonNull)
                    .max(Timestamp::compareTo)
                    .orElse(Timestamp.valueOf(cleVague + ":00"));

                // Intervalle de la vague (début/fin fenêtre d'attente)
                Timestamp[] intervalleVague = vagueIntervalles.get(cleVague);

                // Départ commun de la vague: si une voiture revient pendant la fenêtre,
                // on peut décaler le départ commun et re-évaluer l'optimal avec ce parc élargi.
                Timestamp heureDepartCommune = calculerHeureDepartCommuneVague(
                    heureVague,
                    intervalleVague,
                    toutesVoitures,
                    intervallesOccupes,
                    dateSimulation
                );

            // Filtrer les voitures disponibles : celles qui ne sont pas en trajet à l'heure de la vague
                long heureVagueMs = heureDepartCommune.getTime();
            List<Voiture> voituresDisponibles = new ArrayList<>();
            for (Voiture v : toutesVoitures) {
                long dispoVoitureMs = getDisponibiliteMsPourDate(v, dateSimulation);
                if (heureVagueMs < dispoVoitureMs) {
                    System.out.println("   ⏳ Voiture #" + v.getIdVoiture() + " (" + v.getRef() +
                                       ") indisponible avant " + new Timestamp(dispoVoitureMs));
                    continue;
                }

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

            // Etat des réservations de la vague (agrégé par ID, avec passagers restants)
            Map<Integer, Reservation> reservationsParId = new LinkedHashMap<>();
            Map<Integer, Integer> passagersRestantsParReservation = new HashMap<>();
            for (Reservation reservation : reservationsVague) {
                Reservation existe = reservationsParId.get(reservation.getId());
                if (existe == null) {
                    reservationsParId.put(reservation.getId(), reservation);
                    passagersRestantsParReservation.put(reservation.getId(), reservation.getNbPassager());
                } else {
                    int cumule = passagersRestantsParReservation.getOrDefault(reservation.getId(), 0)
                            + reservation.getNbPassager();
                    passagersRestantsParReservation.put(reservation.getId(), cumule);
                    if (reservation.getDateHeure() != null && existe.getDateHeure() != null
                            && reservation.getDateHeure().before(existe.getDateHeure())) {
                        reservationsParId.put(reservation.getId(), reservation);
                    }
                }
            }

            // Boucle principale: toujours choisir la réservation prioritaire, puis la terminer
            while (aDesPassagersRestants(passagersRestantsParReservation)) {
                Reservation reservationPrincipale = choisirReservationPrincipale(
                        reservationsParId,
                        passagersRestantsParReservation,
                        idsReservationsPrioritaires);

                if (reservationPrincipale == null) {
                    break;
                }

                int restantsPrincipale = passagersRestantsParReservation.getOrDefault(reservationPrincipale.getId(), 0);
                System.out.println("\n📌 Réservation prioritaire #" + reservationPrincipale.getId() +
                        " - " + restantsPrincipale + " passagers restants");

                boolean progressionPrincipale = false;

                while (passagersRestantsParReservation.getOrDefault(reservationPrincipale.getId(), 0) > 0) {
                    int demande = passagersRestantsParReservation.getOrDefault(reservationPrincipale.getId(), 0);
                    CandidatAffectation candidat = choisirCandidatPourFraction(
                            demande,
                            voituresDisponibles,
                            assignationsVague,
                            compteurTrajetsJour);

                    if (candidat == null || candidat.placesDisponibles <= 0) {
                        break;
                    }

                    int nbAffectes = Math.min(demande, candidat.placesDisponibles);
                    Reservation fractionPrincipale = clonerReservationAvecPassagers(reservationPrincipale, nbAffectes);

                    SimulationAssignation cible = candidat.assignationExistante;
                    if (cible == null) {
                        cible = new SimulationAssignation(candidat.voiture, heureDepartCommune);
                        assignationsVague.put(candidat.voiture.getIdVoiture(), cible);
                        System.out.println("   → Nouvelle voiture sélectionnée: #" + candidat.voiture.getIdVoiture() +
                                " (" + candidat.voiture.getRef() + ")");
                    }

                    cible.ajouterReservation(fractionPrincipale);
                    int nouveauRestantPrincipale = demande - nbAffectes;
                    passagersRestantsParReservation.put(reservationPrincipale.getId(), nouveauRestantPrincipale);
                    if (nouveauRestantPrincipale > 0) {
                        idsReservationsPrioritaires.add(reservationPrincipale.getId());
                    }
                    progressionPrincipale = true;

                    System.out.println("   ✅ " + nbAffectes + " passager(s) affecté(s) à Voiture #" +
                            cible.getVoiture().getIdVoiture() + " | Restants réservation: " + nouveauRestantPrincipale);

                    // Règle demandée: remplir la voiture avec la réservation la plus proche des places restantes
                    remplirAssignationAvecReservationProche(
                            cible,
                            reservationsParId,
                            passagersRestantsParReservation,
                            idsReservationsPrioritaires);
                }

                if (!progressionPrincipale) {
                    break;
                }
            }

            // Tout reliquat devient non assigné pour retraitement prioritaire en vague suivante
            for (Map.Entry<Integer, Reservation> entry : reservationsParId.entrySet()) {
                int idReservation = entry.getKey();
                int restants = passagersRestantsParReservation.getOrDefault(idReservation, 0);
                if (restants > 0) {
                    Reservation reliquat = clonerReservationAvecPassagers(entry.getValue(), restants);
                    reservationsNonAssignees.add(reliquat);
                    idsReservationsPrioritaires.add(idReservation);
                    System.out.println("   ❌ " + restants + " passager(s) non assigné(s) pour réservation #" + idReservation);
                }
            }

            // Incrémenter le compteur de trajets pour chaque voiture utilisée dans cette vague
            for (SimulationAssignation assignation : assignationsVague.values()) {
                int idVoiture = assignation.getVoiture().getIdVoiture();
                compteurTrajetsJour.put(idVoiture, compteurTrajetsJour.getOrDefault(idVoiture, 0) + 1);
            }

            // ÉTAPE 1: Calculer les heures de trajet initiales pour chaque assignation
            for (SimulationAssignation assignation : assignationsVague.values()) {
                if (intervalleVague != null) {
                    assignation.setDebutVague(intervalleVague[0]);
                    assignation.setFinFenetreVague(intervalleVague[1]);
                }
                assignation.setDateHeureDepart(heureDepartCommune);
                calculerHeuresTrajet(assignation, aeroport, vitesseMoyenne);
            }

            // ÉTAPE 2: Vérifier si des réassignations sont possibles pendant la fenêtre d'attente
            verifierReassignationPendantAttente(
                assignationsVague,
                intervalleVague,
                toutesVoitures,
                intervallesOccupes,
                compteurTrajetsJour,
                dateSimulation
            );

            // ÉTAPE 2.5: Traiter les réservations non assignées avec les voitures qui reviennent
            // Créer une copie temporaire des intervalles occupés avec les assignations de cette vague
            Map<Integer, List<long[]>> intervallesTempAvecVague = new HashMap<>();
            for (Map.Entry<Integer, List<long[]>> entry : intervallesOccupes.entrySet()) {
                intervallesTempAvecVague.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            for (SimulationAssignation sa : assignationsVague.values()) {
                long depMs = sa.getDateHeureDepart().getTime();
                long arrMs = sa.getDateHeureArrivee().getTime();
                intervallesTempAvecVague.computeIfAbsent(sa.getVoiture().getIdVoiture(), k -> new ArrayList<>())
                    .add(new long[]{depMs, arrMs});
            }

            // Liste pour stocker les assignations différées (trajets de retour)
            List<SimulationAssignation> assignationsDifferees = new ArrayList<>();

            // Tenter d'assigner les réservations non assignées aux voitures qui reviennent
            List<Reservation> reservationsTraitees = traiterReservationsNonAssigneesAvecVoituresRetour(
                reservationsNonAssignees,
                intervalleVague,
                heureDepartCommune,
                toutesVoitures,
                intervallesTempAvecVague,
                assignationsDifferees,  // Passer la liste des assignations différées
                compteurTrajetsJour,
                aeroport,
                vitesseMoyenne,
                dateSimulation
            );
            reservationsNonAssignees.removeAll(reservationsTraitees);

            // ÉTAPE 3: Recalculer les heures de trajet (si réassignation) et vérifier les chevauchements
            for (SimulationAssignation assignation : assignationsVague.values()) {
                // Recalculer les heures (au cas où la voiture a changé)
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
                    // Déplacer toutes les réservations vers non-assignées pour retraitement
                    for (Reservation r : assignation.getReservations()) {
                        reservationsNonAssignees.add(r);
                    }
                } else {
                    resultat.ajouterAssignation(assignation);
                    // Enregistrer le nouvel intervalle occupé
                    intervallesOccupes.computeIfAbsent(idVoitureAssignee, k -> new ArrayList<>())
                        .add(new long[]{departMs, arriveeMs});
                }
            }

            // ÉTAPE 3.5: Ajouter les assignations différées (trajets de retour) au résultat
            for (SimulationAssignation assignationDifferee : assignationsDifferees) {
                long departMs = assignationDifferee.getDateHeureDepart().getTime();
                long arriveeMs = assignationDifferee.getDateHeureArrivee().getTime();
                int idVoitureAssignee = assignationDifferee.getVoiture().getIdVoiture();

                // Vérifier chevauchement avec les intervalles existants
                boolean chevauchement = false;
                List<long[]> intervals = intervallesOccupes.get(idVoitureAssignee);
                if (intervals != null) {
                    for (long[] interval : intervals) {
                        if (departMs < interval[1] && interval[0] < arriveeMs) {
                            chevauchement = true;
                            System.out.println("   ⚠️ Voiture #" + idVoitureAssignee +
                                " chevauchement détecté (différé): trajet [" + assignationDifferee.getDateHeureDepart() +
                                " - " + assignationDifferee.getDateHeureArrivee() +
                                "] chevauche [" + new Timestamp(interval[0]) + " - " + new Timestamp(interval[1]) + "]");
                            break;
                        }
                    }
                }

                if (chevauchement) {
                    for (Reservation r : assignationDifferee.getReservations()) {
                        reservationsNonAssignees.add(r);
                    }
                } else {
                    resultat.ajouterAssignation(assignationDifferee);
                    intervallesOccupes.computeIfAbsent(idVoitureAssignee, k -> new ArrayList<>())
                        .add(new long[]{departMs, arriveeMs});
                }
            }

            numeroVague++;
            System.out.println();
        }

        // Traiter les réservations qui restent non assignées après toutes les vagues
        if (!reservationsNonAssignees.isEmpty()) {
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("❌ " + reservationsNonAssignees.size() + 
                               " réservation(s) définitivement non assignée(s)");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            for (Reservation r : reservationsNonAssignees) {
                System.out.println("   • Réservation #" + r.getId() + " - " + 
                                   r.getIdClient() + " (" + r.getNbPassager() + " passagers)");
                resultat.ajouterReservationNonAssignee(r);
            }
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
     * Sprint 6: Priorité au nombre de trajets (min)
     */
    private SimulationAssignation trouverMeilleureVoiture(
            Reservation reservation,
            List<Voiture> voituresDisponibles,
            Map<Integer, SimulationAssignation> assignationsVague,
            Timestamp heureVague,
            Map<Integer, Integer> compteurTrajetsJour) {

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

        // 5. SPRINT 6: Priorité au nombre de trajets (minimum) POUR LE JOUR MÊME
        // Utiliser le compteur passé en paramètre (base + simulation)
        Map<Integer, Integer> nbTrajetsParVoiture = new HashMap<>();
        for (Voiture v : voituresOptimales) {
            int nbTrajets = compteurTrajetsJour.getOrDefault(v.getIdVoiture(), 0);
            nbTrajetsParVoiture.put(v.getIdVoiture(), nbTrajets);
        }

        // Trouver le nombre minimum de trajets
        int nbTrajetsMin = nbTrajetsParVoiture.values().stream()
                .min(Integer::compare)
                .orElse(0);

        // Filtrer les voitures avec le nombre minimum de trajets
        List<Voiture> voituresAvecMinTrajets = voituresOptimales.stream()
                .filter(v -> nbTrajetsParVoiture.get(v.getIdVoiture()) == nbTrajetsMin)
                .collect(Collectors.toList());

        System.out.println("   → Écart minimal: " + ecartMin + " | Trajets min: " + nbTrajetsMin + 
                           " | Candidats: " + voituresAvecMinTrajets.size());

        // 6. Priorité carburant: D (Diesel) > H (Hybride) > E (Essence)
        Voiture voitureChoisie = choisirParCarburant(voituresAvecMinTrajets);

        System.out.println("   → Voiture choisie: #" + voitureChoisie.getIdVoiture() + 
                           " (" + voitureChoisie.getRef() + ") | Carburant: " + voitureChoisie.getCarburantLibelle() +
                           " | Trajets: " + nbTrajetsParVoiture.get(voitureChoisie.getIdVoiture()));

        // 7. Créer une nouvelle assignation
        SimulationAssignation assignation = new SimulationAssignation(voitureChoisie, heureVague);
        assignation.ajouterReservation(reservation);
        assignationsVague.put(voitureChoisie.getIdVoiture(), assignation);

        return assignation;
    }

        /**
         * Sélectionne le meilleur candidat pour une fraction de réservation.
         * Règles:
         * 1) place >= demande avec écart minimal
         * 2) sinon, place < demande avec place max (plus proche inférieur)
         * 3) tie-break: moins de trajets du jour, diesel, random
         */
        private CandidatAffectation choisirCandidatPourFraction(
            int nbPassagersRestants,
            List<Voiture> voituresDisponibles,
            Map<Integer, SimulationAssignation> assignationsVague,
            Map<Integer, Integer> compteurTrajetsJour) {

        List<CandidatAffectation> candidats = new ArrayList<>();

        // Candidats provenant des voitures déjà ouvertes dans la vague
        for (SimulationAssignation sa : assignationsVague.values()) {
            if (sa.getPlacesRestantes() > 0) {
            candidats.add(new CandidatAffectation(sa.getVoiture(), sa, sa.getPlacesRestantes()));
            }
        }

        // Candidats provenant des voitures non encore utilisées dans la vague
        for (Voiture v : voituresDisponibles) {
            if (!assignationsVague.containsKey(v.getIdVoiture()) && v.getCapacite() > 0) {
            candidats.add(new CandidatAffectation(v, null, v.getCapacite()));
            }
        }

        if (candidats.isEmpty()) {
            return null;
        }

        List<CandidatAffectation> superieurs = candidats.stream()
            .filter(c -> c.placesDisponibles >= nbPassagersRestants)
            .collect(Collectors.toList());

        List<CandidatAffectation> pool;
        if (!superieurs.isEmpty()) {
            int ecartMin = superieurs.stream()
                .mapToInt(c -> c.placesDisponibles - nbPassagersRestants)
                .min().orElse(0);
            pool = superieurs.stream()
                .filter(c -> (c.placesDisponibles - nbPassagersRestants) == ecartMin)
                .collect(Collectors.toList());
        } else {
            List<CandidatAffectation> inferieurs = candidats.stream()
                .filter(c -> c.placesDisponibles < nbPassagersRestants)
                .collect(Collectors.toList());
            if (inferieurs.isEmpty()) {
            return null;
            }
            int placeMax = inferieurs.stream()
                .mapToInt(c -> c.placesDisponibles)
                .max().orElse(0);
            pool = inferieurs.stream()
                .filter(c -> c.placesDisponibles == placeMax)
                .collect(Collectors.toList());
        }

        // Tie-break 1: nombre de trajets du jour (moins c'est mieux)
        int minTrajets = pool.stream()
            .mapToInt(c -> compteurTrajetsJour.getOrDefault(c.voiture.getIdVoiture(), 0)
                + (assignationsVague.containsKey(c.voiture.getIdVoiture()) ? 1 : 0))
            .min().orElse(0);
        List<CandidatAffectation> apresTrajets = pool.stream()
            .filter(c -> (compteurTrajetsJour.getOrDefault(c.voiture.getIdVoiture(), 0)
                + (assignationsVague.containsKey(c.voiture.getIdVoiture()) ? 1 : 0)) == minTrajets)
            .collect(Collectors.toList());

        // Tie-break 2: carburant prioritaire (D > H > E)
        int meilleurCarburant = apresTrajets.stream()
            .mapToInt(c -> getPrioriteCarburant(c.voiture.getCarburant()))
            .max().orElse(0);
        List<CandidatAffectation> apresCarburant = apresTrajets.stream()
            .filter(c -> getPrioriteCarburant(c.voiture.getCarburant()) == meilleurCarburant)
            .collect(Collectors.toList());

        // Tie-break 3: random
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
            if (restants != null && restants > 0) {
                return true;
            }
        }
        return false;
    }

    private Reservation choisirReservationPrincipale(
            Map<Integer, Reservation> reservationsParId,
            Map<Integer, Integer> passagersRestantsParReservation,
            Set<Integer> idsReservationsPrioritaires) {

        List<Reservation> candidates = new ArrayList<>();
        for (Reservation reservation : reservationsParId.values()) {
            if (passagersRestantsParReservation.getOrDefault(reservation.getId(), 0) > 0) {
                candidates.add(reservation);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        boolean existePrioritaire = candidates.stream().anyMatch(r -> idsReservationsPrioritaires.contains(r.getId()));
        if (existePrioritaire) {
            candidates.removeIf(r -> !idsReservationsPrioritaires.contains(r.getId()));
        }

        candidates.sort((r1, r2) -> {
            int p1 = passagersRestantsParReservation.getOrDefault(r1.getId(), 0);
            int p2 = passagersRestantsParReservation.getOrDefault(r2.getId(), 0);
            int cmpPassagers = Integer.compare(p2, p1);
            if (cmpPassagers != 0) {
                return cmpPassagers;
            }
            Timestamp t1 = r1.getDateHeure();
            Timestamp t2 = r2.getDateHeure();
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
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
            Reservation reservationRemplissage = choisirReservationPourRemplissage(
                    assignation.getPlacesRestantes(),
                    reservationsParId,
                    passagersRestantsParReservation,
                    idsReservationsPrioritaires);

            if (reservationRemplissage == null) {
                break;
            }

            int restants = passagersRestantsParReservation.getOrDefault(reservationRemplissage.getId(), 0);
            if (restants <= 0) {
                break;
            }

            int nbAffectes = Math.min(restants, assignation.getPlacesRestantes());
            Reservation fraction = clonerReservationAvecPassagers(reservationRemplissage, nbAffectes);
            assignation.ajouterReservation(fraction);

            int nouveauRestant = restants - nbAffectes;
            passagersRestantsParReservation.put(reservationRemplissage.getId(), nouveauRestant);

            System.out.println("   ↳ Remplissage: réservation #" + reservationRemplissage.getId() +
                    " +" + nbAffectes + " passager(s), restants réservation=" + nouveauRestant +
                    ", places restantes voiture=" + assignation.getPlacesRestantes());
        }
    }

    private Reservation choisirReservationPourRemplissage(
            int placesRestantes,
            Map<Integer, Reservation> reservationsParId,
            Map<Integer, Integer> passagersRestantsParReservation,
            Set<Integer> idsReservationsPrioritaires) {

        List<Reservation> candidates = new ArrayList<>();
        for (Reservation reservation : reservationsParId.values()) {
            if (passagersRestantsParReservation.getOrDefault(reservation.getId(), 0) > 0) {
                candidates.add(reservation);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        boolean existePrioritaire = candidates.stream().anyMatch(r -> idsReservationsPrioritaires.contains(r.getId()));
        if (existePrioritaire) {
            candidates.removeIf(r -> !idsReservationsPrioritaires.contains(r.getId()));
        }

        List<Reservation> superieursOuEgaux = new ArrayList<>();
        List<Reservation> inferieurs = new ArrayList<>();

        for (Reservation reservation : candidates) {
            int restants = passagersRestantsParReservation.getOrDefault(reservation.getId(), 0);
            if (restants >= placesRestantes) {
                superieursOuEgaux.add(reservation);
            } else {
                inferieurs.add(reservation);
            }
        }

        List<Reservation> pool = !superieursOuEgaux.isEmpty() ? superieursOuEgaux : inferieurs;
        if (pool.isEmpty()) {
            return null;
        }

        pool.sort((r1, r2) -> {
            int p1 = passagersRestantsParReservation.getOrDefault(r1.getId(), 0);
            int p2 = passagersRestantsParReservation.getOrDefault(r2.getId(), 0);

            int ecart1 = Math.abs(p1 - placesRestantes);
            int ecart2 = Math.abs(p2 - placesRestantes);
            int cmpEcart = Integer.compare(ecart1, ecart2);
            if (cmpEcart != 0) {
                return cmpEcart;
            }

            int cmpPassagers = Integer.compare(p2, p1);
            if (cmpPassagers != 0) {
                return cmpPassagers;
            }

            Timestamp t1 = r1.getDateHeure();
            Timestamp t2 = r2.getDateHeure();
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t1.compareTo(t2);
        });

        return pool.get(0);
    }

    /**
     * Compare deux voitures pour déterminer si la nouvelle est meilleure que l'actuelle
     * Critères (dans l'ordre): écart capacité → nombre trajets → carburant (D > H > E)
     *
     * @return true si nouvelleVoiture est strictement meilleure que voitureActuelle
     */
    private boolean estMeilleureVoiture(Voiture nouvelleVoiture, Voiture voitureActuelle,
            int nbPassagers, Map<Integer, Integer> compteurTrajetsJour) {

        // 1. Comparer l'écart de capacité
        int ecartNouvelle = nouvelleVoiture.getCapacite() - nbPassagers;
        int ecartActuelle = voitureActuelle.getCapacite() - nbPassagers;

        if (ecartNouvelle < ecartActuelle) return true;
        if (ecartNouvelle > ecartActuelle) return false;

        // 2. Comparer le nombre de trajets (moins = mieux)
        int trajetsNouvelle = compteurTrajetsJour.getOrDefault(nouvelleVoiture.getIdVoiture(), 0);
        int trajetsActuelle = compteurTrajetsJour.getOrDefault(voitureActuelle.getIdVoiture(), 0);

        if (trajetsNouvelle < trajetsActuelle) return true;
        if (trajetsNouvelle > trajetsActuelle) return false;

        // 3. Comparer le carburant (D > H > E)
        int prioriteNouvelle = getPrioriteCarburant(nouvelleVoiture.getCarburant());
        int prioriteActuelle = getPrioriteCarburant(voitureActuelle.getCarburant());

        return prioriteNouvelle > prioriteActuelle;
    }

    /**
     * Retourne la priorité du carburant (plus c'est haut, mieux c'est)
     */
    private int getPrioriteCarburant(String carburant) {
        if ("D".equals(carburant)) return 3;
        if ("H".equals(carburant)) return 2;
        if ("E".equals(carburant)) return 1;
        return 0;
    }

    /**
     * Vérifie si des réassignations sont possibles pendant la fenêtre d'attente.
     *
     * Si une meilleure voiture devient disponible pendant la fenêtre d'attente
     * (avant l'heure de départ d'une assignation), on réassigne.
     *
     * @param assignationsVague      Les assignations de la vague actuelle
     * @param intervalleVague        [debutFenetre, finFenetre] de la vague
     * @param toutesVoitures         Toutes les voitures du système
     * @param intervallesOccupes     Intervalles occupés par voiture
     * @param compteurTrajetsJour    Compteur de trajets par voiture pour le jour
     * @param assignationsVagueIds   IDs des voitures déjà utilisées dans la vague
     */
    private void verifierReassignationPendantAttente(
            Map<Integer, SimulationAssignation> assignationsVague,
            Timestamp[] intervalleVague,
            List<Voiture> toutesVoitures,
            Map<Integer, List<long[]>> intervallesOccupes,
            Map<Integer, Integer> compteurTrajetsJour,
            Date dateSimulation) {

        if (intervalleVague == null || assignationsVague.isEmpty()) {
            return;
        }

        long debutFenetreMs = intervalleVague[0].getTime();
        long finFenetreMs = intervalleVague[1].getTime();

        // Pour chaque assignation de la vague
        for (SimulationAssignation assignation : assignationsVague.values()) {
            Voiture voitureActuelle = assignation.getVoiture();
            long heureDepartMs = assignation.getDateHeureDepart().getTime();

            // Calculer le nombre total de passagers de cette assignation
            int nbPassagersTotal = assignation.getReservations().stream()
                    .mapToInt(Reservation::getNbPassager)
                    .sum();

            // Chercher les voitures qui deviennent disponibles dans [debutFenetre, heureDepart[
            for (Voiture voitureCandidate : toutesVoitures) {
                // Ignorer la voiture actuelle
                if (voitureCandidate.getIdVoiture() == voitureActuelle.getIdVoiture()) {
                    continue;
                }

                // Ignorer les voitures déjà utilisées dans cette vague
                if (assignationsVague.containsKey(voitureCandidate.getIdVoiture())) {
                    continue;
                }

                // Vérifier la capacité
                if (voitureCandidate.getCapacite() < nbPassagersTotal) {
                    continue;
                }

                // Trouver le moment où cette voiture devient disponible
                long heureDispoTrajetMs = trouverHeureDisponibilite(voitureCandidate, intervallesOccupes, debutFenetreMs);
                long heureDispoInitialeMs = getDisponibiliteMsPourDate(voitureCandidate, dateSimulation);
                long heureDispoMs = Math.max(heureDispoTrajetMs, heureDispoInitialeMs);

                // La voiture doit devenir disponible APRÈS le début de la fenêtre
                // et AVANT l'heure de départ de l'assignation
                if (heureDispoMs > debutFenetreMs && heureDispoMs < heureDepartMs) {
                    // Vérifier si cette voiture est meilleure
                    if (estMeilleureVoiture(voitureCandidate, voitureActuelle, nbPassagersTotal, compteurTrajetsJour)) {
                        System.out.println("   🔄 RÉASSIGNATION: Voiture #" + voitureActuelle.getIdVoiture() +
                                " (" + voitureActuelle.getRef() + ", " + voitureActuelle.getCarburantLibelle() +
                                ") → Voiture #" + voitureCandidate.getIdVoiture() +
                                " (" + voitureCandidate.getRef() + ", " + voitureCandidate.getCarburantLibelle() +
                                ") disponible à " + new Timestamp(heureDispoMs));

                        // Mettre à jour le compteur de trajets
                        compteurTrajetsJour.put(voitureActuelle.getIdVoiture(),
                                compteurTrajetsJour.getOrDefault(voitureActuelle.getIdVoiture(), 1) - 1);
                        compteurTrajetsJour.put(voitureCandidate.getIdVoiture(),
                                compteurTrajetsJour.getOrDefault(voitureCandidate.getIdVoiture(), 0) + 1);

                        // Réassigner
                        assignation.setVoiture(voitureCandidate);
                        voitureActuelle = voitureCandidate;
                    }
                }
            }
        }
    }

    /**
     * Trouve l'heure à laquelle une voiture devient disponible (fin de son dernier intervalle occupé)
     * Si la voiture n'a pas d'intervalle chevauchant avec la fenêtre, retourne 0 (disponible immédiatement)
     */
    private long trouverHeureDisponibilite(Voiture voiture, Map<Integer, List<long[]>> intervallesOccupes, long debutFenetreMs) {
        List<long[]> intervals = intervallesOccupes.get(voiture.getIdVoiture());
        if (intervals == null || intervals.isEmpty()) {
            return 0; // Disponible depuis le début
        }

        long heureDispoDerniere = 0;
        for (long[] interval : intervals) {
            // Si l'intervalle se termine APRÈS le début de la fenêtre
            if (interval[1] > debutFenetreMs) {
                // La voiture devient disponible à la fin de cet intervalle
                if (interval[1] > heureDispoDerniere) {
                    heureDispoDerniere = interval[1];
                }
            }
        }

        return heureDispoDerniere;
    }

    /**
     * Calcule l'heure de départ commune d'une vague.
     *
     * Règle métier: si une voiture revient pendant la fenêtre d'attente,
     * on peut retarder le départ commun à ce retour pour reconsidérer l'optimal.
     */
    private Timestamp calculerHeureDepartCommuneVague(
            Timestamp heureVague,
            Timestamp[] intervalleVague,
            List<Voiture> toutesVoitures,
            Map<Integer, List<long[]>> intervallesOccupes,
            Date dateSimulation) {

        if (intervalleVague == null) {
            return heureVague;
        }

        long departInitialMs = heureVague.getTime();
        long finFenetreMs = intervalleVague[1].getTime();
        long meilleurDepartMs = departInitialMs;
        long retourLePlusTot = Long.MAX_VALUE;

        for (Voiture voiture : toutesVoitures) {
            long heureDispoTrajet = trouverHeureDisponibilite(voiture, intervallesOccupes, departInitialMs);
            long heureDispoInitiale = getDisponibiliteMsPourDate(voiture, dateSimulation);
            long heureDispo = Math.max(heureDispoTrajet, heureDispoInitiale);
            if (heureDispo > departInitialMs && heureDispo <= finFenetreMs) {
                if (heureDispo < retourLePlusTot) {
                    retourLePlusTot = heureDispo;
                }
            }
        }

        if (retourLePlusTot != Long.MAX_VALUE) {
            meilleurDepartMs = retourLePlusTot;
            System.out.println("   ⏱️ Départ commun décalé à " + new Timestamp(meilleurDepartMs)
                    + " (retour voiture pendant la fenêtre)");
        }

        return new Timestamp(meilleurDepartMs);
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

    /**
     * Traite les réservations non assignées en vérifiant si des voitures reviennent dans la fenêtre d'attente.
     *
     * @return Liste des réservations qui ont pu être assignées
     */
    private List<Reservation> traiterReservationsNonAssigneesAvecVoituresRetour(
            List<Reservation> reservationsNonAssignees,
            Timestamp[] intervalleVague,
            Timestamp heureDepartCommune,
            List<Voiture> toutesVoitures,
            Map<Integer, List<long[]>> intervallesOccupes,
            List<SimulationAssignation> assignationsDifferees,  // Liste pour stocker les nouvelles assignations
            Map<Integer, Integer> compteurTrajetsJour,
            Lieu aeroport,
            double vitesseMoyenne,
            Date dateSimulation) {

        List<Reservation> reservationsTraitees = new ArrayList<>();

        if (intervalleVague == null || reservationsNonAssignees.isEmpty()) {
            return reservationsTraitees;
        }

        long debutFenetreMs = intervalleVague[0].getTime();
        long departCommunMs = heureDepartCommune.getTime();

        // Trier les réservations non assignées par nombre de passagers décroissant puis date_heure croissante
        List<Reservation> reservationsATraiter = new ArrayList<>(reservationsNonAssignees);
        reservationsATraiter.sort((r1, r2) -> {
            int cmpPassagers = Integer.compare(r2.getNbPassager(), r1.getNbPassager());
            if (cmpPassagers != 0) {
                return cmpPassagers;
            }
            Timestamp t1 = r1.getDateHeure();
            Timestamp t2 = r2.getDateHeure();
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t1.compareTo(t2);
        });

        for (Reservation reservation : reservationsATraiter) {
            int nbPassagers = reservation.getNbPassager();

            // Chercher les voitures qui reviennent dans la fenêtre d'attente
            List<VoitureDisponibilite> voituresQuiReviennent = new ArrayList<>();

            for (Voiture voiture : toutesVoitures) {
                // Ignorer les voitures sans capacité suffisante
                if (voiture.getCapacite() < nbPassagers) {
                    continue;
                }

                long heureDispoTrajetMs = trouverHeureDisponibilite(voiture, intervallesOccupes, debutFenetreMs);
                long heureDispoInitialeMs = getDisponibiliteMsPourDate(voiture, dateSimulation);
                long heureDispoMs = Math.max(heureDispoTrajetMs, heureDispoInitialeMs);

                // La voiture doit être disponible au plus tard à l'heure de départ commune.
                if (heureDispoMs > debutFenetreMs && heureDispoMs <= departCommunMs) {
                    voituresQuiReviennent.add(new VoitureDisponibilite(voiture, heureDispoMs));
                }
            }

            if (voituresQuiReviennent.isEmpty()) {
                continue;
            }

            // Trier par règles métier habituelles (capacité -> trajets -> carburant -> random)
            voituresQuiReviennent.sort((v1, v2) -> {
                // Écart capacité
                int ecart1 = v1.voiture.getCapacite() - nbPassagers;
                int ecart2 = v2.voiture.getCapacite() - nbPassagers;
                int cmpEcart = Integer.compare(ecart1, ecart2);
                if (cmpEcart != 0) return cmpEcart;

                // Nombre de trajets
                int trajets1 = compteurTrajetsJour.getOrDefault(v1.voiture.getIdVoiture(), 0);
                int trajets2 = compteurTrajetsJour.getOrDefault(v2.voiture.getIdVoiture(), 0);
                int cmpTrajets = Integer.compare(trajets1, trajets2);
                if (cmpTrajets != 0) return cmpTrajets;

                // Carburant (D > H > E)
                return Integer.compare(getPrioriteCarburant(v2.voiture.getCarburant()),
                                       getPrioriteCarburant(v1.voiture.getCarburant()));
            });

            // Prendre la meilleure voiture
            VoitureDisponibilite meilleure = voituresQuiReviennent.get(0);
            Voiture voitureChoisie = meilleure.voiture;
                Timestamp heureDepart = heureDepartCommune;

            System.out.println("\n   🔄 ASSIGNATION DIFFÉRÉE: Réservation #" + reservation.getId() +
                    " (" + reservation.getIdClient() + ") → Voiture #" + voitureChoisie.getIdVoiture() +
                    " (" + voitureChoisie.getRef() + ", " + voitureChoisie.getCarburantLibelle() +
                    ") disponible, départ commun à " + heureDepart);

            // Créer une NOUVELLE assignation pour ce trajet différé (trajet de retour)
                SimulationAssignation nouvelleAssignation = new SimulationAssignation(voitureChoisie, heureDepartCommune);
            nouvelleAssignation.setDateHeureDepart(heureDepart); // Définir l'heure de départ = retour voiture
            nouvelleAssignation.ajouterReservation(reservation);
            nouvelleAssignation.setDebutVague(intervalleVague[0]);
            nouvelleAssignation.setFinFenetreVague(intervalleVague[1]);
            assignationsDifferees.add(nouvelleAssignation);  // Ajouter à la liste des assignations différées

            // Calculer les heures de trajet
            calculerHeuresTrajet(nouvelleAssignation, aeroport, vitesseMoyenne);

            // Mettre à jour les intervalles occupés
            long departMs = nouvelleAssignation.getDateHeureDepart().getTime();
            long arriveeMs = nouvelleAssignation.getDateHeureArrivee().getTime();
            intervallesOccupes.computeIfAbsent(voitureChoisie.getIdVoiture(), k -> new ArrayList<>())
                .add(new long[]{departMs, arriveeMs});

            // Incrémenter le compteur de trajets
            compteurTrajetsJour.put(voitureChoisie.getIdVoiture(),
                compteurTrajetsJour.getOrDefault(voitureChoisie.getIdVoiture(), 0) + 1);

            reservationsTraitees.add(reservation);
        }

        return reservationsTraitees;
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
     * - Départ = MAX(date_heure de la DERNIÈRE réservation, heure de retour de la voiture si déjà définie)
     * - Route: aéroport -> lieu le plus proche -> lieu suivant -> ... -> dernier lieu -> retour aéroport
     * - Si distances similaires, ordre alphabétique du lieu
     */
    private void calculerHeuresTrajet(SimulationAssignation assignation, Lieu aeroport, double vitesseMoyenne) {
        // SPRINT 6: L'heure de départ = date_heure de la DERNIÈRE réservation de cette voiture
        Timestamp departReservation = assignation.getReservations().stream()
                .map(Reservation::getDateHeure)
                .max(Timestamp::compareTo)
                .orElse(assignation.getHeureVague());

        // Si une heure de départ est déjà définie (ex: voiture qui revient), prendre le MAX
        Timestamp departExistant = assignation.getDateHeureDepart();
        Timestamp depart = (departExistant != null && departExistant.after(departReservation))
                ? departExistant
                : departReservation;

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
     * Regroupe les réservations par vague selon le temps d'attente (SPRINT 6).
     * 
     * LOGIQUE:
     * - Vague commence à la première réservation non traitée
     * - Fenêtre de traitement = [première_réservation, première_réservation + temps_attente]
     * - Toutes les réservations dans cette fenêtre font partie de la même vague
     * - La vague suivante commence à la première réservation APRÈS la fenêtre
     * 
     * IMPORTANT: Chaque voiture part à l'heure de SA dernière réservation (calculé dans calculerHeuresTrajet)
     * 
     * Exemple:
     * - MATIN001: 06:00 → Ouvre fenêtre [06:00, 06:30]
     * - MATIN002: 06:30 → Dans la fenêtre, même vague
     * - MATIN003: 07:00 → Hors fenêtre, nouvelle vague
     * 
     * Si VOI0004 a MATIN001 (06:00) + MATIN003 (07:00), elle part à 07:00
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
            // La première réservation ouvre la fenêtre de traitement
            Timestamp debutFenetre = triees.get(i).getDateHeure();
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

            // La clé de la vague = fenêtre de traitement (pour affichage)
            String cleVague = String.format("%tF %tH:%tM", debutFenetre, debutFenetre, debutFenetre);
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
