package com.sistema_pedidos.model

import java.sql.Timestamp

data class Pedido(
    val id: Long? = null,
    val numeroPedido: String? = null,
    val clienteId: Long,
    val dataPedido: Timestamp = Timestamp(System.currentTimeMillis()),
    val total: Double,
    val dataEntrega: Timestamp? = null,
    val horaEntrega: Timestamp? = null
)
