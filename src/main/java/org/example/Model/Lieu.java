package org.example.Model;

public class Lieu {
    private int idLieuHotel;
    private String code;
    private String libelle;

    // Constructeurs
    public Lieu() {}

    public Lieu(String code, String libelle) {
        this.code = code;
        this.libelle = libelle;
    }

    // Getters et Setters
    public int getIdLieuHotel() { return idLieuHotel; }
    public void setIdLieuHotel(int idLieuHotel) { this.idLieuHotel = idLieuHotel; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    @Override
    public String toString() {
        return "Lieu{" +
                "idLieuHotel=" + idLieuHotel +
                ", code='" + code + '\'' +
                ", libelle='" + libelle + '\'' +
                '}';
    }
}
