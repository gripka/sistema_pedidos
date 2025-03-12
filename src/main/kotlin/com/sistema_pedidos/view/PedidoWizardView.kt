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
import com.sistema_pedidos.controller.PedidoWizardController
import javafx.application.Platform
import javafx.scene.Parent
import javafx.scene.image.ImageView
import javafx.scene.image.Image
import java.time.LocalDate

class PedidoWizardView : BorderPane() {
    // Add these field references as class properties
    private lateinit var nomeField: TextField
    private lateinit var sobrenomeField: TextField
    private lateinit var cpfField: TextField
    private lateinit var razaoSocialField: TextField
    private lateinit var nomeFantasiaField: TextField
    private lateinit var cnpjField: TextField
    private lateinit var ieField: TextField
    private lateinit var telefoneField: TextField
    private lateinit var emailField: TextField
    private lateinit var observacaoField: TextField
    private lateinit var logradouroField: TextField
    private lateinit var numeroField: TextField
    private lateinit var complementoField: TextField
    private lateinit var bairroField: TextField
    private lateinit var cidadeField: TextField
    private lateinit var estadoField: ComboBox<String>
    private lateinit var cepField: TextField

    private var descontoField: TextField? = null
    private var valorRadio: RadioButton? = null
    private var percentualRadio: RadioButton? = null

    private lateinit var horaRetiradaCombo: ComboBox<String>
    private lateinit var minutoRetiradaCombo: ComboBox<String>
    private lateinit var dataRetiradaPicker: DatePicker

    private val produtosContainer = VBox().apply {
        spacing = 15.0
    }

    fun initializeDiscountFields() {
        if (scene != null) {
            descontoField = scene.lookup("#descontoField") as? TextField
            valorRadio = scene.lookup("#valor") as? RadioButton
            percentualRadio = scene.lookup("#percentual") as? RadioButton
        }
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
        private var clienteData = mutableMapOf<String, String>()
        private var isPessoaFisicaSelected = true

        private lateinit var dinheiroButton: ToggleButton
        private lateinit var pixButton: ToggleButton
        private lateinit var cartaoCreditoButton: ToggleButton
        private lateinit var cartaoDebitoButton: ToggleButton
        private lateinit var voucherButton: ToggleButton

        private lateinit var pendingToggle: ToggleButton
        private lateinit var paidToggle: ToggleButton

        private lateinit var nomeDestinatarioField: TextField
        private lateinit var telefoneDestinatarioField: TextField
        private lateinit var enderecoEntregaField: TextField
        private lateinit var numeroEntregaField: TextField
        private lateinit var referenciaEntregaField: TextField
        private lateinit var cidadeEntregaField: TextField
        private lateinit var bairroEntregaField: TextField
        private lateinit var cepEntregaField: TextField
        private lateinit var valorEntregaField: TextField

        private lateinit var dataEntregaPicker: DatePicker
        private lateinit var horaEntregaCombo: ComboBox<String>
        private lateinit var minutoEntregaCombo: ComboBox<String>

        private val controller = PedidoWizardController()
        private lateinit var entregaClienteRadio: RadioButton
        private val stepIndicators = ArrayList<StackPane>()
        private val stepLabels = listOf("Cliente", "Produtos", "Pagamento", "Entrega", "Confirmação")

        private lateinit var customerTypeToggle: ToggleGroup

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
            var targetStep = step

            if (currentStep == 0 && step > 0) {
                saveClienteData()
            }

            // Skip delivery step if not delivering to customer's address
            if (step == 3 && ::entregaClienteRadio.isInitialized && !entregaClienteRadio.isSelected) {
                targetStep = 4  // Skip to confirmation step
            }

            // Set the new content first, then update confirmation later
            val container = stepContainers[targetStep]
            center = container

            // Then handle transition effects and other UI updates
            val fadeTransition = FadeTransition(Duration.millis(300.0), container)
            fadeTransition.fromValue = 0.0
            fadeTransition.toValue = 1.0
            fadeTransition.play()

            // Update navigation buttons
            prevButton.isDisable = targetStep == 0
            nextButton.isVisible = targetStep < stepContainers.size - 1
            finishButton.isVisible = targetStep == stepContainers.size - 1

            currentStep = targetStep

            // Always update confirmation step when showing it
            if (targetStep == 4) {
                Platform.runLater {
                    Thread.sleep(100)
                    updateConfirmationStep()
                }
            }

            // Update step indicators
            for (i in stepIndicators.indices) {
                stepIndicators[i].styleClass.removeAll("current-step", "past-step", "future-step")
                when {
                    i < targetStep -> stepIndicators[i].styleClass.add("past-step")
                    i == targetStep -> stepIndicators[i].styleClass.add("current-step")
                    else -> stepIndicators[i].styleClass.add("future-step")
                }
            }
        }

        private fun saveClienteData() {
            clienteData.clear()
            isPessoaFisicaSelected = customerTypeToggle.selectedToggle?.userData == "PESSOA_FISICA"

            try {
                if (isPessoaFisicaSelected) {
                    if (::nomeField.isInitialized) clienteData["nome"] = nomeField.text ?: ""
                    if (::sobrenomeField.isInitialized) clienteData["sobrenome"] = sobrenomeField.text ?: ""
                    if (::cpfField.isInitialized) clienteData["cpf"] = cpfField.text ?: ""
                } else {
                    if (::razaoSocialField.isInitialized) clienteData["razaoSocial"] = razaoSocialField.text ?: ""
                    if (::nomeFantasiaField.isInitialized) clienteData["nomeFantasia"] = nomeFantasiaField.text ?: ""
                    if (::cnpjField.isInitialized) clienteData["cnpj"] = cnpjField.text ?: ""
                    if (::ieField.isInitialized) clienteData["ie"] = ieField.text ?: ""
                }

                // Common fields
                if (::telefoneField.isInitialized) clienteData["telefone"] = telefoneField.text ?: ""
                if (::emailField.isInitialized) clienteData["email"] = emailField.text ?: ""
                if (::observacaoField.isInitialized) clienteData["observacao"] = observacaoField.text ?: ""
                if (::logradouroField.isInitialized) clienteData["logradouro"] = logradouroField.text ?: ""
                if (::numeroField.isInitialized) clienteData["numero"] = numeroField.text ?: ""
                if (::complementoField.isInitialized) clienteData["complemento"] = complementoField.text ?: ""
                if (::bairroField.isInitialized) clienteData["bairro"] = bairroField.text ?: ""
                if (::cidadeField.isInitialized) clienteData["cidade"] = cidadeField.text ?: ""
                if (::estadoField.isInitialized) clienteData["estado"] = estadoField.value ?: ""
                if (::cepField.isInitialized) clienteData["cep"] = cepField.text ?: ""

                println("Client data saved: ${clienteData.entries.joinToString { "${it.key}=${it.value}" }}")
            } catch (e: Exception) {
                println("Error saving client data: ${e.message}")
                e.printStackTrace()
            }
        }

        //Step 0
        private fun getClienteInfo(): List<Pair<String, String>> {
            val commonInfo = mutableListOf<Pair<String, String>>()

            try {
                println("Getting client info from stored data, isPessoaFisica: $isPessoaFisicaSelected")

                if (isPessoaFisicaSelected) {
                    commonInfo.add("Nome" to (clienteData["nome"] ?: "-"))
                    commonInfo.add("Sobrenome" to (clienteData["sobrenome"] ?: "-"))
                    commonInfo.add("CPF" to (clienteData["cpf"] ?: "-"))
                    commonInfo.add("Tipo" to "Pessoa Física")
                } else {
                    commonInfo.add("Razão Social" to (clienteData["razaoSocial"] ?: "-"))
                    commonInfo.add("Nome Fantasia" to (clienteData["nomeFantasia"] ?: "-"))
                    commonInfo.add("CNPJ" to (clienteData["cnpj"] ?: "-"))
                    commonInfo.add("Inscrição Estadual" to (clienteData["ie"] ?: "-"))
                    commonInfo.add("Tipo" to "Pessoa Jurídica")
                }

                // Add common fields from stored data
                commonInfo.addAll(listOf(
                    "Telefone" to (clienteData["telefone"] ?: "-"),
                    "Email" to (clienteData["email"] ?: "-"),
                    "Observação" to (clienteData["observacao"] ?: "-"),
                    "Logradouro" to (clienteData["logradouro"] ?: "-"),
                    "Número" to (clienteData["numero"] ?: "-"),
                    "Complemento" to (clienteData["complemento"] ?: "-"),
                    "Bairro" to (clienteData["bairro"] ?: "-"),
                    "Cidade" to (clienteData["cidade"] ?: "-"),
                    "Estado" to (clienteData["estado"] ?: "-"),
                    "CEP" to (clienteData["cep"] ?: "-")
                ))

                println("Client data retrieved: ${commonInfo.size} fields")
                return commonInfo
            } catch (e: Exception) {
                println("Error in getClienteInfo: ${e.message}")
                e.printStackTrace()
                return listOf("Erro" to "Falha ao carregar dados do cliente: ${e.message}")
            }
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
            val pagamentoInfo = mutableListOf<Pair<String, String>>()

            try {
                println("Getting payment info...")

                // Use direct references instead of lookup
                println("Payment buttons found: " +
                        "dinheiro=${::dinheiroButton.isInitialized}, " +
                        "pix=${::pixButton.isInitialized}, " +
                        "crédito=${::cartaoCreditoButton.isInitialized}, " +
                        "débito=${::cartaoDebitoButton.isInitialized}, " +
                        "voucher=${::voucherButton.isInitialized}")

                // Find which button is selected
                val selectedPaymentMethod = when {
                    ::dinheiroButton.isInitialized && dinheiroButton.isSelected -> "Dinheiro"
                    ::pixButton.isInitialized && pixButton.isSelected -> "PIX"
                    ::cartaoCreditoButton.isInitialized && cartaoCreditoButton.isSelected -> "Cartão de Crédito"
                    ::cartaoDebitoButton.isInitialized && cartaoDebitoButton.isSelected -> "Cartão de Débito"
                    ::voucherButton.isInitialized && voucherButton.isSelected -> "Voucher"
                    else -> "Não especificado"
                }
                pagamentoInfo.add("Forma de Pagamento" to selectedPaymentMethod)

                // Get payment method - use direct class-based lookup
                val paymentButtons = lookupAll(".payment-toggle-button").filterIsInstance<ToggleButton>()
                println("Found ${paymentButtons.size} payment buttons, selected: $selectedPaymentMethod")

                // Get payment status - use direct class-based lookup
                val statusButtons = listOf(pendingToggle, paidToggle)
                val selectedStatus = statusButtons.find { it.isSelected }?.text ?: "Pendente"
                println("Found ${statusButtons.size} status buttons, selected: $selectedStatus")
                pagamentoInfo.add("Status do Pagamento" to selectedStatus)

                // Get discount information - use class fields directly, not lookup
                println("Discount field found: ${descontoField != null}, value: ${descontoField?.text}")
                println("Valor radio found: ${valorRadio != null}, selected: ${valorRadio?.isSelected}")
                println("Percentual radio found: ${percentualRadio != null}, selected: ${percentualRadio?.isSelected}")

                if (descontoField != null && !descontoField!!.text.isNullOrBlank()) {
                    val tipoDesconto = if (valorRadio?.isSelected == true) "Valor" else "Percentual"
                    pagamentoInfo.add("Tipo de Desconto" to tipoDesconto)
                    pagamentoInfo.add("Valor do Desconto" to descontoField!!.text)
                    println("Discount added to payment info: $tipoDesconto - ${descontoField!!.text}")
                } else {
                    println("Discount field empty or null")
                }

                // Get pickup schedule information if delivery is not selected
                if (::entregaClienteRadio.isInitialized && !entregaClienteRadio.isSelected) {
                    // Use class field references instead of lookup
                    if (::dataRetiradaPicker.isInitialized && ::horaRetiradaCombo.isInitialized && ::minutoRetiradaCombo.isInitialized) {
                        val formattedDate = dataRetiradaPicker.value?.format(
                            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        ) ?: LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))

                        val hora = horaRetiradaCombo.value ?: "00"
                        val minuto = minutoRetiradaCombo.value ?: "00"

                        pagamentoInfo.add("Data de Retirada" to formattedDate)
                        pagamentoInfo.add("Horário de Retirada" to "$hora:$minuto")
                    }
                }

                // Get total after discount
                val totalLabel = lookup(".total-nav-value") as? Label
                if (totalLabel != null && totalLabel.text != "R$ 0,00") {
                    pagamentoInfo.add("Total" to totalLabel.text)
                }

                return pagamentoInfo
            } catch (e: Exception) {
                println("Error getting payment info: ${e.message}")
                e.printStackTrace()
                return listOf("Erro" to "Falha ao carregar dados de pagamento: ${e.message}")
            }
        }

        private fun getEntregaInfo(): List<Pair<String, String>> {
            val entregaInfo = mutableListOf<Pair<String, String>>()

            try {
                println("Starting delivery info collection...")

                // First check if delivery is enabled
                if (!::entregaClienteRadio.isInitialized || !entregaClienteRadio.isSelected) {
                    entregaInfo.add("Entrega" to "Não")
                    return entregaInfo
                }

                // Delivery is enabled
                entregaInfo.add("Entrega" to "Sim")

                // Use direct field references
                if (::nomeDestinatarioField.isInitialized && !nomeDestinatarioField.text.isNullOrBlank())
                    entregaInfo.add("Nome do Destinatário" to nomeDestinatarioField.text)

                if (::telefoneDestinatarioField.isInitialized && !telefoneDestinatarioField.text.isNullOrBlank())
                    entregaInfo.add("Telefone" to telefoneDestinatarioField.text)

                if (::enderecoEntregaField.isInitialized && !enderecoEntregaField.text.isNullOrBlank())
                    entregaInfo.add("Endereço" to enderecoEntregaField.text)

                if (::numeroEntregaField.isInitialized && !numeroEntregaField.text.isNullOrBlank())
                    entregaInfo.add("Número" to numeroEntregaField.text)

                if (::referenciaEntregaField.isInitialized && !referenciaEntregaField.text.isNullOrBlank())
                    entregaInfo.add("Referência" to referenciaEntregaField.text)

                if (::cidadeEntregaField.isInitialized && !cidadeEntregaField.text.isNullOrBlank())
                    entregaInfo.add("Cidade" to cidadeEntregaField.text)

                if (::bairroEntregaField.isInitialized && !bairroEntregaField.text.isNullOrBlank())
                    entregaInfo.add("Bairro" to bairroEntregaField.text)

                if (::cepEntregaField.isInitialized && !cepEntregaField.text.isNullOrBlank())
                    entregaInfo.add("CEP" to cepEntregaField.text)

                if (::valorEntregaField.isInitialized && !valorEntregaField.text.isNullOrBlank())
                    entregaInfo.add("Valor" to valorEntregaField.text)

                // Date and time (already working)
                if (::dataEntregaPicker.isInitialized && dataEntregaPicker.value != null) {
                    val formattedDate = dataEntregaPicker.value.format(
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    )
                    entregaInfo.add("Data" to formattedDate)
                }

                if (::horaEntregaCombo.isInitialized && ::minutoEntregaCombo.isInitialized) {
                    val hora = horaEntregaCombo.value?.toString() ?: "00"
                    val minuto = minutoEntregaCombo.value?.toString() ?: "00"
                    entregaInfo.add("Horário" to "$hora:$minuto")
                }

                println("Delivery info collection finished: ${entregaInfo.size} items")
                return entregaInfo
            } catch (e: Exception) {
                println("Error collecting delivery info: ${e.message}")
                e.printStackTrace()
                return listOf("Entrega" to "Sim", "Erro" to "Falha ao carregar dados: ${e.message}")
            }
        }

        // Helper function to recursively collect all nodes
        private fun collectAllNodes(parent: Node, collected: MutableList<Node>) {
            collected.add(parent)
            if (parent is Parent) {
                parent.childrenUnmodifiable.forEach { child ->
                    collectAllNodes(child, collected)
                }
            }
        }

        // Step 1: Cliente
        private fun createClientStep(): Pane {
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

            customerTypeToggle = ToggleGroup()

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

            // Initialize pessoa física fields
            nomeField = TextField().apply {
                id = "nomeField"
                styleClass.add("text-field")
                maxWidth = Double.POSITIVE_INFINITY
                promptText = "Nome"
            }

            sobrenomeField = TextField().apply {
                id = "sobrenomeField"
                styleClass.add("text-field")
                maxWidth = Double.POSITIVE_INFINITY
                promptText = "Sobrenome"
            }

            cpfField = TextField().apply {
                id = "cpfField"
                styleClass.add("text-field")
                prefWidth = 150.0
                promptText = "000.000.000-00"

                // CPF formatter
                textProperty().addListener { _, oldValue, newValue ->
                    if (newValue == null) {
                        text = oldValue
                        return@addListener
                    }

                    val value = newValue.replace(Regex("[^0-9]"), "").take(11)

                    val formattedValue = when {
                        value.length > 9 -> "${value.substring(0, 3)}.${value.substring(3, 6)}.${value.substring(6, 9)}-${value.substring(9)}"
                        value.length > 6 -> "${value.substring(0, 3)}.${value.substring(3, 6)}.${value.substring(6)}"
                        value.length > 3 -> "${value.substring(0, 3)}.${value.substring(3)}"
                        else -> value
                    }

                    if (formattedValue != newValue) {
                        text = formattedValue
                        positionCaret(text.length)
                    }
                }
            }

            // Initialize pessoa jurídica fields
            razaoSocialField = TextField().apply {
                id = "razaoSocialField"
                styleClass.add("text-field")
                maxWidth = Double.POSITIVE_INFINITY
                promptText = "Razão Social"
            }

            nomeFantasiaField = TextField().apply {
                id = "nomeFantasiaField"
                styleClass.add("text-field")
                maxWidth = Double.POSITIVE_INFINITY
                promptText = "Nome Fantasia"
            }

            cnpjField = TextField().apply {
                id = "cnpjField"
                styleClass.add("text-field")
                prefWidth = 150.0
                promptText = "00.000.000/0000-00"

                // CNPJ formatter
                textProperty().addListener { _, oldValue, newValue ->
                    if (newValue == null) {
                        text = oldValue
                        return@addListener
                    }

                    val value = newValue.replace(Regex("[^0-9]"), "").take(14)

                    val formattedValue = when {
                        value.length > 12 -> "${value.substring(0, 2)}.${value.substring(2, 5)}.${value.substring(5, 8)}/${value.substring(8, 12)}-${value.substring(12)}"
                        value.length > 8 -> "${value.substring(0, 2)}.${value.substring(2, 5)}.${value.substring(5, 8)}/${value.substring(8)}"
                        value.length > 5 -> "${value.substring(0, 2)}.${value.substring(2, 5)}.${value.substring(5)}"
                        value.length > 2 -> "${value.substring(0, 2)}.${value.substring(2)}"
                        else -> value
                    }

                    if (formattedValue != newValue) {
                        text = formattedValue
                        positionCaret(text.length)
                    }
                }
            }

            ieField = TextField().apply {
                id = "ieField"
                styleClass.add("text-field")
                prefWidth = 150.0
                promptText = "Inscrição Estadual"
            }

            // Container for pessoa física fields
            val pessoaFisicaFields = VBox(10.0).apply {
                isVisible = true
                isManaged = true
                children.addAll(
                    HBox(10.0).apply {
                        children.addAll(
                            VBox(5.0).apply {
                                HBox.setHgrow(this, Priority.ALWAYS)
                                children.addAll(
                                    Label("Nome").apply { styleClass.add("field-label") },
                                    nomeField
                                )
                            },
                            VBox(5.0).apply {
                                HBox.setHgrow(this, Priority.ALWAYS)
                                children.addAll(
                                    Label("Sobrenome").apply { styleClass.add("field-label") },
                                    sobrenomeField
                                )
                            }
                        )
                    },
                    HBox(10.0).apply {
                        children.addAll(
                            VBox(5.0).apply {
                                children.addAll(
                                    Label("CPF").apply { styleClass.add("field-label") },
                                    cpfField
                                )
                            }
                        )
                    }
                )
            }

            // Container for pessoa jurídica fields
            val pessoaJuridicaFields = VBox(10.0).apply {
                isVisible = false
                isManaged = false
                children.addAll(
                    HBox(10.0).apply {
                        children.addAll(
                            VBox(5.0).apply {
                                HBox.setHgrow(this, Priority.ALWAYS)
                                children.addAll(
                                    Label("Razão Social").apply { styleClass.add("field-label") },
                                    razaoSocialField
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
                                    nomeFantasiaField
                                )
                            }
                        )
                    },
                    HBox(10.0).apply {
                        children.addAll(
                            VBox(5.0).apply {
                                children.addAll(
                                    Label("CNPJ").apply { styleClass.add("field-label") },
                                    cnpjField
                                )
                            },
                            VBox(5.0).apply {
                                children.addAll(
                                    Label("Inscrição Estadual").apply { styleClass.add("field-label") },
                                    ieField
                                )
                            }
                        )
                    }
                )
            }

            // Initialize common fields
            telefoneField = TextField().apply {
                id = "telefoneField"
                styleClass.add("text-field")
                prefWidth = 150.0
                promptText = "(00) 00000-0000"

                // Phone formatter
                textProperty().addListener { _, oldValue, newValue ->
                    if (newValue == null) {
                        text = oldValue
                        return@addListener
                    }

                    val value = newValue.replace(Regex("[^0-9]"), "").take(11)

                    val formattedValue = when {
                        value.length > 6 -> "(${value.substring(0, 2)}) ${value.substring(2, 7)}-${value.substring(7)}"
                        value.length > 2 -> "(${value.substring(0, 2)}) ${value.substring(2)}"
                        else -> value
                    }

                    if (formattedValue != newValue) {
                        text = formattedValue
                        positionCaret(text.length)
                    }
                }
            }

            emailField = TextField().apply {
                id = "emailField"
                styleClass.add("text-field")
                maxWidth = Double.POSITIVE_INFINITY
                promptText = "email@exemplo.com"
            }

            observacaoField = TextField().apply {
                id = "observacaoField"
                styleClass.add("text-field")
                maxWidth = Double.POSITIVE_INFINITY
                promptText = "Observações sobre o cliente"
            }

            // Common fields for both customer types
            val commonFields = VBox(10.0).apply {
                children.addAll(
                    HBox(10.0).apply {
                        children.addAll(
                            VBox(5.0).apply {
                                children.addAll(
                                    Label("Telefone").apply { styleClass.add("field-label") },
                                    telefoneField
                                )
                            },
                            VBox(5.0).apply {
                                HBox.setHgrow(this, Priority.ALWAYS)
                                children.addAll(
                                    Label("Email").apply { styleClass.add("field-label") },
                                    emailField
                                )
                            }
                        )
                    },
                    VBox(5.0).apply {
                        children.addAll(
                            Label("Observações").apply { styleClass.add("field-label") },
                            observacaoField
                        )
                    }
                )
            }

            // Initialize address fields
            logradouroField = TextField().apply {
                id = "logradouroField"
                styleClass.add("text-field")
                maxWidth = Double.POSITIVE_INFINITY
                promptText = "Rua, Avenida, etc."
            }

            numeroField = TextField().apply {
                id = "numeroField"
                styleClass.add("text-field")
                prefWidth = 100.0
                promptText = "Nº"
            }

            complementoField = TextField().apply {
                id = "complementoField"
                styleClass.add("text-field")
                maxWidth = Double.POSITIVE_INFINITY
                promptText = "Complemento"
            }

            bairroField = TextField().apply {
                id = "bairroField"
                styleClass.add("text-field")
                maxWidth = Double.POSITIVE_INFINITY
                promptText = "Bairro"
            }

            cidadeField = TextField().apply {
                id = "cidadeField"
                styleClass.add("text-field")
                maxWidth = Double.POSITIVE_INFINITY
                promptText = "Cidade"
            }

            estadoField = ComboBox<String>().apply {
                id = "estadoField"
                styleClass.add("combo-box")
                prefWidth = 100.0
                items.addAll(
                    "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
                    "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI",
                    "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
                )
            }

            cepField = TextField().apply {
                id = "cepField"
                styleClass.add("text-field")
                prefWidth = 150.0
                promptText = "00000-000"

                // CEP formatter
                textProperty().addListener { _, oldValue, newValue ->
                    if (newValue == null) {
                        text = oldValue
                        return@addListener
                    }

                    val value = newValue.replace(Regex("[^0-9]"), "").take(8)

                    val formattedValue = if (value.length > 5) {
                        "${value.substring(0, 5)}-${value.substring(5)}"
                    } else {
                        value
                    }

                    if (formattedValue != newValue) {
                        text = formattedValue
                        positionCaret(text.length)
                    }
                }
            }

            // Address section
            val addressSection = VBox(15.0).apply {
                children.addAll(
                    Label("Endereço").apply { styleClass.add("section-subheader") },
                    HBox(10.0).apply {
                        children.addAll(
                            VBox(5.0).apply {
                                HBox.setHgrow(this, Priority.ALWAYS)
                                children.addAll(
                                    Label("Logradouro").apply { styleClass.add("field-label") },
                                    logradouroField
                                )
                            },
                            VBox(5.0).apply {
                                children.addAll(
                                    Label("Número").apply { styleClass.add("field-label") },
                                    numeroField
                                )
                            }
                        )
                    },
                    HBox(10.0).apply {
                        children.addAll(
                            VBox(5.0).apply {
                                HBox.setHgrow(this, Priority.ALWAYS)
                                children.addAll(
                                    Label("Complemento").apply { styleClass.add("field-label") },
                                    complementoField
                                )
                            }
                        )
                    },
                    HBox(10.0).apply {
                        children.addAll(
                            VBox(5.0).apply {
                                HBox.setHgrow(this, Priority.ALWAYS)
                                children.addAll(
                                    Label("Bairro").apply { styleClass.add("field-label") },
                                    bairroField
                                )
                            }
                        )
                    },
                    HBox(10.0).apply {
                        children.addAll(
                            VBox(5.0).apply {
                                HBox.setHgrow(this, Priority.ALWAYS)
                                children.addAll(
                                    Label("Cidade").apply { styleClass.add("field-label") },
                                    cidadeField
                                )
                            },
                            VBox(5.0).apply {
                                children.addAll(
                                    Label("Estado").apply { styleClass.add("field-label") },
                                    estadoField
                                )
                            },
                            VBox(5.0).apply {
                                children.addAll(
                                    Label("CEP").apply { styleClass.add("field-label") },
                                    cepField
                                )
                            }
                        )
                    }
                )
            }

            // Toggle between pessoa física and pessoa jurídica
            customerTypeToggle.selectedToggleProperty().addListener { _, _, newValue ->
                val isPessoaFisica = (newValue as RadioButton).userData == "PESSOA_FISICA"
                pessoaFisicaFields.isVisible = isPessoaFisica
                pessoaFisicaFields.isManaged = isPessoaFisica
                pessoaJuridicaFields.isVisible = !isPessoaFisica
                pessoaJuridicaFields.isManaged = !isPessoaFisica
            }

            // Add all components to container
            contentContainer.children.addAll(
                sectionHeader,
                customerTypeBox,
                pessoaFisicaFields,
                pessoaJuridicaFields,
                Separator(),
                commonFields,
                Separator(),
                addressSection
            )

            // Set the ScrollPane to the BorderPane's center
            mainContainer.center = scrollPane

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

            horaRetiradaCombo = ComboBox<String>().apply {
                items.addAll((0..23).map { String.format("%02d", it) })
                value = "12"
                prefWidth = 70.0
            }

            minutoRetiradaCombo = ComboBox<String>().apply {
                items.addAll((0..59).map { String.format("%02d", it) })
                value = "00"
                prefWidth = 70.0
            }

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

            // Initialize button references first
            dinheiroButton = createPaymentToggleButton("Dinheiro", true, paymentToggleGroup, "icons/moneyp.png").apply {
                id = "dinheiroButton"
            }

            cartaoCreditoButton = createPaymentToggleButton("Cartão de Crédito", false, paymentToggleGroup, "icons/credit-cardp.png").apply {
                id = "cartaoCreditoButton"
            }

            cartaoDebitoButton = createPaymentToggleButton("Cartão de Débito", false, paymentToggleGroup, "icons/debit-cardp.png").apply {
                id = "cartaoDebitoButton"
            }

            pixButton = createPaymentToggleButton("PIX", false, paymentToggleGroup, "icons/pixp.png").apply {
                id = "pixButton"
            }

            voucherButton = createPaymentToggleButton("Voucher", false, paymentToggleGroup, "icons/voucherp.png").apply {
                id = "voucherButton"
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
                            dinheiroButton,
                            cartaoCreditoButton,
                            cartaoDebitoButton,
                            pixButton,
                            voucherButton
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

            valorRadio = RadioButton("Valor (R$)").apply {
                toggleGroup = descontoToggleGroup
                isSelected = true
                styleClass.add("custom-radio")
                id = "valor"
            }

            percentualRadio = RadioButton("Percentual (%)").apply {
                toggleGroup = descontoToggleGroup
                styleClass.add("custom-radio")
                id = "percentual"
            }

            descontoField = TextField().apply {
                id = "descontoField"
                styleClass.add("text-field")
                prefWidth = 150.0
                alignment = Pos.CENTER_RIGHT
                promptText = "R$ 0,00"
                controller.setDescontoField(this)
                var currentTextListener = controller.formatarMoeda(this)

                textProperty().addListener { _, _, _ ->
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
                            children.addAll(valorRadio!!, percentualRadio!!)
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
// Status section
            val statusToggleGroup = ToggleGroup().apply {
                selectedToggleProperty().addListener { _, oldToggle, newToggle ->
                    if (newToggle == null && oldToggle != null) {
                        // If all toggles were deselected, reselect the previously selected toggle
                        selectToggle(oldToggle)
                    }
                }
            }

// Assign directly to the class properties (remove the val keyword)
            pendingToggle = ToggleButton("Pendente").apply {
                id = "pendingToggle"
                toggleGroup = statusToggleGroup
                styleClass.addAll("status-toggle", "payment-toggle-button")
                prefWidth = 120.0
                prefHeight = 35.0
                isSelected = true
                val iconUrl = javaClass.getResource("/icons/pendingp.png")
                val image = Image(iconUrl.toString(), 0.0, 0.0, true, true)
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

            paidToggle = ToggleButton("Pago").apply {
                id = "paidToggle"
                toggleGroup = statusToggleGroup
                styleClass.addAll("status-toggle", "payment-toggle-button")
                prefWidth = 120.0
                prefHeight = 35.0
                // Fix: Use the correct icon (paidp.png for the initial state)
                val iconUrl = javaClass.getResource("/icons/paidp.png")
                val image = Image(iconUrl.toString(), 0.0, 0.0, true, true)
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

            // Use these in the retiradaBox creation
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

                        // Create date picker with reference
                        dataRetiradaPicker = DatePicker().apply {
                            value = LocalDate.now()
                            styleClass.add("date-picker")
                            prefWidth = 150.0
                            promptText = "dd/mm/aaaa"
                            converter = javafx.util.converter.LocalDateStringConverter(
                                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                            )
                        }

                        add(Label("Data de Retirada").apply { styleClass.add("field-label") }, 0, 0)
                        add(dataRetiradaPicker, 0, 1)
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
                                    value = "08" // Default to 8 AM
                                    horaRetiradaCombo = this // Store reference
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
                                    minutoRetiradaCombo = this // Store reference
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

        private fun createDeliveryStep(): Pane {
            val section = createSectionHeader("Informações de Entrega")

            val deliveryForm = VBox(20.0).apply {
                id = "delivery-form"
                children.addAll(
                    HBox(10.0).apply {
                        children.addAll(
                            VBox(10.0).apply {
                                HBox.setHgrow(this, Priority.ALWAYS)
                                children.addAll(
                                    Label("Nome do Destinatário").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        id = "nomeDestinatarioField"
                                        styleClass.add("text-field")
                                        maxWidth = Double.POSITIVE_INFINITY
                                        this@OrderTabContent.nomeDestinatarioField = this
                                    }
                                )
                            },
                            VBox(10.0).apply {
                                children.addAll(
                                    Label("Telefone do Destinatário").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        id = "telefoneDestinatarioField"
                                        styleClass.add("text-field")
                                        prefWidth = 200.0
                                        promptText = "(XX) XXXXX-XXXX"
                                        this@OrderTabContent.telefoneDestinatarioField = this
                                    }
                                )
                            },
                            VBox(10.0).apply {
                                children.addAll(
                                    Label("Telefone do Destinatário").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        id = "telefoneDestinatarioField"
                                        styleClass.add("text-field")
                                        prefWidth = 200.0
                                        promptText = "(XX) XXXXX-XXXX"
                                        // Assign to class property
                                        this@OrderTabContent.telefoneDestinatarioField = this

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
                                        id = "enderecoEntregaField"
                                        styleClass.add("text-field")
                                        maxWidth = Double.POSITIVE_INFINITY
                                        // Assign to class property
                                        this@OrderTabContent.enderecoEntregaField = this
                                    }
                                )
                            },
                            VBox(10.0).apply {
                                children.addAll(
                                    Label("Número").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        id = "numeroEntregaField"
                                        styleClass.add("text-field")
                                        prefWidth = 80.0
                                        // Assign to class property
                                        this@OrderTabContent.numeroEntregaField = this
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
                                    Label("Referência").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        id = "referenciaEntregaField"
                                        styleClass.add("text-field")
                                        prefWidth = 300.0
                                        minWidth = 250.0
                                        maxWidth = Double.POSITIVE_INFINITY
                                        HBox.setHgrow(this, Priority.ALWAYS)
                                        // Assign to class property
                                        this@OrderTabContent.referenciaEntregaField = this
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
                                        id = "cidadeEntregaField"
                                        styleClass.add("text-field")
                                        HBox.setHgrow(this, Priority.ALWAYS)
                                        // Assign to class property
                                        this@OrderTabContent.cidadeEntregaField = this
                                    }
                                )
                            },
                            VBox(10.0).apply {
                                children.addAll(
                                    Label("Bairro").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        id = "bairroEntregaField"
                                        styleClass.add("text-field")
                                        prefWidth = 200.0
                                        // Assign to class property
                                        this@OrderTabContent.bairroEntregaField = this
                                    }
                                )
                            },
                            VBox(10.0).apply {
                                children.addAll(
                                    Label("CEP").apply { styleClass.add("field-label") },
                                    TextField().apply {
                                        id = "cepEntregaField"
                                        styleClass.add("text-field")
                                        prefWidth = 120.0
                                        promptText = "00000-000"
                                        controller.formatarCep(this)
                                        // Assign to class property
                                        this@OrderTabContent.cepEntregaField = this
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
                                        id = "valorEntregaField"
                                        styleClass.add("text-field")
                                        prefWidth = 150.0
                                        alignment = Pos.CENTER_RIGHT
                                        controller.setValorEntregaField(this)
                                        controller.formatarMoeda(this)
                                        // Assign to class property
                                        this@OrderTabContent.valorEntregaField = this
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
                                        id = "dataEntregaField"
                                        styleClass.add("date-picker")
                                        prefWidth = 150.0
                                        // Assign to class property
                                        this@OrderTabContent.dataEntregaPicker = this
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
                                                id = "horaEntregaField"
                                                styleClass.add("time-picker")
                                                prefWidth = 70.0
                                                maxWidth = 70.0
                                                isEditable = true
                                                items.addAll((0..23).map { String.format("%02d", it) })
                                                value = "00"
                                                // Assign to class property
                                                this@OrderTabContent.horaEntregaCombo = this
                                            },
                                            Label(":").apply {
                                                styleClass.add("field-label")
                                                style = "-fx-padding: 5 0 0 0;"
                                            },
                                            ComboBox<String>().apply {
                                                id = "minutoEntregaField"
                                                styleClass.add("time-picker")
                                                prefWidth = 70.0
                                                maxWidth = 70.0
                                                isEditable = true
                                                items.addAll((0..59 step 15).map { String.format("%02d", it) })
                                                value = "00"
                                                // Assign to class property
                                                this@OrderTabContent.minutoEntregaCombo = this
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
                spacing = 15.0
                children.add(deliveryForm) // Removed enableDeliveryBox
            })
        }

        // Step 5: Confirmação
        private fun createConfirmationStep(): Pane {
            val section = createSectionHeader("Confirme seu Pedido")

            // Create the container
            val summaryContainer = VBox(30.0).apply {
                styleClass.add("summary-container")
                padding = Insets(20.0)
                id = "summary-container"

                // Add placeholder text during initialization
                children.add(Label("Os dados do pedido serão exibidos aqui quando você navegar até esta etapa.")
                    .apply {
                        style = "-fx-font-style: italic; -fx-text-fill: #888888;"
                        alignment = Pos.CENTER
                        maxWidth = Double.MAX_VALUE
                        padding = Insets(30.0)
                    })
            }

            return createContentPane(section, summaryContainer)
        }

        // Update method to properly handle the summary data
        private fun updateConfirmationStep() {
            Platform.runLater {
                try {
                    println("Updating confirmation step")

                    // More reliable container lookup with multiple fallback strategies
                    var summaryContainer: VBox? = lookup("#summary-container") as? VBox

                    if (summaryContainer == null) {
                        println("First lookup attempt failed, trying alternatives...")
                        // Try finding by class
                        summaryContainer = lookupAll(".summary-container").filterIsInstance<VBox>().firstOrNull()

                        if (summaryContainer == null) {
                            // Last resort - find any VBox inside the confirmation step container
                            val currentContainer = stepContainers[4]
                            summaryContainer = currentContainer.lookupAll(".summary-container")
                                .filterIsInstance<VBox>().firstOrNull()
                                ?: currentContainer.lookupAll("VBox").filterIsInstance<VBox>().firstOrNull()
                        }
                    }

                    if (summaryContainer != null) {
                        println("Summary container found. Updating contents...")
                        updateSummaryContainer(summaryContainer)
                    } else {
                        println("All attempts to find summary container failed. Creating a new one.")
                        val newContainer = VBox(30.0).apply {
                            styleClass.add("summary-container")
                            padding = Insets(20.0)
                            id = "summary-container"
                        }

                        // Replace content in step container
                        val confirmationStep = stepContainers[4]
                        if (confirmationStep is BorderPane) {
                            val scrollContent = (confirmationStep.center as? ScrollPane)?.content as? VBox
                            scrollContent?.children?.clear()
                            scrollContent?.children?.add(newContainer)
                            updateSummaryContainer(newContainer)
                        }
                    }
                } catch (e: Exception) {
                    println("Error in updateConfirmationStep: ${e.message}")
                    e.printStackTrace()
                }
            }
        }

        private fun updateSummaryContainer(container: VBox) {
            // Clear existing content
            container.children.clear()

            try {
                // Collect data with debug information
                val clientInfo = getClienteInfo()
                println("Client info items: ${clientInfo.size}")

                val produtosInfo = getProdutosInfo()
                println("Produtos info items: ${produtosInfo.size}")

                val pagamentoInfo = getPagamentoInfo()
                println("Pagamento info items: ${pagamentoInfo.size}")

                val entregaInfo = getEntregaInfo()
                println("Entrega info items: ${entregaInfo.size}")

                if (clientInfo.isNotEmpty()) {
                    val filteredClientInfo = clientInfo.filter { it.second.isNotEmpty() }
                    if (filteredClientInfo.isNotEmpty()) {
                        container.children.add(createSummarySection("Cliente", filteredClientInfo))
                    }
                }

                if (produtosInfo.isNotEmpty()) {
                    container.children.add(createSummarySection("Produtos", produtosInfo))
                }

                if (pagamentoInfo.isNotEmpty()) {
                    container.children.add(createSummarySection("Pagamento", pagamentoInfo))
                }

                if (entregaInfo.isNotEmpty()) {
                    container.children.add(createSummarySection("Entrega", entregaInfo))
                }

            } catch (e: Exception) {
                container.children.add(
                    Label("Erro ao carregar dados: ${e.message}").apply {
                        style = "-fx-text-fill: red;"
                        maxWidth = Double.MAX_VALUE
                    }
                )
            }
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
            return VBox(5.0).apply {
                styleClass.add("summary-section")
                padding = Insets(15.0)

                // Add title
                children.add(Label(title).apply {
                    styleClass.add("summary-section-title")
                })

                // Add content in a grid
                val grid = GridPane().apply {
                    hgap = 15.0
                    vgap = 8.0
                    padding = Insets(10.0, 5.0, 0.0, 5.0)

                    // Create two columns
                    columnConstraints.addAll(
                        ColumnConstraints().apply {
                            minWidth = 120.0
                            prefWidth = 150.0
                        },
                        ColumnConstraints().apply {
                            hgrow = Priority.ALWAYS
                            setFillWidth(true) // Using setter instead of direct property access
                        }
                    )
                }

                // Add all items to the grid, including rows with default values
                items.forEachIndexed { index, (label, value) ->
                    grid.add(Label("$label:").apply {
                        styleClass.add("summary-label")
                    }, 0, index)

                    grid.add(Label(value).apply {
                        styleClass.add("summary-value")
                        isWrapText = true
                    }, 1, index)
                }

                children.add(grid)
            }
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