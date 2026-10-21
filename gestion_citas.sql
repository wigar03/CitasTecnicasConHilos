-- =============================================================================
-- EVALUACIÓN PRÁCTICA SUMATIVA: Sistema de gestión de citas de atención técnica
-- TecnoAsistencia Nicaragua S.A. — JavaFX y JDBC
-- Archivo: gestion_citas.sql
-- Base de datos: gestion_citas
-- Motor: PostgreSQL
-- =============================================================================

-- 1. CREACIÓN DE LA BASE DE DATOS (si no existe)
-- En psql / terminal: Si se ejecuta conectado a la base 'postgres' por defecto:
SELECT 'CREATE DATABASE gestion_citas'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'gestion_citas')\gexec

-- Conectarse a la base de datos gestion_citas (para psql):
\c gestion_citas;

-- =============================================================================
-- 2. TABLAS Y RELACIONES (MODELO OBLIGATORIO)
-- Relación: cliente (1) — (N) cita_tecnica
-- =============================================================================

-- Tabla: cliente
CREATE TABLE IF NOT EXISTS cliente (
    id SERIAL PRIMARY KEY,
    nombres VARCHAR(80) NOT NULL,
    apellidos VARCHAR(80) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    correo VARCHAR(120) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- Tabla: cita_tecnica
CREATE TABLE IF NOT EXISTS cita_tecnica (
    id SERIAL PRIMARY KEY,
    cliente_id INT NOT NULL,
    fecha DATE NOT NULL,
    hora TIME NOT NULL,
    tipo_servicio VARCHAR(80) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    CONSTRAINT fk_cita_cliente FOREIGN KEY (cliente_id) 
        REFERENCES cliente(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_estado_cita CHECK (estado IN ('Pendiente', 'En proceso', 'Finalizada', 'Cancelada'))
);

-- =============================================================================
-- 3. ÍNDICES DE RENDIMIENTO (Búsquedas y Filtros RF05, RF06)
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_cliente_correo ON cliente(correo);
CREATE INDEX IF NOT EXISTS idx_cliente_nombres ON cliente(LOWER(nombres));
CREATE INDEX IF NOT EXISTS idx_cliente_apellidos ON cliente(LOWER(apellidos));
CREATE INDEX IF NOT EXISTS idx_cita_cliente_id ON cita_tecnica(cliente_id);
CREATE INDEX IF NOT EXISTS idx_cita_fecha ON cita_tecnica(fecha);
CREATE INDEX IF NOT EXISTS idx_cita_estado ON cita_tecnica(estado);

-- =============================================================================
-- 4. DATOS DE PRUEBA INICIALES (Para comprobación de aceptación)
-- =============================================================================
INSERT INTO cliente (nombres, apellidos, telefono, correo, activo) VALUES
('Carlos Alberto', 'Mendoza Rivas', '+505 8812-3456', 'carlos.mendoza@email.com', TRUE),
('María Fernanda', 'López Salazar', '+505 8765-4321', 'maria.lopez@email.com', TRUE),
('Roberto José', 'Guillén Zamora', '+505 8432-1098', 'roberto.guillen@email.com', FALSE)
ON CONFLICT (correo) DO NOTHING;

INSERT INTO cita_tecnica (cliente_id, fecha, hora, tipo_servicio, descripcion, estado)
SELECT c.id, CURRENT_DATE + INTERVAL '1 day', '09:00:00', 'Mantenimiento preventivo', 'Limpieza interna y cambio de pasta térmica en laptop Dell.', 'Pendiente'
FROM cliente c WHERE c.correo = 'carlos.mendoza@email.com'
  AND NOT EXISTS (SELECT 1 FROM cita_tecnica ct WHERE ct.cliente_id = c.id AND ct.tipo_servicio = 'Mantenimiento preventivo');

INSERT INTO cita_tecnica (cliente_id, fecha, hora, tipo_servicio, descripcion, estado)
SELECT c.id, CURRENT_DATE + INTERVAL '2 day', '14:30:00', 'Reparación de hardware', 'Reemplazo de teclado dañado y actualización de memoria RAM.', 'En proceso'
FROM cliente c WHERE c.correo = 'carlos.mendoza@email.com'
  AND NOT EXISTS (SELECT 1 FROM cita_tecnica ct WHERE ct.cliente_id = c.id AND ct.tipo_servicio = 'Reparación de hardware');

INSERT INTO cita_tecnica (cliente_id, fecha, hora, tipo_servicio, descripcion, estado)
SELECT c.id, CURRENT_DATE, '10:15:00', 'Diagnóstico general', 'Equipo de escritorio no enciende ni emite señales sonoras.', 'Finalizada'
FROM cliente c WHERE c.correo = 'maria.lopez@email.com'
  AND NOT EXISTS (SELECT 1 FROM cita_tecnica ct WHERE ct.cliente_id = c.id AND ct.tipo_servicio = 'Diagnóstico general');
