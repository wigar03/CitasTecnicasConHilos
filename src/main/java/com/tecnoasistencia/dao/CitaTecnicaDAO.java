package com.tecnoasistencia.dao;

import com.tecnoasistencia.conexion.ConexionDB;
import com.tecnoasistencia.modelo.CitaTecnica;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Data Access Object (DAO) para la entidad CitaTecnica.
 * Gestiona el acceso a datos en PostgreSQL con JDBC, utilizando try-with-resources
 * y PreparedStatement parametrizado para garantizar persistencia e integridad (RF03, RF10).
 */
public class CitaTecnicaDAO {

    private Connection abrirConexion() throws SQLException {
        Connection conexion = ConexionDB.conectar();
        if (conexion == null) {
            throw new SQLException("No se pudo establecer conexión con PostgreSQL. Verifique credenciales y servicio.");
        }
        return conexion;
    }

    /**
     * Lista todas las citas técnicas con los datos del cliente asociado mediante JOIN (RF03, RF04).
     *
     * @return ObservableList de CitaTecnica ordenada cronológicamente.
     * @throws SQLException si ocurre un error de base de datos.
     */
    public ObservableList<CitaTecnica> listarCitas() throws SQLException {
        String sql = "SELECT ct.id, ct.cliente_id, (c.nombres || ' ' || c.apellidos) AS nombre_cliente, "
                + "ct.fecha, ct.hora, ct.tipo_servicio, ct.descripcion, ct.estado "
                + "FROM cita_tecnica ct "
                + "JOIN cliente c ON c.id = ct.cliente_id "
                + "ORDER BY ct.fecha DESC, ct.hora DESC, ct.id DESC";

        ObservableList<CitaTecnica> lista = FXCollections.observableArrayList();

        try (Connection conn = abrirConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                CitaTecnica cita = new CitaTecnica(
                        rs.getInt("id"),
                        rs.getInt("cliente_id"),
                        rs.getString("nombre_cliente"),
                        rs.getDate("fecha").toLocalDate(),
                        rs.getTime("hora").toLocalTime(),
                        rs.getString("tipo_servicio"),
                        rs.getString("descripcion"),
                        rs.getString("estado")
                );
                lista.add(cita);
            }
        }
        return lista;
    }

    /**
     * Inserta una nueva cita técnica en la base de datos (RF03, RF10).
     *
     * @param cita Objeto CitaTecnica con los datos validados.
     * @return true si la inserción fue exitosa, false en caso contrario.
     * @throws SQLException si ocurre un error o violación de clave foránea.
     */
    public boolean insertarCita(CitaTecnica cita) throws SQLException {
        String sql = "INSERT INTO cita_tecnica (cliente_id, fecha, hora, tipo_servicio, descripcion, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = abrirConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            asignarParametros(ps, cita);

            int filasAfectadas = ps.executeUpdate();
            if (filasAfectadas == 1) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        cita.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Actualiza los datos de una cita técnica existente por su ID (RF03, RF10).
     *
     * @param cita Objeto CitaTecnica con los datos modificados.
     * @return true si se actualizó el registro, false si no se encontró.
     * @throws SQLException si ocurre un error de persistencia.
     */
    public boolean actualizarCita(CitaTecnica cita) throws SQLException {
        String sql = "UPDATE cita_tecnica SET cliente_id = ?, fecha = ?, hora = ?, "
                + "tipo_servicio = ?, descripcion = ?, estado = ? WHERE id = ?";

        try (Connection conn = abrirConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            asignarParametros(ps, cita);
            ps.setInt(7, cita.getId());

            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Elimina físicamente una cita técnica por su identificador único (RF03, RF10).
     *
     * @param id Identificador de la cita técnica.
     * @return true si fue eliminada, false si no existía.
     * @throws SQLException si ocurre un error durante la eliminación.
     */
    public boolean eliminarCita(int id) throws SQLException {
        String sql = "DELETE FROM cita_tecnica WHERE id = ?";

        try (Connection conn = abrirConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Busca una cita técnica específica por su ID.
     */
    public CitaTecnica buscarPorId(int id) throws SQLException {
        String sql = "SELECT ct.id, ct.cliente_id, (c.nombres || ' ' || c.apellidos) AS nombre_cliente, "
                + "ct.fecha, ct.hora, ct.tipo_servicio, ct.descripcion, ct.estado "
                + "FROM cita_tecnica ct "
                + "JOIN cliente c ON c.id = ct.cliente_id "
                + "WHERE ct.id = ?";

        try (Connection conn = abrirConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new CitaTecnica(
                            rs.getInt("id"),
                            rs.getInt("cliente_id"),
                            rs.getString("nombre_cliente"),
                            rs.getDate("fecha").toLocalDate(),
                            rs.getTime("hora").toLocalTime(),
                            rs.getString("tipo_servicio"),
                            rs.getString("descripcion"),
                            rs.getString("estado")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Verifica si ya existe una cita registrada para el mismo cliente en la misma fecha y hora (RF07).
     */
    public boolean existeCitaCliente(int clienteId, LocalDate fecha, LocalTime hora, int idExcluir) throws SQLException {
        String sql = "SELECT COUNT(*) FROM cita_tecnica WHERE cliente_id = ? AND fecha = ? AND hora = ? AND id <> ?";
        try (Connection conn = abrirConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            ps.setDate(2, Date.valueOf(fecha));
            ps.setTime(3, Time.valueOf(hora));
            ps.setInt(4, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void asignarParametros(PreparedStatement ps, CitaTecnica cita) throws SQLException {
        ps.setInt(1, cita.getClienteId());
        ps.setDate(2, Date.valueOf(cita.getFecha()));
        ps.setTime(3, Time.valueOf(cita.getHora()));
        ps.setString(4, cita.getTipoServicio());
        ps.setString(5, cita.getDescripcion());
        ps.setString(6, cita.getEstado());
    }
}