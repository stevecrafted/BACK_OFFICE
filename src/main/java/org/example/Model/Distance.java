package org.example.Model;

public class Distance {
    private int idDistance;
    private int idDepart;
    private int idArrive;
    private double kilometer;

    // Constructeurs
    public Distance() {}

    public Distance(int idDepart, int idArrive, double kilometer) {
        this.idDepart = idDepart;
        this.idArrive = idArrive;
        this.kilometer = kilometer;
    }

    // Getters et Setters
    public int getIdDistance() { return idDistance; }
    public void setIdDistance(int idDistance) { this.idDistance = idDistance; }

    public int getIdDepart() { return idDepart; }
    public void setIdDepart(int idDepart) { this.idDepart = idDepart; }

    public int getIdArrive() { return idArrive; }
    public void setIdArrive(int idArrive) { this.idArrive = idArrive; }

    public double getKilometer() { return kilometer; }
    public void setKilometer(double kilometer) { this.kilometer = kilometer; }

    @Override
    public String toString() {
        return "Distance{" +
                "idDistance=" + idDistance +
                ", idDepart=" + idDepart +
                ", idArrive=" + idArrive +
                ", kilometer=" + kilometer +
                '}';
    }
}
