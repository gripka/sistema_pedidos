package com.sistema_pedidos.model

import java.sql.Timestamp

data class Pedido(
    val id: Long? = null,
    val numero: String,
    val clienteId: Long? = null,
    val telefoneContato: String,
    val dataPedido: String? = null,
    val observacao: String?,
    val status: String = "Pendente",
    val valorTotal: Double = 0.0,
    val valorDesconto: Double = 0.0,
    val tipoDesconto: String?,
    val formaPagamento: String?,
    val valorTrocoPara: Double?,
    val valorTroco: Double?,
    val dataRetirada: String? = null,
    val horaRetirada: String? = null
)
