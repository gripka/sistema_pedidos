package com.sistema_pedidos.model

data class Produto(
    val id: Long = 0,
    val codigo: String = "",
    val nome: String = "",
    val descricao: String = "",
    val valorUnitario: Double = 0.0,
    val categoria: String = "",
    val unidadeMedida: String = "UN",
    val estoqueMinimo: Int = 0,
    val estoqueAtual: Int = 0,
    val status: String = "Ativo",
    val dataCadastro: String = "",
    val dataAtualizacao: String = "",
    val ehInsumo: Boolean = false
)