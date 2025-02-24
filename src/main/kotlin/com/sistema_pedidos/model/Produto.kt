package com.sistema_pedidos.model

data class Produto(
    val id: Long? = null,
    val codigo: String? = null,
    val nome: String,
    val descricao: String?,
    val valorUnitario: Double,
    val categoria: String?,
    val unidadeMedida: String = "UN",
    val estoqueMinimo: Int = 0,
    val estoqueAtual: Int = 0,
    val status: String = "Ativo",
    val dataCadastro: String? = null,
    val dataAtualizacao: String? = null
)