package org.example.Model;

public class Carburant {
    private int idCarburant;
    private String libelle;

    // Constructeurs
    public Carburant() {}

    public Carburant(String libelle) {
        this.libelle = libelle;
    }

    // Getters et Setters
    public int getIdCarburant() { return idCarburant; }
    public void setIdCarburant(int idCarburant) { this.idCarburant = idCarburant; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    @Override
    public String toString() {
        return "Carburant{" +
                "idCarburant=" + idCarburant +
                ", libelle='" + libelle + '\'' +
                '}';
    }
}
