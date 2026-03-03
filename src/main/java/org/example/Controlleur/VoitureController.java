package org.example.Controlleur;

import org.annotation.*;
import org.example.DAO.VoitureDAO;
import org.example.DAO.CarburantDAO;
import org.example.DTO.VoitureDTO;
import org.example.Model.Voiture;
import org.example.Model.Carburant;
import org.example.Service.VoitureService;
import org.Entity.ModelView;

import java.sql.Date;
import java.util.List;

@AnnotationContoller
public class VoitureController {

    private VoitureDAO voitureDAO = new VoitureDAO();
    private CarburantDAO carburantDAO = new CarburantDAO();
    private VoitureService voitureService = new VoitureService();

    // ========== LISTE DES VOITURES (avec filtre optionnel par date de départ) ==========
    @GetMapping("/voitures")
    public ModelView listeVoitures(
            @AnnotationRequestParam(value = "dateDepart") String dateDepartStr) {
        ModelView mv = new ModelView();
        mv.setView("voitures/liste.jsp");

        if (dateDepartStr != null && !dateDepartStr.isEmpty()) {
            try {
                Date date = Date.valueOf(dateDepartStr);
                List<Voiture> voitures = voitureDAO.findByDateDepart(date);
                mv.addAttribute("voitures", voitures);
                mv.addAttribute("dateDepart", dateDepartStr);
                mv.addAttribute("message", voitures.size() + " voiture(s) avec départ le " + dateDepartStr);
            } catch (IllegalArgumentException e) {
                mv.addAttribute("error", "Format de date invalide : " + dateDepartStr);
                List<Voiture> voitures = voitureDAO.findAll();
                mv.addAttribute("voitures", voitures);
            }
        } else {
            List<Voiture> voitures = voitureDAO.findAll();
            mv.addAttribute("voitures", voitures);
        }

        return mv;
    }

    // ========== API VOITURES (JSON avec DTO) ==========
    @Json
    @GetMapping("/api/voitures")
    public List<VoitureDTO> getVoitures() {
        System.out.println("API /api/voitures appelée");

        // Retourner toutes les voitures (avec DTO)
        return voitureService.getAllVoitures();
    }

    // ========== API VOITURES PAR CARBURANT (JSON avec DTO) ==========
    @Json
    @GetMapping("/api/voitures/carburant")
    public List<VoitureDTO> getVoituresByCarburant(
            @AnnotationRequestParam(value = "id") String idCarburantStr) {

        System.out.println("API /api/voitures/carburant appelée avec params:");
        System.out.println("  id: " + idCarburantStr);

        // Filtrer par carburant
        if (idCarburantStr != null && !idCarburantStr.isEmpty()) {
            try {
                int idCarburant = Integer.parseInt(idCarburantStr);
                return voitureService.getVoituresByCarburant(idCarburant);
            } catch (NumberFormatException e) {
                System.err.println("Format d'ID carburant invalide : " + idCarburantStr);
                return List.of();
            }
        }

        return List.of();
    }

    // ========== API VOITURES PAR DATE DE DÉPART (JSON avec DTO) ==========
    @Json
    @GetMapping("/api/voitures/date")
    public List<VoitureDTO> getVoituresByDate(
            @AnnotationRequestParam(value = "dateDepart") String dateDepartStr) {

        System.out.println("API /api/voitures/date appelée avec params:");
        System.out.println("  dateDepart: " + dateDepartStr);

        if (dateDepartStr != null && !dateDepartStr.isEmpty()) {
            try {
                Date date = Date.valueOf(dateDepartStr);
                return voitureService.getVoituresByDateDepart(date);
            } catch (IllegalArgumentException e) {
                System.err.println("Format de date invalide : " + dateDepartStr);
                return List.of();
            }
        }

        return List.of();
    }

    // ========== FORMULAIRE DE CRÉATION ==========
    @GetMapping("/voitures/nouveau")
    public ModelView nouvelleVoiture() {
        ModelView mv = new ModelView();
        mv.setView("voitures/form.jsp");
        mv.addAttribute("action", "create");

        // Liste des carburants pour le formulaire
        List<Carburant> carburants = carburantDAO.findAll();
        mv.addAttribute("carburants", carburants);

        return mv;
    }

    // ========== CRÉER UNE VOITURE ==========
    @PostMapping("/voitures/create")
    public ModelView creerVoiture(
            @AnnotationRequestParam("capacite") int capacite,
            @AnnotationRequestParam("idCarburant") int idCarburant) {

        // Récupérer l'objet Carburant complet
        Carburant carburant = carburantDAO.findById(idCarburant);

        if (carburant == null) {
            ModelView mv = new ModelView();
            mv.setView("voitures/form.jsp");
            mv.addAttribute("error", "Carburant non trouvé");
            List<Carburant> carburants = carburantDAO.findAll();
            mv.addAttribute("carburants", carburants);
            return mv;
        }

        // Le ref sera généré automatiquement basé sur l'ID
        Voiture voiture = new Voiture(capacite, "", carburant);

        ModelView mv = new ModelView();

        if (voitureDAO.create(voiture)) {
            mv.setView("redirect:/voitures");
            mv.addAttribute("message", "Voiture créée avec succès - Ref: " + voiture.getRef());
        } else {
            mv.setView("voitures/form.jsp");
            mv.addAttribute("error", "Erreur lors de la création");
            mv.addAttribute("voiture", voiture);

            // Recharger la liste des carburants
            List<Carburant> carburants = carburantDAO.findAll();
            mv.addAttribute("carburants", carburants);
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

            // Liste des carburants pour le formulaire
            List<Carburant> carburants = carburantDAO.findAll();
            mv.addAttribute("carburants", carburants);
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
            @AnnotationRequestParam("idCarburant") int idCarburant) {

        // Récupérer l'objet Carburant complet
        Carburant carburant = carburantDAO.findById(idCarburant);

        if (carburant == null) {
            ModelView mv = new ModelView();
            mv.setView("voitures/form.jsp");
            mv.addAttribute("error", "Carburant non trouvé");
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

            // Recharger la liste des carburants
            List<Carburant> carburants = carburantDAO.findAll();
            mv.addAttribute("carburants", carburants);
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
