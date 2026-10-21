package com.tecnoasistencia.utilidad;

import com.tecnoasistencia.modelo.Cliente;
import javafx.scene.control.TextFormatter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Utilidades de validación estricta y reglas de negocio para TecnoAsistencia Nicaragua S.A.
 * Previene ingresos maliciosos, inconsistencias temporales y datos corruptos (RF07, RF08).
 */
public final class Validaciones {

    public static final List<String> ESTADOS = List.of("Pendiente", "En proceso", "Finalizada", "Cancelada");

    public static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    // Correo RFC 5322 simplificado
    private static final Pattern CORREO = Pattern.compile(
            "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@"
                    + "(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$");

    // Solo letras (incluye vocales con tildes, diéresis, ñ y Ñ) y espacios simples entre palabras
    private static final Pattern SOLO_LETRAS = Pattern.compile(
            "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü]+(?:[\\s][A-Za-zÁÉÍÓÚáéíóúÑñÜü]+)*$");

    // Teléfono: prefijo internacional opcional (+), seguido de dígitos, guiones o espacios
    private static final Pattern TELEFONO_CARACTERES = Pattern.compile("^\\+?[0-9\\s\\-]+$");

    // Horario laboral de atención técnica de TecnoAsistencia: 07:00 a 19:00
    public static final LocalTime HORA_INICIO_LABORAL = LocalTime.of(7, 0);
    public static final LocalTime HORA_FIN_LABORAL = LocalTime.of(19, 0);

    private Validaciones() { }

    /**
     * Valida que un nombre o apellido contenga exclusivamente letras y espacios,
     * sin números ni símbolos raros, con longitud entre 2 y 80 caracteres.
     */
    public static String validarNombreOApellido(String texto, String nombreCampo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El campo '" + nombreCampo + "' es obligatorio.");
        }
        String limpio = texto.trim().replaceAll("\\s+", " ");
        if (limpio.length() < 2) {
            throw new IllegalArgumentException("El campo '" + nombreCampo + "' debe contener al menos 2 letras.");
        }
        if (limpio.length() > 80) {
            throw new IllegalArgumentException("El campo '" + nombreCampo + "' no puede exceder 80 caracteres.");
        }
        if (!SOLO_LETRAS.matcher(limpio).matches()) {
            throw new IllegalArgumentException("El campo '" + nombreCampo + "' solo debe contener letras y espacios válidos (no se permiten números ni símbolos).");
        }
        return limpio;
    }

    /**
     * Valida formato, longitud y caracteres de teléfono.
     * Prohíbe letras; permite prefijo '+' y requiere entre 8 y 20 caracteres con al menos 8 dígitos numéricos.
     */
    public static String validarTelefono(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El campo 'Teléfono' es obligatorio.");
        }
        String limpio = texto.trim();
        if (limpio.length() > 20) {
            throw new IllegalArgumentException("El teléfono no puede exceder 20 caracteres.");
        }
        if (!TELEFONO_CARACTERES.matcher(limpio).matches()) {
            throw new IllegalArgumentException("El teléfono solo puede contener números, espacios, guiones y opcionalmente el prefijo '+'. No se admiten letras.");
        }
        // Contar la cantidad de dígitos reales
        long digitos = limpio.chars().filter(Character::isDigit).count();
        if (digitos < 8) {
            throw new IllegalArgumentException("El número de teléfono debe contener al menos 8 dígitos numéricos (ej. +505 8888-1234).");
        }
        return limpio;
    }

    /**
     * Valida el formato de correo electrónico básico y longitud máxima de 120 caracteres.
     */
    public static boolean correoValido(String correo) {
        return correo != null && correo.trim().length() <= 120 && CORREO.matcher(correo.trim()).matches();
    }

    /**
     * Lee y valida una hora estricta en formato HH:mm (00:00 a 23:59).
     */
    public static LocalTime leerHora(String texto) {
        if (texto == null || !texto.trim().matches("^\\d{1,2}:\\d{2}$")) {
            throw new IllegalArgumentException("Escriba la hora con formato HH:mm. Ejemplo: 09:30 o 14:00.");
        }
        String horaTexto = texto.trim();
        // Si ingresan 9:30, normalizar a 09:30
        if (horaTexto.length() == 4) {
            horaTexto = "0" + horaTexto;
        }
        try {
            return LocalTime.parse(horaTexto, HORA);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("La hora debe ser un valor válido entre 00:00 y 23:59.");
        }
    }

    /**
     * Valida reglas temporales de negocio para agendar citas técnicas:
     * - No permite agendar citas en fechas pasadas.
     * - Si la cita es para hoy, la hora no puede haber pasado ya.
     * - Valida horario laboral razonable de atención técnica (07:00 a 19:00).
     */
    public static void validarFechaHoraCita(LocalDate fecha, LocalTime hora, boolean esEdicion, String estado) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de la cita es obligatoria.");
        }
        if (hora == null) {
            throw new IllegalArgumentException("La hora de la cita es obligatoria.");
        }

        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();

        // Si es una cita nueva o una cita en estado "Pendiente" que se está reprogramando:
        boolean requiereControlFuturo = !esEdicion || "Pendiente".equalsIgnoreCase(estado);

        if (requiereControlFuturo) {
            if (fecha.isBefore(hoy)) {
                throw new IllegalArgumentException("No se pueden agendar citas técnicas para fechas pasadas (fecha seleccionada: " + fecha + ").");
            }

            if (fecha.isEqual(hoy) && hora.isBefore(ahora)) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
                throw new IllegalArgumentException("No se puede agendar una cita para una hora que ya transcurrió hoy (hora indicada: "
                        + hora.format(fmt) + ", hora actual del sistema: " + ahora.format(fmt) + ").");
            }
        }

        // Restricción de horario de atención técnica (07:00 a 19:00)
        if (hora.isBefore(HORA_INICIO_LABORAL) || hora.isAfter(HORA_FIN_LABORAL)) {
            throw new IllegalArgumentException("El horario de atención técnica es de 07:00 a 19:00. La hora "
                    + hora.format(DateTimeFormatter.ofPattern("HH:mm")) + " está fuera del horario laboral.");
        }
    }

    /**
     * Valida que no se agenden citas nuevas a clientes con estado inactivo.
     */
    public static void validarClienteParaCita(Cliente cliente, boolean esEdicion) {
        if (cliente == null) {
            throw new IllegalArgumentException("Debe seleccionar un cliente registrado.");
        }
        if (!esEdicion && !cliente.isActivo()) {
            throw new IllegalArgumentException("No se puede agendar una nueva cita técnica al cliente '"
                    + cliente.getNombres() + " " + cliente.getApellidos()
                    + "' porque su estado actual es INACTIVO. Active el cliente primero en el módulo de Clientes.");
        }
    }

    /**
     * Valida texto obligatorio no vacío y longitud máxima.
     */
    public static String textoObligatorio(String texto, String campo, int minimo, int maximo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(campo + " es obligatorio.");
        }
        String valor = texto.trim();
        if (valor.length() < minimo) {
            throw new IllegalArgumentException(campo + " debe contener al menos " + minimo + " caracteres.");
        }
        if (valor.length() > maximo) {
            throw new IllegalArgumentException(campo + " admite hasta " + maximo + " caracteres.");
        }
        return valor;
    }

    public static String textoObligatorio(String texto, String campo, int maximo) {
        return textoObligatorio(texto, campo, 1, maximo);
    }

    // =========================================================================
    // FACTORÍAS DE FILTROS EN TIEMPO REAL (TextFormatter) PARA JavaFX
    // =========================================================================

    /**
     * Filtro para TextField que solo permite letras y espacios, limitando la longitud máxima.
     */
    public static TextFormatter<String> crearFiltroSoloLetras(int maxLongitud) {
        UnaryOperator<TextFormatter.Change> filtro = cambio -> {
            String nuevoTexto = cambio.getControlNewText();
            if (nuevoTexto.length() > maxLongitud) {
                return null;
            }
            if (nuevoTexto.isEmpty() || nuevoTexto.matches("^[A-Za-zÁÉÍÓÚáéíóúÑñÜü\\s]*$")) {
                return cambio;
            }
            return null;
        };
        return new TextFormatter<>(filtro);
    }

    /**
     * Filtro para TextField de teléfono: solo dígitos, espacios, guiones y un '+' opcional al inicio.
     */
    public static TextFormatter<String> crearFiltroTelefono(int maxLongitud) {
        UnaryOperator<TextFormatter.Change> filtro = cambio -> {
            String nuevoTexto = cambio.getControlNewText();
            if (nuevoTexto.length() > maxLongitud) {
                return null;
            }
            if (nuevoTexto.isEmpty() || nuevoTexto.matches("^\\+?[0-9\\s\\-]*$")) {
                return cambio;
            }
            return null;
        };
        return new TextFormatter<>(filtro);
    }

    /**
     * Filtro para limitar longitud general en TextFields.
     */
    public static TextFormatter<String> crearFiltroLongitud(int maxLongitud) {
        UnaryOperator<TextFormatter.Change> filtro = cambio -> {
            if (cambio.getControlNewText().length() > maxLongitud) {
                return null;
            }
            return cambio;
        };
        return new TextFormatter<>(filtro);
    }
}