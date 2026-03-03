package org.example.DTO;

public class VoitureDTO {
    private int idVoiture;
    private int capacite;
    private String ref;
    private int idCarburant;
    private String libelleCarburant; // Pour afficher le libellé du carburant

    // Constructeurs
    public VoitureDTO() {}

    public VoitureDTO(int idVoiture, int capacite, String ref, int idCarburant, String libelleCarburant) {
        this.idVoiture = idVoiture;
        this.capacite = capacite;
        this.ref = ref;
        this.idCarburant = idCarburant;
        this.libelleCarburant = libelleCarburant;
    }

    // Getters et Setters
    public int getIdVoiture() { return idVoiture; }
    public void setIdVoiture(int idVoiture) { this.idVoiture = idVoiture; }

    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }

    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }

    public int getIdCarburant() { return idCarburant; }
    public void setIdCarburant(int idCarburant) { this.idCarburant = idCarburant; }

    public String getLibelleCarburant() { return libelleCarburant; }
    public void setLibelleCarburant(String libelleCarburant) { this.libelleCarburant = libelleCarburant; }
}
