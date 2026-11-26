# CafeteriaDescuentos — Gestión de descuentos de una cafetería (Java + Swing + JDBC)

Aplicación de escritorio en **Java puro, sin frameworks externos**, que calcula los
descuentos aplicables a los pedidos de una cafetería. Integra los tres temas del curso:

| Unidad | Tema | Dónde se ve en el proyecto |
|---|---|---|
| I | POO y diseño | `Producto` (abstracta), `Bebida`, `Comida`, diagrama de clases en `docs/` |
| II | Programación funcional y Git | `ReglaDescuento` (`@FunctionalInterface`), lambdas en `CatalogoReglas`, historial del repositorio |
| III | Interfaz y persistencia | `VentanaCafeteria` (Swing) y `BaseDatos`, `ProductoDAO`, `PedidoDAO` (JDBC) |

Proyecto Integrador (AA4) — Programación Orientada a Objetos II — Grupo 9.

## Integrantes

| Integrante | Usuario de GitHub |
|---|---|
| Carlos Andrés Castrillón Ceballos | carloscastrillon62-coder |
| Ximena Murillo Granada | ximenamurillo-coder |
| José Ángel Higuita Puerta | josehiguitap-hub |

## Requisitos

- JDK 17 o superior.
- Driver de SQLite incluido en `lib/sqlite-jdbc-3.42.0.1.jar` (no es un framework: es el
  driver JDBC que permite a Java comunicarse con SQLite).

## Estructura

```
CafeteriaDescuentos/
├── src/cafeteria/
│   ├── Producto.java           Clase abstracta base
│   ├── Bebida.java             Hereda de Producto (ml, caliente/fría)
│   ├── Comida.java             Hereda de Producto (gramos, vegetariana)
│   ├── ReglaDescuento.java     @FunctionalInterface para las lambdas
│   ├── CatalogoReglas.java     Promociones como lambdas + funciones de orden superior
│   ├── Pedido.java             Pedido y cálculo del subtotal con streams
│   ├── ItemPedido.java         Record: producto + cantidad
│   ├── ResultadoDescuento.java Record: subtotal, descuento y total
│   ├── BaseDatos.java          Conexión JDBC y ejecución de los scripts
│   ├── ProductoDAO.java        Consultas del catálogo (PreparedStatement)
│   ├── PedidoDAO.java          Guarda pedidos en una transacción
│   ├── VentanaCafeteria.java   Ventana Swing (JFrame) y main
│   └── PruebasCafeteria.java   12 pruebas de consola, sin frameworks
├── sql/
│   ├── 01_crear_tablas.sql     productos, pedidos y pedido_items
│   └── 02_poblar_datos.sql     16 productos de ejemplo
├── docs/
│   ├── diagrama_clases.mmd     Fuente del diagrama de clases (Mermaid)
│   └── diagrama_clases.png     Diagrama de clases UML
├── lib/sqlite-jdbc-3.42.0.1.jar
├── compilar.bat  ejecutar.bat  pruebas.bat
└── README.md
```

## Compilar y ejecutar (Windows)

Doble clic en `compilar.bat` y luego en `ejecutar.bat`, o desde la terminal, ubicado en la
carpeta del proyecto:

```powershell
javac -encoding UTF-8 -d out src\cafeteria\*.java
java -cp "out;lib\sqlite-jdbc-3.42.0.1.jar" cafeteria.VentanaCafeteria
```

Pruebas (doble clic en `pruebas.bat`):

```powershell
java -cp "out;lib\sqlite-jdbc-3.42.0.1.jar" cafeteria.PruebasCafeteria
```

En Linux o macOS cambie `;` por `:` en el classpath y `\` por `/` en las rutas.

## Crear la base de datos

La base de datos es el archivo `cafeteria.db` (SQLite) y **no se sube al repositorio**.
Cada persona la crea con los scripts de la carpeta `sql`, de una de estas dos formas:

- **Desde la aplicación:** abrir la ventana y presionar **Crear BD con scripts**.
- **Desde DBeaver:** crear una conexión SQLite al archivo `cafeteria.db` de la carpeta del
  proyecto y ejecutar `01_crear_tablas.sql` y después `02_poblar_datos.sql`.

## Cómo se usa

1. Al iniciar, la aplicación prueba la conexión JDBC y escribe el resultado en el área de
   texto: en verde si funcionó, en rojo con el mensaje técnico si falló.
2. Se seleccionan productos del catálogo y se agregan al pedido con la cantidad deseada.
3. El **monto total** se llena solo con el subtotal del pedido, pero también se puede
   escribir a mano.
4. Se elige una **promoción** y se presiona **Calcular Descuento**.
5. Con **Guardar pedido** el pedido queda registrado en la base de datos.

## Promociones disponibles

| Promoción | Regla (lambda) |
|---|---|
| Sin descuento | `subtotal -> 0` |
| General (compra mayor a $10.000) | `subtotal -> subtotal > 10000 ? subtotal * 0.15 : 0` |
| Estudiante | `subtotal -> subtotal * 0.10` |
| Desayuno (antes de las 10 a. m.) | `subtotal -> subtotal > 8000 ? subtotal * 0.20 : 0` |
| Compra grande (mayor a $50.000) | `subtotal -> subtotal > 50000 ? subtotal * 0.25 : subtotal * 0.05` |
| Día del café | Combina las dos mejores con `maximo(...)` y `conTope(..., 20000)` |

Para **agregar una promoción nueva** basta con llamar a
`CatalogoReglas.registrar("Nombre", "Descripción", subtotal -> ...)`, sin modificar
ninguna regla existente ni la ventana: eso es el principio abierto/cerrado de SOLID.

## Ramas del repositorio

| Rama | Contenido |
|---|---|
| `main` | Versión estable del proyecto |
| `feature/modelo-poo` | Clases Producto, Bebida, Comida y el diagrama de clases |
| `feature/descuentos-lambda` | Interfaz funcional, catálogo de reglas y pruebas |
| `feature/gui-persistencia` | Ventana Swing, scripts SQL y DAO con JDBC |
| `docs/informe` | README y el informe en PDF |
