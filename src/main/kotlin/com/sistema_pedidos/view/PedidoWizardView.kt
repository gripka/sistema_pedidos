package com.sistema_pedidos.view

import javafx.animation.FadeTransition
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.shape.Circle
import javafx.util.Duration
import javafx.scene.Node
import javafx.scene.layout.Priority
import javafx.scene.control.*
import javafx.scene.layout.*
import com.sistema_pedidos.controller.PedidoWizardController
import javafx.scene.image.ImageView
import javafx.scene.image.Image
import javafx.scene.shape.Rectangle
import javafx.scene.paint.Color
import java.awt.event.KeyEvent
import java.time.LocalDate

class PedidoWizardView : BorderPane() {

    private val produtosContainer = VBox().apply {
        spacing = 15.0
    }

    // Main tab pane to hold multiple order tabs
    private val tabPane = TabPane()

    // Counter for naming tabs
    private var tabCounter = 1

    // Reference to the "new tab" button tab
    private lateinit var newTabButton: Tab

    init {
        stylesheets.add(javaClass.getResource("/pedidowizardview.css").toExternalForm())

        // Setup tab pane
        tabPane.tabClosingPolicy = TabPane.TabClosingPolicy.ALL_TABS
        tabPane.selectionModel.selectedItemProperty().addListener { _, _, newTab ->
            if (newTab == newTabButton) {
                // If "+" tab is selected, create a new order tab and select it
                val index = tabPane.tabs.size - 1
                tabPane.selectionModel.select(index - 1)
                addOrderTab()
            }
        }

        // Create the "+" tab for adding new orders
        newTabButton = Tab("+")
        newTabButton.isClosable = false

        // Create initial tab and add the "+" tab
        tabPane.tabs.add(createOrderTab("Pedido ${tabCounter++}"))
        tabPane.tabs.add(newTabButton)

        // Add tab pane to the root layout
        center = tabPane
    }

    /**
     * Adds a new order tab and selects it
     */
    private fun addOrderTab() {
        val newTab = createOrderTab("Pedido ${tabCounter++}")
        tabPane.tabs.add(tabPane.tabs.size - 1, newTab)
        tabPane.selectionModel.select(newTab)
    }

    /**
     * Creates a new order tab with a complete wizard flow
     */
    private fun createOrderTab(title: String): Tab {
        val orderView = OrderTabContent()
        val tab = Tab(title, orderView)
        return tab
    }

    private inner class OrderTabContent : BorderPane() {
        // Create dedicated controller for this tab
        private val controller = PedidoWizardController()
        private lateinit var entregaClienteRadio: RadioButton
        private val stepIndicators = ArrayList<StackPane>()
        private val stepLabels = listOf("Cliente", "Produtos", "Pagamento", "Entrega", "Confirmação")

        // Content containers for each step
        private val stepContainers = ArrayList<Pane>()
        private var currentStep = 0

        // Navigation buttons
        private lateinit var prevButton: Button
        private lateinit var nextButton: Button
        private lateinit var finishButton: Button

        init {
            styleClass.add("order-tab-content")

            // Create step containers
            createStepContainers()

            // Create step indicators
            top = createStepIndicatorBar()

            // Create navigation bar
            bottom = createNavigationBar()

            // Start with first step
            showStep(0)

            // Initialize delivery step indicator as disabled since it starts unchecked
            updateStepIndicators(false)
        }

        // Add this method to the OrderTabContent class
        private fun updateStepIndicators(isDeliveryEnabled: Boolean) {
            // Update the delivery step indicator (index 3)
            if (!isDeliveryEnabled) {
                // If delivery is disabled, always show it as skipped
                stepIndicators[3].styleClass.removeAll("current-step", "past-step", "future-step")
                stepIndicators[3].styleClass.add("skipped-step")
            } else {
                // If delivery is enabled, show according to current step
                stepIndicators[3].styleClass.removeAll("skipped-step")
                when {
                    currentStep < 3 -> stepIndicators[3].styleClass.add("future-step")
                    currentStep == 3 -> stepIndicators[3].styleClass.add("current-step")
                    else -> stepIndicators[3].styleClass.add("past-step")
                }
            }
        }

        private fun createStepContainers() {
            // Step 1: Cliente
            stepContainers.add(createClientStep())

            // Step 2: Produtos
            stepContainers.add(createProductsStep())

            // Step 3: Pagamento
            stepContainers.add(createPaymentStep())

            // Step 4: Entrega
            stepContainers.add(createDeliveryStep())

            // Step 5: Confirmação
            stepContainers.add(createConfirmationStep())
        }

        private fun createStepIndicatorBar(): HBox {
            val indicatorBar = HBox(20.0).apply {
                alignment = Pos.CENTER
                padding = Insets(20.0)
                styleClass.add("step-indicator-bar")
            }

            for (i in stepLabels.indices) {
                val indicator = createStepIndicator(i)
                stepIndicators.add(indicator)

                // Add indicator to bar
                indicatorBar.children.add(VBox(5.0).apply {
                    alignment = Pos.CENTER
                    children.addAll(
                        indicator,
                        Label(stepLabels[i]).apply {
                            styleClass.add("step-label")
                        }
                    )
                })

                // Add separator line between indicators except for the last one
                if (i < stepLabels.size - 1) {
                    indicatorBar.children.add(
                        Separator().apply {
                            styleClass.add("step-separator")
                            prefWidth = 50.0
                        }
                    )
                }
            }

            return indicatorBar
        }

        private fun createStepIndicator(step: Int): StackPane {
            val circle = Circle(20.0).apply {
                styleClass.add("step-circle")
            }

            val label = Label("${step + 1}").apply {
                styleClass.add("step-number")
            }

            return StackPane(circle, label).apply {
                styleClass.add("step-indicator")
                if (step == 0) {
                    styleClass.add("current-step")
                } else {
                    styleClass.add("future-step")
                }
            }
        }

        private fun createNavigationBar(): HBox {
            prevButton = Button("Voltar").apply {
                styleClass.add("button_estilo2")
                prefWidth = 150.0
                prefHeight = 40.0
                isDisable = true
                setOnAction {
                    // When going back, check if coming from confirmation and delivery is disabled
                    if (currentStep == 4 && ::entregaClienteRadio.isInitialized && !entregaClienteRadio.isSelected) {
                        showStep(2)  // Go back to payment step
                    } else if (currentStep > 0) {
                        showStep(currentStep - 1)
                    }
                }
            }

            // Add total value display in the center
            val totalContainer = HBox().apply {
                styleClass.add("total-nav-display")
                alignment = Pos.CENTER
                children.addAll(
                    Label("Total:").apply {
                        styleClass.add("total-nav-label")
                        style = "-fx-font-weight: bold; -fx-font-size: 16px;"
                    },
                    Label("R$ 0,00").apply {
                        styleClass.add("total-nav-value")
                        style = "-fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 0 0 0 10px;"
                        // Set this label to be updated by the controller
                        controller.setTotalLabel(this)
                    }
                )
            }

            nextButton = Button("Avançar").apply {
                styleClass.add("button_estilo")
                styleClass.add("primary-button")
                prefWidth = 150.0
                prefHeight = 40.0
                setOnAction {
                    if (currentStep < stepContainers.size - 1) {
                        // If on payment step and delivery is not selected, skip to confirmation
                        if (currentStep == 2 && ::entregaClienteRadio.isInitialized && !entregaClienteRadio.isSelected) {
                            showStep(4)  // Skip to confirmation step
                        } else {
                            showStep(currentStep + 1)
                        }
                    }
                }
            }

            finishButton = Button("Finalizar Pedido").apply {
                styleClass.add("nav-button")
                styleClass.add("success-button")
                prefWidth = 200.0
                prefHeight = 40.0
                isVisible = false
                setOnAction {
                    // This would contain the logic to save the order
                }
            }

            return HBox(20.0).apply {
                padding = Insets(20.0)
                alignment = Pos.CENTER
                styleClass.add("navigation-bar")

                children.addAll(
                    prevButton,
                    Region().apply { HBox.setHgrow(this, Priority.ALWAYS) },
                    totalContainer,
                    Region().apply { HBox.setHgrow(this, Priority.ALWAYS) },
                    nextButton,
                    finishButton
                )
            }
        }

        private fun showStep(step: Int) {
            // Update step indicators
            var targetStep = step

            // Skip delivery step if not delivering to customer's address
            if (step == 3 && ::entregaClienteRadio.isInitialized && !entregaClienteRadio.isSelected) {
                targetStep = 4  // Skip to confirmation step
            }
            for (i in stepIndicators.indices) {
                stepIndicators[i].styleClass.removeAll("current-step", "past-step", "future-step")
                when {
                    i < targetStep -> stepIndicators[i].styleClass.add("past-step")
                    i == targetStep -> stepIndicators[i].styleClass.add("current-step")
                    else -> stepIndicators[i].styleClass.add("future-step")
                }
            }

            // Show appropriate container with fade effect
            val container = stepContainers[targetStep]
            val fadeTransition = FadeTransition(Duration.millis(300.0), container)
            fadeTransition.fromValue = 0.0
            fadeTransition.toValue = 1.0

            // Set the new content
            center = container
            fadeTransition.play()

            // Update navigation buttons
            prevButton.isDisable = targetStep == 0
            nextButton.isVisible = targetStep < stepContainers.size - 1
            finishButton.isVisible = targetStep == stepContainers.size - 1

            currentStep = targetStep
        }

        // The existing step creation methods remain the same but are moved to the OrderTabContent class
        // For brevity, I'm not duplicating all of them here, but they would be copied as-is

        // Step 1: Cliente
        private fun createClientStep(): Pane {
            // Create a container that will hold the ScrollPane
            val mainContainer = BorderPane()

            val contentContainer = VBox(15.0).apply {
                styleClass.add("step-container")
                padding = Insets(20.0)
            }

            val scrollPane = ScrollPane(contentContainer).apply {
                isFitToWidth = true
                styleClass.add("content-scroll-pane")
            }

            // Section header
            val sectionHeader = Label("Informações do Cliente").apply {
                styleClass.add("section-header")
            }

            // Customer type selection
            val customerTypeToggle = ToggleGroup()

            val customerTypeBox = HBox(20.0).apply {
                alignment = Pos.CENTER_LEFT
                children.addAll(
                    RadioButton("Pessoa Física").apply {
                        toggleGroup = customerTypeToggle
                        isSelected = true
                        userData = "PESSOA_FISICA"
                        styleClass.add("custom-radio")
                    },
                    RadioButton("Pessoa Jurídica").apply {
                        toggleGroup = customerTypeToggle
                        userData = "PESSOA_JURIDICA"
                        styleClass.add("custom-radio")
                    }
                )
            }

            // Container for pessoa física fields
            val pessoaFisicaFields = VBox(10.0).apply {
                isVisible = true
                isManaged = true  // Add this line
                children.addAll(
                    HBox(10.0).apply {
                        children.addAll(
                            VBox(5.0).apply {
                                HBox.setHgrow(this, Priority.ALWAYS)
                                children.addAll(
                                    Label("Nome").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        id = "nomeField"
                                        styleClass.add("text-field")
                                        maxWidth = Double.POSITIVE_INFINITY
                                        HBox.setHgrow(this, Priority.ALWAYS)
                                    }
                                )
                            },
                            VBox(5.0).apply {
                                HBox.setHgrow(this, Priority.ALWAYS)
                                children.addAll(
                                    Label("Sobrenome").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        id = "sobrenomeField"
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
                            VBox(5.0).apply {
                                children.addAll(
                                    Label("CPF").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        id = "cpfField"
                                        styleClass.add("text-field")
                                        prefWidth = 150.0
                                        promptText = "000.000.000-00"

                                        // CPF formatter implementation
                                        textProperty().addListener { _, oldValue, newValue ->
                                            if (newValue == null) {
                                                text = oldValue
                                                return@addListener
                                            }

                                            // Remove non-digits
                                            var value = newValue.replace(Regex("[^0-9]"), "")

                                            // Limit to 11 digits (CPF length)
                                            if (value.length > 11) {
                                                value = value.substring(0, 11)
                                            }

                                            // Format with dots and dash
                                            if (value.length > 9) {
                                                value = "${value.substring(0, 3)}.${value.substring(3, 6)}.${value.substring(6, 9)}-${value.substring(9)}"
                                            } else if (value.length > 6) {
                                                value = "${value.substring(0, 3)}.${value.substring(3, 6)}.${value.substring(6)}"
                                            } else if (value.length > 3) {
                                                value = "${value.substring(0, 3)}.${value.substring(3)}"
                                            }

                                            if (value != newValue) {
                                                text = value
                                                positionCaret(text.length)
                                            }
                                        }
                                    }
                                )
                            }
                        )
                    }
                )
            }

            // Container for pessoa jurídica fields
            val pessoaJuridicaFields = VBox(10.0).apply {
                isVisible = false
                isManaged = false  // Add this line
                children.addAll(
                    HBox(10.0).apply {
                        children.addAll(
                            VBox(5.0).apply {
                                HBox.setHgrow(this, Priority.ALWAYS)
                                children.addAll(
                                    Label("Razão Social").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        id = "razaoSocialField"
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
                            VBox(5.0).apply {
                                HBox.setHgrow(this, Priority.ALWAYS)
                                children.addAll(
                                    Label("Nome Fantasia").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        id = "nomeFantasiaField"
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
                            VBox(5.0).apply {
                                children.addAll(
                                    Label("CNPJ").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        id = "cnpjField"
                                        styleClass.add("text-field")
                                        prefWidth = 180.0
                                        promptText = "00.000.000/0000-00"

                                        // CNPJ formatter implementation
                                        textProperty().addListener { _, oldValue, newValue ->
                                            if (newValue != null && newValue != oldValue) {
                                                val digits = newValue.replace(Regex("[^0-9]"), "")
                                                if (digits.length > 14) {
                                                    // Don't allow more than 14 digits
                                                    text = oldValue
                                                    return@addListener
                                                }

                                                val formatted = when {
                                                    digits.length <= 2 -> digits
                                                    digits.length <= 5 -> "${digits.substring(0, 2)}.${digits.substring(2)}"
                                                    digits.length <= 8 -> "${digits.substring(0, 2)}.${digits.substring(2, 5)}.${digits.substring(5)}"
                                                    digits.length <= 12 -> "${digits.substring(0, 2)}.${digits.substring(2, 5)}.${digits.substring(5, 8)}/${digits.substring(8)}"
                                                    else -> "${digits.substring(0, 2)}.${digits.substring(2, 5)}.${digits.substring(5, 8)}/${digits.substring(8, 12)}-${digits.substring(12)}"
                                                }

                                                if (formatted != newValue) {
                                                    val caretPosition = caretPosition
                                                    text = formatted

                                                    // Calculate the new position based on added formatting characters
                                                    val newPosition = caretPosition + (formatted.length - newValue.length)
                                                    if (newPosition >= 0 && newPosition <= formatted.length) {
                                                        positionCaret(newPosition)
                                                    }
                                                }
                                            }
                                        }

                                        addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED) { event ->
                                            if (event.character == " " || text.replace(Regex("[^0-9]"), "").length >= 14) {
                                                event.consume()
                                            }
                                        }
                                    }
                                )
                            },
                            VBox(5.0).apply {
                                children.addAll(
                                    Label("Inscrição Estadual").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        id = "ieField"
                                        styleClass.add("text-field")
                                        prefWidth = 150.0
                                        controller.formatarInscricaoEstadual(this)
                                    }
                                )
                            }
                        )
                    }
                )
            }

            // Common fields for both customer types
            val commonFields = VBox(10.0).apply {
                children.addAll(
                    HBox(10.0).apply {
                        children.addAll(
                            VBox(5.0).apply {
                                children.addAll(
                                    Label("Telefone").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        id = "telefoneField"
                                        styleClass.add("text-field")
                                        prefWidth = 150.0
                                        promptText = "(00) 00000-0000"

                                        // Add phone formatter
                                        textProperty().addListener { _, oldValue, newValue ->
                                            if (newValue == null) {
                                                text = oldValue
                                                return@addListener
                                            }

                                            // Allow only numbers
                                            val digits = newValue.replace(Regex("[^0-9]"), "")

                                            // Format based on the length
                                            val formatted = when {
                                                digits.length <= 2 -> {
                                                    if (digits.isNotEmpty()) "($digits" else ""
                                                }
                                                digits.length <= 7 -> {
                                                    "(${digits.substring(0, 2)}) ${digits.substring(2)}"
                                                }
                                                else -> {
                                                    val len = digits.length.coerceAtMost(11)
                                                    "(${digits.substring(0, 2)}) ${digits.substring(2, 7)}-${digits.substring(7, len)}"
                                                }
                                            }

                                            if (formatted != newValue) {
                                                text = formatted
                                                positionCaret(text.length)
                                            }
                                        }
                                    }
                                )
                            },
                            VBox(5.0).apply {
                                HBox.setHgrow(this, Priority.ALWAYS)
                                children.addAll(
                                    Label("Email").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        id = "emailField"
                                        styleClass.add("text-field")
                                        maxWidth = Double.POSITIVE_INFINITY
                                        HBox.setHgrow(this, Priority.ALWAYS)
                                    }
                                )
                            }
                        )
                    },
                    VBox(5.0).apply {
                        children.addAll(
                            Label("Observação").apply { styleClass.add("field-label") },
                            TextField().apply {
                                id = "observacaoField"
                                styleClass.add("text-field")
                                maxWidth = Double.POSITIVE_INFINITY
                            }
                        )
                    }
                )
            }

            // Address section
            val addressSection = VBox(15.0).apply {
                children.add(Label("Endereço").apply {
                    styleClass.add("section-header")
                    style = "-fx-font-size: 16px;"
                })

                children.add(HBox(10.0).apply {
                    children.addAll(
                        VBox(5.0).apply {
                            children.addAll(
                                Label("CEP").apply { styleClass.add("field-label") },
                                TextField().apply {
                                    id = "cepField"
                                    styleClass.add("text-field")
                                    prefWidth = 120.0
                                    promptText = "00000-000"
                                    controller.formatarCep(this)
                                }
                            )
                        },
                        VBox(5.0).apply {
                            HBox.setHgrow(this, Priority.ALWAYS)
                            children.addAll(
                                Label("Endereço").apply { styleClass.add("field-label") },
                                TextField().apply {
                                    id = "logradouroField"
                                    styleClass.add("text-field")
                                    maxWidth = Double.POSITIVE_INFINITY
                                    HBox.setHgrow(this, Priority.ALWAYS)
                                }
                            )
                        }
                    )
                })

                children.add(HBox(10.0).apply {
                    children.addAll(
                        VBox(5.0).apply {
                            children.addAll(
                                Label("Número").apply { styleClass.add("field-label") },
                                TextField().apply {
                                    id = "numeroField"
                                    styleClass.add("text-field")
                                    prefWidth = 80.0
                                }
                            )
                        },
                        VBox(5.0).apply {
                            HBox.setHgrow(this, Priority.ALWAYS)
                            children.addAll(
                                Label("Complemento").apply { styleClass.add("field-label") },
                                TextField().apply {
                                    id = "complementoField"
                                    styleClass.add("text-field")
                                    maxWidth = Double.POSITIVE_INFINITY
                                    HBox.setHgrow(this, Priority.ALWAYS)
                                }
                            )
                        }
                    )
                })

                children.add(HBox(10.0).apply {
                    children.addAll(
                        VBox(5.0).apply {
                            children.addAll(
                                Label("Bairro").apply { styleClass.add("field-label") },
                                TextField().apply {
                                    id = "bairroField"
                                    styleClass.add("text-field")
                                    prefWidth = 200.0
                                }
                            )
                        },
                        VBox(5.0).apply {
                            HBox.setHgrow(this, Priority.ALWAYS)
                            children.addAll(
                                Label("Cidade").apply { styleClass.add("field-label") },
                                TextField().apply {
                                    id = "cidadeField"
                                    styleClass.add("text-field")
                                    maxWidth = Double.POSITIVE_INFINITY
                                    HBox.setHgrow(this, Priority.ALWAYS)
                                }
                            )
                        },
                        VBox(5.0).apply {
                            children.addAll(
                                Label("Estado").apply { styleClass.add("field-label") },
                                ComboBox<String>().apply {
                                    id = "estadoField"
                                    styleClass.addAll("combo-box")
                                    prefWidth = 80.0
                                    maxWidth = 80.0
                                    isEditable = false
                                    items.addAll(
                                        "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
                                        "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI",
                                        "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
                                    )
                                    value = "PR"
                                }
                            )
                        }
                    )
                })
            }

            // Toggle between pessoa física and pessoa jurídica
// Toggle between pessoa física and pessoa jurídica
            customerTypeToggle.selectedToggleProperty().addListener { _, _, newValue ->
                val isPessoaFisica = (newValue as RadioButton).userData == "PESSOA_FISICA"
                pessoaFisicaFields.isVisible = isPessoaFisica
                pessoaFisicaFields.isManaged = isPessoaFisica
                pessoaJuridicaFields.isVisible = !isPessoaFisica
                pessoaJuridicaFields.isManaged = !isPessoaFisica
            }

            // Add all components to container
// Add all components to container
            contentContainer.children.addAll(
                sectionHeader,
                customerTypeBox,
                pessoaFisicaFields,
                pessoaJuridicaFields,
                Separator().apply {
                    padding = Insets(5.0, 0.0, 5.0, 0.0)
                },
                commonFields,
                Separator().apply {
                    padding = Insets(5.0, 0.0, 5.0, 0.0)
                },
                addressSection
            )

            // Set the ScrollPane to the BorderPane's center
            mainContainer.center = scrollPane

            // Return the BorderPane which is a Pane
            return mainContainer
        }

        // Step 2: Produtos
        private fun createProductsStep(): Pane {
            val section = createSectionHeader("Produtos")


            // Get the productos container from the controller
            val produtosContainer = controller.getProdutosContainer().apply {
                styleClass.add("produtos-container")
                padding = Insets(0.0, 0.0, 10.0, 0.0)
            }

            // Add initial product
            controller.addNovoProduto()

            val addButton = Button().apply {
                styleClass.add("adicionar-produto-button")
                text = "Adicionar Produto"
                prefWidth = 180.0
                prefHeight = 40.0
                //graphic = ImageView(Image(javaClass.getResourceAsStream("/icons/mais.png"))).apply {
                //    fitHeight = 20.0
                //    fitWidth = 20.0
                //}
                setOnAction {
                    controller.addNovoProduto()
                }
            }

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

            val buttonBar = HBox(10.0).apply {
                alignment = Pos.CENTER_LEFT
                children.add(addButton)
            }

            return createContentPane(section, VBox(20.0).apply {
                padding = Insets(20.0)
                spacing = 20.0
                children.addAll(
                    produtosContainer,
                    buttonBar,
                    observacaoBox,
                )
            })
        }

        // Step 3: Pagamento
        private fun createPaymentStep(): Pane {
            val section = createSectionHeader("Forma de Pagamento")

            // Main container using GridPane for better space utilization
            val mainGrid = GridPane().apply {
                hgap = 20.0
                vgap = 15.0
                padding = Insets(20.0)
                columnConstraints.addAll(
                    ColumnConstraints().apply { percentWidth = 50.0 },
                    ColumnConstraints().apply { percentWidth = 50.0 }
                )
            }

            // Payment method selection - taking full width in first row
            val paymentToggleGroup = ToggleGroup().apply {
                selectedToggleProperty().addListener { _, oldToggle, newToggle ->
                    if (newToggle == null && oldToggle != null) {
                        // If all toggles were deselected, reselect the previously selected toggle
                        selectToggle(oldToggle)
                    }
                }
            }

            // Create horizontal layout for payment and delivery options
            val topRow = HBox(20.0).apply {
                alignment = Pos.CENTER_LEFT
            }

            val paymentBox = VBox(10.0).apply {
                style = "-fx-padding: 15; -fx-background-color: #f8f9fa; -fx-background-radius: 5; -fx-border-color: #e9ecef; -fx-border-radius: 5;"
                HBox.setHgrow(this, Priority.ALWAYS)
                children.addAll(
                    Label("Forma de Pagamento").apply {
                        styleClass.add("field-label")
                        style = "-fx-font-size: 15px; -fx-font-weight: bold;"
                    },
                    FlowPane().apply {
                        hgap = 10.0
                        vgap = 10.0
                        alignment = Pos.CENTER_LEFT
                        prefWrapLength = 500.0 // Adjust based on testing
                        padding = Insets(10.0, 0.0, 0.0, 0.0)
                        children.addAll(
                            createPaymentToggleButton("Dinheiro", true, paymentToggleGroup, "icons/moneyp.png"),
                            createPaymentToggleButton("Cartão de Crédito", false, paymentToggleGroup, "icons/credit-cardp.png"),
                            createPaymentToggleButton("Cartão de Débito", false, paymentToggleGroup, "icons/debit-cardp.png"),
                            createPaymentToggleButton("PIX", false, paymentToggleGroup, "icons/pixp.png"),
                            createPaymentToggleButton("Voucher", false, paymentToggleGroup, "icons/voucherp.png")
                        )
                    }
                )
            }

            // Create delivery option box
            val entregaBox = VBox(10.0).apply {
                style = "-fx-padding: 15; -fx-background-color: #f8f9fa; -fx-background-radius: 5; -fx-border-color: #e9ecef; -fx-border-radius: 5;"
                minWidth = 300.0
                children.addAll(
                    Label("Opção de Entrega").apply {
                        styleClass.add("field-label")
                        style = "-fx-font-size: 15px; -fx-font-weight: bold;"
                    },
                    // Store reference to this radio button
                    RadioButton("Entregar no endereço do cliente").apply {
                        entregaClienteRadio = this  // Save reference
                        styleClass.add("custom-radio")
                        padding = Insets(10.0, 0.0, 0.0, 0.0)

                        // Create delivery details section
                        val entregaDetails = VBox(10.0).apply {
                            isVisible = false
                            isManaged = false
                            padding = Insets(10.0, 0.0, 0.0, 0.0)

                            children.addAll(
                                TextField().apply {
                                    styleClass.add("text-field")
                                    promptText = "Endereço de entrega"
                                },
                                HBox(10.0).apply {
                                    children.addAll(
                                        TextField().apply {
                                            styleClass.add("text-field")
                                            HBox.setHgrow(this, Priority.ALWAYS)
                                            promptText = "Taxa de entrega (R$)"
                                            prefWidth = 150.0
                                            alignment = Pos.CENTER_RIGHT
                                            controller.formatarMoeda(this)
                                        },
                                        Button("Calcular").apply {
                                            styleClass.add("button_estilo2")
                                            prefHeight = 36.0
                                        }
                                    )
                                }
                            )
                        }

                        // Add the details to the parent VBox
                        (parent as? VBox)?.children?.add(entregaDetails)

                        // Toggle visibility of delivery details when checkbox is clicked
                        selectedProperty().addListener { _, _, isSelected ->
                            entregaDetails.isVisible = isSelected
                            entregaDetails.isManaged = isSelected
                            updateStepIndicators(isSelected)
                        }
                    }
                )
            }

            topRow.children.addAll(paymentBox, entregaBox)
            mainGrid.add(topRow, 0, 0, 2, 1) // Span 2 columns

            // Column 1: Discount and Change
            val leftColumn = VBox(15.0)

            // Discount section
            val descontoToggleGroup = ToggleGroup()
            controller.setDescontoToggleGroup(descontoToggleGroup)

            val valorRadioButton = RadioButton("Valor (R$)").apply {
                toggleGroup = descontoToggleGroup
                isSelected = true
                styleClass.add("custom-radio")
                id = "valor"
            }

            val percentualRadioButton = RadioButton("Percentual (%)").apply {
                toggleGroup = descontoToggleGroup
                styleClass.add("custom-radio")
                id = "percentual"
            }

            val descontoField = TextField().apply {
                styleClass.add("text-field")
                prefWidth = 150.0
                alignment = Pos.CENTER_RIGHT
                promptText = "R$ 0,00"
                controller.setDescontoField(this)
                var currentTextListener = controller.formatarMoeda(this)

                // Add a listener to update the total when text changes
                textProperty().addListener { _, _, _ ->
                    // Calculate new total with discount
                    controller.aplicarDesconto()
                }

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

                    // Trigger discount recalculation when toggle changes
                    controller.aplicarDesconto()
                }
            }

            val discountBox = VBox(10.0).apply {
                style = "-fx-padding: 15; -fx-background-color: #f8f9fa; -fx-background-radius: 5; -fx-border-color: #e9ecef; -fx-border-radius: 5;"
                children.addAll(
                    Label("Desconto").apply {
                        styleClass.add("field-label")
                        style = "-fx-font-size: 15px; -fx-font-weight: bold;"
                    },
                    GridPane().apply {
                        hgap = 15.0
                        vgap = 10.0
                        padding = Insets(10.0, 0.0, 0.0, 0.0)
                        add(Label("Tipo de Desconto").apply { styleClass.add("field-label") }, 0, 0)
                        add(HBox(15.0).apply {
                            alignment = Pos.CENTER_LEFT
                            children.addAll(valorRadioButton, percentualRadioButton)
                        }, 0, 1)
                        add(Label("Valor do Desconto").apply { styleClass.add("field-label") }, 1, 0)
                        add(descontoField, 1, 1)
                    }
                )
            }
            leftColumn.children.add(discountBox)

            // Troco section (only visible for cash payment)
            val trocoParaField = TextField().apply {
                styleClass.add("text-field")
                prefWidth = 150.0
                alignment = Pos.CENTER_RIGHT
                promptText = "R$ 0,00"
                controller.formatarMoeda(this)
                Tooltip.install(this, Tooltip("Digite o valor recebido para calcular o troco"))
            }
            controller.setTrocoParaField(trocoParaField)

            val trocoCalculadoLabel = Label("R$ 0,00").apply {
                styleClass.add("troco-label")
                prefWidth = 150.0
                alignment = Pos.CENTER_RIGHT
                style = "-fx-background-color: white; -fx-border-color: #dfe4ea;"
            }
            controller.setTrocoCalculadoLabel(trocoCalculadoLabel)

            paymentToggleGroup.selectedToggleProperty().addListener { _, _, newToggle ->
                val selectedButton = newToggle as? ToggleButton
                trocoParaField.isDisable = selectedButton?.text != "Dinheiro"
                trocoCalculadoLabel.isDisable = selectedButton?.text != "Dinheiro"
            }

            val trocoBox = VBox(10.0).apply {
                style = "-fx-padding: 15; -fx-background-color: #f8f9fa; -fx-background-radius: 5; -fx-border-color: #e9ecef; -fx-border-radius: 5;"
                children.addAll(
                    Label("Troco").apply {
                        styleClass.add("field-label")
                        style = "-fx-font-size: 15px; -fx-font-weight: bold;"
                    },
                    GridPane().apply {
                        hgap = 15.0
                        vgap = 10.0
                        padding = Insets(10.0, 0.0, 0.0, 0.0)
                        add(Label("Troco Para").apply { styleClass.add("field-label") }, 0, 0)
                        add(trocoParaField.apply {
                            // Add a listener to update troco calculation when text changes
                            textProperty().addListener { _, _, _ -> controller.calcularTroco() }
                        }, 0, 1)
                        add(Label("Troco a Devolver").apply { styleClass.add("field-label") }, 1, 0)
                        add(trocoCalculadoLabel, 1, 1)
                    }
                )
            }
            leftColumn.children.add(trocoBox)

            mainGrid.add(leftColumn, 0, 1)

            // Column 2: Status and Pickup scheduling
            val rightColumn = VBox(15.0)

            // Status section
            val statusToggleGroup = ToggleGroup().apply {
                selectedToggleProperty().addListener { _, oldToggle, newToggle ->
                    if (newToggle == null && oldToggle != null) {
                        // If all toggles were deselected, reselect the previously selected toggle
                        selectToggle(oldToggle)
                    }
                }
            }
            val pendingToggle = ToggleButton("Pendente").apply {
                toggleGroup = statusToggleGroup
                styleClass.addAll("status-toggle", "payment-toggle-button")
                prefWidth = 120.0
                prefHeight = 35.0
                isSelected = true
                val iconUrl = javaClass.getResource("/icons/pendingp.png")
                val image = Image(iconUrl.toString(), 0.0, 0.0, true, true) // Preserves quality
                graphic = ImageView(image).apply {
                    fitWidth = 20.0
                    fitHeight = 20.0
                    isPreserveRatio = true
                    isSmooth = true
                }

                selectedProperty().addListener { _, _, isSelected ->
                    val selectedIconUrl = if (isSelected) {
                        javaClass.getResource("/icons/pending.png")
                    } else {
                        javaClass.getResource("/icons/pendingp.png")
                    }
                    val selectedImage = Image(selectedIconUrl.toString(), 0.0, 0.0, true, true)
                    graphic = ImageView(selectedImage).apply {
                        fitWidth = 20.0
                        fitHeight = 20.0
                        isPreserveRatio = true
                        isSmooth = true
                    }
                }
            }

            val paidToggle = ToggleButton("Pago").apply {
                toggleGroup = statusToggleGroup
                styleClass.addAll("status-toggle", "payment-toggle-button")
                prefWidth = 120.0
                prefHeight = 35.0
                val iconUrl = javaClass.getResource("/icons/paidp.png")
                val image = Image(iconUrl.toString(), 0.0, 0.0, true, true) // Preserves quality
                graphic = ImageView(image).apply {
                    fitWidth = 20.0
                    fitHeight = 20.0
                    isPreserveRatio = true
                    isSmooth = true
                }

                selectedProperty().addListener { _, _, isSelected ->
                    val selectedIconUrl = if (isSelected) {
                        javaClass.getResource("/icons/paid.png")
                    } else {
                        javaClass.getResource("/icons/paidp.png")
                    }
                    val selectedImage = Image(selectedIconUrl.toString(), 0.0, 0.0, true, true)
                    graphic = ImageView(selectedImage).apply {
                        fitWidth = 20.0
                        fitHeight = 20.0
                        isPreserveRatio = true
                        isSmooth = true
                    }
                }
            }

            val statusBox = VBox(10.0).apply {
                style = "-fx-padding: 15; -fx-background-color: #f8f9fa; -fx-background-radius: 5; -fx-border-color: #e9ecef; -fx-border-radius: 5;"
                minHeight = 145.0 // Added fixed height for consistency
                children.addAll(
                    Label("Status do Pagamento").apply {
                        styleClass.add("field-label")
                        style = "-fx-font-size: 15px; -fx-font-weight: bold;"
                    },
                    HBox(10.0).apply {
                        padding = Insets(10.0, 0.0, 0.0, 0.0)
                        VBox.setVgrow(this, Priority.ALWAYS) // Allow HBox to grow vertically
                        alignment = Pos.CENTER_LEFT // Center content vertically
                        children.addAll(pendingToggle, paidToggle)
                    }
                )
            }

            // Set initial icon state for pending toggle which is selected by default
            val iconUrl = javaClass.getResource("/icons/pending.png")
            if (iconUrl != null) {
                val image = Image(iconUrl.toExternalForm())
                val imageView = ImageView(image)
                imageView.fitWidth = 20.0
                imageView.fitHeight = 20.0
                imageView.isPreserveRatio = true
                pendingToggle.graphic = imageView
            }
            rightColumn.children.add(statusBox)

// Retirada section
            val dataPicker = DatePicker().apply {
                value = java.time.LocalDate.now()
                styleClass.add("date-picker")
                prefWidth = 150.0
                promptText = "dd/mm/aaaa"
                converter = javafx.util.converter.LocalDateStringConverter(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                )
            }

            val retiradaBox = VBox(10.0).apply {
                id = "retirada-fields"
                style = "-fx-padding: 15; -fx-background-color: #f8f9fa; -fx-background-radius: 5; -fx-border-color: #e9ecef; -fx-border-radius: 5;"
                minHeight = 140.0 // Added fixed height for consistency
                children.addAll(
                    Label("Agendamento para Retirada").apply {
                        styleClass.add("field-label")
                        style = "-fx-font-size: 15px; -fx-font-weight: bold;"
                    },
                    GridPane().apply {
                        hgap = 15.0
                        vgap = 10.0
                        padding = Insets(10.0, 0.0, 0.0, 0.0)
                        VBox.setVgrow(this, Priority.ALWAYS) // Allow GridPane to grow vertically
                        alignment = Pos.CENTER_LEFT // Center content vertically
                        add(Label("Data de Retirada").apply { styleClass.add("field-label") }, 0, 0)
                        add(dataPicker, 0, 1)
                        add(Label("Hora da Retirada").apply { styleClass.add("field-label") }, 1, 0)
                        add(HBox(5.0).apply {
                            alignment = Pos.CENTER_LEFT
                            children.addAll(
                                ComboBox<String>().apply {
                                    styleClass.add("time-picker")
                                    prefWidth = 70.0
                                    maxWidth = 70.0
                                    isEditable = true
                                    items.addAll((0..23).map { String.format("%02d", it) })
                                    value = "00"
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
                                    items.addAll((0..59 step 15).map { String.format("%02d", it) })
                                    value = "00"
                                }
                            )
                        }, 1, 1)
                    }
                )
            }
            rightColumn.children.add(retiradaBox)

            mainGrid.add(rightColumn, 1, 1)

            return createContentPane(section, mainGrid)
        }

        private fun createPaymentToggleButton(
            text: String,
            selected: Boolean = false,
            toggleGroup: ToggleGroup,
            iconPath: String? = null
        ): ToggleButton {
            return ToggleButton(text).apply {
                styleClass.add("payment-toggle-button")
                isSelected = selected
                this.toggleGroup = toggleGroup

                // Set width based on text length
                when (text) {
                    "Cartão de Crédito" -> prefWidth = 180.0 // Wider for longer text
                    "Cartão de Débito" -> prefWidth = 170.0  // Wider for longer text
                    else -> prefWidth = 130.0                // Default width
                }

                prefHeight = 40.0

                iconPath?.let {
                    // Extract base path and extension to create paths for both versions
                    val pathParts = it.split(".")
                    val basePath = pathParts[0]
                    val extension = if (pathParts.size > 1) ".${pathParts[1]}" else ""

                    // Create both versions of icon paths
                    val normalIconPath = "/${basePath}${extension}"
                    val selectedIconPath = "/${basePath.removeSuffix("p")}${extension}"

                    // Create image views for both states
                    val normalIcon = ImageView(Image(javaClass.getResourceAsStream(normalIconPath)))
                    val selectedIcon = ImageView(Image(javaClass.getResourceAsStream(selectedIconPath)))

                    // Configure both icons
                    normalIcon.fitHeight = 25.0
                    normalIcon.fitWidth = 25.0
                    normalIcon.isPreserveRatio = true

                    selectedIcon.fitHeight = 25.0
                    selectedIcon.fitWidth = 25.0
                    selectedIcon.isPreserveRatio = true

                    // Set initial icon based on selected state
                    graphic = if (selected) selectedIcon else normalIcon
                    contentDisplay = ContentDisplay.LEFT
                    graphicTextGap = 10.0

                    // Add listener to change icon when selection state changes
                    selectedProperty().addListener { _, _, newValue ->
                        graphic = if (newValue) selectedIcon else normalIcon
                    }
                }
            }
        }

        // Step 4: Entrega
        private fun createDeliveryStep(): Pane {
            val section = createSectionHeader("Informações de Entrega")

            val enableDeliveryBox = HBox(10.0).apply {
                alignment = Pos.CENTER_LEFT
                padding = Insets(0.0, 0.0, 0.0, 0.0)
            }

            val deliveryForm = VBox(20.0).apply {
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
                                    }
                                )
                            },
                            VBox(10.0).apply {
                                children.addAll(
                                    Label("Telefone do Destinatário").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        styleClass.add("text-field")
                                        prefWidth = 200.0
                                        promptText = "(XX) XXXXX-XXXX"

                                        // Add input mask for phone number
                                        textProperty().addListener { _, oldValue, newValue ->
                                            if (newValue == null) {
                                                text = oldValue
                                                return@addListener
                                            }

                                            // Keep only digits
                                            val digits = newValue.replace(Regex("[^\\d]"), "")

                                            // Format as phone number
                                            val formatted = when {
                                                digits.isEmpty() -> ""
                                                digits.length <= 2 -> "(" + digits
                                                digits.length <= 7 -> "(${digits.substring(0, 2)}) ${digits.substring(2)}"
                                                else -> {
                                                    val len = digits.length
                                                    val maxDigits = 11 // Allow up to 11 digits (including DDD)
                                                    val truncated = digits.substring(0, minOf(len, maxDigits))

                                                    if (truncated.length <= 10) {
                                                        // Format as (XX) XXXX-XXXX for 10 digits
                                                        "(${truncated.substring(0, 2)}) ${truncated.substring(2, 6)}-${truncated.substring(6)}"
                                                    } else {
                                                        // Format as (XX) XXXXX-XXXX for 11 digits
                                                        "(${truncated.substring(0, 2)}) ${truncated.substring(2, 7)}-${truncated.substring(7)}"
                                                    }
                                                }
                                            }

                                            if (formatted != newValue) {
                                                text = formatted
                                                positionCaret(text.length)
                                            }
                                        }
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
                                    Label("Endereço de Entrega").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        styleClass.add("text-field")
                                        maxWidth = Double.POSITIVE_INFINITY
                                    }
                                )
                            },
                            VBox(10.0).apply {
                                children.addAll(
                                    Label("Número").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        styleClass.add("text-field")
                                        prefWidth = 80.0
                                    }
                                )
                            }
                        )
                    },

                    HBox(10.0).apply {
                        children.addAll(
                            VBox(10.0).apply {
                                HBox.setHgrow(this, Priority.ALWAYS)  // Make the entire VBox grow
                                children.addAll(
                                    Label("Referência").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        styleClass.add("text-field")
                                        prefWidth = 300.0  // Set preferred width to 300px
                                        minWidth = 250.0   // Set minimum width to 250px
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
                                children.addAll(
                                    Label("Cidade").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        styleClass.add("text-field")
                                        HBox.setHgrow(this, Priority.ALWAYS)
                                    }
                                )
                            },
                            VBox(10.0).apply {
                                children.addAll(
                                    Label("Bairro").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        styleClass.add("text-field")
                                        prefWidth = 200.0
                                    }
                                )
                            },
                            VBox(10.0).apply {
                                children.addAll(
                                    Label("CEP").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        styleClass.add("text-field")
                                        prefWidth = 120.0
                                        promptText = "00000-000"
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
                                    TextField("R$ 0,00").apply {
                                        styleClass.add("text-field")
                                        prefWidth = 150.0
                                        alignment = Pos.CENTER_RIGHT
                                        controller.setValorEntregaField(this)
                                        controller.formatarMoeda(this)
                                        textProperty().addListener { _, _, _ ->
                                            controller.setValorEntregaTotal(controller.parseMoneyValue(text))
                                            controller.calculateTotal()
                                        }
                                    }
                                )
                            },
                            VBox(10.0).apply {
                                children.addAll(
                                    Label("Data da Entrega").apply { styleClass.add("field-label") },
                                    DatePicker(LocalDate.now()).apply {
                                        styleClass.add("date-picker")
                                        prefWidth = 150.0
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
                                                items.addAll((0..23).map { String.format("%02d", it) })
                                                value = "00"
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
                                                items.addAll((0..59 step 15).map { String.format("%02d", it) })
                                                value = "00"
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }
                )
            }

            return createContentPane(section, VBox(20.0).apply {
                padding = Insets(20.0)
                children.addAll(
                    enableDeliveryBox,
                    deliveryForm
                )
            })
        }

        // Step 5: Confirmação
        private fun createConfirmationStep(): Pane {
            val section = createSectionHeader("Confirme seu Pedido")

            val summaryContainer = VBox(30.0).apply {
                styleClass.add("summary-container")
                padding = Insets(20.0)

                children.addAll(
                    createSummarySection("Cliente", listOf(
                        "Nome" to "João Silva",
                        "Telefone" to "(11) 98765-4321"
                    )),
                    createSummarySection("Produtos", listOf(
                        "Item 1" to "2x Pizza Grande (R$ 50,00) = R$ 100,00",
                        "Item 2" to "1x Refrigerante 2L (R$ 12,00) = R$ 12,00"
                    )),
                    createSummarySection("Pagamento", listOf(
                        "Forma de Pagamento" to "Dinheiro",
                        "Status" to "Pendente",
                        "Troco para" to "R$ 150,00",
                        "Troco a devolver" to "R$ 38,00"
                    )),
                    createSummarySection("Entrega", listOf(
                        "Endereço" to "Rua das Flores, 123",
                        "Bairro" to "Centro",
                        "Valor da Entrega" to "R$ 10,00"
                    )),

                    HBox().apply {
                        styleClass.add("total-summary")
                        padding = Insets(15.0)
                        alignment = Pos.CENTER_RIGHT

                        children.addAll(
                            Label("TOTAL DO PEDIDO:").apply {
                                styleClass.add("total-summary-label")
                            },
                            Label("R$ 122,00").apply {
                                styleClass.add("total-summary-value")
                                padding = Insets(0.0, 0.0, 0.0, 15.0)
                            }
                        )
                    }
                )
            }

            return createContentPane(section, summaryContainer)
        }

        private fun createProductRow(num: Int): HBox {
            val removeButton = Button("×").apply {
                styleClass.add("remove-button")
                minWidth = 30.0
                minHeight = 30.0
                setOnAction {
                    // Get the parent row
                    val productRow = this.parent as HBox
                    // Call controller method to remove and update numbers
                    controller.removeProduct(productRow)
                }
            }

            return HBox(15.0).apply {
                styleClass.add("product-row")
                alignment = Pos.CENTER_LEFT
                padding = Insets(10.0)

                children.addAll(
                    Label("$num").apply {
                        styleClass.add("product-number")
                        minWidth = 20.0
                    },
                    VBox(5.0).apply {
                        prefWidth = 100.0
                        children.addAll(
                            Label("Quantidade").apply { styleClass.add("field-label") },
                            HBox(5.0).apply {
                                alignment = Pos.CENTER_LEFT
                                children.addAll(
                                    Button("-").apply { styleClass.add("qty-button") },
                                    TextField("1").apply {
                                        styleClass.add("qty-field")
                                        prefWidth = 50.0
                                        alignment = Pos.CENTER
                                    },
                                    Button("+").apply { styleClass.add("qty-button") }
                                )
                            }
                        )
                    },
                    VBox(5.0).apply {
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
                    VBox(5.0).apply {
                        prefWidth = 150.0
                        children.addAll(
                            Label("Preço Unitário").apply { styleClass.add("field-label") },
                            TextField("R$ 0,00").apply {
                                styleClass.add("text-field")
                                alignment = Pos.CENTER_RIGHT
                            }
                        )
                    },
                    VBox(5.0).apply {
                        prefWidth = 150.0
                        children.addAll(
                            Label("Subtotal").apply { styleClass.add("field-label") },
                            TextField("R$ 0,00").apply {
                                styleClass.add("text-field")
                                isEditable = false
                                alignment = Pos.CENTER_RIGHT
                                style = "-fx-background-color: #f8f9fa;"
                            }
                        )
                    },
                    removeButton
                )
            }
        }

        private fun createPaymentOption(text: String, selected: Boolean): ToggleButton {
            return ToggleButton(text).apply {
                styleClass.add("payment-option")
                prefWidth = 140.0
                prefHeight = 100.0
                isSelected = selected
            }
        }

        private fun createSummarySection(title: String, items: List<Pair<String, String>>): VBox {
            val section = VBox(10.0).apply {
                children.add(Label(title).apply {
                    styleClass.add("summary-section-title")
                })

                val grid = GridPane().apply {
                    hgap = 20.0
                    vgap = 10.0
                    styleClass.add("summary-grid")

                    items.forEachIndexed { index, (label, value) ->
                        add(Label(label + ":").apply {
                            styleClass.add("summary-field-label")
                        }, 0, index)

                        add(Label(value).apply {
                            styleClass.add("summary-field-value")
                        }, 1, index)

                        GridPane.setFillWidth(children.last(), true)
                    }
                }

                children.add(grid)
            }

            return section
        }

        private fun createSectionHeader(title: String): HBox {
            return HBox().apply {
                alignment = Pos.CENTER
                spacing = 15.0
                padding = Insets(10.0, 10.0, 30.0, 10.0)
                children.addAll(
                    Separator().apply { prefWidth = 200.0 },
                    Label(title).apply {
                        styleClass.add("section-header")
                        style = "-fx-font-size: 22px; -fx-font-weight: bold;"
                    },
                    Separator().apply { prefWidth = 200.0 }
                )
            }
        }

        private fun createContentPane(header: Node, contentNode: Node): Pane {
            val scrollPane = ScrollPane().apply {
                isFitToWidth = true
                vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
                hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
                styleClass.add("content-scroll-pane")
                content = VBox(20.0).apply {
                    children.addAll(header, contentNode)
                    padding = Insets(0.0, 0.0, 50.0, 0.0)
                }
            }

            return BorderPane().apply {
                center = scrollPane
                styleClass.add("step-container")
                padding = Insets(0.0)
            }
        }

        private inner class ToggleSwitch : StackPane() {
            private val switchButton = Button()
            private var isOn = false

            init {
                children.add(switchButton)

                switchButton.styleClass.add("toggle-switch")
                switchButton.setOnAction {
                    isOn = !isOn
                    updateVisualState()
                }

                updateVisualState()
            }

            private fun updateVisualState() {
                if (isOn) {
                    switchButton.styleClass.add("toggle-switch-on")
                    switchButton.styleClass.remove("toggle-switch-off")
                } else {
                    switchButton.styleClass.add("toggle-switch-off")
                    switchButton.styleClass.remove("toggle-switch-on")
                }
            }
        }
    }
}