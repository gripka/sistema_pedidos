package com.sistema_pedidos.model

import java.sql.Timestamp

data class LogUsuario(
    val id: Long? = null,
    val usuarioId: Long,
    val acao: String,
    val dataAcao: Timestamp = Timestamp(System.currentTimeMillis())
)
