// MainApplication.java
package org.example.gbhujanfx1;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/*
mvn clean install

mvn clean package

java --module-path /Users/gbhujan/javafxsdk/javafx-sdk-23.0.1/lib --add-modules javafx.controls,javafx.fxml -jar target/gbhujanFX1-1.0-SNAPSHOT.jar
*/

/*

mvn javafx:jlink

*/


/*

--module-path /Users/gbhujan/javafxsdk/javafx-sdk-23.0.1/lib --add-modules javafx.controls,javafx.fxml -jar target/gbhujanFX1-1.0-SNAPSHOT.jar

*/

/*

.gitignore  # Include IDE files, build artifacts

pom.xml
src/
└── main/
    ├── java/
    │   └── org/example/gbhujanfx1/
    │       ├── HelloApplication.java
    │       └── module-info.java
    └── resources/  # If you add resources later
README.md  # Add installation/running instructions


jar tf /Users/gbhujan/.m2/repository/org/example/gbhujanFX1/1.0-SNAPSHOT/gbhujanFX1-1.0-SNAPSHOT.jar

*/

public class Application extends javafx.application.Application {
    private TextArea responseArea;

    @Override
    public void init() throws Exception {
        // Initialize any background tasks here
        super.init();
    }

    @Override
    public void stop() throws Exception {
        // Cleanup when app closes
        super.stop();
    }


    @Override
    public void start(Stage stage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // Form fields
        TextField nameField = new TextField();
        nameField.setPromptText("Name");

        TextArea messageField = new TextArea();
        messageField.setPromptText("Message");
        messageField.setPrefRowCount(3);

        responseArea = new TextArea();
        responseArea.setEditable(false);
        responseArea.setPrefRowCount(10);

        Button submitButton = new Button("Submit");
        submitButton.setOnAction(e -> handleSubmit(nameField.getText(), messageField.getText()));

        root.getChildren().addAll(
            new Label("Name:"), nameField,
            new Label("Message:"), messageField,
            submitButton,
            new Label("Response:"), responseArea
        );

        Scene scene = new Scene(root, 400, 500);
        stage.setTitle("API Form Demo");
        stage.setScene(scene);
        stage.show();
    }

    private void handleSubmit(String name, String message) {
        try {
            // Create JSON payload
            String jsonPayload = String.format("""
                {
                    "name": "%s",
                    "message": "%s"
                }""", name, message);

            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://your-api-endpoint.com/post"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

            // Send request asynchronously
            HttpClient.newHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    // Update UI on JavaFX thread
                    javafx.application.Platform.runLater(() ->
                        responseArea.setText(response.body())
                    );
                })
                .exceptionally(error -> {
                    javafx.application.Platform.runLater(() ->
                        responseArea.setText("Error: " + error.getMessage())
                    );
                    return null;
                });

        } catch (Exception e) {
            responseArea.setText("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {

        // Safe startup for macOS
//        Platform.startup(() -> {});
        launch(args);

    }


}

