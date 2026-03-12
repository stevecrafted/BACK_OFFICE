<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="org.example.Model.Voiture" %>
<!DOCTYPE html>
<html>
<head>
    <title>Liste des Voitures</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; }
        .btn { 
            padding: 10px 20px; 
            border: none; 
            border-radius: 3px; 
            cursor: pointer; 
            text-decoration: none;
            display: inline-block;
            margin: 5px;
        }
        .btn-primary { background-color: #007bff; color: white; }
        .btn-warning { background-color: #ffc107; color: black; }
        .btn-danger { background-color: #dc3545; color: white; }
        .btn-info { background-color: #17a2b8; color: white; }
        .btn-secondary { background-color: #6c757d; color: white; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
        th { background-color: #007bff; color: white; }
        tr:hover { background-color: #f5f5f5; }
        .message { padding: 10px; margin-bottom: 20px; border-radius: 3px; }
        .success { background-color: #d4edda; color: #155724; }
        .error { background-color: #f8d7da; color: #721c24; }
        .filter-section {
            background: #f8f9fa;
            padding: 15px;
            border-radius: 5px;
            margin-bottom: 20px;
            border: 1px solid #dee2e6;
        }
        .filter-section label { font-weight: bold; margin-right: 10px; }
        .filter-section input[type="date"] { 
            padding: 8px 12px; 
            border: 1px solid #ddd; 
            border-radius: 3px; 
            font-size: 14px; 
        }
    </style>
</head>
<body>
    <h1>🚗 Liste des Voitures</h1>

    <% String message = (String) request.getAttribute("message"); %>
    <% if (message != null) { %>
        <div class="message success"><%= message %></div>
    <% } %>

    <% String error = (String) request.getAttribute("error"); %>
    <% if (error != null) { %>
        <div class="message error"><%= error %></div>
    <% } %>

    <a href="/voitures/nouveau" class="btn btn-primary">Nouvelle Voiture</a>
    <a href="/api/voitures" class="btn btn-info">API JSON</a>

    <% 
        List<Voiture> voitures = (List<Voiture>) request.getAttribute("voitures");
        if (voitures != null && !voitures.isEmpty()) {
    %>
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Référence</th>
                    <th>Capacité</th>
                    <th>Carburant</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <% for (Voiture v : voitures) { %>
                    <tr>
                        <td><%= v.getIdVoiture() %></td>
                        <td><strong><%= v.getRef() %></strong></td>
                        <td><%= v.getCapacite() %> places</td>
                        <td><%= v.getCarburantLibelle() %></td>
                        <td>
                            <a href="/voitures/details?id=<%= v.getIdVoiture() %>" class="btn btn-info">👁️</a>
                            <a href="/voitures/edit?id=<%= v.getIdVoiture() %>" class="btn btn-warning">✏️</a>
                            <form method="POST" action="/voitures/delete" style="display:inline;">
                                <input type="hidden" name="id" value="<%= v.getIdVoiture() %>">
                                <button type="submit" class="btn btn-danger" onclick="return confirm('Supprimer cette voiture ?')">🗑️</button>
                            </form>
                        </td>
                    </tr>
                <% } %>
            </tbody>
        </table>
    <% } else { %>
        <p style="margin-top: 20px; color: #666;">Aucune voiture trouvée.</p>
    <% } %>
</body>
</html>
