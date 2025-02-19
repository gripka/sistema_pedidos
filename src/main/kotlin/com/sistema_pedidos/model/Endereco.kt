package com.sistema_pedidos.model

data class Endereco(
    val id: Long? = null,
    val clienteId: Long,
    val endereco: String,
    val referencia: String?,
    val cidade: String,
    val estado: String,
    val cep: String?
)
