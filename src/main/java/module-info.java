module org.example.personalizednewsrecommendationsystem {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires java.sql;
    requires mysql.connector.j;
    requires org.json;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.core5.httpcore5;
    requires org.apache.httpcomponents.httpcore;
    requires javafx.web;
    requires java.net.http;

    opens org.example.OOD.Controllers to javafx.fxml;

    exports org.example.OOD.Application;
}