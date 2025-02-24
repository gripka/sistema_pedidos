package com.sistema_pedidos.model

data class ItemPedido(
    val id: Long? = null,
    val pedidoId: Long,
    val produtoId: Long,
    val quantidade: Int = 1,
    val valorUnitario: Double,
    val subtotal: Double
)
