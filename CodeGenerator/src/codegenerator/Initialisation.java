/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codegenerator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author TOAVINA
 */
public class Initialisation {
    public static void generateFileConfig() throws IOException {
        // Ouverture du fichier source
        try (BufferedReader reader = new BufferedReader(new FileReader("template.temp"));
             FileWriter writer = new FileWriter("config.json")) {
            // Lecture du contenu ligne par ligne
            String line;
            while ((line = reader.readLine()) != null) {
                // Écriture de chaque ligne dans le fichier de destination
                writer.write(line);
                writer.write(System.lineSeparator()); // Ajout d'un saut de ligne
            }
        }
    }
    
    public static void openInNotepad(String fileName) throws IOException {
        // Créer le processus pour exécuter Notepad avec le fichier spécifié
        ProcessBuilder builder = new ProcessBuilder("notepad.exe", fileName);
        builder.start();
    }
}
