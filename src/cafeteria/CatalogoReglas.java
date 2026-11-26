package cafeteria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Catálogo de las promociones de la cafetería.
 *
 * Cada promoción es una {@link ReglaDescuento} escrita como una <b>expresión
 * lambda</b>. Las promociones viven en una lista a la que se puede agregar
 * una nueva con {@link #registrar}, sin tocar las que ya están ni la ventana:
 * eso es el <b>principio abierto/cerrado</b> de SOLID (abierto a extensión,
 * cerrado a modificación).
 *
 * La clase también ofrece dos <b>funciones de orden superior</b>: métodos que
 * reciben reglas y devuelven una regla nueva ({@link #maximo} y
 * {@link #conTope}), para combinar promociones sin reescribirlas.
 */
public final class CatalogoReglas {

    /** Umbral de la regla general, tomado del ejemplo de la actividad. */
    public static final double UMBRAL_GENERAL = 10000;

    /** Una promoción con su nombre, su explicación y la lambda que la calcula. */
    public record Promocion(String nombre, String descripcion, ReglaDescuento regla) {

        /** El JComboBox de la ventana muestra lo que devuelve toString(). */
        @Override
        public String toString() {
            return nombre;
        }
    }

    private static final List<Promocion> PROMOCIONES = new ArrayList<>();

    static {
        // --------- Las reglas, escritas como expresiones lambda ---------

        registrar("Sin descuento",
                "No aplica ninguna promoción: el cliente paga el subtotal completo.",
                subtotal -> 0);

        // Regla del ejemplo de la actividad, en pesos colombianos.
        registrar("General (compra mayor a $10.000)",
                "15 % de descuento si el subtotal supera los $10.000; de lo contrario, 0 %.",
                subtotal -> subtotal > UMBRAL_GENERAL ? subtotal * 0.15 : 0);

        registrar("Estudiante",
                "10 % de descuento sobre cualquier compra presentando el carné.",
                subtotal -> subtotal * 0.10);

        registrar("Desayuno (antes de las 10 a. m.)",
                "20 % de descuento si el subtotal supera los $8.000.",
                subtotal -> subtotal > 8000 ? subtotal * 0.20 : 0);

        registrar("Compra grande (mayor a $50.000)",
                "25 % de descuento para compras superiores a $50.000; 5 % para el resto.",
                subtotal -> subtotal > 50000 ? subtotal * 0.25 : subtotal * 0.05);

        // Ejemplo del principio abierto/cerrado: esta promoción se agrega
        // combinando dos reglas que ya existían, sin modificar ninguna de ellas.
        registrar("Día del café (la mejor de las dos)",
                "Aplica automáticamente la promoción que más le convenga al cliente "
                        + "entre la general y la de estudiante, con un tope de $20.000.",
                conTope(maximo(buscar("General (compra mayor a $10.000)"), buscar("Estudiante")), 20000));
    }

    private CatalogoReglas() {
        // Clase de utilidades: no se crean objetos de ella.
    }

    // ------------------------------------------------------------------
    // Registro y consulta de promociones
    // ------------------------------------------------------------------

    /** Agrega una promoción nueva al catálogo (extensión sin modificación). */
    public static void registrar(String nombre, String descripcion, ReglaDescuento regla) {
        PROMOCIONES.add(new Promocion(nombre, descripcion, regla));
    }

    /** Promociones disponibles, en el orden en que se registraron. */
    public static List<Promocion> getPromociones() {
        return Collections.unmodifiableList(PROMOCIONES);
    }

    /**
     * Índice inmutable de las promociones por su nombre.
     *
     * Se arma con un pipeline de streams y se devuelve envuelto en
     * Collections.unmodifiableMap, de modo que quien lo reciba pueda
     * consultarlo pero no modificar el catálogo por detrás.
     */
    public static Map<String, Promocion> getIndice() {
        return Collections.unmodifiableMap(PROMOCIONES.stream()
                .collect(Collectors.toMap(Promocion::nombre, p -> p,
                        (anterior, nueva) -> nueva, LinkedHashMap::new)));
    }

    /** Busca la regla de una promoción por su nombre. */
    public static ReglaDescuento buscar(String nombre) {
        Promocion promocion = getIndice().get(nombre);
        if (promocion == null) {
            throw new IllegalArgumentException("No existe la promoción " + nombre);
        }
        return promocion.regla();
    }

    // ------------------------------------------------------------------
    // Funciones de orden superior: reciben reglas y devuelven una regla
    // ------------------------------------------------------------------

    /** Devuelve una regla que aplica, de las dos, la que más descuento dé. */
    public static ReglaDescuento maximo(ReglaDescuento a, ReglaDescuento b) {
        return subtotal -> Math.max(a.calcularDescuento(subtotal), b.calcularDescuento(subtotal));
    }

    /** Devuelve la misma regla pero sin pasarse de un descuento máximo. */
    public static ReglaDescuento conTope(ReglaDescuento regla, double maximo) {
        return subtotal -> Math.min(regla.calcularDescuento(subtotal), maximo);
    }
}
