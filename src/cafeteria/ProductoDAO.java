package cafeteria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DAO (Data Access Object) de la tabla productos: aquí vive todo el SQL de
 * consulta del catálogo.
 *
 * La ventana nunca escribe SQL; solo llama a estos métodos. Eso mantiene
 * separados el modelo y la vista (bajo acoplamiento y alta cohesión).
 *
 * Las consultas usan PreparedStatement con parámetros (?), lo que evita
 * problemas con las comillas y protege contra la inyección de SQL.
 */
public class ProductoDAO {

    private static final String SELECT =
            "SELECT id, codigo, nombre, tipo, precio, mililitros, caliente, gramos, vegetariana FROM productos ";

    private final BaseDatos baseDatos;

    public ProductoDAO(BaseDatos baseDatos) {
        this.baseDatos = baseDatos;
    }

    /** Devuelve todos los productos ordenados por tipo y código. */
    public List<Producto> listar() throws SQLException {
        return buscar("");
    }

    /** Busca los productos cuyo código, nombre o tipo contengan el texto dado. */
    public List<Producto> buscar(String texto) throws SQLException {
        String sql = SELECT + "WHERE codigo LIKE ? OR nombre LIKE ? OR tipo LIKE ? ORDER BY tipo, codigo";
        String patron = "%" + (texto == null ? "" : texto.trim()) + "%";
        List<Producto> productos = new ArrayList<>();
        try (Connection conexion = baseDatos.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setString(3, patron);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    productos.add(crearDesde(rs));
                }
            }
        }
        return List.copyOf(productos);          // la lista se entrega inmutable
    }

    /**
     * Cuántos productos hay de cada tipo, por ejemplo {Bebida=9, Comida=7}.
     *
     * Pipeline de streams: agrupa el catálogo por el tipo de cada producto y
     * cuenta los de cada grupo. El mapa se devuelve inmutable.
     */
    public Map<String, Long> contarPorTipo() throws SQLException {
        return Collections.unmodifiableMap(listar().stream()
                .collect(Collectors.groupingBy(Producto::getTipo, Collectors.counting())));
    }

    /** El conteo anterior escrito en una línea: "9 Bebida, 7 Comida". */
    public String resumenEnTexto() throws SQLException {
        return contarPorTipo().entrySet().stream()
                .map(entrada -> entrada.getValue() + " " + entrada.getKey())
                .collect(Collectors.joining(", "));
    }

    /**
     * Convierte una fila de la tabla en el objeto que corresponde: según la
     * columna tipo crea una {@link Bebida} o una {@link Comida}. Las dos se
     * devuelven como {@link Producto}, que es el tipo que maneja el resto de
     * la aplicación (polimorfismo).
     */
    private static Producto crearDesde(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String codigo = rs.getString("codigo");
        String nombre = rs.getString("nombre");
        double precio = rs.getDouble("precio");
        if ("BEBIDA".equalsIgnoreCase(rs.getString("tipo"))) {
            return new Bebida(id, codigo, nombre, precio, rs.getInt("mililitros"), rs.getInt("caliente") == 1);
        }
        return new Comida(id, codigo, nombre, precio, rs.getInt("gramos"), rs.getInt("vegetariana") == 1);
    }
}
