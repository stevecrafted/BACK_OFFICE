<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.example.Model.*" %>
<%@ page import="org.example.DAO.LieuDAO" %>
<%@ page import="org.example.DAO.ReservationDAO" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.text.DecimalFormat" %>
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
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .vague-header label {
            color: white;
            margin: 0;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 5px;
        }
        .vague-header input[type="checkbox"] {
            width: 18px;
            height: 18px;
            cursor: pointer;
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
        .itineraire {
            font-size: 12px;
            line-height: 1.8;
        }
        .itineraire .etape {
            display: flex;
            align-items: center;
            gap: 5px;
            white-space: nowrap;
        }
        .itineraire .etape-num {
            background: #4CAF50;
            color: white;
            border-radius: 50%;
            width: 20px;
            height: 20px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-size: 10px;
            font-weight: bold;
            flex-shrink: 0;
        }
        .itineraire .etape-detail {
            color: #666;
            font-size: 11px;
        }
        .itineraire .total-trajet {
            margin-top: 5px;
            padding-top: 5px;
            border-top: 1px dashed #ccc;
            font-weight: bold;
            color: #333;
        }
        .existing-section {
            border: 2px solid #4CAF50;
            border-radius: 8px;
            padding: 15px;
            margin-bottom: 20px;
            background-color: #f1f8e9;
        }
        .existing-section h2 {
            color: #2e7d32;
            margin-top: 0;
        }
        .existing-section th {
            background-color: #388e3c;
        }
        .cb-line {
            width: 18px;
            height: 18px;
            cursor: pointer;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🎯 ETU003861   ETU003349    ETU003167
        </h1>

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

            <!-- ==================== ASSIGNATIONS EXISTANTES EN BASE ==================== -->
            <% if (resultat.getAssignationsExistantes() != null && !resultat.getAssignationsExistantes().isEmpty()) { %>
                <div class="existing-section">
                    <h2>🔒 Assignations déjà enregistrées en base</h2>

                    <form method="POST" action="/assignations/supprimer-selection" id="formSupprimer">
                        <input type="hidden" name="date" value="<%= dateSimulation %>">
                        <input type="hidden" name="deleteSelections" id="deleteSelectionsInput" value="">

                        <div style="margin: 10px 0; text-align: center;">
                            <button type="submit" class="btn-danger" 
                                    onclick="return preparerSuppression()"
                                    style="background-color: #f44336; color: white; padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; font-size: 14px;">
                                🗑️ Supprimer les sélections
                            </button>
                        </div>

                    <%
                        LieuDAO lieuDAOEx = new LieuDAO();
                        ReservationDAO reservationDAOEx = new ReservationDAO();
                        SimpleDateFormat sdfEx = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                        DecimalFormat dfEx = new DecimalFormat("#.##");
                        
                        // Regrouper par vague
                        Map<java.sql.Timestamp, List<SimulationAssignation>> existantesParVague = new TreeMap<>();
                        for (SimulationAssignation saEx : resultat.getAssignationsExistantes()) {
                            existantesParVague.computeIfAbsent(saEx.getHeureVague(), k -> new ArrayList<>()).add(saEx);
                        }
                        
                        int numVagueEx = 1;
                        SimpleDateFormat sdfHeure = new SimpleDateFormat("HH'h'mm");
                        for (Map.Entry<java.sql.Timestamp, List<SimulationAssignation>> entryEx : existantesParVague.entrySet()) {
                    %>
                        <div class="vague-header" style="background-color: #388e3c;">
                            <span>🔒 VAGUE EXISTANTE #<%= numVagueEx %> - <%= sdfHeure.format(entryEx.getKey()) %></span>
                            <label>
                                <input type="checkbox" class="cb-vague-ex" data-vague-ex="<%= numVagueEx %>" 
                                       onchange="toggleVagueEx(<%= numVagueEx %>)"> Tout sélectionner
                            </label>
                        </div>
                        <table>
                            <thead>
                                <tr>
                                    <th style="width: 40px;">✔</th>
                                    <th>Voiture</th>
                                    <th>Référence</th>
                                    <th>Capacité</th>
                                    <th>Carburant</th>
                                    <th>Réservations (Client → Lieu)</th>
                                    <th>Passagers</th>
                                    <th>Places Restantes</th>
                                    <th>Date/Heure Départ</th>
                                    <th>Date/Heure Arrivée</th>
                                    <th>Itinéraire</th>
                                </tr>
                            </thead>
                            <tbody>
                            <% for (SimulationAssignation saEx : entryEx.getValue()) {
                                Voiture vEx = saEx.getVoiture();
                                int totalPEx = 0;
                                Integer idAssignationEx = null;
                                if (saEx.getReservations() != null && !saEx.getReservations().isEmpty()) {
                                    Assignation aEx = new org.example.DAO.AssignationDAO().findByReservation(saEx.getReservations().get(0).getId());
                                    if (aEx != null) {
                                        idAssignationEx = aEx.getId();
                                    }
                                }
                                for (int iEx = 0; iEx < saEx.getReservations().size(); iEx++) {
                                    Reservation rEx = saEx.getReservations().get(iEx);
                                    totalPEx += rEx.getNbPassager();
                                }
                            %>
                                <tr>
                                    <td>
                                        <input type="checkbox" class="cb-line-ex cb-vague-ex-<%= numVagueEx %>" 
                                               data-delete-selections="<%= idAssignationEx != null ? idAssignationEx : "" %>"
                                               data-vague-ex="<%= numVagueEx %>">
                                    </td>
                                    <td>#<%= vEx.getIdVoiture() %></td>
                                    <td><strong><%= vEx.getRef() %></strong></td>
                                    <td><%= vEx.getCapacite() %> places</td>
                                    <td>
                                        <span class="badge <%= "D".equals(vEx.getCarburant()) ? "badge-diesel" : "badge-essence" %>">
                                            <%= vEx.getCarburantLibelle() %>
                                        </span>
                                    </td>
                                    <td>
                                        <% for (Reservation rEx : saEx.getReservations()) { 
                                            Lieu lieuEx = lieuDAOEx.findById(rEx.getIdLieu());
                                            String lieuNomEx = lieuEx != null ? lieuEx.getLibelle() : "Lieu #" + rEx.getIdLieu();
                                            Reservation rOriginaleEx = reservationDAOEx.findById(rEx.getId());
                                            int totalClientEx = rOriginaleEx != null ? rOriginaleEx.getNbPassager() : rEx.getNbPassager();
                                        %>
                                            #<%= rEx.getId() %>: <%= rEx.getIdClient() %> → <%= lieuNomEx %> (<%= rEx.getNbPassager() %>/<%= totalClientEx %>p)<br>
                                        <% } %>
                                    </td>
                                    <td><strong><%= totalPEx %></strong></td>
                                    <td><%= saEx.getPlacesRestantes() %></td>
                                    <td><%= saEx.getDateHeureDepart() != null ? sdfEx.format(saEx.getDateHeureDepart()) : "-" %></td>
                                    <td><%= saEx.getDateHeureArrivee() != null ? sdfEx.format(saEx.getDateHeureArrivee()) : "-" %></td>
                                    <td>
                                        <div class="itineraire">
                                        <% if (saEx.getItineraire() != null && !saEx.getItineraire().isEmpty()) {
                                            for (EtapeItineraire etapeEx : saEx.getItineraire()) { %>
                                            <div class="etape">
                                                <span class="etape-num"><%= etapeEx.getOrdre() %></span>
                                                <%= etapeEx.getLieuDepart() %> → <%= etapeEx.getLieuArrivee() %>
                                                <span class="etape-detail">(<%= dfEx.format(etapeEx.getDistanceKm()) %> km, <%= dfEx.format(etapeEx.getDureeMinutes()) %> min)</span>
                                            </div>
                                        <% } %>
                                            <div class="total-trajet">
                                                Total: <%= dfEx.format(saEx.getDistanceTotale()) %> km, <%= dfEx.format(saEx.getDureeTotaleMinutes()) %> min
                                            </div>
                                        <% } else { %>
                                            -
                                        <% } %>
                                        </div>
                                    </td>
                                </tr>
                            <% } %>
                            </tbody>
                        </table>
                    <%
                        numVagueEx++;
                        }
                    %>
                    </form>
                </div>
            <% } %>
 
            <% if (resultat.getTotalReservationsAssignees() > 0) { %>

                <!-- Formulaire de confirmation avec checkboxes -->
                <form method="POST" action="/assignations/confirmer" id="formConfirmer">
                    <input type="hidden" name="date" value="<%= dateSimulation %>">
                    <input type="hidden" name="selections" id="selectionsInput" value="">

                    <!-- Bouton de confirmation -->
                    <div style="margin: 20px 0; text-align: center;">
                        <button type="submit" class="btn-success" 
                                onclick="return preparerConfirmation()">
                            ✔️ Confirmer et Enregistrer les sélections
                        </button>
                    </div>

                    <!-- Détails des assignations par vague -->
                    <%
                        Map<java.sql.Timestamp, List<SimulationAssignation>> assignationsParVague = new TreeMap<>();
                        for (SimulationAssignation sa : resultat.getAssignations()) {
                            assignationsParVague.computeIfAbsent(sa.getHeureVague(), k -> new ArrayList<>()).add(sa);
                        }

                        int numeroVague = 1;
                        int indexGlobal = 0;
                        SimpleDateFormat sdfVague = new SimpleDateFormat("HH'h'mm");
                        for (Map.Entry<java.sql.Timestamp, List<SimulationAssignation>> entry : assignationsParVague.entrySet()) {
                            // Récupérer l'intervalle de la vague depuis la première assignation
                            SimulationAssignation premiereSA = entry.getValue().get(0);
                            String intervalleVague = "";
                            if (premiereSA.getDebutVague() != null && premiereSA.getFinFenetreVague() != null) {
                                intervalleVague = " (" + sdfVague.format(premiereSA.getDebutVague()) + " - " + sdfVague.format(premiereSA.getFinFenetreVague()) + ")";
                            }
                    %>
                        <div class="vague-header">
                            <span>🌊 VAGUE #<%= numeroVague %> - Départ: <%= sdfVague.format(entry.getKey()) %><%= intervalleVague %></span>
                            <label>
                                <input type="checkbox" class="cb-vague" data-vague="<%= numeroVague %>" 
                                       onchange="toggleVague(<%= numeroVague %>)" checked> Tout sélectionner
                            </label>
                        </div>

                        <table>
                            <thead>
                                <tr>
                                    <th style="width: 40px;">✔</th>
                                    <th>Voiture</th>
                                    <th>Référence</th>
                                    <th>Capacité</th>
                                    <th>Carburant</th>
                                    <th>Réservations (Client → Lieu)</th>
                                    <th>Passagers</th>
                                    <th>Places Restantes</th>
                                    <th>Date/Heure Départ</th>
                                    <th>Date/Heure Arrivée</th>
                                    <th>Itinéraire</th>
                                </tr>
                            </thead>
                            <tbody>
                                <%
                                    LieuDAO lieuDAO = new LieuDAO();
                                    ReservationDAO reservationDAO = new ReservationDAO();
                                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                                    DecimalFormat df = new DecimalFormat("#.##");
                                    for (SimulationAssignation sa : entry.getValue()) {
                                        Voiture voiture = sa.getVoiture();
                                        int totalPassagers = 0;
                                        for (Reservation r : sa.getReservations()) {
                                            totalPassagers += r.getNbPassager();
                                        }
                                        
                                        // Payload par ligne: idVoiture|departMs|arriveeMs|idRes:ordre;idRes:ordre
                                        long departMs = sa.getDateHeureDepart() != null ? sa.getDateHeureDepart().getTime() : 0L;
                                        long arriveeMs = sa.getDateHeureArrivee() != null ? sa.getDateHeureArrivee().getTime() : 0L;
                                        StringBuilder reservationPayload = new StringBuilder();
                                        for (int i = 0; i < sa.getReservations().size(); i++) {
                                            Reservation r = sa.getReservations().get(i);
                                            if (i > 0) reservationPayload.append(";");
                                            reservationPayload.append(r.getId()).append(":").append(i + 1);
                                        }
                                        String dataSelectionLine = voiture.getIdVoiture() + "|" + departMs + "|" + arriveeMs + "|" + reservationPayload;
                                %>
                                <tr>
                                    <td>
                                        <input type="checkbox" class="cb-line cb-vague-<%= numeroVague %>" 
                                               data-selections="<%= dataSelectionLine %>"
                                               data-vague="<%= numeroVague %>" checked>
                                    </td>
                                    <td>#<%= voiture.getIdVoiture() %></td>
                                    <td><strong><%= voiture.getRef() %></strong></td>
                                    <td><%= voiture.getCapacite() %> places</td>
                                    <td>
                                        <span class="badge <%= "D".equals(voiture.getCarburant()) ? "badge-diesel" : "badge-essence" %>">
                                            <%= voiture.getCarburantLibelle() %>
                                        </span>
                                    </td>
                                    <td>
                                        <% for (Reservation r : sa.getReservations()) { 
                                            Lieu lieu = lieuDAO.findById(r.getIdLieu());
                                            String lieuNom = lieu != null ? lieu.getLibelle() : "Lieu #" + r.getIdLieu();
                                            Reservation rOriginale = reservationDAO.findById(r.getId());
                                            int totalClient = rOriginale != null ? rOriginale.getNbPassager() : r.getNbPassager();
                                        %>
                                            #<%= r.getId() %>: <%= r.getIdClient() %> → <%= lieuNom %> (<%= r.getNbPassager() %>/<%= totalClient %>p)<br>
                                        <% } %>
                                    </td>
                                    <td><strong><%= totalPassagers %></strong></td>
                                    <td><%= sa.getPlacesRestantes() %></td>
                                    <td><%= sa.getDateHeureDepart() != null ? sdf.format(sa.getDateHeureDepart()) : "-" %></td>
                                    <td><%= sa.getDateHeureArrivee() != null ? sdf.format(sa.getDateHeureArrivee()) : "-" %></td>
                                    <td>
                                        <div class="itineraire">
                                        <% if (sa.getItineraire() != null && !sa.getItineraire().isEmpty()) {
                                            for (EtapeItineraire etape : sa.getItineraire()) { %>
                                            <div class="etape">
                                                <span class="etape-num"><%= etape.getOrdre() %></span>
                                                <%= etape.getLieuDepart() %> → <%= etape.getLieuArrivee() %>
                                                <span class="etape-detail">(<%= df.format(etape.getDistanceKm()) %> km, <%= df.format(etape.getDureeMinutes()) %> min)</span>
                                            </div>
                                        <% } %>
                                            <div class="total-trajet">
                                                Total: <%= df.format(sa.getDistanceTotale()) %> km, <%= df.format(sa.getDureeTotaleMinutes()) %> min
                                            </div>
                                        <% } else { %>
                                            -
                                        <% } %>
                                        </div>
                                    </td>
                                </tr>
                                <% 
                                        indexGlobal++;
                                    } 
                                %>
                            </tbody>
                        </table>
                    <%
                            numeroVague++;
                        }
                    %>
                </form>
            <% } %>

            <!-- Réservations non assignées -->
            <% if (!resultat.getReservationsNonAssignees().isEmpty()) { 
                LieuDAO lieuDAO2 = new LieuDAO();
            %>
                <div class="vague-header" style="background-color: #f44336;">
                    <span>❌ RÉSERVATIONS NON ASSIGNÉES</span>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Client</th>
                            <th>Lieu</th>
                            <th>Passagers</th>
                            <th>Date/Heure</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% for (Reservation r : resultat.getReservationsNonAssignees()) { 
                            Lieu lieu2 = lieuDAO2.findById(r.getIdLieu());
                            String lieuNom2 = lieu2 != null ? lieu2.getLibelle() : "Lieu #" + r.getIdLieu();
                        %>
                        <tr>
                            <td>#<%= r.getId() %></td>
                            <td><%= r.getIdClient() %></td>
                            <td><%= lieuNom2 %></td>
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

    <script>
        // ==================== SIMULATION CHECKBOXES ====================
        // Toggle tous les checkboxes d'une vague (simulation)
        function toggleVague(numVague) {
            var cbVague = document.querySelector('.cb-vague[data-vague="' + numVague + '"]');
            var checkboxes = document.querySelectorAll('.cb-vague-' + numVague);
            for (var i = 0; i < checkboxes.length; i++) {
                checkboxes[i].checked = cbVague.checked;
            }
        }

        // Mettre à jour le checkbox de la vague quand on change un checkbox individuel
        document.addEventListener('change', function(e) {
            if (e.target.classList.contains('cb-line')) {
                var numVague = e.target.getAttribute('data-vague');
                var checkboxes = document.querySelectorAll('.cb-vague-' + numVague);
                var allChecked = true;
                for (var i = 0; i < checkboxes.length; i++) {
                    if (!checkboxes[i].checked) {
                        allChecked = false;
                        break;
                    }
                }
                var cbVague = document.querySelector('.cb-vague[data-vague="' + numVague + '"]');
                if (cbVague) cbVague.checked = allChecked;
            }
        });

        // Préparer les données avant soumission (confirmation)
        function preparerConfirmation() {
            var checkboxes = document.querySelectorAll('.cb-line:checked');
            if (checkboxes.length === 0) {
                alert('Veuillez sélectionner au moins une assignation.');
                return false;
            }

            var selections = [];
            for (var i = 0; i < checkboxes.length; i++) {
                var data = checkboxes[i].getAttribute('data-selections');
                if (data) {
                    selections.push(data);
                }
            }

            document.getElementById('selectionsInput').value = selections.join('##');
            
            var nbTotal = 0;
            for (var j = 0; j < selections.length; j++) {
                var parts = selections[j].split('|');
                if (parts.length === 4 && parts[3]) {
                    nbTotal += parts[3].split(';').length;
                }
            }
            
            return confirm('Confirmer l\'enregistrement de ' + nbTotal + ' assignation(s) sélectionnée(s) ?');
        }

        // ==================== EXISTING ASSIGNATIONS CHECKBOXES ====================
        // Toggle tous les checkboxes d'une vague existante
        function toggleVagueEx(numVague) {
            var cbVague = document.querySelector('.cb-vague-ex[data-vague-ex="' + numVague + '"]');
            var checkboxes = document.querySelectorAll('.cb-vague-ex-' + numVague);
            for (var i = 0; i < checkboxes.length; i++) {
                checkboxes[i].checked = cbVague.checked;
            }
        }

        // Mettre à jour le checkbox de la vague existante
        document.addEventListener('change', function(e) {
            if (e.target.classList.contains('cb-line-ex')) {
                var numVague = e.target.getAttribute('data-vague-ex');
                var checkboxes = document.querySelectorAll('.cb-vague-ex-' + numVague);
                var allChecked = true;
                for (var i = 0; i < checkboxes.length; i++) {
                    if (!checkboxes[i].checked) {
                        allChecked = false;
                        break;
                    }
                }
                var cbVague = document.querySelector('.cb-vague-ex[data-vague-ex="' + numVague + '"]');
                if (cbVague) cbVague.checked = allChecked;
            }
        });

        // Préparer les données avant suppression
        function preparerSuppression() {
            var checkboxes = document.querySelectorAll('.cb-line-ex:checked');
            if (checkboxes.length === 0) {
                alert('Veuillez sélectionner au moins une assignation à supprimer.');
                return false;
            }

            var selections = [];
            for (var i = 0; i < checkboxes.length; i++) {
                var data = checkboxes[i].getAttribute('data-delete-selections');
                if (data && data.trim() !== '') {
                    selections.push(data);
                }
            }

            document.getElementById('deleteSelectionsInput').value = selections.join(',');
            
            var nbTotal = 0;
            for (var j = 0; j < selections.length; j++) {
                nbTotal += selections[j].split(',').length;
            }
            
            return confirm('Supprimer ' + nbTotal + ' assignation(s) sélectionnée(s) ?');
        }
    </script>
</body>
</html>
