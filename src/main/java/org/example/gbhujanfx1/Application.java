package org.example.gbhujanfx1;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
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

    @Override
    public void start(Stage stage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // Search Box
        searchField = new TextField();
        searchField.setPromptText("Search across all columns...");
        searchField.setPrefWidth(300);

        Button searchButton = new Button("Search");
        searchButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

        HBox searchBox = new HBox(10);
        searchBox.getChildren().addAll(new Label("Search:"), searchField, searchButton);
        searchBox.setPadding(new Insets(0, 0, 10, 0));

        // Pagination
        pagination = new Pagination();
        pagination.setPageFactory(this::createPage);
        pagination.setMaxPageIndicatorCount(5);

        root.getChildren().addAll(searchBox, pagination);

        // Search Button Action
        searchButton.setOnAction(e -> refreshTable(searchField.getText()));
        refreshTable(""); // Initial Load

        // Scene Setup
        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Random Data Viewer");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        executorService.shutdown();
    }

    private void refreshTable(String searchTerm) {
        CompletableFuture.runAsync(() -> {
            try {
                // Fetch row count for pagination
                long count = DbConnection.executeCountSync(
                        "SELECT COUNT(*) FROM t_random " +
                                "WHERE (CAST(random_num AS TEXT) ILIKE ? OR CAST(random_float AS TEXT) ILIKE ? OR md5 ILIKE ?)",
                        searchTerm
                );
                int pageCount = (int) Math.ceil(count / (double) ROWS_PER_PAGE);

                // Update pagination and reset to the first page
                Platform.runLater(() -> {
                    pagination.setPageCount(pageCount);
                    pagination.setCurrentPageIndex(0);
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, executorService);
    }

    private Node createPage(int pageIndex) {
        TableView<RandomData> pageTableView = new TableView<>();
        setupTableColumns(pageTableView);

        // Fetch data asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                List<RandomData> results = DbConnection.executeQuerySync(
                        "SELECT * FROM t_random " +
                                "WHERE (CAST(random_num AS TEXT) ILIKE ? OR CAST(random_float AS TEXT) ILIKE ? OR md5 ILIKE ?) " +
                                "ORDER BY random_num LIMIT ? OFFSET ?",
                        searchField.getText(),
                        ROWS_PER_PAGE,
                        pageIndex * ROWS_PER_PAGE
                );

                // Update the TableView on the JavaFX thread
                Platform.runLater(() -> pageTableView.getItems().addAll(results));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, executorService);

        return pageTableView;
    }

    private void setupTableColumns(TableView<RandomData> table) {
        TableColumn<RandomData, Integer> numCol = new TableColumn<>("Random Number");
        numCol.setCellValueFactory(cellData -> cellData.getValue().randomNumProperty().asObject());
        numCol.setPrefWidth(200);
        numCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<RandomData, Double> floatCol = new TableColumn<>("Random Float");
        floatCol.setCellValueFactory(cellData -> cellData.getValue().randomFloatProperty().asObject());
        floatCol.setPrefWidth(200);
        floatCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<RandomData, String> md5Col = new TableColumn<>("MD5");
        md5Col.setCellValueFactory(cellData -> cellData.getValue().md5Property());
        md5Col.setPrefWidth(380);
        md5Col.setStyle("-fx-alignment: CENTER-LEFT;");

        table.getColumns().addAll(numCol, floatCol, md5Col);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

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
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "please_ignore_this");
    }

    public static List<RandomData> executeQuerySync(String sql, String searchTerm, int limit, int offset) throws SQLException {
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            String likeTerm = "%" + searchTerm + "%";
            stmt.setString(1, likeTerm);
            stmt.setString(2, likeTerm);
            stmt.setString(3, likeTerm);
            stmt.setInt(4, limit);
            stmt.setInt(5, offset);
            ResultSet rs = stmt.executeQuery();
            return resultSetToList(rs);
        }
    }

    public static long executeCountSync(String sql, String searchTerm) throws SQLException {
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            String likeTerm = "%" + searchTerm + "%";
            stmt.setString(1, likeTerm);
            stmt.setString(2, likeTerm);
            stmt.setString(3, likeTerm);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getLong(1);
        }
    }

    private static List<RandomData> resultSetToList(ResultSet rs) throws SQLException {
        List<RandomData> results = new ArrayList<>();
        while (rs.next()) {
            results.add(new RandomData(rs.getInt("random_num"), rs.getDouble("random_float"), rs.getString("md5")));
        }
        return results;
    }
}
