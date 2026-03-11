package org.example.Controlleur;

import org.annotation.*;
import org.example.DAO.ReservationDAO;
import org.example.DAO.LieuDAO;
import org.example.DAO.AssignationDAO;
import org.example.Model.Reservation;
import org.example.Model.Lieu;
import org.example.Model.Assignation;
import org.Entity.ModelView;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AnnotationContoller
public class ReservationController {

    private ReservationDAO reservationDAO = new ReservationDAO();
    private LieuDAO lieuDAO = new LieuDAO();
    private AssignationDAO assignationDAO = new AssignationDAO();

    /**
     * Liste toutes les réservations
     */
    @GetMapping("/reservations")
    public ModelView listeReservations() {
        ModelView mv = new ModelView();
        mv.setView("reservations/liste.jsp");

        List<Reservation> reservations = reservationDAO.findAll();
        
        // Enrichir avec les détails
        List<Map<String, Object>> reservationsDetails = new ArrayList<>();
        
        for (Reservation reservation : reservations) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("reservation", reservation);
            
            Lieu lieu = lieuDAO.findById(reservation.getIdLieu());
            detail.put("lieu", lieu);
            
            Assignation assignation = assignationDAO.findByReservation(reservation.getId());
            detail.put("assignation", assignation);
            detail.put("estAssignee", assignation != null);
            
            reservationsDetails.add(detail);
        }
        
        mv.addAttribute("reservations", reservationsDetails);

        return mv;
    }

    /**
     * API JSON - Liste toutes les réservations
     */
    @Json
    @GetMapping("/api/reservations")
    public List<Reservation> listeReservationsAPI() {
        return reservationDAO.findAll();
    }

    /**
     * Formulaire de création
     */
    @GetMapping("/reservations/nouveau")
    public ModelView nouvelleReservation() {
        ModelView mv = new ModelView();
        mv.setView("reservations/form.jsp");
        mv.addAttribute("action", "create");

        List<Lieu> lieux = lieuDAO.findByType("hotel");
        mv.addAttribute("lieux", lieux);

        return mv;
    }

    /**
     * Créer une réservation
     */
    @PostMapping("/reservations/create")
    public ModelView creerReservation(
            @AnnotationRequestParam("idLieu") int idLieu,
            @AnnotationRequestParam("idClient") String idClient,
            @AnnotationRequestParam("nbPassager") int nbPassager,
            @AnnotationRequestParam("dateHeure") String dateHeureStr) {

        Reservation reservation = new Reservation(idLieu, idClient, nbPassager);

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
            java.util.Date parsedDate = dateFormat.parse(dateHeureStr);
            Timestamp timestamp = new Timestamp(parsedDate.getTime());
            reservation.setDateHeure(timestamp);
        } catch (Exception e) {
            System.err.println("Erreur de parsing de date : " + e.getMessage());
        }

        ModelView mv = new ModelView();

        if (reservationDAO.create(reservation)) {
            mv.setView("redirect:/reservations");
            mv.addAttribute("message", "Réservation créée avec succès");
        } else {
            mv.setView("reservations/form.jsp");
            mv.addAttribute("error", "Erreur lors de la création");
            mv.addAttribute("reservation", reservation);

            List<Lieu> lieux = lieuDAO.findByType("hotel");
            mv.addAttribute("lieux", lieux);
        }

        return mv;
    }

    /**
     * Supprimer une réservation
     */
    @PostMapping("/reservations/delete")
    public ModelView supprimerReservation(@AnnotationRequestParam("id") int id) {
        ModelView mv = new ModelView();

        if (reservationDAO.delete(id)) {
            mv.setView("redirect:/reservations");
            mv.addAttribute("message", "Réservation supprimée avec succès");
        } else {
            mv.setView("redirect:/reservations");
            mv.addAttribute("error", "Erreur lors de la suppression");
        }

        return mv;
    }
}
