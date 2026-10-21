# CitasTecnicasConHilos
### Sistema de Gestión de Citas de Atención Técnica con Concurrencia Multihilo (JavaFX y JDBC)
**TecnoAsistencia Nicaragua S.A.** | **Desarrollador / Colaborador:** [William García (@wigar03)](https://github.com/wigar03)  
**Tecnologías:** Java 21, JavaFX 21, PostgreSQL JDBC, Concurrencia y Multihilo (`java.util.concurrent`, `javafx.concurrent`)

---

## 1. Descripción del Proyecto

`CitasTecnicasConHilos` es una evolución del sistema de gestión de citas técnicas de **TecnoAsistencia Nicaragua S.A.** que implementa una **arquitectura concurrente y multihilo profesional**.

En aplicaciones de escritorio tradicionales, ejecutar llamadas de entrada/salida (I/O) o consultas a bases de datos relacionales directamente en el **JavaFX Application Thread** provoca congelamientos de interfaz gráfica (ANR - *Application Not Responding*), caídas de fotogramas e interrupciones en la experiencia del usuario. 

Este proyecto resuelve este problema desacoplando todas las operaciones de persistencia, monitoreo, cálculo y exportación hacia **hilos secundarios administrados**, manteniendo la interfaz gráfica siempre fluida, responsiva y con retroalimentación visual continua.

---

## 2. Arquitectura de Concurrencia y Hilos

El proyecto implementa un diseño concurrente estructurado en el paquete `com.tecnoasistencia.concurrencia`:

```
src/main/java/com/tecnoasistencia/
├── App.java                               # Inicialización de la aplicación, barra de estado global y shutdown de hilos
├── concurrencia/
│   ├── AppThreadPool.java                 # Gestor centralizado de Thread Pools (Worker pool y Scheduled pool)
│   ├── AsyncUIHelper.java                 # Vinculación reactiva de Task a controles UI y cambio seguro de hilos
│   ├── MonitorConexion.java               # Hilo demonio de monitoreo continuo (Heartbeat/Health Check) a PostgreSQL
│   ├── ServicioMetricas.java              # Tarea en segundo plano para cálculo asíncrono de métricas
│   ├── MonitorRecordatorios.java          # Hilo demonio de detección periódica de citas próximas
│   └── ExportadorReporteAsync.java        # Tarea de exportación a CSV con seguimiento de progreso en tiempo real
├── conexion/
│   └── ConexionDB.java                    # Conexión centralizada JDBC con verificación 'SELECT 1'
├── modelo/
│   ├── Cliente.java                       # Entidad Cliente
│   ├── CitaTecnica.java                   # Entidad Cita Técnica
│   └── EstadoCita.java                    # Enumerador seguro de estados
├── dao/
│   ├── ClienteDAO.java                    # Consultas SQL con PreparedStatement para clientes
│   └── CitaTecnicaDAO.java                # Consultas SQL con PreparedStatement para citas
├── controlador/
│   ├── ClienteController.java             # CRUD asíncrono de clientes y búsqueda cancelable
│   └── CitaTecnicaController.java         # CRUD asíncrono de citas, métricas y exportación CSV
└── utilidad/
    ├── Alertas.java                       # Diálogos y confirmaciones JavaFX
    └── Validaciones.java                  # Validaciones de reglas de negocio
```

### 2.1. Componentes Clave de Concurrencia

1. **Gestor Centralizado de Hilos (`AppThreadPool`)**:
   - Administra un `FixedThreadPool` para tareas de trabajo (`citas-worker-%d`) dimensionado dinámicamente según los núcleos del CPU.
   - Administra un `ScheduledThreadPoolExecutor` para tareas periódicas (`citas-cron-%d`).
   - Configura todos los hilos como **hilos demonio (*daemon threads*)**, garantizando que la JVM pueda cerrarse de inmediato sin procesos zombi.
   - Integra un apagado ordenado (*graceful shutdown*) en `App.stop()`.

2. **Vinculación Reactiva de UI (`AsyncUIHelper`)**:
   - `runOnFXThread(Runnable)`: Garantiza que cualquier actualización de la vista ocurra estrictamente en el *JavaFX Application Thread*, protegiendo la aplicación contra excepciones de concurrencia.
   - `vincularTareaAControles(Task, ProgressIndicator, Node...)`: Enlaza la propiedad `task.runningProperty()` para mostrar spinners y deshabilitar botones de acción durante la ejecución, previniendo condiciones de carrera y clics duplicados.

3. **Carga y Operaciones CRUD Asíncronas**:
   - La recuperación de clientes y citas se ejecuta mediante `Task<T>`, mostrando barras de progreso e indicadores de estado ("Cargando clientes...", "Datos sincronizados").
   - Las operaciones de Guardar, Actualizar y Eliminar verifican duplicados y restricciones en segundo plano, informando el resultado mediante eventos `setOnSucceeded` y `setOnFailed`.

4. **Búsqueda en Base de Datos Cancelable**:
   - En `ClienteController`, al iniciar una nueva búsqueda en PostgreSQL, si existe una búsqueda previa en ejecución, se cancela automáticamente (`tareaBusquedaActual.cancel(true)`), protegiendo la aplicación contra resultados desordenados o saturación de red.

5. **Hilo Demonio de Monitoreo Continuo (Heartbeat)**:
   - `MonitorConexion` ejecuta periódicamente cada 6 segundos una comprobación de conectividad (`SELECT 1`) en un hilo secundario.
   - Muestra en tiempo real en la barra de estado global de `App` el estado de la base de datos (🟢 En línea con latencia en ms, 🔴 Desconectada, 🟡 Comprobando).

6. **Servicio Asíncrono de Métricas (`ServicioMetricas`)**:
   - Procesa agregados estadísticos (total de citas, pendientes, completadas, canceladas y tasa de efectividad) sobre copias inmutables (*snapshots*), actualizando el banner de métricas sin congelar el scroll ni los filtros.

7. **Monitor Periódico de Recordatorios (`MonitorRecordatorios`)**:
   - Hilo demonio que escanea cada 15 segundos las citas del día actual, detectando citas programadas para las próximas 2 horas y emitiendo avisos visuales en tiempo real.

8. **Exportación a CSV con Seguimiento de Progreso (`ExportadorReporteAsync`)**:
   - Escribe registros en un archivo CSV en disco utilizando `Task<File>`.
   - Emite actualizaciones de progreso porcentual (`updateProgress`) y mensajes descriptivos paso a paso (`updateMessage`) que se reflejan de forma fluida en la interfaz.

---

## 3. Requisitos del Entorno

* **Java Development Kit (JDK):** Versión 21 o superior.
* **Apache Maven:** Versión 3.8 o superior.
* **Motor de Base de Datos:** PostgreSQL 14, 15, 16 o superior.
* **Sistema Operativo:** Linux, Windows o macOS.

---

## 4. Configuración de PostgreSQL

Ejecute el script oficial [`gestion_citas.sql`](gestion_citas.sql) para inicializar las tablas e índices:

```bash
psql -U postgres -f gestion_citas.sql
```

### Variables de Entorno (Opcional)

Si su PostgreSQL local no usa las credenciales por defecto (`admin`/`1234`), configure las variables de entorno correspondientes:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/gestion_citas"
export DB_USER="postgres"
export DB_PASS="tu_contraseña"
```

---

## 5. Compilación y Ejecución

```bash
# Limpiar y compilar el proyecto
mvn clean compile

# Ejecutar la aplicación JavaFX con soporte de hilos
mvn javafx:run
```

---

## 6. Guía de Pruebas de Concurrencia y Aceptación

1. **Responsividad de UI durante operaciones pesadas:**
   - Presione el botón **Exportar CSV** en la pestaña de citas técnicas. Observe cómo la barra de progreso avanza en tiempo real mientras la interfaz permanece completamente interactiva (se puede hacer scroll, cambiar filtros o escribir texto sin congelamientos).
2. **Prevención de Clics Duplicados:**
   - Al presionar **Guardar** o **Actualizar**, observe que los botones se deshabilitan instantáneamente durante la transacción en segundo plano y se reactivan al finalizar.
3. **Monitoreo Continuo de Conexión (Heartbeat):**
   - Inicie la aplicación y verifique el indicador inferior `🟢 DB En línea (X ms)`.
   - Detenga temporalmente el servicio de PostgreSQL (`sudo systemctl stop postgresql`); en menos de 6 segundos el badge cambiará a `🔴 DB Desconectada`.
   - Vuelva a iniciar PostgreSQL; el monitor detectará automáticamente la recuperación y restaurará el estado a `🟢 DB En línea`.
4. **Búsqueda Asíncrona Cancelable:**
   - Ingrese un término en el campo de búsqueda de clientes y pulse Buscar repetidas veces; compruebe que no se producen bloqueos y que las tareas previas se cancelan limpiamente.
5. **Detección de Citas Próximas en Segundo Plano:**
   - Agende una cita para el día de hoy en un horario dentro de las próximas 2 horas. En pocos segundos, el monitor demonio actualizará la píldora superior a `🔔 ¡Atención! Cita programada para las próximas 2 horas`.
6. **Cálculo Asíncrono de Métricas:**
   - Filtre citas o modifique el estado de una cita a "Completada"; observe cómo el banner de métricas superior recalcula los totales y el porcentaje de cumplimiento inmediatamente.
7. **Cierre Limpio de la Aplicación:**
   - Cierre la ventana de la aplicación. Verifique en los registros de la consola que `AppThreadPool` y los monitores demonio se detienen de forma ordenada (`[AppThreadPool] Grupos de hilos detenidos exitosamente`), finalizando la JVM sin procesos colgados.
