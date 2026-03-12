package org.example.Util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DatabaseConnection {

    private static String url;
    private static String username;
    private static String password;

    static {
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("✅ Driver PostgreSQL chargé");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Driver PostgreSQL non trouvé");
            throw new RuntimeException("Driver PostgreSQL requis", e);
        }

        // Charger la configuration depuis beans.xml
        try {
            InputStream is = DatabaseConnection.class.getClassLoader().getResourceAsStream("beans.xml");
            if (is == null) {
                throw new RuntimeException("Fichier beans.xml non trouvé dans le classpath");
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();

            NodeList beanList = doc.getElementsByTagName("bean");
            for (int i = 0; i < beanList.getLength(); i++) {
                Element bean = (Element) beanList.item(i);
                if ("dataSource".equals(bean.getAttribute("id"))) {
                    NodeList properties = bean.getElementsByTagName("property");
                    for (int j = 0; j < properties.getLength(); j++) {
                        Element prop = (Element) properties.item(j);
                        String name = prop.getAttribute("name");
                        String value = prop.getAttribute("value");
                        switch (name) {
                            case "url": url = value; break;
                            case "username": username = value; break;
                            case "password": DatabaseConnection.password = value; break;
                        }
                    }
                    break;
                }
            }

            if (url == null || username == null || password == null) {
                throw new RuntimeException("Configuration dataSource incomplète dans beans.xml");
            }

            System.out.println("✅ Configuration base de données chargée depuis beans.xml");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de beans.xml: " + e.getMessage());
            throw new RuntimeException("Impossible de charger la configuration depuis beans.xml", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        System.out.println("==========================================");
        System.out.println(" TENTATIVE DE CONNEXION À LA BASE:");
        System.out.println("URL: " + url);
        System.out.println("User: " + username);
        System.out.println("==========================================");

        try {
            Connection conn = DriverManager.getConnection(url, username, password);
            System.out.println("✅ CONNEXION RÉUSSIE!");
            return conn;
        } catch (SQLException e) {
            System.err.println("❌ ÉCHEC CONNEXION: " + e.getMessage());
            System.err.println("Code d'erreur: " + e.getErrorCode());
            System.err.println("État SQL: " + e.getSQLState());
            throw e;
        }
    }

}