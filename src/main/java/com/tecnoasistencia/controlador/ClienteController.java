package com.tecnoasistencia.controlador;

import com.tecnoasistencia.dao.ClienteDAO;
import com.tecnoasistencia.modelo.Cliente;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import com.tecnoasistencia.utilidad.Alertas;
import com.tecnoasistencia.utilidad.Validaciones;

/**
 * Controlador de la vista de clientes (cliente-view.fxml).
 * Configura diseño visual y animaciones con JavaFX nativo puro (sin CSS externo).
 */
public class ClienteController implements Initializable {


    @FXML
    private BorderPane rootPane;

    @FXML
    private VBox headerBox;

    @FXML
    private Label lblEmpresa;

    @FXML
    private Label lblTitulo;

    @FXML
    private VBox panelFormulario;

    @FXML
    private Label lblFormTitulo;

    @FXML
    private Label lblFormSubtitulo;

    @FXML
    private Label lblId;

    @FXML
    private Label lblNombres;

    @FXML
    private Label lblApellidos;

    @FXML
    private Label lblTelefono;

    @FXML
    private Label lblCorreo;

    @FXML
    private Label lblEstado;

    @FXML
    private TextField txtId;

    @FXML
    private TextField txtNombres;

    @FXML
    private TextField txtApellidos;

    @FXML
    private TextField txtTelefono;

    @FXML
    private TextField txtCorreo;

    @FXML
    private CheckBox chkActivo;

    @FXML
    private Button btnGuardar;

    @FXML
    private Button btnActualizar;

    @FXML
    private Button btnEliminar;

    @FXML
    private Button btnLimpiar;

    @FXML
    private VBox panelTabla;

    @FXML
    private Label lblBuscar;

    @FXML
    private TextField txtBuscar;

    @FXML
    private Button btnBuscar;

    @FXML
    private Button btnLimpiarBusqueda;

    @FXML
    private TableView<Cliente> tblClientes;

    @FXML
    private TableColumn<Cliente, Integer> colId;

    @FXML
    private TableColumn<Cliente, String> colNombres;

    @FXML
    private TableColumn<Cliente, String> colApellidos;

    @FXML
    private TableColumn<Cliente, String> colTelefono;

    @FXML
    private TableColumn<Cliente, String> colCorreo;

    @FXML
    private TableColumn<Cliente, Boolean> colActivo;

    @FXML
    private Label lblContador;

    private ClienteDAO clienteDAO;
    private final ObservableList<Cliente> listaClientes = FXCollections.observableArrayList();
    private final FilteredList<Cliente> clientesFiltrados = new FilteredList<>(listaClientes, c -> true);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clienteDAO = new ClienteDAO();

        configurarDisenoVisualJavaFX();
        configurarAnimacionesJavaFX();
        configurarColumnasTabla();
        configurarSeleccionTabla();
        configurarFiltrosEntrada();
        configurarBusqueda();

        SortedList<Cliente> clientesOrdenados = new SortedList<>(clientesFiltrados);
        clientesOrdenados.comparatorProperty().bind(tblClientes.comparatorProperty());
        tblClientes.setItems(clientesOrdenados);
        tblClientes.setPlaceholder(new Label("No se encontraron clientes registrados o que coincidan con la búsqueda."));

        cargarClientes();
    }

    /**
     * Aplica filtros de entrada en tiempo real (TextFormatter) para evitar que el usuario
     * ingrese números en nombres o letras en teléfonos, y bloquea desbordamiento de longitud.
     */
    private void configurarFiltrosEntrada() {
        txtNombres.setTextFormatter(Validaciones.crearFiltroSoloLetras(80));
        txtApellidos.setTextFormatter(Validaciones.crearFiltroSoloLetras(80));
        txtTelefono.setTextFormatter(Validaciones.crearFiltroTelefono(20));
        txtCorreo.setTextFormatter(Validaciones.crearFiltroLongitud(120));
        txtBuscar.setTextFormatter(Validaciones.crearFiltroLongitud(80));
    }

    /**
     * Configura el enlace de datos (CellValueFactory) para cada columna del TableView
     * y aplica formato con componentes nativos de JavaFX.
     */
    private void configurarColumnasTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombres.setCellValueFactory(new PropertyValueFactory<>("nombres"));
        colApellidos.setCellValueFactory(new PropertyValueFactory<>("apellidos"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));

        // Renderizado visual con etiquetas nativas de JavaFX
        colActivo.setCellFactory(column -> new TableCell<Cliente, Boolean>() {
            @Override
            protected void updateItem(Boolean activo, boolean empty) {
                super.updateItem(activo, empty);
                if (empty || activo == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(activo ? "Activo" : "Inactivo");
                    badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                    badge.setPadding(new Insets(3, 10, 3, 10));

                    Color colorTexto = activo ? Color.web("#15803d") : Color.web("#b91c1c");
                    Color colorFondo = activo ? Color.web("#dcfce7") : Color.web("#fee2e2");

                    badge.setTextFill(colorTexto);
                    badge.setBackground(new Background(new BackgroundFill(colorFondo, new CornerRadii(12), Insets.EMPTY)));

                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    /**
     * Carga todos los clientes desde la base de datos a través de ClienteDAO
     * y actualiza el TableView y el contador (RF02, RF09) con animación FadeTransition.
     */
    public void cargarClientes() {
        try {
            ObservableList<Cliente> desdeDB = clienteDAO.listarClientes();
            listaClientes.setAll(desdeDB);
            clientesFiltrados.setPredicate(c -> true);
            actualizarContador(clientesFiltrados.size());
            animarCargaTabla();
        } catch (IllegalStateException e) {
            Alertas.errorDatos(e);
        }
    }

    /**
     * Animación de desvanecimiento suave (JavaFX FadeTransition) al refrescar el TableView.
     */
    private void animarCargaTabla() {
        if (tblClientes == null) return;
        FadeTransition ft = new FadeTransition(Duration.millis(300), tblClientes);
        ft.setFromValue(0.4);
        ft.setToValue(1.0);
        ft.playFromStart();
    }

    /**
     * Actualiza el contador de clientes en la interfaz.
     */
    private void actualizarContador(int total) {
        if (lblContador != null) {
            lblContador.setText("Total de clientes: " + total);
        }
    }

    /**
     * Aplica el diseño visual programáticamente usando las clases nativas de JavaFX
     * (Background, Border, Color, Font, DropShadow), sin recurrir a CSS externo.
     */
    private void configurarDisenoVisualJavaFX() {
        if (rootPane != null) {
            rootPane.setBackground(new Background(new BackgroundFill(Color.web("#f8fafc"), CornerRadii.EMPTY, Insets.EMPTY)));
        }

        if (headerBox != null) {
            LinearGradient gradienteHeader = new LinearGradient(
                    0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#0f172a")),
                    new Stop(1, Color.web("#1e293b"))
            );
            headerBox.setBackground(new Background(new BackgroundFill(gradienteHeader, CornerRadii.EMPTY, Insets.EMPTY)));
        }

        if (lblEmpresa != null) {
            lblEmpresa.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            lblEmpresa.setTextFill(Color.web("#94a3b8"));
        }
        if (lblTitulo != null) {
            lblTitulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
            lblTitulo.setTextFill(Color.WHITE);
        }

        DropShadow sombraCard = new DropShadow();
        sombraCard.setColor(Color.rgb(0, 0, 0, 0.06));
        sombraCard.setRadius(12);
        sombraCard.setOffsetY(3);

        CornerRadii radioCard = new CornerRadii(10);
        Background fondoCard = new Background(new BackgroundFill(Color.WHITE, radioCard, Insets.EMPTY));
        Border bordeCard = new Border(new BorderStroke(Color.web("#e2e8f0"), BorderStrokeStyle.SOLID, radioCard, new BorderWidths(1)));

        if (panelFormulario != null) {
            panelFormulario.setBackground(fondoCard);
            panelFormulario.setBorder(bordeCard);
            panelFormulario.setEffect(sombraCard);
        }

        if (panelTabla != null) {
            panelTabla.setBackground(fondoCard);
            panelTabla.setBorder(bordeCard);
            panelTabla.setEffect(sombraCard);
        }

        if (lblFormTitulo != null) {
            lblFormTitulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            lblFormTitulo.setTextFill(Color.web("#1e293b"));
        }
        if (lblFormSubtitulo != null) {
            lblFormSubtitulo.setFont(Font.font("Segoe UI", 12));
            lblFormSubtitulo.setTextFill(Color.web("#64748b"));
        }
        if (lblBuscar != null) {
            lblBuscar.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            lblBuscar.setTextFill(Color.web("#334155"));
        }
        if (lblContador != null) {
            lblContador.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            lblContador.setTextFill(Color.web("#64748b"));
        }

        estilarBoton(btnGuardar, "#2563eb", "#1d4ed8");
        estilarBoton(btnActualizar, "#f59e0b", "#d97706");
        estilarBoton(btnEliminar, "#ef4444", "#dc2626");
        estilarBoton(btnLimpiar, "#64748b", "#475569");
        estilarBoton(btnBuscar, "#0284c7", "#0369a1");
        estilarBoton(btnLimpiarBusqueda, "#64748b", "#475569");

        estilarCampoTexto(txtNombres);
        estilarCampoTexto(txtApellidos);
        estilarCampoTexto(txtTelefono);
        estilarCampoTexto(txtCorreo);
        estilarCampoTexto(txtBuscar);

        if (txtId != null) {
            txtId.setBackground(new Background(new BackgroundFill(Color.web("#f1f5f9"), new CornerRadii(5), Insets.EMPTY)));
            txtId.setBorder(new Border(new BorderStroke(Color.web("#e2e8f0"), BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(1))));
        }
    }

    private void estilarBoton(Button boton, String colorHex, String colorHoverHex) {
        if (boton == null) return;
        CornerRadii radii = new CornerRadii(6);
        boton.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        boton.setTextFill(Color.WHITE);
        boton.setCursor(Cursor.HAND);
        boton.setPadding(new Insets(8, 16, 8, 16));

        Background normalBg = new Background(new BackgroundFill(Color.web(colorHex), radii, Insets.EMPTY));
        Background hoverBg = new Background(new BackgroundFill(Color.web(colorHoverHex), radii, Insets.EMPTY));
        boton.setBackground(normalBg);

        boton.hoverProperty().addListener((obs, oldVal, isHover) -> {
            if (!boton.isDisabled()) {
                boton.setBackground(isHover ? hoverBg : normalBg);
            }
        });
    }

    private void estilarCampoTexto(TextField tf) {
        if (tf == null) return;
        CornerRadii radii = new CornerRadii(5);
        Border normalBorder = new Border(new BorderStroke(Color.web("#cbd5e1"), BorderStrokeStyle.SOLID, radii, new BorderWidths(1)));
        Border focusBorder = new Border(new BorderStroke(Color.web("#2563eb"), BorderStrokeStyle.SOLID, radii, new BorderWidths(1.5)));
        Background bg = new Background(new BackgroundFill(Color.WHITE, radii, Insets.EMPTY));

        tf.setBackground(bg);
        tf.setBorder(normalBorder);
        tf.setPadding(new Insets(7, 10, 7, 10));

        tf.focusedProperty().addListener((obs, oldVal, isFocused) -> {
            tf.setBorder(isFocused ? focusBorder : normalBorder);
        });
    }

    private void configurarAnimacionesJavaFX() {
        agregarAnimacionHover(btnGuardar);
        agregarAnimacionHover(btnActualizar);
        agregarAnimacionHover(btnEliminar);
        agregarAnimacionHover(btnLimpiar);
        agregarAnimacionHover(btnBuscar);
        agregarAnimacionHover(btnLimpiarBusqueda);
    }

    private void agregarAnimacionHover(Button boton) {
        if (boton == null) return;
        ScaleTransition stIn = new ScaleTransition(Duration.millis(120), boton);
        stIn.setToX(1.04);
        stIn.setToY(1.04);

        ScaleTransition stOut = new ScaleTransition(Duration.millis(120), boton);
        stOut.setToX(1.0);
        stOut.setToY(1.0);

        boton.setOnMouseEntered(e -> {
            if (!boton.isDisabled()) {
                stIn.playFromStart();
            }
        });
        boton.setOnMouseExited(e -> {
            stOut.playFromStart();
        });
    }

    /**
     * Animación de sacudida (JavaFX TranslateTransition) cuando un campo falla la validación.
     */
    private void animarError(Node nodo) {
        if (nodo == null) return;
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), nodo);
        shake.setByX(8);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> nodo.setTranslateX(0));
        shake.playFromStart();
    }

    /**
     * Animación sutil de realce (JavaFX FadeTransition) al seleccionar un registro.
     */
    private void animarSeleccionFormulario() {
        if (panelFormulario == null) return;
        FadeTransition ft = new FadeTransition(Duration.millis(200), panelFormulario);
        ft.setFromValue(0.7);
        ft.setToValue(1.0);
        ft.playFromStart();
    }

    /**
     * Configura el listener de selección de la tabla para poblar el formulario (RF02).
     */
    private void configurarSeleccionTabla() {
        tblClientes.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, nuevoCliente) -> {
            if (nuevoCliente != null) {
                cargarClienteEnFormulario(nuevoCliente);
                animarSeleccionFormulario();
            }
        });
    }

    /**
     * Carga los datos del cliente seleccionado en los campos del formulario.
     */
    private void cargarClienteEnFormulario(Cliente cliente) {
        txtId.setText(String.valueOf(cliente.getId()));
        txtNombres.setText(cliente.getNombres());
        txtApellidos.setText(cliente.getApellidos());
        txtTelefono.setText(cliente.getTelefono());
        txtCorreo.setText(cliente.getCorreo());
        chkActivo.setSelected(cliente.isActivo());

        btnGuardar.setDisable(true);
        btnActualizar.setDisable(false);
        btnEliminar.setDisable(false);
    }

    /**
     * Registra un nuevo cliente validando campos obligatorios y formato de correo (RF02, RF07).
     */
    @FXML
    private void handleGuardar(ActionEvent event) {
        if (!validarFormulario(false)) {
            return;
        }

        String nombres = Validaciones.validarNombreOApellido(txtNombres.getText(), "Nombres");
        String apellidos = Validaciones.validarNombreOApellido(txtApellidos.getText(), "Apellidos");
        String telefono = Validaciones.validarTelefono(txtTelefono.getText());
        String correo = txtCorreo.getText().trim();
        boolean activo = chkActivo.isSelected();

        Cliente nuevoCliente = new Cliente(nombres, apellidos, telefono, correo, activo);
        boolean insertado = clienteDAO.insertarCliente(nuevoCliente);

        if (insertado) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Operación Exitosa", "Cliente registrado exitosamente.");
            txtBuscar.clear();
            limpiarFormulario();
            cargarClientes();
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de Inserción", "No se pudo registrar el cliente. Verifique la conexión a la base de datos.");
        }
    }

    /**
     * Actualiza la información de un cliente existente (RF02, RF07).
     */
    @FXML
    private void handleActualizar(ActionEvent event) {
        if (txtId.getText() == null || txtId.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Selección Requerida", "Debe seleccionar un cliente de la tabla para modificarlo.");
            return;
        }

        if (!validarFormulario(true)) {
            return;
        }

        int id = Integer.parseInt(txtId.getText().trim());
        String nombres = Validaciones.validarNombreOApellido(txtNombres.getText(), "Nombres");
        String apellidos = Validaciones.validarNombreOApellido(txtApellidos.getText(), "Apellidos");
        String telefono = Validaciones.validarTelefono(txtTelefono.getText());
        String correo = txtCorreo.getText().trim();
        boolean activo = chkActivo.isSelected();

        Cliente clienteModificado = new Cliente(id, nombres, apellidos, telefono, correo, activo);
        boolean actualizado = clienteDAO.actualizarCliente(clienteModificado);

        if (actualizado) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Operación Exitosa", "Cliente actualizado exitosamente.");
            txtBuscar.clear();
            limpiarFormulario();
            cargarClientes();
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de Actualización", "No se pudo actualizar el cliente. Verifique la conexión a la base de datos.");
        }
    }

    /**
     * Limpia los campos del formulario y restablece el estado de los botones.
     */
    @FXML
    private void handleLimpiar(ActionEvent event) {
        limpiarFormulario();
    }

    private void limpiarFormulario() {
        txtId.clear();
        txtNombres.clear();
        txtApellidos.clear();
        txtTelefono.clear();
        txtCorreo.clear();
        chkActivo.setSelected(true);

        tblClientes.getSelectionModel().clearSelection();

        btnGuardar.setDisable(false);
        btnActualizar.setDisable(true);
        btnEliminar.setDisable(true);
    }

    /**
     * Valida los campos obligatorios, longitudes máximas y formato de correo electrónico (RF07).
     * Aplica animación de sacudida (JavaFX shake) en el campo que falle.
     */
    private boolean validarFormulario(boolean esEdicion) {
        try {
            try {
                Validaciones.validarNombreOApellido(txtNombres.getText(), "Nombres");
            } catch (IllegalArgumentException e) {
                animarError(txtNombres);
                mostrarAlerta(Alert.AlertType.WARNING, "Validación de Nombres", e.getMessage());
                txtNombres.requestFocus();
                return false;
            }

            try {
                Validaciones.validarNombreOApellido(txtApellidos.getText(), "Apellidos");
            } catch (IllegalArgumentException e) {
                animarError(txtApellidos);
                mostrarAlerta(Alert.AlertType.WARNING, "Validación de Apellidos", e.getMessage());
                txtApellidos.requestFocus();
                return false;
            }

            try {
                Validaciones.validarTelefono(txtTelefono.getText());
            } catch (IllegalArgumentException e) {
                animarError(txtTelefono);
                mostrarAlerta(Alert.AlertType.WARNING, "Validación de Teléfono", e.getMessage());
                txtTelefono.requestFocus();
                return false;
            }

            String correo = txtCorreo.getText() != null ? txtCorreo.getText().trim() : "";
            if (correo.isEmpty()) {
                animarError(txtCorreo);
                mostrarAlerta(Alert.AlertType.WARNING, "Campo Obligatorio", "El campo 'Correo' es obligatorio.");
                txtCorreo.requestFocus();
                return false;
            }
            if (correo.length() > 120) {
                animarError(txtCorreo);
                mostrarAlerta(Alert.AlertType.WARNING, "Longitud Excedida", "El campo 'Correo' no puede exceder 120 caracteres.");
                txtCorreo.requestFocus();
                return false;
            }
            if (!Validaciones.correoValido(correo)) {
                animarError(txtCorreo);
                mostrarAlerta(Alert.AlertType.WARNING, "Formato Inválido", "El formato del correo electrónico no es válido. Ejemplo: usuario@dominio.com");
                txtCorreo.requestFocus();
                return false;
            }

            int idExcluir = esEdicion ? Integer.parseInt(txtId.getText().trim()) : 0;
            if (clienteDAO.existeCorreo(correo, idExcluir)) {
                animarError(txtCorreo);
                mostrarAlerta(Alert.AlertType.WARNING, "Correo Duplicado", "Ya existe un cliente registrado con el correo: " + correo);
                txtCorreo.requestFocus();
                return false;
            }

            return true;
        } catch (IllegalStateException e) {
            Alertas.errorDatos(e);
            return false;
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alertas.mostrar(tipo, titulo, mensaje);
    }

    /**
     * Elimina físicamente un cliente tras confirmación obligatoria (RF02, RF08).
     * Previene la eliminación si el cliente tiene citas técnicas asociadas e informa el motivo.
     */
    @FXML
    private void handleEliminar(ActionEvent event) {
        try {
            if (txtId.getText() == null || txtId.getText().trim().isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Selección Requerida", "Debe seleccionar un cliente de la tabla para eliminarlo.");
                return;
            }

            int id = Integer.parseInt(txtId.getText().trim());
            String nombreCompleto = txtNombres.getText().trim() + " " + txtApellidos.getText().trim();

            // Solicitar confirmación antes de eliminar (RF08)
            Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacion.setTitle("Confirmar Eliminación");
            confirmacion.setHeaderText("¿Está seguro de eliminar al cliente?");
            confirmacion.setContentText("Esta acción eliminará permanentemente al cliente '" + nombreCompleto + "'. ¿Desea continuar?");

            Optional<ButtonType> respuesta = confirmacion.showAndWait();
            if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) {
                return;
            }

            // Restricción: No se permite eliminar un cliente que tenga citas técnicas asociadas (RF08)
            if (clienteDAO.tieneCitasAsociadas(id)) {
                mostrarAlerta(Alert.AlertType.WARNING, "Eliminación Restringida",
                        "No se puede eliminar el cliente '" + nombreCompleto + "' porque tiene citas técnicas registradas en el sistema.\n\n"
                                + "Para proceder con la eliminación física del cliente, primero debe eliminar las citas técnicas asociadas; cancelarlas no elimina la relación.");
                return;
            }

            boolean eliminado = clienteDAO.eliminarCliente(id);
            if (eliminado) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Operación Exitosa", "Cliente eliminado exitosamente.");
                txtBuscar.clear();
                limpiarFormulario();
                cargarClientes();
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Error de Eliminación", "No se pudo eliminar el cliente. Ocurrió un error en la base de datos.");
            }
        } catch (IllegalStateException e) {
            Alertas.errorDatos(e);
        }
    }

    /**
     * Configura el listener dinámico en el campo de texto para filtrar en tiempo real en memoria (RF05).
     * Evita saturar la conexión a PostgreSQL con una consulta por cada tecla.
     */
    private void configurarBusqueda() {
        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            aplicarFiltroMemoria(newValue);
        });
    }

    /**
     * Aplica el predicado de búsqueda reactiva en memoria sobre FilteredList.
     */
    private void aplicarFiltroMemoria(String criterio) {
        if (criterio == null || criterio.isBlank()) {
            clientesFiltrados.setPredicate(c -> true);
        } else {
            String c = criterio.trim().toLowerCase();
            clientesFiltrados.setPredicate(cliente ->
                    (cliente.getNombres() != null && cliente.getNombres().toLowerCase().contains(c))
                    || (cliente.getApellidos() != null && cliente.getApellidos().toLowerCase().contains(c))
                    || (cliente.getCorreo() != null && cliente.getCorreo().toLowerCase().contains(c))
            );
        }
        actualizarContador(clientesFiltrados.size());
    }

    /**
     * Manejador del botón Buscar (o pulsar Enter en txtBuscar) (RF05).
     * Ejecuta la consulta directa con PreparedStatement contra PostgreSQL.
     */
    @FXML
    private void handleBuscar(ActionEvent event) {
        String criterio = txtBuscar.getText() != null ? txtBuscar.getText().trim() : "";
        if (criterio.isEmpty()) {
            cargarClientes();
        } else {
            try {
                ObservableList<Cliente> resultadoDB = clienteDAO.buscarClientes(criterio);
                listaClientes.setAll(resultadoDB);
                clientesFiltrados.setPredicate(c -> true);
                actualizarContador(resultadoDB.size());
                animarCargaTabla();
            } catch (IllegalStateException e) {
                Alertas.errorDatos(e);
            }
        }
    }

    /**
     * Manejador del botón Restablecer/Limpiar búsqueda (RF05, RF06).
     */
    @FXML
    private void handleLimpiarBusqueda(ActionEvent event) {
        txtBuscar.clear();
        cargarClientes();
    }
}