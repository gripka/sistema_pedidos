package com.sistema_pedidos.view

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.Separator
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox

class NovoPedidoView(private val fieldWidth: Double = 300.0, private val maxFieldWidth: Double = 400.0) : VBox() {
    init {
        padding = Insets(10.0, 0.0, 0.0, 10.0)

        stylesheets.add(javaClass.getResource("/novopedidoview.css").toExternalForm())

        val clienteSection = HBox().apply {
            alignment = Pos.CENTER
            spacing = 10.0
            children.addAll(
                Separator().apply { prefWidth = 300.0 },
                Label("Cliente").apply { styleClass.add("section-label") },
                Separator().apply { prefWidth = 300.0 }
            )
        }

        val nomeTelefoneVBox = VBox(10.0).apply {
            children.addAll(
                Label("Nome").apply { styleClass.add("field-label") },
                TextField().apply {
                    styleClass.add("text-field")
                    prefWidth = fieldWidth
                    maxWidth = maxFieldWidth
                },
                Label("Telefone").apply { styleClass.add("field-label") },
                TextField().apply {
                    styleClass.add("text-field")
                    prefWidth = fieldWidth
                    maxWidth = maxFieldWidth
                }
            )
        }

        val observacaoVBox = VBox(10.0).apply {
            children.addAll(
                Label("Observação").apply { styleClass.add("field-label") },
                TextArea().apply {
                    styleClass.add("text-area")
                    prefWidth = fieldWidth
                    maxWidth = maxFieldWidth
                    prefHeight = 100.0 // Adjust the height as needed
                }
            )
        }

        val clienteHBox = HBox(10.0).apply {
            children.addAll(nomeTelefoneVBox, observacaoVBox)
        }

        val produtosSection = HBox().apply {
            alignment = Pos.CENTER
            spacing = 10.0
            children.addAll(
                Separator().apply { prefWidth = 300.0 },
                Label("Produtos").apply { styleClass.add("section-label") },
                Separator().apply { prefWidth = 300.0 }
            )
        }

        children.addAll(clienteSection, clienteHBox, produtosSection)
    }
}