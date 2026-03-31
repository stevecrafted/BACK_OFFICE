package org.example.Service;

import org.example.DAO.VoitureDAO;
import org.example.Model.Voiture;
import org.example.DTO.VoitureDTO;
import org.example.Util.UtilSimulation;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    
    // ========================================
    // SPRINT 8: FEATURE 1 - Return Vehicle Selection
    // ========================================
    
    /**
     * SPRINT 8 - Feature 1.1
     * Trouve le meilleur véhicule parmi ceux qui reviennent pour transporter des passagers.
     * 
     * Algorithme:
     * 1. Filtrer les véhicules "fullFit" (capaciteRestante >= nombrePassager)
     * 2. Si fullFit existe: trier par capaciteRestante croissante et choisir le meilleur
     * 3. Sinon: filtrer les véhicules "partialFit" (0 < capaciteRestante < nombrePassager)
     * 4. Si partialFit existe: trier par capaciteRestante décroissante et choisir le meilleur
     * 5. Sinon: retourner null
     * 
     * @param vehicules Liste des véhicules disponibles (avec capaciteRestante calculée)
     * @param nombrePassager Nombre de passagers à transporter
     * @param compteurTrajetsJour Compteur de trajets par véhicule pour la journée
     * @return Le meilleur véhicule ou null si aucun disponible
     */
    public Voiture getRetourVehicule(List<VoitureAvecCapacite> vehicules, int nombrePassager, 
                                     Map<Integer, Integer> compteurTrajetsJour) {
        if (vehicules == null || vehicules.isEmpty()) {
            return null;
        }
        
        Voiture vehiculeResultat = null;
        
        // 1. Filtrer les véhicules "fullFit" (peuvent prendre TOUS les passagers)
        List<VoitureAvecCapacite> fullFit = new ArrayList<>();
        for (VoitureAvecCapacite v : vehicules) {
            if (v.capaciteRestante >= nombrePassager) {
                fullFit.add(v);
            }
        }
        
        // 2. Trier fullFit par capaciteRestante CROISSANTE (le plus petit qui convient)
        fullFit.sort((v1, v2) -> Integer.compare(v1.capaciteRestante, v2.capaciteRestante));
        
        // 3. Si fullFit n'est pas vide, choisir le meilleur
        if (!fullFit.isEmpty()) {
            List<Voiture> voituresFullFit = new ArrayList<>();
            for (VoitureAvecCapacite v : fullFit) {
                voituresFullFit.add(v.voiture);
            }
            vehiculeResultat = UtilSimulation.trouverVoitureCarburantNbrTrajet(
                voituresFullFit, compteurTrajetsJour, nombrePassager);
        } else {
            // 4. Filtrer les véhicules "partialFit" (peuvent prendre une partie)
            List<VoitureAvecCapacite> partialFit = new ArrayList<>();
            for (VoitureAvecCapacite v : vehicules) {
                if (v.capaciteRestante > 0 && v.capaciteRestante < nombrePassager) {
                    partialFit.add(v);
                }
            }
            
            // 5. Trier partialFit par capaciteRestante DÉCROISSANTE (prendre le maximum)
            partialFit.sort((v1, v2) -> Integer.compare(v2.capaciteRestante, v1.capaciteRestante));
            
            // 6. Si partialFit n'est pas vide, choisir le meilleur
            if (!partialFit.isEmpty()) {
                List<Voiture> voituresPartialFit = new ArrayList<>();
                for (VoitureAvecCapacite v : partialFit) {
                    voituresPartialFit.add(v.voiture);
                }
                vehiculeResultat = UtilSimulation.trouverVoitureCarburantNbrTrajet(
                    voituresPartialFit, compteurTrajetsJour, nombrePassager);
            }
        }
        
        return vehiculeResultat;
    }
    
    /**
     * SPRINT 8 - Feature 1.2
     * Récupère les véhicules qui reviennent en PREMIER (même heure de retour minimale).
     * 
     * @param voituresEnTrajet Map des véhicules en trajet avec leur heure de retour
     * @param dateHeureFin Début de la fenêtre de recherche
     * @param dateHeureProchain Fin de la fenêtre de recherche
     * @return Liste des véhicules qui reviennent en premier, ou null si aucun
     */
    public List<VoitureAvecCapacite> getPremiersVehicules(Map<Integer, Timestamp> voituresEnTrajet,
                                                          Timestamp dateHeureFin, 
                                                          Timestamp dateHeureProchain) {
        if (voituresEnTrajet == null || voituresEnTrajet.isEmpty()) {
            return null;
        }
        
        // 1. Filtrer les véhicules qui reviennent dans la fenêtre [dateHeureFin, dateHeureProchain]
        List<VoitureAvecRetour> vehiculesRetour = new ArrayList<>();
        
        for (Map.Entry<Integer, Timestamp> entry : voituresEnTrajet.entrySet()) {
            Timestamp heureRetour = entry.getValue();
            
            // Vérifier si le retour est dans la fenêtre
            if (heureRetour != null && 
                !heureRetour.before(dateHeureFin) && 
                heureRetour.before(dateHeureProchain)) {
                
                Voiture voiture = voitureDAO.findById(entry.getKey());
                if (voiture != null) {
                    vehiculesRetour.add(new VoitureAvecRetour(voiture, heureRetour));
                }
            }
        }
        
        if (vehiculesRetour.isEmpty()) {
            return null;
        }
        
        // 2. Trier par heure de retour
        vehiculesRetour.sort((v1, v2) -> v1.heureRetour.compareTo(v2.heureRetour));
        
        // 3. Trouver l'heure de retour minimale
        Timestamp minRetour = vehiculesRetour.get(0).heureRetour;
        
        // 4. Collecter tous les véhicules qui reviennent à cette heure minimale
        List<VoitureAvecCapacite> premiersVehicules = new ArrayList<>();
        
        for (VoitureAvecRetour v : vehiculesRetour) {
            if (v.heureRetour.equals(minRetour)) {
                // Capacité restante = capacité totale (véhicule vient de revenir)
                premiersVehicules.add(new VoitureAvecCapacite(v.voiture, v.voiture.getCapacite()));
            } else {
                break; // Les suivants reviennent plus tard
            }
        }
        
        return premiersVehicules;
    }
    
    // ========================================
    // CLASSES INTERNES POUR SPRINT 8
    // ========================================
    
    /**
     * Classe pour stocker un véhicule avec sa capacité restante
     */
    public static class VoitureAvecCapacite {
        public Voiture voiture;
        public int capaciteRestante;
        
        public VoitureAvecCapacite(Voiture voiture, int capaciteRestante) {
            this.voiture = voiture;
            this.capaciteRestante = capaciteRestante;
        }
    }
    
    /**
     * Classe pour stocker un véhicule avec son heure de retour
     */
    private static class VoitureAvecRetour {
        public Voiture voiture;
        public Timestamp heureRetour;
        
        public VoitureAvecRetour(Voiture voiture, Timestamp heureRetour) {
            this.voiture = voiture;
            this.heureRetour = heureRetour;
        }
    }
}
