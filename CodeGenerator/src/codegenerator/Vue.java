/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codegenerator;

import java.io.BufferedReader;
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

        // Obtenir les champs avec leurs labels
        Map<String, String> fieldsWithLabels = CodeGenerator.getFieldsWithLabels(tableName, con);

        for (Map.Entry<String, String> entry : fieldsWithLabels.entrySet()) {
            String columnName = entry.getKey();
            String label = entry.getValue();

            // Si le label est vide, utiliser un input simple
            if (label.isEmpty()) {
                inputHtml.append(String.format("  %s: <input type=\"text\" name=\"%s\" value=\"${%s.%s}\" /><br/>\n", columnName, columnName, tableName, columnName));
            } else {
                // Sinon, utiliser un select avec les options
                CodeGenerator.generateModelRepository(columnName);
                /*inputHtml.append(String.format("  %s: <select name=\"%s\">\n", label, columnName + ".id"));
                inputHtml.append(String.format("    <c:forEach items=\"${%s}\" var=\"%s\">\n", columnName + "s", columnName));
                inputHtml.append("      <option value=\"${").append(columnName).append(".id}\">${").append(columnName).append(".").append(label).append("}</option>\n");
                inputHtml.append("    </c:forEach>\n");
                inputHtml.append("  </select>\n");*/
                inputHtml.append(generateSelectField(label, label, columnName));
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
    
    public static String generateSelectField(String label, String attribute, String className) throws IOException {
        // Lecture du fichier select.temp
        BufferedReader reader = new BufferedReader(new FileReader("select.temp"));
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        // Lecture ligne par ligne
        while ((line = reader.readLine()) != null) {
            // Remplacement des placeholders par les valeurs fournies
            line = line.replace("[label]", label)
                    .replace("[className]", capitalizeFirstLetter(className))
                    .replace("[attributes]", attribute)
                    .replace("[classNameLower]", className.toLowerCase())
                    .replace("[methodLabel]", "get"+capitalizeFirstLetter(label)+"()");

            stringBuilder.append(line).append("\n");
        }
        reader.close();

        return stringBuilder.toString();
    }
    
    public static String capitalizeFirstLetter(String word) {
        return word.substring(0, 1).toUpperCase() + word.substring(1);
    }

}
