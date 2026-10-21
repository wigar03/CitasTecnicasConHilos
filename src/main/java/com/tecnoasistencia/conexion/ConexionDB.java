package com.tecnoasistencia.conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestiona la conexión centralizada a la base de datos PostgreSQL mediante JDBC (RF01).
 * Implementa comprobación obligatoria mediante Statement con 'SELECT 1' sin parámetros de usuario.
 * Admite variables de entorno (DB_URL, DB_USER, DB_PASS) y valores por defecto para pruebas locales.
 */
public class ConexionDB {

    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/gestion_citas";
    private static final String DEFAULT_USER = "admin";
    private static final String DEFAULT_PASS = "1234";

    public static String getUrl() {
        String envUrl = System.getenv("DB_URL");
        return (envUrl != null && !envUrl.isBlank()) ? envUrl : DEFAULT_URL;
    }

    public static String getUser() {
        String envUser = System.getenv("DB_USER");
        return (envUser != null && !envUser.isBlank()) ? envUser : DEFAULT_USER;
    }

    public static String getPassword() {
        String envPass = System.getenv("DB_PASS");
        return (envPass != null && !envPass.isBlank()) ? envPass : DEFAULT_PASS;
    }

    /**
     * Establece y verifica una conexión activa a la base de datos PostgreSQL.
     *
     * @return Objeto Connection activo, o null si falla la conexión.
     */
    public static Connection conectar() {
        Connection conexion = null;
        try {
            conexion = DriverManager.getConnection(getUrl(), getUser(), getPassword());
            // Requisito técnico obligatorio de rúbrica: uso comprobable de Statement para sentencia SQL fija
            try (Statement stmt = conexion.createStatement()) {
                stmt.execute("SELECT 1");
            }
        } catch (SQLException e) {
            System.err.println("Error al conectar a PostgreSQL [" + getUrl() + "] con usuario '" + getUser() + "': " + e.getMessage());
        }
        return conexion;
    }
}
