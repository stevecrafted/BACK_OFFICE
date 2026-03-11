package org.example.Controlleur;

import org.annotation.*;
import org.example.DAO.AssignationDAO;
import org.example.DAO.ReservationDAO;
import org.example.DAO.VoitureDAO;
import org.example.Model.Assignation;
import org.example.Model.Reservation;
import org.example.Model.Voiture;
import org.example.Model.ResultatSimulation;
import org.example.Model.SimulationAssignation;
import org.example.Service.AssignationService;
import org.example.Service.SimulationAssignationService;
import org.Entity.ModelView;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AnnotationContoller
public class AssignationController {

    private AssignationService assignationService = new AssignationService();
    private SimulationAssignationService simulationService = new SimulationAssignationService();
    private AssignationDAO assignationDAO = new AssignationDAO();
    private VoitureDAO voitureDAO = new VoitureDAO();
    private ReservationDAO reservationDAO = new ReservationDAO();

    /**
     * Liste toutes les assignations
     */
    @GetMapping("/assignations")
    public ModelView listeAssignations() {
        ModelView mv = new ModelView();
        mv.setView("assignations/liste.jsp");

        List<Assignation> assignations = assignationDAO.findAll();
        
        // Enrichir avec les détails
        List<Map<String, Object>> assignationsDetails = new ArrayList<>();
        
        for (Assignation assignation : assignations) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("assignation", assignation);
            
            Voiture voiture = voitureDAO.findById(assignation.getIdVoiture());
            detail.put("voiture", voiture);
            
            Reservation reservation = reservationDAO.findById(assignation.getIdReservation());
            detail.put("reservation", reservation);
            
            assignationsDetails.add(detail);
        }
        
        mv.addAttribute("assignations", assignationsDetails);

        return mv;
    }

    /**
     * API JSON - Liste toutes les assignations
     */
    @Json
    @GetMapping("/api/assignations")
    public List<AssignationDetail> listeAssignationsAPI() {
        List<Assignation> assignations = assignationDAO.findAll();
        List<AssignationDetail> details = new ArrayList<>();
        
        for (Assignation assignation : assignations) {
            Voiture voiture = voitureDAO.findById(assignation.getIdVoiture());
            Reservation reservation = reservationDAO.findById(assignation.getIdReservation());
            
            details.add(new AssignationDetail(assignation, voiture, reservation));
        }
        
        return details;
    }

    /**
     * Déclenche l'assignation automatique des voitures aux réservations
     */
    @PostMapping("/assignations/auto")
    public ModelView assignerAutomatiquement() {
        ModelView mv = new ModelView();

        System.out.println("\n🚗 Déclenchement de l'assignation automatique...\n");

        List<Assignation> assignations = assignationService.assignerVoituresAutomatiquement();

        mv.setView("redirect:/assignations");
        mv.addAttribute("message", assignations.size() + " assignation(s) créée(s) avec succès");

        return mv;
    }

    /**
     * API JSON pour déclencher l'assignation automatique
     */
    @Json
    @PostMapping("/api/assignations/auto")
    public AssignationResult assignerAutomatiquementAPI() {
        System.out.println("\n🚗 API: Déclenchement de l'assignation automatique...\n");

        List<Assignation> assignations = assignationService.assignerVoituresAutomatiquement();

        return new AssignationResult(
            true,
            assignations.size() + " assignation(s) créée(s)",
            assignations
        );
    }

    /**
     * Supprimer une assignation
     */
    @PostMapping("/assignations/delete")
    public ModelView supprimerAssignation(@AnnotationRequestParam("id") int id) {
        ModelView mv = new ModelView();

        if (assignationDAO.delete(id)) {
            mv.setView("redirect:/assignations");
            mv.addAttribute("message", "Assignation supprimée avec succès");
        } else {
            mv.setView("redirect:/assignations");
            mv.addAttribute("error", "Erreur lors de la suppression");
        }

        return mv;
    }

    /**
     * Afficher le formulaire de simulation
     */
    @GetMapping("/assignations/simuler")
    public ModelView afficherFormulaireSimulation() {
        ModelView mv = new ModelView();
        mv.setView("assignations/simulation.jsp");
        return mv;
    }

    /**
     * Lancer la simulation pour une date donnée
     */
    @PostMapping("/assignations/simuler")
    public ModelView lancerSimulation(@AnnotationRequestParam("date") String dateStr) {
        ModelView mv = new ModelView();
        mv.setView("assignations/simulation.jsp");

        try {
            Date dateSimulation = Date.valueOf(dateStr);
            ResultatSimulation resultat = simulationService.simulerAssignation(dateSimulation);
            
            mv.addAttribute("resultat", resultat);
            mv.addAttribute("dateSimulation", dateStr);
            
        } catch (IllegalArgumentException e) {
            mv.addAttribute("error", "Format de date invalide. Utilisez YYYY-MM-DD");
        } catch (Exception e) {
            mv.addAttribute("error", "Erreur lors de la simulation : " + e.getMessage());
            e.printStackTrace();
        }

        return mv;
    }

    /**
     * API JSON - Lancer la simulation
     */
    @Json
    @GetMapping("/api/assignations/simuler")
    public ResultatSimulation lancerSimulationAPI(@AnnotationRequestParam("date") String dateStr) {
        try {
            Date dateSimulation = Date.valueOf(dateStr);
            return simulationService.simulerAssignation(dateSimulation);
        } catch (Exception e) {
            System.err.println("❌ Erreur API simulation : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Confirmer et enregistrer la simulation en base de données
     */
    @PostMapping("/assignations/confirmer")
    public ModelView confirmerSimulation(@AnnotationRequestParam("date") String dateStr) {
        ModelView mv = new ModelView();

        try {
            Date dateSimulation = Date.valueOf(dateStr);
            ResultatSimulation resultat = simulationService.simulerAssignation(dateSimulation);
            
            int nbAssignations = 0;
            
            // Enregistrer chaque assignation en base
            for (SimulationAssignation simAssignation : resultat.getAssignations()) {
                for (Reservation reservation : simAssignation.getReservations()) {
                    Assignation assignation = new Assignation();
                    assignation.setIdReservation(reservation.getId());
                    assignation.setIdVoiture(simAssignation.getVoiture().getIdVoiture());
                    
                    if (assignationDAO.create(assignation)) {
                        nbAssignations++;
                    }
                }
            }
            
            mv.setView("assignations/simulation.jsp");
            mv.addAttribute("message", nbAssignations + " assignation(s) confirmée(s) et enregistrée(s)");
            
        } catch (Exception e) {
            mv.setView("redirect:/assignations/simuler");
            mv.addAttribute("error", "Erreur lors de la confirmation : " + e.getMessage());
            e.printStackTrace();
        }

        return mv;
    }

    /**
     * Classe pour la réponse JSON
     */
    public static class AssignationResult {
        private boolean success;
        private String message;
        private List<Assignation> assignations;

        public AssignationResult(boolean success, String message, List<Assignation> assignations) {
            this.success = success;
            this.message = message;
            this.assignations = assignations;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public List<Assignation> getAssignations() { return assignations; }
    }

    /**
     * Classe pour les détails d'assignation (API)
     */
    public static class AssignationDetail {
        private Assignation assignation;
        private Voiture voiture;
        private Reservation reservation;

        public AssignationDetail(Assignation assignation, Voiture voiture, Reservation reservation) {
            this.assignation = assignation;
            this.voiture = voiture;
            this.reservation = reservation;
        }

        public Assignation getAssignation() { return assignation; }
        public Voiture getVoiture() { return voiture; }
        public Reservation getReservation() { return reservation; }
    }
}
