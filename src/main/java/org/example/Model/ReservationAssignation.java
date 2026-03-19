package org.example.Model;

public class ReservationAssignation {
    private int id;
    private int idAssignation;
    private int idReservation;
    private int ordreItineraire;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdAssignation() { return idAssignation; }
    public void setIdAssignation(int idAssignation) { this.idAssignation = idAssignation; }

    public int getIdReservation() { return idReservation; }
    public void setIdReservation(int idReservation) { this.idReservation = idReservation; }

    public int getOrdreItineraire() { return ordreItineraire; }
    public void setOrdreItineraire(int ordreItineraire) { this.ordreItineraire = ordreItineraire; }

    @Override
    public String toString() {
        return "ReservationAssignation{" +
                "id=" + id +
                ", idAssignation=" + idAssignation +
                ", idReservation=" + idReservation +
                ", ordreItineraire=" + ordreItineraire +
                '}';
    }
}
