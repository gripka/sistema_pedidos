package com.sistema_pedidos.model

data class Cliente(
    val id: Long? = null,
    val nome: String,
    val sobrenome: String,
    val telefone: String?,
    val observacao: String?
)
