package com.sistema_pedidos.model

data class Desconto(
    val id: Long? = null,
    val descricao: String,
    val tipo: String,
    val valor: Double,
    val ativo: Boolean = true
)
