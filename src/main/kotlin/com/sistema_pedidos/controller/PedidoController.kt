package com.sistema_pedidos.controller

import com.sistema_pedidos.model.Produto
import javafx.collections.FXCollections
import javafx.collections.ObservableList

class PedidoController {
    // Lista de produtos do pedido como ObservableList
    val itensPedido: ObservableList<Produto> = FXCollections.observableArrayList()

    // Função para adicionar produto
    fun adicionarProduto(nome: String, preco: Double, quantidade: Int) {
        val produto = Produto(nome, preco, quantidade)
        itensPedido.add(produto)
    }

    // Função para calcular o total do pedido
    fun calcularTotal(): Double {
        return itensPedido.sumOf { it.preco * it.quantidade }
    }
}
