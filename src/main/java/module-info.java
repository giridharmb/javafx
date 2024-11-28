module org.example.gbhujanfx1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires javafx.base;
    requires javafx.graphics;
//    requires javafx.swing;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;

    exports org.example.gbhujanfx1;
    opens org.example.gbhujanfx1 to javafx.fxml;
}
