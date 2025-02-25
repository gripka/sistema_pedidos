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
import javafx.application.Platform
import javafx.beans.property.DoubleProperty
import javafx.collections.ListChangeListener
import javafx.scene.Node
import javafx.util.converter.LocalDateStringConverter as JavaFxLocalDateStringConverter
import java.time.format.DateTimeFormatter
import java.util.Locale


class NovoPedidoView : BorderPane() {
    private val controller = NovoPedidoController()
    private lateinit var entregaForm: VBox
    private lateinit var totalLabelRef: Label
    private lateinit var valorEntregaField: TextField

    private val contentContainer = VBox().apply {
        spacing = 10.0
        padding = Insets(15.0, 20.0, 40.0, 30.0)
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
        controller.setupListeners()
    }

    private fun DoubleProperty.animate(endValue: Double, duration: Duration) {
        TranslateTransition(duration).apply {
            toX = endValue
            node = this@animate.bean as Node
            play()
        }
    }
    private lateinit var totalLabel: Label

    fun createBarraInferior(): HBox {
        totalLabel = Label("R$ 0,00").apply {
            styleClass.add("total-value-label")
        }

        controller.setTotalLabel(totalLabel)
        controller.addNovoProduto()
        controller.getProdutosContainer().children.addListener(
            ListChangeListener { _ -> controller.updateTotal(totalLabel) }
        )

        return HBox().apply {
            padding = Insets(20.0)
            spacing = 20.0
            maxHeight = 80.0
            minHeight = 80.0
            styleClass.add("barra-inferior")
            alignment = Pos.CENTER

            val leftRegion = Region().apply {
                HBox.setHgrow(this, Priority.ALWAYS)
                maxWidth = Double.POSITIVE_INFINITY
            }

            val centerBox = HBox().apply {
                alignment = Pos.CENTER
                children.add(
                    Button("Salvar Pedido").apply {
                        styleClass.add("salvar-pedido-button")
                        prefWidth = 200.0
                        prefHeight = 40.0
                    }
                )
            }

            val rightRegion = Region().apply {
                HBox.setHgrow(this, Priority.ALWAYS)
                maxWidth = Double.POSITIVE_INFINITY
            }

            val totalContainer = VBox().apply {
                styleClass.add("total-container")
                padding = Insets(10.0, 20.0, 10.0, 20.0)
                alignment = Pos.CENTER_LEFT
                minWidth = 300.0
                maxWidth = 300.0
                children.add(
                    HBox(5.0).apply {
                        alignment = Pos.CENTER_LEFT
                        children.addAll(
                            Label("Total do Pedido:").apply {
                                styleClass.add("total-label")
                            },
                            Region().apply {
                                HBox.setHgrow(this, Priority.ALWAYS)
                            },
                            totalLabel
                        )
                    }
                )
            }

            children.addAll(leftRegion, centerBox, rightRegion, totalContainer)
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
                            promptText = "(XX) XXXXX-XXXX"
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

        val observacaoBox = VBox(10.0).apply {
            children.addAll(
                Label("Observação do Pedido").apply {
                    styleClass.add("field-label")
                },
                TextField().apply {
                    styleClass.add("text-field")
                    maxWidth = Double.POSITIVE_INFINITY
                    promptText = "Digite uma observação para o pedido"
                }
            )
        }

        val adicionarProdutoButton = Button("Adicionar Produto").apply {
            styleClass.addAll("novo-pedido-button")
            prefWidth = 150.0
            prefHeight = 35.0
            setOnAction {
                controller.addNovoProduto()
            }
        }

        return VBox(10.0).apply {
            children.addAll(
                pedidosSection,
                produtosContainer,
                observacaoBox,
                adicionarProdutoButton
            )
        }
    }

    private fun createPagamentoSection(): VBox {
        val descontoToggleGroup = ToggleGroup()
        controller.setDescontoToggleGroup(descontoToggleGroup)

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

        val paymentToggleGroup = ToggleGroup()
        val formasPagamento = listOf("Dinheiro", "Cartão de Crédito", "Cartão de Débito", "PIX", "Voucher")

        val toggleButtons = HBox(10.0).apply {
            alignment = Pos.CENTER
            children.addAll(
                formasPagamento.map { forma ->
                    ToggleButton(forma).apply {
                        toggleGroup = paymentToggleGroup
                        styleClass.add("payment-toggle-button")
                        prefWidth = 130.0
                        prefHeight = 35.0
                        if (forma == "Dinheiro") isSelected = true
                    }
                }
            )
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
            styleClass.addAll("text-field")
            prefWidth = 100.0
            maxWidth = 100.0
            alignment = Pos.CENTER_RIGHT
            style = "-fx-padding: 8px 6px;"
            background = Background(BackgroundFill(javafx.scene.paint.Color.rgb(250, 251, 252), CornerRadii(3.0), Insets.EMPTY))
            border = Border(BorderStroke(
                javafx.scene.paint.Color.rgb(223, 225, 230),
                BorderStrokeStyle.SOLID,
                CornerRadii(3.0),
                BorderWidths(2.0)
            ))
        }

        val paymentFieldsBox = HBox(20.0).apply {
            alignment = Pos.CENTER_LEFT
            children.addAll(
                VBox(10.0).apply {
                    prefWidth = 450.0
                    children.addAll(
                        Label("Desconto").apply { styleClass.add("field-label") },
                        HBox(10.0).apply {
                            alignment = Pos.CENTER_LEFT
                            spacing = 20.0
                            children.addAll(
                                HBox(20.0).apply {
                                    prefHeight = 36.0
                                    alignment = Pos.CENTER_LEFT
                                    children.addAll(
                                        RadioButton("Valor (R$)").apply {
                                            toggleGroup = descontoToggleGroup
                                            isSelected = true
                                            styleClass.addAll("custom-radio")
                                            id = "valor"
                                            prefWidth = 120.0
                                        },
                                        RadioButton("Percentual (%)").apply {
                                            toggleGroup = descontoToggleGroup
                                            styleClass.addAll("custom-radio")
                                            id = "percentual"
                                            prefWidth = 140.0
                                        }
                                    )
                                },
                                TextField().apply {
                                    styleClass.add("text-field")
                                    prefWidth = 120.0
                                    maxWidth = 120.0
                                    alignment = Pos.CENTER_RIGHT
                                    promptText = "R$ 0,00"
                                    var currentTextListener = controller.formatarMoeda(this)
                                    controller.setDescontoField(this)

                                    descontoToggleGroup.selectedToggleProperty().addListener { _, _, newToggle ->
                                        val isValor = (newToggle as? RadioButton)?.id == "valor"
                                        textProperty().removeListener(currentTextListener)
                                        text = ""

                                        if (isValor) {
                                            promptText = "R$ 0,00"
                                            currentTextListener = controller.formatarMoeda(this)
                                        } else {
                                            promptText = "0,00"
                                            currentTextListener = controller.formatarPercentual(this)
                                        }
                                    }
                                }
                            )
                        }
                    )
                },
                VBox(10.0).apply {
                    prefWidth = 150.0
                    children.addAll(
                        Label("Troco Para").apply { styleClass.add("field-label") },
                        TextField().apply {
                            styleClass.add("text-field")
                            prefWidth = 100.0
                            maxWidth = 100.0
                            alignment = Pos.CENTER_RIGHT
                            promptText = "R$ 0,00"
                            controller.formatarMoeda(this)
                            controller.setTrocoParaField(this)
                        }
                    )
                },
                VBox(10.0).apply {
                    prefWidth = 150.0
                    children.addAll(
                        Label("Troco a Devolver").apply { styleClass.add("field-label") },
                        Label("R$ 0,00").apply {
                            styleClass.addAll("text-field")
                            prefWidth = 100.0
                            maxWidth = 100.0
                            alignment = Pos.CENTER_RIGHT
                            style = "-fx-padding: 8px 6px;"
                            background = Background(BackgroundFill(
                                javafx.scene.paint.Color.rgb(250, 251, 252),
                                CornerRadii(3.0),
                                Insets.EMPTY
                            ))
                            border = Border(BorderStroke(
                                javafx.scene.paint.Color.rgb(223, 225, 230),
                                BorderStrokeStyle.SOLID,
                                CornerRadii(3.0),
                                BorderWidths(2.0)
                            ))
                            controller.setTrocoCalculadoLabel(this)
                        }
                    )
                }
            )
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

        paymentToggleGroup.selectedToggleProperty().addListener { _, _, newToggle ->
            val selectedButton = newToggle as? ToggleButton
            trocoParaTextField.isDisable = selectedButton?.text != "Dinheiro"
            trocoCalculadoLabel.isDisable = selectedButton?.text != "Dinheiro"
        }

        return VBox(10.0).apply {
            children.addAll(
                pagamentoSection,
                VBox(10.0).apply {
                    children.addAll(
                        Label("").apply { styleClass.add("field-label") },
                        toggleButtons
                    )
                },
                paymentFieldsBox,
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

        val toggleSwitch = HBox(10.0).apply {
            alignment = Pos.CENTER

            val switchContainer = StackPane().apply {
                minWidth = 56.0
                minHeight = 24.0
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
                    translateX = -16.0
                }

                setOnMouseClicked { event ->
                    if (event.x >= 0 && event.x <= 56.0 && event.y >= 0 && event.y <= 24.0) {
                        checkbox.isSelected = !checkbox.isSelected
                        event.consume()
                    }
                }

                checkbox.selectedProperty().addListener { _, _, isSelected ->
                    circle.translateXProperty().animate(
                        if (isSelected) 14.0 else -16.0,
                        Duration.millis(200.0)
                    )

                    if (isSelected) {
                        slider.styleClass.add("selected")
                        entregaForm.isDisable = false
                        entregaForm.opacity = 1.0
                    } else {
                        slider.styleClass.remove("selected")
                        entregaForm.isDisable = true
                        entregaForm.opacity = 0.6
                        // Clear delivery value and update total when delivery is disabled
                        Platform.runLater {
                            controller.setValorEntregaField(valorEntregaField)
                            valorEntregaField.text = "R$ 0,00"
                            controller.updateTotal(totalLabel)
                        }
                    }
                }

                children.addAll(slider, circle, checkbox)
            }

            val label = Label("").apply {
                styleClass.add("field-label")
            }

            children.addAll(switchContainer, label)
        }


        entregaForm = VBox(10.0).apply {
            isDisable = true
            opacity = 0.9
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
                                    promptText = "(XX) XXXXX-XXXX"

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
                                Label("CEP").apply { styleClass.add("field-label") },
                                TextField().apply {
                                    styleClass.add("text-field")
                                    prefWidth = 100.0
                                    maxWidth = 100.0
                                    promptText = "XXXXX-XXX"
                                    controller.formatarCep(this)
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
                                }.also { field ->
                                    valorEntregaField = field
                                    controller.setValorEntregaField(field)
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
                                    isEditable = false
                                    converter = JavaFxLocalDateStringConverter(
                                        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                                        DateTimeFormatter.ofPattern("dd/MM/yyyy")
                                    )
                                    Locale.setDefault(Locale("pt", "BR"))
                                    value = java.time.LocalDate.now()
                                }
                            )
                        },
                        VBox(10.0).apply {
                            children.addAll(
                                Label("Hora da Entrega").apply { styleClass.add("field-label") },
                                HBox(5.0).apply {
                                    alignment = Pos.CENTER_LEFT
                                    children.addAll(
                                        ComboBox<String>().apply {
                                            styleClass.add("time-picker")
                                            prefWidth = 70.0
                                            maxWidth = 70.0
                                            isEditable = true
                                            val hours = (0..23).map { String.format("%02d", it) }
                                            items.addAll(hours)
                                            value = "08"

                                            editor.textProperty().addListener { _, _, newValue ->
                                                if (newValue.isEmpty()) return@addListener

                                                try {
                                                    val hour = newValue.toInt()
                                                    if (hour in 0..23) {
                                                        value = String.format("%02d", hour)
                                                    } else {
                                                        editor.text = value
                                                    }
                                                } catch (e: NumberFormatException) {
                                                    editor.text = value
                                                }
                                            }

                                            setOnKeyPressed { event ->
                                                if (event.code.isDigitKey) {
                                                    editor.positionCaret(editor.text.length)
                                                }
                                            }
                                        },
                                        Label(":").apply {
                                            styleClass.add("field-label")
                                            style = "-fx-padding: 5 0 0 0;"
                                        },
                                        ComboBox<String>().apply {
                                            styleClass.add("time-picker")
                                            prefWidth = 70.0
                                            maxWidth = 70.0
                                            isEditable = true
                                            val minutes = (0..59 step 15).map { String.format("%02d", it) }
                                            items.addAll(minutes)
                                            value = "00"

                                            editor.textProperty().addListener { _, _, newValue ->
                                                if (newValue.isEmpty()) return@addListener

                                                try {
                                                    val minute = newValue.toInt()
                                                    if (minute in 0..59) {
                                                        value = String.format("%02d", minute)
                                                    } else {
                                                        editor.text = value
                                                    }
                                                } catch (e: NumberFormatException) {
                                                    editor.text = value
                                                }
                                            }

                                            setOnKeyPressed { event ->
                                                if (event.code.isDigitKey) {
                                                    editor.positionCaret(editor.text.length)
                                                }
                                            }
                                        }
                                    )
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