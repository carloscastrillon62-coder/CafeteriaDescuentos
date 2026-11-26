package cafeteria;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Pruebas de consola, sin frameworks externos.
 *
 * Crean una base de datos temporal con los scripts de la carpeta sql y
 * comprueban el modelo POO, las reglas de descuento escritas con lambdas y
 * la persistencia con JDBC.
 *
 * Uso: java -cp "out;lib/sqlite-jdbc-3.42.0.1.jar" cafeteria.PruebasCafeteria   (Windows)
 */
public class PruebasCafeteria {

    /** Una prueba que puede lanzar excepciones. */
    @FunctionalInterface
    interface Prueba {
        boolean ejecutar() throws Exception;
    }

    private static final String ARCHIVO = "prueba_cafeteria.db";

    public static void main(String[] args) throws Exception {
        new File(ARCHIVO).delete();
        BaseDatos bd = new BaseDatos("jdbc:sqlite:" + ARCHIVO);
        ProductoDAO productoDAO = new ProductoDAO(bd);
        PedidoDAO pedidoDAO = new PedidoDAO(bd);
        List<Boolean> resultados = new ArrayList<>();

        resultados.add(prueba("1. Los scripts crean 16 productos (9 bebidas y 7 comidas)", () -> {
            bd.ejecutarScript(Path.of("sql", "01_crear_tablas.sql"));
            bd.ejecutarScript(Path.of("sql", "02_poblar_datos.sql"));
            List<Producto> todos = productoDAO.listar();
            long bebidas = todos.stream().filter(p -> p instanceof Bebida).count();
            long comidas = todos.stream().filter(p -> p instanceof Comida).count();
            return bd.existenTablas() && todos.size() == 16 && bebidas == 9 && comidas == 7;
        }));

        resultados.add(prueba("2. Herencia y polimorfismo: cada producto da su tipo y su detalle", () -> {
            Producto bebida = productoDAO.buscar("BEB-004").get(0);
            Producto comida = productoDAO.buscar("COM-001").get(0);
            return bebida.getTipo().equals("Bebida") && bebida.getDetalle().equals("350 ml, caliente")
                    && comida.getTipo().equals("Comida") && comida.getDetalle().equals("90 g, vegetariana");
        }));

        resultados.add(prueba("3. Lambda de la guia: mas de $10.000 descuenta 15%", () -> {
            ReglaDescuento general = CatalogoReglas.buscar("General (compra mayor a $10.000)");
            return general.calcularDescuento(20000) == 3000
                    && general.calcularDescuento(10000) == 0
                    && general.aplicarA(20000) == 17000;
        }));

        resultados.add(prueba("4. Lambda de estudiante: 10% sobre cualquier compra", () -> {
            ReglaDescuento estudiante = CatalogoReglas.buscar("Estudiante");
            return estudiante.calcularDescuento(8000) == 800 && estudiante.calcularDescuento(50000) == 5000;
        }));

        resultados.add(prueba("5. Pedido: el subtotal se suma con streams", () -> {
            Pedido pedido = new Pedido();
            pedido.agregar(productoDAO.buscar("BEB-003").get(0), 2);   // capuchino 7.500
            pedido.agregar(productoDAO.buscar("COM-002").get(0), 3);   // pandebono 2.500
            pedido.agregar(productoDAO.buscar("BEB-003").get(0), 1);   // suma al capuchino
            return pedido.getItems().size() == 2 && pedido.getUnidades() == 6
                    && pedido.getSubtotal() == 30000;
        }));

        resultados.add(prueba("6. Funcion de orden superior: maximo() toma el mejor descuento", () -> {
            ReglaDescuento mejor = CatalogoReglas.maximo(
                    CatalogoReglas.buscar("General (compra mayor a $10.000)"),
                    CatalogoReglas.buscar("Estudiante"));
            return mejor.calcularDescuento(20000) == 3000      // gana la general (15%)
                    && mejor.calcularDescuento(5000) == 500;   // gana la de estudiante (10%)
        }));

        resultados.add(prueba("7. Funcion de orden superior: conTope() limita el descuento", () -> {
            ReglaDescuento topada = CatalogoReglas.conTope(CatalogoReglas.buscar("Estudiante"), 2000);
            return topada.calcularDescuento(10000) == 1000 && topada.calcularDescuento(90000) == 2000;
        }));

        resultados.add(prueba("8. Abierto/cerrado: se registra una regla nueva sin tocar las demas", () -> {
            int antes = CatalogoReglas.getPromociones().size();
            CatalogoReglas.registrar("Promocion de prueba", "30% fijo", subtotal -> subtotal * 0.30);
            return CatalogoReglas.getPromociones().size() == antes + 1
                    && CatalogoReglas.buscar("Promocion de prueba").calcularDescuento(10000) == 3000
                    && CatalogoReglas.buscar("Estudiante").calcularDescuento(10000) == 1000;
        }));

        resultados.add(prueba("9. El descuento nunca supera el subtotal", () -> {
            ResultadoDescuento r = Pedido.calcular(1000, "Exagerada", subtotal -> subtotal * 5);
            return r.descuento() == 1000 && r.total() == 0;
        }));

        resultados.add(prueba("10. Persistencia: el pedido se guarda con su detalle", () -> {
            Pedido pedido = new Pedido();
            pedido.agregar(productoDAO.buscar("COM-005").get(0), 1);   // wrap 14.500
            pedido.agregar(productoDAO.buscar("BEB-008").get(0), 2);   // limonada 9.000
            ReglaDescuento general = CatalogoReglas.buscar("General (compra mayor a $10.000)");
            ResultadoDescuento resultado = pedido.calcular("General (compra mayor a $10.000)", general);
            int id = pedidoDAO.guardar(pedido, resultado);
            PedidoDAO.PedidoGuardado guardado = pedidoDAO.listarUltimos(1).get(0);
            return id > 0 && pedidoDAO.contar() == 1 && guardado.unidades() == 3
                    && guardado.subtotal() == 32500 && guardado.descuento() == 4875
                    && guardado.total() == 27625;
        }));

        resultados.add(prueba("11. Pipeline de streams: el catalogo se agrupa y cuenta por tipo", () -> {
            java.util.Map<String, Long> conteo = productoDAO.contarPorTipo();
            return conteo.get("Bebida") == 9 && conteo.get("Comida") == 7
                    && productoDAO.resumenEnTexto().contains("9 Bebida");
        }));

        resultados.add(prueba("12. Colecciones inmutables: no se pueden modificar desde fuera", () -> {
            Pedido pedido = new Pedido();
            pedido.agregar(productoDAO.buscar("BEB-001").get(0), 2);
            pedido.agregar(productoDAO.buscar("COM-002").get(0), 3);
            boolean resumenOk = pedido.getResumenPorTipo().get("Bebida") == 2
                    && pedido.getResumenPorTipo().get("Comida") == 3;
            boolean listaProtegida = esInmutable(() -> pedido.getItems().clear());
            boolean mapaProtegido = esInmutable(() -> pedido.getResumenPorTipo().clear());
            boolean indiceProtegido = esInmutable(() -> CatalogoReglas.getIndice().clear());
            return resumenOk && listaProtegida && mapaProtegido && indiceProtegido;
        }));

        resultados.add(prueba("13. Validacion: un monto negativo se rechaza", () -> {
            try {
                Pedido.calcular(-100, "Estudiante", CatalogoReglas.buscar("Estudiante"));
                return false;
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("negativo");
            }
        }));

        resultados.add(prueba("14. Validacion: no se puede guardar un pedido vacio", () -> {
            try {
                pedidoDAO.guardar(new Pedido(), new ResultadoDescuento(0, "Sin descuento", 0, 0));
                return false;
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("no tiene productos");
            }
        }));

        new File(ARCHIVO).delete();
        long aprobadas = resultados.stream().filter(Boolean::booleanValue).count();
        System.out.printf("%nPruebas aprobadas: %d de %d%n", aprobadas, resultados.size());
    }

    /** Devuelve true si la operación lanza UnsupportedOperationException. */
    private static boolean esInmutable(Runnable operacion) {
        try {
            operacion.run();
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        }
    }

    private static boolean prueba(String nombre, Prueba prueba) {
        boolean ok;
        try {
            ok = prueba.ejecutar();
        } catch (Exception e) {
            System.out.println("    Error: " + e);
            ok = false;
        }
        System.out.printf("[%s] %s%n", ok ? "OK" : "FALLA", nombre);
        return ok;
    }
}
