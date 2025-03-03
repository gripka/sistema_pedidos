package com.sistema_pedidos.controller

import com.sistema_pedidos.model.Produto
import javafx.collections.FXCollections
import javafx.collections.ObservableList

class PedidoController {
    val itensPedido: ObservableList<Produto> = FXCollections.observableArrayList()

    fun adicionarProduto(id: Long, nome: String, valorUnitario: Double, quantidade: Int) {
        val produto = Produto(
            id = id,
            codigo = null.toString(),
            nome = nome,
            descricao = null.toString(),
            valorUnitario = valorUnitario,
            categoria = null.toString(),
            unidadeMedida = "UN",
            estoqueMinimo = 0,
            estoqueAtual = 0,
            status = "Ativo",
            dataCadastro = null.toString(),
            dataAtualizacao = null.toString()
        )
        itensPedido.add(produto)
    }

    fun calcularTotal(): Double {
        return itensPedido.sumOf { it.valorUnitario }
    }
}