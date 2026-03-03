package org.example.Model;

public class Hotel {
    private int id;
    private String code;
    private String libelle;

    // Constructeurs
    public Hotel() {}

    public Hotel(String code, String libelle) {
        this.code = code;
        this.libelle = libelle;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    @Override
    public String toString() {
        return "Hotel{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", libelle='" + libelle + '\'' +
                '}';
    }
}