package com.sistema_pedidos.model

enum class TipoCliente {
    PESSOA_FISICA,
    PESSOA_JURIDICA
}

data class Cliente(
    val id: Long = 0,
    val tipo: TipoCliente = TipoCliente.PESSOA_FISICA,

    // PF fields
    val nome: String = "",
    val sobrenome: String = "",
    val cpf: String = "",

    // PJ fields
    val razaoSocial: String = "",
    val nomeFantasia: String = "",
    val cnpj: String = "",
    val inscricaoEstadual: String = "",

    // Common fields
    val telefone: String = "",
    val email: String = "",
    val observacao: String = "",

    // Address fields
    val cep: String = "",
    val logradouro: String = "",
    val numero: String = "",
    val complemento: String = "",
    val bairro: String = "",
    val cidade: String = "",
    val estado: String = ""
) {
    val nomeCompleto: String
        get() = if (sobrenome.isBlank()) nome else "$nome $sobrenome"

    val nomeDisplay: String
        get() = when (tipo) {
            TipoCliente.PESSOA_FISICA -> nomeCompleto
            TipoCliente.PESSOA_JURIDICA -> razaoSocial
        }

    val documentoDisplay: String
        get() = when (tipo) {
            TipoCliente.PESSOA_FISICA -> cpf
            TipoCliente.PESSOA_JURIDICA -> cnpj
        }

    val tipoDisplay: String
        get() = when (tipo) {
            TipoCliente.PESSOA_FISICA -> "PF"
            TipoCliente.PESSOA_JURIDICA -> "PJ"
        }
}