package com.sistema_pedidos.view

import com.sistema_pedidos.controller.NovoPedidoController
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.image.Image
import javafx.scene.image.ImageView

class NovoPedidoView(private val fieldWidth: Double = 300.0, private val maxFieldWidth: Double = 400.0) : VBox() {
    private val controller = NovoPedidoController()

    init {
        padding = Insets(15.0, 15.0, 0.0, 30.0)

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

        val nomeHBox = HBox(10.0).apply {
            children.addAll(
                VBox(10.0).apply {
                    HBox.setHgrow(this, Priority.ALWAYS)
                    children.addAll(
                        Label("Nome").apply { styleClass.add("field-label") },
                        TextField().apply {
                            styleClass.add("text-field")
                            maxWidth = Double.POSITIVE_INFINITY
                            HBox.setHgrow(this, Priority.ALWAYS)
                        }
                    )
                },
                VBox(10.0).apply {
                    HBox.setHgrow(this, Priority.ALWAYS)
                    children.addAll(
                        Label("Sobrenome").apply { styleClass.add("field-label") },
                        TextField().apply {
                            styleClass.add("text-field")
                            maxWidth = Double.POSITIVE_INFINITY
                            HBox.setHgrow(this, Priority.ALWAYS)
                        }
                    )
                }
            )
        }

        val contatoHBox = HBox(10.0).apply {
            children.addAll(
                VBox(10.0).apply {
                    children.addAll(
                        Label("Telefone").apply { styleClass.add("field-label") },
                        TextField().apply {
                            styleClass.add("text-field")
                            maxWidth = 130.0
                            controller.formatarTelefone(this)
                        }
                    )
                },
                VBox(10.0).apply {
                    HBox.setHgrow(this, Priority.ALWAYS)
                    children.addAll(
                        Label("Observação").apply { styleClass.add("field-label") },
                        TextField().apply {
                            styleClass.add("text-field")
                            maxWidth = Double.POSITIVE_INFINITY
                            HBox.setHgrow(this, Priority.ALWAYS)
                        }
                    )
                }
            )
        }

        val clienteVBox = VBox(10.0).apply {
            children.addAll(clienteSection, nomeHBox, contatoHBox)
        }

        val pedidosSection = HBox().apply {
            alignment = Pos.CENTER
            spacing = 10.0
            padding = Insets(20.0, 0.0, 0.0, 0.0)
            children.addAll(
                Separator().apply { prefWidth = 300.0 },
                Label("Pedido").apply { styleClass.add("section-label") },
                Separator().apply { prefWidth = 300.0 }
            )
        }

        val produtosHBox = HBox(10.0).apply {
            alignment = Pos.CENTER_LEFT
            children.addAll(
                VBox(10.0).apply {
                    children.addAll(
                        Label("Quantidade").apply { styleClass.add("field-label") },
                        HBox(5.0).apply {
                            alignment = Pos.CENTER_LEFT
                            val quantityBox = this
                            children.addAll(
                                Button().apply {
                                    styleClass.addAll("quantity-button", "flat-button")
                                    NovoPedidoView::class.java.getResourceAsStream("/icons/menosp.png")?.let {
                                        graphic = ImageView(Image(it)).apply {
                                            fitHeight = 30.0
                                            fitWidth = 30.0
                                            isPreserveRatio = true
                                        }
                                    }
                                    prefWidth = 35.0
                                    prefHeight = 35.0
                                    setOnAction {
                                        controller.decrementQuantity(quantityBox.children[1] as TextField)
                                    }
                                },
                                TextField().apply {
                                    styleClass.add("text-field")
                                    prefWidth = 50.0
                                    maxWidth = 50.0
                                    text = "1"
                                    alignment = Pos.CENTER
                                    textProperty().addListener { _, _, newValue ->
                                        controller.validateQuantity(this, newValue)
                                    }
                                },
                                Button().apply {
                                    styleClass.addAll("quantity-button", "flat-button")
                                    NovoPedidoView::class.java.getResourceAsStream("/icons/maisp.png")?.let {
                                        graphic = ImageView(Image(it)).apply {
                                            fitHeight = 30.0
                                            fitWidth = 30.0
                                            isPreserveRatio = true
                                        }
                                    }
                                    prefWidth = 35.0
                                    prefHeight = 35.0
                                    setOnAction {
                                        controller.incrementQuantity(quantityBox.children[1] as TextField)
                                    }
                                }
                            )
                        }
                    )
                },
                VBox(10.0).apply {
                    HBox.setHgrow(this, Priority.ALWAYS)
                    children.addAll(
                        Label("Produto").apply { styleClass.add("field-label") },
                        TextField().apply {
                            styleClass.add("text-field")
                            maxWidth = Double.POSITIVE_INFINITY
                            HBox.setHgrow(this, Priority.ALWAYS)
                        }
                    )
                },

                VBox(10.0).apply {
                    children.addAll(
                        Label("Valor Unitário").apply { styleClass.add("field-label") },
                        TextField().apply {
                            styleClass.add("text-field")
                            prefWidth = 50.0
                            maxWidth = 50.0
                        }
                    )
                },
                VBox(10.0).apply {
                    children.addAll(
                        Label("Subtotal").apply { styleClass.add("field-label") },
                        TextField().apply {
                            styleClass.add("text-field")
                            prefWidth = 50.0
                            maxWidth = 50.0
                            isEditable = false
                        }
                    )
                }
            )
        }
        // In NovoPedidoView class
        val produtosContainer = controller.getProdutosContainer()

        val adicionarProdutoButton = Button("Adicionar Produto").apply {
            styleClass.addAll("flat-button")
            prefWidth = 150.0
            prefHeight = 35.0
            setOnAction {
                controller.addNovoProduto()
            }
        }

        val pedidosContainer = VBox(10.0).apply {
            children.addAll(produtosContainer, adicionarProdutoButton)
        }


        children.addAll(clienteVBox, pedidosSection, pedidosContainer)    }
}