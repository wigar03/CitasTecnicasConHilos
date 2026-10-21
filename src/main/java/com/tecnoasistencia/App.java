package com.tecnoasistencia;

import com.tecnoasistencia.controlador.CitaTecnicaController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Punto de entrada principal de la aplicación JavaFX para TecnoAsistencia Nicaragua S.A.
 * Coordina la carga modular de vistas y la sincronización reactiva entre pestañas (RF01, RF09).
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        TabPane panel = new TabPane();
        panel.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Cargar vista de clientes
        FXMLLoader loaderClientes = new FXMLLoader(getClass().getResource("/com/tecnoasistencia/vistas/cliente-view.fxml"));
        Parent vistaClientes = loaderClientes.load();
        Tab tabClientes = new Tab("Clientes", vistaClientes);

        // Cargar vista de citas técnicas
        FXMLLoader loaderCitas = new FXMLLoader(getClass().getResource("/com/tecnoasistencia/vistas/cita-view.fxml"));
        Parent vistaCitas = loaderCitas.load();
        CitaTecnicaController citaController = loaderCitas.getController();
        Tab tabCitas = new Tab("Citas técnicas", vistaCitas);

        panel.getTabs().addAll(tabClientes, tabCitas);

        // RF09: Sincronización automática entre pestañas al seleccionar "Citas técnicas"
        panel.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == tabCitas && citaController != null) {
                citaController.cargarDatos();
            }
        });

        // Iniciar hilo demonio de monitoreo continuo de conexión
        com.tecnoasistencia.concurrencia.MonitorConexion monitor = com.tecnoasistencia.concurrencia.MonitorConexion.getInstance();
        monitor.iniciar();

        // Barra inferior de estado global y monitoreo concurrente
        javafx.scene.layout.HBox statusBar = new javafx.scene.layout.HBox(12);
        statusBar.setPadding(new javafx.geometry.Insets(6, 16, 6, 16));
        statusBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: #0f172a; -fx-border-color: #1e293b; -fx-border-width: 1 0 0 0;");

        javafx.scene.control.Label lblAppInfo = new javafx.scene.control.Label("TecnoAsistencia | Concurrencia Multihilo Activa");
        lblAppInfo.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.control.Label lblDetalleConexion = new javafx.scene.control.Label();
        lblDetalleConexion.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        lblDetalleConexion.textProperty().bind(monitor.detalleProperty());

        javafx.scene.control.Label lblBadgeConexion = new javafx.scene.control.Label();
        lblBadgeConexion.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-background-radius: 10;");

        monitor.estadoProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo != null) {
                lblBadgeConexion.setText(nuevo.getEtiqueta());
                lblBadgeConexion.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-background-radius: 10; "
                        + "-fx-text-fill: white; -fx-background-color: " + nuevo.getColorHex() + ";");
            }
        });
        lblBadgeConexion.setText(monitor.getEstadoActual().getEtiqueta());
        lblBadgeConexion.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-background-radius: 10; "
                + "-fx-text-fill: white; -fx-background-color: " + monitor.getEstadoActual().getColorHex() + ";");

        statusBar.getChildren().addAll(lblAppInfo, spacer, lblDetalleConexion, lblBadgeConexion);

        javafx.scene.layout.BorderPane rootLayout = new javafx.scene.layout.BorderPane();
        rootLayout.setCenter(panel);
        rootLayout.setBottom(statusBar);

        Scene scene = new Scene(rootLayout, 1220, 780);
        stage.setTitle("TecnoAsistencia Nicaragua S.A. - Sistema de Gestión de Citas Técnicas");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        com.tecnoasistencia.concurrencia.MonitorConexion.getInstance().detener();
        com.tecnoasistencia.concurrencia.AppThreadPool.shutdown();
        super.stop();
    }


    public static void main(String[] args) {
        launch(args);
    }
}