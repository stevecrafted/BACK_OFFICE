package org.example.DTO;

public class VoitureDTO {
    private int idVoiture;
    private int capacite;
    private String ref;
    private String carburant; // E, H, D
    private String libelleCarburant;

    // Constructeurs
    public VoitureDTO() {}

    public VoitureDTO(int idVoiture, int capacite, String ref, String carburant, String libelleCarburant) {
        this.idVoiture = idVoiture;
        this.capacite = capacite;
        this.ref = ref;
        this.carburant = carburant;
        this.libelleCarburant = libelleCarburant;
    }

    // Getters et Setters
    public int getIdVoiture() { return idVoiture; }
    public void setIdVoiture(int idVoiture) { this.idVoiture = idVoiture; }

    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }

    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }

    public String getCarburant() { return carburant; }
    public void setCarburant(String carburant) { this.carburant = carburant; }

    public String getLibelleCarburant() { return libelleCarburant; }
    public void setLibelleCarburant(String libelleCarburant) { this.libelleCarburant = libelleCarburant; }
}
