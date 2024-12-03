package org.example.gbhujanfx1;

import java.io.*;
import java.util.Properties;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
    private final int ROWS_PER_PAGE = 20;
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    private static final String ALLOWED_CHARACTERS_REGEX = "^[a-zA-Z0-9\\-_.,=@#$&:;/()]+$";


    @Override
    public void start(Stage stage) {

        stage.getIcons().add(new Image("appicon.png"));

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        searchField = new TextField();
        searchField.setPromptText("Search across all columns using '+' for AND, '|' for OR...");
        searchField.setPrefWidth(500);


        HBox searchBox = new HBox(10);
        Label searchLabel = new Label("Search:");
        searchField = new TextField();
        searchField.setPromptText("Search across all columns using '+' for AND, '|' for OR...");

// Center-align the text inside the searchField
        searchField.setAlignment(Pos.CENTER);

// Allow the searchField to expand and fill remaining space
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchButton = new Button("Search");
        searchButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

// Add the components to the searchBox
        searchBox.getChildren().addAll(searchLabel, searchField, searchButton);
        searchBox.setPadding(new Insets(0, 0, 10, 0));

// Align the components properly
        searchBox.setAlignment(Pos.CENTER_LEFT);


        TableView<RandomData> tableView = new TableView<>();
        setupTableColumns(tableView);

        VBox.setVgrow(tableView, Priority.ALWAYS); // Allow table to expand vertically
        tableView.prefHeightProperty().bind(root.heightProperty().subtract(120)); // Adjust height dynamically

        pagination = new Pagination();
        pagination.setMaxPageIndicatorCount(5);

        // Listener to update data when the page changes
        pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            loadPageData(tableView, newIndex.intValue());
        });

        root.getChildren().addAll(searchBox, tableView, pagination);

        searchButton.setOnAction(e -> {
            refreshTable(tableView);
        });

        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Advanced Search Viewer");
        stage.setResizable(true); // Allow resizing
        stage.setScene(scene);

        // Initialize DB connection
        boolean success = DbConnection.initializeConnection();
        if (!success) {
            showAlert("Initialization Failed", "Failed to initialize database connection. Exiting application.");
            Platform.exit();
        }

        refreshTable(tableView); // Initial Load
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


    private void refreshTable(TableView<RandomData> tableView) {
        String searchTerm = preprocessSearchTerm(searchField.getText().trim());
        if (searchTerm == null) {
            showAlert("Invalid Input", "The search term contains invalid characters.");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String sqlCondition = DbConnection.buildSqlCondition(searchTerm);

                long count = DbConnection.executeCountSync(
                        "SELECT COUNT(*) FROM t_random WHERE " + sqlCondition,
                        searchTerm
                );

                int pageCount = (int) Math.ceil(count / (double) ROWS_PER_PAGE);

                Platform.runLater(() -> {
                    pagination.setPageCount(pageCount);
                    pagination.setCurrentPageIndex(0); // Reset to the first page
                    loadPageData(tableView, 0); // Load the first page of data
                });
            } catch (SQLException e) {
                Platform.runLater(() -> showAlert("Database Error", "Failed to load data: " + e.getMessage()));
                e.printStackTrace();
            }
        }, executorService);
    }

    private void loadPageData(TableView<RandomData> tableView, int pageIndex) {
        CompletableFuture.runAsync(() -> {
            try {
                String searchTerm = preprocessSearchTerm(searchField.getText().trim());
                String sqlCondition = DbConnection.buildSqlCondition(searchTerm);

                var results = DbConnection.executeQuerySync(
                        "SELECT * FROM t_random WHERE " + sqlCondition + " ORDER BY random_num LIMIT ? OFFSET ?",
                        searchTerm,
                        ROWS_PER_PAGE,
                        pageIndex * ROWS_PER_PAGE
                );

                Platform.runLater(() -> {
                    tableView.getItems().clear(); // Clear previous data
                    tableView.getItems().addAll(results); // Add new data
                });
            } catch (SQLException e) {
                Platform.runLater(() -> showAlert("Database Error", "Failed to load page: " + e.getMessage()));
                e.printStackTrace();
            }
        }, executorService);
    }


    private Node createPage(int pageIndex) {
        TableView<RandomData> pageTableView = new TableView<>();
        setupTableColumns(pageTableView);

        CompletableFuture.runAsync(() -> {
            try {
                String sqlCondition = DbConnection.buildSqlCondition(searchField.getText().trim());

                var results = DbConnection.executeQuerySync(
                        "SELECT * FROM t_random WHERE " + sqlCondition + " ORDER BY random_num LIMIT ? OFFSET ?",
                        searchField.getText().trim(),
                        ROWS_PER_PAGE,
                        pageIndex * ROWS_PER_PAGE
                );

                Platform.runLater(() -> {
                    pageTableView.getItems().clear(); // Clear previous results
                    pageTableView.getItems().addAll(results); // Add new results
                });
            } catch (SQLException e) {
                Platform.runLater(() -> showAlert("Database Error", "Failed to load page: " + e.getMessage()));
                e.printStackTrace();
            }
        }, executorService);

        return pageTableView;
    }

    private void setupTableColumns(TableView<RandomData> table) {
        TableColumn<RandomData, Integer> numCol = new TableColumn<>("Random Number");
        numCol.setCellValueFactory(cellData -> cellData.getValue().randomNumProperty().asObject());
        numCol.setPrefWidth(200);
        centerColumn(numCol); // Center align

        TableColumn<RandomData, Double> floatCol = new TableColumn<>("Random Float");
        floatCol.setCellValueFactory(cellData -> cellData.getValue().randomFloatProperty().asObject());
        floatCol.setPrefWidth(200);
        centerColumn(floatCol); // Center align

        TableColumn<RandomData, String> md5Col = new TableColumn<>("MD5");
        md5Col.setCellValueFactory(cellData -> cellData.getValue().md5Property());
        md5Col.setPrefWidth(380);
        centerColumn(md5Col); // Center align

        table.getColumns().addAll(numCol, floatCol, md5Col);
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


    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
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

    public RandomData(int randomNum, double randomFloat, String md5) {
        this.randomNum = new javafx.beans.property.SimpleIntegerProperty(randomNum);
        this.randomFloat = new javafx.beans.property.SimpleDoubleProperty(randomFloat);
        this.md5 = new javafx.beans.property.SimpleStringProperty(md5);
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
}

// DbConnection class
class DbConnection {
    private static final Logger LOGGER = Logger.getLogger(DbConnection.class.getName());
    private static Connection connection;

    public static boolean initializeConnection() {
        try {
            Properties config = loadConfig();

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

            verifyRequiredColumns();

            LOGGER.info("Database connection initialized successfully.");
            return true;

        } catch (FileNotFoundException e) {
            showErrorDialog("Configuration Missing", "Configuration file not found: " + getConfigFilePath(), e);
        } catch (SQLException e) {
            showErrorDialog("Database Connection Error", "Failed to connect to the database: " + e.getMessage(), e);
        } catch (IOException | IllegalArgumentException e) {
            showErrorDialog("Configuration Error", e.getMessage(), e);
        }
        return false;
    }

    private static Properties loadConfig() throws IOException {
        File configFile = new File(getConfigFilePath());

        if (!configFile.exists()) {
            throw new FileNotFoundException("Configuration file not found: " + configFile.getAbsolutePath());
        }

        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);
        }

        return properties;
    }

    private static String getConfigFilePath() {
        return System.getProperty("user.home") + "/config/db.conf";
    }

    private static void verifyRequiredColumns() throws SQLException {
        String query = "SELECT random_num, random_float, md5 FROM t_random LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.executeQuery();
        } catch (SQLException e) {
            throw new SQLException("Required columns are missing in the database.");
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

    public static List<RandomData> executeQuerySync(String sql, String searchTerm, int limit, int offset) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, searchTerm, limit, offset);
            ResultSet rs = stmt.executeQuery();
            return resultSetToList(rs);
        }
    }

    public static long executeCountSync(String sql, String searchTerm) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, searchTerm, -1, -1); // No LIMIT or OFFSET for COUNT query
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getLong(1);
        }
    }

    private static void setParameters(PreparedStatement stmt, String searchTerm, int limit, int offset) throws SQLException {
        int index = 1;

        if (!searchTerm.isEmpty()) {
            String[] keywords = extractKeywords(searchTerm);
            for (String keyword : keywords) {
                String escapedKeyword = keyword.replace("_", "\\_").replace("%", "\\%");
                String likeTerm = "%" + escapedKeyword.trim() + "%";

                stmt.setString(index++, likeTerm); // For random_num
                stmt.setString(index++, likeTerm); // For random_float
                stmt.setString(index++, likeTerm); // For md5
            }
        }

        // Set LIMIT and OFFSET only if specified
        if (limit > 0) {
            stmt.setInt(index++, limit);
        }
        if (offset >= 0) {
            stmt.setInt(index, offset);
        }

        System.out.println("Prepared Statement Parameters Set: " + stmt);
    }

    public static String buildSqlCondition(String searchTerm) {
        if (searchTerm.isEmpty()) {
            return "1=1"; // Match all rows if no search term
        }

        String operator = searchTerm.contains("+") ? "AND" : "OR";
        String[] keywords = extractKeywords(searchTerm);

        StringBuilder condition = new StringBuilder();
        for (int i = 0; i < keywords.length; i++) {
            condition.append("(")
                    .append("CAST(random_num AS TEXT) ILIKE ? OR ")
                    .append("CAST(random_float AS TEXT) ILIKE ? OR ")
                    .append("md5 ILIKE ?")
                    .append(")");
            if (i < keywords.length - 1) {
                condition.append(" ").append(operator).append(" ");
            }
        }

        return condition.toString();
    }

    private static String[] extractKeywords(String searchTerm) {
        return searchTerm.contains("+") ? searchTerm.split("\\+") : searchTerm.split("\\|");
    }

    private static List<RandomData> resultSetToList(ResultSet rs) throws SQLException {
        List<RandomData> results = new ArrayList<>();
        while (rs.next()) {
            results.add(new RandomData(rs.getInt("random_num"), rs.getDouble("random_float"), rs.getString("md5")));
        }
        return results;
    }
}