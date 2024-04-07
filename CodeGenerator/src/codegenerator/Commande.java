/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codegenerator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

/**
 *
 * @author TOAVINA
 */
public class Commande {
    public static void generateOptions(Scanner scanner) throws IOException, SQLException {
        // Lire le fichier setup.json
        JsonParser parser = new JsonParser();
        JsonObject config = (JsonObject) parser.parse(new FileReader("setup.json"));

        // Récupérer les informations de connexion à la base de données
        JsonObject database = (JsonObject) config.get("database");
        String host = (String) database.get("host").getAsString();
        int port = database.get("port").getAsInt();
        String username = (String) database.get("username").getAsString();
        String password = (String) database.get("password").getAsString();
        String db = (String) database.get("database_name").getAsString();
        String databaseName = "postgresql";
        Connection con = DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/" + db, username, password);
       
        // Récupérer les chemins et les packages
        JsonObject paths = (JsonObject) config.get("paths");
        String projectPath = (String) paths.get("project").getAsString();
        String modelPath = (String) paths.get("model").getAsString();
        String controllerPath = (String) paths.get("controller").getAsString();
        String repositoryPath = (String) paths.get("repository").getAsString();
        String viewPath = (String) paths.get("view").getAsString();
        
        JsonObject packageInfo = (JsonObject) config.get("package");
        String modelPackage = (String) packageInfo.get("model").getAsString();
        String controllerPackage = (String) packageInfo.get("controller").getAsString();
        String repositoryPackage = (String) packageInfo.get("repository").getAsString();
        
        System.out.println("Choix disponibles:");
        System.out.println("a - Model");
        System.out.println("b - Vue");
        System.out.println("c - Controller");
        System.out.println("d - Repository");
        System.out.println("e - CRUD");
        System.out.print("Votre choix: ");
        String choix = scanner.next();

        System.out.print("Entrez le nom de la table (séparé par des virgules si plusieurs): ");
        String tables = scanner.next();

        String[] tableNames = tables.split(",");

        for (String tableName : tableNames) {
            switch (choix) {
                case "a":
                    CodeGenerator.generateModel(tableName.trim(), modelPackage, databaseName, modelPath, con);
                    break;
                case "b":
                    CodeGenerator.generateInsertionForm(tableName.trim(), tableName.toLowerCase(), databaseName, viewPath, con);
                    CodeGenerator.generateUpdate(tableName.trim(), databaseName, databaseName, viewPath, con);
                    CodeGenerator.generateList(tableName.trim(), tableName.toLowerCase(), databaseName, viewPath, con);
                    break;
                case "c":
                    CodeGenerator.generateController(tableName.trim(), controllerPackage, databaseName, controllerPath, con);
                    break;
                case "d":
                    CodeGenerator.generateRepository(tableName.trim(), repositoryPackage, databaseName, repositoryPath, con);
                    break;
                case "e":
                    CodeGenerator.generateCrud(tableName.trim());
                    break;
                default:
                    System.out.println("Choix invalide.");
            }
        }
    }
}
