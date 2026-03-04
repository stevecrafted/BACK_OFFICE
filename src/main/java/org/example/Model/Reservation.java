package org.example.Model;

import java.sql.Timestamp;

public class Reservation {
    private int id;
    private int idHotel;
    private String idClient;
    private int nbPassager;
    private Timestamp dateHeure;
 // véhicule assigné par la planification (nullable)

    // Constructeurs
    public Reservation() {}

    public Reservation(int idHotel, String idClient, int nbPassager) {
        this.idHotel = idHotel;
        this.idClient = idClient;
        this.nbPassager = nbPassager;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdHotel() { return idHotel; }
    public void setIdHotel(int idHotel) { this.idHotel = idHotel; }

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
                ", idHotel=" + idHotel +
                ", idClient='" + idClient + '\'' +
                ", nbPassager=" + nbPassager +
                ", dateHeure=" + dateHeure +
                
                '}';
    }
}
