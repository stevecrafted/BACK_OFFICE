package org.example.Controlleur;

import org.annotation.*;
import org.example.DAO.VoitureDAO;
import org.example.DTO.VoitureDTO;
import org.example.Model.Voiture;
import org.example.Service.VoitureService;
import org.Entity.ModelView;

import java.util.List;

@AnnotationContoller
public class VoitureController {

    private VoitureDAO voitureDAO = new VoitureDAO();
    private VoitureService voitureService = new VoitureService();

    // ========== LISTE DES VOITURES ==========
    @GetMapping("/voitures")
    public ModelView listeVoitures() {
        ModelView mv = new ModelView();
        mv.setView("voitures/liste.jsp");

        List<Voiture> voitures = voitureDAO.findAll();
        mv.addAttribute("voitures", voitures);

        return mv;
    }

    // ========== API VOITURES (JSON avec DTO) ==========
    @Json
    @GetMapping("/api/voitures")
    public List<VoitureDTO> getVoitures() {
        return voitureService.getAllVoitures();
    }

    // ========== API VOITURES PAR CARBURANT (JSON avec DTO) ==========
    @Json
    @GetMapping("/api/voitures/carburant")
    public List<VoitureDTO> getVoituresByCarburant(
            @AnnotationRequestParam(value = "type") String carburantType) {

        if (carburantType != null && !carburantType.isEmpty()) {
            return voitureService.getVoituresByCarburant(carburantType);
        }

        return List.of();
    }

    // ========== FORMULAIRE DE CRÉATION ==========
    @GetMapping("/voitures/nouveau")
    public ModelView nouvelleVoiture() {
        ModelView mv = new ModelView();
        mv.setView("voitures/form.jsp");
        mv.addAttribute("action", "create");
        return mv;
    }

    // ========== CRÉER UNE VOITURE ==========
    @PostMapping("/voitures/create")
    public ModelView creerVoiture(
            @AnnotationRequestParam("capacite") int capacite,
            @AnnotationRequestParam("carburant") String carburant) {

        if (!carburant.equals("E") && !carburant.equals("H") && !carburant.equals("D")) {
            ModelView mv = new ModelView();
            mv.setView("voitures/form.jsp");
            mv.addAttribute("error", "Type de carburant invalide (E, H ou D attendu)");
            return mv;
        }

        Voiture voiture = new Voiture(capacite, "", carburant);

        ModelView mv = new ModelView();

        if (voitureDAO.create(voiture)) {
            mv.setView("redirect:/voitures");
            mv.addAttribute("message", "Voiture créée avec succès - Ref: " + voiture.getRef());
        } else {
            mv.setView("voitures/form.jsp");
            mv.addAttribute("error", "Erreur lors de la création");
            mv.addAttribute("voiture", voiture);
        }

        return mv;
    }

    // ========== FORMULAIRE DE MODIFICATION ==========
    @GetMapping("/voitures/edit")
    public ModelView editerVoiture(@AnnotationRequestParam("id") int id) {
        ModelView mv = new ModelView();

        Voiture voiture = voitureDAO.findById(id);

        if (voiture != null) {
            mv.setView("voitures/form.jsp");
            mv.addAttribute("action", "update");
            mv.addAttribute("voiture", voiture);
        } else {
            mv.setView("redirect:/voitures");
            mv.addAttribute("error", "Voiture non trouvée");
        }

        return mv;
    }

    // ========== METTRE À JOUR UNE VOITURE ==========
    @PostMapping("/voitures/update")
    public ModelView mettreAJourVoiture(
            @AnnotationRequestParam("id") int id,
            @AnnotationRequestParam("capacite") int capacite,
            @AnnotationRequestParam("ref") String ref,
            @AnnotationRequestParam("carburant") String carburant) {

        if (!carburant.equals("E") && !carburant.equals("H") && !carburant.equals("D")) {
            ModelView mv = new ModelView();
            mv.setView("voitures/form.jsp");
            mv.addAttribute("error", "Type de carburant invalide");
            return mv;
        }

        Voiture voiture = new Voiture(capacite, ref, carburant);
        voiture.setIdVoiture(id);

        ModelView mv = new ModelView();

        if (voitureDAO.update(voiture)) {
            mv.setView("redirect:/voitures");
            mv.addAttribute("message", "Voiture mise à jour avec succès");
        } else {
            mv.setView("voitures/form.jsp");
            mv.addAttribute("error", "Erreur lors de la mise à jour");
            mv.addAttribute("voiture", voiture);
        }

        return mv;
    }

    // ========== SUPPRIMER UNE VOITURE ==========
    @PostMapping("/voitures/delete")
    public ModelView supprimerVoiture(@AnnotationRequestParam("id") int id) {
        ModelView mv = new ModelView();

        if (voitureDAO.delete(id)) {
            mv.setView("redirect:/voitures");
            mv.addAttribute("message", "Voiture supprimée avec succès");
        } else {
            mv.setView("redirect:/voitures");
            mv.addAttribute("error", "Erreur lors de la suppression");
        }

        return mv;
    }

    // ========== DÉTAILS D'UNE VOITURE ==========
    @GetMapping("/voitures/details")
    public ModelView detailsVoiture(@AnnotationRequestParam("id") int id) {
        ModelView mv = new ModelView();

        Voiture voiture = voitureDAO.findById(id);

        if (voiture != null) {
            mv.setView("voitures/details.jsp");
            mv.addAttribute("voiture", voiture);
        } else {
            mv.setView("redirect:/voitures");
            mv.addAttribute("error", "Voiture non trouvée");
        }

        return mv;
    }
}
