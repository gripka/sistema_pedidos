// Produto class definition
package com.sistema_pedidos.model

data class Produto(
    val id: Int,
    val nome: String,
    val quantidade: Int,
    val valorUnitario: Double
)