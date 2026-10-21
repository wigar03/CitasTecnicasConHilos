package com.tecnoasistencia.excepcion;

/**
 * Excepción lanzada cuando una operación no cumple con las reglas de negocio
 * del sistema de citas técnicas (por ejemplo: cliente inexistente, estado inválido,
 * campos obligatorios faltantes o inconsistencias temporales).
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }

    public ReglaNegocioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
