package com.tecnoasistencia.dao;

import com.tecnoasistencia.conexion.ConexionDB;
import com.tecnoasistencia.modelo.Cliente;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

public class ClienteDAO {

    private Connection abrirConexion() throws SQLException {
        Connection conexion = ConexionDB.conectar();
        if (conexion == null) throw new SQLException("No se pudo conectar con PostgreSQL.");
        return conexion;
    }

    public boolean insertarCliente(Cliente cliente){
        String sql = "INSERT INTO cliente (nombres, apellidos, telefono, correo, activo) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = abrirConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cliente.getNombres());
            pstmt.setString(2, cliente.getApellidos());
            pstmt.setString(3, cliente.getTelefono());
            pstmt.setString(4, cliente.getCorreo());
            pstmt.setBoolean(5, cliente.isActivo());

            return pstmt.executeUpdate() == 1;
        } catch (SQLException e){
            System.err.println("Error al insertar cliente: " + e.getMessage());
            return false;
        }
    }

    public ObservableList<Cliente> listarClientes(){
        ObservableList<Cliente> listaClientes = FXCollections.observableArrayList();
        String sql = "SELECT id, nombres, apellidos, telefono, correo, activo FROM cliente";

        try (Connection conn = abrirConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)){
            while (rs.next()){
                Cliente cliente = new Cliente(
                        rs.getInt("id"),
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("telefono"),
                        rs.getString("correo"),
                        rs.getBoolean("activo")
                );
                listaClientes.add(cliente);
            }
        } catch (SQLException e){
            throw new IllegalStateException("Error al listar clientes: ", e);
        }
        return listaClientes;
    }

    public boolean actualizarCliente(Cliente cliente){
        String sql ="UPDATE cliente SET nombres = ?, apellidos = ?, telefono = ?, correo = ?, activo = ? WHERE id = ?";
        try (Connection conn = abrirConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cliente.getNombres());
            pstmt.setString(2, cliente.getApellidos());
            pstmt.setString(3, cliente.getTelefono());
            pstmt.setString(4, cliente.getCorreo());
            pstmt.setBoolean(5, cliente.isActivo());
            pstmt.setInt(6, cliente.getId());

            return pstmt.executeUpdate() == 1;

        } catch (SQLException e){
            System.err.println("Error al actualizar cliente: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminarCliente(int clienteId){
        String sqlVerificarCitas = "SELECT COUNT(*) FROM cita_tecnica WHERE cliente_id = ?";
        String sqlEliminar = "DELETE FROM cliente WHERE id = ?";

        try (Connection conn = abrirConexion();
             PreparedStatement pstmtVerificar = conn.prepareStatement(sqlVerificarCitas)){

            pstmtVerificar.setInt(1, clienteId);
            try (ResultSet rs = pstmtVerificar.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0){
                    System.err.println("Acción no permitida: No se puede eliminar un cliente con citas técnicas asociadas.");
                    return false;
                }
            }

            try (PreparedStatement pstmtEliminar = conn.prepareStatement(sqlEliminar)){
                pstmtEliminar.setInt(1, clienteId);
                return pstmtEliminar.executeUpdate() == 1;
            }

        } catch (SQLException e) {
            System.err.println("Error al eliminar cliente: " + e.getMessage());
            return false;
        }
    }

    public boolean existeCorreo(String correo, int idExcluir) {
        String sql = "SELECT COUNT(*) FROM cliente WHERE LOWER(TRIM(correo)) = LOWER(TRIM(?)) AND id <> ?";
        try (Connection conn = abrirConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, correo.trim());
            pstmt.setInt(2, idExcluir);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al verificar existencia de correo: ", e);
        }
        return false;
    }

    public boolean tieneCitasAsociadas(int clienteId) {
        String sql = "SELECT COUNT(*) FROM cita_tecnica WHERE cliente_id = ?";
        try (Connection conn = abrirConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, clienteId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al verificar citas asociadas: ", e);
        }
        return false;
    }

    /**
     * Busca clientes por coincidencia parcial en nombres, apellidos o correo
     * sin distinguir mayúsculas ni minúsculas usando PreparedStatement (RF05).
     *
     * @param criterio Texto o término a buscar.
     * @return ObservableList de clientes que coinciden con el criterio.
     */
    public ObservableList<Cliente> buscarClientes(String criterio) {
        ObservableList<Cliente> listaResultado = FXCollections.observableArrayList();
        String sql = "SELECT id, nombres, apellidos, telefono, correo, activo FROM cliente "
                + "WHERE LOWER(nombres) LIKE ? OR LOWER(apellidos) LIKE ? OR LOWER(correo) LIKE ? "
                + "ORDER BY id ASC";

        try (Connection conn = abrirConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String parametro = "%" + (criterio == null ? "" : criterio.trim().toLowerCase()) + "%";
            pstmt.setString(1, parametro);
            pstmt.setString(2, parametro);
            pstmt.setString(3, parametro);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Cliente cliente = new Cliente(
                            rs.getInt("id"),
                            rs.getString("nombres"),
                            rs.getString("apellidos"),
                            rs.getString("telefono"),
                            rs.getString("correo"),
                            rs.getBoolean("activo")
                    );
                    listaResultado.add(cliente);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al buscar clientes: ", e);
        }
        return listaResultado;
    }

}