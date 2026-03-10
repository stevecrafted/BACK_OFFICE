package org.example.Model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Représente une simulation d'assignation (pas encore enregistrée en base)
 * Contient la voiture, ses réservations et l'itinéraire calculé
 */
public class SimulationAssignation {
    private Voiture voiture;
    private List<Reservation> reservations;
    private int placesRestantes;
    private Timestamp heureVague;

    // Itinéraire calculé (Aéroport → Hôtels → Aéroport)
    private List<EtapeItineraire> itineraire;
    private double distanceTotale;
    private Timestamp heureDepart;
    private Timestamp heureRetour;

    public SimulationAssignation(Voiture voiture, Timestamp heureVague) {
        this.voiture = voiture;
        this.reservations = new ArrayList<>();
        this.placesRestantes = voiture.getCapacite();
        this.heureVague = heureVague;
        this.itineraire = new ArrayList<>();
        this.distanceTotale = 0;
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

    // Itinéraire
    public List<EtapeItineraire> getItineraire() { return itineraire; }
    public void setItineraire(List<EtapeItineraire> itineraire) { this.itineraire = itineraire; }

    public double getDistanceTotale() { return distanceTotale; }
    public void setDistanceTotale(double distanceTotale) { this.distanceTotale = distanceTotale; }

    public Timestamp getHeureDepart() { return heureDepart; }
    public void setHeureDepart(Timestamp heureDepart) { this.heureDepart = heureDepart; }

    public Timestamp getHeureRetour() { return heureRetour; }
    public void setHeureRetour(Timestamp heureRetour) { this.heureRetour = heureRetour; }
}
