package org.example.Model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Assignation {
    private int id;
    private int idVoiture;
    private Timestamp dateHeureDepart;
    private Timestamp dateHeureArrivee;
    private List<ReservationAssignation> reservationAssignations;

    // Constructeurs
    public Assignation() {
        this.reservationAssignations = new ArrayList<>();
    }

    public Assignation(int idVoiture, Timestamp dateHeureDepart, Timestamp dateHeureArrivee) {
        this.idVoiture = idVoiture;
        this.dateHeureDepart = dateHeureDepart;
        this.dateHeureArrivee = dateHeureArrivee;
        this.reservationAssignations = new ArrayList<>();
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdVoiture() { return idVoiture; }
    public void setIdVoiture(int idVoiture) { this.idVoiture = idVoiture; }

    public Timestamp getDateHeureDepart() { return dateHeureDepart; }
    public void setDateHeureDepart(Timestamp dateHeureDepart) { this.dateHeureDepart = dateHeureDepart; }

    public Timestamp getDateHeureArrivee() { return dateHeureArrivee; }
    public void setDateHeureArrivee(Timestamp dateHeureArrivee) { this.dateHeureArrivee = dateHeureArrivee; }

    public List<ReservationAssignation> getReservationAssignations() { return reservationAssignations; }
    public void setReservationAssignations(List<ReservationAssignation> reservationAssignations) {
        this.reservationAssignations = reservationAssignations;
    }

    public void ajouterReservationAssignation(int idReservation, int ordreItineraire) {
        ReservationAssignation ra = new ReservationAssignation();
        ra.setIdReservation(idReservation);
        ra.setOrdreItineraire(ordreItineraire);
        this.reservationAssignations.add(ra);
    }

    @Override
    public String toString() {
        return "Assignation{" +
                "id=" + id +
                ", idVoiture=" + idVoiture +
                ", dateHeureDepart=" + dateHeureDepart +
                ", dateHeureArrivee=" + dateHeureArrivee +
                ", reservationAssignations=" + reservationAssignations +
                '}';
    }
}
