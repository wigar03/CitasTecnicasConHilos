package com.tecnoasistencia.modelo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Representa la entidad de Cita Técnica en el sistema TecnoAsistencia Nicaragua S.A.
 * Mapea directamente la tabla 'cita_tecnica' en PostgreSQL según el modelo obligatorio de la rúbrica:
 * - id INT PK AUTO_INCREMENT
 * - cliente_id INT NOT NULL FK a cliente.id
 * - fecha DATE NOT NULL
 * - hora TIME NOT NULL
 * - tipo_servicio VARCHAR(80) NOT NULL
 * - descripcion VARCHAR(255) NOT NULL
 * - estado VARCHAR(20) NOT NULL (Pendiente, En proceso, Finalizada, Cancelada)
 */
public class CitaTecnica {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private int id;
    private int clienteId;
    private String nombreCliente; // Obtenido vía JOIN con cliente (nombres || ' ' || apellidos)
    private LocalDate fecha;
    private LocalTime hora;
    private String tipoServicio;
    private String descripcion;
    private String estado;

    /**
     * Constructor por defecto. Asigna el estado inicial en "Pendiente".
     */
    public CitaTecnica() {
        this.estado = EstadoCita.PENDIENTE.getDescripcion();
    }

    /**
     * Constructor para nuevas citas antes de su inserción en la base de datos.
     */
    public CitaTecnica(int clienteId, LocalDate fecha, LocalTime hora,
                       String tipoServicio, String descripcion, String estado) {
        this.clienteId = clienteId;
        this.fecha = fecha;
        this.hora = hora;
        this.tipoServicio = tipoServicio;
        this.descripcion = descripcion;
        this.estado = estado != null ? estado : EstadoCita.PENDIENTE.getDescripcion();
    }

    /**
     * Constructor completo para persistencia sin nombre de cliente.
     */
    public CitaTecnica(int id, int clienteId, LocalDate fecha, LocalTime hora,
                       String tipoServicio, String descripcion, String estado) {
        this.id = id;
        this.clienteId = clienteId;
        this.fecha = fecha;
        this.hora = hora;
        this.tipoServicio = tipoServicio;
        this.descripcion = descripcion;
        this.estado = estado != null ? estado : EstadoCita.PENDIENTE.getDescripcion();
    }

    /**
     * Constructor completo para visualización con JOIN con la tabla cliente (RF03, RF04).
     */
    public CitaTecnica(int id, int clienteId, String nombreCliente, LocalDate fecha,
                       LocalTime hora, String tipoServicio, String descripcion, String estado) {
        this.id = id;
        this.clienteId = clienteId;
        this.nombreCliente = nombreCliente;
        this.fecha = fecha;
        this.hora = hora;
        this.tipoServicio = tipoServicio;
        this.descripcion = descripcion;
        this.estado = estado != null ? estado : EstadoCita.PENDIENTE.getDescripcion();
    }

    /**
     * Constructor de compatibilidad para consultas simplificadas.
     */
    public CitaTecnica(int id, int clienteId, String nombreCliente, LocalDate fecha,
                       LocalTime hora, String tipoServicio, String estado) {
        this(id, clienteId, nombreCliente, fecha, hora, tipoServicio, tipoServicio, estado);
    }

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getClienteId() {
        return clienteId;
    }

    public void setClienteId(int clienteId) {
        this.clienteId = clienteId;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public String getTipoServicio() {
        return tipoServicio;
    }

    public void setTipoServicio(String tipoServicio) {
        this.tipoServicio = tipoServicio;
    }

    /**
     * Alias de tipoServicio para compatibilidad con RF04 (columna "Servicio" en TableView).
     */
    public String getServicio() {
        return tipoServicio;
    }

    public void setServicio(String servicio) {
        this.tipoServicio = servicio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public EstadoCita getEstadoEnum() {
        return estado != null ? EstadoCita.fromDescripcion(estado) : null;
    }

    public void setEstadoEnum(EstadoCita estadoEnum) {
        this.estado = estadoEnum != null ? estadoEnum.getDescripcion() : null;
    }

    // ==========================================
    // MÉTODOS DE FORMATO PARA JAVAFX
    // ==========================================

    public String getFechaFormateada() {
        return fecha != null ? fecha.format(FORMATO_FECHA) : "";
    }

    public String getHoraFormateada() {
        return hora != null ? hora.format(FORMATO_HORA) : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CitaTecnica that = (CitaTecnica) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Cita #" + id + " - " + (nombreCliente != null ? nombreCliente : "Cliente " + clienteId)
                + " (" + getFechaFormateada() + " " + getHoraFormateada() + " - " + estado + ")";
    }
}
