package com.sistema_pedidos.model

data class Entrega(
    val id: Long? = null,
    val pedidoId: Long,
    val nomeDestinatario: String,
    val telefoneDestinatario: String?,
    val endereco: String,
    val referencia: String?,
    val cidade: String,
    val bairro: String,
    val cep: String?,
    val valorEntrega: Double?,
    val dataEntrega: String,
    val horaEntrega: String
)