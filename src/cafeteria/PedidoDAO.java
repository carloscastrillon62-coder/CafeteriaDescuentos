package cafeteria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de los pedidos: guarda en la base de datos el pedido con su descuento
 * y el detalle de los productos, y consulta los pedidos ya guardados.
 *
 * El guardado se hace dentro de una <b>transacción</b>: si falla la inserción
 * de alguna línea, se deshace todo con rollback y la base de datos no queda a
 * medias.
 */
public class PedidoDAO {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BaseDatos baseDatos;

    public PedidoDAO(BaseDatos baseDatos) {
        this.baseDatos = baseDatos;
    }

    /** Resumen de un pedido guardado, para mostrarlo en la ventana. */
    public record PedidoGuardado(int id, String fecha, double subtotal, String regla,
                                 double descuento, double total, int unidades) {
    }

    /**
     * Guarda el pedido y su resultado de descuento.
     *
     * @return el id que la base de datos le asignó al pedido
     */
    public int guardar(Pedido pedido, ResultadoDescuento resultado) throws SQLException {
        if (pedido.estaVacio()) {
            throw new IllegalArgumentException("El pedido no tiene productos.");
        }
        String sqlPedido = "INSERT INTO pedidos (fecha, subtotal, regla, descuento, total) VALUES (?, ?, ?, ?, ?)";
        String sqlItem = "INSERT INTO pedido_items (id_pedido, id_producto, cantidad, precio_unitario) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection conexion = baseDatos.conectar()) {
            conexion.setAutoCommit(false);                       // inicia la transacción
            try (PreparedStatement ps = conexion.prepareStatement(sqlPedido, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, LocalDateTime.now().format(FORMATO_FECHA));
                ps.setDouble(2, resultado.subtotal());
                ps.setString(3, resultado.regla());
                ps.setDouble(4, resultado.descuento());
                ps.setDouble(5, resultado.total());
                ps.executeUpdate();

                int idPedido;
                try (ResultSet llaves = ps.getGeneratedKeys()) {
                    llaves.next();
                    idPedido = llaves.getInt(1);
                }

                try (PreparedStatement psItem = conexion.prepareStatement(sqlItem)) {
                    for (ItemPedido item : pedido.getItems()) {
                        psItem.setInt(1, idPedido);
                        psItem.setInt(2, item.producto().getId());
                        psItem.setInt(3, item.cantidad());
                        psItem.setDouble(4, item.producto().getPrecio());
                        psItem.addBatch();
                    }
                    psItem.executeBatch();
                }
                conexion.commit();                               // confirma la transacción
                return idPedido;
            } catch (SQLException e) {
                conexion.rollback();                             // deshace si algo falló
                throw e;
            }
        }
    }

    /** Devuelve los últimos pedidos guardados, del más reciente al más antiguo. */
    public List<PedidoGuardado> listarUltimos(int cantidad) throws SQLException {
        String sql = "SELECT p.id, p.fecha, p.subtotal, p.regla, p.descuento, p.total, "
                + "       COALESCE(SUM(i.cantidad), 0) AS unidades "
                + "FROM pedidos p LEFT JOIN pedido_items i ON i.id_pedido = p.id "
                + "GROUP BY p.id ORDER BY p.id DESC LIMIT ?";
        List<PedidoGuardado> pedidos = new ArrayList<>();
        try (Connection conexion = baseDatos.conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pedidos.add(new PedidoGuardado(rs.getInt("id"), rs.getString("fecha"),
                            rs.getDouble("subtotal"), rs.getString("regla"),
                            rs.getDouble("descuento"), rs.getDouble("total"), rs.getInt("unidades")));
                }
            }
        }
        return pedidos;
    }

    /** Cuenta cuántos pedidos hay guardados. */
    public int contar() throws SQLException {
        try (Connection conexion = baseDatos.conectar();
             PreparedStatement ps = conexion.prepareStatement("SELECT COUNT(*) FROM pedidos");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
