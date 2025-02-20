package com.sistema_pedidos.view

import com.sistema_pedidos.controller.NovoPedidoController
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*

class NovoPedidoView : BorderPane() {
    private val controller = NovoPedidoController()

    // Container do conteúdo que pode rolar
    private val contentContainer = VBox().apply {
        spacing = 10.0
        padding = Insets(15.0, 20.0, 20.0, 30.0)
        children.addAll(
            createClientSection(),
            createPedidosSection(),
            createPagamentoSection(),
            createEntregaSection()
        )
    }

    init {
        stylesheets.add(javaClass.getResource("/novopedidoview.css").toExternalForm())

        // Criar o ScrollPane para o conteúdo rolável
        val scrollPane = ScrollPane(contentContainer).apply {
            isFitToWidth = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
            styleClass.add("main-scroll-pane")
        }

        // Cria um VBox para o layout principal com o ScrollPane e a BottomBar fixa
        val mainLayout = VBox().apply {
            // Coloca o ScrollPane no VBox e faz ele crescer para ocupar o espaço restante
            children.add(scrollPane)
            VBox.setVgrow(scrollPane, Priority.ALWAYS)

            // Coloca a BottomBar fixa no final
            children.add(createBottomBar()) // BottomBar fixa
        }

        // Define o layout principal no centro do BorderPane
        center = mainLayout
    }

    // Criar a BottomBar fixa
    private fun createBottomBar(): HBox {
        return HBox().apply {
            padding = Insets(20.0)
            alignment = Pos.CENTER
            spacing = 20.0
            maxHeight = 80.0
            minHeight = 80.0
            styleClass.add("bottom-bar")
            style = """
            -fx-background-color: white;
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, -5);
            -fx-border-width: 1 0 0 0;
            -fx-border-color: #e0e0e0;
        """

            val totalLabel = Label("Total do Pedido: R$ 0,00").apply {
                styleClass.add("total-label")
            }

            val salvarButton = Button("Salvar Pedido").apply {
                styleClass.add("salvar-button")
                prefWidth = 200.0
                prefHeight = 40.0
            }

            children.addAll(
                Region().apply { HBox.setHgrow(this, Priority.ALWAYS) },
                totalLabel,
                salvarButton,
                Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }
            )
        }
    }

    private fun createClientSection(): VBox {
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

        return VBox(10.0).apply {
            children.addAll(clienteSection, nomeHBox, contatoHBox)
        }
    }

    private fun createPedidosSection(): VBox {
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

        val produtosContainer = controller.getProdutosContainer()

        val adicionarProdutoButton = Button("Adicionar Produto").apply {
            styleClass.addAll("novo-pedido-button")
            prefWidth = 150.0
            prefHeight = 35.0
            setOnAction {
                controller.addNovoProduto()
            }
        }

        return VBox(10.0).apply {
            children.addAll(pedidosSection, produtosContainer, adicionarProdutoButton)
        }
    }

    private fun createPagamentoSection(): VBox {
        val pagamentoSection = HBox().apply {
            alignment = Pos.CENTER
            spacing = 10.0
            padding = Insets(20.0, 0.0, 0.0, 0.0)
            children.addAll(
                Separator().apply { prefWidth = 300.0 },
                Label("Pagamento").apply { styleClass.add("section-label") },
                Separator().apply { prefWidth = 300.0 }
            )
        }

        return VBox(10.0).apply {
            children.addAll(
                pagamentoSection,
                HBox(10.0).apply {
                    children.addAll(
                        VBox(10.0).apply {
                            children.addAll(
                                Label("Forma de Pagamento").apply { styleClass.add("field-label") },
                                ComboBox<String>().apply {
                                    items.addAll("Dinheiro", "Cartão de Crédito", "Cartão de Débito", "PIX")
                                    styleClass.add("combo-box")
                                    prefWidth = 200.0
                                }
                            )
                        },
                        VBox(10.0).apply {
                            children.addAll(
                                Label("Troco Para").apply { styleClass.add("field-label") },
                                TextField().apply {
                                    styleClass.add("text-field")
                                    prefWidth = 100.0
                                    maxWidth = 100.0
                                    alignment = Pos.CENTER_RIGHT
                                    isDisable = true
                                }
                            )
                        },
                        VBox(10.0).apply {
                            children.addAll(
                                Label("Status").apply { styleClass.add("field-label") },
                                ComboBox<String>().apply {
                                    items.addAll("Pendente", "Pago")
                                    value = "Pendente"
                                    styleClass.add("combo-box")
                                    prefWidth = 150.0
                                }
                            )
                        }
                    )
                }
            )
        }
    }

    private fun createEntregaSection(): VBox {
        val entregaSection = HBox().apply {
            alignment = Pos.CENTER
            spacing = 10.0
            padding = Insets(20.0, 0.0, 0.0, 0.0)
            children.addAll(
                Separator().apply { prefWidth = 300.0 },
                Label("Entrega").apply { styleClass.add("section-label") },
                Separator().apply { prefWidth = 300.0 }
            )
        }

        val checkBoxEntrega = CheckBox("Necessita entrega").apply {
            styleClass.add("check-box-entrega")
            padding = Insets(0.0, 0.0, 10.0, 0.0)
        }

        val entregaForm = VBox(10.0).apply {
            isVisible = false
            children.addAll(
                HBox(10.0).apply {
                    children.addAll(
                        VBox(10.0).apply {
                            HBox.setHgrow(this, Priority.ALWAYS)
                            children.addAll(
                                Label("Nome do Destinatário").apply { styleClass.add("field-label") },
                                TextField().apply {
                                    styleClass.add("text-field")
                                    maxWidth = Double.POSITIVE_INFINITY
                                    HBox.setHgrow(this, Priority.ALWAYS)
                                }
                            )
                        },
                        VBox(10.0).apply {
                            children.addAll(
                                Label("Telefone do Destinatário").apply { styleClass.add("field-label") },
                                TextField().apply {
                                    styleClass.add("text-field")
                                    prefWidth = 130.0
                                    controller.formatarTelefone(this)
                                }
                            )
                        }
                    )
                },
                HBox(10.0).apply {
                    children.addAll(
                        VBox(10.0).apply {
                            HBox.setHgrow(this, Priority.ALWAYS)
                            children.addAll(
                                Label("Endereço").apply { styleClass.add("field-label") },
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
                                Label("Referência").apply { styleClass.add("field-label") },
                                TextField().apply {
                                    styleClass.add("text-field")
                                    maxWidth = Double.POSITIVE_INFINITY
                                    HBox.setHgrow(this, Priority.ALWAYS)
                                }
                            )
                        }
                    )
                },
                HBox(10.0).apply {
                    children.addAll(
                        VBox(10.0).apply {
                            HBox.setHgrow(this, Priority.ALWAYS)
                            children.addAll(
                                Label("Cidade").apply { styleClass.add("field-label") },
                                TextField().apply {
                                    styleClass.add("text-field")
                                    maxWidth = Double.POSITIVE_INFINITY
                                    HBox.setHgrow(this, Priority.ALWAYS)
                                }
                            )
                        },
                        VBox(10.0).apply {
                            children.addAll(
                                Label("Estado").apply { styleClass.add("field-label") },
                                TextField().apply {
                                    styleClass.add("text-field")
                                    prefWidth = 60.0
                                    maxWidth = 60.0
                                }
                            )
                        },
                        VBox(10.0).apply {
                            children.addAll(
                                Label("CEP").apply { styleClass.add("field-label") },
                                TextField().apply {
                                    styleClass.add("text-field")
                                    prefWidth = 100.0
                                    maxWidth = 100.0
                                }
                            )
                        }
                    )
                },
                HBox(10.0).apply {
                    children.addAll(
                        VBox(10.0).apply {
                            children.addAll(
                                Label("Valor da Entrega").apply { styleClass.add("field-label") },
                                TextField().apply {
                                    styleClass.add("text-field")
                                    prefWidth = 100.0
                                    maxWidth = 100.0
                                    alignment = Pos.CENTER_RIGHT
                                }
                            )
                        },
                        VBox(10.0).apply {
                            children.addAll(
                                Label("Data da Entrega").apply { styleClass.add("field-label") },
                                DatePicker().apply {
                                    styleClass.add("date-picker")
                                    prefWidth = 150.0
                                    maxWidth = 150.0
                                }
                            )
                        },
                        VBox(10.0).apply {
                            children.addAll(
                                Label("Hora da Entrega").apply { styleClass.add("field-label") },
                                TextField().apply {
                                    styleClass.add("text-field")
                                    prefWidth = 100.0
                                    maxWidth = 100.0
                                    promptText = "HH:MM"
                                }
                            )
                        }
                    )
                }
            )
        }

        checkBoxEntrega.setOnAction {
            entregaForm.isVisible = checkBoxEntrega.isSelected
        }

        return VBox(10.0).apply {
            children.addAll(entregaSection, checkBoxEntrega, entregaForm)
        }
    }
}