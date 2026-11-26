package cafeteria;

/**
 * Resultado inmutable de aplicar una regla de descuento a un pedido.
 *
 * @param subtotal  valor del pedido antes del descuento
 * @param regla     nombre de la regla que se aplicó
 * @param descuento valor descontado
 * @param total     valor final que paga el cliente
 */
public record ResultadoDescuento(double subtotal, String regla, double descuento, double total) {

    /** Porcentaje real que representó el descuento sobre el subtotal. */
    public double porcentaje() {
        return subtotal == 0 ? 0 : descuento * 100 / subtotal;
    }
}
