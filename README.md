# Evaluacion_JavaFX_CitasTecnicas
### Sistema de Gestión de Citas de Atención Técnica — JavaFX y JDBC
**TecnoAsistencia Nicaragua S.A.** | Universidad Americana (UAM) — Ingeniería de Sistemas  
**Asignatura:** Programación de Aplicaciones de Escritorio | **Unidad II:** Acceso a bases de datos

---

## 1. Integrantes del Equipo y Distribución de Responsabilidades

| Nombre Completo | Usuario GitHub | Rol | Distribución de Responsabilidades |
| :--- | :--- | :---: | :--- |
| **Andrés Sebastián González Maradiaga** | [@g-nzaan](https://github.com/g-nzaan) | Integrante 1 | Conexión JDBC centralizada (`ConexionDB`), modelo `Cliente`, operaciones CRUD en `ClienteDAO`, verificación fija con `Statement` (`SELECT 1`) y validación de eliminación. |
| **William Antonio García García** | [@Wigar03](https://github.com/Wigar03) | Integrante 2 | Interfaz gráfica FXML y `TableView` de Clientes (`cliente-view.fxml`, `ClienteController`), enlace de datos con `ClienteDAO`, formulario CRUD, validaciones visuales y búsqueda dinámica insensible a mayúsculas/minúsculas (RF02, RF05, RF08, RF09). |
| **Caleb Jordan Tardencilla Alvarado** | [@CTardencilla](https://github.com/CTardencilla) | Integrante 3 | Modelo `CitaTecnica`, DAO de Citas Técnicas (`CitaTecnicaDAO`), integridad referencial con clave foránea hacia `cliente`, restricciones de estados y persistencia JDBC (RF03, RF10). |
| **Rafael Hernández Sánchez** | [@rafaelhs07](https://github.com/rafaelhs07) | Integrante 4 | Interfaz gráfica FXML de Citas Técnicas (`cita-view.fxml`, `CitaTecnicaController`), ComboBox de clientes, filtros por estado y fecha, validaciones y documentación del repositorio (RF03, RF04, RF06, RF07). |

---

## 2. Requisitos del Entorno

* **Java Development Kit (JDK):** Versión 21 o superior.
* **Apache Maven:** Versión 3.8+ (o Maven Wrapper integrado).
* **Motor de Base de Datos:** PostgreSQL 14, 15, 16 o superior.
* **Entorno de Desarrollo Recomendado:** IntelliJ IDEA 2023 / 2024.

---

## 3. Configuración de PostgreSQL y Base de Datos

El sistema utiliza la base de datos relacional `gestion_citas` bajo el motor PostgreSQL en el puerto por defecto `5432`.

### 3.1. Ejecución del Script Oficial `gestion_citas.sql`

En la raíz del proyecto se encuentra el archivo [`gestion_citas.sql`](gestion_citas.sql), el cual crea la base de datos (si no existe), las tablas relacionales con sus restricciones de integridad, índices para optimización de consultas y datos iniciales de prueba.

Ejecútelo desde la terminal mediante `psql`:

```bash
# Ejecutar con el usuario administrador local de PostgreSQL (ej. postgres o admin)
psql -U postgres -f gestion_citas.sql
```

O desde herramientas visuales (**pgAdmin / DBeaver / IntelliJ Database Tool**):
1. Abra el archivo `gestion_citas.sql`.
2. Ejecute el script contra su servidor PostgreSQL local.

### 3.2. Modelo Relacional Obligatorio

* **Tabla `cliente`:**
  - `id` (SERIAL PRIMARY KEY)
  - `nombres` (VARCHAR(80) NOT NULL)
  - `apellidos` (VARCHAR(80) NOT NULL)
  - `telefono` (VARCHAR(20) NOT NULL)
  - `correo` (VARCHAR(120) NOT NULL UNIQUE)
  - `activo` (BOOLEAN NOT NULL DEFAULT TRUE)

* **Tabla `cita_tecnica`:**
  - `id` (SERIAL PRIMARY KEY)
  - `cliente_id` (INT NOT NULL REFERENCES cliente(id) ON DELETE RESTRICT ON UPDATE CASCADE)
  - `fecha` (DATE NOT NULL)
  - `hora` (TIME NOT NULL)
  - `tipo_servicio` (VARCHAR(80) NOT NULL)
  - `descripcion` (VARCHAR(255) NOT NULL)
  - `estado` (VARCHAR(20) NOT NULL CHECK in `('Pendiente', 'En proceso', 'Finalizada', 'Cancelada')`)

### 3.3. Credenciales Locales y Variables de Entorno

Por defecto, la clase [`ConexionDB.java`](src/main/java/com/tecnoasistencia/conexion/ConexionDB.java) utiliza:
* **Host/Puerto:** `localhost:5432`
* **Base de datos:** `gestion_citas`
* **Usuario por defecto:** `admin` (o configurable)
* **Contraseña por defecto:** `1234`

Si su entorno local utiliza otras credenciales (por ejemplo usuario `postgres`), puede configurarlas sin modificar el código fuente mediante variables de entorno:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/gestion_citas"
export DB_USER="postgres"
export DB_PASS="tu_contraseña"
```

---

## 4. Compilación y Ejecución del Proyecto

Para compilar y ejecutar la aplicación mediante Maven:

```bash
# Limpiar y compilar el proyecto
mvn clean compile

# Iniciar la aplicación JavaFX
mvn javafx:run
```

O desde **IntelliJ IDEA**:
1. Abra el proyecto importando el archivo `pom.xml`.
2. Espere a que Maven descargue las dependencias (JavaFX 21 y driver PostgreSQL JDBC `42.7.3`).
3. Ejecute la clase principal `com.tecnoasistencia.App`.

---

## 5. Arquitectura del Proyecto

El proyecto sigue una arquitectura en capas con estricta separación de responsabilidades:

```
src/main/java/com/tecnoasistencia/
├── App.java                     # Punto de entrada JavaFX y sincronización reactiva de pestañas
├── conexion/
│   └── ConexionDB.java          # Conexión centralizada JDBC con verificación 'SELECT 1' vía Statement
├── modelo/
│   ├── Cliente.java             # Entidad de modelo Cliente
│   ├── CitaTecnica.java         # Entidad de modelo Cita Técnica (fecha, hora, tipo_servicio, descripcion)
│   └── EstadoCita.java          # Enumerador seguro de estados de citas técnicas
├── dao/
│   ├── ClienteDAO.java          # Operaciones CRUD y búsqueda con PreparedStatement para clientes
│   └── CitaTecnicaDAO.java      # Operaciones CRUD, persistencia y JOINs para citas técnicas
├── controlador/
│   ├── ClienteController.java   # Controlador FXML de clientes, formulario, TableView y filtros
│   └── CitaTecnicaController.java # Controlador FXML de citas, ComboBox y validaciones
├── utilidad/
│   ├── Alertas.java             # Gestión centralizada de diálogos y confirmaciones JavaFX
│   └── Validaciones.java        # Reglas de negocio y formatos (correo, HH:mm, longitudes)
└── excepcion/
    ├── AccesoDatosException.java # Excepciones de persistencia e integridad JDBC
    └── ReglaNegocioException.java # Excepciones de reglas de negocio
```

---

## 6. Guía de Pruebas Obligatorias de Aceptación (Rúbrica Sección 9)

1. **Registrar un cliente:** Ingrese nombres, apellidos, teléfono, correo único y guarde; observe su aparición inmediata en la tabla.
2. **Rechazo de correo duplicado:** Intente registrar un nuevo cliente con un correo ya existente; la aplicación emitirá una alerta explicativa y no permitirá duplicados.
3. **Modificar cliente:** Seleccione un cliente, modifique su teléfono y presione Actualizar; confirme que el cambio persiste en la base de datos.
4. **Registrar cita asociada:** Cambie a la pestaña *Citas técnicas*, seleccione el cliente en el ComboBox, asigne fecha, hora (HH:mm), tipo de servicio, descripción y guarde.
5. **Búsqueda insensible a mayúsculas/minúsculas:** En la pestaña *Clientes*, escriba una porción del apellido o correo en la barra de búsqueda y verifique los resultados filtrados.
6. **Filtrar citas y limpiar:** En la pestaña *Citas técnicas*, filtre por estado (ej. "Pendiente") o por fecha, y presione *Limpiar filtros* para recuperar todos los registros.
7. **Actualizar estado de cita:** Seleccione una cita y cambie su estado a "Finalizada".
8. **Eliminación restringida de cliente:** Intente eliminar un cliente que posee citas asociadas; la aplicación rechazará la operación y explicará detalladamente que debe eliminar las citas previamente.
9. **Eliminación en orden correcto:** Elimine primero la cita asociada y luego elimine el cliente; confirme cada diálogo de confirmación.
10. **Persistencia garantizada:** Cierre la aplicación por completo y vuelva a iniciarla con `mvn javafx:run`; compruebe que todos los registros se conservan intactos en PostgreSQL.
