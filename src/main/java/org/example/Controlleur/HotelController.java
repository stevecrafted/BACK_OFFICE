package org.example.Controlleur;

import org.annotation.*;
import org.example.DAO.LieuDAO;
import org.example.Model.Lieu;
import org.Entity.ModelView;

import java.util.List;

@AnnotationContoller
public class HotelController {

    private LieuDAO lieuDAO = new LieuDAO();

    // ========== LISTE DES LIEUX ==========
    @GetMapping("/lieux")
    public ModelView listeLieux() {
        ModelView mv = new ModelView();
        mv.setView("lieux/liste.jsp");
        
        List<Lieu> lieux = lieuDAO.findAll();
        mv.addAttribute("lieux", lieux);
        
        return mv;
    }

    // ========== API JSON - LISTE DES LIEUX ==========
    @Json
    @GetMapping("/api/lieux")
    public List<Lieu> listeLieuxAPI() {
        return lieuDAO.findAll();
    }

    // ========== API JSON - LIEUX PAR TYPE ==========
    @Json
    @GetMapping("/api/lieux/type")
    public List<Lieu> listeLieuxParType(@AnnotationRequestParam("type") String type) {
        return lieuDAO.findByType(type);
    }
}