package com.sistema_pedidos.controller

import com.sistema_pedidos.view.NovoPedidoView

class NovoPedidoController {
    private val novoPedidoView = NovoPedidoView()

    fun getView(): NovoPedidoView {
        return novoPedidoView
    }

    // Adicione métodos para manipular a lógica do NovoPedidoView aqui
}