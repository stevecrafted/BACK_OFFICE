package org.example.Model;

import java.sql.Timestamp;

public class Reservation {
    private int id;
    private int idLieu;
    private String idClient;
    private int nbPassager;
    private Timestamp dateHeure;

    // Constructeurs
    public Reservation() {}

    public Reservation(int idLieu, String idClient, int nbPassager) {
        this.idLieu = idLieu;
        this.idClient = idClient;
        this.nbPassager = nbPassager;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdLieu() { return idLieu; }
    public void setIdLieu(int idLieu) { this.idLieu = idLieu; }

    public String getIdClient() { return idClient; }
    public void setIdClient(String idClient) { this.idClient = idClient; }

    public int getNbPassager() { return nbPassager; }
    public void setNbPassager(int nbPassager) { this.nbPassager = nbPassager; }

    public Timestamp getDateHeure() { return dateHeure; }
    public void setDateHeure(Timestamp dateHeure) { this.dateHeure = dateHeure; }

    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", idLieu=" + idLieu +
                ", idClient='" + idClient + '\'' +
                ", nbPassager=" + nbPassager +
                ", dateHeure=" + dateHeure +
                '}';
    }
}
