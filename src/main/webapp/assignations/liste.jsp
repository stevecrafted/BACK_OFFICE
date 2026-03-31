<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.example.Model.*" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Liste des Assignations</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background-color: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 {
            color: #333;
            border-bottom: 3px solid #4CAF50;
            padding-bottom: 10px;
        }
        .actions {
            margin: 20px 0;
        }
        .btn {
            padding: 10px 20px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            text-decoration: none;
            display: inline-block;
            font-size: 14px;
        }
        .btn-primary {
            background-color: #4CAF50;
            color: white;
        }
        .btn-primary:hover {
            background-color: #45a049;
        }
        .btn-danger {
            background-color: #f44336;
            color: white;
        }
        .btn-danger:hover {
            background-color: #da190b;
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
        }
        tr:hover {
            background-color: #f5f5f5;
        }
        .message {
            padding: 10px;
            margin: 10px 0;
            border-radius: 4px;
        }
        .success {
            background-color: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }
        .error {
            background-color: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }
        .badge {
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: bold;
        }
        .badge-info {
            background-color: #17a2b8;
            color: white;
        }
        .badge-success {
            background-color: #28a745;
            color: white;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🚗 Assignations Voitures - Réservations</h1>

        <% if (request.getAttribute("message") != null) { %>
            <div class="message success">
                <%= request.getAttribute("message") %>
            </div>
        <% } %>

        <% if (request.getAttribute("error") != null) { %>
            <div class="message error">
                <%= request.getAttribute("error") %>
            </div>
        <% } %>

        <div class="actions">
            <a href="/assignations/simuler" class="btn btn-primary">
                🎯 Nouvelle Simulation
            </a>
            <form action="/assignations/auto" method="post" style="display: inline;">
                <button type="submit" class="btn btn-primary">
                    ⚡ Lancer Assignation Automatique
                </button>
            </form>
        </div>

        <%
            List<Map<String, Object>> assignations = (List<Map<String, Object>>) request.getAttribute("assignations");
            if (assignations != null && !assignations.isEmpty()) {
        %>
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Voiture</th>
                        <th>Capacité</th>
                        <th>Réservation</th>
                        <th>Client</th>
                        <th>Passagers</th>
                        <th>Lieu</th>
                        <th>Départ</th>
                        <th>Arrivée</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <%
                        for (Map<String, Object> detail : assignations) {
                            Assignation assignation = (Assignation) detail.get("assignation");
                            Voiture voiture = (Voiture) detail.get("voiture");
                            Reservation reservation = (Reservation) detail.get("reservation");
                            List<Reservation> reservations = (List<Reservation>) detail.get("reservations");
                            int totalPassagers = 0;
                            if (reservations != null) {
                                for (Reservation res : reservations) {
                                    totalPassagers += res.getNbPassager();
                                }
                            }
                    %>
                        <tr>
                            <td><span class="badge badge-info">#<%= assignation.getId() %></span></td>
                            <td>
                                <strong><%= voiture.getRef() %></strong><br>
                                <small>ID: <%= voiture.getIdVoiture() %></small>
                            </td>
                            <td><span class="badge badge-success"><%= voiture.getCapacite() %> places</span></td>
                            <td>
                                <%= reservations != null ? reservations.size() : 0 %> réservation(s)
                                <% if (reservation != null) { %><br><small>#<%= reservation.getId() %> ...</small><% } %>
                            </td>
                            <td><%= reservation != null ? reservation.getIdClient() : "-" %></td>
                            <td><strong><%= totalPassagers %></strong> passagers</td>
                            <td><%= reservation != null ? ("Lieu #" + reservation.getIdLieu()) : "-" %></td>
                            <td><%= assignation.getDateHeureDepart() %></td>
                            <td><%= assignation.getDateHeureArrivee() %></td>
                            <td>
                                <form action="/assignations/delete" method="post" style="display: inline;" 
                                      onsubmit="return confirm('Supprimer cette assignation ?');">
                                    <input type="hidden" name="id" value="<%= assignation.getId() %>">
                                    <button type="submit" class="btn btn-danger">Supprimer</button>
                                </form>
                            </td>
                        </tr>
                    <% } %>
                </tbody>
            </table>
        <% } else { %>
            <p style="text-align: center; color: #666; padding: 40px;">
                Aucune assignation trouvée. Cliquez sur "Lancer Assignation Automatique" pour commencer.
            </p>
        <% } %>
    </div>
</body>
</html>
