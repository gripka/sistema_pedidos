package com.sistema_pedidos.controller

import com.sistema_pedidos.model.Produto
import javafx.collections.FXCollections
import javafx.collections.ObservableList

class PedidoController {
    // Lista de produtos do pedido como ObservableList
    val itensPedido: ObservableList<Produto> = FXCollections.observableArrayList()

    // Função para adicionar produto
    fun adicionarProduto(id: Int, nome: String, preco: Double, quantidade: Int) {
        val produto = Produto(id, nome, quantidade, preco)
        itensPedido.add(produto)
    }

    // Função para calcular o total do pedido
    fun calcularTotal(): Double {
        return itensPedido.sumOf { it.valorUnitario * it.quantidade }
    }
}