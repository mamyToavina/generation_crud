/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codegenerator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.Map;

/**
 *
 * @author TOAVINA
 */

public class Vue { 
    public static String generateInput(String tableName, Connection con) throws SQLException, IOException {
        StringBuilder inputHtml = new StringBuilder();

        Map<String, String> fieldsWithLabels = CodeGenerator.getFieldsWithLabels(tableName, con);

        boolean isFirstField = true;

        for (Map.Entry<String, String> entry : fieldsWithLabels.entrySet()) {
            String columnName = entry.getKey();
            String label = entry.getValue();

            if (isFirstField) {
                inputHtml.append("     "+generateInput(columnName,tableName,"hidden",""));
                isFirstField = false;
                continue;
            }

            if (label.isEmpty()) {
                inputHtml.append("     "+generateInput(columnName,tableName,"text",columnName));
            } else {

                CodeGenerator.generateModelRepository(columnName);
                inputHtml.append(generateSelectField(label, label, columnName));
            }
        }

        return inputHtml.toString();
    }

    public static String generateUpdateForm(String tableName, Connection con) throws SQLException, IOException {
        StringBuilder inputHtml = new StringBuilder();

        // Obtenir les champs avec leurs labels
        Map<String, String> fieldsWithLabels = CodeGenerator.getFieldsWithLabels(tableName, con);

        boolean isFirstField = true; // Variable pour vérifier si c'est le premier champ

        for (Map.Entry<String, String> entry : fieldsWithLabels.entrySet()) {
            String columnName = entry.getKey();
            String label = entry.getValue();

            // Si c'est le premier champ, utilisez un input de type "hidden"
            if (isFirstField) {
                inputHtml.append("     "+generateInput(columnName,tableName,"hidden",""));
                isFirstField = false; // Mettez à jour la variable pour indiquer que le premier champ a été ajouté
                continue;
            }

            // Si le label est vide, utiliser un input simple
            if (label.isEmpty()) {
                inputHtml.append("     "+generateInput(columnName,tableName,"text",columnName));
            } else {
                // Sinon, utiliser un select avec les options
                CodeGenerator.generateModelRepository(columnName);
                inputHtml.append(generateSelectFieldUpdate(label, label, columnName, tableName));
            }
        }

        return inputHtml.toString();
    }
 
    public static String generateThead(String tableName, Connection con) throws SQLException {
        StringBuilder thead = new StringBuilder();
        Map<String, String> fieldsWithLabels = CodeGenerator.getFieldsWithLabels(tableName, con);

        for (Map.Entry<String, String> entry : fieldsWithLabels.entrySet()) {
            String columnName = entry.getKey();
            String label = entry.getValue();
            thead.append("   <th>").append(columnName).append("</th>\n        ");
        }
        thead.append("     <th>").append("Action").append("</th>\n");

        return thead.toString();
    }
    
    public static String generateTd(String tableName, Connection con) throws SQLException {
        StringBuilder td = new StringBuilder();
        Map<String, String> fieldsWithLabels = CodeGenerator.getFieldsWithLabels(tableName, con);

        for (Map.Entry<String, String> entry : fieldsWithLabels.entrySet()) {
            String columnName = entry.getKey();
            String label = entry.getValue();

            // Générer la balise <td> correspondant à chaque colonne
            if (label.isEmpty()) {
                // Si le label est vide, utiliser un champ simple
                td.append(String.format("                 <td><%%= %s.get%s() %%></td>\n   ", tableName, capitalizeFirstLetter(columnName)));
            } else {
                // Sinon, utiliser une expression dot pour accéder à la propriété de l'objet associé
                td.append(String.format("           <td><%%= %s.get%s().get%s() %%></td>\n", tableName, capitalizeFirstLetter(columnName), capitalizeFirstLetter(label)));
            }
        }

        return td.toString();
    }
    
    public static String generateInput(String columnName, String tableName, String type,String label) throws IOException {
        // Lecture du fichier select.temp
        BufferedReader reader = new BufferedReader(new FileReader("input.temp"));
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        // Lecture ligne par ligne
        while ((line = reader.readLine()) != null) {
            // Remplacement des placeholders par les valeurs fournies
            line = line.replace("[columnName]", columnName)
                    .replace("[tableName]", tableName)
                    .replace("[label]", label)
                    .replace("[type]", type);

            stringBuilder.append(line).append("\n");
        }
        reader.close();

        return stringBuilder.toString();
    }
    
    public static String generateSelectField(String label, String attribute, String className) throws IOException {
        // Lecture du fichier select.temp
        BufferedReader reader = new BufferedReader(new FileReader("select.temp"));
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        // Lecture ligne par ligne
        while ((line = reader.readLine()) != null) {
            // Remplacement des placeholders par les valeurs fournies
            line = line.replace("[label]", className.toLowerCase())
                    .replace("[className]", capitalizeFirstLetter(className))
                    .replace("[attributes]", attribute)
                    .replace("[classNameLower]", className.toLowerCase())
                    .replace("[methodLabel]", "get"+capitalizeFirstLetter(label)+"()");

            stringBuilder.append(line).append("\n");
        }
        reader.close();

        return stringBuilder.toString();
    }
    
    public static String generateSelectFieldUpdate(String label, String attribute, String className,String tableName) throws IOException {
        // Lecture du fichier select.temp
        BufferedReader reader = new BufferedReader(new FileReader("select_update.temp"));
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        // Lecture ligne par ligne
        while ((line = reader.readLine()) != null) {
            // Remplacement des placeholders par les valeurs fournies
            line = line.replace("[label]", className.toLowerCase())
                    .replace("[className]", capitalizeFirstLetter(className))
                    .replace("[attributes]", attribute)
                    .replace("[classNameLower]", className.toLowerCase())
                    .replace("[tableName]", capitalizeFirstLetter(tableName))
                    .replace("[tableNameLower]", tableName.toLowerCase())
                    .replace("[methodLabel]", "get"+capitalizeFirstLetter(label)+"()");

            stringBuilder.append(line).append("\n");
        }
        reader.close();

        return stringBuilder.toString();
    }
    
    public static String generateImportVue(Map<String, String> metadata, JsonObject types) throws FileNotFoundException {
        StringBuilder importSpace = new StringBuilder();
        JsonParser parser = new JsonParser();
        JsonObject config = (JsonObject) parser.parse(new FileReader("setup.json"));
        JsonObject packageInfo = (JsonObject) config.get("package");
        String modelPackage = (String) packageInfo.get("model").getAsString();

        for (String fieldType : metadata.values()) {
            String field_t = fieldType.substring(0, fieldType.length() - 2);
            if (!types.has(field_t)) {
                importSpace.append("<%@ page import=\"").append(modelPackage+".").append(field_t+".").append(capitalizeFirstLetter(field_t)).append("\"%>\n");
            }
        }
        return importSpace.toString();
    }
    
    public static String capitalizeFirstLetter(String word) {
        return word.substring(0, 1).toUpperCase() + word.substring(1);
    }

}
