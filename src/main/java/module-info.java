module com.tecnoasistencia {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires java.sql;
    requires org.postgresql.jdbc;

    opens com.tecnoasistencia to javafx.fxml;
    opens com.tecnoasistencia.controlador to javafx.fxml;
    opens com.tecnoasistencia.modelo to javafx.base, javafx.fxml;

    exports com.tecnoasistencia;
    exports com.tecnoasistencia.controlador;
    exports com.tecnoasistencia.modelo;
}
