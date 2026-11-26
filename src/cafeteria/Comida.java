package cafeteria;

/**
 * Comida de la cafetería: hereda de {@link Producto} y agrega el peso en
 * gramos y si es vegetariana.
 *
 * Sobrescribe getTipo() y getDetalle() (polimorfismo).
 */
public class Comida extends Producto {

    private final int gramos;
    private final boolean vegetariana;

    public Comida(int id, String codigo, String nombre, double precio, int gramos, boolean vegetariana) {
        super(id, codigo, nombre, precio);
        if (gramos <= 0) {
            throw new IllegalArgumentException("Los gramos deben ser mayores que cero.");
        }
        this.gramos = gramos;
        this.vegetariana = vegetariana;
    }

    public int getGramos() {
        return gramos;
    }

    public boolean esVegetariana() {
        return vegetariana;
    }

    @Override
    public String getTipo() {
        return "Comida";
    }

    @Override
    public String getDetalle() {
        return gramos + " g, " + (vegetariana ? "vegetariana" : "no vegetariana");
    }
}
