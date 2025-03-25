package com.sistema_pedidos.controller

import com.github.anastaciocintra.escpos.EscPos
import com.github.anastaciocintra.escpos.Style
import com.github.anastaciocintra.escpos.EscPosConst
import com.github.anastaciocintra.escpos.EscPos.CutMode
import com.github.anastaciocintra.output.PrinterOutputStream
import com.sun.javafx.sg.prism.NGCanvas.LINE_WIDTH
import java.io.IOException
import javax.print.PrintService
import javax.print.PrintServiceLookup
import java.util.prefs.Preferences
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class NotaNaoFiscalController {
    private val prefs = Preferences.userNodeForPackage(NotaNaoFiscalController::class.java)
    private val DEFAULT_PRINTER_KEY = "default_thermal_printer"
    private val COMPANY_NAME = "FLORICULTURA ADRIANA FLORES"
    private val COMPANY_PHONE = "(42) 99815-5900"
    private val COMPANY_ADDRESS = "Rua 19 de Dezembro, 347 - Centro"
    private val COMPANY_CNPJ = "40.348.582/0001-16"
    private val LINE_WIDTH = 32 // Maximum characters per line for most thermal printers

    // Lista de impressoras térmicas conhecidas
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
     * Imprime uma nota fiscal sem valor fiscal na impressora térmica
     */
    fun imprimirNotaFiscal(
        printService: PrintService? = null,
        pedidoData: Map<String, Any>
    ) {
        try {
            // Use a impressora especificada ou obtenha a impressora padrao
            val impressora = printService ?: getImpressoraPadrao()
            ?: throw IOException("Nenhuma impressora termica encontrada.")

            if (!isThermalPrinter(impressora)) {
                throw IOException("A impressora ${impressora.name} nao e uma impressora termica.")
            }

            println("Usando impressora termica: ${impressora.name}")
            imprimirEmTermica(impressora, pedidoData)
            println("Impressao concluida com sucesso!")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Erro ao imprimir: ${e.message}")

            if (e is IOException) {
                println("Verifique se a impressora termica esta conectada e ligada")
            }
        }
    }

    /**
     * Normaliza texto removendo acentos e caracteres especiais
     */
    private fun normalizarTexto(texto: String): String {
        return texto.replace("[áàâã]".toRegex(), "a")
            .replace("[éèê]".toRegex(), "e")
            .replace("[íìî]".toRegex(), "i")
            .replace("[óòôõ]".toRegex(), "o")
            .replace("[úùû]".toRegex(), "u")
            .replace("[ÁÀÂÃ]".toRegex(), "A")
            .replace("[ÉÈÊ]".toRegex(), "E")
            .replace("[ÍÌÎ]".toRegex(), "I")
            .replace("[ÓÒÔÕ]".toRegex(), "O")
            .replace("[ÚÙÛ]".toRegex(), "U")
            .replace("[ç]".toRegex(), "c")
            .replace("[Ç]".toRegex(), "C")
            .replace("[ñ]".toRegex(), "n")
            .replace("[Ñ]".toRegex(), "N")
    }

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

                val smallFont = Style()
                    .setFontSize(Style.FontSize._1, Style.FontSize._1)

                val smallCenter = Style()
                    .setFontSize(Style.FontSize._1, Style.FontSize._1)
                    .setJustification(EscPosConst.Justification.Center)

                val smallBold = Style()
                    .setFontSize(Style.FontSize._1, Style.FontSize._1)
                    .setBold(true)

                val smallBoldCenter = Style()
                    .setFontSize(Style.FontSize._1, Style.FontSize._1)
                    .setBold(true)
                    .setJustification(EscPosConst.Justification.Center)

                val linhaDiv = "------------------------------------------------"


                // Cabecalho da empresa
                escpos.write(boldCenter, normalizarTexto(COMPANY_NAME))
                    .feed(1)
                    .write(center, normalizarTexto(COMPANY_ADDRESS))
                    .feed(1)
                    .write(center, normalizarTexto("Tel: $COMPANY_PHONE"))
                    .feed(1)
                    .write(center, normalizarTexto("CNPJ: $COMPANY_CNPJ"))
                    .feed(1)
                    .write(boldCenter, normalizarTexto("CUPOM DE RESERVA - VIA CLIENTE"))
                    .feed(1)
                    .writeLF(linhaDiv)

                val numeroPedido = normalizarTexto(pedidoData["numero"] as String)
                val dataPedido = normalizarTexto(formatarData(pedidoData["data_pedido"] as String))
                val statusPedido = normalizarTexto(pedidoData["status_pedido"] as String)
                val statusPagamento = normalizarTexto(pedidoData["status"] as String)


                escpos.write(boldCenter, normalizarTexto("PEDIDO #$numeroPedido"))
                    .feed(1)
                    .writeLF(normalizarTexto("Data: $dataPedido"))
                    .writeLF(normalizarTexto("Status: $statusPedido"))
                    .feed(1)
                    .writeLF(linhaDiv)

                val clienteInfo = pedidoData["cliente"] as? Map<String, Any>
                val telefoneContato = normalizarTexto(pedidoData["telefone_contato"] as? String ?: "")

                escpos.write(bold, "CLIENTE:")
                    .feed(1)
                escpos.write(smallFont, "")

                if (clienteInfo != null) {
                    val tipoCliente = clienteInfo["tipo"] as? String
                    if (tipoCliente == "PESSOA_FISICA" || tipoCliente == null) {
                        val nome = normalizarTexto(clienteInfo["nome"] as? String ?: "")
                        val sobrenome = normalizarTexto(clienteInfo["sobrenome"] as? String ?: "")
                        val nomeCompleto = "$nome $sobrenome".trim()

                        if (nomeCompleto.isEmpty()) {
                            imprimirTextoComWrapping(escpos, "Nome: Cliente nao identificado")
                        } else {
                            imprimirTextoComWrapping(escpos, "Nome: $nomeCompleto")
                        }
                    } else {
                        val razaoSocial = normalizarTexto(clienteInfo["razao_social"] as? String ?: "")
                        val nomeFantasia = normalizarTexto(clienteInfo["nome_fantasia"] as? String ?: "")
                        imprimirTextoComWrapping(escpos, "Empresa: $razaoSocial")
                        if (nomeFantasia.isNotEmpty()) {
                            imprimirTextoComWrapping(escpos, "Nome Fantasia: $nomeFantasia")
                        }
                    }

                    escpos.writeLF("Tel: ${clienteInfo["telefone"] ?: telefoneContato}")
                } else {
                    escpos.writeLF("Tel: $telefoneContato")
                }

                escpos.feed(1)
                    .writeLF(linhaDiv)
                escpos.write(Style(), "")

                escpos.write(bold, "PRODUTOS:")
                    .feed(1)

                val itens = pedidoData["itens"] as? List<Map<String, Any>> ?: emptyList()

                escpos.writeLF("QTD PRODUTO             VALOR   TOTAL")

                itens.forEach { item ->
                    val qtd = (item["quantidade"] as Number).toInt()
                    val nome = normalizarTexto(item["nome_produto"] as String)
                    val valorUnit = formatarValor(item["valor_unitario"] as Number)
                    val subtotal = formatarValor(item["subtotal"] as Number)
                    val pedidoObservacao = pedidoData["observacao"] as? String ?: ""

                    // Find the first space before position 20
                    val palavras = nome.split(" ")
                    var linhaAtual = ""
                    var primeiraLinha = true

                    for (palavra in palavras) {
                        if (linhaAtual.isEmpty()) {
                            if ((palavra.length <= 20 && primeiraLinha) || (!primeiraLinha && palavra.length <= 19)) {
                                linhaAtual = palavra
                            } else {
                                if (primeiraLinha) {
                                    escpos.writeLF(
                                        String.format(
                                            "%-3s %-20s %-7s %s",
                                            "${qtd}x", "", valorUnit, subtotal
                                        )
                                    )
                                }
                                escpos.writeLF("    $palavra")
                                primeiraLinha = false
                                continue
                            }
                        } else {
                            val novaLinha = "$linhaAtual $palavra"
                            if ((novaLinha.length <= 20 && primeiraLinha) || (!primeiraLinha && novaLinha.length <= 19)) {
                                linhaAtual = novaLinha
                            } else {
                                if (primeiraLinha) {
                                    escpos.writeLF(
                                        String.format(
                                            "%-3s %-20s %-7s %s",
                                            "${qtd}x", truncateText(linhaAtual, 20), valorUnit, subtotal
                                        )
                                    )
                                    primeiraLinha = false
                                } else {
                                    escpos.writeLF("    $linhaAtual")
                                }
                                linhaAtual = palavra
                            }
                        }
                    }

                    if (linhaAtual.isNotEmpty()) {
                        if (primeiraLinha) {
                            escpos.writeLF(
                                String.format(
                                    "%-3s %-20s %-7s %s",
                                    "${qtd}x", truncateText(linhaAtual, 20), valorUnit, subtotal
                                )
                            )
                        } else {
                            escpos.writeLF("    $linhaAtual")
                        }
                    }
//aqui
                    if (pedidoObservacao.isNotEmpty()) {
                        escpos.feed(1)
                        imprimirTextoComWrapping(escpos, "Observacao: $pedidoObservacao")
                    }
                }
//ate aqui
                escpos.feed(1)
                    .writeLF(linhaDiv)

                val valorTotal = pedidoData["valor_total"] as? Number ?: 0.0
                val valorDesconto = pedidoData["valor_desconto"] as? Number ?: 0.0
                val tipoDesconto = pedidoData["tipo_desconto"] as? String ?: ""
                val formaPagamento = pedidoData["forma_pagamento"] as? String ?: "Nao informado"
                val valorTrocoPara = pedidoData["valor_troco_para"] as? Number
                val valorTroco = pedidoData["valor_troco"] as? Number

                val entrega = pedidoData["entrega"] as? Map<String, Any>
                val valorEntrega = entrega?.get("valor_entrega") as? Number ?: 0.0

                escpos.write(bold, "PAGAMENTO:")
                    .feed(1)
                escpos.write(smallFont, "")
                imprimirTextoComWrapping(escpos, "Forma: $formaPagamento")

                if (((valorDesconto as? Number)?.toDouble() ?: 0.0) > 0) {
                    val descInfo = if (tipoDesconto == "percentual") {
                        // For percentage discounts, we need to calculate the actual percentage
                        val totalValue = (pedidoData["valor_total"] as? Number)?.toDouble() ?: 1.0
                        val descontoValue = (valorDesconto as? Number)?.toDouble() ?: 0.0

                        // Avoid division by zero
                        val percentualCalculado = if (totalValue > 0)
                            (descontoValue / (totalValue + descontoValue)) * 100
                        else 0.0

                        val percentualFormatado = String.format("%.2f", percentualCalculado)
                        "Desconto: ${percentualFormatado}%"
                    } else {
                        "Desconto: R$ $valorDesconto"
                    }
                    escpos.writeLF(descInfo)
                }

                if (valorEntrega.toDouble() > 0) {
                    escpos.writeLF("Taxa de entrega: R$ ${formatarValor(valorEntrega)}")
                }

                escpos.write(bold, "TOTAL: R$ $valorTotal")
                    .feed(1)

                if (formaPagamento == "Dinheiro" &&
                    valorTrocoPara != null && valorTrocoPara.toDouble() > 0 &&
                    valorTroco != null && valorTroco.toDouble() > 0) {
                    val valorTrocoParaStr = formatarValor(valorTrocoPara)
                    val valorTrocoStr = formatarValor(valorTroco)
                    escpos.writeLF("Troco para: R$ $valorTrocoParaStr")
                    escpos.writeLF("Troco: R$ $valorTrocoStr")
                }

                escpos.feed(1)
                    .writeLF("Pagamento: $statusPagamento") //pagamento
                escpos.feed(1)
                    .writeLF(linhaDiv)
                    .writeLF(linhaDiv)
                escpos.write(Style(), "")

                if (entrega != null) {
                    escpos.write(bold, "ENTREGA:")
                        .feed(1)
                    val nomeDestinatario = entrega["nome_destinatario"] as? String ?: ""
                    imprimirTextoComWrapping(escpos, "Destinatario: $nomeDestinatario")

                    val telefone = entrega["telefone_destinatario"] as? String ?: ""
                    if (telefone.isNotEmpty()) {
                        escpos.writeLF("Telefone: $telefone")
                    }

                    val numero = (entrega["numero"] as? String)?.takeIf { it.isNotBlank() } ?: "S/N"
                    val endereco = normalizarTexto("${entrega["endereco"]}, $numero")
                    if (endereco.isNotEmpty()) {
                        escpos.write(bold, "Endereco:")
                            .feed(1)
                        // Process address using word wrapping
                        val palavras = endereco.split(" ")
                        var linhaAtual = ""
                        var primeiraLinha = true

                        for (palavra in palavras) {
                            if (linhaAtual.isEmpty()) {
                                linhaAtual = if (primeiraLinha) palavra else "    $palavra"
                            } else {
                                val novaLinha = "$linhaAtual $palavra"
                                if ((novaLinha.length <= LINE_WIDTH && primeiraLinha) ||
                                    (!primeiraLinha && novaLinha.length <= LINE_WIDTH - 4)) {
                                    linhaAtual = novaLinha
                                } else {
                                    escpos.writeLF(linhaAtual)
                                    primeiraLinha = false
                                    linhaAtual = "    $palavra"
                                }
                            }
                        }

                        if (linhaAtual.isNotEmpty()) {
                            escpos.writeLF(linhaAtual)
                        }
                    }

                    val referencia = normalizarTexto(entrega["referencia"] as? String ?: "")
                    if (referencia.isNotEmpty()) {
                        escpos.write(bold, "Referencia:")
                            .feed(1)
                        // Process reference using word wrapping
                        val palavras = referencia.split(" ")
                        var linhaAtual = ""
                        var primeiraLinha = true

                        for (palavra in palavras) {
                            if (linhaAtual.isEmpty()) {
                                linhaAtual = if (primeiraLinha) palavra else "    $palavra"
                            } else {
                                val novaLinha = "$linhaAtual $palavra"
                                if ((novaLinha.length <= LINE_WIDTH && primeiraLinha) ||
                                    (!primeiraLinha && novaLinha.length <= LINE_WIDTH - 4)) {
                                    linhaAtual = novaLinha
                                } else {
                                    escpos.writeLF(linhaAtual)
                                    primeiraLinha = false
                                    linhaAtual = "    $palavra"
                                }
                            }
                        }

                        if (linhaAtual.isNotEmpty()) {
                            escpos.writeLF(linhaAtual)
                        }
                    }

                    val bairro = normalizarTexto(entrega["bairro"] as? String ?: "")
                    if (bairro.isNotEmpty()) {
                        imprimirTextoComWrapping(escpos, "Bairro: $bairro")
                    }

                    val cidade = normalizarTexto(entrega["cidade"] as? String ?: "")
                    if (cidade.isNotEmpty()) {
                        imprimirTextoComWrapping(escpos, "Cidade: $cidade")
                    }

                    val cep = entrega["cep"] as? String ?: ""
                    if (cep.isNotEmpty()) {
                        escpos.writeLF("CEP: $cep")
                    }

                    val dataEntregaRaw = entrega["data_entrega"] as? String
                    val horaEntregaRaw = entrega["hora_entrega"] as? String

                    val dataEntrega = if (!dataEntregaRaw.isNullOrBlank()) formatarData(dataEntregaRaw) else "A combinar"
                    val horaEntrega = if (!horaEntregaRaw.isNullOrBlank()) horaEntregaRaw else "A combinar"

                    escpos.writeLF("Data: $dataEntrega")
                    escpos.writeLF("Horario: $horaEntrega")

                } else {
                    // Informações de retirada
                    val dataRetirada = pedidoData["data_retirada"] as? String
                    val horaRetirada = pedidoData["hora_retirada"] as? String

                    if (dataRetirada != null && horaRetirada != null) {
                        escpos.write(bold, "RETIRADA PROGRAMADA:")
                            .feed(1)
                            .writeLF("Data: ${formatarData(dataRetirada)}")
                            .writeLF("Hora: $horaRetirada")
                            .writeLF("Local: ${normalizarTexto(COMPANY_ADDRESS)}")
                            .feed(1)
                    }
                }

                escpos.feed(1)
                    .writeLF(linhaDiv)

                // Rodape
                escpos.feed(1)
                    .write(center, COMPANY_NAME)
                    .feed(1)
                    .write(center, normalizarTexto("Obrigado pela preferência!"))
                    .feed(1)
                    .write(smallCenter, "Impresso em: " + SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Date()))
                    .feed(6)
                    .cut(CutMode.FULL)
            }
        }
    }

    private fun imprimirTextoComWrapping(escpos: EscPos, texto: String, maxWidth: Int = LINE_WIDTH) {
        if (texto.length <= maxWidth) {
            escpos.writeLF(texto)
            return
        }

        var remainingText = texto
        while (remainingText.isNotEmpty()) {
            val length = minOf(maxWidth, remainingText.length)
            val cutPoint = if (length < remainingText.length) {
                // Try to cut at a space to avoid breaking words
                val lastSpace = remainingText.substring(0, length).lastIndexOf(' ')
                if (lastSpace > maxWidth / 2) lastSpace + 1 else length
            } else {
                length
            }

            escpos.writeLF(remainingText.substring(0, cutPoint))
            remainingText = remainingText.substring(cutPoint).trimStart()
        }
    }

    private fun imprimirTextoIndentado(escpos: EscPos, texto: String, indentacao: Int) {
        val indent = " ".repeat(indentacao)
        val maxWidth = LINE_WIDTH - indentacao

        var remainingText = texto
        while (remainingText.isNotEmpty()) {
            val length = minOf(maxWidth, remainingText.length)
            val cutPoint = if (length < remainingText.length) {
                val lastSpace = remainingText.substring(0, length).lastIndexOf(' ')
                if (lastSpace > maxWidth / 2) lastSpace + 1 else length
            } else {
                length
            }

            escpos.writeLF(indent + remainingText.substring(0, cutPoint))
            remainingText = remainingText.substring(cutPoint).trimStart()
        }
    }

    private fun truncateText(text: String, maxLength: Int): String {
        return if (text.length <= maxLength) text else text.substring(0, maxLength)
    }

    private fun formatarData(data: String): String {
        try {
            val formatoOriginal = SimpleDateFormat("yyyy-MM-dd")
            val formatoDesejado = SimpleDateFormat("dd/MM/yyyy")
            val date = formatoOriginal.parse(data)
            return formatoDesejado.format(date)
        } catch (e: Exception) {
            return data
        }
    }

    private fun formatarValor(valor: Number): String {
        return String.format("%.2f", valor.toDouble())
    }
}