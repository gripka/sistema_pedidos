package com.sistema_pedidos.model

data class Produto(
    var id: Int,
    var nome: String,
    var quantidade: Int,
    var valorUnitario: Double,
    var descricao: String? = null,
    var categoria: String? = null,
    var status: String = "ativo"
)