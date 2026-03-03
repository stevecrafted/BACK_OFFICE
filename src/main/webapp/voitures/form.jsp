<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="org.example.Model.Voiture" %>
<%@ page import="org.example.Model.Carburant" %>
<!DOCTYPE html>
<html>
<head>
    <title><%= "create".equals(request.getAttribute("action")) ? "Nouvelle Voiture" : "Modifier Voiture" %></title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; max-width: 600px; }
        .form-group { margin-bottom: 15px; }
        label { display: block; margin-bottom: 5px; font-weight: bold; }
        input[type="number"], select {
            width: 100%; 
            padding: 8px; 
            border: 1px solid #ddd; 
            border-radius: 3px; 
            box-sizing: border-box;
        }
        .btn { 
            padding: 10px 20px; 
            border: none; 
            border-radius: 3px; 
            cursor: pointer; 
            text-decoration: none;
            display: inline-block;
        }
        .btn-success { background-color: #28a745; color: white; }
        .btn-secondary { background-color: #6c757d; color: white; }
        .message { padding: 10px; margin-bottom: 20px; border-radius: 3px; }
        .error { background-color: #f8d7da; color: #721c24; }
        .info { background-color: #d1ecf1; color: #0c5460; padding: 10px; border-radius: 3px; margin-bottom: 15px; }
    </style>
</head>
<body>
    <h1><%= "create".equals(request.getAttribute("action")) ? "🚗 Nouvelle Voiture" : "✏️ Modifier Voiture" %></h1>

    <% String error = (String) request.getAttribute("error"); %>
    <% if (error != null) { %>
        <div class="message error"><%= error %></div>
    <% } %>

    <% 
        String action = (String) request.getAttribute("action");
        Voiture voiture = (Voiture) request.getAttribute("voiture");
        List<Carburant> carburants = (List<Carburant>) request.getAttribute("carburants");
        String formAction = "create".equals(action) ? "/voitures/create" : "/voitures/update";
    %>

    <% if ("create".equals(action)) { %>
        <div class="info">ℹ️ La référence sera générée automatiquement (ex: VOI0001)</div>
    <% } %>

    <form method="POST" action="<%= formAction %>">
        
        <% if ("update".equals(action) && voiture != null) { %>
            <input type="hidden" name="id" value="<%= voiture.getIdVoiture() %>">
            <input type="hidden" name="ref" value="<%= voiture.getRef() %>">
            <div class="form-group">
                <label>Référence actuelle</label>
                <input type="text" value="<%= voiture.getRef() %>" disabled style="background-color: #e9ecef;">
            </div>
        <% } %>

        <div class="form-group">
            <label for="capacite">Capacité (nombre de places) *</label>
            <input type="number" 
                   id="capacite" 
                   name="capacite" 
                   min="1"
                   max="50"
                   value="<%= voiture != null ? voiture.getCapacite() : "" %>" 
                   required>
        </div>

        <div class="form-group">
            <label for="idCarburant">Type de Carburant *</label>
            <select id="idCarburant" name="idCarburant" required>
                <option value="">-- Sélectionnez un carburant --</option>
                <% 
                    if (carburants != null) {
                        for (Carburant c : carburants) {
                            boolean selected = voiture != null && voiture.getCarburant() != null && voiture.getCarburant().getIdCarburant() == c.getIdCarburant();
                %>
                    <option value="<%= c.getIdCarburant() %>" <%= selected ? "selected" : "" %>>
                        <%= c.getLibelle() %>
                    </option>
                <% 
                        }
                    }
                %>
            </select>
        </div>

        <div class="form-group">
            <button type="submit" class="btn btn-success">
                <%= "create".equals(action) ? "➕ Créer" : "💾 Mettre à jour" %>
            </button>
            <a href="/voitures" class="btn btn-secondary">❌ Annuler</a>
        </div>
    </form>
</body>
</html>
