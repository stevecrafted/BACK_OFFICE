package org.example.Controlleur;

import org.annotation.*;
import org.example.DAO.CarburantDAO;
import org.example.Model.Carburant;
import org.Entity.ModelView;

import java.util.List;

@AnnotationContoller
public class CarburantController {

    private CarburantDAO carburantDAO = new CarburantDAO();

    // ========== API CARBURANTS (JSON) ==========
    @Json
    @GetMapping("/api/carburants")
    public List<Carburant> getCarburants() {
        System.out.println("API /api/carburants appelée");
        return carburantDAO.findAll();
    }

    // ========== LISTE DES CARBURANTS (JSP) ==========
    @GetMapping("/carburants")
    public ModelView listeCarburants() {
        ModelView mv = new ModelView();
        mv.setView("carburants/liste.jsp");

        List<Carburant> carburants = carburantDAO.findAll();
        mv.addAttribute("carburants", carburants);

        return mv;
    }

    // ========== CRÉER UN CARBURANT ==========
    @PostMapping("/carburants/create")
    public ModelView creerCarburant(
            @AnnotationRequestParam("libelle") String libelle) {

        Carburant carburant = new Carburant(libelle);

        ModelView mv = new ModelView();

        if (carburantDAO.create(carburant)) {
            mv.setView("redirect:/carburants");
            mv.addAttribute("message", "Carburant créé avec succès");
        } else {
            mv.setView("carburants/form.jsp");
            mv.addAttribute("error", "Erreur lors de la création");
            mv.addAttribute("carburant", carburant);
        }

        return mv;
    }

    // ========== SUPPRIMER UN CARBURANT ==========
    @PostMapping("/carburants/delete")
    public ModelView supprimerCarburant(@AnnotationRequestParam("id") int id) {
        ModelView mv = new ModelView();

        if (carburantDAO.delete(id)) {
            mv.setView("redirect:/carburants");
            mv.addAttribute("message", "Carburant supprimé avec succès");
        } else {
            mv.setView("redirect:/carburants");
            mv.addAttribute("error", "Erreur lors de la suppression");
        }

        return mv;
    }
}
