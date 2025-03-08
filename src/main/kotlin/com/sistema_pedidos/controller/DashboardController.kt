package com.sistema_pedidos.controller

import com.sistema_pedidos.database.DatabaseHelper
import java.sql.Connection
import java.sql.SQLException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DashboardController {
    private val dbHelper = DatabaseHelper()

    fun getTotalPedidosHoje(): Int {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return try {
            dbHelper.getConnection().use { conn ->
                val query = "SELECT COUNT(*) FROM pedidos WHERE date(data_pedido) = ?"
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, today)
                    val rs = stmt.executeQuery()
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            0
        }
    }

    // In DashboardController.kt
    fun getTotalEntregasRealizadas(): Int {
        // Count orders with delivery that are completed
        try {
            val query = """
            SELECT COUNT(*) as total 
            FROM pedidos p
            JOIN entregas e ON p.id = e.pedido_id
            WHERE p.status_pedido = 'Concluido'
        """

            DatabaseHelper().getConnection().use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    val resultSet = stmt.executeQuery()
                    if (resultSet.next()) {
                        return resultSet.getInt("total")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    fun getTotalPedidosSemEntrega(): Int {
        // Count orders without associated delivery entries
        try {
            val query = """
            SELECT COUNT(*) as total 
            FROM pedidos p
            WHERE NOT EXISTS (SELECT 1 FROM entregas e WHERE e.pedido_id = p.id)
            AND p.status_pedido != 'Cancelado'
        """

            DatabaseHelper().getConnection().use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    val resultSet = stmt.executeQuery()
                    if (resultSet.next()) {
                        return resultSet.getInt("total")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    fun getTotalPedidosCancelados(): Int {
        // Count cancelled orders
        try {
            val query = """
            SELECT COUNT(*) as total 
            FROM pedidos 
            WHERE status_pedido = 'Cancelado'
        """

            DatabaseHelper().getConnection().use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    val resultSet = stmt.executeQuery()
                    if (resultSet.next()) {
                        return resultSet.getInt("total")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    fun getTotalDescontosAplicados(): Double {
        // Sum of all discounts applied
        try {
            val query = """
            SELECT SUM(valor_desconto) as total 
            FROM pedidos 
            WHERE valor_desconto > 0
        """

            DatabaseHelper().getConnection().use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    val resultSet = stmt.executeQuery()
                    if (resultSet.next()) {
                        return resultSet.getDouble("total")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0.0
    }

    fun getProdutosMaisVendidosAno(): List<Map<String, Any>> {
        val produtos = mutableListOf<Map<String, Any>>()

        try {
            val now = java.time.LocalDate.now()
            val firstDayOfYear = now.withDayOfYear(1).toString()
            val lastDayOfYear = now.withDayOfYear(now.lengthOfYear()).toString()

            val query = """
        SELECT p.codigo, p.nome, 
            SUM(ip.quantidade) as quantidade_vendida, 
            SUM(ip.subtotal) as valor_total
        FROM produtos p
        JOIN itens_pedido ip ON p.id = ip.produto_id
        JOIN pedidos ped ON ip.pedido_id = ped.id
        WHERE ped.status_pedido != 'Cancelado'
        AND ip.subtotal > 0
        AND ped.data_pedido BETWEEN ? AND ?
        GROUP BY p.id
        ORDER BY quantidade_vendida DESC
        LIMIT 5
        """

            DatabaseHelper().getConnection().use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, firstDayOfYear)
                    stmt.setString(2, lastDayOfYear)

                    val resultSet = stmt.executeQuery()
                    while (resultSet.next()) {
                        produtos.add(mapOf(
                            "codigo" to resultSet.getString("codigo"),
                            "nome" to resultSet.getString("nome"),
                            "quantidade_vendida" to resultSet.getInt("quantidade_vendida"),
                            "valor_total" to resultSet.getDouble("valor_total")
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return produtos
    }

    fun getProdutosMaisVendidosSemana(): List<Map<String, Any>> {
        val produtos = mutableListOf<Map<String, Any>>()

        try {
            // Get the first and last day of the current week
            val now = java.time.LocalDate.now()
            val firstDayOfWeek = now.minusDays(now.dayOfWeek.value - 1L).toString()
            val lastDayOfWeek = now.plusDays(7L - now.dayOfWeek.value).toString()

            val query = """
        SELECT p.codigo, p.nome, 
            SUM(ip.quantidade) as quantidade_vendida, 
            SUM(ip.subtotal) as valor_total
        FROM produtos p
        JOIN itens_pedido ip ON p.id = ip.produto_id
        JOIN pedidos ped ON ip.pedido_id = ped.id
        WHERE ped.status_pedido != 'Cancelado'
        AND ip.subtotal > 0
        AND ped.data_pedido BETWEEN ? AND ?
        GROUP BY p.id
        ORDER BY quantidade_vendida DESC
        LIMIT 5
        """

            DatabaseHelper().getConnection().use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, firstDayOfWeek)
                    stmt.setString(2, lastDayOfWeek)

                    val resultSet = stmt.executeQuery()
                    while (resultSet.next()) {
                        produtos.add(mapOf(
                            "codigo" to resultSet.getString("codigo"),
                            "nome" to resultSet.getString("nome"),
                            "quantidade_vendida" to resultSet.getInt("quantidade_vendida"),
                            "valor_total" to resultSet.getDouble("valor_total")
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return produtos
    }

    fun getProdutosMaisVendidos(): List<Map<String, Any>> {
        val produtos = mutableListOf<Map<String, Any>>()

        try {
            val now = java.time.LocalDate.now()
            val firstDayOfMonth = now.withDayOfMonth(1).toString()
            val lastDayOfMonth = now.withDayOfMonth(now.lengthOfMonth()).toString()

            val query = """
        SELECT p.codigo, p.nome, 
            SUM(ip.quantidade) as quantidade_vendida, 
            SUM(ip.subtotal) as valor_total
        FROM produtos p
        JOIN itens_pedido ip ON p.id = ip.produto_id
        JOIN pedidos ped ON ip.pedido_id = ped.id
        WHERE ped.status_pedido != 'Cancelado'
        AND ip.subtotal > 0
        AND ped.data_pedido BETWEEN ? AND ?
        GROUP BY p.id
        ORDER BY quantidade_vendida DESC
        LIMIT 5
        """

            DatabaseHelper().getConnection().use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, firstDayOfMonth)
                    stmt.setString(2, lastDayOfMonth)

                    val resultSet = stmt.executeQuery()
                    while (resultSet.next()) {
                        produtos.add(mapOf(
                            "codigo" to resultSet.getString("codigo"),
                            "nome" to resultSet.getString("nome"),
                            "quantidade_vendida" to resultSet.getInt("quantidade_vendida"),
                            "valor_total" to resultSet.getDouble("valor_total")
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return produtos
    }

    fun getTicketMedio(): Double {
        return try {
            dbHelper.getConnection().use { conn ->
                val query = """
                    SELECT AVG(valor_total) as ticket_medio 
                    FROM pedidos 
                    WHERE status_pedido != 'Cancelado'
                    AND valor_total > 0
                """
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(query)
                    if (rs.next()) rs.getDouble("ticket_medio") else 0.0
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            0.0
        }
    }

    fun getTotalProdutosCadastrados(): Int {
        return try {
            dbHelper.getConnection().use { conn ->
                val query = "SELECT COUNT(*) FROM produtos WHERE status = 'Ativo'"
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(query)
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            0
        }
    }

    fun getTotalProdutosEstoqueBaixo(): Int {
        return try {
            dbHelper.getConnection().use { conn ->
                val query = "SELECT COUNT(*) FROM produtos WHERE estoque_atual <= estoque_minimo AND estoque_atual > 0 AND status = 'Ativo'"
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(query)
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            0
        }
    }

    fun getTotalEntradasMes(): Double {
        return try {
            dbHelper.getConnection().use { conn ->
                val query = """
                    SELECT SUM(valor_total) as total 
                    FROM pedidos 
                    WHERE status = 'Pago'
                    AND valor_total > 0
                    AND strftime('%Y-%m', data_pedido) = strftime('%Y-%m', 'now')
                """
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(query)
                    if (rs.next()) rs.getDouble("total") else 0.0
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            0.0
        }
    }

    fun getTotalPedidosConcluidos(): Int {
        return try {
            dbHelper.getConnection().use { conn ->
                val query = "SELECT COUNT(*) FROM pedidos WHERE status_pedido = 'Concluido'"
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(query)
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            0
        }
    }

    fun getEntregasHoje(): Int {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return try {
            dbHelper.getConnection().use { conn ->
                val query = """
                SELECT COUNT(*) 
                FROM entregas e
                JOIN pedidos p ON e.pedido_id = p.id
                WHERE date(e.data_entrega) = ?
                AND p.status_pedido != 'Cancelado'
            """
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, today)
                    val rs = stmt.executeQuery()
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            0
        }
    }

    fun getTotalProdutosSemEstoque(): Int {
        return try {
            dbHelper.getConnection().use { conn ->
                val query = "SELECT COUNT(*) FROM produtos WHERE estoque_atual = 0 AND status = 'Ativo'"
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(query)
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            0
        }
    }

    fun getValorTotalHoje(): Double {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return try {
            dbHelper.getConnection().use { conn ->
                val query = """
                    SELECT SUM(valor_total) FROM pedidos 
                    WHERE date(data_pedido) = ? 
                    AND status = 'Pago'
                    AND valor_total > 0
                    """
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, today)
                    val rs = stmt.executeQuery()
                    if (rs.next()) rs.getDouble(1) else 0.0
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            0.0
        }
    }

    fun getTotalClientes(): Int {
        return try {
            dbHelper.getConnection().use { conn ->
                val query = "SELECT COUNT(*) FROM clientes"
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(query)
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            0
        }
    }

    fun getItensBaixoEstoque(): List<Map<String, Any>> {
        return try {
            dbHelper.getConnection().use { conn ->
                val query = """
                    SELECT id, nome, codigo, estoque_atual, estoque_minimo 
                    FROM produtos 
                    WHERE estoque_atual <= estoque_minimo AND status = 'Ativo'
                    ORDER BY (estoque_atual * 1.0 / CASE WHEN estoque_minimo = 0 THEN 1 ELSE estoque_minimo END) ASC
                    LIMIT 10
                """
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(query)
                    val items = mutableListOf<Map<String, Any>>()
                    while (rs.next()) {
                        items.add(mapOf(
                            "id" to rs.getInt("id"),
                            "nome" to rs.getString("nome"),
                            "codigo" to rs.getString("codigo"),
                            "estoque_atual" to rs.getInt("estoque_atual"),
                            "estoque_minimo" to rs.getInt("estoque_minimo")
                        ))
                    }
                    items
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getPedidosPendentes(): List<Map<String, Any>> {
        return try {
            dbHelper.getConnection().use { conn ->
                val query = """
                    SELECT p.id, p.numero, p.valor_total, p.data_pedido, p.status_pedido, 
                           COALESCE(c.nome, c.razao_social, 'Cliente não cadastrado') as cliente
                    FROM pedidos p 
                    LEFT JOIN clientes c ON p.cliente_id = c.id
                    WHERE p.status_pedido IN ('Pendente', 'Preparando', 'Em Entrega')
                    ORDER BY p.data_pedido DESC
                    LIMIT 10
                """
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(query)
                    val items = mutableListOf<Map<String, Any>>()
                    while (rs.next()) {
                        items.add(mapOf(
                            "id" to rs.getInt("id"),
                            "numero" to rs.getString("numero"),
                            "valor_total" to rs.getDouble("valor_total"),
                            "data_pedido" to rs.getString("data_pedido"),
                            "status_pedido" to rs.getString("status_pedido"),
                            "cliente" to rs.getString("cliente")
                        ))
                    }
                    items
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getVendasUltimos30Dias(): Map<String, Double> {
        return try {
            dbHelper.getConnection().use { conn ->
                val query = """
                SELECT date(data_pedido) as data, SUM(valor_total) as total
                FROM pedidos
                WHERE data_pedido >= date('now', '-30 days')
                AND status = 'Pago'
                AND valor_total > 0
                GROUP BY date(data_pedido)
                ORDER BY data_pedido
            """
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(query)
                    val vendas = mutableMapOf<String, Double>()
                    while (rs.next()) {
                        vendas[rs.getString("data")] = rs.getDouble("total")
                    }
                    vendas
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            emptyMap()
        }
    }

    fun getVendasUltimos7Dias(): Map<String, Double> {
        return try {
            dbHelper.getConnection().use { conn ->
                val query = """
                    SELECT date(data_pedido) as data, SUM(valor_total) as total
                    FROM pedidos
                    WHERE data_pedido >= date('now', '-7 days')
                    AND status = 'Pago'
                    AND valor_total > 0
                    GROUP BY date(data_pedido)
                    ORDER BY data_pedido
                """
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(query)
                    val vendas = mutableMapOf<String, Double>()
                    while (rs.next()) {
                        vendas[rs.getString("data")] = rs.getDouble("total")
                    }
                    vendas
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            emptyMap()
        }
    }
}