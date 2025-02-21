package com.sistema_pedidos.view

import com.sistema_pedidos.controller.NovoPedidoController
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.shape.Circle
import javafx.scene.shape.Rectangle
import javafx.scene.layout.StackPane
import javafx.util.Duration
import javafx.animation.TranslateTransition
import javafx.beans.property.DoubleProperty
import javafx.scene.Node

class NovoPedidoView : BorderPane() {
    private val controller = NovoPedidoController()
    private lateinit var entregaForm: VBox

    // Container do conteúdo que pode rolar
    private val contentContainer = VBox().apply {
        spacing = 10.0
        padding = Insets(15.0, 20.0, 40.0, 30.0) // Added bottom padding
        children.addAll(
            createClientSection(),
            createPedidosSection(),
            createPagamentoSection(),
            createEntregaSection()
        )
    }

    init {
        stylesheets.add(javaClass.getResource("/novopedidoview.css").toExternalForm())

        val scrollPane = ScrollPane(contentContainer).apply {
            isFitToWidth = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
            styleClass.add("main-scroll-pane")
            VBox.setVgrow(this, Priority.ALWAYS)
            maxHeight = Double.MAX_VALUE
        }

        center = scrollPane
        bottom = createBarraInferior()
    }

    private fun DoubleProperty.animate(endValue: Double, duration: Duration) {
        TranslateTransition(duration).apply {
            toX = endValue
            node = this@animate.bean as Node
            play()
        }
    }

    private fun createBarraInferior(): HBox {
        return HBox().apply {
            padding = Insets(20.0)
            spacing = 20.0
            maxHeight = 80.0
            minHeight = 80.0
            styleClass.add("barra-inferior")

            val leftBox = HBox().apply {
                HBox.setHgrow(this, Priority.ALWAYS)
            }

            val centerBox = HBox().apply {
                minWidth = 200.0
                alignment = Pos.CENTER
                children.add(
                    Button("Salvar Pedido").apply {
                        styleClass.add("salvar-pedido-button")
                        prefWidth = 200.0
                        prefHeight = 40.0
                    }
                )
            }

            val rightBox = HBox().apply {
                HBox.setHgrow(this, Priority.ALWAYS)
                alignment = Pos.CENTER_RIGHT
                children.add(
                    VBox().apply {
                        styleClass.add("total-container")
                        padding = Insets(10.0, 20.0, 10.0, 20.0)
                        alignment = Pos.CENTER
                        children.add(
                            Label("Total do Pedido: R$ 0,00").apply {
                                styleClass.add("total-label")
                            }
                        )
                    }
                )
            }

            children.addAll(leftBox, centerBox, rightBox)
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

        val toggleGroup = ToggleGroup()
        val formasPagamento = listOf("Dinheiro", "Cartão de Crédito", "Cartão de Débito", "PIX", "Voucher")

        val toggleButtons = HBox(10.0).apply {
            children.addAll(
                formasPagamento.map { forma ->
                    ToggleButton(forma).apply {
                        this.toggleGroup = toggleGroup
                        styleClass.add("payment-toggle-button")
                        prefWidth = 130.0
                        prefHeight = 35.0
                        if (forma == "Dinheiro") isSelected = true
                    }
                }
            )
        }

        val descontoTextField = TextField().apply {
            styleClass.add("text-field")
            prefWidth = 100.0
            maxWidth = 100.0
            alignment = Pos.CENTER_RIGHT
            promptText = "0,00"
            controller.formatarMoeda(this)
        }

        val trocoParaTextField = TextField().apply {
            styleClass.add("text-field")
            prefWidth = 100.0
            maxWidth = 100.0
            alignment = Pos.CENTER_RIGHT
            promptText = "0,00"
            controller.formatarMoeda(this)
        }

        val trocoCalculadoLabel = Label("R$ 0,00").apply {
            styleClass.add("text-field")
            prefWidth = 100.0
            maxWidth = 100.0
            alignment = Pos.CENTER_RIGHT
            style = "-fx-padding: 5;"
        }

        val statusToggleGroup = ToggleGroup()
        val statusButtons = HBox(10.0).apply {
            children.addAll(
                ToggleButton("Pendente").apply {
                    this.toggleGroup = statusToggleGroup
                    styleClass.add("payment-toggle-button")
                    prefWidth = 100.0
                    prefHeight = 35.0
                    isSelected = true
                },
                ToggleButton("Pago").apply {
                    this.toggleGroup = statusToggleGroup
                    styleClass.add("payment-toggle-button")
                    prefWidth = 100.0
                    prefHeight = 35.0
                }
            )
        }

        toggleGroup.selectedToggleProperty().addListener { _, _, newToggle ->
            val selectedButton = newToggle as? ToggleButton
            trocoParaTextField.isDisable = selectedButton?.text != "Dinheiro"
            trocoCalculadoLabel.isDisable = selectedButton?.text != "Dinheiro"
        }

        return VBox(10.0).apply {
            children.addAll(
                pagamentoSection,
                VBox(10.0).apply {
                    children.addAll(
                        Label("Forma de Pagamento").apply { styleClass.add("field-label") },
                        toggleButtons
                    )
                },
                HBox(10.0).apply {
                    spacing = 10.0
                    children.addAll(
                        VBox(10.0).apply {
                            children.addAll(
                                Label("Desconto").apply { styleClass.add("field-label") },
                                descontoTextField
                            )
                        },
                        VBox(10.0).apply {
                            children.addAll(
                                Label("Troco Para").apply { styleClass.add("field-label") },
                                trocoParaTextField
                            )
                        },
                        VBox(10.0).apply {
                            children.addAll(
                                Label("Troco a Devolver").apply { styleClass.add("field-label") },
                                trocoCalculadoLabel
                            )
                        }
                    )
                },
                VBox(10.0).apply {
                    children.addAll(
                        Label("Status").apply { styleClass.add("field-label") },
                        statusButtons
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

        val toggleSwitch = HBox(10.0).apply { // Using HBox to contain switch and label
            alignment = Pos.CENTER

            val switchContainer = StackPane().apply {
                minWidth = 56.0 // Match slider width
                minHeight = 24.0 // Match slider height
                maxWidth = 56.0
                maxHeight = 24.0
                styleClass.add("switch")

                val checkbox = CheckBox().apply {
                    opacity = 0.0
                    isFocusTraversable = false
                }

                val slider = Rectangle(56.0, 24.0).apply {
                    styleClass.add("slider")
                    arcWidth = 24.0
                    arcHeight = 24.0
                }

                val circle = Circle(10.0).apply {
                    styleClass.add("slider-circle")
                    translateX = -14.0
                }

                setOnMouseClicked { event ->
                    // Only handle clicks within the slider bounds
                    if (event.x >= 0 && event.x <= 56.0 && event.y >= 0 && event.y <= 24.0) {
                        checkbox.isSelected = !checkbox.isSelected
                        event.consume()
                    }
                }

                checkbox.selectedProperty().addListener { _, _, isSelected ->
                    circle.translateXProperty().animate(
                        if (isSelected) 14.0 else -14.0,
                        Duration.millis(200.0)
                    )

                    if (isSelected) {
                        slider.styleClass.add("selected")
                    } else {
                        slider.styleClass.remove("selected")
                    }

                    entregaForm.isVisible = isSelected
                }

                children.addAll(slider, circle, checkbox)
            }

            val label = Label("").apply {
                styleClass.add("field-label")
            }

            children.addAll(switchContainer, label)
        }


        entregaForm = VBox(10.0).apply {
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
                                Label("Bairro").apply { styleClass.add("field-label") },
                                TextField().apply {
                                    styleClass.add("text-field")
                                    prefWidth = 350.0
                                    maxWidth = 350.0
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
                                    controller.formatarMoeda(this)
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

        return VBox(10.0).apply {
            children.addAll(entregaSection, toggleSwitch, entregaForm)
        }
    }
}