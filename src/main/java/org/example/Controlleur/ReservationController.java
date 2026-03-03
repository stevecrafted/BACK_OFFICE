package org.example.Controlleur;

import org.annotation.*;
import org.example.DAO.ReservationDAO;
import org.example.DTO.ReservationDTO;
import org.example.DAO.HotelDAO;
import org.example.Model.Reservation;
import org.example.Service.ReservationService;
import org.example.Model.Hotel;
import org.example.token.GenrateToken;
import org.Entity.ModelView;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

@AnnotationContoller
public class ReservationController {

    private ReservationDAO reservationDAO = new ReservationDAO();
    private HotelDAO hotelDAO = new HotelDAO();
    private ReservationService reservationService = new ReservationService();

    // ========== LISTE DES RESERVATIONS ==========
    @GetMapping("/reservations")
    public ModelView listeReservations() {
        ModelView mv = new ModelView();
        mv.setView("reservations/liste.jsp");

        List<Reservation> reservations = reservationDAO.findAll();
        mv.addAttribute("reservations", reservations);

        return mv;
    }

    // ========== API RESERVATIONS (JSON avec DTO) ==========
    @Json
    @GetMapping("/api/reservations")
    public List<ReservationDTO> getReservations(
            @AnnotationRequestParam(value = "token") String token,
            @AnnotationRequestParam(value = "dateStr") String dateStr) {

        System.out.println("API /api/reservations appelée avec params:");
        System.out.println("  token: " + token);
        System.out.println("  dateStr: " + dateStr);

        // Vérification du token
        if (!GenrateToken.isTokenValide(token)) {
            System.err.println("❌ Token invalide ou expiré : " + token);
            return List.of(); // Retourner liste vide si token invalide
        }
        System.out.println("✅ Token valide");

        // 1. Filtrer par date si spécifié
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                Date date = Date.valueOf(dateStr);
                return reservationService.getListReservationByDate(date);
            } catch (IllegalArgumentException e) {
                System.err.println("Format de date invalide : " + dateStr);
                return List.of(); // Retourner liste vide
            }
        }

        // 4. Retourner toutes les réservations (avec DTO)
        return reservationService.getAllReservations();
    }

    // ========== FORMULAIRE DE CRÉATION ==========
    @GetMapping("/reservations/nouveau")
    public ModelView nouvelleReservation() {
        ModelView mv = new ModelView();
        mv.setView("reservations/form.jsp");
        mv.addAttribute("action", "create");

        // Liste des hotels pour le formulaire
        List<Hotel> hotels = hotelDAO.findAll();
        mv.addAttribute("hotels", hotels);

        return mv;
    }

    // ========== CRÉER UNE RESERVATION ==========
    @PostMapping("/reservations/create")
    public ModelView creerReservation(
            @AnnotationRequestParam("idHotel") int idHotel,
            @AnnotationRequestParam("idClient") String idClient,
            @AnnotationRequestParam("nbPassager") int nbPassager,
            @AnnotationRequestParam("dateHeure") String dateHeureStr) {

        Reservation reservation = new Reservation(idHotel, idClient, nbPassager);

        // Convertir la chaîne de date-heure en Timestamp
        try {
            // Format attendu: yyyy-MM-ddTHH:mm (HTML5 datetime-local)
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
            java.util.Date parsedDate = dateFormat.parse(dateHeureStr);
            Timestamp timestamp = new Timestamp(parsedDate.getTime());
            reservation.setDateHeure(timestamp);
        } catch (Exception e) {
            System.err.println("❌ Erreur de parsing de date : " + e.getMessage());
        }

        ModelView mv = new ModelView();

        if (reservationDAO.create(reservation)) {
            mv.setView("redirect:/reservations");
            mv.addAttribute("message", "Réservation créée avec succès");
        } else {
            mv.setView("reservations/form.jsp");
            mv.addAttribute("error", "Erreur lors de la création");
            mv.addAttribute("reservation", reservation);

            // Recharger la liste des hotels
            List<Hotel> hotels = hotelDAO.findAll();
            mv.addAttribute("hotels", hotels);
        }

        return mv;
    }

    // ========== METTRE À JOUR UNE RESERVATION ==========
    @PostMapping("/reservations/update")
    public ModelView mettreAJourReservation(
            @AnnotationRequestParam("id") int id,
            @AnnotationRequestParam("idHotel") int idHotel,
            @AnnotationRequestParam("idClient") String idClient,
            @AnnotationRequestParam("nbPassager") int nbPassager,
            @AnnotationRequestParam("dateHeure") String dateHeureStr) {
        Reservation reservation = new Reservation(idHotel, idClient, nbPassager);
        reservation.setId(id);

        // Convertir la chaîne de date-heure en Timestamp
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
            java.util.Date parsedDate = dateFormat.parse(dateHeureStr);
            Timestamp timestamp = new Timestamp(parsedDate.getTime());
            reservation.setDateHeure(timestamp);
        } catch (Exception e) {
            System.err.println("❌ Erreur de parsing de date : " + e.getMessage());
        }

        ModelView mv = new ModelView();

        if (reservationDAO.update(reservation)) {
            mv.setView("redirect:/reservations");
            mv.addAttribute("message", "Réservation mise à jour");
        } else {
            mv.setView("reservations/form.jsp");
            mv.addAttribute("error", "Erreur lors de la mise à jour");
            mv.addAttribute("reservation", reservation);

            // Recharger la liste des hotels
            List<Hotel> hotels = hotelDAO.findAll();
            mv.addAttribute("hotels", hotels);
        }

        return mv;
    }
}