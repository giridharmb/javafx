package org.example.gbhujanfx1;

import java.io.*;
import java.util.Properties;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        Tab db1Tab = createDatabaseTab("DB1", System.getProperty("user.home") + "/config/db.conf", "t_random_v1", "t_random_v2");
        Tab db2Tab = createDatabaseTab("DB2", System.getProperty("user.home") + "/config/db_v2.conf", "t_random_v3", "t_random_v4");

        tabPane.getTabs().addAll(db1Tab, db2Tab);
    }

    private Tab createTableTab(String tableName) {
        Tab tableTab = new Tab(tableName);

        VBox container = new VBox(10);
        container.setPadding(new Insets(10));
        container.setSpacing(5);

        // Search box
        TextField searchField = new TextField();
        searchField.getStyleClass().add("search-box");
        searchField.setPromptText("Search...");
        searchField.setAlignment(Pos.CENTER); // Center-align text in the search box

        Button searchButton = new Button("Search");
        searchButton.getStyleClass().add("search-button");

        // Exact Search Checkbox
        CheckBox exactSearchCheckBox = new CheckBox("Exact Search");
        exactSearchCheckBox.setStyle("-fx-padding: 5;"); // Add some padding

        // TableView
        TableView<RandomData> tableView = new TableView<>();
        tableView.getStyleClass().add("table-view");

        // Set up columns and make them dynamically resize
        setupTableColumns(tableView, tableName);

        // Use UNCONSTRAINED_RESIZE_POLICY to prevent auto-resizing issues
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Bind the TableView width to the container's width
        tableView.prefWidthProperty().bind(container.widthProperty());

        // Embed the TableView inside a ScrollPane
        ScrollPane scrollPane = new ScrollPane(tableView);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); // Enable horizontal scrollbar
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); // Enable vertical scrollbar
        scrollPane.setFitToWidth(true); // Ensure the content stretches to fit the width
        scrollPane.setFitToHeight(true); // Allow vertical resizing
        VBox.setVgrow(scrollPane, Priority.ALWAYS); // Ensure ScrollPane grows with the window

        // Prevent double scrollbars by setting ScrollPane's content width binding
        tableView.prefWidthProperty().bind(scrollPane.widthProperty().subtract(20)); // Account for padding and scrollbar width

        // Pagination
        Pagination pagination = new Pagination();
        pagination.getStyleClass().add("pagination");
        pagination.setMaxPageIndicatorCount(5);
        pagination.setMinHeight(40); // Ensure consistent height for pagination
        pagination.setStyle("-fx-alignment: center;"); // Center pagination buttons horizontally

        // Search actions
        searchButton.setOnAction(e -> searchAndLoadData(searchField, tableView, pagination, tableName, exactSearchCheckBox.isSelected()));
        searchField.setOnAction(e -> searchAndLoadData(searchField, tableView, pagination, tableName, exactSearchCheckBox.isSelected())); // Trigger search on Enter key press

        // Pagination action
        pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            loadTableData(tableView, tableName, searchField.getText(), pagination, newIndex.intValue(), exactSearchCheckBox.isSelected());
        });

        // Layout
        HBox searchBox = new HBox(10, searchField, searchButton, exactSearchCheckBox);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchBox.setAlignment(Pos.CENTER);

        container.getChildren().addAll(searchBox, scrollPane, pagination);
        VBox.setVgrow(container, Priority.ALWAYS); // Make container grow to fill space
        tableTab.setContent(container);

        // Initial data load
        loadTableData(tableView, tableName, "", pagination, 0, false);

        return tableTab;
    }


    private void searchAndLoadData(TextField searchField, TableView<RandomData> tableView, Pagination pagination, String tableName, boolean exactSearch) {
        String searchTerm = searchField.getText().trim();
        if (!isValidSearchInput(searchTerm)) {
            showAlert("Invalid Input", "The search term contains invalid characters. Please correct it and try again.");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String sqlCondition = DbConnection.buildSqlCondition(tableName, searchTerm, exactSearch);

                // Execute count query
                String countQuery = "SELECT COUNT(*) FROM " + tableName + " WHERE " + sqlCondition;
                long totalRecords = DbConnection.executeCountSync(countQuery, tableName, searchTerm, exactSearch);

                Platform.runLater(() -> {
                    if (totalRecords == 0) {
                        tableView.getItems().clear();
                        pagination.setDisable(true);
                        showAlert("No Results", "No data found for the search term.");
                    } else {
                        pagination.setDisable(false);
                        pagination.setPageCount((int) Math.ceil(totalRecords / (double) ROWS_PER_PAGE));
                    }
                });

                // Execute data query
                String dataQuery = "SELECT * FROM " + tableName + " WHERE " + sqlCondition + " LIMIT " + ROWS_PER_PAGE + " OFFSET 0";
                List<RandomData> results = DbConnection.executeQuerySync(dataQuery, tableName, searchTerm, ROWS_PER_PAGE, 0, exactSearch);

                Platform.runLater(() -> {
                    tableView.getItems().clear();
                    tableView.getItems().addAll(results);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", "Failed to load data: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    private void loadTableData(TableView<RandomData> tableView, String tableName, String searchTerm, Pagination pagination, int pageIndex, boolean exactSearch) {
        CompletableFuture.runAsync(() -> {
            try {
                String sqlCondition = DbConnection.buildSqlCondition(tableName, searchTerm, exactSearch);
                String sortQuery = buildSortQuery(tableView);

                // Execute count query
                String countQuery = "SELECT COUNT(*) FROM " + tableName + " WHERE " + sqlCondition;
                long totalRecords = DbConnection.executeCountSync(countQuery, tableName, searchTerm, exactSearch);

                Platform.runLater(() -> {
                    pagination.setDisable(totalRecords == 0);
                    if (totalRecords == 0) {
                        tableView.getItems().clear();
                        showAlert("No Results", "No data found for the search term.");
                        return;
                    }

                    pagination.setPageCount((int) Math.ceil(totalRecords / (double) ROWS_PER_PAGE));
                });

                // Execute data query
                String dataQuery = "SELECT * FROM " + tableName + " WHERE " + sqlCondition + " " + sortQuery
                        + " LIMIT " + ROWS_PER_PAGE + " OFFSET " + (pageIndex * ROWS_PER_PAGE);
                List<RandomData> results = DbConnection.executeQuerySync(dataQuery, tableName, searchTerm, ROWS_PER_PAGE, pageIndex * ROWS_PER_PAGE, exactSearch);

                Platform.runLater(() -> {
                    tableView.getItems().clear();
                    tableView.getItems().addAll(results);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", "Failed to load table data: " + e.getMessage()));
                e.printStackTrace();
            }
        }, executorService);
    }

    private String buildSortQuery(TableView<RandomData> tableView) {
        List<TableColumn<RandomData, ?>> sortOrder = tableView.getSortOrder();

        if (sortOrder.isEmpty()) {
            return ""; // No sorting applied
        }

        StringBuilder orderByClause = new StringBuilder("ORDER BY ");
        for (TableColumn<RandomData, ?> column : sortOrder) {
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

    private String getColumnName(TableColumn<RandomData, ?> column) {
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

    private Tab createDatabaseTab(String dbName, String configFile, String... tables) {
        Tab databaseTab = new Tab(dbName);

        if (!DbConnection.initializeConnection(configFile)) {
            databaseTab.setContent(new Label("Failed to connect to database."));
            return databaseTab;
        }

        TabPane tableTabs = new TabPane();
        for (String table : tables) {
            tableTabs.getTabs().add(createTableTab(table));
        }

        databaseTab.setContent(tableTabs);
        return databaseTab;
    }

    @Override
    public void start(Stage stage) {
        stage.getIcons().add(new Image("appicon.png"));

        // Root layout for the entire application
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        VBox.setVgrow(root, Priority.ALWAYS); // Allow the root layout to grow

        // Initialize and set up database tabs
        setupTabs(); // This initializes tabPane
        VBox.setVgrow(tabPane, Priority.ALWAYS); // Ensure the tab pane grows to fill space
        root.getChildren().add(tabPane); // Add tabPane to the root layout

        // Set up the scene and stage
        Scene scene = new Scene(root, 1500, 900);
        // Load the CSS file
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setTitle("Advanced Search Viewer");
        stage.setResizable(true);
        stage.setScene(scene);

        // Initialize the database connection
        boolean success = DbConnection.initializeConnection(System.getProperty("user.home") + "/config/db.conf");
        if (!success) {
            showAlert("Initialization Failed", "Failed to initialize database connection. Exiting application.");
            Platform.exit();
        }

        // Initial table load
//    refreshTable(tableView);

        stage.show();
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
        currentConfigFile = configFile;
        try {
            Properties config = loadConfig(configFile);

            String host = config.getProperty("db.host");
            String user = config.getProperty("db.username");
            String password = config.getProperty("db.password");
            String database = config.getProperty("db.name");

            if (host == null || user == null || password == null || database == null) {
                throw new IllegalArgumentException("Invalid configuration: Missing required database properties.");
            }

            connection = DriverManager.getConnection(
                    "jdbc:postgresql://" + host + "/" + database,
                    user,
                    password
            );

            LOGGER.info("Database connection initialized successfully with config: " + configFile);
            return true;

        } catch (FileNotFoundException e) {
            showErrorDialog("Configuration Missing", "Configuration file not found: " + configFile, e);
        } catch (SQLException e) {
            showErrorDialog("Database Connection Error", "Failed to connect to the database: " + e.getMessage(), e);
        } catch (IOException | IllegalArgumentException e) {
            showErrorDialog("Configuration Error", e.getMessage(), e);
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
            setParameters(stmt, tableName, searchTerm, -1, -1, exactSearch);
            System.out.println("Executing Count Query: " + stmt);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getLong(1);
        }
    }

    public static List<RandomData> executeQuerySync(String sql, String tableName, String searchTerm, int limit, int offset, boolean exactSearch) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, tableName, searchTerm, limit, offset, exactSearch);
            System.out.println("Executing Query: " + stmt);
            ResultSet rs = stmt.executeQuery();
            return resultSetToList(rs);
        }
    }

    private static void setParameters(PreparedStatement stmt, String tableName, String searchTerm, int limit, int offset, boolean exactSearch) throws SQLException {
        int index = 1;

        if (!searchTerm.isEmpty()) {
            String[] keywords = extractKeywords(searchTerm);

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

            for (String keyword : keywords) {
                String queryParam = exactSearch ? keyword : "%" + keyword.trim() + "%";

                for (String column : columns) {
                    stmt.setString(index++, queryParam);
                }
            }
        }

        // Set limit and offset if placeholders exist
        if (stmt.getParameterMetaData().getParameterCount() >= index) {
            if (limit > 0) {
                stmt.setInt(index++, limit);
            }
            if (offset >= 0) {
                stmt.setInt(index++, offset);
            }
        }
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

    private void showAlert(String title, String message) {
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

    private static List<RandomData> resultSetToList(ResultSet rs) throws SQLException {
        List<RandomData> results = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        if (metaData.getColumnCount() == 0) {
            System.err.println("No columns found in result set.");
            return results; // Return empty list if no columns are found
        }

        while (rs.next()) {
            int randomNum = rs.getInt("random_num");
            double randomFloat = rs.getDouble("random_float");

            String md5 = null;
            String md5_1 = null;
            String md5_2 = null;

            try {
                md5 = rs.getString("md5");
            } catch (SQLException ignored) {
            }

            try {
                md5_1 = rs.getString("md5_1");
            } catch (SQLException ignored) {
            }

            try {
                md5_2 = rs.getString("md5_2");
            } catch (SQLException ignored) {
            }

            results.add(new RandomData(randomNum, randomFloat, md5, md5_1, md5_2));
        }
        return results;
    }


}