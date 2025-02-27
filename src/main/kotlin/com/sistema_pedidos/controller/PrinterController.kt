package com.sistema_pedidos.controller

import com.github.anastaciocintra.escpos.EscPos
import com.github.anastaciocintra.escpos.Style
import com.github.anastaciocintra.escpos.EscPosConst
import com.github.anastaciocintra.escpos.barcode.BarCode
import com.github.anastaciocintra.escpos.EscPos.CutMode   // Add this import
import com.github.anastaciocintra.output.PrinterOutputStream
import java.io.IOException
import javax.print.PrintService
import javax.print.PrintServiceLookup

class PrinterController {
    fun imprimirPedido(
        numeroPedido: String,
        clienteInfo: List<Pair<String, String>>,
        produtos: List<Map<String, String>>,
        pagamentoInfo: List<Pair<String, String>>,
        entregaInfo: List<Pair<String, String>>
    ) {
        val printService = PrintServiceLookup.lookupDefaultPrintService()
            ?: throw IOException("Nenhuma impressora térmica encontrada.")

        PrinterOutputStream(printService).use { outputStream ->
            EscPos(outputStream).use { escpos ->
                // Cabeçalho
                escpos.writeLF("     FLORICULTURA PORTAL")
                    .writeLF("     PEDIDO #$numeroPedido")
                    .writeLF("Data: ${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}")
                    .feed(1)
                    .writeLF("--------------------------------")

                // Cliente
                escpos.writeLF("CLIENTE:")
                clienteInfo.forEach { (key, value) ->
                    if (value.isNotEmpty()) {
                        escpos.writeLF("$key: $value")
                    }
                }
                escpos.feed(1)
                    .writeLF("--------------------------------")

                // Produtos
                escpos.writeLF("PRODUTOS:")
                escpos.writeLF("Qtd Produto          Valor    Total")
                escpos.writeLF("--------------------------------")
                produtos.forEach { produto ->
                    escpos.writeLF("${produto["quantidade"]}x ${produto["nome"]}")
                    escpos.writeLF("   R$ ${produto["valorUnitario"]} = R$ ${produto["subtotal"]}")
                }
                escpos.feed(1)
                    .writeLF("--------------------------------")

                // Pagamento
                escpos.writeLF("PAGAMENTO:")
                val filteredPagamento = if (entregaInfo.first().second == "Sim") {
                    pagamentoInfo.filter { !it.first.contains("Retirada") }
                } else {
                    pagamentoInfo
                }
                filteredPagamento.forEach { (key, value) ->
                    if (value.isNotEmpty()) {
                        escpos.writeLF("$key: $value")
                    }
                }
                escpos.feed(1)
                    .writeLF("--------------------------------")

                // Entrega (se houver)
                if (entregaInfo.first().second == "Sim") {
                    escpos.writeLF("ENTREGA:")
                    entregaInfo.drop(1).forEach { (key, value) ->
                        if (value.isNotEmpty()) {
                            escpos.writeLF("$key: $value")
                        }
                    }
                    escpos.feed(1)
                        .writeLF("--------------------------------")
                }


                escpos.feed(3)
                    .cut(CutMode.FULL)
            }
        }
    }
}