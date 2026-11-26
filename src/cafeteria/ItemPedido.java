package cafeteria;

/**
 * Una línea del pedido: un producto y cuántas unidades se pidieron.
 *
 * Es un record, es decir, un objeto inmutable que solo guarda datos. Java
 * genera el constructor y los métodos de acceso producto() y cantidad().
 *
 * @param producto producto pedido
 * @param cantidad número de unidades (mayor que cero)
 */
public record ItemPedido(Producto producto, int cantidad) {

    public ItemPedido {
        if (producto == null) {
            throw new IllegalArgumentException("El producto es obligatorio.");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
    }

    /** Precio del producto multiplicado por la cantidad. */
    public double subtotal() {
        return producto.getPrecio() * cantidad;
    }
}
