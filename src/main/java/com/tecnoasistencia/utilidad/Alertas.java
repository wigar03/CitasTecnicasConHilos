package com.tecnoasistencia.utilidad;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/** Mensajes y confirmaciones comunes para ambos módulos. */
public final class Alertas {
    private Alertas() { }
    public static void mostrar(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
    public static boolean confirmar(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.OK, ButtonType.CANCEL);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        return alerta.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }
    public static void errorDatos(Exception error) {
        error.printStackTrace();
        mostrar(Alert.AlertType.ERROR, "Error de base de datos",
                "No se pudo completar la operación. Revise PostgreSQL, la conexión y el esquema de tablas. "
                        + "Consulte la consola para ver el detalle.");
    }
}