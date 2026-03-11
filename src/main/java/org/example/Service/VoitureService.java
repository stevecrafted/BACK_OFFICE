package org.example.Service;

import org.example.DAO.VoitureDAO;
import org.example.Model.Voiture;
import org.example.DTO.VoitureDTO;

import java.util.List;
import java.util.stream.Collectors;

public class VoitureService {
    
    private VoitureDAO voitureDAO;
    
    public VoitureService() {
        this.voitureDAO = new VoitureDAO();
    }
    
    /**
     * Récupère toutes les voitures sous forme de DTO
     */
    public List<VoitureDTO> getAllVoitures() {
        List<Voiture> voitures = voitureDAO.findAll();
        
        return voitures.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Récupère les voitures par carburant
     */
    public List<VoitureDTO> getVoituresByCarburant(String carburant) {
        System.out.println("Recherche des voitures pour le carburant : " + carburant);
        List<Voiture> voitures = voitureDAO.findByCarburant(carburant);
        
        return voitures.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    
    /**
     * Convertit une Voiture en VoitureDTO
     */
    public VoitureDTO convertToDTO(Voiture voiture) {
        if (voiture == null) {
            return null;
        }
        
        return new VoitureDTO(
            voiture.getIdVoiture(),
            voiture.getCapacite(),
            voiture.getRef(),
            voiture.getCarburant(),
            voiture.getCarburantLibelle()
        );
    }
}
