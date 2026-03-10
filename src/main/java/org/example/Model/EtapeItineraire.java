package org.example.Model;

import java.sql.Timestamp;

/**
 * Représente une étape dans l'itinéraire d'une voiture
 * (ex: Aéroport → Colbert, Colbert → Novotel, etc.)
 */
public class EtapeItineraire {
    private Hotel lieuDepart;
    private Hotel lieuArrivee;
    private double distanceKm;
    private Timestamp heureDepart;
    private Timestamp heureArrivee;

    public EtapeItineraire(Hotel lieuDepart, Hotel lieuArrivee, double distanceKm,
                           Timestamp heureDepart, Timestamp heureArrivee) {
        this.lieuDepart = lieuDepart;
        this.lieuArrivee = lieuArrivee;
        this.distanceKm = distanceKm;
        this.heureDepart = heureDepart;
        this.heureArrivee = heureArrivee;
    }

    // Getters
    public Hotel getLieuDepart() { return lieuDepart; }
    public Hotel getLieuArrivee() { return lieuArrivee; }
    public double getDistanceKm() { return distanceKm; }
    public Timestamp getHeureDepart() { return heureDepart; }
    public Timestamp getHeureArrivee() { return heureArrivee; }

    @Override
    public String toString() {
        return lieuDepart.getNom() + " → " + lieuArrivee.getNom() +
               " (" + distanceKm + " km, départ: " + heureDepart + ", arrivée: " + heureArrivee + ")";
    }
}
