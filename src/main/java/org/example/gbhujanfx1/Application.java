// MainApplication.java
package org.example.gbhujanfx1;

import java.net.URI;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/*
Final:

mvn clean compile package javafx:jlink jpackage:jpackage

Will Generate >>

target/dist/MyApp-1.0.0.pkg

Log >>

[INFO] Using: /Users/giri/Library/Java/JavaVirtualMachines/openjdk-23.0.1/Contents/Home/bin/jpackage, major version: 23
[INFO] jpackage options:
[INFO]   --name MyApp
[INFO]   --dest /Users/giri/git/java/javafx/target/dist
[INFO]   --type pkg
[INFO]   --app-version 1.0.0
[INFO]   --runtime-image /Users/giri/git/java/javafx/target/gbhujanfx1
[INFO]   --input /Users/giri/git/java/javafx/target
[INFO]   --main-class org.example.gbhujanfx1.Application
[INFO]   --main-jar gbhujanfx1-1.0-SNAPSHOT.jar
*/

/*
mvn clean install

mvn clean package
*/

/*
Run > Edit Configurations > Template = "Application"

Run > Edit Configurations > VM Options >

--module-path /Users/giri/javafx/javafx-sdk-23.0.1/lib --add-modules javafx.controls,javafx.fxml,javafx.graphics -jar target/gbhujanFX1-1.0-SNAPSHOT.jar --add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.util=ALL-UNNAMED --add-exports javafx.base/com.sun.javafx.reflect=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED --add-exports javafx.graphics/com.sun.glass.ui=ALL-UNNAMED

Main Class:

org.example.gbhujanfx1.Application

Working Directory >

$PROJECT_DIR$
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

Query JavaFX Packages >>

jar tf /Users/giri/.m2/repository/org/example/gbhujanfx1/1.0-SNAPSHOT/gbhujanfx1-1.0-SNAPSHOT.jar|grep javafx
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

