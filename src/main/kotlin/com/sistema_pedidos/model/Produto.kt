// Produto class definition
package com.sistema_pedidos.model

data class Produto(
    var id: Int,
    var nome: String,
    var quantidade: Int,
    var valorUnitario: Double
)