package com.tecnoasistencia.excepcion;

/**
 * Excepción lanzada cuando ocurre una falla en el acceso a datos o persistencia JDBC
 * (errores de conexión, violación de restricciones SQL, etc.).
 * Mantiene la causa original para trazabilidad y depuración.
 */
public class AccesoDatosException extends RuntimeException {

    public AccesoDatosException(String mensaje) {
        super(mensaje);
    }

    public AccesoDatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
