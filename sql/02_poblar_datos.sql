-- =====================================================================
--  Script 2: poblado de la base de datos "cafeteria"
--  Motor: SQLite 3
--  AA4 - Proyecto Integrador - Programación Orientada a Objetos II - Grupo 9
--
--  Ejecutar después de 01_crear_tablas.sql.
--  Inserta 16 productos de ejemplo (precios en pesos colombianos):
--  9 bebidas y 7 comidas.
-- =====================================================================

PRAGMA foreign_keys = ON;

-- Bebidas: se usan las columnas mililitros y caliente
INSERT INTO productos (codigo, nombre, tipo, precio, mililitros, caliente) VALUES
    ('BEB-001', 'Tinto campesino',          'BEBIDA',  2000, 180, 1),
    ('BEB-002', 'Café americano',           'BEBIDA',  4500, 240, 1),
    ('BEB-003', 'Capuchino',                'BEBIDA',  7500, 240, 1),
    ('BEB-004', 'Latte vainilla',           'BEBIDA',  8500, 350, 1),
    ('BEB-005', 'Chocolate santafereño',    'BEBIDA',  6500, 300, 1),
    ('BEB-006', 'Aromática de frutas',      'BEBIDA',  3500, 250, 1),
    ('BEB-007', 'Jugo natural de mora',     'BEBIDA',  6000, 400, 0),
    ('BEB-008', 'Limonada de coco',         'BEBIDA',  9000, 400, 0),
    ('BEB-009', 'Café frío con hielo',      'BEBIDA',  8000, 350, 0);

-- Comidas: se usan las columnas gramos y vegetariana
INSERT INTO productos (codigo, nombre, tipo, precio, gramos, vegetariana) VALUES
    ('COM-001', 'Croissant de mantequilla', 'COMIDA',  4500,  90, 1),
    ('COM-002', 'Pandebono',                'COMIDA',  2500,  70, 1),
    ('COM-003', 'Almojábana',               'COMIDA',  2800,  80, 1),
    ('COM-004', 'Sándwich de jamón y queso','COMIDA', 12000, 220, 0),
    ('COM-005', 'Wrap de pollo',            'COMIDA', 14500, 260, 0),
    ('COM-006', 'Ensalada de frutas',       'COMIDA',  9500, 300, 1),
    ('COM-007', 'Porción de torta de zanahoria', 'COMIDA', 7500, 120, 1);

-- Verificación: productos ordenados por tipo y código
SELECT codigo, nombre, tipo, precio
FROM productos
ORDER BY tipo, codigo;
