package org.example.Model;

public class Voiture {
    private int idVoiture;
    private int capacite;
    private String ref;
    private int idCarburant;

    // Constructeurs
    public Voiture() {}

    public Voiture(int capacite, String ref, int idCarburant) {
        this.capacite = capacite;
        this.ref = ref;
        this.idCarburant = idCarburant;
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

    @Override
    public String toString() {
        return "Voiture{" +
                "idVoiture=" + idVoiture +
                ", capacite=" + capacite +
                ", ref='" + ref + '\'' +
                ", idCarburant=" + idCarburant +
                '}';
    }
}
