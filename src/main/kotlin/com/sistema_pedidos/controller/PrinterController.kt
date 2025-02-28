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

class PrinterController {
    private val prefs = Preferences.userNodeForPackage(PrinterController::class.java)
    private val DEFAULT_PRINTER_KEY = "default_thermal_printer"

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
        numeroPedido: String,
        clienteInfo: List<Pair<String, String>>,
        produtos: List<Map<String, String>>,
        pagamentoInfo: List<Pair<String, String>>,
        entregaInfo: List<Pair<String, String>>
    ) {
        try {
            // Use a impressora especificada ou obtenha a impressora padrão
            val impressora = printService ?: getImpressoraPadrao()
            ?: throw IOException("Nenhuma impressora térmica encontrada.")

            if (!isThermalPrinter(impressora)) {
                throw IOException("A impressora ${impressora.name} não é uma impressora térmica.")
            }

            println("Usando impressora térmica: ${impressora.name}")
            imprimirEmTermica(impressora, numeroPedido, clienteInfo, produtos, pagamentoInfo, entregaInfo)
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
        numeroPedido: String,
        clienteInfo: List<Pair<String, String>>,
        produtos: List<Map<String, String>>,
        pagamentoInfo: List<Pair<String, String>>,
        entregaInfo: List<Pair<String, String>>
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

                // Cabeçalho
                escpos.write(boldCenter, "FLORICULTURA PORTAL")
                    .feed(1)
                    .write(boldCenter, "PEDIDO #$numeroPedido")
                    .feed(1)
                    .write(center, "Data: ${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}")
                    .feed(1)
                    .writeLF("--------------------------------")

                // Cliente
                escpos.write(bold, "CLIENTE:")
                    .feed(1)
                clienteInfo.forEach { (key, value) ->
                    if (value.isNotEmpty()) {
                        escpos.writeLF("$key: $value")
                    }
                }
                escpos.feed(1)
                    .writeLF("--------------------------------")

                // Produtos
                escpos.write(bold, "PRODUTOS:")
                    .feed(1)

                produtos.forEachIndexed { index, produto ->
                    val qtd = produto["quantidade"] ?: "0"
                    val nome = produto["nome"] ?: "Sem nome"
                    val valorUnit = produto["valorUnitario"] ?: "0,00"
                    val subtotal = produto["subtotal"] ?: "0,00"

                    // Formato para nome de produto longo: quebra em múltiplas linhas
                    val maxLen = 32
                    if (nome.length > maxLen) {
                        escpos.writeLF("${qtd}x ${nome.take(maxLen)}")
                        var pos = maxLen
                        while (pos < nome.length) {
                            escpos.writeLF("   ${nome.substring(pos, Math.min(pos + maxLen, nome.length))}")
                            pos += maxLen
                        }
                    } else {
                        escpos.writeLF("${qtd}x $nome")
                    }

                    escpos.writeLF("   R$ $valorUnit = R$ $subtotal")

                    // Linha em branco entre produtos, exceto no último
                    if (index < produtos.size - 1) escpos.feed(1)
                }
                escpos.feed(1)
                    .writeLF("--------------------------------")

                // Pagamento
                escpos.write(bold, "PAGAMENTO:")
                    .feed(1)
                val filteredPagamento = pagamentoInfo.filter { it.second.isNotEmpty() }
                filteredPagamento.forEach { (key, value) ->
                    escpos.writeLF("$key: $value")
                }
                escpos.feed(1)
                    .writeLF("--------------------------------")

                // Entrega (se houver)
                if (entregaInfo.isNotEmpty() && entregaInfo.first().second == "Sim") {
                    escpos.write(bold, "ENTREGA:")
                        .feed(1)
                    entregaInfo.drop(1).filter { it.second.isNotEmpty() }.forEach { (key, value) ->
                        escpos.writeLF("$key: $value")
                    }
                    escpos.feed(1)
                        .writeLF("--------------------------------")
                }

                // Rodapé
                escpos.feed(1)
                    .write(center, "Obrigado pela preferência!")
                    .feed(1)
                    .write(center, "www.floweronline.com.br")
                    .feed(3)
                    .cut(CutMode.FULL)
            }
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
