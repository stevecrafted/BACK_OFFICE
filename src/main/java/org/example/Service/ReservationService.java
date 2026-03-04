package org.example.Service;

import org.example.DAO.ReservationDAO;
import org.example.Model.Reservation;
import org.example.DTO.ReservationDTO;

import java.util.List;
import java.util.stream.Collectors;

public class ReservationService {
    
    private ReservationDAO reservationDAO;
    
    public ReservationService() {
        this.reservationDAO = new ReservationDAO();
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
            reservation.getIdHotel(),
            reservation.getIdClient(),
            reservation.getNbPassager(),
            reservation.getDateHeure()
        );
    }
    
}