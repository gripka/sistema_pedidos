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
import com.sistema_pedidos.controller.NovoPedidoController
import javafx.scene.image.ImageView
import javafx.scene.image.Image

class PedidoWizardView : BorderPane() {

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

    /**
     * Inner class representing the content of each order tab
     */
    private inner class OrderTabContent : BorderPane() {
        // Step indicators
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
                    if (currentStep > 0) {
                        showStep(currentStep - 1)
                    }
                }
            }

            nextButton = Button("Avançar").apply {
                styleClass.add("button_estilo")
                styleClass.add("primary-button")
                prefWidth = 150.0
                prefHeight = 40.0
                setOnAction {
                    if (currentStep < stepContainers.size - 1) {
                        showStep(currentStep + 1)
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
                    nextButton,
                    finishButton
                )
            }
        }

        private fun showStep(step: Int) {
            // Update step indicators
            for (i in stepIndicators.indices) {
                stepIndicators[i].styleClass.removeAll("current-step", "past-step", "future-step")
                when {
                    i < step -> stepIndicators[i].styleClass.add("past-step")
                    i == step -> stepIndicators[i].styleClass.add("current-step")
                    else -> stepIndicators[i].styleClass.add("future-step")
                }
            }

            // Show appropriate container with fade effect
            val container = stepContainers[step]
            val fadeTransition = FadeTransition(Duration.millis(300.0), container)
            fadeTransition.fromValue = 0.0
            fadeTransition.toValue = 1.0

            // Set the new content
            center = container
            fadeTransition.play()

            // Update navigation buttons
            prevButton.isDisable = step == 0
            nextButton.isVisible = step < stepContainers.size - 1
            finishButton.isVisible = step == stepContainers.size - 1

            currentStep = step
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
                                        // Add CPF formatter here if needed
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
                                        // Add CNPJ formatter if needed
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
                                        // Add phone formatter if needed
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
                                    // Add CEP formatter if needed
                                }
                            )
                        },
                        VBox(5.0).apply {
                            HBox.setHgrow(this, Priority.ALWAYS)
                            children.addAll(
                                Label("Logradouro").apply { styleClass.add("field-label") },
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

            val controller = NovoPedidoController()

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

            val totalBox = HBox().apply {
                styleClass.add("total-summary")
                padding = Insets(15.0)
                alignment = Pos.CENTER_RIGHT
                spacing = 20.0

                children.addAll(
                    Label("Total:").apply {
                        styleClass.add("total-summary-label")
                    },
                    Label("R$ 0,00").apply {
                        styleClass.add("total-summary-value")
                        controller.setTotalLabel(this)
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
                    totalBox
                )
            })
        }

        // Step 3: Pagamento
        private fun createPaymentStep(): Pane {
            val section = createSectionHeader("Forma de Pagamento")

            val paymentOptions = HBox(10.0).apply {
                alignment = Pos.CENTER
                children.addAll(
                    createPaymentOption("Dinheiro", true),
                    createPaymentOption("Cartão de Crédito", false),
                    createPaymentOption("Cartão de Débito", false),
                    createPaymentOption("PIX", false),
                    createPaymentOption("Voucher", false)
                )
            }

            val discountBox = HBox(20.0).apply {
                children.addAll(
                    VBox(10.0).apply {
                        children.addAll(
                            Label("Tipo de Desconto").apply { styleClass.add("field-label") },
                            HBox(15.0).apply {
                                alignment = Pos.CENTER_LEFT
                                children.addAll(
                                    RadioButton("Valor (R$)").apply {
                                        styleClass.add("custom-radio")
                                        isSelected = true
                                    },
                                    RadioButton("Percentual (%)").apply {
                                        styleClass.add("custom-radio")
                                    }
                                )
                            }
                        )
                    },
                    VBox(10.0).apply {
                        children.addAll(
                            Label("Valor do Desconto").apply { styleClass.add("field-label") },
                            TextField("R$ 0,00").apply {
                                styleClass.add("text-field")
                                prefWidth = 150.0
                                alignment = Pos.CENTER_RIGHT
                            }
                        )
                    }
                )
            }

            val trocoBox = HBox(20.0).apply {
                children.addAll(
                    VBox(10.0).apply {
                        children.addAll(
                            Label("Troco Para").apply { styleClass.add("field-label") },
                            TextField("R$ 0,00").apply {
                                styleClass.add("text-field")
                                prefWidth = 150.0
                                alignment = Pos.CENTER_RIGHT
                            }
                        )
                    },
                    VBox(10.0).apply {
                        children.addAll(
                            Label("Troco a Devolver").apply { styleClass.add("field-label") },
                            Label("R$ 0,00").apply {
                                styleClass.add("troco-label")
                                prefWidth = 150.0
                                alignment = Pos.CENTER_RIGHT
                            }
                        )
                    }
                )
            }

            val statusBox = VBox(10.0).apply {
                children.addAll(
                    Label("Status do Pagamento").apply { styleClass.add("field-label") },
                    HBox(10.0).apply {
                        children.addAll(
                            ToggleButton("Pendente").apply {
                                styleClass.add("status-toggle")
                                prefWidth = 120.0
                                prefHeight = 35.0
                                isSelected = true
                            },
                            ToggleButton("Pago").apply {
                                styleClass.add("status-toggle")
                                prefWidth = 120.0
                                prefHeight = 35.0
                            }
                        )
                    }
                )
            }

            val retiradaBox = HBox(20.0).apply {
                children.addAll(
                    VBox(10.0).apply {
                        children.addAll(
                            Label("Data de Retirada").apply { styleClass.add("field-label") },
                            DatePicker().apply {
                                styleClass.add("date-picker")
                                prefWidth = 150.0
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
                                        value = "08"
                                    },
                                    Label(":").apply { styleClass.add("time-separator") },
                                    ComboBox<String>().apply {
                                        styleClass.add("time-picker")
                                        prefWidth = 70.0
                                        value = "00"
                                    }
                                )
                            }
                        )
                    }
                )
            }

            return createContentPane(section, VBox(30.0).apply {
                padding = Insets(20.0)
                children.addAll(
                    paymentOptions,
                    discountBox,
                    trocoBox,
                    statusBox,
                    retiradaBox
                )
            })
        }

        private fun createDeliveryStep(): Pane {
            val section = createSectionHeader("Informações de Entrega")

            val enableDeliveryBox = HBox(10.0).apply {
                alignment = Pos.CENTER_LEFT
                padding = Insets(0.0, 0.0, 20.0, 0.0)
                children.addAll(
                    Label("Ativar Entrega:").apply {
                        styleClass.add("field-label")
                        style = "-fx-font-size: 16px;"
                    },

                )
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
                                        promptText = "XXXXX-XXX"
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
                                    }
                                )
                            },
                            VBox(10.0).apply {
                                children.addAll(
                                    Label("Data da Entrega").apply { styleClass.add("field-label") },
                                    DatePicker().apply {
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
                                                value = "08"
                                            },
                                            Label(":").apply { styleClass.add("time-separator") },
                                            ComboBox<String>().apply {
                                                styleClass.add("time-picker")
                                                prefWidth = 70.0
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