package com.sistema_pedidos.controller

import com.github.anastaciocintra.escpos.EscPos
import com.github.anastaciocintra.escpos.Style
import com.github.anastaciocintra.escpos.EscPosConst
import com.github.anastaciocintra.escpos.EscPos.CutMode
import com.github.anastaciocintra.output.PrinterOutputStream
import java.io.IOException
import javax.print.PrintService
import javax.print.PrintServiceLookup
import java.util.prefs.Preferences
import java.text.SimpleDateFormat
import java.util.Date

class PrinterController {
    private val prefs = Preferences.userNodeForPackage(PrinterController::class.java)
    private val DEFAULT_PRINTER_KEY = "default_thermal_printer"
    private val COMPANY_NAME = "FLORICULTURA ADRIANA FLORES"
    private val COMPANY_PHONE = "(42) 99815-5900"
    private val COMPANY_ADDRESS = "Rua 19 de Dezembro, 347 - Centro"

    // Lista de impressoras térmicas conhecidas (pode ser expandida conforme necessário)
    private val knownThermalPrinters = listOf(
        "EPSON TM-T20", "EPSON TM-T88", "Bematech MP-4200", "Daruma DR700", "Elgin i9",
        "POS-80", "Generic / Text Only", "POS58 Printer"
    )

    /**
     * Lista todas as impressoras disponíveis no sistema
     */
    fun listarImpressoras(): List<PrintService> {
        return PrintServiceLookup.lookupPrintServices(null, null).toList()
    }

    /**
     * Lista apenas impressoras térmicas disponíveis no sistema
     */
    fun listarImpressorasTermicas(): List<PrintService> {
        return listarImpressoras().filter { isThermalPrinter(it) }
    }

    /**
     * Obtém a impressora térmica padrão configurada para impressões
     */
    fun getImpressoraPadrao(): PrintService? {
        val nomeSalvo = prefs.get(DEFAULT_PRINTER_KEY, null)

        return if (nomeSalvo != null) {
            listarImpressoras().find { it.name == nomeSalvo && isThermalPrinter(it) }
        } else {
            // Tenta encontrar a primeira impressora térmica disponível
            listarImpressorasTermicas().firstOrNull()
        }
    }

    /**
     * Define uma impressora térmica como padrão para impressões
     */
    fun definirImpressoraPadrao(printService: PrintService) {
        if (isThermalPrinter(printService)) {
            prefs.put(DEFAULT_PRINTER_KEY, printService.name)
        } else {
            throw IllegalArgumentException("A impressora selecionada não é uma impressora térmica")
        }
    }

    /**
     * Verifica se uma impressora é térmica (baseado no nome ou outras características)
     */
    fun isThermalPrinter(printService: PrintService): Boolean {
        val printerName = printService.name.lowercase()
        return knownThermalPrinters.any { printerName.contains(it.lowercase()) }
    }

    /**
     * Imprime um pedido na impressora térmica especificada
     */
    fun imprimirPedido(
        printService: PrintService? = null,
        pedidoData: Map<String, Any>
    ) {
        try {
            // Use a impressora especificada ou obtenha a impressora padrão
            val impressora = printService ?: getImpressoraPadrao()
            ?: throw IOException("Nenhuma impressora térmica encontrada.")

            if (!isThermalPrinter(impressora)) {
                throw IOException("A impressora ${impressora.name} não é uma impressora térmica.")
            }

            println("Usando impressora térmica: ${impressora.name}")
            imprimirEmTermica(impressora, pedidoData)
            println("Impressão concluída com sucesso!")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Erro ao imprimir: ${e.message}")

            if (e is IOException) {
                println("Verifique se a impressora térmica está conectada e ligada")
            }
        }
    }

    /**
     * Imprime em impressora térmica usando ESC/POS
     */
    private fun imprimirEmTermica(
        impressora: PrintService,
        pedidoData: Map<String, Any>
    ) {
        PrinterOutputStream(impressora).use { outputStream ->
            EscPos(outputStream).use { escpos ->
                // Estilos
                val boldCenter = Style()
                    .setBold(true)
                    .setJustification(EscPosConst.Justification.Center)

                val center = Style()
                    .setJustification(EscPosConst.Justification.Center)

                val bold = Style().setBold(true)

                val right = Style()
                    .setJustification(EscPosConst.Justification.Right)

                val linhaDiv = "--------------------------------"

                // Cabeçalho da empresa
                escpos.write(boldCenter, COMPANY_NAME)
                    .feed(1)
                    .write(center, COMPANY_ADDRESS)
                    .feed(1)
                    .write(center, "Tel: $COMPANY_PHONE")
                    .feed(1)
                    .writeLF(linhaDiv)

                // Detalhes do pedido
                val numeroPedido = pedidoData["numero"] as String
                val dataPedido = formatarData(pedidoData["data_pedido"] as String)
                val statusPedido = pedidoData["status_pedido"] as String
                val statusPagamento = pedidoData["status"] as String

                escpos.write(boldCenter, "PEDIDO #$numeroPedido")
                    .feed(1)
                    .writeLF("Data: $dataPedido")
                    .writeLF("Status: $statusPedido")
                    .writeLF("Pagamento: $statusPagamento")
                    .feed(1)
                    .writeLF(linhaDiv)

                // Cliente
                val clienteInfo = pedidoData["cliente"] as? Map<String, Any>
                val telefoneContato = pedidoData["telefone_contato"] as? String ?: ""

                escpos.write(bold, "CLIENTE:")
                    .feed(1)

                if (clienteInfo != null) {
                    val tipoCliente = clienteInfo["tipo"] as? String
                    if (tipoCliente == "PESSOA_FISICA") {
                        val nome = clienteInfo["nome"] as? String ?: ""
                        val sobrenome = clienteInfo["sobrenome"] as? String ?: ""
                        escpos.writeLF("Nome: $nome $sobrenome")
                    } else {
                        val razaoSocial = clienteInfo["razao_social"] as? String ?: ""
                        val nomeFantasia = clienteInfo["nome_fantasia"] as? String ?: ""
                        escpos.writeLF("Empresa: $razaoSocial")
                        if (nomeFantasia.isNotEmpty()) {
                            escpos.writeLF("Nome Fantasia: $nomeFantasia")
                        }
                    }
                    escpos.writeLF("Tel: ${clienteInfo["telefone"] ?: telefoneContato}")
                } else {
                    escpos.writeLF("Tel: $telefoneContato")
                }

                val observacao = pedidoData["observacao"] as? String ?: ""
                if (observacao.isNotEmpty()) {
                    escpos.feed(1)
                        .writeLF("Observação: $observacao")
                }

                escpos.feed(1)
                    .writeLF(linhaDiv)

                // Produtos
                escpos.write(bold, "PRODUTOS:")
                    .feed(1)

                val itens = pedidoData["itens"] as? List<Map<String, Any>> ?: emptyList()

                // Cabeçalho da tabela de itens
                escpos.writeLF("QTD PRODUTO             VALOR   TOTAL")
                escpos.writeLF(linhaDiv)

                itens.forEach { item ->
                    val qtd = (item["quantidade"] as Number).toInt()
                    val nome = item["nome_produto"] as String
                    val valorUnit = formatarValor(item["valor_unitario"] as Number)
                    val subtotal = formatarValor(item["subtotal"] as Number)

                    // Formato tabular para os produtos
                    val truncatedName = truncateText(nome, 20)
                    escpos.writeLF(String.format("%-3s %-20s %-7s %s",
                        "${qtd}x", truncatedName, valorUnit, subtotal))

                    // Se o nome for muito longo, continuar em nova linha
                    if (nome.length > 20) {
                        val remaining = nome.substring(20)
                        val chunks = remaining.chunked(28)
                        chunks.forEach { chunk ->
                            escpos.writeLF("    $chunk")
                        }
                    }
                }

                escpos.feed(1)
                    .writeLF(linhaDiv)

                // Valores e pagamento
                val valorTotal = formatarValor(pedidoData["valor_total"] as Number)
                val valorDesconto = formatarValor(pedidoData["valor_desconto"] as? Number ?: 0)
                val tipoDesconto = pedidoData["tipo_desconto"] as? String ?: ""
                val formaPagamento = pedidoData["forma_pagamento"] as? String ?: "Não informado"
                val valorTrocoPara = pedidoData["valor_troco_para"] as? Number
                val valorTroco = pedidoData["valor_troco"] as? Number

                escpos.write(bold, "PAGAMENTO:")
                    .feed(1)
                    .writeLF("Forma: $formaPagamento")

                if ((valorDesconto.toDoubleOrNull() ?: 0.0) > 0) {
                    val descInfo = if (tipoDesconto == "percentual") {
                        val percentual = pedidoData["valor_desconto"] as Number
                        "Desconto: ${percentual}%"
                    } else {
                        "Desconto: R$ $valorDesconto"
                    }
                    escpos.writeLF(descInfo)
                }

                escpos.write(bold, "TOTAL: R$ $valorTotal")
                    .feed(1)

                if (formaPagamento == "Dinheiro" && valorTrocoPara != null && valorTroco != null) {
                    val valorTrocoParaStr = formatarValor(valorTrocoPara)
                    val valorTrocoStr = formatarValor(valorTroco)
                    escpos.writeLF("Troco para: R$ $valorTrocoParaStr")
                    escpos.writeLF("Troco: R$ $valorTrocoStr")
                }

                escpos.feed(1)
                    .writeLF(linhaDiv)

                // Entrega ou retirada
                val entrega = pedidoData["entrega"] as? Map<String, Any>
                if (entrega != null) {
                    escpos.write(bold, "ENTREGA:")
                        .feed(1)
                        .writeLF("Destinatário: ${entrega["nome_destinatario"]}")
                        .writeLF("Telefone: ${entrega["telefone_destinatario"]}")
                        .writeLF("Endereço: ${entrega["endereco"]}, ${entrega["numero"]}")

                    val referencia = entrega["referencia"] as? String ?: ""
                    if (referencia.isNotEmpty()) {
                        escpos.writeLF("Referência: $referencia")
                    }

                    escpos.writeLF("Bairro: ${entrega["bairro"]}")
                        .writeLF("Cidade: ${entrega["cidade"]}")

                    val cep = entrega["cep"] as? String ?: ""
                    if (cep.isNotEmpty()) {
                        escpos.writeLF("CEP: $cep")
                    }

                    val valorEntrega = formatarValor(entrega["valor_entrega"] as? Number ?: 0)
                    val dataEntrega = formatarData(entrega["data_entrega"] as String)
                    val horaEntrega = entrega["hora_entrega"] as String

                    escpos.writeLF("Valor entrega: R$ $valorEntrega")
                        .writeLF("Data/Hora: $dataEntrega $horaEntrega")
                } else {
                    // Informações de retirada
                    val dataRetirada = pedidoData["data_retirada"] as? String
                    val horaRetirada = pedidoData["hora_retirada"] as? String

                    if (dataRetirada != null && horaRetirada != null) {
                        escpos.write(bold, "RETIRADA:")
                            .feed(1)
                            .writeLF("Data: ${formatarData(dataRetirada)}")
                            .writeLF("Hora: $horaRetirada")
                    }
                }

                escpos.feed(1)
                    .writeLF(linhaDiv)

                // Rodapé
                escpos.feed(1)
                    .write(center, "Obrigado pela preferência!")
                    .feed(1)
                    .feed(3)
                    .cut(CutMode.FULL)
            }
        }
    }

    /**
     * Formata texto para o tamanho especificado
     */
    private fun truncateText(text: String, maxLength: Int): String {
        return if (text.length <= maxLength) text else text.substring(0, maxLength)
    }

    /**
     * Formata um valor monetário
     */
    private fun formatarValor(valor: Number): String {
        return String.format("%.2f", valor.toDouble())
    }

    /**
     * Formata uma data no formato brasileiro
     */
    private fun formatarData(dataStr: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd")
            val outputFormat = SimpleDateFormat("dd/MM/yyyy")
            val date = inputFormat.parse(dataStr)
            return outputFormat.format(date)
        } catch (e: Exception) {
            return dataStr
        }
    }

    /**
     * Imprime texto simples na impressora térmica especificada
     */
    fun imprimirTexto(texto: String, printService: PrintService? = null) {
        try {
            val impressora = printService ?: getImpressoraPadrao()
            ?: throw IOException("Nenhuma impressora térmica encontrada.")

            if (!isThermalPrinter(impressora)) {
                throw IOException("A impressora ${impressora.name} não é uma impressora térmica.")
            }

            PrinterOutputStream(impressora).use { outputStream ->
                EscPos(outputStream).use { escpos ->
                    escpos.writeLF(texto)
                        .feed(3)
                        .cut(CutMode.FULL)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Erro ao imprimir texto: ${e.message}")
        }
    }

    /**
     * Verifica se a impressora térmica especificada está disponível
     */
    fun verificarImpressora(printService: PrintService?): Boolean {
        return try {
            val impressora = printService ?: getImpressoraPadrao()
            ?: return false

            // Verifica se é uma impressora térmica
            if (!isThermalPrinter(impressora)) {
                return false
            }

            // Verifica se a impressora está disponível usando DocFlavor
            impressora.isDocFlavorSupported(javax.print.DocFlavor.BYTE_ARRAY.AUTOSENSE)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}