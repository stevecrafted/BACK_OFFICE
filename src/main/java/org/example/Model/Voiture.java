package org.example.Model;

import java.sql.Time;

public class Voiture {
    private int idVoiture;
    private int capacite;
    private String ref;
    private String carburant; // E=Essence, H=Hybride, D=Diesel
    private Time disponibilite;

    // Constructeurs
    public Voiture() {}

    public Voiture(int capacite, String ref, String carburant) {
        this.capacite = capacite;
        this.ref = ref;
        this.carburant = carburant;
    }

    public Voiture(int capacite, String ref, String carburant, Time disponibilite) {
        this.capacite = capacite;
        this.ref = ref;
        this.carburant = carburant;
        this.disponibilite = disponibilite;
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

    public Time getDisponibilite() { return disponibilite; }
    public void setDisponibilite(Time disponibilite) { this.disponibilite = disponibilite; }

    public String getCarburantLibelle() {
        if (carburant == null) return "";
        switch (carburant) {
            case "D": return "Diesel";
            case "H": return "Hybride";
            case "E": return "Essence";
            default: return carburant;
        }
    }

    @Override
    public String toString() {
        return "Voiture{" +
                "idVoiture=" + idVoiture +
                ", capacite=" + capacite +
                ", ref='" + ref + '\'' +
                ", carburant='" + carburant + '\'' +
                ", disponibilite=" + disponibilite +
                '}';
    }
}
