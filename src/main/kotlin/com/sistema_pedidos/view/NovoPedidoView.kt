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
import javafx.stage.StageStyle
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

        controller.setContentContainer(contentContainer)
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

            val saveButton = Button("Salvar Pedido").apply {
                styleClass.add("salvar-pedido-button")
                prefWidth = 200.0
                prefHeight = 40.0
                setOnAction {
                    if (showConfirmationDialog()) {
                        val clienteInfo = getClienteInfo()
                        val pagamentoInfo = getPagamentoInfo()
                        val entregaInfo = getEntregaInfo()

                        if (controller.salvarPedido(clienteInfo, pagamentoInfo, entregaInfo)) {
                            Alert(Alert.AlertType.INFORMATION).apply {
                                title = "Sucesso"
                                headerText = null
                                contentText = "Pedido salvo com sucesso!"
                                graphic = null
                                initStyle(StageStyle.UNDECORATED)
                                dialogPane.apply {
                                    stylesheets.addAll(this@NovoPedidoView.stylesheets)
                                    styleClass.add("success")
                                }
                                setOnShown {
                                    val window = dialogPane.scene.window
                                    window.centerOnScreen()
                                }

                                showAndWait()
                                controller.resetForm()
                            }
                        } else {
                            Alert(Alert.AlertType.ERROR).apply {
                                title = "Erro"
                                headerText = null
                                contentText = "Erro ao salvar o pedido"
                                graphic = null
                                initStyle(StageStyle.UNDECORATED)
                                dialogPane.apply {
                                    stylesheets.addAll(this@NovoPedidoView.stylesheets)
                                    styleClass.add("error")
                                }
                                showAndWait()
                            }
                        }
                    }
                }
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

            children.addAll(leftRegion, saveButton, rightRegion, totalContainer)
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
                        if (forma == "Dinheiro") isSelected = true  // Default selection
                    }
                }
            )
        }

        paymentToggleGroup.selectedToggleProperty().addListener { _, _, newValue ->
            if (newValue == null) {
                paymentToggleGroup.toggles.first().isSelected = true
            }
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
                    isSelected = true  // Ensure Pendente is selected by default
                },
                ToggleButton("Pago").apply {
                    this.toggleGroup = statusToggleGroup
                    styleClass.add("payment-toggle-button")
                    prefWidth = 100.0
                    prefHeight = 35.0
                }
            )
        }

// Add listener to ensure a status is always selected
        statusToggleGroup.selectedToggleProperty().addListener { _, _, newValue ->
            if (newValue == null) {
                // If no toggle is selected, select the default (Pendente)
                statusToggleGroup.toggles.first().isSelected = true
            }
        }

        paymentToggleGroup.selectedToggleProperty().addListener { _, _, newToggle ->
            val selectedButton = newToggle as? ToggleButton
            trocoParaTextField.isDisable = selectedButton?.text != "Dinheiro"
            trocoCalculadoLabel.isDisable = selectedButton?.text != "Dinheiro"
        }

        val retiradaFields = HBox(10.0).apply {
            id = "retirada-fields"
            spacing = 20.0
            children.addAll(
                VBox(10.0).apply {
                    children.addAll(
                        Label("Data de Retirada").apply { styleClass.add("field-label") },
                        DatePicker().apply {
                            value = java.time.LocalDate.now()
                            styleClass.add("date-picker")
                            prefWidth = 150.0
                            maxWidth = 150.0
                            promptText = "dd/mm/aaaa"
                            converter = JavaFxLocalDateStringConverter(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                                DateTimeFormatter.ofPattern("dd/MM/yyyy")
                            )
                        }
                    )
                },
                VBox(10.0).apply {
                    children.addAll(
                        Label("Hora da Retirada").apply { styleClass.add("field-label") },
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
                                }
                            )
                        }
                    )
                }
            )
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
                },
                retiradaFields
            )
        }
    }

    private fun showConfirmationDialog(): Boolean {
        val content = VBox(10.0).apply {
            padding = Insets(20.0)
            prefWidth = 500.0
            style = """
            -fx-background-color: white;
            -fx-background-radius: 5;
        """
            children.addAll(
                createConfirmationSection("Cliente", getClienteInfo()),
                createConfirmationSection("Produtos", getProdutosInfo()),
                createConfirmationSection("Pagamento", getPagamentoInfo()),
                createConfirmationSection("Entrega", getEntregaInfo())
            )
        }

        val dialog = Dialog<ButtonType>().apply {
            initStyle(StageStyle.UNDECORATED)
            title = "Confirmação do Pedido"
            headerText = "Confirme os dados do pedido"
            dialogPane.content = ScrollPane(content).apply {
                isFitToWidth = true
                prefViewportHeight = 400.0
            }
            dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

            dialogPane.lookupButton(ButtonType.OK).style = "-fx-pref-width: 100px;"
            dialogPane.lookupButton(ButtonType.CANCEL).style = "-fx-pref-width: 100px;"

            (dialogPane.lookup(".button-bar") as ButtonBar).buttonOrder = "C:OK:0:1"

            dialogPane.stylesheets.addAll(this@NovoPedidoView.stylesheets)

            dialogPane.style = """
            -fx-border-color: #D3D3D3;
            -fx-border-width: 1px;
        """
        }

        return dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK
    }

    private fun createConfirmationSection(title: String, items: List<Pair<String, String>>): VBox {
        val filteredItems = if (title == "Pagamento" && getEntregaInfo().first().second == "Sim") {
            items.filter { !it.first.contains("Retirada") }
        } else {
            items
        }

        return VBox(5.0).apply {
            children.add(Label(title).apply {
                styleClass.add("section-label")
            })

            filteredItems.forEach { (label, value) ->
                children.add(
                    HBox(10.0).apply {
                        children.addAll(
                            Label("$label:").apply {
                                styleClass.add("field-label")
                                prefWidth = 150.0
                            },
                            Label(value).apply {
                                styleClass.add("field-label")
                            }
                        )
                    }
                )
            }

            children.add(Separator().apply {
                padding = Insets(10.0, 0.0, 10.0, 0.0)
            })
        }
    }

    private fun getClienteInfo(): List<Pair<String, String>> {
        val nomeField = findTextField("Nome")
        val sobrenomeField = findTextField("Sobrenome")
        val telefoneField = findTextField("Telefone")
        val observacaoField = findTextField("Observação")

        return listOf(
            "Nome" to "${nomeField?.text ?: ""} ${sobrenomeField?.text ?: ""}".trim(),
            "Telefone" to (telefoneField?.text ?: ""),
            "Observação" to (observacaoField?.text ?: "")
        )
    }

    private fun getProdutosInfo(): List<Pair<String, String>> {
        val produtos = mutableListOf<Pair<String, String>>()

        controller.getProdutosContainer().children.forEach { node ->
            val hBox = node as HBox
            val qtdField = ((hBox.children[1] as VBox).children[1] as HBox).children[1] as TextField
            val prodField = ((hBox.children[2] as VBox).children[1] as TextField)
            val valorField = ((hBox.children[3] as VBox).children[1] as TextField)
            val subtotalField = ((hBox.children[4] as VBox).children[1] as TextField)

            produtos.add("Produto ${produtos.size + 1}" to
                    "${qtdField.text}x ${prodField.text} (${valorField.text}) = ${subtotalField.text}")
        }

        return produtos
    }

    private fun getPagamentoInfo(): List<Pair<String, String>> {
        val formaPagamento = findSelectedToggleText("payment-toggle-button")
        val status = findSelectedToggleText("payment-toggle-button", 1)
        val descontoType = if ((controller.getDescontoToggleGroup().selectedToggle as? RadioButton)?.id == "valor") "Valor" else "Percentual"
        val desconto = findTextField("Desconto")?.text ?: "R$ 0,00"
        val dataRetirada = findDatePicker("Data de Retirada")?.value?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: ""
        val horaRetirada = findPickupTimeValue() ?: ""

        return listOf(
            "Forma de Pagamento" to (formaPagamento ?: ""),
            "Status" to (status ?: "Pendente"),
            "Desconto ($descontoType)" to desconto,
            "Data de Retirada" to dataRetirada,
            "Hora de Retirada" to horaRetirada,
            "Total do Pedido" to (totalLabel.text)
        )
    }

    private fun findPickupTimeValue(): String? {
        val timeContainer = contentContainer.lookupAll(".time-picker")
            .filterIsInstance<ComboBox<String>>()
            .filter { picker ->
                val parent = picker.parent?.parent as? VBox
                val label = parent?.children?.firstOrNull { it is Label } as? Label
                label?.text == "Hora da Retirada"
            }
            .toList()

        return if (timeContainer.size >= 2) {
            "${timeContainer[0].value}:${timeContainer[1].value}"
        } else null
    }

    private fun getEntregaInfo(): List<Pair<String, String>> {
        if (entregaForm.isDisable) return listOf("Entrega" to "Não")

        val nomeField = findTextField("Nome do Destinatário")
        val telefoneField = findTextField("Telefone do Destinatário")
        val enderecoField = findTextField("Endereço")
        val referenciaField = findTextField("Referência")
        val cidadeField = findTextField("Cidade")
        val bairroField = findTextField("Bairro")
        val cepField = findTextField("CEP")
        val valorField = valorEntregaField
        val dataField = findDatePicker("Data da Entrega")
        val horaField = findTimeValue()

        return listOf(
            "Entrega" to "Sim",
            "Nome" to (nomeField?.text ?: ""),
            "Telefone" to (telefoneField?.text ?: ""),
            "Endereço" to (enderecoField?.text ?: ""),
            "Referência" to (referenciaField?.text ?: ""),
            "Cidade" to (cidadeField?.text ?: ""),
            "Bairro" to (bairroField?.text ?: ""),
            "CEP" to (cepField?.text ?: ""),
            "Valor" to (valorField.text),
            "Data" to (dataField?.value?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: ""),
            "Hora" to (horaField ?: "")
        )
    }

    private fun findTextField(labelText: String): TextField? {
        return contentContainer.lookupAll(".text-field")
            .filterIsInstance<TextField>()
            .firstOrNull { field ->
                val parent = field.parent
                if (parent is VBox) {
                    val label = parent.children.firstOrNull { it is Label } as? Label
                    label?.text == labelText
                } else false
            }
    }

    private fun findSelectedToggleText(styleClass: String, index: Int = 0): String? {
        return contentContainer.lookupAll(".$styleClass")
            .filterIsInstance<ToggleButton>()
            .groupBy { it.toggleGroup }
            .values
            .elementAtOrNull(index)
            ?.firstOrNull { it.isSelected }
            ?.text
    }

    private fun findDatePicker(labelText: String): DatePicker? {
        return contentContainer.lookupAll(".date-picker")
            .filterIsInstance<DatePicker>()
            .firstOrNull { picker ->
                val parent = picker.parent
                if (parent is VBox) {
                    val label = parent.children.firstOrNull { it is Label } as? Label
                    label?.text == labelText
                } else false
            }
    }

    private fun findTimeValue(): String? {
        val timeContainer = contentContainer.lookupAll(".time-picker")
            .filterIsInstance<ComboBox<String>>()
            .filter { picker ->
                val parent = picker.parent?.parent as? VBox
                val label = parent?.children?.firstOrNull { it is Label } as? Label
                label?.text == "Hora da Entrega"
            }
            .toList()

        return if (timeContainer.size >= 2) {
            "${timeContainer[0].value}:${timeContainer[1].value}"
        } else null
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
                style = "-fx-cursor: hand;"

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

                children.setAll(slider, circle, checkbox)

                setOnMouseClicked { event ->
                    checkbox.isSelected = !checkbox.isSelected
                    event.consume()
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
                        // Disable pickup fields
                        contentContainer.lookupAll(".date-picker").forEach { node ->
                            val datePicker = node as? DatePicker
                            val parent = datePicker?.parent as? VBox
                            val label = parent?.children?.firstOrNull { it is Label } as? Label
                            if (label?.text == "Data de Retirada") {
                                datePicker?.isDisable = true
                            }
                        }
                        contentContainer.lookupAll(".time-picker").forEach { node ->
                            val timePicker = node as? ComboBox<*>
                            val parent = timePicker?.parent?.parent as? VBox
                            val label = parent?.children?.firstOrNull { it is Label } as? Label
                            if (label?.text == "Hora da Retirada") {
                                timePicker?.isDisable = true
                            }
                        }
                    } else {
                        slider.styleClass.remove("selected")
                        entregaForm.isDisable = true
                        entregaForm.opacity = 0.6
                        // Enable pickup fields
                        contentContainer.lookupAll(".date-picker").forEach { node ->
                            val datePicker = node as? DatePicker
                            val parent = datePicker?.parent as? VBox
                            val label = parent?.children?.firstOrNull { it is Label } as? Label
                            if (label?.text == "Data de Retirada") {
                                datePicker?.isDisable = false
                            }
                        }
                        contentContainer.lookupAll(".time-picker").forEach { node ->
                            val timePicker = node as? ComboBox<*>
                            val parent = timePicker?.parent?.parent as? VBox
                            val label = parent?.children?.firstOrNull { it is Label } as? Label
                            if (label?.text == "Hora da Retirada") {
                                timePicker?.isDisable = false
                            }
                        }
                        Platform.runLater {
                            controller.setValorEntregaField(valorEntregaField)
                            valorEntregaField.text = "R$ 0,00"
                            controller.updateTotal(totalLabel)
                        }
                    }
                }
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