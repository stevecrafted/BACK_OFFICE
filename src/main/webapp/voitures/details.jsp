<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.example.Model.Voiture" %>
<!DOCTYPE html>
<html>
<head>
    <title>Détails Voiture</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; max-width: 600px; }
        .card {
            border: 1px solid #ddd;
            border-radius: 5px;
            padding: 20px;
            margin-top: 20px;
            background-color: #f9f9f9;
        }
        .detail-row {
            margin-bottom: 15px;
            padding-bottom: 10px;
            border-bottom: 1px solid #ddd;
        }
        .detail-row:last-child { border-bottom: none; }
        .label {
            font-weight: bold;
            color: #555;
            display: inline-block;
            width: 150px;
        }
        .value { color: #333; }
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
        .btn-secondary { background-color: #6c757d; color: white; }
    </style>
</head>
<body>
    <h1>🚗 Détails de la Voiture</h1>

    <% 
        Voiture voiture = (Voiture) request.getAttribute("voiture");
        if (voiture != null) {
    %>
        <div class="card">
            <div class="detail-row">
                <span class="label">ID:</span>
                <span class="value"><%= voiture.getIdVoiture() %></span>
            </div>
            
            <div class="detail-row">
                <span class="label">Référence:</span>
                <span class="value"><strong><%= voiture.getRef() %></strong></span>
            </div>
            
            <div class="detail-row">
                <span class="label">Capacité:</span>
                <span class="value"><%= voiture.getCapacite() %> places</span>
            </div>
            
            <div class="detail-row">
                <span class="label">Carburant:</span>
                <span class="value"><%= voiture.getCarburantLibelle() %></span>
            </div>
        </div>

        <div style="margin-top: 20px;">
            <a href="/voitures" class="btn btn-primary">📋 Retour</a>
            <a href="/voitures/edit?id=<%= voiture.getIdVoiture() %>" class="btn btn-warning">✏️ Modifier</a>
        </div>
    <% } else { %>
        <p style="color: #dc3545;">Voiture non trouvée.</p>
        <a href="/voitures" class="btn btn-secondary">Retour</a>
    <% } %>
</body>
</html>
