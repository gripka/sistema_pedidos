package com.sistema_pedidos.model

import java.sql.Timestamp

data class Notificacao(
    val id: Long? = null,
    val pedidoId: Long,
    val tipoNotificacao: String,
    val mensagem: String,
    val dataEnvio: Timestamp = Timestamp(System.currentTimeMillis()),
    val lido: Boolean = false
)
