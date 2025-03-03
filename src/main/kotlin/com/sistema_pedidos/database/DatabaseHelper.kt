package com.sistema_pedidos.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*

class DatabaseHelper {
    companion object {
        init {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))
            System.setProperty("user.timezone", "America/Sao_Paulo")
        }
    }

    private val dbPath = "jdbc:sqlite:pedidos.db"
    private var connection: Connection? = null

    init {
        connection = DriverManager.getConnection(dbPath)
        createTables()
    }

    fun getConnection(): Connection {
        if (connection == null || connection?.isClosed == true) {
            connection = DriverManager.getConnection(dbPath)
        }
        return connection!!
    }

    private fun createTables() {
        val queries = arrayOf(
            """CREATE TABLE IF NOT EXISTS clientes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT,
                sobrenome TEXT,
                telefone TEXT UNIQUE,
                observacao TEXT
            )""",

            """CREATE TABLE IF NOT EXISTS produtos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                codigo TEXT UNIQUE,
                nome TEXT NOT NULL,
                descricao TEXT,
                valor_unitario DECIMAL(10,2) NOT NULL DEFAULT 0,
                categoria TEXT,
                unidade_medida TEXT DEFAULT 'UN' CHECK (unidade_medida IN ('UN', 'KG', 'L', 'M', 'CX')),
                estoque_minimo INTEGER DEFAULT 0,
                estoque_atual INTEGER DEFAULT 0,
                status TEXT CHECK (status IN ('Ativo', 'Inativo')) DEFAULT 'Ativo',
                eh_insumo INTEGER DEFAULT 0 CHECK (eh_insumo IN (0, 1)),
                data_cadastro TIMESTAMP DEFAULT (datetime('now', 'localtime')),
                data_atualizacao TIMESTAMP DEFAULT (datetime('now', 'localtime'))
            )""",

            """CREATE TABLE IF NOT EXISTS pedidos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                numero TEXT UNIQUE NOT NULL,
                cliente_id INTEGER NULL,
                telefone_contato TEXT NOT NULL,
                data_pedido TIMESTAMP DEFAULT (datetime('now', 'localtime')),
                observacao TEXT,
                status TEXT CHECK (status IN ('Pendente', 'Pago', 'Cancelado')) DEFAULT 'Pendente',
                status_pedido TEXT CHECK (status_pedido IN ('Concluido', 'Preparando', 'Pendente', 'Em Entrega', 'Cancelado')) DEFAULT 'Pendente',
                valor_total DECIMAL(10,2) DEFAULT 0.00,
                valor_desconto DECIMAL(10,2) DEFAULT 0.00,
                tipo_desconto TEXT CHECK (tipo_desconto IN ('valor', 'percentual')),
                forma_pagamento TEXT CHECK (forma_pagamento IN ('Dinheiro', 'Cartão de Crédito', 'Cartão de Débito', 'PIX', 'Voucher')),
                valor_troco_para DECIMAL(10,2),
                valor_troco DECIMAL(10,2),
                data_retirada DATE,
                hora_retirada TIME,
                FOREIGN KEY (cliente_id) REFERENCES clientes(id)
            )""",

            """CREATE TABLE IF NOT EXISTS itens_pedido (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                pedido_id INTEGER NOT NULL,
                produto_id INTEGER NULL,
                nome_produto TEXT NOT NULL,
                quantidade INTEGER NOT NULL DEFAULT 1,
                valor_unitario DECIMAL(10,2) NOT NULL,
                subtotal DECIMAL(10,2) NOT NULL,
                FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
                FOREIGN KEY (produto_id) REFERENCES produtos(id)
            )""",

            """CREATE TABLE IF NOT EXISTS entregas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                pedido_id INTEGER NOT NULL UNIQUE,
                nome_destinatario TEXT NOT NULL,
                telefone_destinatario TEXT,
                endereco TEXT NOT NULL,
                referencia TEXT,
                cidade TEXT NOT NULL,
                bairro TEXT NOT NULL,
                cep TEXT,
                valor_entrega DECIMAL(10,2),
                data_entrega DATE NOT NULL,
                hora_entrega TIME NOT NULL,
                FOREIGN KEY (pedido_id) REFERENCES pedidos(id)
            )""",

            """CREATE TABLE IF NOT EXISTS produto_insumos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                produto_id INTEGER NOT NULL,
                insumo_id INTEGER NOT NULL,
                quantidade DECIMAL(10,2) NOT NULL,
                FOREIGN KEY (produto_id) REFERENCES produtos(id),
                FOREIGN KEY (insumo_id) REFERENCES produtos(id),
                UNIQUE(produto_id, insumo_id)
            )""",

            """CREATE TRIGGER IF NOT EXISTS gerar_numero_pedido
                AFTER INSERT ON pedidos
                WHEN NEW.numero IS NULL
                BEGIN
                    UPDATE pedidos 
                    SET numero = 'PED' || substr('000' || NEW.id, -4)
                    WHERE id = NEW.id;
                END
            """,

            """CREATE TRIGGER IF NOT EXISTS gerar_codigo_produto
                AFTER INSERT ON produtos
                WHEN NEW.codigo IS NULL
                BEGIN
                    UPDATE produtos
                    SET codigo = 'PROD' || substr('000000' || NEW.id, -6)
                    WHERE id = NEW.id;
                END
            """,

            """CREATE TRIGGER IF NOT EXISTS atualizar_data_produto
                AFTER UPDATE ON produtos
                BEGIN
                    UPDATE produtos
                    SET data_atualizacao = datetime('now', 'localtime')
                    WHERE id = NEW.id;
                END
            """,
            """CREATE TRIGGER IF NOT EXISTS formatar_valores_monetarios
                BEFORE INSERT ON pedidos
                BEGIN
                    UPDATE pedidos 
                    SET valor_total = ROUND(NEW.valor_total, 2),
                        valor_desconto = ROUND(NEW.valor_desconto, 2),
                        valor_troco_para = ROUND(NEW.valor_troco_para, 2),
                        valor_troco = ROUND(NEW.valor_troco, 2)
                    WHERE id = NEW.id;
                END;
            """,

            """CREATE TRIGGER IF NOT EXISTS formatar_valores_itens
                BEFORE INSERT ON itens_pedido
                BEGIN
                    UPDATE itens_pedido
                    SET valor_unitario = ROUND(NEW.valor_unitario, 2),
                        subtotal = ROUND(NEW.subtotal, 2)
                    WHERE id = NEW.id;
                END;
            """,

            """CREATE TRIGGER IF NOT EXISTS formatar_valores_entregas
                BEFORE INSERT ON entregas
                BEGIN
                    UPDATE entregas
                    SET valor_entrega = ROUND(NEW.valor_entrega, 2)
                    WHERE id = NEW.id;
                END;
            """,

            """CREATE INDEX IF NOT EXISTS idx_cliente_telefone ON clientes(telefone)""",
            """CREATE INDEX IF NOT EXISTS idx_produto_codigo ON produtos(codigo)""",
            """CREATE INDEX IF NOT EXISTS idx_produto_nome ON produtos(nome)""",
            """CREATE INDEX IF NOT EXISTS idx_produto_categoria ON produtos(categoria)""",
            """CREATE INDEX IF NOT EXISTS idx_pedido_numero ON pedidos(numero)""",
            """CREATE INDEX IF NOT EXISTS idx_pedido_cliente ON pedidos(cliente_id)""",
            """CREATE INDEX IF NOT EXISTS idx_item_pedido ON itens_pedido(pedido_id)""",
            """CREATE INDEX IF NOT EXISTS idx_item_produto ON itens_pedido(produto_id)""",
            """CREATE INDEX IF NOT EXISTS idx_entrega_pedido ON entregas(pedido_id)""",
            """CREATE INDEX IF NOT EXISTS idx_produto_insumos_produto ON produto_insumos(produto_id)""",
            """CREATE INDEX IF NOT EXISTS idx_produto_insumos_insumo ON produto_insumos(insumo_id)"""
        )

        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                queries.forEach { query ->
                    stmt.execute(query)
                }
            }
        }
    }

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