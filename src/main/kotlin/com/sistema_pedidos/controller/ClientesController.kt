package com.sistema_pedidos.controller

import com.sistema_pedidos.database.DatabaseHelper
import com.sistema_pedidos.model.Cliente
import com.sistema_pedidos.model.TipoCliente
import javafx.application.Platform
import javafx.scene.control.Alert
import java.sql.SQLException

class ClientesController {
    private val database = DatabaseHelper()

    init {
        atualizarEstruturaTabelaClientes()
    }

    private fun atualizarEstruturaTabelaClientes() {
        try {
            database.getConnection().use { conn ->
                // Check if new columns exist
                val rs = conn.metaData.getColumns(null, null, "clientes", "tipo")
                if (!rs.next()) {
                    // Add new columns if they don't exist
                    conn.createStatement().execute("""
                        ALTER TABLE clientes ADD COLUMN tipo TEXT 
                        CHECK (tipo IN ('PESSOA_FISICA', 'PESSOA_JURIDICA')) DEFAULT 'PESSOA_FISICA'
                    """)

                    // PF fields
                    conn.createStatement().execute("ALTER TABLE clientes ADD COLUMN cpf TEXT")

                    // PJ fields
                    conn.createStatement().execute("ALTER TABLE clientes ADD COLUMN razao_social TEXT")
                    conn.createStatement().execute("ALTER TABLE clientes ADD COLUMN nome_fantasia TEXT")
                    conn.createStatement().execute("ALTER TABLE clientes ADD COLUMN cnpj TEXT")
                    conn.createStatement().execute("ALTER TABLE clientes ADD COLUMN inscricao_estadual TEXT")

                    // Common fields
                    conn.createStatement().execute("ALTER TABLE clientes ADD COLUMN email TEXT")

                    // Address fields
                    conn.createStatement().execute("ALTER TABLE clientes ADD COLUMN cep TEXT")
                    conn.createStatement().execute("ALTER TABLE clientes ADD COLUMN logradouro TEXT")
                    conn.createStatement().execute("ALTER TABLE clientes ADD COLUMN numero TEXT")
                    conn.createStatement().execute("ALTER TABLE clientes ADD COLUMN complemento TEXT")
                    conn.createStatement().execute("ALTER TABLE clientes ADD COLUMN bairro TEXT")
                    conn.createStatement().execute("ALTER TABLE clientes ADD COLUMN cidade TEXT")
                    conn.createStatement().execute("ALTER TABLE clientes ADD COLUMN estado TEXT")
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    fun contarClientes(termo: String = ""): Int {
        var total = 0
        val termoBusca = "%${termo.trim()}%"

        try {
            database.getConnection().use { conn ->
                conn.prepareStatement("""
                SELECT COUNT(*) as total
                FROM clientes 
                WHERE nome LIKE ? 
                   OR sobrenome LIKE ? 
                   OR telefone LIKE ? 
                   OR cpf LIKE ? 
                   OR cnpj LIKE ?
                   OR razao_social LIKE ?
                   OR nome_fantasia LIKE ?
                   OR email LIKE ?
            """).use { stmt ->
                    stmt.setString(1, termoBusca)
                    stmt.setString(2, termoBusca)
                    stmt.setString(3, termoBusca)
                    stmt.setString(4, termoBusca)
                    stmt.setString(5, termoBusca)
                    stmt.setString(6, termoBusca)
                    stmt.setString(7, termoBusca)
                    stmt.setString(8, termoBusca)

                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        total = rs.getInt("total")
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            showAlert("Erro", "Falha ao contar clientes: ${e.message}")
        }

        return total
    }



    fun buscarClientesPaginado(termo: String = "", pagina: Int = 1, itensPorPagina: Int = 50): List<Cliente> {
        val clientes = mutableListOf<Cliente>()
        val termoBusca = "%${termo.trim()}%"
        val offset = (pagina - 1) * itensPorPagina

        try {
            database.getConnection().use { conn ->
                conn.prepareStatement("""
                SELECT id, tipo, 
                       nome, sobrenome, cpf,
                       razao_social, nome_fantasia, cnpj, inscricao_estadual,
                       telefone, email, observacao,
                       cep, logradouro, numero, complemento, bairro, cidade, estado
                FROM clientes 
                WHERE nome LIKE ? 
                   OR sobrenome LIKE ? 
                   OR telefone LIKE ? 
                   OR cpf LIKE ? 
                   OR cnpj LIKE ?
                   OR razao_social LIKE ?
                   OR nome_fantasia LIKE ?
                   OR email LIKE ?
                ORDER BY CASE tipo
                    WHEN 'PESSOA_FISICA' THEN nome
                    WHEN 'PESSOA_JURIDICA' THEN razao_social
                END
                LIMIT ? OFFSET ?
            """).use { stmt ->
                    stmt.setString(1, termoBusca)
                    stmt.setString(2, termoBusca)
                    stmt.setString(3, termoBusca)
                    stmt.setString(4, termoBusca)
                    stmt.setString(5, termoBusca)
                    stmt.setString(6, termoBusca)
                    stmt.setString(7, termoBusca)
                    stmt.setString(8, termoBusca)
                    stmt.setInt(9, itensPorPagina)
                    stmt.setInt(10, offset)

                    val rs = stmt.executeQuery()

                    while (rs.next()) {
                        // Your existing cliente mapping code here
                        // Copy from your existing buscarClientesPorTermo method
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            showAlert("Erro", "Falha ao buscar clientes: ${e.message}")
        }

        return clientes
    }

    fun buscarTodosClientes(): List<Cliente> {
        val clientes = mutableListOf<Cliente>()

        try {
            database.getConnection().use { conn ->
                conn.prepareStatement("""
                    SELECT id, tipo, 
                           nome, sobrenome, cpf,
                           razao_social, nome_fantasia, cnpj, inscricao_estadual,
                           telefone, email, observacao,
                           cep, logradouro, numero, complemento, bairro, cidade, estado
                    FROM clientes 
                    ORDER BY CASE tipo
                        WHEN 'PESSOA_FISICA' THEN nome
                        WHEN 'PESSOA_JURIDICA' THEN razao_social
                    END
                """).use { stmt ->
                    val rs = stmt.executeQuery()

                    while (rs.next()) {
                        val tipoCliente = try {
                            TipoCliente.valueOf(rs.getString("tipo") ?: "PESSOA_FISICA")
                        } catch (e: Exception) {
                            TipoCliente.PESSOA_FISICA
                        }

                        clientes.add(Cliente(
                            id = rs.getLong("id"),
                            tipo = tipoCliente,

                            // PF fields
                            nome = rs.getString("nome") ?: "",
                            sobrenome = rs.getString("sobrenome") ?: "",
                            cpf = rs.getString("cpf") ?: "",

                            // PJ fields
                            razaoSocial = rs.getString("razao_social") ?: "",
                            nomeFantasia = rs.getString("nome_fantasia") ?: "",
                            cnpj = rs.getString("cnpj") ?: "",
                            inscricaoEstadual = rs.getString("inscricao_estadual") ?: "",

                            // Common fields
                            telefone = rs.getString("telefone") ?: "",
                            email = rs.getString("email") ?: "",
                            observacao = rs.getString("observacao") ?: "",

                            // Address fields
                            cep = rs.getString("cep") ?: "",
                            logradouro = rs.getString("logradouro") ?: "",
                            numero = rs.getString("numero") ?: "",
                            complemento = rs.getString("complemento") ?: "",
                            bairro = rs.getString("bairro") ?: "",
                            cidade = rs.getString("cidade") ?: "",
                            estado = rs.getString("estado") ?: ""
                        ))
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            showAlert("Erro", "Falha ao buscar clientes: ${e.message}")
        }

        return clientes
    }

    fun buscarClientesPorTermo(termo: String): List<Cliente> {
        val clientes = mutableListOf<Cliente>()
        val termoBusca = "%${termo.trim()}%"

        try {
            database.getConnection().use { conn ->
                conn.prepareStatement("""
                    SELECT id, tipo, 
                           nome, sobrenome, cpf,
                           razao_social, nome_fantasia, cnpj, inscricao_estadual,
                           telefone, email, observacao,
                           cep, logradouro, numero, complemento, bairro, cidade, estado
                    FROM clientes 
                    WHERE nome LIKE ? 
                       OR sobrenome LIKE ? 
                       OR telefone LIKE ? 
                       OR cpf LIKE ? 
                       OR cnpj LIKE ?
                       OR razao_social LIKE ?
                       OR nome_fantasia LIKE ?
                       OR email LIKE ?
                    ORDER BY CASE tipo
                        WHEN 'PESSOA_FISICA' THEN nome
                        WHEN 'PESSOA_JURIDICA' THEN razao_social
                    END
                """).use { stmt ->
                    stmt.setString(1, termoBusca)
                    stmt.setString(2, termoBusca)
                    stmt.setString(3, termoBusca)
                    stmt.setString(4, termoBusca)
                    stmt.setString(5, termoBusca)
                    stmt.setString(6, termoBusca)
                    stmt.setString(7, termoBusca)
                    stmt.setString(8, termoBusca)

                    val rs = stmt.executeQuery()

                    while (rs.next()) {
                        val tipoCliente = try {
                            TipoCliente.valueOf(rs.getString("tipo") ?: "PESSOA_FISICA")
                        } catch (e: Exception) {
                            TipoCliente.PESSOA_FISICA
                        }

                        clientes.add(Cliente(
                            id = rs.getLong("id"),
                            tipo = tipoCliente,

                            // PF fields
                            nome = rs.getString("nome") ?: "",
                            sobrenome = rs.getString("sobrenome") ?: "",
                            cpf = rs.getString("cpf") ?: "",

                            // PJ fields
                            razaoSocial = rs.getString("razao_social") ?: "",
                            nomeFantasia = rs.getString("nome_fantasia") ?: "",
                            cnpj = rs.getString("cnpj") ?: "",
                            inscricaoEstadual = rs.getString("inscricao_estadual") ?: "",

                            // Common fields
                            telefone = rs.getString("telefone") ?: "",
                            email = rs.getString("email") ?: "",
                            observacao = rs.getString("observacao") ?: "",

                            // Address fields
                            cep = rs.getString("cep") ?: "",
                            logradouro = rs.getString("logradouro") ?: "",
                            numero = rs.getString("numero") ?: "",
                            complemento = rs.getString("complemento") ?: "",
                            bairro = rs.getString("bairro") ?: "",
                            cidade = rs.getString("cidade") ?: "",
                            estado = rs.getString("estado") ?: ""
                        ))
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            showAlert("Erro", "Falha ao buscar clientes: ${e.message}")
        }

        return clientes
    }

    fun adicionarCliente(cliente: Cliente): Boolean {
        try {
            database.getConnection().use { conn ->
                conn.prepareStatement("""
                INSERT INTO clientes (
                    tipo, 
                    nome, sobrenome, cpf,
                    razao_social, nome_fantasia, cnpj, inscricao_estadual,
                    telefone, email, observacao,
                    cep, logradouro, numero, complemento, bairro, cidade, estado
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """).use { stmt ->
                    stmt.setString(1, cliente.tipo.name)

                    // PF fields - with capitalization
                    stmt.setString(2, formatarTextoCapitalizado(cliente.nome))
                    stmt.setString(3, formatarTextoCapitalizado(cliente.sobrenome))
                    stmt.setString(4, cliente.cpf)

                    // PJ fields - with capitalization
                    stmt.setString(5, formatarTextoCapitalizado(cliente.razaoSocial))
                    stmt.setString(6, formatarTextoCapitalizado(cliente.nomeFantasia))
                    stmt.setString(7, cliente.cnpj)
                    stmt.setString(8, cliente.inscricaoEstadual)

                    // Common fields
                    stmt.setString(9, cliente.telefone)
                    stmt.setString(10, cliente.email)
                    stmt.setString(11, cliente.observacao)

                    // Address fields - with capitalization
                    stmt.setString(12, cliente.cep)
                    stmt.setString(13, formatarTextoCapitalizado(cliente.logradouro))
                    stmt.setString(14, cliente.numero)
                    stmt.setString(15, formatarTextoCapitalizado(cliente.complemento))
                    stmt.setString(16, formatarTextoCapitalizado(cliente.bairro))
                    stmt.setString(17, formatarTextoCapitalizado(cliente.cidade))
                    stmt.setString(18, cliente.estado)

                    stmt.executeUpdate()
                }
            }
            return true
        } catch (e: SQLException) {
            e.printStackTrace()
            val mensagemErro = if (e.message?.contains("UNIQUE constraint failed") == true) {
                "Já existe um cliente cadastrado com este telefone."
            } else {
                "Falha ao adicionar cliente: ${e.message}"
            }
            showAlert("Erro", mensagemErro)
            return false
        }
    }

    fun atualizarCliente(cliente: Cliente): Boolean {
        try {
            database.getConnection().use { conn ->
                conn.prepareStatement("""
                UPDATE clientes 
                SET tipo = ?,
                    nome = ?, sobrenome = ?, cpf = ?,
                    razao_social = ?, nome_fantasia = ?, cnpj = ?, inscricao_estadual = ?,
                    telefone = ?, email = ?, observacao = ?,
                    cep = ?, logradouro = ?, numero = ?, complemento = ?, bairro = ?, cidade = ?, estado = ?
                WHERE id = ?
            """).use { stmt ->
                    stmt.setString(1, cliente.tipo.name)

                    // PF fields - with capitalization
                    stmt.setString(2, formatarTextoCapitalizado(cliente.nome))
                    stmt.setString(3, formatarTextoCapitalizado(cliente.sobrenome))
                    stmt.setString(4, cliente.cpf)

                    // PJ fields - with capitalization
                    stmt.setString(5, formatarTextoCapitalizado(cliente.razaoSocial))
                    stmt.setString(6, formatarTextoCapitalizado(cliente.nomeFantasia))
                    stmt.setString(7, cliente.cnpj)
                    stmt.setString(8, cliente.inscricaoEstadual)

                    // Common fields
                    stmt.setString(9, cliente.telefone)
                    stmt.setString(10, cliente.email)
                    stmt.setString(11, cliente.observacao)

                    // Address fields - with capitalization
                    stmt.setString(12, cliente.cep)
                    stmt.setString(13, formatarTextoCapitalizado(cliente.logradouro))
                    stmt.setString(14, cliente.numero)
                    stmt.setString(15, formatarTextoCapitalizado(cliente.complemento))
                    stmt.setString(16, formatarTextoCapitalizado(cliente.bairro))
                    stmt.setString(17, formatarTextoCapitalizado(cliente.cidade))
                    stmt.setString(18, cliente.estado)

                    stmt.setLong(19, cliente.id)

                    stmt.executeUpdate()
                }
            }
            return true
        } catch (e: SQLException) {
            e.printStackTrace()
            val mensagemErro = if (e.message?.contains("UNIQUE constraint failed") == true) {
                "Já existe um cliente cadastrado com este telefone."
            } else {
                "Falha ao atualizar cliente: ${e.message}"
            }
            showAlert("Erro", mensagemErro)
            return false
        }
    }

    // Function to format text with proper capitalization, respecting exceptions
    private fun formatarTextoCapitalizado(texto: String): String {
        val excecoes = setOf("de", "da", "do", "das", "dos", "e", "com", "para", "a", "o", "em",
            "por", "sem", "sob", "sobre", "à", "às", "ao", "aos")

        return texto.lowercase().split(" ").joinToString(" ") { palavra ->
            if (palavra in excecoes) palavra else palavra.replaceFirstChar { it.uppercase() }
        }
    }

    fun excluirCliente(id: Long): Boolean {
        try {
            database.getConnection().use { conn ->
                // Check if client has orders
                conn.prepareStatement("SELECT COUNT(*) FROM pedidos WHERE cliente_id = ?").use { checkStmt ->
                    checkStmt.setLong(1, id)
                    val rs = checkStmt.executeQuery()
                    if (rs.next() && rs.getInt(1) > 0) {
                        showAlert("Erro", "Não é possível excluir o cliente pois ele possui pedidos vinculados.")
                        return false
                    }
                }

                // Delete client if no orders are found
                conn.prepareStatement("DELETE FROM clientes WHERE id = ?").use { stmt ->
                    stmt.setLong(1, id)
                    stmt.executeUpdate()
                }
            }
            return true
        } catch (e: SQLException) {
            e.printStackTrace()
            showAlert("Erro", "Falha ao excluir cliente: ${e.message}")
            return false
        }
    }

    fun buscarClientePorTelefone(telefone: String): Cliente? {
        try {
            database.getConnection().use { conn ->
                conn.prepareStatement("""
                    SELECT id, tipo, 
                           nome, sobrenome, cpf,
                           razao_social, nome_fantasia, cnpj, inscricao_estadual,
                           telefone, email, observacao,
                           cep, logradouro, numero, complemento, bairro, cidade, estado
                    FROM clientes 
                    WHERE telefone = ?
                """).use { stmt ->
                    stmt.setString(1, telefone)
                    val rs = stmt.executeQuery()

                    if (rs.next()) {
                        val tipoCliente = try {
                            TipoCliente.valueOf(rs.getString("tipo") ?: "PESSOA_FISICA")
                        } catch (e: Exception) {
                            TipoCliente.PESSOA_FISICA
                        }

                        return Cliente(
                            id = rs.getLong("id"),
                            tipo = tipoCliente,

                            // PF fields
                            nome = rs.getString("nome") ?: "",
                            sobrenome = rs.getString("sobrenome") ?: "",
                            cpf = rs.getString("cpf") ?: "",

                            // PJ fields
                            razaoSocial = rs.getString("razao_social") ?: "",
                            nomeFantasia = rs.getString("nome_fantasia") ?: "",
                            cnpj = rs.getString("cnpj") ?: "",
                            inscricaoEstadual = rs.getString("inscricao_estadual") ?: "",

                            // Common fields
                            telefone = rs.getString("telefone") ?: "",
                            email = rs.getString("email") ?: "",
                            observacao = rs.getString("observacao") ?: "",

                            // Address fields
                            cep = rs.getString("cep") ?: "",
                            logradouro = rs.getString("logradouro") ?: "",
                            numero = rs.getString("numero") ?: "",
                            complemento = rs.getString("complemento") ?: "",
                            bairro = rs.getString("bairro") ?: "",
                            cidade = rs.getString("cidade") ?: "",
                            estado = rs.getString("estado") ?: ""
                        )
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            showAlert("Erro", "Falha ao buscar cliente por telefone: ${e.message}")
        }

        return null
    }

    private fun showAlert(title: String, message: String) {
        val alert = Alert(Alert.AlertType.ERROR).apply {
            this.title = title
            headerText = null
            contentText = message

            dialogPane.styleClass.add("custom-dialog")
        }

        with(alert.dialogPane) {
            lookupAll(".content").forEach { it.styleClass.add("dialog-content") }
            lookupAll(".header-panel").forEach { it.styleClass.add("dialog-header") }
            lookupAll(".button-bar").forEach { it.styleClass.add("dialog-button-bar") }
        }

        Platform.runLater {
            alert.dialogPane.buttonTypes.forEach { buttonType ->
                val button = alert.dialogPane.lookupButton(buttonType)
                button.styleClass.add("dialog-button")
            }
        }

        alert.showAndWait()
    }
}