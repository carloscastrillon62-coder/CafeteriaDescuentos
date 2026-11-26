package cafeteria;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * AA4 - Proyecto Integrador. Ventana principal de la cafetería.
 *
 * Es la <b>vista</b> de la aplicación: muestra el catálogo, deja armar un
 * pedido, aplica una promoción y guarda el resultado. No contiene SQL ni
 * fórmulas de descuento; para eso llama al modelo ({@link Pedido},
 * {@link CatalogoReglas}) y a los DAO ({@link ProductoDAO},
 * {@link PedidoDAO}). Esa separación es la que da bajo acoplamiento.
 *
 * Componentes que pide la actividad:
 *  - JFrame con JPanel y layouts (BorderLayout y GridLayout).
 *  - JTextField para el monto total y JButton "Calcular Descuento".
 *  - JTextArea dentro de un JScrollPane para el resultado y los mensajes de
 *    conexión a la base de datos.
 *
 * Al iniciar, la aplicación intenta la conexión JDBC y escribe en el
 * JTextArea si funcionó (en verde) o cuál fue el error (en rojo).
 */
public final class VentanaCafeteria extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static final Color VERDE = new Color(0, 128, 0);
    private static final Color ROJO = new Color(190, 0, 0);
    private static final Color GRIS = new Color(90, 90, 90);
    private static final Path SCRIPT_CREAR = Path.of("sql", "01_crear_tablas.sql");
    private static final Path SCRIPT_POBLAR = Path.of("sql", "02_poblar_datos.sql");
    private static final NumberFormat PESOS = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));

    static {
        PESOS.setMaximumFractionDigits(0);
    }

    // ------------------------------------------------------------------
    // Modelo y acceso a datos (encapsulados como private final)
    // ------------------------------------------------------------------
    private final transient BaseDatos baseDatos;
    private final transient ProductoDAO productoDAO;
    private final transient PedidoDAO pedidoDAO;
    private final transient Pedido pedido = new Pedido();
    private transient List<Producto> catalogo = List.of();

    // ------------------------------------------------------------------
    // Componentes de la interfaz
    // ------------------------------------------------------------------
    private final JTextField campoBuscar = new JTextField(20);
    private final JButton botonScripts = new JButton("Crear BD con scripts");

    private final DefaultTableModel modeloCatalogo = new DefaultTableModel(
            new Object[] {"ID", "Código", "Nombre", "Tipo", "Detalle", "Precio"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tablaCatalogo = new JTable(modeloCatalogo);
    private final JSpinner cantidad = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));

    private final DefaultTableModel modeloPedido = new DefaultTableModel(
            new Object[] {"Producto", "Cantidad", "Precio", "Subtotal"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tablaPedido = new JTable(modeloPedido);

    private final JTextField campoMonto = new JTextField(12);
    private final JComboBox<CatalogoReglas.Promocion> comboPromocion = new JComboBox<>();
    private final JButton botonCalcular = new JButton("Calcular Descuento");
    private final JButton botonGuardar = new JButton("Guardar pedido");
    private final JTextArea areaResultado = new JTextArea(9, 60);
    private final JLabel etiquetaResumen = new JLabel(" ");

    public VentanaCafeteria(String url) {
        super("AA4 - Cafetería: cálculo de descuentos (Swing + JDBC)");
        this.baseDatos = new BaseDatos(url);
        this.productoDAO = new ProductoDAO(baseDatos);
        this.pedidoDAO = new PedidoDAO(baseDatos);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(construirPanelPrincipal());
        botonCalcular.addActionListener(this);          // la ventana escucha el botón principal
        getRootPane().setDefaultButton(botonCalcular);  // Enter también calcula
        pack();
        setLocationRelativeTo(null);
        iniciarConexion();
    }

    // ==================================================================
    // 1. Construcción de la interfaz
    // ==================================================================
    private JPanel construirPanelPrincipal() {
        JPanel principal = new JPanel(new BorderLayout(10, 10));
        principal.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        principal.add(construirBarraBusqueda(), BorderLayout.NORTH);
        principal.add(construirTablas(), BorderLayout.CENTER);
        principal.add(construirPanelDescuento(), BorderLayout.SOUTH);
        return principal;
    }

    private JPanel construirBarraBusqueda() {
        JButton botonBuscar = new JButton("Buscar");
        JButton botonTodos = new JButton("Mostrar todos");
        botonBuscar.addActionListener(e -> cargarCatalogo(campoBuscar.getText()));
        campoBuscar.addActionListener(e -> cargarCatalogo(campoBuscar.getText()));
        botonTodos.addActionListener(e -> {
            campoBuscar.setText("");
            cargarCatalogo("");
        });
        botonScripts.addActionListener(this::crearBaseDatos);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        barra.add(new JLabel("Buscar producto (código, nombre o tipo):"));
        barra.add(campoBuscar);
        barra.add(botonBuscar);
        barra.add(botonTodos);
        barra.add(botonScripts);
        return barra;
    }

    /** Dos tablas lado a lado con un GridLayout: catálogo y pedido. */
    private JPanel construirTablas() {
        tablaCatalogo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaCatalogo.setRowHeight(22);
        tablaCatalogo.getTableHeader().setReorderingAllowed(false);
        anchos(tablaCatalogo, 35, 70, 190, 65, 120, 90);
        derecha(tablaCatalogo, 5);
        JScrollPane scrollCatalogo = new JScrollPane(tablaCatalogo);
        scrollCatalogo.setBorder(BorderFactory.createTitledBorder("Catálogo de productos"));
        scrollCatalogo.setPreferredSize(new Dimension(590, 250));

        JButton botonAgregar = new JButton("Agregar al pedido");
        botonAgregar.addActionListener(e -> agregarAlPedido());
        JPanel accionesCatalogo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        accionesCatalogo.add(new JLabel("Cantidad:"));
        accionesCatalogo.add(cantidad);
        accionesCatalogo.add(botonAgregar);

        JPanel panelCatalogo = new JPanel(new BorderLayout(0, 4));
        panelCatalogo.add(scrollCatalogo, BorderLayout.CENTER);
        panelCatalogo.add(accionesCatalogo, BorderLayout.SOUTH);

        tablaPedido.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaPedido.setRowHeight(22);
        tablaPedido.getTableHeader().setReorderingAllowed(false);
        anchos(tablaPedido, 230, 70, 90, 100);
        derecha(tablaPedido, 1, 2, 3);
        JScrollPane scrollPedido = new JScrollPane(tablaPedido);
        scrollPedido.setBorder(BorderFactory.createTitledBorder("Pedido actual"));
        scrollPedido.setPreferredSize(new Dimension(430, 250));

        JButton botonQuitar = new JButton("Quitar línea");
        JButton botonVaciar = new JButton("Vaciar pedido");
        botonQuitar.addActionListener(e -> quitarDelPedido());
        botonVaciar.addActionListener(e -> {
            pedido.limpiar();
            refrescarPedido();
            mostrarInfo("Pedido vaciado.");
        });
        JPanel accionesPedido = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        accionesPedido.add(botonQuitar);
        accionesPedido.add(botonVaciar);

        etiquetaResumen.setFont(etiquetaResumen.getFont().deriveFont(Font.BOLD));
        JPanel piePedido = new JPanel(new BorderLayout());
        piePedido.add(etiquetaResumen, BorderLayout.WEST);
        piePedido.add(accionesPedido, BorderLayout.EAST);

        JPanel panelPedido = new JPanel(new BorderLayout(0, 4));
        panelPedido.add(scrollPedido, BorderLayout.CENTER);
        panelPedido.add(piePedido, BorderLayout.SOUTH);

        JPanel tablas = new JPanel(new GridLayout(1, 2, 10, 0));
        tablas.add(panelCatalogo);
        tablas.add(panelPedido);
        return tablas;
    }

    /** Formulario del descuento y área de resultado. */
    private JPanel construirPanelDescuento() {
        for (CatalogoReglas.Promocion promocion : CatalogoReglas.getPromociones()) {
            comboPromocion.addItem(promocion);
        }
        comboPromocion.setSelectedIndex(1);                 // la regla general del enunciado
        comboPromocion.addActionListener(e -> mostrarDescripcionPromocion());
        campoMonto.setText("0");
        campoMonto.setHorizontalAlignment(SwingConstants.RIGHT);
        botonGuardar.addActionListener(e -> guardarPedido());

        JPanel formulario = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        formulario.add(new JLabel("Monto total (pesos):"));
        formulario.add(campoMonto);
        formulario.add(new JLabel("   Promoción:"));
        formulario.add(comboPromocion);
        formulario.add(botonCalcular);
        formulario.add(botonGuardar);

        areaResultado.setEditable(false);
        areaResultado.setLineWrap(true);
        areaResultado.setWrapStyleWord(true);
        areaResultado.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        areaResultado.setForeground(GRIS);
        JScrollPane scroll = new JScrollPane(areaResultado);
        scroll.setBorder(BorderFactory.createTitledBorder("Resultado y mensajes de conexión"));

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(formulario, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private static void anchos(JTable tabla, int... valores) {
        for (int i = 0; i < valores.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(valores[i]);
        }
    }

    private static void derecha(JTable tabla, int... columnas) {
        DefaultTableCellRenderer render = new DefaultTableCellRenderer();
        render.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int columna : columnas) {
            tabla.getColumnModel().getColumn(columna).setCellRenderer(render);
        }
    }

    // ==================================================================
    // 2. Conexión a la base de datos al iniciar (JDBC + try-catch)
    // ==================================================================
    private void iniciarConexion() {
        try {
            Class.forName("org.sqlite.JDBC");                 // carga del driver
            String mensaje = baseDatos.probarConexion();      // usa try-with-resources
            if (!baseDatos.existenTablas()) {
                mostrarError("ATENCIÓN: la base de datos todavía no tiene tablas.\n"
                        + "Presione \"Crear BD con scripts\" para crearlas y cargar los productos.\n\n"
                        + mensaje);
                return;
            }
            mostrarExito(mensaje);
            cargarCatalogo("");
        } catch (ClassNotFoundException e) {
            mostrarError("FALLO DE CONEXIÓN: DRIVER NO ENCONTRADO\n\n"
                    + "Clase del driver: " + e.getMessage() + "\n"
                    + "Agregue el archivo sqlite-jdbc.jar de la carpeta lib al classpath.");
        } catch (NoClassDefFoundError e) {
            mostrarError("FALLO DE CONEXIÓN: DRIVER INCOMPLETO\n\n"
                    + "El driver necesita otra librería que no está en el classpath: " + e.getMessage());
        } catch (SQLException e) {
            mostrarError("FALLO DE CONEXIÓN\n\n"
                    + "Mensaje técnico: " + e.getMessage() + "\n"
                    + "SQLState: " + e.getSQLState() + "   |   Código de error: " + e.getErrorCode());
        }
    }

    /** Ejecuta los dos scripts de la carpeta sql. */
    private void crearBaseDatos(ActionEvent evento) {
        if (!Files.exists(SCRIPT_CREAR) || !Files.exists(SCRIPT_POBLAR)) {
            mostrarError("No se encontró la carpeta sql con los scripts. "
                    + "Ejecute la aplicación desde la carpeta del proyecto.");
            return;
        }
        int respuesta = JOptionPane.showConfirmDialog(this,
                "Se crearán de nuevo las tablas y se cargarán los productos de ejemplo.\n"
                        + "Los datos actuales se borrarán. ¿Desea continuar?",
                "Crear base de datos", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            int sentencias = baseDatos.ejecutarScript(SCRIPT_CREAR) + baseDatos.ejecutarScript(SCRIPT_POBLAR);
            pedido.limpiar();
            refrescarPedido();
            campoBuscar.setText("");
            cargarCatalogo("");
            mostrarExito("Base de datos creada: se ejecutaron " + sentencias + " sentencias de los scripts SQL.\n"
                    + "El catálogo quedó con " + catalogo.size() + " productos ("
                    + productoDAO.resumenEnTexto() + ").");
        } catch (IOException e) {
            mostrarError("No se pudo leer el script: " + e.getMessage());
        } catch (SQLException e) {
            mostrarError(BaseDatos.mensajeAmigable(e));
        }
    }

    // ==================================================================
    // 3. Catálogo y pedido
    // ==================================================================
    private void cargarCatalogo(String texto) {
        try {
            catalogo = productoDAO.buscar(texto);
            modeloCatalogo.setRowCount(0);
            catalogo.forEach(p -> modeloCatalogo.addRow(new Object[] {p.getId(), p.getCodigo(),
                    p.getNombre(), p.getTipo(), p.getDetalle(), PESOS.format(p.getPrecio())}));
            if (texto != null && !texto.isBlank()) {
                mostrarInfo("Búsqueda \"" + texto.trim() + "\": " + catalogo.size() + " producto(s) encontrado(s).");
            }
        } catch (SQLException e) {
            mostrarError(BaseDatos.mensajeAmigable(e));
        }
    }

    private void agregarAlPedido() {
        int fila = tablaCatalogo.getSelectedRow();
        if (fila < 0) {
            mostrarError("Seleccione en el catálogo el producto que desea agregar.");
            return;
        }
        Producto producto = catalogo.get(fila);
        pedido.agregar(producto, (Integer) cantidad.getValue());
        refrescarPedido();
        mostrarInfo("Se agregaron " + cantidad.getValue() + " x " + producto.getNombre()
                + ". Subtotal del pedido: " + PESOS.format(pedido.getSubtotal()));
    }

    private void quitarDelPedido() {
        int fila = tablaPedido.getSelectedRow();
        if (fila < 0) {
            mostrarError("Seleccione en el pedido la línea que desea quitar.");
            return;
        }
        pedido.quitar(fila);
        refrescarPedido();
        mostrarInfo("Línea quitada del pedido.");
    }

    /** Vuelve a pintar la tabla del pedido y copia el subtotal al campo del monto. */
    private void refrescarPedido() {
        modeloPedido.setRowCount(0);
        pedido.getItems().forEach(item -> modeloPedido.addRow(new Object[] {
                item.producto().getNombre(), item.cantidad(),
                PESOS.format(item.producto().getPrecio()), PESOS.format(item.subtotal())}));
        campoMonto.setText(String.valueOf(Math.round(pedido.getSubtotal())));
        etiquetaResumen.setText(pedido.estaVacio()
                ? "Pedido vacío"
                : String.format("%d unidades (%s)   |   Subtotal: %s",
                        pedido.getUnidades(), pedido.getResumenEnTexto(),
                        PESOS.format(pedido.getSubtotal())));
    }

    // ==================================================================
    // 4. Evento del botón "Calcular Descuento" (ActionListener)
    // ==================================================================
    /**
     * Pasos que pide la actividad: 1) obtener el monto del JTextField,
     * 2) aplicar la lógica lambda y 3) mostrar el resultado en la ventana.
     */
    @Override
    public void actionPerformed(ActionEvent evento) {
        try {
            double monto = leerMonto();                                   // 1. monto del JTextField
            CatalogoReglas.Promocion promocion = (CatalogoReglas.Promocion) comboPromocion.getSelectedItem();
            ResultadoDescuento resultado =
                    Pedido.calcular(monto, promocion.nombre(), promocion.regla());   // 2. lambda
            mostrarExito(formatear(resultado, promocion));                // 3. mostrar en la GUI
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    /** Lee el monto escrito en el JTextField; acepta 12000, 12.000 o $ 12.000. */
    private double leerMonto() {
        String texto = campoMonto.getText();
        String limpio = texto == null ? "" : texto.replace("$", "").replace(".", "")
                .replace(" ", "").replace(",", ".").trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("Escriba el monto total o arme un pedido en el catálogo.");
        }
        double monto;
        try {
            monto = Double.parseDouble(limpio);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El monto debe ser un número. Ejemplo: 12000");
        }
        if (monto < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo.");
        }
        return monto;
    }

    private String formatear(ResultadoDescuento r, CatalogoReglas.Promocion promocion) {
        return String.format(
                "DESCUENTO CALCULADO%n%n"
                        + "Promoción:  %s%n"
                        + "            %s%n%n"
                        + "Subtotal:   %s%n"
                        + "Descuento:  %s   (%.1f %%)%n"
                        + "TOTAL:      %s",
                promocion.nombre(), promocion.descripcion(),
                PESOS.format(r.subtotal()), PESOS.format(r.descuento()), r.porcentaje(),
                PESOS.format(r.total()));
    }

    private void mostrarDescripcionPromocion() {
        CatalogoReglas.Promocion promocion = (CatalogoReglas.Promocion) comboPromocion.getSelectedItem();
        if (promocion != null) {
            mostrarInfo(promocion.nombre() + ": " + promocion.descripcion());
        }
    }

    // ==================================================================
    // 5. Guardar el pedido en la base de datos
    // ==================================================================
    private void guardarPedido() {
        if (pedido.estaVacio()) {
            mostrarError("El pedido está vacío. Agregue productos del catálogo antes de guardar.");
            return;
        }
        try {
            CatalogoReglas.Promocion promocion = (CatalogoReglas.Promocion) comboPromocion.getSelectedItem();
            ResultadoDescuento resultado = pedido.calcular(promocion.nombre(), promocion.regla());
            int id = pedidoDAO.guardar(pedido, resultado);
            int total = pedidoDAO.contar();
            mostrarExito(String.format(
                    "PEDIDO GUARDADO (número %d)%n%n"
                            + "Productos:  %d líneas, %d unidades%n"
                            + "Promoción:  %s%n"
                            + "Subtotal:   %s%n"
                            + "Descuento:  %s%n"
                            + "TOTAL:      %s%n%n"
                            + "La base de datos tiene %d pedido(s) guardado(s).",
                    id, pedido.getItems().size(), pedido.getUnidades(), resultado.regla(),
                    PESOS.format(resultado.subtotal()), PESOS.format(resultado.descuento()),
                    PESOS.format(resultado.total()), total));
            pedido.limpiar();
            refrescarPedido();
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        } catch (SQLException e) {
            mostrarError(BaseDatos.mensajeAmigable(e));
        }
    }

    // ==================================================================
    // 6. Mensajes en el JTextArea
    // ==================================================================
    private void mostrarExito(String mensaje) {
        escribir(mensaje, VERDE);
    }

    private void mostrarError(String mensaje) {
        escribir(mensaje, ROJO);
    }

    private void mostrarInfo(String mensaje) {
        escribir(mensaje, GRIS);
    }

    private void escribir(String mensaje, Color color) {
        areaResultado.setForeground(color);
        areaResultado.setText(mensaje);
        areaResultado.setCaretPosition(0);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
            // Si no se puede usar el estilo del sistema, Swing usa el suyo por defecto.
        }
        String url = args.length > 0 ? args[0] : BaseDatos.URL_POR_DEFECTO;
        SwingUtilities.invokeLater(() -> new VentanaCafeteria(url).setVisible(true));
    }
}
