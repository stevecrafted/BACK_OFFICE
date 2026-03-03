package org.example.Model;

public class Hotel {
    private int id;
    private String nom;

    // Constructeurs
    public Hotel() {}

    public Hotel(String nom) {
        this.nom = nom;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    @Override
    public String toString() {
        return "Hotel{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                '}';
    }
}