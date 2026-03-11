package org.example.Model;

public class Lieu {
    private int id;
    private String code;
    private String libelle;
    private String type; // 'hotel' ou 'aeroport'

    // Constructeurs
    public Lieu() {}

    public Lieu(String code, String libelle, String type) {
        this.code = code;
        this.libelle = libelle;
        this.type = type;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    @Override
    public String toString() {
        return "Lieu{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", libelle='" + libelle + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
