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

        Scene scene = new Scene(panel, 1220, 760);
        stage.setTitle("TecnoAsistencia Nicaragua S.A. - Sistema de Gestión de Citas Técnicas");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}