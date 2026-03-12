package org.example.Model;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * Résultat complet d'une simulation d'assignation
 */
public class ResultatSimulation {
    private Date dateSimulation;
    private List<SimulationAssignation> assignations;
    private List<Reservation> reservationsNonAssignees;
    private List<SimulationAssignation> assignationsExistantes;
    private int nbVagues;

    public ResultatSimulation(Date dateSimulation) {
        this.dateSimulation = dateSimulation;
        this.assignations = new ArrayList<>();
        this.reservationsNonAssignees = new ArrayList<>();
        this.assignationsExistantes = new ArrayList<>();
        this.nbVagues = 0;
    }

    public void ajouterAssignation(SimulationAssignation assignation) {
        assignations.add(assignation);
    }

    public void ajouterReservationNonAssignee(Reservation reservation) {
        reservationsNonAssignees.add(reservation);
    }

    public void ajouterAssignationExistante(SimulationAssignation assignation) {
        assignationsExistantes.add(assignation);
    }

    // Getters et Setters
    public Date getDateSimulation() { return dateSimulation; }
    public List<SimulationAssignation> getAssignations() { return assignations; }
    public List<Reservation> getReservationsNonAssignees() { return reservationsNonAssignees; }
    public List<SimulationAssignation> getAssignationsExistantes() { return assignationsExistantes; }
    public int getNbVagues() { return nbVagues; }
    public void setNbVagues(int nbVagues) { this.nbVagues = nbVagues; }
    
    public int getTotalReservationsAssignees() {
        return assignations.stream()
                .mapToInt(SimulationAssignation::getNbReservations)
                .sum();
    }
}
