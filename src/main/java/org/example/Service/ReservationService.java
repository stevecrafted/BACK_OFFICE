package org.example.Service;

import org.example.DAO.ReservationDAO;
import org.example.Model.Reservation;
import org.example.DTO.ReservationDTO;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReservationService {
    
    private ReservationDAO reservationDAO;
    
    public ReservationService() {
        this.reservationDAO = new ReservationDAO();
    } 
    
    /**
     * Récupère les réservations par date
     */
    public List<ReservationDTO> getListReservationByDate(Date date) {
        if (date == null) {
            System.err.println("La date ne peut pas être null");
            return new ArrayList<>();
        }
        
        System.out.println("Recherche des réservations pour la date : " + date);
        List<Reservation> reservations = reservationDAO.findByDate(date);
        
        // Convertir en DTO
        return reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Récupère toutes les réservations sous forme de DTO
     */
    public List<ReservationDTO> getAllReservations() {
        List<Reservation> reservations = reservationDAO.findAll();
        
        return reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Convertit une Reservation en ReservationDTO
     */
    public ReservationDTO convertToDTO(Reservation reservation) {
        if (reservation == null) {
            return null;
        }
        
        return new ReservationDTO(
            reservation.getId(),
            reservation.getIdLieu(),
            reservation.getIdClient(),
            reservation.getNbPassager(),
            reservation.getDateHeure()
        );
    }
    
}