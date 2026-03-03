package org.example.Model;

import java.sql.Timestamp;

public class Voyage {
    private int idVoyage;
    private int idReservation;
    private Timestamp dateHeureDepart;
    private Timestamp dateHeureArrivee;

    // Constructeurs
    public Voyage() {}

    public Voyage(int idReservation, Timestamp dateHeureDepart, Timestamp dateHeureArrivee) {
        this.idReservation = idReservation;
        this.dateHeureDepart = dateHeureDepart;
        this.dateHeureArrivee = dateHeureArrivee;
    }

    // Getters et Setters
    public int getIdVoyage() { return idVoyage; }
    public void setIdVoyage(int idVoyage) { this.idVoyage = idVoyage; }

    public int getIdReservation() { return idReservation; }
    public void setIdReservation(int idReservation) { this.idReservation = idReservation; }

    public Timestamp getDateHeureDepart() { return dateHeureDepart; }
    public void setDateHeureDepart(Timestamp dateHeureDepart) { this.dateHeureDepart = dateHeureDepart; }

    public Timestamp getDateHeureArrivee() { return dateHeureArrivee; }
    public void setDateHeureArrivee(Timestamp dateHeureArrivee) { this.dateHeureArrivee = dateHeureArrivee; }

    @Override
    public String toString() {
        return "Voyage{" +
                "idVoyage=" + idVoyage +
                ", idReservation=" + idReservation +
                ", dateHeureDepart=" + dateHeureDepart +
                ", dateHeureArrivee=" + dateHeureArrivee +
                '}';
    }
}
