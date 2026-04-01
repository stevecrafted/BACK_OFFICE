package org.example.Model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Représente une simulation d'assignation (pas encore enregistrée en base)
 */
public class SimulationAssignation {
    private Voiture voiture;
    private List<Reservation> reservations;
    private int placesRestantes;
    private Timestamp heureVague;
    private Timestamp dateHeureDepart;
    private Timestamp dateHeureArrivee;
    private Timestamp debutVague;
    private Timestamp finFenetreVague;
    private List<EtapeItineraire> itineraire = new ArrayList<>();
    
    // Map pour stocker le nombre de passagers réellement assignés par réservation (id -> nbPassagers assignés)
    private Map<Integer, Integer> passagersParReservation = new HashMap<>();

    public SimulationAssignation(Voiture voiture, Timestamp heureVague) {
        this.voiture = voiture;
        this.reservations = new ArrayList<>();
        this.placesRestantes = voiture.getCapacite();
        this.heureVague = heureVague;
    }

    public boolean peutAccueillir(int nbPassagers) {
        return placesRestantes >= nbPassagers;
    }

    /**
     * Ajoute une réservation à l'assignation.
     * Le nombre de passagers de la réservation correspond aux passagers à prendre dans ce véhicule.
     */
    public void ajouterReservation(Reservation reservation) {
        int passagersAPrendre = Math.min(reservation.getNbPassager(), placesRestantes);
        
        // Stocker le nombre de passagers pris pour cette réservation
        passagersParReservation.put(reservation.getId(), passagersAPrendre);
        
        reservations.add(reservation);
        placesRestantes -= passagersAPrendre;
    }
    
    /**
     * Retourne le nombre de passagers assignés pour une réservation donnée
     */
    public int getPassagersAssignes(int idReservation) {
        return passagersParReservation.getOrDefault(idReservation, 0);
    }
    
    /**
     * Retourne la map complète des passagers par réservation
     */
    public Map<Integer, Integer> getPassagersParReservation() {
        return passagersParReservation;
    }

    public int getEcartPlaces(int nbPassagers) {
        return Math.abs(placesRestantes - nbPassagers);
    }

    public boolean isPleine() {
        return placesRestantes == 0;
    }

    // Getters
    public Voiture getVoiture() { return voiture; }
    public void setVoiture(Voiture voiture) {
        this.voiture = voiture;
        // Recalculer les places restantes avec la nouvelle capacité
        int passagersActuels = passagersParReservation.values().stream().mapToInt(Integer::intValue).sum();
        this.placesRestantes = voiture.getCapacite() - passagersActuels;
    }
    
    /**
     * Retourne le nombre total de passagers assignés dans cette assignation
     */
    public int getTotalPassagers() {
        return passagersParReservation.values().stream().mapToInt(Integer::intValue).sum();
    }
    public List<Reservation> getReservations() { return reservations; }
    public int getPlacesRestantes() { return placesRestantes; }
    public Timestamp getHeureVague() { return heureVague; }
    public int getNbReservations() { return reservations.size(); }

    public Timestamp getDateHeureDepart() { return dateHeureDepart; }
    public void setDateHeureDepart(Timestamp dateHeureDepart) { this.dateHeureDepart = dateHeureDepart; }

    public Timestamp getDateHeureArrivee() { return dateHeureArrivee; }
    public void setDateHeureArrivee(Timestamp dateHeureArrivee) { this.dateHeureArrivee = dateHeureArrivee; }

    public Timestamp getDebutVague() { return debutVague; }
    public void setDebutVague(Timestamp debutVague) { this.debutVague = debutVague; }

    public Timestamp getFinFenetreVague() { return finFenetreVague; }
    public void setFinFenetreVague(Timestamp finFenetreVague) { this.finFenetreVague = finFenetreVague; }

    public List<EtapeItineraire> getItineraire() { return itineraire; }
    public void setItineraire(List<EtapeItineraire> itineraire) { this.itineraire = itineraire; }

    public double getDistanceTotale() {
        return itineraire.stream().mapToDouble(EtapeItineraire::getDistanceKm).sum();
    }

    public double getDureeTotaleMinutes() {
        return itineraire.stream().mapToDouble(EtapeItineraire::getDureeMinutes).sum();
    }
}
