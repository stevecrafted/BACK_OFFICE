package org.example.Model;

public class Voiture {
    private int idVoiture;
    private int capacite;
    private String ref;
    private Carburant carburant;

    // Constructeurs
    public Voiture() {}

    public Voiture(int capacite, String ref, Carburant carburant) {
        this.capacite = capacite;
        this.ref = ref;
        this.carburant = carburant;
    }

    // Getters et Setters
    public int getIdVoiture() { return idVoiture; }
    public void setIdVoiture(int idVoiture) { this.idVoiture = idVoiture; }

    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }

    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }

    public Carburant getCarburant() { return carburant; }
    public void setCarburant(Carburant carburant) { this.carburant = carburant; }

    @Override
    public String toString() {
        return "Voiture{" +
                "idVoiture=" + idVoiture +
                ", capacite=" + capacite +
                ", ref='" + ref + '\'' +
                ", carburant=" + carburant +
                '}';
    }
}
