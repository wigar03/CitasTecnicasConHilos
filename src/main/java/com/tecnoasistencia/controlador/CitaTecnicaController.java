package com.tecnoasistencia.controlador;

import com.tecnoasistencia.dao.CitaTecnicaDAO;
import com.tecnoasistencia.dao.ClienteDAO;
import com.tecnoasistencia.modelo.CitaTecnica;
import com.tecnoasistencia.modelo.Cliente;
import com.tecnoasistencia.utilidad.Alertas;
import com.tecnoasistencia.utilidad.Validaciones;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.concurrent.Task;
import com.tecnoasistencia.concurrencia.AppThreadPool;
import com.tecnoasistencia.concurrencia.AsyncUIHelper;

/**
 * Controlador de la vista de citas técnicas (cita-view.fxml).
 * Implementa las operaciones CRUD, validaciones de negocio, filtros dinámicos
 * y alertas requeridas por la rúbrica (RF03, RF04, RF06, RF07, RF08, RF09).
 */
public class CitaTecnicaController {

    @FXML private ComboBox<Cliente> cmbCliente;
    @FXML private DatePicker dpFecha;
    @FXML private TextField txtHora;
    @FXML private TextField txtServicio;
    @FXML private TextField txtDescripcion;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private TextField txtId;

    @FXML private ComboBox<String> cmbFiltroEstado;
    @FXML private DatePicker dpFiltroFecha;

    @FXML private TableView<CitaTecnica> tblCitas;
    @FXML private TableColumn<CitaTecnica, Integer> colId;
    @FXML private TableColumn<CitaTecnica, String> colCliente;
    @FXML private TableColumn<CitaTecnica, LocalDate> colFecha;
    @FXML private TableColumn<CitaTecnica, LocalTime> colHora;
    @FXML private TableColumn<CitaTecnica, String> colServicio;
    @FXML private TableColumn<CitaTecnica, String> colDescripcion;
    @FXML private TableColumn<CitaTecnica, String> colEstado;

    @FXML private Button btnGuardar;
    @FXML private Button btnActualizar;
    @FXML private Button btnEliminar;
    @FXML private Label lblContador;
    @FXML private ProgressIndicator progresoCitas;
    @FXML private Label lblEstadoCitas;

    @FXML private Label lblMetricaTotal;
    @FXML private Label lblMetricaPendientes;
    @FXML private Label lblMetricaCompletadas;
    @FXML private Label lblMetricaCanceladas;
    @FXML private Label lblMetricaEfectividad;
    @FXML private Label lblRecordatorio;

    public record DatosCargaCitas(ObservableList<Cliente> clientes, ObservableList<CitaTecnica> citas) {}

    private final CitaTecnicaDAO citaDAO = new CitaTecnicaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ObservableList<CitaTecnica> citas = FXCollections.observableArrayList();
    private final FilteredList<CitaTecnica> filtradas = new FilteredList<>(citas, cita -> true);
    private int idSeleccionado;

    @FXML
    public void initialize() {
        cmbEstado.getItems().setAll(Validaciones.ESTADOS);
        cmbFiltroEstado.getItems().add("Todos");
        cmbFiltroEstado.getItems().addAll(Validaciones.ESTADOS);
        cmbFiltroEstado.setValue("Todos");

        dpFecha.setEditable(false);
        dpFiltroFecha.setEditable(false);

        // Deshabilitar fechas pasadas en el selector visual para nuevas citas
        dpFecha.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (idSeleccionado == 0 && date != null && date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #94a3b8;");
                }
            }
        });

        // Filtros de entrada física en tiempo real para evitar desbordamientos
        txtHora.setTextFormatter(Validaciones.crearFiltroLongitud(5));
        txtServicio.setTextFormatter(Validaciones.crearFiltroLongitud(80));
        txtDescripcion.setTextFormatter(Validaciones.crearFiltroLongitud(255));

        cmbCliente.setCellFactory(lista -> celdaCliente());
        cmbCliente.setButtonCell(celdaCliente());

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colServicio.setCellValueFactory(new PropertyValueFactory<>("tipoServicio"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        colFecha.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate valor, boolean empty) {
                super.updateItem(valor, empty);
                setText(empty || valor == null ? null : valor.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
        });

        colHora.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(LocalTime valor, boolean empty) {
                super.updateItem(valor, empty);
                setText(empty || valor == null ? null : valor.format(Validaciones.HORA));
            }
        });

        SortedList<CitaTecnica> ordenadas = new SortedList<>(filtradas);
        ordenadas.comparatorProperty().bind(tblCitas.comparatorProperty());
        tblCitas.setItems(ordenadas);
        tblCitas.setPlaceholder(new Label("No hay citas registradas para los filtros seleccionados."));

        tblCitas.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            if (actual != null) {
                cargarFormulario(actual);
            }
        });

        cmbFiltroEstado.valueProperty().addListener((obs, anterior, actual) -> aplicarFiltros());
        dpFiltroFecha.valueProperty().addListener((obs, anterior, actual) -> aplicarFiltros());

        // Enlazar monitor demonio de recordatorios a la etiqueta visual
        com.tecnoasistencia.concurrencia.MonitorRecordatorios monitorRec =
                com.tecnoasistencia.concurrencia.MonitorRecordatorios.getInstance();
        if (lblRecordatorio != null) {
            lblRecordatorio.textProperty().bind(monitorRec.mensajeAvisoProperty());
            monitorRec.tieneAlertasProperty().addListener((obs, ant, tiene) -> {
                if (Boolean.TRUE.equals(tiene)) {
                    lblRecordatorio.setStyle("-fx-font-size: 11px; -fx-padding: 3 8 3 8; -fx-background-radius: 10; "
                            + "-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-font-weight: bold;");
                } else {
                    lblRecordatorio.setStyle("-fx-font-size: 11px; -fx-padding: 3 8 3 8; -fx-background-radius: 10; "
                            + "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: bold;");
                }
            });
        }

        limpiarFormulario();
        cargarDatos();
    }


    private ListCell<Cliente> celdaCliente() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Cliente cliente, boolean empty) {
                super.updateItem(cliente, empty);
                if (empty || cliente == null) {
                    setText(null);
                } else {
                    setText(cliente.getId() + " - " + cliente.getNombres() + " " + cliente.getApellidos()
                            + (cliente.isActivo() ? "" : " (inactivo)"));
                }
            }
        };
    }

    /**
     * Recarga la lista de clientes en el ComboBox y las citas en la tabla de forma asíncrona
     * mediante JavaFX Task (RF03, RF09), evitando congelamientos al sincronizar entre pestañas.
     */
    @FXML
    public void cargarDatos() {
        Task<DatosCargaCitas> tarea = new Task<>() {
            @Override
            protected DatosCargaCitas call() throws Exception {
                ObservableList<Cliente> clientesActuales = clienteDAO.listarClientes();
                ObservableList<CitaTecnica> citasActuales = citaDAO.listarCitas();
                return new DatosCargaCitas(clientesActuales, citasActuales);
            }
        };

        tarea.setOnRunning(e -> {
            if (progresoCitas != null) {
                progresoCitas.setVisible(true);
                progresoCitas.setManaged(true);
            }
            if (lblEstadoCitas != null) {
                lblEstadoCitas.setText("Sincronizando datos...");
            }
        });

        tarea.setOnSucceeded(e -> {
            if (progresoCitas != null) {
                progresoCitas.setVisible(false);
                progresoCitas.setManaged(false);
            }
            if (lblEstadoCitas != null) {
                lblEstadoCitas.setText("Datos sincronizados");
            }
            DatosCargaCitas datos = tarea.getValue();
            cmbCliente.getItems().setAll(datos.clientes());
            citas.setAll(datos.citas());
            com.tecnoasistencia.concurrencia.MonitorRecordatorios.getInstance().actualizarCache(datos.citas());
            aplicarFiltros();
        });

        tarea.setOnFailed(e -> {
            if (progresoCitas != null) {
                progresoCitas.setVisible(false);
                progresoCitas.setManaged(false);
            }
            if (lblEstadoCitas != null) {
                lblEstadoCitas.setText("Error al sincronizar");
            }
            Throwable ex = tarea.getException();
            if (ex instanceof Exception exObj) {
                Alertas.errorDatos(exObj);
            }
        });

        AppThreadPool.execute(tarea);
    }

    private void cargarFormulario(CitaTecnica cita) {
        idSeleccionado = cita.getId();
        txtId.setText(String.valueOf(cita.getId()));

        cmbCliente.setValue(cmbCliente.getItems().stream()
                .filter(c -> c.getId() == cita.getClienteId())
                .findFirst()
                .orElse(null));

        dpFecha.setValue(cita.getFecha());
        txtHora.setText(cita.getHora().format(Validaciones.HORA));
        txtServicio.setText(cita.getTipoServicio());
        txtDescripcion.setText(cita.getDescripcion());
        cmbEstado.setValue(cita.getEstado());

        btnGuardar.setDisable(true);
        btnActualizar.setDisable(false);
        btnEliminar.setDisable(false);
    }

    private CitaTecnica leerFormulario() {
        Cliente cliente = cmbCliente.getValue();
        if (cliente == null) {
            throw new IllegalArgumentException("Debe seleccionar un cliente existente de la lista.");
        }

        // Regla de negocio: No agendar nuevas citas a clientes inactivos
        Validaciones.validarClienteParaCita(cliente, idSeleccionado != 0);

        if (dpFecha.getValue() == null) {
            throw new IllegalArgumentException("Seleccione la fecha de la cita.");
        }

        LocalTime hora = Validaciones.leerHora(txtHora.getText());

        if (cmbEstado.getValue() == null || !Validaciones.ESTADOS.contains(cmbEstado.getValue())) {
            throw new IllegalArgumentException("Seleccione un estado válido (" + String.join(", ", Validaciones.ESTADOS) + ").");
        }

        // Regla de negocio: No permitir fechas pasadas ni horas pasadas de hoy
        Validaciones.validarFechaHoraCita(dpFecha.getValue(), hora, idSeleccionado != 0, cmbEstado.getValue());

        String servicio = Validaciones.textoObligatorio(txtServicio.getText(), "El tipo de servicio", 3, 80);
        String descripcion = Validaciones.textoObligatorio(txtDescripcion.getText(), "La descripción", 5, 255);

        return new CitaTecnica(
                idSeleccionado,
                cliente.getId(),
                cliente.toString(),
                dpFecha.getValue(),
                hora,
                servicio,
                descripcion,
                cmbEstado.getValue()
        );
    }

    @FXML
    private void handleGuardar() {
        guardar(false);
    }

    @FXML
    private void handleActualizar() {
        guardar(true);
    }

    private void guardar(boolean actualizar) {
        try {
            if (actualizar && idSeleccionado == 0) {
                throw new IllegalArgumentException("Seleccione una cita técnica de la tabla para actualizar.");
            }

            CitaTecnica cita = leerFormulario();

            String accion = actualizar ? "actualizar" : "registrar";
            String mensajeConfirmacion = "¿Desea " + accion + " la cita para el cliente "
                    + cita.getNombreCliente() + " el día " + cita.getFechaFormateada()
                    + " a las " + cita.getHoraFormateada() + "?";

            if (!Alertas.confirmar(actualizar ? "Confirmar Actualización" : "Confirmar Registro", mensajeConfirmacion)) {
                return;
            }

            Task<Integer> tareaGuardar = new Task<>() {
                @Override
                protected Integer call() throws Exception {
                    // Código 1: Conflicto de horario
                    if (citaDAO.existeCitaCliente(cita.getClienteId(), cita.getFecha(), cita.getHora(), cita.getId())) {
                        return 1;
                    }
                    // Código 2: Éxito; Código 3: No completado
                    boolean exito = actualizar ? citaDAO.actualizarCita(cita) : citaDAO.insertarCita(cita);
                    return exito ? 2 : 3;
                }
            };

            AsyncUIHelper.vincularTareaAControles(tareaGuardar, progresoCitas, btnGuardar, btnActualizar, btnEliminar);
            if (lblEstadoCitas != null) lblEstadoCitas.setText("Persistiendo cita en segundo plano...");

            tareaGuardar.setOnSucceeded(e -> {
                AsyncUIHelper.desvincularControles(progresoCitas, btnGuardar, btnActualizar, btnEliminar);
                int codigo = tareaGuardar.getValue();
                if (codigo == 1) {
                    Alertas.mostrar(Alert.AlertType.WARNING, "Conflicto de Horario",
                            "El cliente '" + cita.getNombreCliente() + "' ya tiene una cita técnica agendada para el "
                                    + cita.getFechaFormateada() + " a las " + cita.getHoraFormateada()
                                    + ".\n\nPor favor asigne un horario o fecha diferente para evitar colisión de citas.");
                    btnGuardar.setDisable(actualizar);
                    btnActualizar.setDisable(!actualizar);
                    btnEliminar.setDisable(!actualizar);
                } else if (codigo == 2) {
                    Alertas.mostrar(Alert.AlertType.INFORMATION, "Operación Exitosa",
                            "Cita técnica " + (actualizar ? "actualizada" : "registrada") + " correctamente.");
                    limpiarFormulario();
                    cargarDatos();
                } else {
                    Alertas.mostrar(Alert.AlertType.WARNING, "Operación no completada",
                            "No se pudo completar la operación. El registro podría no existir en la base de datos.");
                    btnGuardar.setDisable(actualizar);
                    btnActualizar.setDisable(!actualizar);
                    btnEliminar.setDisable(!actualizar);
                }
            });

            tareaGuardar.setOnFailed(e -> {
                AsyncUIHelper.desvincularControles(progresoCitas, btnGuardar, btnActualizar, btnEliminar);
                btnGuardar.setDisable(actualizar);
                btnActualizar.setDisable(!actualizar);
                btnEliminar.setDisable(!actualizar);
                Throwable ex = tareaGuardar.getException();
                if (ex instanceof Exception exObj) {
                    Alertas.errorDatos(exObj);
                }
            });

            AppThreadPool.execute(tareaGuardar);

        } catch (IllegalArgumentException e) {
            Alertas.mostrar(Alert.AlertType.WARNING, "Validación de Formulario", e.getMessage());
        }
    }

    @FXML
    private void handleEliminar() {
        if (idSeleccionado == 0) {
            Alertas.mostrar(Alert.AlertType.WARNING, "Selección Requerida",
                    "Seleccione una cita técnica de la tabla para eliminarla.");
            return;
        }

        final int idABorrar = idSeleccionado;

        if (!Alertas.confirmar("Confirmar Eliminación",
                "¿Está seguro de eliminar permanentemente la cita técnica #" + idABorrar + "?")) {
            return;
        }

        Task<Boolean> tareaEliminar = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return citaDAO.eliminarCita(idABorrar);
            }
        };

        AsyncUIHelper.vincularTareaAControles(tareaEliminar, progresoCitas, btnGuardar, btnActualizar, btnEliminar);
        if (lblEstadoCitas != null) lblEstadoCitas.setText("Eliminando cita en segundo plano...");

        tareaEliminar.setOnSucceeded(e -> {
            AsyncUIHelper.desvincularControles(progresoCitas, btnGuardar, btnActualizar, btnEliminar);
            boolean eliminado = tareaEliminar.getValue();
            if (eliminado) {
                Alertas.mostrar(Alert.AlertType.INFORMATION, "Operación Exitosa",
                        "Cita técnica #" + idABorrar + " eliminada permanentemente.");
                limpiarFormulario();
                cargarDatos();
            } else {
                Alertas.mostrar(Alert.AlertType.WARNING, "Registro No Encontrado",
                        "La cita ya no existe en la base de datos. Se actualizará la lista.");
                cargarDatos();
            }
        });

        tareaEliminar.setOnFailed(e -> {
            AsyncUIHelper.desvincularControles(progresoCitas, btnGuardar, btnActualizar, btnEliminar);
            btnEliminar.setDisable(false);
            btnActualizar.setDisable(false);
            Throwable ex = tareaEliminar.getException();
            if (ex instanceof Exception exObj) {
                Alertas.errorDatos(exObj);
            }
        });

        AppThreadPool.execute(tareaEliminar);
    }

    @FXML
    public void limpiarFormulario() {
        tblCitas.getSelectionModel().clearSelection();
        idSeleccionado = 0;
        txtId.clear();
        cmbCliente.setValue(null);
        dpFecha.setValue(null);
        txtHora.clear();
        txtServicio.clear();
        txtDescripcion.clear();
        cmbEstado.setValue("Pendiente");

        btnGuardar.setDisable(false);
        btnActualizar.setDisable(true);
        btnEliminar.setDisable(true);
    }

    @FXML
    private void aplicarFiltros() {
        String estado = cmbFiltroEstado.getValue();
        LocalDate fecha = dpFiltroFecha.getValue();

        filtradas.setPredicate(cita -> {
            boolean coincideEstado = estado == null || "Todos".equals(estado) || estado.equalsIgnoreCase(cita.getEstado());
            boolean coincideFecha = fecha == null || fecha.equals(cita.getFecha());
            return coincideEstado && coincideFecha;
        });

        lblContador.setText("Citas mostradas: " + filtradas.size() + " de " + citas.size());
        actualizarMetricasAsync();
    }

    /**
     * Calcula de forma asíncrona en un hilo de AppThreadPool las métricas agregadas
     * sin bloquear la tabla ni los filtros.
     */
    public void actualizarMetricasAsync() {
        Task<com.tecnoasistencia.concurrencia.ServicioMetricas.ResumenEstadisticas> tareaMetricas =
                com.tecnoasistencia.concurrencia.ServicioMetricas.calcularMetricasAsync(citas, cmbCliente.getItems());

        tareaMetricas.setOnSucceeded(e -> {
            com.tecnoasistencia.concurrencia.ServicioMetricas.ResumenEstadisticas r = tareaMetricas.getValue();
            if (lblMetricaTotal != null) lblMetricaTotal.setText("Total: " + r.totalCitas());
            if (lblMetricaPendientes != null) lblMetricaPendientes.setText("Pendientes: " + r.pendientes());
            if (lblMetricaCompletadas != null) lblMetricaCompletadas.setText("Completadas: " + r.completadas());
            if (lblMetricaCanceladas != null) lblMetricaCanceladas.setText("Canceladas: " + r.canceladas());
            if (lblMetricaEfectividad != null) lblMetricaEfectividad.setText(String.format("Cumplimiento: %.1f%%", r.porcentajeCompletadas()));
        });

        AppThreadPool.execute(tareaMetricas);
    }

    @FXML
    private void handleLimpiarFiltros() {
        cmbFiltroEstado.setValue("Todos");
        dpFiltroFecha.setValue(null);
        aplicarFiltros();
    }
}