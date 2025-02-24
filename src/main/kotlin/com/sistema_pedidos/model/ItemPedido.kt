package com.sistema_pedidos.model

data class ItemPedido(
    val id: Long? = null,
    val pedidoId: Long,
    val produtoId: Long, // Changed from String to Long
    val quantidade: Int,
    val valorUnitario: Double,
    val subtotal: Double
)
