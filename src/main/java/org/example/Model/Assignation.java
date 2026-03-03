package org.example.Model;

import java.sql.Timestamp;

public class Assignation {
    private int id;
    private int idVoiture;
    private int idReservation;
    private Timestamp dateAssignation;

    // Constructeurs
    public Assignation() {}

    public Assignation(int idVoiture, int idReservation) {
        this.idVoiture = idVoiture;
        this.idReservation = idReservation;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdVoiture() { return idVoiture; }
    public void setIdVoiture(int idVoiture) { this.idVoiture = idVoiture; }

    public int getIdReservation() { return idReservation; }
    public void setIdReservation(int idReservation) { this.idReservation = idReservation; }

    public Timestamp getDateAssignation() { return dateAssignation; }
    public void setDateAssignation(Timestamp dateAssignation) { this.dateAssignation = dateAssignation; }

    @Override
    public String toString() {
        return "Assignation{" +
                "id=" + id +
                ", idVoiture=" + idVoiture +
                ", idReservation=" + idReservation +
                ", dateAssignation=" + dateAssignation +
                '}';
    }
}
