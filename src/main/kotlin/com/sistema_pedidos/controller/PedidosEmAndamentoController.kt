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

import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.LineSeparator
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue


class PedidosEmAndamentoController {
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
        SELECT p.id, p.numero, p.data_pedido, p.telefone_contato, p.status, p.status_pedido,
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
                            "status_pedido" to rs.getString("status_pedido"),
                            "retirada" to retiradaTexto
                        )

                        resultados.add(pedidoMap)
                    }
                }
            }
        }

        return resultados
    }

    fun atualizarStatusPagamentoPedido(pedidoId: Long, novoStatus: String): Boolean {
        try {
            DatabaseHelper().getConnection().use { connection ->
                connection.prepareStatement(
                    "UPDATE pedidos SET status = ? WHERE id = ?"
                ).use { statement ->
                    statement.setString(1, novoStatus)
                    statement.setLong(2, pedidoId)
                    statement.executeUpdate()
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
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

    fun exportarPedidoParaPDF(orderDetails: Map<String, Any>, filePath: String): Boolean {
        try {
            // Create PDF document with margins
            val writer = PdfWriter(filePath)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)
            document.setMargins(36f, 36f, 36f, 36f)

            // Add title
            val title = Paragraph("PEDIDO ${orderDetails["numero"]}")
            title.setFontSize(14f)
            title.setBold()
            title.setTextAlignment(TextAlignment.CENTER)
            document.add(title)

            // Add date
            val date = Paragraph("Data: ${orderDetails["data_pedido"]}")
            date.setFontSize(10f)
            date.setTextAlignment(TextAlignment.CENTER)
            document.add(date)

            val solidLine = SolidLine(1f)
            val lineSeparator = LineSeparator(solidLine)
            lineSeparator.setMarginTop(10f)
            lineSeparator.setMarginBottom(10f)
            document.add(lineSeparator)

            // Add customer details
            addSectionToPDF(document, "Cliente", orderDetails["cliente"] as List<Pair<String, String>>)

            // Add products as table
            addProductsTable(document, orderDetails["produtos"] as List<Pair<String, String>>)

            // Add payment info
            addSectionToPDF(document, "Pagamento", orderDetails["pagamento"] as List<Pair<String, String>>)

            // Add delivery info if available
            val entregaDetails = orderDetails["entrega"] as List<Pair<String, String>>
            if (entregaDetails.isNotEmpty() && entregaDetails.first().second != "Não") {
                addSectionToPDF(document, "Entrega", entregaDetails)
            }

            // Add footer
            val footer = Paragraph("Documento gerado em: ${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))}")
            footer.setFontSize(8f)
            footer.setItalic()
            footer.setTextAlignment(TextAlignment.CENTER)
            footer.setMarginTop(20f)
            document.add(footer)

            // Close document
            document.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun addSectionToPDF(document: Document, title: String, items: List<Pair<String, String>>) {
        // Add section title
        val sectionTitle = Paragraph(title)
        sectionTitle.setFontSize(12f)
        sectionTitle.setBold()
        sectionTitle.setMarginTop(10f)
        document.add(sectionTitle)

// Add separator
        val solidLine = SolidLine(1f)
        val lineSeparator = LineSeparator(solidLine)
        lineSeparator.setMarginTop(10f)
        lineSeparator.setMarginBottom(10f)
        document.add(lineSeparator)

        // Create a 2-column table for label:value pairs
        val columnWidths = floatArrayOf(30f, 70f)
        val table = Table(UnitValue.createPercentArray(columnWidths))
        table.setWidth(UnitValue.createPercentValue(100f))

        // Add items as table rows
        items.forEach { (label, value) ->
            val labelCell = Cell()
            val labelParagraph = Paragraph(label)
            labelParagraph.setFontSize(9f)
            labelParagraph.setBold()
            labelCell.add(labelParagraph)
            table.addCell(labelCell)

            val valueCell = Cell()
            val valueParagraph = Paragraph(value)
            valueParagraph.setFontSize(9f)
            valueCell.add(valueParagraph)
            table.addCell(valueCell)
        }

        document.add(table)
        document.add(Paragraph("\n").setFontSize(5f))
    }

    private fun addProductsTable(document: Document, products: List<Pair<String, String>>) {
        // Add section title
        val sectionTitle = Paragraph("Produtos")
        sectionTitle.setFontSize(12f)
        sectionTitle.setBold()
        sectionTitle.setMarginTop(10f)
        document.add(sectionTitle)

        // Add a subtle line under the section title
        val solidLine = SolidLine(0.5f)
        val lineSeparator = LineSeparator(solidLine)
        lineSeparator.setMarginBottom(5f)
        lineSeparator.setWidth(UnitValue.createPercentValue(100f))
        document.add(lineSeparator)

        // Create table for products - updated column widths for 5 columns
        val columnWidths = floatArrayOf(12f, 36f, 12f, 20f, 20f)
        val table = Table(UnitValue.createPercentArray(columnWidths))
        table.setWidth(UnitValue.createPercentValue(100f))

        // Add header
        val headerCell1 = Cell()
        val headerParagraph1 = Paragraph("Código")
        headerParagraph1.setFontSize(9f)
        headerParagraph1.setBold()
        headerCell1.add(headerParagraph1)
        table.addCell(headerCell1)

        val headerCell2 = Cell()
        val headerParagraph2 = Paragraph("Produto")
        headerParagraph2.setFontSize(9f)
        headerParagraph2.setBold()
        headerCell2.add(headerParagraph2)
        table.addCell(headerCell2)

        val headerCell3 = Cell()
        val headerParagraph3 = Paragraph("Qtd")
        headerParagraph3.setFontSize(9f)
        headerParagraph3.setBold()
        headerCell3.add(headerParagraph3)
        table.addCell(headerCell3)

        val headerCell4 = Cell()
        val headerParagraph4 = Paragraph("Unitário")
        headerParagraph4.setFontSize(9f)
        headerParagraph4.setBold()
        headerCell4.add(headerParagraph4)
        table.addCell(headerCell4)

        val headerCell5 = Cell()
        val headerParagraph5 = Paragraph("Subtotal")
        headerParagraph5.setFontSize(9f)
        headerParagraph5.setBold()
        headerCell5.add(headerParagraph5)
        table.addCell(headerCell5)

        // Process the raw product data into columns
        var codigo = ""
        var nome = ""
        var quantidade = ""
        var unitario = ""
        var subtotal = ""

        // Updated to process in groups of 5
        for (i in products.indices step 5) {
            if (i+4 < products.size) {
                codigo = products[i].second
                nome = products[i+1].second
                quantidade = products[i+2].second
                unitario = products[i+3].second
                subtotal = products[i+4].second

                val cell1 = Cell()
                val para1 = Paragraph(codigo)
                para1.setFontSize(9f)
                cell1.add(para1)
                table.addCell(cell1)

                val cell2 = Cell()
                val para2 = Paragraph(nome)
                para2.setFontSize(9f)
                cell2.add(para2)
                table.addCell(cell2)

                val cell3 = Cell()
                val para3 = Paragraph(quantidade)
                para3.setFontSize(9f)
                cell3.add(para3)
                table.addCell(cell3)

                val cell4 = Cell()
                val para4 = Paragraph(unitario)
                para4.setFontSize(9f)
                cell4.add(para4)
                table.addCell(cell4)

                val cell5 = Cell()
                val para5 = Paragraph(subtotal)
                para5.setFontSize(9f)
                cell5.add(para5)
                table.addCell(cell5)
            }
        }

        document.add(table)
        document.add(Paragraph("\n").setFontSize(5f))
    }

    fun getCompleteOrderDetails(orderId: Long): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        DatabaseHelper().getConnection().use { conn ->
            // Fetch basic order info
            conn.prepareStatement("""
    SELECT p.*, strftime('%d/%m/%Y', p.data_pedido) as data_formatada,
           c.nome, c.sobrenome
    FROM pedidos p
    LEFT JOIN clientes c ON p.cliente_id = c.id
    WHERE p.id = ?
""").use { stmt ->
                stmt.setLong(1, orderId)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    result["id"] = rs.getLong("id")
                    result["numero"] = rs.getString("numero")
                    result["data_pedido"] = rs.getString("data_formatada")
                    result["status"] = rs.getString("status")

                    // Client info with name and last name
                    val nome = rs.getString("nome") ?: ""
                    val sobrenome = rs.getString("sobrenome") ?: ""
                    val clienteInfo = mutableListOf(
                        Pair("Nome", "$nome $sobrenome".trim()),
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
// Fetch product items
            val produtos = mutableListOf<Pair<String, String>>()
            conn.prepareStatement("""
    SELECT ip.*, p.codigo as produto_codigo
    FROM itens_pedido ip
    LEFT JOIN produtos p ON p.id = ip.produto_id
    WHERE ip.pedido_id = ?
    ORDER BY ip.id
""").use { stmt ->
                stmt.setLong(1, orderId)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    // Get product code (use item ID if code is null)
                    val codigo = rs.getString("produto_codigo") ?: rs.getString("id") ?: "N/A"
                    val nomeProduto = rs.getString("nome_produto")
                    val quantidade = rs.getInt("quantidade").toString()
                    val valorUnitario = "R$ " + String.format("%.2f", rs.getDouble("valor_unitario")).replace(".", ",")
                    val subtotal = "R$ " + String.format("%.2f", rs.getDouble("subtotal")).replace(".", ",")

                    // Add each product's data as 5 consecutive pairs
                    produtos.add(Pair("Codigo", codigo))
                    produtos.add(Pair("Nome", nomeProduto))
                    produtos.add(Pair("Quantidade", quantidade))
                    produtos.add(Pair("Unitário", valorUnitario))
                    produtos.add(Pair("Subtotal", subtotal))
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
                    entregaInfo.add(Pair("Número", rs.getString("numero") ?: ""))
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

    fun atualizarStatusPedido(pedidoId: Long, novoStatus: String): Boolean {
        try {
            DatabaseHelper().getConnection().use { connection ->
                connection.prepareStatement(
                    "UPDATE pedidos SET status_pedido = ? WHERE id = ?"
                ).use { statement ->
                    statement.setString(1, novoStatus)
                    statement.setLong(2, pedidoId)
                    statement.executeUpdate()
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun extractNumericValue(formattedValue: String): Double {
        return try {
            formattedValue.replace("R$", "")
                .replace(".", "")
                .replace(",", ".")
                .trim()
                .toDouble()
        } catch (e: Exception) {
            0.0
        }
    }

    fun imprimirPedido(pedido: Map<String, Any>) {
        try {
            val pedidoId = pedido["id"] as Long

            // Get complete order data
            val orderDetails = getCompleteOrderDetails(pedidoId)

            // Format data as expected by PrinterController
            val pedidoData = mutableMapOf<String, Any>()

            // Add basic order info
            pedidoData["numero"] = pedido["numero"] as String
            pedidoData["data_pedido"] = orderDetails["data_pedido"] as String
            pedidoData["status"] = orderDetails["status"] as String
            pedidoData["status_pedido"] = pedido["status_pedido"] as String
            pedidoData["valor_total"] = extractNumericValue(pedido["valor_total"] as String)

            val clienteInfoList = orderDetails["cliente"] as List<Pair<String, String>>
            val clienteMap = mutableMapOf<String, Any>()
            clienteInfoList.forEach { (key, value) ->
                when(key) {
                    "Nome" -> {
                        // Split the full name into first name and surname
                        val nameParts = value.trim().split(" ", limit = 2)
                        clienteMap["nome"] = nameParts.firstOrNull() ?: ""
                        clienteMap["sobrenome"] = if (nameParts.size > 1) nameParts[1] else ""
                        // Add the tipo field - default to PESSOA_FISICA since we're treating it as a person
                        clienteMap["tipo"] = "PESSOA_FISICA"
                    }
                    "Telefone" -> pedidoData["telefone_contato"] = value
                    "Observação" -> pedidoData["observacao"] = value
                }
            }
            pedidoData["cliente"] = clienteMap

            // Add payment info
            val pagamentoInfoList = orderDetails["pagamento"] as List<Pair<String, String>>
            pagamentoInfoList.forEach { (key, value) ->
                when(key) {
                    "Forma de Pagamento" -> pedidoData["forma_pagamento"] = value
                    "Valor Total" -> if(!pedidoData.containsKey("valor_total"))
                        pedidoData["valor_total"] = extractNumericValue(value)
                    "Desconto" -> pedidoData["valor_desconto"] = extractNumericValue(value)
                    "Troco Para" -> pedidoData["valor_troco_para"] = extractNumericValue(value)
                    "Troco" -> pedidoData["valor_troco"] = extractNumericValue(value)
                    "Data de Retirada" -> pedidoData["data_retirada"] = value
                    "Hora de Retirada" -> pedidoData["hora_retirada"] = value
                }
            }

            // Add product items
            val produtosList = mutableListOf<Map<String, Any>>()
            val produtosInfoList = orderDetails["produtos"] as List<Pair<String, String>>

            for (i in produtosInfoList.indices step 5) {
                if (i+4 < produtosInfoList.size) {
                    val produtoMap = mutableMapOf<String, Any>()
                    produtoMap["codigo"] = produtosInfoList[i].second
                    produtoMap["nome_produto"] = produtosInfoList[i+1].second
                    produtoMap["quantidade"] = produtosInfoList[i+2].second.toInt()
                    produtoMap["valor_unitario"] = extractNumericValue(produtosInfoList[i+3].second)
                    produtoMap["subtotal"] = extractNumericValue(produtosInfoList[i+4].second)
                    produtosList.add(produtoMap)
                }
            }
            pedidoData["itens"] = produtosList

            // Add delivery info if available
            val entregaInfoList = orderDetails["entrega"] as List<Pair<String, String>>
            if (entregaInfoList.isNotEmpty() && entregaInfoList.first().second == "Sim") {
                val entregaMap = mutableMapOf<String, Any>()
                entregaInfoList.forEach { (key, value) ->
                    when(key) {
                        "Nome" -> entregaMap["nome_destinatario"] = value
                        "Telefone" -> entregaMap["telefone_destinatario"] = value
                        "Endereço" -> entregaMap["endereco"] = value
                        "Número" -> entregaMap["numero"] = value
                        "Referência" -> entregaMap["referencia"] = value
                        "Cidade" -> entregaMap["cidade"] = value
                        "Bairro" -> entregaMap["bairro"] = value
                        "CEP" -> entregaMap["cep"] = value
                        "Valor" -> entregaMap["valor_entrega"] = extractNumericValue(value)
                        "Data" -> entregaMap["data_entrega"] = value
                        "Hora" -> entregaMap["hora_entrega"] = value
                    }
                }
                pedidoData["entrega"] = entregaMap
            }

            // Print the order
            val printerController = PrinterController()
            printerController.imprimirPedido(pedidoData = pedidoData)

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