package com.tecnoasistencia.modelo;

import java.text.Normalizer;
import java.util.Arrays;

/**
 * Representa los estados permitidos para una Cita Técnica en el sistema TecnoAsistencia.
 * Contiene métodos de normalización y validación segura para evitar discrepancias por
 * mayúsculas, minúsculas, tildes o espacios accidentales.
 */
public enum EstadoCita {

    PENDIENTE("Pendiente"),
    EN_PROCESO("En proceso"),
    FINALIZADA("Finalizada"),
    CANCELADA("Cancelada");

    private final String descripcion;

    EstadoCita(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Retorna la descripción oficial tal y como se almacena en la base de datos.
     *
     * @return Cadena exacta ("Pendiente", "En proceso", "Finalizada", "Cancelada").
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Convierte de forma segura una cadena de texto en un EstadoCita, ignorando
     * mayúsculas, minúsculas, espacios iniciales/finales y acentos diacríticos.
     *
     * @param texto Cadena con el nombre o descripción del estado.
     * @return La constante EstadoCita correspondiente.
     * @throws IllegalArgumentException si el texto es nulo, vacío o no corresponde a ningún estado válido.
     */
    public static EstadoCita fromDescripcion(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new IllegalArgumentException("El estado de la cita no puede ser nulo ni vacío. " +
                    "Estados permitidos: " + Arrays.toString(valoresValidos()));
        }

        String normalizado = normalizar(texto);

        for (EstadoCita estado : values()) {
            if (normalizar(estado.descripcion).equals(normalizado) ||
                normalizar(estado.name()).equals(normalizado)) {
                return estado;
            }
        }

        throw new IllegalArgumentException("Estado no permitido: '" + texto + "'. " +
                "Los estados válidos son: " + Arrays.toString(valoresValidos()));
    }

    /**
     * Comprueba si una cadena representa un estado válido sin lanzar excepción.
     *
     * @param texto Cadena a comprobar.
     * @return true si es válido, false en caso contrario.
     */
    public static boolean esValido(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return false;
        }
        String normalizado = normalizar(texto);
        for (EstadoCita estado : values()) {
            if (normalizar(estado.descripcion).equals(normalizado) ||
                normalizar(estado.name()).equals(normalizado)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retorna un arreglo con las descripciones textuales válidas para la interfaz y mensajes de error.
     *
     * @return Arreglo de strings con los estados permitidos.
     */
    public static String[] valoresValidos() {
        EstadoCita[] valores = values();
        String[] nombres = new String[valores.length];
        for (int i = 0; i < valores.length; i++) {
            nombres[i] = valores[i].getDescripcion();
        }
        return nombres;
    }

    private static String normalizar(String input) {
        if (input == null) return "";
        String limpio = input.trim().toLowerCase();
        // Quitar acentos/tildes para evitar discrepancias
        limpio = Normalizer.normalize(limpio, Normalizer.Form.NFD)
                           .replaceAll("\\p{M}", "");
        // Reemplazar guiones bajos o espacios múltiples por un solo espacio
        return limpio.replace('_', ' ').replaceAll("\\s+", " ");
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
