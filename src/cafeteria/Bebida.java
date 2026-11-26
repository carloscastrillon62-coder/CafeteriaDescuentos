package cafeteria;

/**
 * Bebida de la cafetería: hereda de {@link Producto} y agrega el tamaño en
 * mililitros y si se sirve caliente o fría.
 *
 * Sobrescribe getTipo() y getDetalle() (polimorfismo).
 */
public class Bebida extends Producto {

    private final int mililitros;
    private final boolean caliente;

    public Bebida(int id, String codigo, String nombre, double precio, int mililitros, boolean caliente) {
        super(id, codigo, nombre, precio);
        if (mililitros <= 0) {
            throw new IllegalArgumentException("Los mililitros deben ser mayores que cero.");
        }
        this.mililitros = mililitros;
        this.caliente = caliente;
    }

    public int getMililitros() {
        return mililitros;
    }

    public boolean esCaliente() {
        return caliente;
    }

    @Override
    public String getTipo() {
        return "Bebida";
    }

    @Override
    public String getDetalle() {
        return mililitros + " ml, " + (caliente ? "caliente" : "fría");
    }
}
