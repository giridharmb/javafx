package org.example.gbhujanfx1;

import java.io.*;
import java.util.Properties;

import java.sql.*;
import java.util.*;
import java.util.logging.*;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
[DB1]
tables=t_random_v1,t_random_v2
show=true

[DB2]
tables=t_random_v4
show=true
*/

public class Application extends javafx.application.Application {
    private TextField searchField;
    private Pagination pagination;
    private final int ROWS_PER_PAGE = 40;


    private static final String ALLOWED_CHARACTERS_REGEX = "^[a-zA-Z0-9\\-_.,=@#$&:;/()]+$";

    private TabPane tabPane;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2); // Thread pool for async tasks


    private void setupTabs() {
        tabPane = new TabPane(); // Initialize the TabPane

        // Main database tabs
//        Tab db1Tab = createDatabaseTab("DB1", System.getProperty("user.home") + "/config/db1.conf", "t_random_v1", "t_random_v2");
//        Tab db2Tab = createDatabaseTab("DB2", System.getProperty("user.home") + "/config/db2.conf", "t_random_v3", "t_random_v4");


        Tab db1Tab = createDatabaseTab("DB1", new String[]{"t_random_v1", "t_random_v2"});
        Tab db2Tab = createDatabaseTab("DB2", new String[]{"t_random_v3", "t_random_v4"});

        tabPane.getTabs().addAll(db1Tab, db2Tab);
    }

    private Tab createTableTab(String tableName) {
        Tab tableTab = new Tab(tableName);

        VBox container = new VBox(10);
        container.setPadding(new Insets(10));
        container.setSpacing(5);

        // Search box
        TextField searchField = new TextField();
        searchField.setPromptText("Search...");
        searchField.setAlignment(Pos.CENTER);

        Button searchButton = new Button("Search");

        // Exact Search Checkbox
        CheckBox exactSearchCheckBox = new CheckBox("Exact Search");

        // TableView for dynamic data
        TableView<Map<String, Object>> tableView = new TableView<>();
        setupDynamicTableColumns(tableView, tableName);

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // ScrollPane for TableView
        ScrollPane scrollPane = new ScrollPane(tableView);
        scrollPane.setFitToWidth(true);


        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        // Pagination
        Pagination pagination = new Pagination();
        pagination.setMaxPageIndicatorCount(5);

        // Search and Pagination actions
        searchButton.setOnAction(e -> searchAndLoadData(searchField, tableView, pagination, tableName, exactSearchCheckBox.isSelected()));
        searchField.setOnAction(e -> searchAndLoadData(searchField, tableView, pagination, tableName, exactSearchCheckBox.isSelected()));

        pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            loadTableData(tableView, tableName, searchField.getText(), pagination, (Integer) newIndex, exactSearchCheckBox.isSelected());
        });

        // Layout
        HBox searchBox = new HBox(10, searchField, searchButton, exactSearchCheckBox);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchBox.setAlignment(Pos.CENTER);

        container.getChildren().addAll(searchBox, scrollPane, pagination);
        VBox.setVgrow(container, Priority.ALWAYS);
        tableTab.setContent(container);

        // Initial data load
        loadTableData(tableView, tableName, "", pagination, 0, false);

        return tableTab;
    }

    private void setupDynamicTableColumns(TableView<Map<String, Object>> tableView, String tableName) {
        tableView.getColumns().clear();
        List<String> columnNames = fetchTableColumns(tableName);

        System.out.println("Setting up table columns for: " + tableName);
        for (String columnName : columnNames) {
            System.out.println("Adding column: " + columnName);
            TableColumn<Map<String, Object>, String> column = new TableColumn<>(columnName);
            column.setCellValueFactory(cellData -> {
                Map<String, Object> row = cellData.getValue();
                Object value = row.get(columnName);
                return new SimpleStringProperty(value != null ? value.toString() : "");
            });
            column.setMinWidth(150);
            tableView.getColumns().add(column);
        }
        System.out.println("Columns set up complete for: " + tableName);
    }

    private List<String> fetchTableColumns(String tableName) {
        List<String> columns = new ArrayList<>();
        String query = "SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ?";

        try (PreparedStatement stmt = DbConnection.getConnection().prepareStatement(query)) {
            stmt.setString(1, tableName.toLowerCase());
            System.out.println("Executing query: " + stmt);

            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            System.out.println("Query returned " + metaData.getColumnCount() + " columns.");

            while (rs.next()) {
                String columnName = rs.getString("column_name");
                System.out.println("Found column: " + columnName);
                columns.add(columnName);
            }

            if (columns.isEmpty()) {
                System.out.println("No columns found for table: " + tableName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return columns;
    }


    private void searchAndLoadData(TextField searchField, TableView<Map<String, Object>> tableView, Pagination pagination, String tableName, boolean exactSearch) {
        loadTableData(tableView, tableName, searchField.getText(), pagination, 0, exactSearch);
    }

    private List<Map<String, Object>> fetchTableData(String tableName) {
        List<Map<String, Object>> data = new ArrayList<>();
        String query = "SELECT * FROM public." + tableName;

        try (PreparedStatement stmt = DbConnection.getConnection().prepareStatement(query)) {
            System.out.println("Executing data query: " + query);
            ResultSet rs = stmt.executeQuery();

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            System.out.println("Data query returned " + columnCount + " columns.");

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    System.out.println("Row [" + columnName + "]: " + value);
                    row.put(columnName, value);
                }
                data.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return data;
    }

    private void loadTableData(TableView<Map<String, Object>> tableView, String tableName, String searchTerm, Pagination pagination, int pageIndex, boolean exactSearch) {
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("Loading data for table: " + tableName);
                String sqlCondition = DbConnection.buildSqlCondition(tableName, searchTerm, exactSearch);

                // Count query
                String countQuery = "SELECT COUNT(*) FROM public." + tableName + " WHERE " + sqlCondition;
                long totalRecords = DbConnection.executeCountSync(countQuery, tableName, searchTerm, exactSearch);

                Platform.runLater(() -> {
                    pagination.setPageCount((int) Math.ceil(totalRecords / (double) ROWS_PER_PAGE));
                    pagination.setDisable(totalRecords == 0);
                });

                if (totalRecords == 0) {
                    System.out.println("No records found for table: " + tableName);
                    Platform.runLater(tableView.getItems()::clear);
                    return;
                }

                // Fetch data query
                String dataQuery = "SELECT * FROM public." + tableName + " WHERE " + sqlCondition + " LIMIT " + ROWS_PER_PAGE + " OFFSET " + (pageIndex * ROWS_PER_PAGE);
                System.out.println("Executing data query: " + dataQuery);
                List<Map<String, Object>> data = DbConnection.executeQuerySync(dataQuery, tableName, searchTerm, ROWS_PER_PAGE, pageIndex * ROWS_PER_PAGE, exactSearch);

                Platform.runLater(() -> {
                    System.out.println("Data fetched: " + data.size() + " rows.");
                    tableView.getItems().clear();
                    tableView.getItems().addAll(data);
                });
            } catch (Exception e) {
                System.err.println("Error while loading data: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Error", "Failed to load data for table: " + tableName + ". " + e.getMessage()));
            }
        }, executorService);
    }

    private String buildSortQuery(TableView<Map<String, Object>> tableView) {
        ObservableList<TableColumn<Map<String, Object>, ?>> sortOrder = tableView.getSortOrder();

        if (sortOrder.isEmpty()) {
            return ""; // No sorting applied
        }

        StringBuilder orderByClause = new StringBuilder("ORDER BY ");
        for (TableColumn<Map<String, Object>, ?> column : sortOrder) {
            String columnName = getColumnName(column);
            boolean ascending = column.getSortType() == TableColumn.SortType.ASCENDING;

            if (columnName != null) {
                orderByClause.append(columnName)
                        .append(ascending ? " ASC" : " DESC")
                        .append(", ");
            }
        }

        // Remove the trailing comma and space
        if (orderByClause.length() > 9) {
            orderByClause.setLength(orderByClause.length() - 2);
        }

        return orderByClause.toString();
    }

    private String getColumnName(TableColumn<Map<String, Object>, ?> column) {
        if (column.getText().equals("Random Number")) {
            return "random_num";
        } else if (column.getText().equals("Random Float")) {
            return "random_float";
        } else if (column.getText().equals("MD5")) {
            return "md5";
        } else if (column.getText().equals("MD5_1")) {
            return "md5_1";
        } else if (column.getText().equals("MD5_2")) {
            return "md5_2";
        }
        return null; // Unknown column
    }

    private <T> void addCopyToClipboardFeature(TableColumn<RandomData, T> column) {
        column.setCellFactory(tc -> {
            TableCell<RandomData, T> cell = new TableCell<>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.toString());
                }
            };

            cell.setOnMouseClicked(event -> {
                if (!cell.isEmpty()) {
                    String text = cell.getText();
                    copyToClipboard(text);
                    showAlert("Copied to Clipboard", "Copied: " + text);
                }
            });

            return cell;
        });
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    private void setupTableColumns(TableView<RandomData> tableView, String tableName) {
        // Create "Random Number" column
        TableColumn<RandomData, Integer> numCol = new TableColumn<>("Random Number");
        numCol.setCellValueFactory(cellData -> cellData.getValue().randomNumProperty().asObject());
        numCol.setMinWidth(150); // Prevent column from collapsing
        addCopyToClipboardFeature(numCol);

        // Create "Random Float" column
        TableColumn<RandomData, Double> floatCol = new TableColumn<>("Random Float");
        floatCol.setCellValueFactory(cellData -> cellData.getValue().randomFloatProperty().asObject());
        floatCol.setMinWidth(150);
        addCopyToClipboardFeature(floatCol);

        // Add conditional columns based on the table name
        if (DbConnection.verifyRequiredColumns(tableName)) {
            if (tableName.startsWith("t_random_v1") || tableName.startsWith("t_random_v2")) {
                TableColumn<RandomData, String> md5Col = new TableColumn<>("MD5");
                md5Col.setCellValueFactory(cellData -> cellData.getValue().md5Property());
                md5Col.setMinWidth(400);
                addCopyToClipboardFeature(md5Col);
                tableView.getColumns().add(md5Col);
            } else if (tableName.startsWith("t_random_v3") || tableName.startsWith("t_random_v4")) {
                TableColumn<RandomData, String> md5_1Col = new TableColumn<>("MD5_1");
                md5_1Col.setCellValueFactory(cellData -> cellData.getValue().md5_1Property());
                md5_1Col.setMinWidth(400);
                addCopyToClipboardFeature(md5_1Col);

                TableColumn<RandomData, String> md5_2Col = new TableColumn<>("MD5_2");
                md5_2Col.setCellValueFactory(cellData -> cellData.getValue().md5_2Property());
                md5_2Col.setMinWidth(400);
                addCopyToClipboardFeature(md5_2Col);

                tableView.getColumns().addAll(md5_1Col, md5_2Col);
            }
        }

        // Add default columns to the table
        tableView.getColumns().addAll(numCol, floatCol);
    }


    private <T> TableColumn<RandomData, T> createColumn(String title, String propertyName, double widthFraction) {
        TableColumn<RandomData, T> column = new TableColumn<>(title);
        column.setCellValueFactory(cellData -> {
            try {
                // Dynamically bind the property using reflection
                return (ObservableValue<T>) cellData.getValue().getClass()
                        .getMethod(propertyName)
                        .invoke(cellData.getValue());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
        centerColumn(column); // Center-align column content
        addCopyToClipboardFeature(column); // Add copy-to-clipboard functionality

        // Adjust column width dynamically based on the table's width
        column.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double tableWidth = column.getTableView().getWidth();
            column.setPrefWidth(tableWidth * widthFraction);
        });

        return column;
    }


    @Override
    public void start(Stage stage) {
        try {
            // Set application icon
            stage.getIcons().add(new Image("appicon.png"));

            // Create the root layout
            VBox root = new VBox(10);
            root.setPadding(new Insets(10));
            VBox.setVgrow(root, Priority.ALWAYS); // Allow the root layout to grow

            // Initialize and set up database tabs from the configuration
            setupTabsFromConfig(); // Dynamically initializes tabPane based on the config file
            if (tabPane == null || tabPane.getTabs().isEmpty()) {
                showAlert("Error", "No databases or tables available. Please check the configuration.");
                Platform.exit();
                return;
            }

            VBox.setVgrow(tabPane, Priority.ALWAYS); // Ensure tabPane grows to fill space
            root.getChildren().add(tabPane); // Add tabPane to the root layout

            // Create the scene and load the stylesheet
            Scene scene = new Scene(root, 1500, 900);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

            // Configure the stage
            stage.setTitle("Advanced Search Viewer");
            stage.setResizable(true);
            stage.setScene(scene);

            stage.show();
        } catch (Exception e) {
            // Handle unexpected exceptions
            e.printStackTrace();
            showAlert("Unexpected Error", "An unexpected error occurred: " + e.getMessage());
            Platform.exit();
        }
    }


    private void setupTabsFromConfig() {
        tabPane = new TabPane(); // Initialize TabPane

        // Read database configurations
        Properties config = loadDatabaseConfig();

        String mainDatabases = config.getProperty("main.databases", "");
        if (mainDatabases.isEmpty()) {
            showAlert("Error", "No databases specified in the '[main]' section of the configuration file.");
            return;
        }

        Set<String> allowedDatabases = new HashSet<>(Arrays.asList(mainDatabases.split(",")));

        for (String key : config.stringPropertyNames()) {
            if (key.endsWith(".show") && Boolean.parseBoolean(config.getProperty(key))) {
                String dbName = key.substring(0, key.indexOf("."));
                if (!allowedDatabases.contains(dbName)) {
                    continue; // Skip databases not in the main section
                }

                String tables = config.getProperty(dbName + ".tables", "");
                String[] tableList = tables.split(",");

                Tab databaseTab = createDatabaseTab(dbName, tableList);
                tabPane.getTabs().add(databaseTab);
            }
        }

        if (tabPane.getTabs().isEmpty()) {
            showAlert("Error", "No valid databases found based on the '[main]' section of the configuration file.");
        }
    }


    private Properties loadDatabaseConfig() {
        Properties config = new Properties();
        String configPath = System.getProperty("user.home") + "/config/databases.conf";

        File configFile = new File(configPath);
        if (!configFile.exists()) {
            showAlert("Configuration Missing", "The configuration file '" + configPath + "' was not found. Please create the configuration file.");
            return config; // Return an empty Properties object
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String currentSection = null;

            // Temporary map to store parsed data
            Map<String, Properties> sections = new LinkedHashMap<>();

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                    continue;
                }

                // Identify section headers
                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1);
                    sections.put(currentSection, new Properties());
                } else if (currentSection != null) {
                    // Parse key-value pairs
                    int equalsIndex = line.indexOf('=');
                    if (equalsIndex != -1) {
                        String key = line.substring(0, equalsIndex).trim();
                        String value = line.substring(equalsIndex + 1).trim();
                        sections.get(currentSection).setProperty(key, value);
                    }
                }
            }

            // Combine all sections into a single flat Properties object
            for (Map.Entry<String, Properties> entry : sections.entrySet()) {
                String prefix = entry.getKey() + ".";
                for (String key : entry.getValue().stringPropertyNames()) {
                    config.setProperty(prefix + key, entry.getValue().getProperty(key));
                }
            }

        } catch (IOException e) {
            showAlert("Error", "Failed to load database configuration: " + e.getMessage());
        }

        return config;
    }

    private String getConfigValue(String[] configArray, String key, String defaultValue) {
        for (String pair : configArray) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].trim().equals(key)) {
                return keyValue[1].trim();
            }
        }
        return defaultValue;
    }

    private Tab createDatabaseTab(String dbName, String[] tables) {
        Tab databaseTab = new Tab(dbName);

        String dbConfigPath = System.getProperty("user.home") + "/config/" + dbName.toLowerCase() + ".conf";
        File dbConfigFile = new File(dbConfigPath);

        if (!dbConfigFile.exists()) {
            showAlert("Configuration Missing", "The configuration file for '" + dbName + "' was not found: " + dbConfigPath);
            databaseTab.setContent(new Label("Configuration file missing for this database."));
            return databaseTab;
        }

        if (!DbConnection.initializeConnection(dbConfigPath)) {
            showAlert("Connection Error", "Failed to connect to the database '" + dbName + "'. Please check the configuration.");
            databaseTab.setContent(new Label("Failed to connect to database."));
            return databaseTab;
        }

        TabPane tableTabs = new TabPane();
        for (String table : tables) {
            if (!table.isEmpty()) {
                Tab tableTab = createTableTab(table);
                tableTabs.getTabs().add(tableTab);
            }
        }

        databaseTab.setContent(tableTabs);
        return databaseTab;
    }

    @Override
    public void stop() {
        executorService.shutdown();
    }

    private String preprocessSearchTerm(String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return ""; // Allow empty search for fetching all rows
        }

        // Trim and remove extra spaces
        searchTerm = searchTerm.trim().replaceAll("\\s+", ""); // Remove all spaces

        // Validate each keyword
        String[] keywords = searchTerm.split("\\+|\\|");
        for (String keyword : keywords) {
            // Ensure keywords match allowed characters, including `_` and `%`
            if (!keyword.matches(ALLOWED_CHARACTERS_REGEX)) {
                return null; // Return null if any keyword is invalid
            }
        }

        return searchTerm; // Return processed search term
    }


    private <T> void centerColumn(TableColumn<RandomData, T> column) {
        column.setCellFactory(tc -> {
            TableCell<RandomData, T> cell = new TableCell<>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.toString());
                    }
                }
            };
            cell.setAlignment(Pos.CENTER); // Center the content
            return cell;
        });
    }

    private boolean isValidSearchInput(String input) {
        if (input.isEmpty()) {
            return true; // Allow empty search term
        }

        // Split the input into keywords based on `+` or `|`
        String[] keywords = input.split("\\+|\\|");
        for (String keyword : keywords) {
            if (!keyword.matches(ALLOWED_CHARACTERS_REGEX)) {
                return false; // If any keyword is invalid, return false
            }
        }

        return true; // All keywords are valid
    }


    public static void main(String[] args) {
        launch(args);
    }
}
// RandomData and DbConnection classes remain unchanged.


// RandomData class
class RandomData {
    private final javafx.beans.property.IntegerProperty randomNum;
    private final javafx.beans.property.DoubleProperty randomFloat;
    private final javafx.beans.property.StringProperty md5;
    private final javafx.beans.property.StringProperty md5_1;
    private final javafx.beans.property.StringProperty md5_2;

    public RandomData(int randomNum, double randomFloat, String md5, String md5_1, String md5_2) {
        this.randomNum = new javafx.beans.property.SimpleIntegerProperty(randomNum);
        this.randomFloat = new javafx.beans.property.SimpleDoubleProperty(randomFloat);
        this.md5 = new javafx.beans.property.SimpleStringProperty(md5);
        this.md5_1 = new javafx.beans.property.SimpleStringProperty(md5_1);
        this.md5_2 = new javafx.beans.property.SimpleStringProperty(md5_2);
    }

    public javafx.beans.property.IntegerProperty randomNumProperty() {
        return randomNum;
    }

    public javafx.beans.property.DoubleProperty randomFloatProperty() {
        return randomFloat;
    }

    public javafx.beans.property.StringProperty md5Property() {
        return md5;
    }

    public javafx.beans.property.StringProperty md5_1Property() {
        return md5_1;
    }

    public javafx.beans.property.StringProperty md5_2Property() {
        return md5_2;
    }
}

// DbConnection class
class DbConnection {
    private static final Logger LOGGER = Logger.getLogger(DbConnection.class.getName());
    private static Connection connection;
    private static String currentConfigFile;

    public static boolean initializeConnection(String configFile) {
        File config = new File(configFile);
        if (!config.exists()) {
            Platform.runLater(() -> showAlert("Configuration Missing", "Configuration file not found: " + configFile));
            return false;
        }

        try {
            Properties dbConfig = loadConfig(configFile);

            String host = dbConfig.getProperty("db.host");
            String user = dbConfig.getProperty("db.username");
            String password = dbConfig.getProperty("db.password");
            String database = dbConfig.getProperty("db.name");

            if (host == null || user == null || password == null || database == null) {
                Platform.runLater(() -> showAlert("Configuration Error", "Missing required database properties in: " + configFile));
                return false;
            }

            connection = DriverManager.getConnection(
                    "jdbc:postgresql://" + host + "/" + database,
                    user,
                    password
            );

            LOGGER.info("Database connection initialized successfully with config: " + configFile);
            return true;

        } catch (SQLException e) {
            Platform.runLater(() -> showAlert("Database Connection Error", "Failed to connect to the database: " + e.getMessage()));
        } catch (IOException e) {
            Platform.runLater(() -> showAlert("Configuration Error", "Failed to load configuration file: " + configFile));
        }

        return false;
    }

    private static Properties loadConfig(String configFile) throws IOException {
        File config = new File(configFile);

        if (!config.exists()) {
            throw new FileNotFoundException("Configuration file not found: " + config.getAbsolutePath());
        }

        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(config)) {
            properties.load(fis);
        }

        return properties;
    }

    public static boolean verifyRequiredColumns(String tableName) {
        String query;
        switch (tableName) {
            case "t_random_v1":
            case "t_random_v2":
                query = "SELECT random_num, random_float, md5 FROM " + tableName + " LIMIT 1";
                break;
            case "t_random_v3":
            case "t_random_v4":
                query = "SELECT random_num, random_float, md5_1, md5_2 FROM " + tableName + " LIMIT 1";
                break;
            default:
                throw new IllegalArgumentException("Unknown table: " + tableName);
        }

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.executeQuery();
            return true;
        } catch (SQLException e) {
            LOGGER.warning("Table " + tableName + " is missing required columns: " + e.getMessage());
            return false;
        }
    }

    private static void showErrorDialog(String title, String message, Exception e) {
        LOGGER.severe(message);
        e.printStackTrace();

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });

        LOGGER.log(Level.SEVERE, message, e);
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database connection is not available.");
        }
        return connection;
    }

    public static long executeCountSync(String sql, String tableName, String searchTerm, boolean exactSearch) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (sql.contains("?")) { // Only set parameters if placeholders exist
                setParameters(stmt, tableName, searchTerm, -1, -1, exactSearch);
            }
            System.out.println("Executing Count Query: " + stmt);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getLong(1);
        }
    }

    public static List<Map<String, Object>> executeQuerySync(String sql, String tableName, String searchTerm, int limit, int offset, boolean exactSearch) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (sql.contains("?")) { // Only set parameters if placeholders exist
                setParameters(stmt, tableName, searchTerm, limit, offset, exactSearch);
            }
            System.out.println("Executing query: " + stmt);
            ResultSet rs = stmt.executeQuery();

            ResultSetMetaData metaData = rs.getMetaData();
            System.out.println("Query returned " + metaData.getColumnCount() + " columns.");
            return resultSetToList(rs);
        }
    }

    private static void setParameters(PreparedStatement stmt, String tableName, String searchTerm, int limit, int offset, boolean exactSearch) throws SQLException {
        int index = 1;

        if (!searchTerm.isEmpty()) {
            String[] keywords = searchTerm.split("\\|"); // Adjust for your search logic

            List<String> columns = fetchTableColumns(tableName); // Dynamically fetch columns for the table
            for (String keyword : keywords) {
                String queryParam = exactSearch ? keyword : "%" + keyword.trim() + "%";
                for (String column : columns) {
                    stmt.setString(index++, queryParam);
                }
            }
        }

        // Add limit and offset
        if (limit > 0) {
            stmt.setInt(index++, limit);
        }
        if (offset >= 0) {
            stmt.setInt(index++, offset);
        }
    }

    private static List<String> fetchTableColumns(String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        String query = "SELECT column_name FROM information_schema.columns WHERE table_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, tableName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                columns.add(rs.getString("column_name"));
            }
        }
        return columns;
    }


    private <T> void addCopyToClipboardFeature(TableColumn<RandomData, T> column) {
        column.setCellFactory(tc -> {
            TableCell<RandomData, T> cell = new TableCell<>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.toString());
                }
            };

            // Set mouse click event on the cell
            cell.setOnMouseClicked(event -> {
                if (!cell.isEmpty()) {
                    String text = cell.getText();
                    if (text != null) {
                        copyToClipboard(text);
                        showAlert("Copied to Clipboard", "Copied: " + text);
                    }
                }
            });

            return cell;
        });
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    public static String buildSqlCondition(String tableName, String searchTerm, boolean exactSearch) {
        if (searchTerm.isEmpty()) {
            return "1=1"; // Match all rows if no search term
        }

        String[] columns;
        switch (tableName) {
            case "t_random_v1":
            case "t_random_v2":
                columns = new String[]{"random_num", "random_float", "md5"};
                break;
            case "t_random_v3":
            case "t_random_v4":
                columns = new String[]{"random_num", "random_float", "md5_1", "md5_2"};
                break;
            default:
                throw new IllegalArgumentException("Unknown table: " + tableName);
        }

        String operator = "OR"; // Default to partial search with OR
        String condition;

        if (exactSearch) {
            // Exact match condition
            condition = Arrays.stream(columns)
                    .map(column -> "CAST(" + column + " AS TEXT) = ?")
                    .reduce((a, b) -> a + " OR " + b)
                    .orElse("");
        } else {
            // Partial match condition
            condition = Arrays.stream(columns)
                    .map(column -> "CAST(" + column + " AS TEXT) ILIKE ?")
                    .reduce((a, b) -> a + " OR " + b)
                    .orElse("");
        }

        return "(" + condition + ")";
    }

    private static String[] extractKeywords(String searchTerm) {
        return searchTerm.contains("+") ? searchTerm.split("\\+") : searchTerm.split("\\|");
    }

    private static List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(metaData.getColumnName(i), rs.getObject(i));
            }
            results.add(row);
        }
        return results;
    }


}