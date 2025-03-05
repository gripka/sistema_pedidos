package com.sistema_pedidos.controller

import com.sistema_pedidos.database.DatabaseHelper
import com.sistema_pedidos.model.Cliente
import javafx.scene.control.Alert
import java.sql.SQLException

class ClientesController {
    private val database = DatabaseHelper()

    fun buscarTodosClientes(): List<Cliente> {
        val clientes = mutableListOf<Cliente>()

        try {
            database.getConnection().use { conn ->
                conn.prepareStatement(
                    "SELECT id, nome, sobrenome, telefone, observacao FROM clientes ORDER BY nome"
                ).use { stmt ->
                    val rs = stmt.executeQuery()

                    while (rs.next()) {
                        clientes.add(
                            Cliente(
                                id = rs.getLong("id"),
                                nome = rs.getString("nome"),
                                sobrenome = rs.getString("sobrenome"),
                                telefone = rs.getString("telefone"),
                                observacao = rs.getString("observacao")
                            )
                        )
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
                conn.prepareStatement(
                    """
                    SELECT id, nome, sobrenome, telefone, observacao 
                    FROM clientes 
                    WHERE nome LIKE ? OR sobrenome LIKE ? OR telefone LIKE ?
                    ORDER BY nome
                    """
                ).use { stmt ->
                    stmt.setString(1, termoBusca)
                    stmt.setString(2, termoBusca)
                    stmt.setString(3, termoBusca)

                    val rs = stmt.executeQuery()

                    while (rs.next()) {
                        clientes.add(
                            Cliente(
                                id = rs.getLong("id"),
                                nome = rs.getString("nome"),
                                sobrenome = rs.getString("sobrenome"),
                                telefone = rs.getString("telefone"),
                                observacao = rs.getString("observacao")
                            )
                        )
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
                conn.prepareStatement(
                    """
                    INSERT INTO clientes (nome, sobrenome, telefone, observacao)
                    VALUES (?, ?, ?, ?)
                    """
                ).use { stmt ->
                    stmt.setString(1, cliente.nome)
                    stmt.setString(2, cliente.sobrenome)
                    stmt.setString(3, cliente.telefone)
                    stmt.setString(4, cliente.observacao)

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
                conn.prepareStatement(
                    """
                    UPDATE clientes 
                    SET nome = ?, sobrenome = ?, telefone = ?, observacao = ?
                    WHERE id = ?
                    """
                ).use { stmt ->
                    stmt.setString(1, cliente.nome)
                    stmt.setString(2, cliente.sobrenome)
                    stmt.setString(3, cliente.telefone)
                    stmt.setString(4, cliente.observacao)
                    stmt.setLong(5, cliente.id)

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
                conn.prepareStatement(
                    """
                    SELECT id, nome, sobrenome, telefone, observacao
                    FROM clientes 
                    WHERE telefone = ?
                    """
                ).use { stmt ->
                    stmt.setString(1, telefone)
                    val rs = stmt.executeQuery()

                    if (rs.next()) {
                        return Cliente(
                            id = rs.getLong("id"),
                            nome = rs.getString("nome"),
                            sobrenome = rs.getString("sobrenome"),
                            telefone = rs.getString("telefone"),
                            observacao = rs.getString("observacao")
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
        Alert(Alert.AlertType.ERROR).apply {
            this.title = title
            this.headerText = null
            this.contentText = message
            showAndWait()
        }
    }
}