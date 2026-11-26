package cafeteria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Pedido que arma el cajero: una lista de productos con sus cantidades.
 *
 * El subtotal se calcula con un stream, que es programación funcional: en
 * lugar de recorrer la lista con un for y acumular en una variable, se
 * describe la operación (tomar cada línea, pedir su subtotal y sumarlos).
 */
public class Pedido {

    private final List<ItemPedido> items = new ArrayList<>();

    /**
     * Agrega un producto. Si el producto ya estaba en el pedido, le suma la
     * cantidad en lugar de repetir la línea.
     */
    public void agregar(Producto producto, int cantidad) {
        int posicion = IntStream.range(0, items.size())
                .filter(i -> items.get(i).producto().getId() == producto.getId())
                .findFirst()
                .orElse(-1);
        if (posicion >= 0) {
            items.set(posicion, new ItemPedido(producto, items.get(posicion).cantidad() + cantidad));
        } else {
            items.add(new ItemPedido(producto, cantidad));
        }
    }

    /** Quita del pedido la línea que está en la posición dada. */
    public void quitar(int posicion) {
        if (posicion < 0 || posicion >= items.size()) {
            throw new IllegalArgumentException("No existe esa línea del pedido.");
        }
        items.remove(posicion);
    }

    /** Vacía el pedido. */
    public void limpiar() {
        items.clear();
    }

    public boolean estaVacio() {
        return items.isEmpty();
    }

    /** Lista de líneas; se devuelve de solo lectura para proteger el pedido. */
    public List<ItemPedido> getItems() {
        return Collections.unmodifiableList(items);
    }

    /** Número total de unidades del pedido. */
    public int getUnidades() {
        return items.stream().mapToInt(ItemPedido::cantidad).sum();
    }

    /** Suma de todas las líneas, calculada con un stream. */
    public double getSubtotal() {
        return items.stream().mapToDouble(ItemPedido::subtotal).sum();
    }

    /**
     * Cuántas unidades hay de cada tipo de producto, por ejemplo
     * {Bebida=2, Comida=3}.
     *
     * Es un pipeline de streams: agrupa las líneas por el tipo del producto
     * (que cada hija de Producto responde a su manera) y suma las cantidades
     * de cada grupo. El mapa se devuelve inmutable para que nadie pueda
     * alterar el resumen desde fuera.
     */
    public Map<String, Integer> getResumenPorTipo() {
        return Collections.unmodifiableMap(items.stream()
                .collect(Collectors.groupingBy(
                        item -> item.producto().getTipo(),
                        Collectors.summingInt(ItemPedido::cantidad))));
    }

    /** El resumen anterior escrito en una línea: "2 Bebida, 3 Comida". */
    public String getResumenEnTexto() {
        return getResumenPorTipo().entrySet().stream()
                .map(entrada -> entrada.getValue() + " " + entrada.getKey())
                .collect(Collectors.joining(", "));
    }

    /**
     * Aplica una regla de descuento al subtotal del pedido.
     *
     * @param nombreRegla nombre que quedará guardado en la base de datos
     * @param regla       la regla (una lambda) que calcula el descuento
     */
    public ResultadoDescuento calcular(String nombreRegla, ReglaDescuento regla) {
        return calcular(getSubtotal(), nombreRegla, regla);
    }

    /**
     * Aplica una regla a un monto escrito a mano en la ventana, sin necesidad
     * de armar el pedido. El descuento nunca puede ser mayor que el subtotal.
     */
    public static ResultadoDescuento calcular(double subtotal, String nombreRegla, ReglaDescuento regla) {
        if (subtotal < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo.");
        }
        double descuento = Math.min(Math.max(regla.calcularDescuento(subtotal), 0), subtotal);
        return new ResultadoDescuento(subtotal, nombreRegla, descuento, subtotal - descuento);
    }
}
