/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package codegenerator;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author TOAVINA
 */

public class CodeGenerator {

   public static Map<String, String> getMetadata(Connection con, String tableName) throws SQLException {
        Map<String, String> metadata = new LinkedHashMap<>();

        // Récupérer les informations sur les colonnes
        DatabaseMetaData dbMetaData = con.getMetaData();
        ResultSet columnsResultSet = dbMetaData.getColumns(null, null, removeWords(tableName), null);

        // Récupérer les informations sur les clés étrangères
        ResultSet foreignKeysResultSet = dbMetaData.getImportedKeys(con.getCatalog(), null, removeWords(tableName));

        // Stocker les noms des tables référencées par chaque colonne étrangère
        Map<String, String> foreignKeyColumns = new HashMap<>();
        while (foreignKeysResultSet.next()) {
            String fkColumnName = foreignKeysResultSet.getString("FKCOLUMN_NAME");
            String pkTableName = foreignKeysResultSet.getString("PKTABLE_NAME");
            foreignKeyColumns.put(fkColumnName, pkTableName);
        }

        while (columnsResultSet.next()) {
            String columnName = columnsResultSet.getString("COLUMN_NAME");
            String columnType = columnsResultSet.getString("TYPE_NAME");

            // Vérifier si la colonne est une clé étrangère
            if (foreignKeyColumns.containsKey(columnName)) {
                // Si c'est une clé étrangère, utiliser le nom de la table référencée comme type
                columnType = foreignKeyColumns.get(columnName);
            }

            int ordinalPosition = columnsResultSet.getInt("ORDINAL_POSITION");
            metadata.put(columnName, columnType + "_" + ordinalPosition);
        }

        metadata = metadata.entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        return metadata;
    }
   
    public static void generateClass(String className, String packageName, String databaseName, String template, String extension, String fileName, String basePath, Connection con) throws IOException, SQLException {
        // Convertir le nom de la classe en majuscule
        String capitalizedClassName = fileName.substring(0, 1).toUpperCase() + fileName.substring(1);
        String code = generateCode(className, packageName, databaseName, template, con);

        // Créer le chemin complet du dossier pour le package
        String packagePath = basePath + File.separator + className.replace(".", File.separator);
        File packageDirectory = new File(packagePath);
        if (!packageDirectory.exists()) {
            packageDirectory.mkdirs();
        }

        // Créer le chemin complet du fichier de classe
        String classFilePath = packagePath + File.separator + capitalizedClassName + extension;
        File classFile = new File(classFilePath);

        // Créer le fichier de classe s'il n'existe pas
        if (!classFile.exists()) {
            classFile.createNewFile();

            // Écrire le contenu initial dans le fichier de classe
            FileWriter writer = new FileWriter(classFile);
            writer.write(code);
            writer.close();

            System.out.println("Class " + capitalizedClassName + " created in package " + packageName);
        } else {
            System.out.println("Class " + capitalizedClassName + " already exists in package " + packageName);
        }
    }

   
   public static String generateCode(String tableName, String packageName, String databaseName,String template, Connection con) throws IOException, SQLException {
        // Lire le contenu du fichier template.temp
        BufferedReader templateReader = new BufferedReader(new FileReader(template));
        StringBuilder templateContent = new StringBuilder();
        String line;
        while ((line = templateReader.readLine()) != null) {
            templateContent.append(line).append("\n");
        }
        templateReader.close();

        // Lire le contenu du fichier database.json
        Gson gson = new Gson();
        JsonArray databaseJsonArray = gson.fromJson(new FileReader("database.json"), JsonArray.class);

        // Trouver les informations sur les types de données de la base de données
        JsonObject databaseInfo = null;
        for (JsonElement element : databaseJsonArray) {
            JsonObject dbObject = element.getAsJsonObject();
            if (dbObject.get("nom").getAsString().equals(databaseName)) {
                databaseInfo = dbObject;
                break;
            }
        }
        if (databaseInfo == null) {
            throw new IllegalArgumentException("Database information not found for database: " + databaseName);
        }

        // Récupérer les types de données de la base de données
        JsonObject types = databaseInfo.getAsJsonObject("types");

        // Récupérer les métadonnées de la table
        Map<String, String> metadata = getMetadata(con, removeWords(tableName).toLowerCase());

        // Construire l'espace d'importation
        String importSpace = generateImportSpace(metadata, types);

        // Construire la déclaration des champs
        String fieldsDeclaration = generateFieldsDeclaration(metadata, types);
        String gettersDeclaration = generateGettersDeclaration(metadata, types);
        String settersDeclaration = generateSettersDeclaration(metadata, types);
        String argsDeclaration = generateArgsConstructor(metadata, types);
        String constructorDeclaration = generateConstructorDeclaration(metadata, types);
        String typeId = getTypeId(removeWords(tableName).toLowerCase(), databaseName, con);
        String passVarDeclaration = passVarDeclaration(metadata, types);
        String otherRepositoryDeclaration = otherRepositoryDeclaration(metadata, types);
        String importRepository = generateImportRepositoryDeclaration(metadata, types);
        String inputDeclaration = Vue.generateInput(removeWords(tableName), con);
        String thDeclaration = Vue.generateThead(tableName, con);
        String tdDeclaration = Vue.generateTd(tableName, con);
        
        JsonParser parser = new JsonParser();
        JsonObject config = (JsonObject) parser.parse(new FileReader("setup.json"));
        JsonObject packageInfo = (JsonObject) config.get("package");
        String modelPackage = (String) packageInfo.get("model").getAsString();
        String controllerPackage = (String) packageInfo.get("controller").getAsString();
        String repositoryPackage = (String) packageInfo.get("repository").getAsString();
        
        
        // Remplacer les balises de remplacement dans le template
        String generatedCode = templateContent.toString()
                .replace("[packageName]", packageName)
                .replace("[importSpace]", importSpace)
                .replace("[className]", removeWords(tableName).substring(0, 1).toUpperCase() + removeWords(tableName).substring(1))
                .replace("[classNameLower]", removeWords(tableName).toLowerCase())
                .replace("[fieldsDeclaration]", fieldsDeclaration)
                .replace("[argsDeclaration]", argsDeclaration)
                .replace("[constructorDeclaration]", constructorDeclaration)
                .replace("[gettersDeclaration]", gettersDeclaration)
                .replace("[typeId]", typeId)
                .replace("[varDeclaration]", gettersDeclaration)
                .replace("[passageVariable]", passVarDeclaration)
                .replace("[otherRepositoryDeclaration]", otherRepositoryDeclaration )
                .replace("[importRepository]", importRepository)
                .replace("[inputDeclaration]", inputDeclaration)
                .replace("[theadDeclaration]", thDeclaration)
                .replace("[tdDeclaration]", tdDeclaration)
                .replace("[packageModel]", modelPackage)
                .replace("[packageRepository]", repositoryPackage)
                .replace("[settersDeclaration]", settersDeclaration);

        // Vous devez remplacer les balises de remplacement restantes par les valeurs appropriées, 
        // telles que les types de champs et les noms de champs, etc.

        return generatedCode;
    }
   
    public static void generateModel(String tableName, String packageName, String databaseName,String basePath, Connection con) throws IOException, SQLException {
        String template = "template.temp";
        String className = tableName.substring(0, 1).toUpperCase() + tableName.substring(1);
        generateClass(tableName, packageName, databaseName, template,".java",tableName,basePath, con);
    }
    
    public static void generateRepository(String tableName, String packageName, String databaseName,String basePath, Connection con) throws IOException, SQLException {
        String template = "repository.temp";
        String className = tableName.substring(0, 1).toUpperCase() + tableName.substring(1)+"Repository";
        generateClass(tableName, packageName, databaseName, template,".java",className,basePath, con);
    }
    
    public static void generateController(String tableName, String packageName, String databaseName,String basePath, Connection con) throws IOException, SQLException {
        String template = "controller.temp";
        String className = tableName.substring(0, 1).toUpperCase() + tableName.substring(1)+"Controller";
        generateClass(tableName, packageName, databaseName, template,".java",className,basePath, con);
    }
    
    public static void generateInsertionForm(String tableName, String packageName, String databaseName,String basePath, Connection con) throws IOException, SQLException {
        String template = "insert.temp";
        String className = tableName.toLowerCase();
        generateClass(tableName, packageName, databaseName, template,".jsp","insert",basePath, con);
    }
    
    public static void generateList(String tableName, String packageName, String databaseName,String basePath, Connection con) throws IOException, SQLException {
        String template = "liste.temp";
        String className = tableName.toLowerCase();
        generateClass(tableName, packageName, databaseName, template,".jsp","liste",basePath, con);
    }
    
    
    public static void generateCrud(String tableName) throws IOException, SQLException {
        // Lire le fichier global_config.json
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

        // Récupérer les chemins et les packages
        JsonObject paths = (JsonObject) config.get("paths");
        String projectPath = (String) paths.get("project").getAsString();
        String modelPath = (String) paths.get("model").getAsString();
        String controllerPath = (String) paths.get("controller").getAsString();
        String repositoryPath = (String) paths.get("repository").getAsString();
        String viewPath = (String) paths.get("view").getAsString();
        System.out.println("ito ary ilay view: "+viewPath);

        JsonObject packageInfo = (JsonObject) config.get("package");
        String modelPackage = (String) packageInfo.get("model").getAsString();
        String controllerPackage = (String) packageInfo.get("controller").getAsString();
        String repositoryPackage = (String) packageInfo.get("repository").getAsString();

        // Créer la connexion à la base de données
        Connection con = DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/" + db, username, password);

        // Générer le modèle
        generateModel(tableName, modelPackage, databaseName, modelPath, con);

        // Générer le repository
        generateRepository(tableName, repositoryPackage, databaseName, repositoryPath, con);

        // Générer le contrôleur
        generateController(tableName, controllerPackage, databaseName, controllerPath, con);

        // Générer le formulaire d'insertion
        generateInsertionForm(tableName, tableName.toLowerCase(), databaseName, viewPath, con);

        // Générer la liste
        generateList(tableName, tableName.toLowerCase(), databaseName, viewPath, con);
    }
    
    
    public static void generateModelRepository(String tableName) throws IOException, SQLException {
        // Lire le fichier global_config.json
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

        // Récupérer les chemins et les packages
        JsonObject paths = (JsonObject) config.get("paths");
        String projectPath = (String) paths.get("project").getAsString();
        String modelPath = (String) paths.get("model").getAsString();
        String controllerPath = (String) paths.get("controller").getAsString();
        String repositoryPath = (String) paths.get("repository").getAsString();
        String viewPath = (String) paths.get("view").getAsString();
        System.out.println("ito ary ilay view: "+viewPath);

        JsonObject packageInfo = (JsonObject) config.get("package");
        String modelPackage = (String) packageInfo.get("model").getAsString();
        String controllerPackage = (String) packageInfo.get("controller").getAsString();
        String repositoryPackage = (String) packageInfo.get("repository").getAsString();

        // Créer la connexion à la base de données
        Connection con = DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/" + db, username, password);

        // Générer le modèle
        generateModel(tableName, modelPackage, databaseName, modelPath, con);

        // Générer le repository
        generateRepository(tableName, repositoryPackage, databaseName, repositoryPath, con);

    }

    public static String generateImportSpace(Map<String, String> metadata, JsonObject types) throws FileNotFoundException {
        StringBuilder importSpace = new StringBuilder();
        JsonParser parser = new JsonParser();
        JsonObject config = (JsonObject) parser.parse(new FileReader("setup.json"));
        JsonObject packageInfo = (JsonObject) config.get("package");
        String modelPackage = (String) packageInfo.get("model").getAsString();
        String controllerPackage = (String) packageInfo.get("controller").getAsString();
        String repositoryPackage = (String) packageInfo.get("repository").getAsString();
        for (String fieldType : metadata.values()) {
            String field_t = fieldType.substring(0, fieldType.length() - 2);
            if (types.has(field_t)) {
                JsonObject typeInfo = types.getAsJsonObject(field_t);
                String importStatement = typeInfo.get("import").getAsString();
                if (!importStatement.isEmpty()) {
                    importSpace.append("import ").append(importStatement).append(";\n");
                }
            }
            
            else{
                importSpace.append("import ").append(modelPackage+".").append(field_t+".").append(Vue.capitalizeFirstLetter(field_t)).append(";\n");
                importSpace.append("import jakarta.persistence.JoinColumn").append(";\n");
                importSpace.append("import jakarta.persistence.ManyToOne").append(";\n");
            }
        }
        return importSpace.toString();
    }

    public static String generateFieldsDeclaration(Map<String, String> metadata, JsonObject types) {
        StringBuilder fieldsDeclaration = new StringBuilder();

        // Tri des métadonnées en fonction de la position dans la base de données
        Map<String, String> sortedMetadata = new LinkedHashMap<>(metadata);

        List<Map.Entry<String, String>> entryList = new ArrayList<>(sortedMetadata.entrySet());
        entryList.sort(Comparator.comparingInt(entry -> Integer.parseInt(entry.getValue().substring(entry.getValue().lastIndexOf("_") + 1))));

        for (Map.Entry<String, String> entry : entryList) {
            String fieldName = entry.getKey();
            String fieldType = entry.getValue().substring(0, entry.getValue().lastIndexOf("_"));

            if (types.has(fieldType)) {
                String fieldTypeName = types.getAsJsonObject(fieldType).get("type").getAsString();
                fieldsDeclaration.append("private ").append(fieldTypeName).append(" ").append(fieldName).append(";\n").append("    ");
            } else {
                fieldsDeclaration.append("@ManyToOne").append("\n    ");
                fieldsDeclaration.append("@JoinColumn(name = \""+fieldName+"\")").append("\n    ");
                fieldsDeclaration.append("private ").append(Vue.capitalizeFirstLetter(fieldType)).append(" ").append(fieldName).append(";\n");
            }
        }
        return fieldsDeclaration.toString();
    }
    
    public static String generateArgsConstructor(Map<String, String> metadata, JsonObject types){
        StringBuilder argsDeclaration = new StringBuilder();
        List<Map.Entry<String, String>> entryList = new ArrayList<>(metadata.entrySet());
        entryList.sort(Comparator.comparingInt(entry -> Integer.parseInt(entry.getValue().substring(entry.getValue().lastIndexOf("_") + 1))));
        // Ajouter les paramètres du constructeur
        for (Map.Entry<String, String> entry : entryList) {
            String fieldName = entry.getKey();
            String fieldType = entry.getValue().substring(0, entry.getValue().lastIndexOf("_"));

            if (types.has(fieldType)) {
                String fieldTypeName = types.getAsJsonObject(fieldType).get("type").getAsString();
                argsDeclaration.append(fieldTypeName).append(" ").append(fieldName).append(", ");
            } else {
                argsDeclaration.append(Vue.capitalizeFirstLetter(fieldType)).append(" ").append(fieldName).append(", ");
            }
        }

        // Supprimer la virgule et l'espace supplémentaire à la fin
        if (argsDeclaration.length() > 2) {
            argsDeclaration.setLength(argsDeclaration.length() - 2);
        }
        
        return argsDeclaration.toString();
    }
    
    public static String generateConstructorDeclaration(Map<String, String> metadata, JsonObject types) {
        StringBuilder constructorDeclaration = new StringBuilder();
        
        List<Map.Entry<String, String>> entryList = new ArrayList<>(metadata.entrySet());
        entryList.sort(Comparator.comparingInt(entry -> Integer.parseInt(entry.getValue().substring(entry.getValue().lastIndexOf("_") + 1))));

        // Ajouter les initialisations des champs dans le constructeur
        for (Map.Entry<String, String> entry : entryList) {
            String fieldName = entry.getKey();
            constructorDeclaration.append("this.").append(fieldName).append(" = ").append(fieldName).append(";\n       ");
        }

        return constructorDeclaration.toString();
    }

    
    public static String generateGettersDeclaration(Map<String, String> metadata, JsonObject types) {
        StringBuilder gettersDeclaration = new StringBuilder();
        List<Map.Entry<String, String>> entryList = new ArrayList<>(metadata.entrySet());
        entryList.sort(Comparator.comparingInt(entry -> Integer.parseInt(entry.getValue().substring(entry.getValue().lastIndexOf("_") + 1))));

        for (Map.Entry<String, String> entry : entryList) {
            String fieldName = entry.getKey();
            String fieldType = entry.getValue().substring(0, entry.getValue().lastIndexOf("_"));

            if (types.has(fieldType)) {
                String fieldTypeName = types.getAsJsonObject(fieldType).get("type").getAsString();
                String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                gettersDeclaration.append("public ").append(fieldTypeName).append(" ").append(getterName).append("() {\n")
                                 .append("       return ").append(fieldName).append(";\n")
                                 .append("    }\n\n").append("    ");
            } else {
                String getterName = "get" + fieldType.substring(0, 1).toUpperCase() + fieldType.substring(1);
                gettersDeclaration.append("public ").append(Vue.capitalizeFirstLetter(fieldType)).append(" ").append(getterName).append("() {\n")
                                 .append("       return ").append(fieldName).append(";\n")
                                 .append("    }\n\n");
            }
        }
        return gettersDeclaration.toString();
    }

    public static String generateSettersDeclaration(Map<String, String> metadata, JsonObject types) {
        StringBuilder settersDeclaration = new StringBuilder();
        List<Map.Entry<String, String>> entryList = new ArrayList<>(metadata.entrySet());
        entryList.sort(Comparator.comparingInt(entry -> Integer.parseInt(entry.getValue().substring(entry.getValue().lastIndexOf("_") + 1))));

        for (Map.Entry<String, String> entry : entryList) {
            String fieldName = entry.getKey();
            String fieldType = entry.getValue().substring(0, entry.getValue().lastIndexOf("_"));

            if (types.has(fieldType)) {
                String fieldTypeName = types.getAsJsonObject(fieldType).get("type").getAsString();
                String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                settersDeclaration.append("public void ").append(setterName).append("(").append(fieldTypeName).append(" ").append(fieldName).append(") {\n")
                                 .append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n")
                                 .append("    }\n\n").append("    ");
            } else {
                String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                settersDeclaration.append("public void ").append(setterName).append("(").append(Vue.capitalizeFirstLetter(fieldType)).append(" ").append(fieldName).append(") {\n")
                                 .append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n")
                                 .append("    }\n\n").append("    ");
            }
        }
        return settersDeclaration.toString();
    }
    
    public static String passVarDeclaration(Map<String, String> metadata, JsonObject types) {
        StringBuilder varDeclaration = new StringBuilder();
        List<Map.Entry<String, String>> entryList = new ArrayList<>(metadata.entrySet());
        entryList.sort(Comparator.comparingInt(entry -> Integer.parseInt(entry.getValue().substring(entry.getValue().lastIndexOf("_") + 1))));

        for (Map.Entry<String, String> entry : entryList) {
            String fieldName = entry.getKey();
            String fieldType = entry.getValue().substring(0, entry.getValue().lastIndexOf("_"));

            if (types.has(fieldType)) {
                String fieldTypeName = types.getAsJsonObject(fieldType).get("type").getAsString();
                varDeclaration.append("");
            } else {
                varDeclaration.append("    model.addAttribute(\"").append(fieldType.toLowerCase()+"s").append("\", ").append(fieldName+"Repository.findAll()").append(");\n");
            }
        }
        return varDeclaration.toString();
    }
    
    
    public static String otherRepositoryDeclaration(Map<String, String> metadata, JsonObject types) {
        StringBuilder varDeclaration = new StringBuilder();
        List<Map.Entry<String, String>> entryList = new ArrayList<>(metadata.entrySet());
        entryList.sort(Comparator.comparingInt(entry -> Integer.parseInt(entry.getValue().substring(entry.getValue().lastIndexOf("_") + 1))));

        for (Map.Entry<String, String> entry : entryList) {
            String fieldName = entry.getKey();
            String fieldType = entry.getValue().substring(0, entry.getValue().lastIndexOf("_"));

            if (types.has(fieldType)) {
                String fieldTypeName = types.getAsJsonObject(fieldType).get("type").getAsString();
                varDeclaration.append("");
            } else {
                varDeclaration.append("@Autowired").append("\n").append("    private "+fieldType.substring(0, 1).toUpperCase() + fieldType.substring(1)+"Repository"+" "+fieldName+"Repository").append(";\n");
            }
        }
        return varDeclaration.toString();
    }
    
    public static String generateImportRepositoryDeclaration(Map<String, String> metadata, JsonObject types) throws FileNotFoundException {
        JsonParser parser = new JsonParser();
        JsonObject config = (JsonObject) parser.parse(new FileReader("setup.json"));
        JsonObject packageInfo = (JsonObject) config.get("package");
        String modelPackage = (String) packageInfo.get("model").getAsString();
        String controllerPackage = (String) packageInfo.get("controller").getAsString();
        String repositoryPackage = (String) packageInfo.get("repository").getAsString();
        StringBuilder varDeclaration = new StringBuilder();
        List<Map.Entry<String, String>> entryList = new ArrayList<>(metadata.entrySet());
        entryList.sort(Comparator.comparingInt(entry -> Integer.parseInt(entry.getValue().substring(entry.getValue().lastIndexOf("_") + 1))));

        for (Map.Entry<String, String> entry : entryList) {
            String fieldName = entry.getKey();
            String fieldType = entry.getValue().substring(0, entry.getValue().lastIndexOf("_"));

            if (types.has(fieldType)) {
                String fieldTypeName = types.getAsJsonObject(fieldType).get("type").getAsString();
                varDeclaration.append("");
            } else {
                varDeclaration.append("import "+repositoryPackage+"."+fieldType.substring(0, 1).toUpperCase() + fieldType.substring(1)+"Repository").append(";\n");
            }
        }
        return varDeclaration.toString();
    }


    public static String removeWords(String input) {
        if (input.endsWith("Repository")) {
            return input.substring(0, input.length() - "Repository".length());
        } else if (input.endsWith("Controller")) {
            return input.substring(0, input.length() - "Controller".length());
        }
        return input;
    }
    
    /*public static HashMap<String, String> getFK(String tableName, Connection con) throws SQLException {
        HashMap<String, String> foreignKeys = new HashMap<>();
        DatabaseMetaData metaData = con.getMetaData();
        ResultSet rs = metaData.getImportedKeys(con.getCatalog(), null, tableName);

        while (rs.next()) {
            String fkColumnName = rs.getString("FKCOLUMN_NAME");
            String pkTableName = rs.getString("PKTABLE_NAME");
            String pkColumnName = rs.getString("PKCOLUMN_NAME");

            String label = getSecondColumnLabel(pkTableName, con);
            foreignKeys.put(pkTableName, label);
        }

        return foreignKeys;
    }

    private static String getSecondColumnLabel(String tableName, Connection con) throws SQLException {
        String label = "";
        DatabaseMetaData metaData = con.getMetaData();
        ResultSet rs = metaData.getColumns(con.getCatalog(), null, removeWords(tableName), null);

        // Skip the first column (typically the primary key)
        if (rs.next()) {
            rs.next();
            label = rs.getString("COLUMN_NAME");
        }

        return label;
    }*/
    
    
    public static Map<String, String> getFieldsWithLabels(String tableName, Connection con) throws SQLException {
        Map<String, String> fieldsWithLabels = new LinkedHashMap<>();
        DatabaseMetaData metaData = con.getMetaData();
        ResultSet rs = metaData.getColumns(con.getCatalog(), null, tableName, null);

        int columnPosition = 1;
        while (rs.next()) {
            String columnName = rs.getString("COLUMN_NAME");
            String columnLabel = "";

            // Obtenez le label de la colonne si elle est une clé étrangère
            String label = getSecondColumnLabel(columnName, tableName, con);
            if (!label.isEmpty()) {
                columnLabel = getSecondColumnLabel(label, con);
                // Utilisez le nom de la table liée comme clé
                columnName = label;
            }

            fieldsWithLabels.put(columnName, columnLabel);
            columnPosition++;
        }

        return fieldsWithLabels;
    }

    private static String getSecondColumnLabel(String columnName, String tableName, Connection con) throws SQLException {
        String label = "";
        DatabaseMetaData metaData = con.getMetaData();
        ResultSet rs = metaData.getImportedKeys(con.getCatalog(), null, tableName);

        while (rs.next()) {
            String fkColumnName = rs.getString("FKCOLUMN_NAME");
            if (columnName.equals(fkColumnName)) {
                String pkTableName = rs.getString("PKTABLE_NAME");
                label = pkTableName;
                break; // Une fois trouvé, on peut arrêter de parcourir les clés étrangères
            }
        }

        return label;
    }
    
    private static String getSecondColumnLabel(String tableName, Connection con) throws SQLException {
        String label = "";
        DatabaseMetaData metaData = con.getMetaData();
        ResultSet rs = metaData.getColumns(con.getCatalog(), null, removeWords(tableName), null);

        // Skip the first column (typically the primary key)
        if (rs.next()) {
            rs.next();
            label = rs.getString("COLUMN_NAME");
        }

        return label;
    }

    
    public static String getTypeId(String tableName, String databaseName, Connection con) throws SQLException {
        String firstColumnTypeName = null;
        DatabaseMetaData metaData = con.getMetaData();
        ResultSet rs = metaData.getColumns(con.getCatalog(), null, removeWords(tableName), null);

        if (rs.next()) {
            String firstColumnName = rs.getString("COLUMN_NAME");
            String firstColumnType = rs.getString("TYPE_NAME");
            JsonObject databaseJson = getDatabaseJson(databaseName);
            
            if (databaseJson != null && databaseJson.getAsJsonObject("types").has(firstColumnType)) {
                firstColumnTypeName = databaseJson.getAsJsonObject("types").getAsJsonObject(firstColumnType).get("type").getAsString();
            }
        }

        return firstColumnTypeName;
    }

    private static JsonObject getDatabaseJson(String databaseName) {
        JsonObject databaseJson = null;
        try {
            Gson gson = new Gson();
            JsonArray databaseJsonArray = gson.fromJson(new FileReader("database.json"), JsonArray.class);
            for (JsonElement element : databaseJsonArray) {
                JsonObject dbObject = element.getAsJsonObject();
                if (dbObject.get("nom").getAsString().equals(databaseName)) {
                    databaseJson = dbObject;
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return databaseJson;
    }


    public static void main(String[] args) throws SQLException, IOException {
        try {
            Connection con = DriverManager.getConnection("jdbc:postgresql://localhost:5000/postgres", "postgres", "toavina");
            //generateInsertionForm("article", "article","postgresql","D:/S6/code/spring mvc/test_projet/src/main/webapp/views", con);
            generateCrud("poste");
            //generateList("service", "ecole","postgresql","D:/S6/code/spring mvc/test_projet/src/main/webapp/views", con);
           
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
}
