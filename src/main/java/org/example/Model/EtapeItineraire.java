package org.example.Model;

public class EtapeItineraire {
    private int ordre;
    private String lieuDepart;
    private String lieuArrivee;
    private double distanceKm;
    private double dureeMinutes;

    public EtapeItineraire(int ordre, String lieuDepart, String lieuArrivee, double distanceKm, double dureeMinutes) {
        this.ordre = ordre;
        this.lieuDepart = lieuDepart;
        this.lieuArrivee = lieuArrivee;
        this.distanceKm = distanceKm;
        this.dureeMinutes = dureeMinutes;
    }

    public int getOrdre() { return ordre; }
    public String getLieuDepart() { return lieuDepart; }
    public String getLieuArrivee() { return lieuArrivee; }
    public double getDistanceKm() { return distanceKm; }
    public double getDureeMinutes() { return dureeMinutes; }
}
