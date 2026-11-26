package cafeteria;

/**
 * Interfaz funcional que representa una regla de descuento.
 *
 * Al tener un solo método abstracto se puede escribir como una expresión
 * lambda, que es lo que pide la actividad:
 *
 * <pre>
 * ReglaDescuento general = subtotal -&gt; subtotal &gt; 10000 ? subtotal * 0.15 : 0;
 * </pre>
 *
 * La anotación @FunctionalInterface no es obligatoria, pero hace que el
 * compilador avise si alguien agrega un segundo método abstracto por error.
 *
 * Gracias a esta interfaz se cumple el <b>principio abierto/cerrado</b> de
 * SOLID: para agregar una promoción nueva basta con escribir otra lambda y
 * registrarla en {@link CatalogoReglas}, sin modificar las reglas que ya
 * existen ni la ventana.
 */
@FunctionalInterface
public interface ReglaDescuento {

    /**
     * Calcula cuánto dinero se descuenta de un subtotal.
     *
     * @param subtotal valor del pedido antes del descuento
     * @return el valor del descuento (0 si la regla no aplica)
     */
    double calcularDescuento(double subtotal);

    /**
     * Método por defecto: aplica la regla y devuelve el total ya con el
     * descuento restado. Los métodos default no cuentan como abstractos, así
     * que la interfaz sigue siendo funcional.
     */
    default double aplicarA(double subtotal) {
        return subtotal - calcularDescuento(subtotal);
    }
}
