package com.sistema_pedidos.model

import java.sql.Timestamp

data class Pagamento(
    val id: Long? = null,
    val pedidoId: Long,
    val formaPagamento: String,
    val valorPago: Double,
    val dataPagamento: Timestamp = Timestamp(System.currentTimeMillis()),
    val status: String
)
