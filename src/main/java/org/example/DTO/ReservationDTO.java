package org.example.DTO;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class ReservationDTO {
    private int id;
    private int idLieu;
    private String idClient;
    private int nbPassager;
    private String dateHeure; // Format String au lieu de Timestamp
    private String dateHeureFormattee; // Format lisible

    // Constructeurs
    public ReservationDTO() {}

    public ReservationDTO(int id, int idLieu, String idClient, int nbPassager, Timestamp dateHeure) {
        this.id = id;
        this.idLieu = idLieu;
        this.idClient = idClient;
        this.nbPassager = nbPassager;
        
        // Convertir Timestamp en String
        if (dateHeure != null) {
            this.dateHeure = dateHeure.toString(); // Format ISO
            this.dateHeureFormattee = formatTimestamp(dateHeure); // Format lisible
        }
    }

    // Méthode pour formater le Timestamp
    private String formatTimestamp(Timestamp timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            return sdf.format(timestamp);
        } catch (Exception e) {
            return timestamp.toString();
        }
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

    public String getDateHeure() { return dateHeure; }
    public void setDateHeure(String dateHeure) { this.dateHeure = dateHeure; }

    public String getDateHeureFormattee() { return dateHeureFormattee; }
    public void setDateHeureFormattee(String dateHeureFormattee) { 
        this.dateHeureFormattee = dateHeureFormattee; 
    }

    @Override
    public String toString() {
        return "ReservationDTO{" +
                "id=" + id +
                ", idHotel=" + idLieu +
                ", idClient='" + idClient + '\'' +
                ", nbPassager=" + nbPassager +
                ", dateHeure='" + dateHeure + '\'' +
                ", dateHeureFormattee='" + dateHeureFormattee + '\'' +
                '}';
    }
}