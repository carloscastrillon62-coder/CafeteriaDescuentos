package cafeteria;

/**
 * Clase base de todo lo que se vende en la cafetería.
 *
 * Es una clase <b>abstracta</b>: no se puede crear un Producto "suelto", solo
 * sus hijas {@link Bebida} y {@link Comida}. Guarda los datos que comparten
 * todos los productos y obliga a cada hija a decir su tipo y su detalle.
 *
 * Principios de POO que aplica:
 *  - Encapsulamiento: los atributos son private final y se leen con getters.
 *  - Herencia: Bebida y Comida extienden esta clase.
 *  - Polimorfismo: getTipo() y getDetalle() son abstractos, así que cada hija
 *    responde a su manera y la interfaz los trata a todos igual.
 */
public abstract class Producto {

    private final int id;
    private final String codigo;
    private final String nombre;
    private final double precio;

    protected Producto(int id, String codigo, String nombre, double precio) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código del producto es obligatorio.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio.");
        }
        if (precio < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo.");
        }
        this.id = id;
        this.codigo = codigo.trim();
        this.nombre = nombre.trim();
        this.precio = precio;
    }

    public int getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public double getPrecio() {
        return precio;
    }

    /** Tipo del producto: cada hija responde "Bebida" o "Comida". */
    public abstract String getTipo();

    /** Detalle propio de cada hija, por ejemplo "350 ml, caliente" o "90 g, vegetariana". */
    public abstract String getDetalle();

    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }
}
