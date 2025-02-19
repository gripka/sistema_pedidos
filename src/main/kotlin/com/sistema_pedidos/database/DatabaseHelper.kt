package com.sistema_pedidos.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class DatabaseHelper {
    private val dbPath = "jdbc:sqlite:pedidos.db"

    init {
        createTables()
    }

    private fun getConnection(): Connection {
        return DriverManager.getConnection(dbPath)
    }

    private fun createTables() {
        val queries = arrayOf(
            """CREATE TABLE IF NOT EXISTS clientes (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            nome TEXT NOT NULL,
            sobrenome TEXT NOT NULL,
            telefone TEXT,
            observacao TEXT
        )""",

            """CREATE TABLE IF NOT EXISTS pedidos (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            numero_pedido TEXT,
            cliente_id INTEGER,
            data_pedido TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            total DECIMAL(10,2),
            data_entrega TIMESTAMP,
            hora_entrega TIMESTAMP,
            FOREIGN KEY (cliente_id) REFERENCES clientes(id)
        )""",

            """CREATE TABLE IF NOT EXISTS itens_pedido (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            pedido_id INTEGER,
            produto TEXT NOT NULL,
            quantidade INTEGER NOT NULL,
            valor_unitario DECIMAL(10,2) NOT NULL,
            subtotal DECIMAL(10,2) NOT NULL,
            FOREIGN KEY (pedido_id) REFERENCES pedidos(id)
        )""",

            """CREATE TABLE IF NOT EXISTS produtos (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            nome TEXT NOT NULL,
            quantidade INTEGER NOT NULL,
            valor_unitario DECIMAL(10,2) NOT NULL,
            descricao TEXT,
            categoria TEXT,
            status TEXT DEFAULT 'ativo'
        )""",

            """CREATE TABLE IF NOT EXISTS pagamentos (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            pedido_id INTEGER,
            forma_pagamento TEXT NOT NULL,
            valor_pago DECIMAL(10,2) NOT NULL,
            data_pagamento TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            status TEXT NOT NULL,
            FOREIGN KEY (pedido_id) REFERENCES pedidos(id)
        )""",

            """CREATE TABLE IF NOT EXISTS notificacoes (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            pedido_id INTEGER,
            tipo_notificacao TEXT NOT NULL,
            mensagem TEXT NOT NULL,
            data_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            lido BOOLEAN DEFAULT FALSE,
            FOREIGN KEY (pedido_id) REFERENCES pedidos(id)
        )""",

            """CREATE TABLE IF NOT EXISTS usuarios (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            nome_usuario TEXT NOT NULL,
            senha TEXT NOT NULL,
            role TEXT NOT NULL,
            ativo BOOLEAN DEFAULT TRUE
        )""",

            """CREATE TABLE IF NOT EXISTS log_usuarios (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            usuario_id INTEGER,
            acao TEXT NOT NULL,
            data_acao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
        )""",

            """CREATE TABLE IF NOT EXISTS descontos (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            descricao TEXT NOT NULL,
            tipo TEXT NOT NULL,
            valor DECIMAL(10,2) NOT NULL,
            ativo BOOLEAN DEFAULT TRUE
        )""",

            """CREATE TABLE IF NOT EXISTS enderecos (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            cliente_id INTEGER,
            endereco TEXT NOT NULL,
            referencia TEXT,
            cidade TEXT NOT NULL,
            estado TEXT NOT NULL,
            cep TEXT,
            FOREIGN KEY (cliente_id) REFERENCES clientes(id)
        )"""
        )

        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                queries.forEach { query ->
                    stmt.execute(query)
                }
            }
        }
    }

    // Move listTables outside of createTables
    fun listTables(): List<String> {
        val tables = mutableListOf<String>()
        val query = "SELECT name FROM sqlite_master WHERE type='table'"

        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(query)
                while (rs.next()) {
                    tables.add(rs.getString("name"))
                }
            }
        }
        return tables
    }
}