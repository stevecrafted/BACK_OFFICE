<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.example.Model.*" %>
<%@ page import="org.example.DAO.HotelDAO" %>
<%@ page import="java.util.*" %>
<!DOCTYPE html>
<html>
<head>
    <title>Simulation d'Assignation</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 {
            color: #333;
            border-bottom: 3px solid #4CAF50;
            padding-bottom: 10px;
        }
        .form-section {
            background: #f9f9f9;
            padding: 20px;
            border-radius: 5px;
            margin-bottom: 20px;
        }
        .form-group {
            margin-bottom: 15px;
        }
        label {
            display: block;
            font-weight: bold;
            margin-bottom: 5px;
            color: #555;
        }
        input[type="date"] {
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 4px;
            font-size: 14px;
            width: 200px;
        }
        button {
            padding: 10px 20px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 14px;
            margin-right: 10px;
        }
        .btn-primary {
            background-color: #4CAF50;
            color: white;
        }
        .btn-primary:hover {
            background-color: #45a049;
        }
        .btn-success {
            background-color: #2196F3;
            color: white;
        }
        .btn-success:hover {
            background-color: #0b7dda;
        }
        .btn-secondary {
            background-color: #757575;
            color: white;
        }
        .btn-secondary:hover {
            background-color: #616161;
        }
        .alert {
            padding: 15px;
            margin-bottom: 20px;
            border-radius: 4px;
        }
        .alert-success {
            background-color: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }
        .alert-error {
            background-color: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }
        .alert-info {
            background-color: #d1ecf1;
            color: #0c5460;
            border: 1px solid #bee5eb;
        }
        .summary {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
            margin-bottom: 20px;
        }
        .summary-card {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px;
            border-radius: 8px;
            text-align: center;
        }
        .summary-card h3 {
            margin: 0 0 10px 0;
            font-size: 32px;
        }
        .summary-card p {
            margin: 0;
            font-size: 14px;
            opacity: 0.9;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        th, td {
            padding: 12px;
            text-align: left;
            border-bottom: 1px solid #ddd;
        }
        th {
            background-color: #4CAF50;
            color: white;
            font-weight: bold;
        }
        tr:hover {
            background-color: #f5f5f5;
        }
        .vague-header {
            background-color: #2196F3;
            color: white;
            padding: 10px;
            margin-top: 20px;
            border-radius: 4px;
            font-weight: bold;
        }
        .badge {
            display: inline-block;
            padding: 4px 8px;
            border-radius: 3px;
            font-size: 12px;
            font-weight: bold;
        }
        .badge-diesel {
            background-color: #ff9800;
            color: white;
        }
        .badge-essence {
            background-color: #9c27b0;
            color: white;
        }
        .no-data {
            text-align: center;
            padding: 40px;
            color: #999;
            font-style: italic;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🎯 Simulation d'Assignation de Voitures</h1>

        <%
            String error = (String) request.getAttribute("error");
            String message = (String) request.getAttribute("message");
            ResultatSimulation resultat = (ResultatSimulation) request.getAttribute("resultat");
            String dateSimulation = (String) request.getAttribute("dateSimulation");
        %>

        <% if (error != null) { %>
            <div class="alert alert-error">❌ <%= error %></div>
        <% } %>

        <% if (message != null) { %>
            <div class="alert alert-success">✅ <%= message %></div>
        <% } %>

        <!-- Formulaire de simulation -->
        <div class="form-section">
            <form method="POST" action="/assignations/simuler">
                <div class="form-group">
                    <label for="date">📅 Sélectionner une date :</label>
                    <input type="date" id="date" name="date" 
                           value="<%= dateSimulation != null ? dateSimulation : "" %>" 
                           required>
                </div>
                <button type="submit" class="btn-primary">🚀 Lancer la Simulation</button>
                <a href="/assignations"><button type="button" class="btn-secondary">← Retour</button></a>
            </form>
        </div>

        <% if (resultat != null) { %>
            <!-- Résumé de la simulation -->
            <div class="summary">
                <div class="summary-card" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
                    <h3><%= resultat.getNbVagues() %></h3>
                    <p>🌊 Vagues de traitement</p>
                </div>
                <div class="summary-card" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
                    <h3><%= resultat.getTotalReservationsAssignees() %></h3>
                    <p>✅ Réservations assignées</p>
                </div>
                <div class="summary-card" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
                    <h3><%= resultat.getAssignations().size() %></h3>
                    <p>🚗 Voitures utilisées</p>
                </div>
                <div class="summary-card" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);">
                    <h3><%= resultat.getReservationsNonAssignees().size() %></h3>
                    <p>❌ Non assignées</p>
                </div>
            </div>

            <% if (resultat.getTotalReservationsAssignees() > 0) { %>
                <!-- Bouton de confirmation -->
                <div style="margin: 20px 0; text-align: center;">
                    <form method="POST" action="/assignations/confirmer" style="display: inline;">
                        <input type="hidden" name="date" value="<%= dateSimulation %>">
                        <button type="submit" class="btn-success" 
                                onclick="return confirm('Confirmer l\'enregistrement de <%= resultat.getTotalReservationsAssignees() %> assignation(s) ?')">
                            ✔️ Confirmer et Enregistrer
                        </button>
                    </form>
                </div>
            <% } %>

            <!-- Détails des assignations par vague -->
            <%
                Map<java.sql.Timestamp, List<SimulationAssignation>> assignationsParVague = new TreeMap<>();
                for (SimulationAssignation sa : resultat.getAssignations()) {
                    assignationsParVague.computeIfAbsent(sa.getHeureVague(), k -> new ArrayList<>()).add(sa);
                }

                int numeroVague = 1;
                for (Map.Entry<java.sql.Timestamp, List<SimulationAssignation>> entry : assignationsParVague.entrySet()) {
            %>
                <div class="vague-header">
                    🌊 VAGUE #<%= numeroVague %> - <%= entry.getKey() %>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th>Voiture</th>
                            <th>Référence</th>
                            <th>Capacité</th>
                            <th>Carburant</th>
                            <th>Réservations (Client → Hôtel)</th>
                            <th>Passagers</th>
                            <th>Places Restantes</th>
                        </tr>
                    </thead>
                    <tbody>
                        <%
                            HotelDAO hotelDAO = new HotelDAO();
                            for (SimulationAssignation sa : entry.getValue()) {
                                Voiture voiture = sa.getVoiture();
                                int totalPassagers = 0;
                                for (Reservation r : sa.getReservations()) {
                                    totalPassagers += r.getNbPassager();
                                }
                        %>
                        <tr>
                            <td>#<%= voiture.getIdVoiture() %></td>
                            <td><strong><%= voiture.getRef() %></strong></td>
                            <td><%= voiture.getCapacite() %> places</td>
                            <td>
                                <% if (voiture.getCarburant() != null) { %>
                                    <span class="badge <%= voiture.getCarburant().getLibelle().equalsIgnoreCase("Diesel") ? "badge-diesel" : "badge-essence" %>">
                                        <%= voiture.getCarburant().getLibelle() %>
                                    </span>
                                <% } %>
                            </td>
                            <td>
                                <% for (Reservation r : sa.getReservations()) { 
                                    Hotel hotel = hotelDAO.findById(r.getIdHotel());
                                    String hotelNom = hotel != null ? hotel.getNom() : "Hotel #" + r.getIdHotel();
                                %>
                                    #<%= r.getId() %>: <%= r.getIdClient() %> → <%= hotelNom %> (<%= r.getNbPassager() %>p)<br>
                                <% } %>
                            </td>
                            <td><strong><%= totalPassagers %></strong></td>
                            <td><%= sa.getPlacesRestantes() %></td>
                        </tr>
                        <% } %>
                    </tbody>
                </table>
            <%
                    numeroVague++;
                }
            %>

            <!-- Réservations non assignées -->
            <% if (!resultat.getReservationsNonAssignees().isEmpty()) { 
                HotelDAO hotelDAO2 = new HotelDAO();
            %>
                <div class="vague-header" style="background-color: #f44336;">
                    ❌ RÉSERVATIONS NON ASSIGNÉES
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Client</th>
                            <th>Hôtel</th>
                            <th>Passagers</th>
                            <th>Date/Heure</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% for (Reservation r : resultat.getReservationsNonAssignees()) { 
                            Hotel hotel = hotelDAO2.findById(r.getIdHotel());
                            String hotelNom = hotel != null ? hotel.getNom() : "Hotel #" + r.getIdHotel();
                        %>
                        <tr>
                            <td>#<%= r.getId() %></td>
                            <td><%= r.getIdClient() %></td>
                            <td><%= hotelNom %></td>
                            <td><%= r.getNbPassager() %></td>
                            <td><%= r.getDateHeure() %></td>
                        </tr>
                        <% } %>
                    </tbody>
                </table>
            <% } %>

        <% } else { %>
            <div class="no-data">
                📋 Sélectionnez une date et lancez la simulation pour voir les résultats
            </div>
        <% } %>
    </div>
</body>
</html>
