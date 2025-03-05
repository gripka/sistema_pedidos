package com.sistema_pedidos.model

data class Cliente(
    val id: Long = 0,
    val nome: String,
    val sobrenome: String? = "",
    val telefone: String,
    val observacao: String? = ""
) {
    val nomeCompleto: String
        get() = if (sobrenome.isNullOrBlank()) nome else "$nome $sobrenome"
}