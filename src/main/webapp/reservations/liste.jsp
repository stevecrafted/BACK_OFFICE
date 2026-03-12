<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.example.Model.*" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Liste des Réservations</title>
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
            border-bottom: 3px solid #2196F3;
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
            background-color: #2196F3;
            color: white;
        }
        .btn-primary:hover {
            background-color: #0b7dda;
        }
        .btn-success {
            background-color: #4CAF50;
            color: white;
        }
        .btn-success:hover {
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
            background-color: #2196F3;
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
        .badge-warning {
            background-color: #ffc107;
            color: #333;
        }
        .status-assigned {
            color: #28a745;
            font-weight: bold;
        }
        .status-pending {
            color: #ffc107;
            font-weight: bold;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>📋 Liste des Réservations</h1>

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
            <a href="/reservations/nouveau" class="btn btn-primary">➕ Nouvelle Réservation</a>
            <a href="/assignations" class="btn btn-success">🚗 Voir les Assignations</a>
        </div>

        <%
            List<Map<String, Object>> reservations = (List<Map<String, Object>>) request.getAttribute("reservations");
            if (reservations != null && !reservations.isEmpty()) {
        %>
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Client</th>
                        <th>Passagers</th>
                        <th>Lieu</th>
                        <th>Date/Heure</th>
                        <th>Statut</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <%
                        for (Map<String, Object> detail : reservations) {
                            Reservation reservation = (Reservation) detail.get("reservation");
                            Lieu lieu = (Lieu) detail.get("lieu");
                            boolean estAssignee = (Boolean) detail.get("estAssignee");
                    %>
                        <tr>
                            <td><span class="badge badge-info">#<%= reservation.getId() %></span></td>
                            <td><strong><%= reservation.getIdClient() %></strong></td>
                            <td><span class="badge badge-success"><%= reservation.getNbPassager() %> pers.</span></td>
                            <td>
                                <% if (lieu != null) { %>
                                    <%= lieu.getLibelle() %><br>
                                    <small><%= lieu.getCode() %></small>
                                <% } else { %>
                                    Lieu #<%= reservation.getIdLieu() %>
                                <% } %>
                            </td>
                            <td><%= reservation.getDateHeure() %></td>
                            <td>
                                <% if (estAssignee) { %>
                                    <span class="status-assigned">✓ Assignée</span>
                                <% } else { %>
                                    <span class="status-pending">⏳ En attente</span>
                                <% } %>
                            </td>
                            <td>
                                <form action="/reservations/delete" method="post" style="display: inline;" 
                                      onsubmit="return confirm('Supprimer cette réservation ?');">
                                    <input type="hidden" name="id" value="<%= reservation.getId() %>">
                                    <button type="submit" class="btn btn-danger">Supprimer</button>
                                </form>
                            </td>
                        </tr>
                    <% } %>
                </tbody>
            </table>
        <% } else { %>
            <p style="text-align: center; color: #666; padding: 40px;">
                Aucune réservation trouvée. Cliquez sur "Nouvelle Réservation" pour commencer.
            </p>
        <% } %>
    </div>
</body>
</html>
