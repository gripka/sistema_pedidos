package com.sistema_pedidos.model

data class Usuario(
    val id: Long? = null,
    val nomeUsuario: String,
    val senha: String,
    val role: String,
    val ativo: Boolean = true
)
