package com.sistema_pedidos.controller

import com.sistema_pedidos.database.DatabaseHelper
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.layout.VBox
import java.io.File
import java.io.FileWriter
import java.sql.ResultSet
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HistoricoPedidosController {
    private val database = DatabaseHelper()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    fun buscarPedidos(
        dataInicial: LocalDate?,
        dataFinal: LocalDate?,
        busca: String,
        status: String?
    ): List<Map<String, Any>> {
        val queryParts = mutableListOf(
            """
        SELECT p.id, p.numero, p.data_pedido, p.telefone_contato, p.status, 
               p.valor_total, p.data_retirada, p.hora_retirada, 
               c.nome, c.sobrenome,
               e.endereco, e.bairro, e.cidade, e.data_entrega, e.hora_entrega
        FROM pedidos p
        LEFT JOIN clientes c ON p.cliente_id = c.id
        LEFT JOIN entregas e ON p.id = e.pedido_id
        WHERE 1=1
        """
        )

        val params = mutableListOf<Any>()

        if (dataInicial != null) {
            queryParts.add("AND DATE(p.data_pedido) >= ?")
            params.add(dataInicial.toString())
        }

        if (dataFinal != null) {
            queryParts.add("AND DATE(p.data_pedido) <= ?")
            params.add(dataFinal.toString())
        }

        if (busca.isNotBlank()) {
            queryParts.add("AND (p.numero LIKE ? OR p.telefone_contato LIKE ? OR c.nome LIKE ?)")
            params.add("%$busca%")
            params.add("%$busca%")
            params.add("%$busca%")
        }

        if (status != null && status.isNotBlank()) {
            queryParts.add("AND p.status = ?")
            params.add(status)
        }

        queryParts.add("ORDER BY p.data_pedido DESC")

        val query = queryParts.joinToString(" ")
        val resultados = mutableListOf<Map<String, Any>>()

        database.getConnection().use { conn ->
            conn.prepareStatement(query).use { stmt ->
                params.forEachIndexed { index, param ->
                    stmt.setObject(index + 1, param)
                }

                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val pedidoId = rs.getLong("id")
                        val dataHora = formatDateTime(rs.getString("data_pedido"))
                        val valorTotal = formatCurrency(rs.getDouble("valor_total"))

                        // Construir string de produtos
                        val produtos = buscarProdutosDoPedido(pedidoId)

                        // Determinar se tem entrega verificando se há endereco
                        val endereco = rs.getString("endereco")
                        val isEntrega = endereco != null
                        val retiradaTexto: String

                        if (isEntrega) {
                            // Formatar informações de entrega
                            val bairro = rs.getString("bairro") ?: ""
                            val cidade = rs.getString("cidade") ?: ""
                            val dataEntrega = rs.getString("data_entrega") ?: ""
                            val horaEntrega = rs.getString("hora_entrega") ?: ""

                            val dataHoraEntrega = formatarDataHoraEntrega(dataEntrega, horaEntrega)

                            // Check if address information exists before displaying it
                            retiradaTexto = if (endereco.isNotBlank()) {
                                "Endereço: $endereco, $bairro - $cidade\n$dataHoraEntrega"
                            } else {
                                dataHoraEntrega // Just show the date/time
                            }
                        } else {
                            // Formatar informações de retirada
                            val dataRetirada = rs.getString("data_retirada") ?: ""
                            val horaRetirada = rs.getString("hora_retirada") ?: ""
                            retiradaTexto = formatarDataHoraRetirada(dataRetirada, horaRetirada)
                        }

                        // Obter nome do cliente diretamente da consulta
                        val nome = rs.getString("nome") ?: ""
                        val sobrenome = rs.getString("sobrenome") ?: ""
                        val nomeCliente = if (nome.isNotBlank()) "$nome $sobrenome".trim() else
                            buscarNomeCliente(rs.getString("telefone_contato"))

                        val pedidoMap = mapOf(
                            "id" to pedidoId,
                            "numero" to rs.getString("numero"),
                            "data_hora" to dataHora,
                            "cliente" to nomeCliente,
                            "telefone" to rs.getString("telefone_contato"),
                            "produtos" to produtos,
                            "valor_total" to valorTotal,
                            "status" to rs.getString("status"),
                            "retirada" to retiradaTexto
                        )

                        resultados.add(pedidoMap)
                    }
                }
            }
        }

        return resultados
    }

    private fun formatarDataHoraRetirada(data: String, hora: String): String {
        return if (data.isNotEmpty() && hora.isNotEmpty()) {
            "Retirada: $data às $hora"
        } else {
            "Retirada: Não definida"
        }
    }
    private fun buscarNomeCliente(telefone: String): String {
        return try {
            DatabaseHelper().getConnection().use { conn ->
                val query = "SELECT nome, sobrenome FROM clientes WHERE telefone = ?"
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, telefone)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            val nome = rs.getString("nome") ?: ""
                            val sobrenome = rs.getString("sobrenome") ?: ""
                            "$nome $sobrenome".trim()
                        } else {
                            ""
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun formatarDataHoraEntrega(data: String, hora: String): String {
        return if (data.isNotEmpty() && hora.isNotEmpty()) {
            "Entrega: $data às $hora"
        } else {
            ""
        }
    }

    private fun formatDateTime(dateTimeStr: String?): String {
        if (dateTimeStr.isNullOrEmpty()) {
            return ""
        }

        return try {
            val pattern = if (dateTimeStr.length > 10) "yyyy-MM-dd HH:mm:ss" else "yyyy-MM-dd"
            val formatter = java.time.format.DateTimeFormatter.ofPattern(pattern)

            val parsedDateTime = if (dateTimeStr.length > 10) {
                java.time.LocalDateTime.parse(dateTimeStr, formatter)
            } else {
                java.time.LocalDate.parse(dateTimeStr, formatter).atStartOfDay()
            }

            val outputFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            parsedDateTime.format(outputFormatter)
        } catch (e: Exception) {
            // Return the original string if parsing fails
            dateTimeStr ?: ""
        }
    }

    private fun formatCurrency(value: Double): String {
        return "R$ ${String.format("%.2f", value).replace(".", ",")}"
    }

    private fun buscarProdutosDoPedido(pedidoId: Long): String {
        return try {
            DatabaseHelper().getConnection().use { conn ->
                val query = """
                SELECT nome_produto, quantidade
                FROM itens_pedido
                WHERE pedido_id = ?
                ORDER BY id
            """.trimIndent()

                conn.prepareStatement(query).use { stmt ->
                    stmt.setLong(1, pedidoId)
                    stmt.executeQuery().use { rs ->
                        val produtosList = mutableListOf<String>()
                        while (rs.next()) {
                            val quantidade = rs.getInt("quantidade")
                            val nomeProduto = rs.getString("nome_produto")
                            produtosList.add("$quantidade x $nomeProduto")
                        }
                        produtosList.joinToString(", ")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun mostrarDetalhesPedido(pedido: Map<String, Any>) {
        try {
            val pedidoId = pedido["id"] as Long
            val detalhes = buscarDetalhePedido(pedidoId)
            val itensPedido = buscarItensPedido(pedidoId)

            val dialog = Dialog<ButtonType>().apply {
                title = "Detalhes do Pedido ${pedido["numero"]}"
                dialogPane.buttonTypes.add(ButtonType.CLOSE)
                dialogPane.minWidth = 600.0

                val content = VBox(10.0).apply {
                    padding = javafx.geometry.Insets(20.0)
                    children.addAll(
                        criarSecaoDetalhesPedido(pedido, detalhes),
                        criarSecaoItensPedido(itensPedido)
                    )
                }

                dialogPane.content = content
            }

            dialog.showAndWait()

        } catch (e: Exception) {
            showAlert("Erro", "Falha ao mostrar detalhes do pedido: ${e.message}")
        }
    }



    fun getCompleteOrderDetails(orderId: Long): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        DatabaseHelper().getConnection().use { conn ->
            // Fetch basic order info
            conn.prepareStatement("""
            SELECT p.*, strftime('%d/%m/%Y', p.data_pedido) as data_formatada
            FROM pedidos p 
            WHERE p.id = ?
        """).use { stmt ->
                stmt.setLong(1, orderId)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    result["id"] = rs.getLong("id")
                    result["numero"] = rs.getString("numero")
                    result["data_pedido"] = rs.getString("data_formatada")
                    result["status"] = rs.getString("status")

                    // Client info
                    val clienteInfo = mutableListOf(
                        Pair("Telefone", rs.getString("telefone_contato") ?: ""),
                        Pair("Observação", rs.getString("observacao") ?: "")
                    )
                    result["cliente"] = clienteInfo

                    // Payment info
                    val pagamentoInfo = mutableListOf(
                        Pair("Forma de Pagamento", rs.getString("forma_pagamento") ?: ""),
                        Pair("Status", rs.getString("status") ?: ""),
                        Pair("Valor Total", "R$ ${String.format("%.2f", rs.getDouble("valor_total")).replace(".", ",")}"),
                        Pair("Desconto", "R$ ${String.format("%.2f", rs.getDouble("valor_desconto")).replace(".", ",")}")
                    )

                    // Add troco info if payment is "Dinheiro"
                    if (rs.getString("forma_pagamento") == "Dinheiro" && rs.getDouble("valor_troco_para") > 0) {
                        pagamentoInfo.add(Pair("Troco Para", "R$ ${String.format("%.2f", rs.getDouble("valor_troco_para")).replace(".", ",")}"))
                        pagamentoInfo.add(Pair("Troco", "R$ ${String.format("%.2f", rs.getDouble("valor_troco")).replace(".", ",")}"))
                    }

                    // Add retirada info if not "Entrega"
                    if (rs.getString("data_retirada") != "Entrega") {
                        pagamentoInfo.add(Pair("Data de Retirada", rs.getString("data_retirada") ?: ""))
                        pagamentoInfo.add(Pair("Hora de Retirada", rs.getString("hora_retirada") ?: ""))
                    }

                    result["pagamento"] = pagamentoInfo
                }
            }

            // Fetch product items
            val produtos = mutableListOf<Pair<String, String>>()
            conn.prepareStatement("""
            SELECT * FROM itens_pedido WHERE pedido_id = ? ORDER BY id
        """).use { stmt ->
                stmt.setLong(1, orderId)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val quantidade = rs.getInt("quantidade")
                    val nomeProduto = rs.getString("nome_produto")
                    val valorUnitario = String.format("%.2f", rs.getDouble("valor_unitario")).replace(".", ",")
                    val subtotal = String.format("%.2f", rs.getDouble("subtotal")).replace(".", ",")

                    produtos.add(Pair(
                        "Item ${produtos.size + 1}",
                        "${quantidade}x $nomeProduto (R$ $valorUnitario) = R$ $subtotal"
                    ))
                }
            }
            result["produtos"] = produtos

            // Check if has delivery
            val entregaInfo = mutableListOf<Pair<String, String>>()
            conn.prepareStatement("""
            SELECT * FROM entregas WHERE pedido_id = ?
        """).use { stmt ->
                stmt.setLong(1, orderId)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    entregaInfo.add(Pair("Entrega", "Sim"))
                    entregaInfo.add(Pair("Nome", rs.getString("nome_destinatario") ?: ""))
                    entregaInfo.add(Pair("Telefone", rs.getString("telefone_destinatario") ?: ""))
                    entregaInfo.add(Pair("Endereço", rs.getString("endereco") ?: ""))
                    entregaInfo.add(Pair("Referência", rs.getString("referencia") ?: ""))
                    entregaInfo.add(Pair("Cidade", rs.getString("cidade") ?: ""))
                    entregaInfo.add(Pair("Bairro", rs.getString("bairro") ?: ""))
                    entregaInfo.add(Pair("CEP", rs.getString("cep") ?: ""))
                    entregaInfo.add(Pair("Valor", "R$ ${String.format("%.2f", rs.getDouble("valor_entrega")).replace(".", ",")}"))
                    entregaInfo.add(Pair("Data", rs.getString("data_entrega") ?: ""))
                    entregaInfo.add(Pair("Hora", rs.getString("hora_entrega") ?: ""))
                } else {
                    entregaInfo.add(Pair("Entrega", "Não"))
                }
            }
            result["entrega"] = entregaInfo
        }

        return result
    }

    private fun criarSecaoDetalhesPedido(
        pedido: Map<String, Any>,
        detalhes: Map<String, Any>
    ): VBox {
        return VBox(5.0).apply {
            // Implementação detalhada com títulos, valores e formatação
        }
    }

    private fun criarSecaoItensPedido(itens: List<Map<String, Any>>): VBox {
        return VBox(5.0).apply {
            // Implementação de uma tabela com os itens do pedido
        }
    }

    private fun buscarDetalhePedido(pedidoId: Long): Map<String, Any> {
        // Query para buscar informações detalhadas do pedido
        return mapOf()
    }

    private fun buscarItensPedido(pedidoId: Long): List<Map<String, Any>> {
        val itens = mutableListOf<Map<String, Any>>()

        try {
            val query = """
                SELECT ip.*, p.codigo as produto_codigo 
                FROM itens_pedido ip
                LEFT JOIN produtos p ON p.id = ip.produto_id
                WHERE ip.pedido_id = ?
            """

            database.getConnection().use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.setLong(1, pedidoId)
                    val rs = stmt.executeQuery()

                    while (rs.next()) {
                        itens.add(mapOf(
                            "id" to rs.getLong("id"),
                            "nome" to rs.getString("nome_produto"),
                            "quantidade" to rs.getInt("quantidade"),
                            "valor_unitario" to rs.getDouble("valor_unitario"),
                            "subtotal" to rs.getDouble("subtotal"),
                            "codigo" to (rs.getString("produto_codigo") ?: "N/A")
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return itens
    }

    fun imprimirPedido(pedido: Map<String, Any>) {
        try {
            val pedidoId = pedido["id"] as Long
            val numeroPedido = pedido["numero"] as String

            // Buscar informações para impressão
            val clienteInfo = buscarClienteInfo(pedidoId)
            val produtos = buscarProdutosParaImpressao(pedidoId)
            val pagamentoInfo = buscarPagamentoInfo(pedidoId)
            val entregaInfo = buscarEntregaInfo(pedidoId)

            // Imprimir pedido
            val printerController = PrinterController()
            printerController.imprimirPedido(
                numeroPedido = numeroPedido,
                clienteInfo = clienteInfo,
                produtos = produtos,
                pagamentoInfo = pagamentoInfo,
                entregaInfo = entregaInfo
            )

            showAlert("Sucesso", "Pedido enviado para impressão", Alert.AlertType.INFORMATION)

        } catch (e: Exception) {
            e.printStackTrace()
            showAlert("Erro", "Falha ao imprimir pedido: ${e.message}")
        }
    }

    private fun buscarClienteInfo(pedidoId: Long): List<Pair<String, String>> {
        // Implementação para buscar informações do cliente
        return listOf()
    }

    private fun buscarProdutosParaImpressao(pedidoId: Long): List<Map<String, String>> {
        // Implementação para buscar produtos
        return listOf()
    }

    private fun buscarPagamentoInfo(pedidoId: Long): List<Pair<String, String>> {
        // Implementação para buscar informações de pagamento
        return listOf()
    }

    private fun buscarEntregaInfo(pedidoId: Long): List<Pair<String, String>> {
        // Implementação para buscar informações de entrega
        return listOf()
    }

    fun exportarPedidos(
        dataInicial: LocalDate?,
        dataFinal: LocalDate?,
        busca: String,
        status: String?
    ) {
        val pedidos = buscarPedidos(dataInicial, dataFinal, busca, status)

        if (pedidos.isEmpty()) {
            showAlert("Aviso", "Não há pedidos para exportar", Alert.AlertType.INFORMATION)
            return
        }

        try {
            val csv = StringBuilder()

            // Header
            csv.append("Número,Data/Hora,Telefone,Cliente,Valor Total,Status,Retirada/Entrega\n")

            // Data rows
            pedidos.forEach { pedido ->
                csv.append("\"${pedido["numero"]}\",")
                csv.append("\"${pedido["data_hora"]}\",")
                csv.append("\"${pedido["telefone"]}\",")
                csv.append("\"${pedido["cliente"]}\",")
                csv.append("\"${pedido["valor_total"]}\",")
                csv.append("\"${pedido["status"]}\",")
                csv.append("\"${pedido["retirada"]}\"\n")
            }

            // Save file
            val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val file = File("pedidos_export_$now.csv")

            FileWriter(file).use { it.write(csv.toString()) }

            showAlert(
                "Sucesso",
                "Arquivo exportado com sucesso: ${file.absolutePath}",
                Alert.AlertType.INFORMATION
            )

        } catch (e: Exception) {
            e.printStackTrace()
            showAlert("Erro", "Falha ao exportar pedidos: ${e.message}")
        }
    }

    private fun showAlert(title: String, message: String, type: Alert.AlertType = Alert.AlertType.ERROR) {
        Alert(type).apply {
            this.title = title
            this.headerText = null
            this.contentText = message
            showAndWait()
        }
    }
}