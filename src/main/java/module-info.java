module org.example.gbhujanfx1 {
   requires javafx.base;
   requires javafx.controls;
   requires javafx.fxml;
   requires javafx.graphics;
   requires javafx.media;
   requires javafx.swing;
   requires javafx.web;

   requires org.controlsfx.controls;
   requires com.dlsc.formsfx;
   requires net.synedra.validatorfx;
   requires org.kordamp.ikonli.javafx;
   requires org.kordamp.bootstrapfx.core;
   requires eu.hansolo.tilesfx;

   requires java.net.http;
   requires java.sql;
   requires org.postgresql.jdbc;

   exports org.example.gbhujanfx1;
   opens org.example.gbhujanfx1 to javafx.fxml;
}