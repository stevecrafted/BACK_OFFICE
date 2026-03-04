<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="org.example.Model.Hotel" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Nouvelle Réservation</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 600px;
            margin: 0 auto;
            background-color: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 {
            color: #333;
            border-bottom: 3px solid #2196F3;
            padding-bottom: 10px;
        }
        .form-group {
            margin-bottom: 20px;
        }
        label {
            display: block;
            margin-bottom: 5px;
            color: #555;
            font-weight: bold;
        }
        input, select {
            width: 100%;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 4px;
            box-sizing: border-box;
            font-size: 14px;
        }
        input:focus, select:focus {
            outline: none;
            border-color: #2196F3;
        }
        .btn {
            padding: 12px 24px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 16px;
            margin-right: 10px;
        }
        .btn-primary {
            background-color: #2196F3;
            color: white;
        }
        .btn-primary:hover {
            background-color: #0b7dda;
        }
        .btn-secondary {
            background-color: #6c757d;
            color: white;
            text-decoration: none;
            display: inline-block;
        }
        .btn-secondary:hover {
            background-color: #5a6268;
        }
        .message {
            padding: 10px;
            margin: 10px 0;
            border-radius: 4px;
        }
        .error {
            background-color: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>📋 Nouvelle Réservation</h1>

        <% if (request.getAttribute("error") != null) { %>
            <div class="message error">
                <%= request.getAttribute("error") %>
            </div>
        <% } %>

        <form action="/reservations/create" method="post">
            <div class="form-group">
                <label for="idClient">Client *</label>
                <input type="text" id="idClient" name="idClient" required 
                       placeholder="Ex: CLIENT001">
            </div>

            <div class="form-group">
                <label for="nbPassager">Nombre de Passagers *</label>
                <input type="number" id="nbPassager" name="nbPassager" required 
                       min="1" max="50" placeholder="Ex: 4">
            </div>

            <div class="form-group">
                <label for="idHotel">Hotel *</label>
                <select id="idHotel" name="idHotel" required>
                    <option value="">-- Sélectionner un hotel --</option>
                    <%
                        List<Hotel> hotels = (List<Hotel>) request.getAttribute("hotels");
                        if (hotels != null) {
                            for (Hotel hotel : hotels) {
                    %>
                        <option value="<%= hotel.getId() %>">
                            <%= hotel.getNom() %> (ID: <%= hotel.getId() %>)
                        </option>
                    <%
                            }
                        }
                    %>
                </select>
            </div>

            <div class="form-group">
                <label for="dateHeure">Date et Heure *</label>
                <input type="datetime-local" id="dateHeure" name="dateHeure" required>
            </div>

            <div class="form-group">
                <button type="submit" class="btn btn-primary">✓ Créer la Réservation</button>
                <a href="/reservations" class="btn btn-secondary">Annuler</a>
            </div>
        </form>
    </div>
</body>
</html>
