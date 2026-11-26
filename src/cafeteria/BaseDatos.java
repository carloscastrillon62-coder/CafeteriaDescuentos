package cafeteria;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Punto único de contacto con la base de datos.
 *
 * Centraliza la URL JDBC, la apertura de conexiones y la ejecución de los
 * scripts SQL. Los DAO ({@link ProductoDAO} y {@link PedidoDAO}) la usan para
 * conectarse, de modo que la aplicación tiene un solo lugar donde cambiar el
 * motor de base de datos si hiciera falta.
 *
 * Todas las conexiones se abren dentro de try-with-resources, así Java llama
 * a close() automáticamente aunque ocurra una excepción.
 */
public class BaseDatos {

    /** URL por defecto: el archivo cafeteria.db en la carpeta del proyecto. */
    public static final String URL_POR_DEFECTO = "jdbc:sqlite:cafeteria.db";

    private static final int TIEMPO_ESPERA_SEGUNDOS = 5;

    private final String url;

    public BaseDatos(String url) {
        this.url = (url == null || url.isBlank()) ? URL_POR_DEFECTO : url.trim();
    }

    public String getUrl() {
        return url;
    }

    /** Abre una conexión y activa las llaves foráneas (en SQLite vienen apagadas). */
    Connection conectar() throws SQLException {
        DriverManager.setLoginTimeout(TIEMPO_ESPERA_SEGUNDOS);
        Connection conexion = DriverManager.getConnection(url);
        try (Statement st = conexion.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conexion;
    }

    /**
     * Prueba la conexión al iniciar la aplicación y devuelve un mensaje para
     * mostrar en el área de texto de la ventana.
     *
     * @throws SQLException si no se puede abrir la conexión
     */
    public String probarConexion() throws SQLException {
        long inicio = System.nanoTime();
        try (Connection conexion = conectar()) {                 // try-with-resources
            DatabaseMetaData datos = conexion.getMetaData();
            long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
            return String.format(
                    "CONEXIÓN EXITOSA%n%n"
                            + "Base de datos: %s %s%n"
                            + "Driver:        %s %s%n"
                            + "URL:           %s%n"
                            + "Tiempo:        %d ms%n%n"
                            + "La conexión se cerró correctamente (try-with-resources).",
                    datos.getDatabaseProductName(), datos.getDatabaseProductVersion(),
                    datos.getDriverName(), datos.getDriverVersion(),
                    url, milisegundos);
        }
    }

    /** Indica si ya existen las tablas de la aplicación. */
    public boolean existenTablas() throws SQLException {
        try (Connection conexion = conectar()) {
            DatabaseMetaData datos = conexion.getMetaData();
            return existeTabla(datos, "productos") && existeTabla(datos, "pedidos");
        }
    }

    private static boolean existeTabla(DatabaseMetaData datos, String tabla) throws SQLException {
        try (ResultSet rs = datos.getTables(null, null, tabla, null)) {
            return rs.next();
        }
    }

    /**
     * Ejecuta un archivo .sql: quita los comentarios (--) y ejecuta cada
     * sentencia separada por punto y coma.
     *
     * @return número de sentencias ejecutadas
     */
    public int ejecutarScript(Path archivo) throws IOException, SQLException {
        StringBuilder sinComentarios = new StringBuilder();
        for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
            int comentario = linea.indexOf("--");
            sinComentarios.append(comentario >= 0 ? linea.substring(0, comentario) : linea).append('\n');
        }
        int ejecutadas = 0;
        try (Connection conexion = conectar(); Statement st = conexion.createStatement()) {
            for (String sentencia : sinComentarios.toString().split(";")) {
                if (!sentencia.isBlank()) {
                    st.execute(sentencia.trim());
                    ejecutadas++;
                }
            }
        }
        return ejecutadas;
    }

    /** Traduce los errores más comunes de la base de datos a un mensaje claro. */
    public static String mensajeAmigable(SQLException e) {
        String m = String.valueOf(e.getMessage());
        if (m.contains("no such table")) {
            return "La base de datos no tiene las tablas. Use el botón \"Crear BD con scripts\".";
        }
        if (m.contains("UNIQUE")) {
            return "Ya existe un registro con esos datos.";
        }
        if (m.contains("FOREIGN KEY")) {
            return "El producto referenciado no existe en la base de datos.";
        }
        return "Error de base de datos: " + m + "\nSQLState: " + e.getSQLState()
                + "   |   Código de error: " + e.getErrorCode();
    }
}
