package org.example.Model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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
    private List<EtapeItineraire> itineraire = new ArrayList<>();

    public SimulationAssignation(Voiture voiture, Timestamp heureVague) {
        this.voiture = voiture;
        this.reservations = new ArrayList<>();
        this.placesRestantes = voiture.getCapacite();
        this.heureVague = heureVague;
    }

    public boolean peutAccueillir(int nbPassagers) {
        return placesRestantes >= nbPassagers;
    }

    public void ajouterReservation(Reservation reservation) {
        reservations.add(reservation);
        placesRestantes -= reservation.getNbPassager();
    }

    public int getEcartPlaces(int nbPassagers) {
        return Math.abs(placesRestantes - nbPassagers);
    }

    // Getters
    public Voiture getVoiture() { return voiture; }
    public List<Reservation> getReservations() { return reservations; }
    public int getPlacesRestantes() { return placesRestantes; }
    public Timestamp getHeureVague() { return heureVague; }
    public int getNbReservations() { return reservations.size(); }

    public Timestamp getDateHeureDepart() { return dateHeureDepart; }
    public void setDateHeureDepart(Timestamp dateHeureDepart) { this.dateHeureDepart = dateHeureDepart; }

    public Timestamp getDateHeureArrivee() { return dateHeureArrivee; }
    public void setDateHeureArrivee(Timestamp dateHeureArrivee) { this.dateHeureArrivee = dateHeureArrivee; }

    public List<EtapeItineraire> getItineraire() { return itineraire; }
    public void setItineraire(List<EtapeItineraire> itineraire) { this.itineraire = itineraire; }

    public double getDistanceTotale() {
        return itineraire.stream().mapToDouble(EtapeItineraire::getDistanceKm).sum();
    }

    public double getDureeTotaleMinutes() {
        return itineraire.stream().mapToDouble(EtapeItineraire::getDureeMinutes).sum();
    }
}
