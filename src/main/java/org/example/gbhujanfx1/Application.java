package org.example.gbhujanfx1;


import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Application extends javafx.application.Application {
   private TextArea responseArea;

   @Override
   public void init() throws Exception {
       super.init();
   }

   @Override
   public void stop() throws Exception {
       super.stop();
   }

   @Override
   public void start(Stage stage) {
       VBox root = new VBox(10);
       root.setPadding(new Insets(10));

       // Personal Info Section
       Label personalInfoLabel = new Label("Personal Information");
       personalInfoLabel.setStyle("-fx-font-weight: bold");

       TextField nameField = new TextField();
       nameField.setPromptText("Full Name");

       TextField emailField = new TextField();
       emailField.setPromptText("Email");

       // Address Section
       Label addressLabel = new Label("Address");
       addressLabel.setStyle("-fx-font-weight: bold");

       TextField streetField = new TextField();
       streetField.setPromptText("Street Address");

       HBox cityStateBox = new HBox(10);
       TextField cityField = new TextField();
       cityField.setPromptText("City");
       ComboBox<String> stateComboBox = new ComboBox<>();
       stateComboBox.getItems().addAll("CA", "NY", "TX", "FL", "WA", "OR", "NV", "AZ");
       stateComboBox.setPromptText("State");
       cityStateBox.getChildren().addAll(cityField, stateComboBox);

       // Message Section
       Label messageLabel = new Label("Message");
       messageLabel.setStyle("-fx-font-weight: bold");

       TextArea messageField = new TextArea();
       messageField.setPromptText("Your message here");
       messageField.setPrefRowCount(3);

       // Priority Selection
       Label priorityLabel = new Label("Priority");
       priorityLabel.setStyle("-fx-font-weight: bold");

       ToggleGroup priorityGroup = new ToggleGroup();
       RadioButton lowPriority = new RadioButton("Low");
       RadioButton mediumPriority = new RadioButton("Medium");
       RadioButton highPriority = new RadioButton("High");
       lowPriority.setToggleGroup(priorityGroup);
       mediumPriority.setToggleGroup(priorityGroup);
       highPriority.setToggleGroup(priorityGroup);
       mediumPriority.setSelected(true);
       HBox priorityBox = new HBox(10);
       priorityBox.getChildren().addAll(lowPriority, mediumPriority, highPriority);

       // Categories
       Label categoriesLabel = new Label("Categories");
       categoriesLabel.setStyle("-fx-font-weight: bold");

       CheckBox generalCheckBox = new CheckBox("General");
       CheckBox technicalCheckBox = new CheckBox("Technical");
       CheckBox billingCheckBox = new CheckBox("Billing");
       HBox categoriesBox = new HBox(10);
       categoriesBox.getChildren().addAll(generalCheckBox, technicalCheckBox, billingCheckBox);

       // Response Area
       responseArea = new TextArea();
       responseArea.setEditable(false);
       responseArea.setPrefRowCount(5);

       // Submit Button
       Button submitButton = new Button("Submit Request");
       submitButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

       root.getChildren().addAll(
           personalInfoLabel,
           new Label("Name:"), nameField,
           new Label("Email:"), emailField,
           addressLabel,
           new Label("Street:"), streetField,
           new Label("City/State:"), cityStateBox,
           messageLabel,
           messageField,
           priorityLabel,
           priorityBox,
           categoriesLabel,
           categoriesBox,
           submitButton,
           new Label("Response:"),
           responseArea
       );


       Scene scene = new Scene(root, 500, 700);
       stage.setTitle("Support Request Form");
       stage.setResizable(false);
       stage.setWidth(500);
       stage.setHeight(700);
       stage.setScene(scene);
       stage.show();

       submitButton.setOnAction(e -> handleSubmit(
           nameField.getText(),
           emailField.getText(),
           streetField.getText(),
           cityField.getText(),
           stateComboBox.getValue(),
           messageField.getText(),
           ((RadioButton)priorityGroup.getSelectedToggle()).getText(),
           getSelectedCategories(generalCheckBox, technicalCheckBox, billingCheckBox)
       ));

   }

   private String getSelectedCategories(CheckBox... boxes) {
       return Arrays.stream(boxes)
           .filter(CheckBox::isSelected)
           .map(CheckBox::getText)
           .collect(Collectors.joining(", "));
   }

   private void handleSubmit(String name, String email, String street,
                           String city, String state, String message,
                           String priority, String categories) {
       try {
           String jsonPayload = String.format("""
               {
                   "name": "%s",
                   "email": "%s", 
                   "street": "%s",
                   "city": "%s",
                   "state": "%s",
                   "message": "%s",
                   "priority": "%s",
                   "categories": "%s"
               }""", name, email, street, city, state, message, priority, categories);

           HttpRequest request = HttpRequest.newBuilder()
               .uri(new URI("http://your-api-endpoint.com/post"))
               .header("Content-Type", "application/json")
               .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
               .build();

           HttpClient.newHttpClient()
               .sendAsync(request, HttpResponse.BodyHandlers.ofString())
               .thenAccept(response -> {
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
       launch(args);
   }
}