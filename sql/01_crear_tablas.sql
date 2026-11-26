-- =====================================================================
--  Script 1: creación de la base de datos "cafeteria"
--  Motor: SQLite 3
--  AA4 - Proyecto Integrador - Programación Orientada a Objetos II - Grupo 9
--
--  En SQLite la base de datos es un archivo. No se usa CREATE DATABASE:
--  el archivo cafeteria.db se crea solo al conectarse con la URL
--  jdbc:sqlite:cafeteria.db (desde la aplicación o desde DBeaver).
--
--  Este script borra las tablas si ya existen y las crea de nuevo vacías.
-- =====================================================================

PRAGMA foreign_keys = ON;

DROP TABLE IF EXISTS pedido_items;
DROP TABLE IF EXISTS pedidos;
DROP TABLE IF EXISTS productos;

-- Productos de la cafetería. La columna tipo indica si la fila se carga
-- como un objeto Bebida o como un objeto Comida (herencia).
CREATE TABLE productos (
    id           INTEGER        PRIMARY KEY AUTOINCREMENT,
    codigo       VARCHAR(20)    NOT NULL UNIQUE,
    nombre       VARCHAR(100)   NOT NULL,
    tipo         VARCHAR(10)    NOT NULL CHECK (tipo IN ('BEBIDA', 'COMIDA')),
    precio       DECIMAL(12, 2) NOT NULL CHECK (precio >= 0),
    mililitros   INTEGER,                      -- solo para las bebidas
    caliente     INTEGER        DEFAULT 0,     -- solo para las bebidas (0 = no, 1 = sí)
    gramos       INTEGER,                      -- solo para las comidas
    vegetariana  INTEGER        DEFAULT 0      -- solo para las comidas (0 = no, 1 = sí)
);

-- Pedidos guardados, con la regla de descuento que se les aplicó.
CREATE TABLE pedidos (
    id         INTEGER        PRIMARY KEY AUTOINCREMENT,
    fecha      TEXT           NOT NULL,
    subtotal   DECIMAL(12, 2) NOT NULL CHECK (subtotal >= 0),
    regla      VARCHAR(80)    NOT NULL,
    descuento  DECIMAL(12, 2) NOT NULL CHECK (descuento >= 0),
    total      DECIMAL(12, 2) NOT NULL CHECK (total >= 0)
);

-- Productos que componen cada pedido (detalle del pedido).
CREATE TABLE pedido_items (
    id               INTEGER        PRIMARY KEY AUTOINCREMENT,
    id_pedido        INTEGER        NOT NULL,
    id_producto      INTEGER        NOT NULL,
    cantidad         INTEGER        NOT NULL CHECK (cantidad > 0),
    precio_unitario  DECIMAL(12, 2) NOT NULL CHECK (precio_unitario >= 0),
    FOREIGN KEY (id_pedido)   REFERENCES pedidos (id),
    FOREIGN KEY (id_producto) REFERENCES productos (id)
);
