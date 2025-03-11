package com.sistema_pedidos.controller

import com.sistema_pedidos.database.DatabaseHelper
import com.sistema_pedidos.model.Pedido
import javafx.geometry.Pos
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.application.Platform
import com.sistema_pedidos.model.Produto
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.*
import java.sql.SQLException
import java.sql.Statement

import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.geometry.Side
import javafx.scene.input.KeyCode
import javafx.stage.StageStyle
import java.sql.Connection
import javafx.beans.property.SimpleStringProperty

class PedidoWizardController {
    private val produtosContainer = VBox(10.0)
    private val listaProdutos: ObservableList<Produto> = FXCollections.observableArrayList()
    private lateinit var valorEntregaField: TextField
    private lateinit var totalLabelRef: Label
    private lateinit var descontoField: TextField
    internal lateinit var descontoToggleGroup: ToggleGroup
    private lateinit var trocoParaField: TextField
    private lateinit var trocoCalculadoLabel: Label
    private lateinit var contentContainer: VBox
    private var subtotalValue: Double = 0.0
    private var valorDesconto: Double = 0.0
    private var valorEntregaTotal: Double = 0.0
    private val tipoDesconto = SimpleStringProperty("valor") // Default to "valor"

    val horaEntregaProperty = SimpleObjectProperty<String>()

    private fun updateRemoveButtonsVisibility() {
        // Only show remove buttons if there's more than one product
        val shouldShowButtons = produtosContainer.children.size > 1

        produtosContainer.children.forEach { node ->
            val hBox = node as HBox
            val lastChild = hBox.children.last()
            if (lastChild is Button && lastChild.styleClass.contains("remove-button")) {
                lastChild.isVisible = shouldShowButtons
                lastChild.isManaged = shouldShowButtons
            }
        }
    }

    private fun incrementQuantity(quantityField: TextField) {
        try {
            val currentValue = quantityField.text.toInt()
            // Set upper limit to prevent unreasonably large quantities
            if (currentValue < 999) {
                quantityField.text = (currentValue + 1).toString()

                // Find the row container
                val row = quantityField.parent.parent.parent as HBox
                // Update product subtotal
                updateProductSubtotal(row)
                // Recalculate the total
                calculateTotal()
            }
        } catch (e: NumberFormatException) {
            // If the text isn't a valid number, reset to 1
            quantityField.text = "1"
        }
    }

    private fun decrementQuantity(quantityField: TextField) {
        try {
            val currentValue = quantityField.text.toInt()
            // Don't allow quantity less than 1
            if (currentValue > 1) {
                quantityField.text = (currentValue - 1).toString()

                // Find the row container
                val row = quantityField.parent.parent.parent as HBox
                // Update product subtotal
                updateProductSubtotal(row)
                // Recalculate the total
                calculateTotal()
            }
        } catch (e: NumberFormatException) {
            // If the text isn't a valid number, reset to 1
            quantityField.text = "1"
        }
    }

    private fun updateProductSubtotal(row: HBox) {
        // Extract quantity field
        val quantityContainer = (row.children[1] as VBox).children[1] as HBox
        val quantityField = quantityContainer.children[1] as TextField
        val quantity = quantityField.text.toIntOrNull() ?: 1

        // Extract unit price field
        val unitPriceField = ((row.children[3] as VBox).children[1] as TextField)
        val unitPrice = parseMoneyValue(unitPriceField.text)

        // Calculate subtotal
        val subtotal = quantity * unitPrice

        // Format subtotal and update field
        val formattedSubtotal = String.format("R$ %,.2f", subtotal)
            .replace(",", ".")
            .replace(".", ",", ignoreCase = true)
            .replaceFirst(",", ".")

        val subtotalField = ((row.children[4] as VBox).children[1] as TextField)
        subtotalField.text = formattedSubtotal
    }

    fun validateQuantity(textField: TextField, newValue: String) {
        val filtered = newValue.filter { it.isDigit() }
        if (filtered != newValue) {
            textField.text = filtered
        }
        if (filtered.isNotEmpty()) {
            val num = filtered.toInt()
            when {
                num < 1 -> textField.text = "1"
                num > 999 -> textField.text = "999"
            }
        } else {
            textField.text = "1"
        }
    }

    fun setContentContainer(container: VBox) {
        contentContainer = container
    }

    fun formatarInscricaoEstadual(textField: TextField) {
        val maxLength = 14 // Most state registrations won't exceed this length

        textField.textProperty().addListener { _, oldValue, newValue ->
            if (newValue == null) {
                textField.text = oldValue
                return@addListener
            }

            // Allow only numbers, letters and dots/dashes
            val cleanValue = newValue.replace(Regex("[^0-9A-Za-z.-]"), "")

            // If the user is deleting characters, don't reformat
            if (cleanValue.length < oldValue.length) {
                textField.text = cleanValue
                return@addListener
            }

            // Restrict to maximum length
            var value = if (cleanValue.length > maxLength) {
                cleanValue.substring(0, maxLength)
            } else {
                cleanValue
            }

            // Don't try to format if already contains separators
            if (!value.contains('.') && !value.contains('-') && value.length > 3) {
                // For most Brazilian states, common format is XXX.XXX.XXX
                val sb = StringBuilder()
                for (i in value.indices) {
                    if (i > 0 && i % 3 == 0 && i < value.length - 1) {
                        sb.append('.')
                    }
                    sb.append(value[i])
                }
                value = sb.toString()
            }

            if (value != newValue) {
                textField.text = value
                textField.positionCaret(textField.text.length)
            }
        }
    }

    fun formatarCnpj(textField: TextField) {
        val maxLength = 14

        textField.textProperty().addListener { _, oldValue, newValue ->
            if (newValue == null) {
                textField.text = oldValue
                return@addListener
            }

            // Extract only digits
            var value = newValue.replace(Regex("[^0-9]"), "")

            // Limit to max length
            if (value.length > maxLength) {
                value = value.substring(0, maxLength)
            }

            // Format with masks
            val formatted = when {
                value.length <= 2 -> value
                value.length <= 5 -> "${value.substring(0, 2)}.${value.substring(2)}"
                value.length <= 8 -> "${value.substring(0, 2)}.${value.substring(2, 5)}.${value.substring(5)}"
                value.length <= 12 -> "${value.substring(0, 2)}.${value.substring(2, 5)}.${value.substring(5, 8)}/${value.substring(8)}"
                else -> "${value.substring(0, 2)}.${value.substring(2, 5)}.${value.substring(5, 8)}/${value.substring(8, 12)}-${value.substring(12)}"
            }

            // Update text if it changed
            if (formatted != newValue) {
                val caretPosition = textField.caretPosition
                val oldLength = newValue.length
                textField.text = formatted

                // Adjust caret position after formatting
                if (caretPosition + (formatted.length - oldLength) > 0) {
                    textField.positionCaret(caretPosition + (formatted.length - oldLength))
                }
            }
        }

        // Block space input
        textField.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED) { event ->
            if (event.character == " ") {
                event.consume()
            }
        }
    }

    fun formatarTelefone(textField: TextField) {
        var isUpdating = false
        textField.textProperty().addListener { _, oldValue, newValue ->
            if (isUpdating || newValue == oldValue) return@addListener

            isUpdating = true
            Platform.runLater {
                try {
                    val digits = newValue.filter { it.isDigit() }.take(11)
                    val formatted = when {
                        digits.isEmpty() -> ""
                        digits.length <= 2 -> digits
                        digits.length <= 7 -> "(${digits.take(2)}) ${digits.drop(2)}"
                        else -> "(${digits.take(2)}) ${digits.slice(2..6)}-${digits.drop(7)}"
                    }

                    if (newValue != formatted) {
                        textField.text = formatted
                        textField.positionCaret(formatted.length)
                    }
                } finally {
                    isUpdating = false
                }
            }
        }
    }

    private fun createProdutosHBox(produto: Produto): HBox {
        return HBox(10.0).apply {
            styleClass.add("product-row")
            alignment = Pos.CENTER_LEFT
            val quantidadeField = createQuantidadeSection(produto)
            val valorField = createValorSection(produto)
            val subtotalField = createSubtotalSection(produto)

            children.addAll(
                Label("${produto.id}. "),
                quantidadeField,
                createProdutoSection(),
                valorField,
                subtotalField,
                createRemoveButton(this, produto)  // Always add the button
            )

            // Set the visibility of the remove button based on the number of products
            val lastChild = children.last()
            if (lastChild is Button && lastChild.styleClass.contains("remove-button")) {
                Platform.runLater {
                    lastChild.isVisible = listaProdutos.size > 1
                    lastChild.isManaged = listaProdutos.size > 1
                }
            }

            val updateSubtotal = {
                val quantidade = ((quantidadeField.children[1] as HBox).children[1] as TextField)
                val valorUnitario = (valorField.children[1] as TextField)
                val subtotal = (subtotalField.children[1] as TextField)

                val quantidadeValue = quantidade.text.toIntOrNull() ?: 1
                val valorUnitarioText = valorUnitario.text
                    .replace(Regex("[^0-9]"), "")

                val valorUnitarioValue = try {
                    valorUnitarioText.toDouble() / 100
                } catch (e: NumberFormatException) {
                    0.0
                }

                val subtotalValue = quantidadeValue * valorUnitarioValue
                val formattedValue = String.format("%,.2f", subtotalValue)
                    .replace(",", ".")
                    .replace(".", ",", ignoreCase = true)
                    .replaceFirst(",", ".")

                Platform.runLater {
                    subtotal.text = "R$ $formattedValue"
                    if (::totalLabelRef.isInitialized) {
                        updateTotal(totalLabelRef)
                    }
                }
            }

            val quantidadeTextField = ((quantidadeField.children[1] as HBox).children[1] as TextField)
            val valorTextField = (valorField.children[1] as TextField)

            quantidadeTextField.textProperty().addListener { _, _, _ ->
                updateSubtotal()
            }

            valorTextField.textProperty().addListener { _, _, _ ->
                updateSubtotal()
            }

            Platform.runLater { updateSubtotal() }
        }
    }

    fun setTotalLabel(label: Label) {
        totalLabelRef = label
    }

    private fun updateTotal(totalLabel: Label) {
        if (!::totalLabelRef.isInitialized) return
        var subtotal = 0.0

        produtosContainer.children.forEach { node ->
            val hBox = node as HBox
            val subtotalVBox =
                hBox.children.find { it is VBox && it.children[0] is Label && (it.children[0] as Label).text == "Subtotal" } as? VBox
            val subtotalField = subtotalVBox?.children?.get(1) as? TextField

            subtotalField?.let { field ->
                val subtotalText = field.text.replace(Regex("[^\\d]"), "")
                subtotal += (subtotalText.toDoubleOrNull() ?: 0.0) / 100
            }
        }

        // Add null check for valorEntregaField
        if (::valorEntregaField.isInitialized) {
            val valorEntregaText = valorEntregaField.text.replace(Regex("[^\\d]"), "")
            val valorEntrega = (valorEntregaText.toDoubleOrNull() ?: 0.0) / 100
            subtotal += valorEntrega
        }

        // Calculate discount
        var totalComDesconto = subtotal

        // Add null check for descontoField and descontoToggleGroup
        if (::descontoField.isInitialized && ::descontoToggleGroup.isInitialized) {
            val descontoText = descontoField.text.replace(Regex("[^\\d]"), "")
            if (descontoText.isNotEmpty()) {
                val descontoValue = descontoText.toDouble() / 100
                val isValor = (descontoToggleGroup.selectedToggle as? RadioButton)?.id == "valor"

                totalComDesconto = if (isValor) {
                    subtotal - descontoValue
                } else {
                    val percentual = descontoValue
                    if (percentual <= 100) {
                        subtotal * (1 - (percentual / 100))
                    } else {
                        subtotal
                    }
                }
            }
        }

        val formattedValue = String.format("%,.2f", totalComDesconto)
            .replace(",", ".")
            .replace(".", ",", ignoreCase = true)
            .replaceFirst(",", ".")

        Platform.runLater {
            totalLabel.text = "R$ $formattedValue"
        }
    }

    fun getDescontoFieldText(): String {
        return descontoField.text
    }

    fun buscarClientePorTelefone(telefone: String, callback: (Map<String, String>?) -> Unit) {
        val telefoneClean = telefone.replace(Regex("[^0-9]"), "")

        if (telefoneClean.length < 8) return callback(null)

        println("Buscando cliente por telefone: $telefoneClean")

        Thread {
            try {
                var clienteInfo: Map<String, String>? = null

                DatabaseHelper().getConnection().use { conn ->
                    // Get all possible formats of the phone number
                    val formatosNumero = listOf(
                        telefoneClean,                                           // 42998222385
                        "($telefoneClean)",                                      // (42998222385)
                        telefoneClean.replaceFirst("(\\d{2})(\\d+)".toRegex(), "($1) $2"),  // (42) 998222385
                        telefoneClean.replaceFirst("(\\d{2})(\\d{5})(\\d+)".toRegex(), "($1) $2-$3") // (42) 99822-2385
                    )

                    // Query with multiple format options
                    val sql = """
                    SELECT * FROM clientes 
                    WHERE telefone IN (?, ?, ?, ?) 
                    OR telefone LIKE ?
                    LIMIT 1
                """

                    val stmt = conn.prepareStatement(sql)
                    formatosNumero.forEachIndexed { index, format ->
                        stmt.setString(index + 1, format)
                    }
                    stmt.setString(5, "%${telefoneClean}%")  // Also try partial match

                    val rs = stmt.executeQuery()

                    if (rs.next()) {
                        clienteInfo = mapOf(
                            "nome" to rs.getString("nome"),
                            "sobrenome" to (rs.getString("sobrenome") ?: ""),
                            "observacao" to (rs.getString("observacao") ?: "")
                        )
                        println("Cliente encontrado: ${rs.getString("nome")} - ${rs.getString("telefone")}")
                    } else {
                        println("Nenhum cliente encontrado com telefone: $telefoneClean")
                        // Debug: List all clients to verify data
                        val allClientsStmt = conn.prepareStatement("SELECT id, nome, telefone FROM clientes")
                        val clientsRs = allClientsStmt.executeQuery()
                        println("Clientes disponíveis:")
                        while (clientsRs.next()) {
                            println("ID: ${clientsRs.getInt("id")}, Nome: ${clientsRs.getString("nome")}, Telefone: ${clientsRs.getString("telefone")}")
                        }
                    }
                }

                Platform.runLater { callback(clienteInfo) }
            } catch (e: Exception) {
                e.printStackTrace()
                Platform.runLater { callback(null) }
            }
        }.start()
    }

    fun getDescontoToggleGroup(): ToggleGroup {
        return descontoToggleGroup
    }

    fun setupListeners() {
        valorEntregaField.textProperty().addListener { _, _, _ ->
            updateTotal(totalLabelRef)
            calcularTroco()
        }

        descontoField.textProperty().addListener { _, _, _ ->
            updateTotal(totalLabelRef)
            calcularTroco()
        }

        trocoParaField.textProperty().addListener { _, _, _ ->
            calcularTroco()
        }

        totalLabelRef.textProperty().addListener { _, _, _ ->
            calcularTroco()
        }
    }

    private fun createRemoveButton(produtoHBox: HBox, produto: Produto): Button {
        return Button().apply {
            styleClass.add("remove-button")
            graphic =
                ImageView(Image(PedidoWizardController::class.java.getResourceAsStream("/icons/closered.png"))).apply {
                    fitHeight = 15.0
                    fitWidth = 15.0
                    isPreserveRatio = true
                }
            prefWidth = 25.0
            prefHeight = 25.0
            translateY = 15.0
            setOnAction {
                // Only remove if there's more than one product
                if (produtosContainer.children.size > 1) {
                    produtosContainer.children.remove(produtoHBox)
                    listaProdutos.remove(produto)
                    atualizarNumeracao()
                    updateRemoveButtonsVisibility()  // Call this instead of atualizarBotoesRemover()
                }
            }
        }
    }

    private fun atualizarNumeracao() {
        produtosContainer.children.forEachIndexed { index, node ->
            val produtoHBox = node as HBox
            val labelNumero = produtoHBox.children.first() as Label
            labelNumero.text = "${index + 1}. "
        }

        val updatedProducts = listaProdutos.mapIndexed { index, produto ->
            produto.copy(id = (index + 1).toLong())
        }
        listaProdutos.clear()
        listaProdutos.addAll(updatedProducts)
    }

    fun setValorEntregaTotal(valor: Double) {
        valorEntregaTotal = valor
        calculateTotal() // Recalculate the total when the delivery value changes
    }

    fun setValorEntregaField(field: TextField) {
        valorEntregaField = field
    }

    fun addNovoProduto() {
        val produtosContainer = getProdutosContainer()

        updateProductNumbers()
        val novoProduto = Produto(
            id = (listaProdutos.size + 1).toLong(),
            codigo = "",
            nome = "Produto ${listaProdutos.size + 1}",
            descricao = null.toString(),
            valorUnitario = 0.0,
            categoria = null.toString(),
            unidadeMedida = "UN",
            estoqueMinimo = 0,
            estoqueAtual = 0,
            status = "Ativo",
            dataCadastro = null.toString(),
            dataAtualizacao = null.toString()
        )
        listaProdutos.add(novoProduto)
        produtosContainer.children.add(createProdutosHBox(novoProduto))
        updateRemoveButtonsVisibility()

        // Add this line to ensure the total is recalculated immediately
        Platform.runLater {
            calculateTotal()
        }
    }
    private fun atualizarBotoesRemover() {
        updateRemoveButtonsVisibility()
    }


    fun getProdutosContainer(): VBox {
        return produtosContainer
    }

    private fun createQuantidadeSection(produto: Produto): VBox {
        return VBox(10.0).apply {
            children.addAll(
                Label("Quantidade").apply {
                    styleClass.add("field-label")
                },
                HBox(5.0).apply {
                    alignment = Pos.CENTER_LEFT
                    val quantityBox = this
                    children.addAll(
                        createQuantityButton("menos"),
                        TextField().apply {
                            styleClass.add("quantidade-field")
                            prefWidth = 50.0
                            maxWidth = 50.0
                            text = "1"
                            alignment = Pos.CENTER
                            textProperty().addListener { _, _, newValue ->
                                validateQuantity(this, newValue)
                            }
                        },
                        createQuantityButton("mais")
                    )
                }
            )
        }
    }

    private fun createValorSection(produto: Produto): VBox {
        return VBox(10.0).apply {
            children.addAll(
                Label("Valor Unitário").apply {
                    styleClass.add("field-label")
                },
                TextField().apply {
                    styleClass.add("text-field")
                    prefWidth = 95.0
                    maxWidth = 95.0
                    text = "R$ ${String.format("%.2f", produto.valorUnitario).replace(".", ",")}"
                    alignment = Pos.CENTER_RIGHT
                    formatarMoeda(this)
                }
            )
        }
    }

    private fun createSubtotalSection(produto: Produto): VBox {
        return VBox(10.0).apply {
            children.addAll(
                Label("Subtotal").apply {
                    styleClass.add("field-label")
                },
                TextField().apply {
                    styleClass.addAll("text-field")
                    prefWidth = 100.0
                    maxWidth = 100.0
                    alignment = Pos.CENTER_RIGHT
                    isEditable = false
                    text = "R$ 0,00"
                    style = """
                    -fx-background-color: rgb(250, 251, 252);
                    -fx-border-color: rgb(223, 225, 230);
                    -fx-border-width: 2;
                    -fx-border-radius: 3;
                    -fx-background-radius: 3;
                    -fx-focus-color: transparent;
                    -fx-faint-focus-color: transparent;
                """
                    focusTraversableProperty().set(false)
                    formatarMoeda(this)
                }
            )
        }
    }

    private fun createProdutoSection(): VBox {
        val productField = TextField().apply {
            styleClass.add("text-field")
            maxWidth = Double.POSITIVE_INFINITY
            HBox.setHgrow(this, Priority.ALWAYS)

            val suggestionsMenu = ContextMenu().apply {
                styleClass.add("product-suggestions")
                style = """
            -fx-background-color: white;
            -fx-border-color: rgb(223, 225, 230);
            -fx-border-width: 1px;
            -fx-border-radius: 3px;
            -fx-background-radius: 3px;
            -fx-effect: dropshadow(gaussian, rgba(9, 30, 66, 0.15), 8, 0, 0, 2);
            -fx-padding: 4px;
        """
            }

            textProperty().addListener { _, _, newValue ->
                if (newValue.length >= 2) {
                    val produtos = searchProducts(newValue)
                    suggestionsMenu.items.clear()

                    if (produtos.isNotEmpty()) {
                        // Add suggestions to menu with hover effects
                        val maxToShow = 6
                        val limitedProdutos = if (produtos.size > maxToShow) produtos.take(maxToShow) else produtos

                        limitedProdutos.forEach { produto ->
                            val menuItem = MenuItem(produto.nome)
                            menuItem.style = """
                        -fx-padding: 5px 8px;
                        -fx-font-size: 13px;
                        -fx-pref-width: ${this.width - 20}px; 
                    """

                            menuItem.setOnAction {
                                checkInsumosSuficientes(produto.id, 1) { hasSuficientes, insumosEmFalta ->
                                    if (!hasSuficientes) {
                                        val dialog = Dialog<ButtonType>()
                                        dialog.title = "Estoque insuficiente"
                                        dialog.headerText = "Falta de insumos para o produto ${produto.nome}"
                                        dialog.initStyle(StageStyle.UNDECORATED)

                                        val buttonTypeProceed = ButtonType("Adicionar mesmo assim", ButtonBar.ButtonData.YES)
                                        val buttonTypeCancel = ButtonType("Cancelar", ButtonBar.ButtonData.NO)
                                        dialog.dialogPane.buttonTypes.addAll(buttonTypeProceed, buttonTypeCancel)
                                        dialog.dialogPane.stylesheets.add(javaClass.getResource("/pedidowizardview.css").toExternalForm())

                                        dialog.dialogPane.stylesheets.addAll(this.stylesheets)

                                        val contentText = StringBuilder("Os seguintes insumos não possuem estoque suficiente:\n\n")
                                        insumosEmFalta.forEach { insumo ->
                                            contentText.append("- ${insumo.nome}: ")
                                            contentText.append("Disponível: ${insumo.estoqueAtual}, ")
                                            contentText.append("Necessário: ${insumo.necessario}\n")
                                        }
                                        contentText.append("\nDeseja adicionar o produto mesmo assim?")

                                        val content = VBox(10.0).apply {
                                            padding = Insets(20.0)
                                            children.add(Label(contentText.toString()).apply {
                                                style = "-fx-font-size: 14px;"
                                                isWrapText = true
                                                maxWidth = 400.0
                                            })
                                        }

                                        dialog.dialogPane.style = """
                                        -fx-background-color: white;
                                        -fx-border-color: #D3D3D3;
                                        -fx-border-width: 1px;
                                    """

                                        dialog.dialogPane.content = content

                                        dialog.dialogPane.lookup(".header-panel")?.style = """
                                        -fx-background-color: #FFA500;
                                        -fx-background-radius: 0;
                                    """

                                        val headerLabel = dialog.dialogPane.lookup(".header-panel .label") as? Label
                                        headerLabel?.style = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;"

                                        // Apply warning style to the proceed button
                                        val proceedButton = dialog.dialogPane.lookupButton(buttonTypeProceed)
                                        proceedButton.styleClass.add("primary-button")  // Instead of "warning-button"

                                        val cancelButton = dialog.dialogPane.lookupButton(buttonTypeCancel)
                                        cancelButton.styleClass.add("secondary-button")

                                        // Configure button bar
                                        val buttonBar = dialog.dialogPane.lookup(".button-bar") as ButtonBar
                                        buttonBar.apply {
                                            buttonOrder = ButtonBar.BUTTON_ORDER_NONE
                                            buttonMinWidth = 100.0
                                            style = """
                                        -fx-background-color: white;
                                        -fx-alignment: center;
                                        """
                                            padding = Insets(0.0, 0.0, 15.0, 0.0)
                                        }

                                        val result = dialog.showAndWait()
                                        if (result.isPresent && result.get() == buttonTypeProceed) {
                                            setProdutoFields(produto, this)
                                        }
                                    } else {
                                        setProdutoFields(produto, this)
                                    }
                                    suggestionsMenu.hide()
                                }
                            }

                            suggestionsMenu.items.add(menuItem)
                        }

                        // Show menu before setting width to ensure proper sizing
                        suggestionsMenu.show(this, Side.BOTTOM, 0.0, 0.0)

                        // Force the menu width to exactly match the field width
                        Platform.runLater {
                            suggestionsMenu.prefWidth = this.width
                            suggestionsMenu.minWidth = this.width
                            suggestionsMenu.maxWidth = this.width
                        }
                    } else {
                        suggestionsMenu.hide()
                    }
                } else {
                    suggestionsMenu.hide()
                }
            }

            // Enhanced key event handling
            setOnKeyPressed { event ->
                when (event.code) {
                    KeyCode.ESCAPE -> {
                        suggestionsMenu.hide()
                    }
                    KeyCode.SPACE -> {
                        if (suggestionsMenu.isShowing) {
                            event.consume()
                        }
                    }
                    else -> {
                    }
                }
            }

            suggestionsMenu.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED) { event ->
                if (event.code == KeyCode.SPACE) {
                    event.consume()
                }
            }

            focusedProperty().addListener { _, _, focused ->
                if (!focused) {
                    Platform.runLater { suggestionsMenu.hide() }
                }
            }
        }

        return VBox(10.0).apply {
            HBox.setHgrow(this, Priority.ALWAYS)
            children.addAll(
                Label("Produto").apply { styleClass.add("field-label") },
                productField
            )
        }
    }

    // Helper method to set product fields
    private fun setProdutoFields(produto: Produto, textField: TextField) {
        textField.text = produto.nome
        // Get the parent product row
        val produtoHBox = textField.parent?.parent as? HBox
        // Get the valor field in the same row
        val valorField = ((produtoHBox?.children?.get(3) as? VBox)?.children?.get(1) as? TextField)
        valorField?.text = "R$ ${String.format("%.2f", produto.valorUnitario).replace('.', ',')}"
    }

    // Data class to hold insumo information for missing ingredients
    data class InsumoEmFalta(val nome: String, val estoqueAtual: Int, val necessario: Double)

    // Method to check if there are enough ingredients for a product
    private fun checkInsumosSuficientes(produtoId: Long, quantidade: Int, callback: (Boolean, List<InsumoEmFalta>) -> Unit) {
        Thread {
            val insumosEmFalta = mutableListOf<InsumoEmFalta>()
            var temInsumosSuficientes = true

            try {
                DatabaseHelper().getConnection().use { conn ->
                    // Check if product has any ingredients
                    val checkInsumosSql = """
                    SELECT COUNT(*) as count FROM produto_insumos WHERE produto_id = ?
                """
                    conn.prepareStatement(checkInsumosSql).use { stmt ->
                        stmt.setLong(1, produtoId)
                        val rs = stmt.executeQuery()
                        if (rs.next() && rs.getInt("count") == 0) {
                            // Product has no ingredients, so we're good
                            Platform.runLater { callback(true, insumosEmFalta) }
                            return@Thread
                        }
                    }

                    // Get all ingredients for this product and check inventory
                    val insumosSql = """
                    SELECT pi.insumo_id, pi.quantidade, p.nome, p.estoque_atual 
                    FROM produto_insumos pi
                    JOIN produtos p ON p.id = pi.insumo_id
                    WHERE pi.produto_id = ?
                """
                    conn.prepareStatement(insumosSql).use { stmt ->
                        stmt.setLong(1, produtoId)
                        val rs = stmt.executeQuery()

                        while (rs.next()) {
                            val insumoNome = rs.getString("nome")
                            val quantidadeNecessaria = rs.getDouble("quantidade") * quantidade
                            val estoqueAtual = rs.getInt("estoque_atual")

                            if (estoqueAtual < quantidadeNecessaria) {
                                insumosEmFalta.add(InsumoEmFalta(insumoNome, estoqueAtual, quantidadeNecessaria))
                                temInsumosSuficientes = false
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // In case of error, we assume there's enough inventory
                temInsumosSuficientes = true
            }

            Platform.runLater { callback(temInsumosSuficientes, insumosEmFalta) }
        }.start()
    }

    private fun searchProducts(query: String): List<Produto> {
        val produtos = mutableListOf<Produto>()

        try {
            DatabaseHelper().getConnection().use { connection ->
                val sql = """
                    SELECT id, codigo, nome, descricao, valor_unitario, categoria,
                           unidade_medida, estoque_minimo, estoque_atual, status,
                           data_cadastro, data_atualizacao
                    FROM produtos
                    WHERE (LOWER(nome) LIKE LOWER(?) OR LOWER(codigo) LIKE LOWER(?))
                    AND status = 'Ativo'
                    LIMIT 10
                """.trimIndent()

                connection.prepareStatement(sql).use { stmt ->
                    val searchPattern = "%$query%"
                    stmt.setString(1, searchPattern)
                    stmt.setString(2, searchPattern)

                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            produtos.add(
                                Produto(
                                    id = rs.getLong("id"),
                                    codigo = rs.getString("codigo"),
                                    nome = rs.getString("nome"),
                                    descricao = rs.getString("descricao"),
                                    valorUnitario = rs.getDouble("valor_unitario"),
                                    categoria = rs.getString("categoria"),
                                    unidadeMedida = rs.getString("unidade_medida"),
                                    estoqueMinimo = rs.getInt("estoque_minimo"),
                                    estoqueAtual = rs.getInt("estoque_atual"),
                                    status = rs.getString("status"),
                                    dataCadastro = rs.getString("data_cadastro"),
                                    dataAtualizacao = rs.getString("data_atualizacao")
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }

        return produtos
    }

    private fun createQuantityButton(tipo: String): Button {
        return Button().apply {
            styleClass.addAll("quantity-button", "flat-button")
            NovoPedidoController::class.java.getResourceAsStream("/icons/${tipo}p.png")?.let {
                graphic = ImageView(Image(it)).apply {
                    fitHeight = 30.0
                    fitWidth = 30.0
                    isPreserveRatio = true
                }
            }
            prefWidth = 35.0
            prefHeight = 35.0
            setOnAction {
                val quantityField = (parent as HBox).getChildren()[1] as TextField
                if (tipo == "mais") incrementQuantity(quantityField)
                else decrementQuantity(quantityField)
            }
        }
    }

    fun formatarMoeda(textField: TextField): javafx.beans.value.ChangeListener<String> {
        var isUpdating = false
        textField.text = "R$ 0,00"

        val textListener = javafx.beans.value.ChangeListener<String> { _, _, newValue ->
            if (isUpdating) return@ChangeListener

            isUpdating = true
            Platform.runLater {
                try {
                    val digits = newValue.replace(Regex("[^\\d]"), "")

                    if (digits.isEmpty()) {
                        textField.text = "R$ 0,00"
                    } else {
                        val value = digits.toDouble() / 100
                        val formattedValue = String.format("%,.2f", value)
                            .replace(",", ".")
                            .replace(".", ",", ignoreCase = true)
                            .replaceFirst(",", ".")
                        textField.text = "R$ $formattedValue"
                    }
                    textField.positionCaret(textField.text.length)
                } finally {
                    isUpdating = false
                }
            }
        }

        textField.textProperty().addListener(textListener)

        textField.focusedProperty().addListener { _, _, isFocused ->
            if (isFocused) {
                Platform.runLater {
                    textField.positionCaret(textField.text.length)
                }
            }
        }

        textField.caretPositionProperty().addListener { _, _, _ ->
            Platform.runLater {
                textField.positionCaret(textField.text.length)
            }
        }

        return textListener
    }

    fun setDescontoField(field: TextField) {
        descontoField = field

        // Add change listener to update total when discount changes
        field.textProperty().addListener { _, _, _ ->
            aplicarDesconto()
        }
    }

    fun setDescontoToggleGroup(group: ToggleGroup) {
        descontoToggleGroup = group

        // Add listener that safely checks if descontoField is initialized
        group.selectedToggleProperty().addListener { _, _, newToggle ->
            if (::descontoField.isInitialized) {
                val isValor = (newToggle as? RadioButton)?.id == "valor"
                descontoField.text = ""
                if (isValor) {
                    descontoField.promptText = "R$ 0,00"
                } else {
                    descontoField.promptText = "0,00"
                }
                tipoDesconto.set(if (isValor) "valor" else "percentual")
            }
        }
    }

    fun setTrocoParaField(field: TextField) {
        trocoParaField = field
    }

    fun setTrocoCalculadoLabel(label: Label) {
        trocoCalculadoLabel = label
    }

    fun calcularTroco() {
        // Add guards for all lateinit properties
        if (!::trocoParaField.isInitialized || !::totalLabelRef.isInitialized || !::trocoCalculadoLabel.isInitialized) return

        val trocoParaText = trocoParaField.text.replace(Regex("[^\\d]"), "")
        val trocoPara = (trocoParaText.toDoubleOrNull() ?: 0.0) / 100

        val totalText = totalLabelRef.text.replace(Regex("[^\\d]"), "")
        val total = (totalText.toDoubleOrNull() ?: 0.0) / 100

        val troco = if (trocoPara >= total) trocoPara - total else 0.0

        val formattedTroco = String.format("%,.2f", troco)
            .replace(",", ".")
            .replace(".", ",", ignoreCase = true)
            .replaceFirst(",", ".")

        Platform.runLater {
            trocoCalculadoLabel.text = "R$ $formattedTroco"
        }
    }

    fun formatarPercentual(textField: TextField): javafx.beans.value.ChangeListener<String> {
        var isUpdating = false
        textField.text = "0,00"

        val textListener = javafx.beans.value.ChangeListener<String> { _, _, newValue ->
            if (isUpdating) return@ChangeListener

            isUpdating = true
            Platform.runLater {
                try {
                    val digits = newValue.replace(Regex("[^\\d]"), "")

                    if (digits.isEmpty()) {
                        textField.text = "0,00"
                    } else {
                        val value = digits.toDouble() / 100
                        if (value > 99.99) {
                            textField.text = "99,99"
                        } else {
                            val formatted = String.format("%.2f", value)
                                .replace(",", ".")
                                .replace(".", ",")
                            if (textField.text != formatted) {
                                textField.text = formatted
                            }
                        }
                    }
                    textField.positionCaret(textField.text.length)
                } finally {
                    isUpdating = false
                }
            }
        }

        textField.textProperty().addListener(textListener)

        textField.focusedProperty().addListener { _, _, isFocused ->
            if (isFocused) {
                Platform.runLater {
                    textField.positionCaret(textField.text.length)
                }
            }
        }

        return textListener
    }

    fun updateProductNumbers() {
        val container = getProdutosContainer()

        // For each product row, update the number label
        for (i in 0 until container.children.size) {
            val productRow = container.children[i] as? HBox
            productRow?.let { row ->
                // Find the product number label (first child in the HBox)
                val numberLabel = row.children.first() as? Label
                numberLabel?.text = "${i + 1}"
            }
        }
    }

    fun removeProduct(productRow: HBox) {
        // Remove the row from its parent container
        val container = productRow.parent as VBox
        container.children.remove(productRow)

        // Renumber remaining product rows
        container.children.forEachIndexed { index, child ->
            if (child is HBox) {
                // Update the product number label (first child of the HBox)
                val numberLabel = child.children[0] as Label
                numberLabel.text = "${index + 1}"
            }
        }

        // Recalculate totals
        calculateTotal()
    }

    fun formatarCep(textField: TextField) {
        val maxLength = 8

        textField.textProperty().addListener { _, oldValue, newValue ->
            if (newValue == null) {
                textField.text = oldValue
                return@addListener
            }

            var value = newValue.replace(Regex("[^0-9]"), "")

            if (value.length > maxLength) {
                value = value.substring(0, maxLength)
            }

            if (value.length > 5) {
                value = value.substring(0, 5) + "-" + value.substring(5)
            }

            if (value != newValue) {
                textField.text = value
                textField.positionCaret(textField.text.length)
            }
        }
    }

    fun resetForm() {
        // Reset all text fields in the client section
        contentContainer.lookupAll(".text-field").forEach { node ->
            if (node is TextField) {
                node.text = if (node.promptText?.contains("R$") == true) "R$ 0,00" else ""
            }
        }

        // Reset date pickers and time pickers
        contentContainer.lookupAll(".date-picker").forEach { node ->
            if (node is DatePicker) {
                // Force a visual update by temporarily setting to null
                Platform.runLater {
                    val today = java.time.LocalDate.now()
                    node.value = null
                    node.value = today
                }
            }
        }

        contentContainer.lookupAll(".time-picker").forEach { node ->
            if (node is ComboBox<*>) {
                // Reset hours to 08 and minutes to 00 based on parent component
                val parent = node.parent?.parent as? VBox
                val label = parent?.children?.firstOrNull { it is Label } as? Label
                val isHourField =
                    label?.text?.contains("Hora") == true && node == (node.parent as HBox).children.first()
                Platform.runLater {
                    node.value = null
                    node.value = if (isHourField) "08" else "00"
                }
            }
        }

        // Reset payment method buttons
        contentContainer.lookupAll(".payment-toggle-button").forEach { node ->
            if (node is ToggleButton) {
                node.isSelected = node.text == "Dinheiro"
            }
        }

        // Reset status buttons
        contentContainer.lookupAll(".payment-toggle-button").forEach { node ->
            if (node is ToggleButton && (node.text == "Pendente" || node.text == "Pago")) {
                node.isSelected = node.text == "Pendente"
            }
        }

        descontoToggleGroup.selectToggle(
            descontoToggleGroup.toggles.find { (it as RadioButton).id == "valor" }
        )

        // Fix: Look for the delivery switch directly in contentContainer instead of a non-existent deliveryContainer
        val entregaSwitch = contentContainer.lookup(".switch") as? StackPane
        entregaSwitch?.let {
            val checkbox = it.children.find { node -> node is CheckBox } as? CheckBox
            checkbox?.isSelected = false
        }

        produtosContainer.children.clear()
        listaProdutos.clear()
        addNovoProduto()

        totalLabelRef.text = "R$ 0,00"

        trocoParaField.text = "R$ 0,00"
        trocoCalculadoLabel.text = "R$ 0,00"

        valorEntregaField.text = "R$ 0,00"

        descontoField.text = if ((descontoToggleGroup.selectedToggle as? RadioButton)?.id == "valor")
            "R$ 0,00" else "0,00"
    }

    fun parseMoneyValue(text: String): Double {
        // Remove non-numeric characters except for decimal separator
        var cleanValue = text.replace(Regex("[R$\\s]"), "")

        // Replace dot used as thousand separator
        cleanValue = cleanValue.replace(".", "")

        // Replace comma with dot for decimal parsing
        cleanValue = cleanValue.replace(",", ".")

        return try {
            if (!text.contains(",")) {
                // If no comma was present, treat as cents
                cleanValue.toDouble() / 100
            } else {
                cleanValue.toDouble()
            }
        } catch (e: NumberFormatException) {
            0.0
        }
    }

    private fun atualizarEstoqueInsumos(connection: Connection, produtoNome: String, quantidade: Int) {
        try {
            // Find the product ID by name
            val findProdutoSql = "SELECT id FROM produtos WHERE nome = ?"
            connection.prepareStatement(findProdutoSql).use { stmt ->
                stmt.setString(1, produtoNome)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    val produtoId = rs.getLong("id")

                    // Get insumos for this product
                    val insumosSql = """
                    SELECT pi.insumo_id, pi.quantidade, p.estoque_atual 
                    FROM produto_insumos pi
                    JOIN produtos p ON p.id = pi.insumo_id
                    WHERE pi.produto_id = ?
                """
                    connection.prepareStatement(insumosSql).use { insumoStmt ->
                        insumoStmt.setLong(1, produtoId)
                        val insumosRs = insumoStmt.executeQuery()

                        while (insumosRs.next()) {
                            val insumoId = insumosRs.getLong("insumo_id")
                            val quantidadePorProduto = insumosRs.getDouble("quantidade")
                            val estoqueAtual = insumosRs.getInt("estoque_atual")

                            // Calculate total quantity to reduce
                            val quantidadeTotal = quantidadePorProduto * quantidade
                            val novoEstoque = Math.max(0, estoqueAtual - quantidadeTotal.toInt())

                            // Update insumo inventory
                            val updateSql = "UPDATE produtos SET estoque_atual = ? WHERE id = ?"
                            connection.prepareStatement(updateSql).use { updateStmt ->
                                updateStmt.setInt(1, novoEstoque)
                                updateStmt.setLong(2, insumoId)
                                updateStmt.executeUpdate()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun findProdutoIdByName(connection: Connection, nomeProduto: String): Long? {
        connection.prepareStatement("SELECT id FROM produtos WHERE nome = ?").use { stmt ->
            stmt.setString(1, nomeProduto)
            val resultSet = stmt.executeQuery()
            if (resultSet.next()) {
                return resultSet.getLong("id")
            }
        }
        return null
    }

    private fun atualizarEstoqueInsumos(connection: Connection, produtoId: Long, quantidade: Int) {
        // Get all ingredients for this product
        connection.prepareStatement("""
        SELECT pi.insumo_id, pi.quantidade 
        FROM produto_insumos pi
        WHERE pi.produto_id = ?
    """).use { stmt ->
            stmt.setLong(1, produtoId)
            val rs = stmt.executeQuery()

            while (rs.next()) {
                val insumoId = rs.getLong("insumo_id")
                val qtdPorProduto = rs.getDouble("quantidade")
                val qtdTotal = qtdPorProduto * quantidade

                // Update insumo inventory
                connection.prepareStatement(
                    "UPDATE produtos SET estoque_atual = estoque_atual - ? WHERE id = ?"
                ).use { updateStmt ->
                    updateStmt.setDouble(1, qtdTotal)
                    updateStmt.setLong(2, insumoId)
                    updateStmt.executeUpdate()
                }
            }
        }
    }

    private fun atualizarEstoqueProduto(connection: Connection, produtoId: Long, quantidade: Int) {
        val stmtSelect = connection.prepareStatement("SELECT estoque_atual, nome FROM produtos WHERE id = ?")
        stmtSelect.setLong(1, produtoId)
        val rs = stmtSelect.executeQuery()

        if (rs.next()) {
            val estoqueAnterior = rs.getInt("estoque_atual")
            val nomeProduto = rs.getString("nome")
            val estoqueNovo = estoqueAnterior - quantidade

            val stmtUpdate = connection.prepareStatement(
                "UPDATE produtos SET estoque_atual = ? WHERE id = ?"
            )
            stmtUpdate.setInt(1, estoqueNovo)
            stmtUpdate.setLong(2, produtoId)
            stmtUpdate.executeUpdate()

            val stmtMovimento = connection.prepareStatement("""
            INSERT INTO movimentacao_estoque (
                produto_id, quantidade_anterior, quantidade_nova, 
                quantidade_movimentada, tipo_movimentacao, motivo, usuario
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """)
            stmtMovimento.setLong(1, produtoId)
            stmtMovimento.setInt(2, estoqueAnterior)
            stmtMovimento.setInt(3, estoqueNovo)
            stmtMovimento.setInt(4, quantidade)
            stmtMovimento.setString(5, "saida")
            stmtMovimento.setString(6, "Venda - Pedido")
            stmtMovimento.setString(7, "Sistema")
            stmtMovimento.executeUpdate()
        }
    }

    private fun buscarClienteIdPorTelefone(connection: Connection, telefone: String): Long? {
        if (telefone.isBlank()) return null

        return connection.prepareStatement("SELECT id FROM clientes WHERE telefone = ?").use { stmt ->
            stmt.setString(1, telefone)
            val rs = stmt.executeQuery()
            if (rs.next()) rs.getLong("id") else null
        }
    }

    fun salvarPedido(
        clienteInfo: List<Pair<String, String>>,
        pagamentoInfo: List<Pair<String, String>>,
        entregaInfo: List<Pair<String, String>>
    ): Boolean {
        try {
            val telefoneContato = clienteInfo.find { it.first == "Telefone" }?.second ?: ""
            val observacao = clienteInfo.find { it.first == "Observação" }?.second
            val status = pagamentoInfo.find { it.first == "Status" }?.second ?: "Pendente"

            val valorTotal = parseMoneyValue(pagamentoInfo.find { it.first == "Total do Pedido" }?.second ?: "0,00")

            val descontoPair = pagamentoInfo.find { it.first.startsWith("Desconto") }
            val tipoDesconto = if ((descontoToggleGroup.selectedToggle as? RadioButton)?.id == "valor")
                "valor" else "percentual"
            val valorDescontoStr = descontoPair?.second?.trim() ?: "0,00"
            val valorDesconto = if (tipoDesconto == "valor") {
                parseMoneyValue(descontoField.text)
            } else {
                // For percentual type
                val percentualText = descontoField.text.replace("%", "").replace(",", ".")
                val percentual = percentualText.toDoubleOrNull() ?: 0.0
                (valorTotal * percentual / 100)
            }
            println("Saving discount: $valorDesconto of type $tipoDesconto")
            val formaPagamento = pagamentoInfo.find { it.first == "Forma de Pagamento" }?.second
            val valorTrocoPara = parseMoneyValue(trocoParaField.text)
            val valorTroco = parseMoneyValue(trocoCalculadoLabel.text)

            DatabaseHelper().getConnection().use { connection ->
                connection.autoCommit = false
                try {
                    var numeroGerado = ""
                    connection.createStatement().use { stmt ->
                        val rs =
                            stmt.executeQuery("SELECT COALESCE(MAX(CAST(SUBSTR(numero, 4) AS INTEGER)), 0) + 1 as next_num FROM pedidos")
                        if (rs.next()) {
                            numeroGerado = "PED%04d".format(rs.getInt("next_num"))
                        }
                    }
// Change from val to var clienteId
                    var clienteId = buscarClienteIdPorTelefone(connection, telefoneContato)
                    if (clienteId == null && telefoneContato.isNotBlank()) {
                        // Extract name and sobrenome
                        val nome = clienteInfo.find { it.first == "Nome" }?.second?.split(" ")?.firstOrNull() ?: ""
                        val sobrenome = clienteInfo.find { it.first == "Nome" }?.second?.split(" ")?.drop(1)?.joinToString(" ") ?: ""

                        // Inserir o cliente novo
                        connection.prepareStatement(
                            "INSERT INTO clientes (nome, sobrenome, telefone) VALUES (?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS
                        ).use { stmt ->
                            stmt.setString(1, nome)
                            stmt.setString(2, sobrenome)
                            stmt.setString(3, telefoneContato)
                            stmt.executeUpdate()

                            stmt.generatedKeys.use { keys ->
                                if (keys.next()) {
                                    clienteId = keys.getLong(1)
                                }
                            }
                        }
                    }
                    val pedidoQuery = """
                        INSERT INTO pedidos (numero, cliente_id, telefone_contato, observacao, status,
                        valor_total, valor_desconto, tipo_desconto, forma_pagamento, valor_troco_para, valor_troco,
                        data_retirada, hora_retirada)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()

                    val pedidoId =
                        connection.prepareStatement(pedidoQuery, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                            stmt.setString(1, numeroGerado)
                            stmt.setObject(2, clienteId)
                            stmt.setString(3, telefoneContato)
                            stmt.setString(4, observacao)
                            stmt.setString(5, status)
                            stmt.setDouble(6, valorTotal)
                            stmt.setDouble(7, valorDesconto)
                            stmt.setString(8, tipoDesconto)
                            stmt.setString(9, formaPagamento)
                            stmt.setDouble(10, valorTrocoPara)
                            stmt.setDouble(11, valorTroco)

                            // Se houver entrega, define data e hora de retirada como "Entrega"
                            if (entregaInfo.first().second == "Sim") {
                                stmt.setString(12, "Entrega")
                                stmt.setString(13, "Entrega")
                            } else {
                                stmt.setString(12, pagamentoInfo.find { it.first == "Data de Retirada" }?.second)
                                stmt.setString(13, pagamentoInfo.find { it.first == "Hora de Retirada" }?.second)
                            }

                            stmt.executeUpdate()
                            stmt.generatedKeys.use { keys ->
                                if (keys.next()) keys.getLong(1) else throw SQLException("Failed to get pedido ID")
                            }
                        }

                    val itemQuery = """
                INSERT INTO itens_pedido (pedido_id, produto_id, nome_produto, quantidade, valor_unitario, subtotal)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()

                    produtosContainer.children.forEach { node ->
                        val hBox = node as HBox
                        val qtdField = ((hBox.children[1] as VBox).children[1] as HBox).children[1] as TextField
                        val produtoField = ((hBox.children[2] as VBox).children[1] as TextField)
                        val valorField = ((hBox.children[3] as VBox).children[1] as TextField)
                        val subtotalField = ((hBox.children[4] as VBox).children[1] as TextField)

                        val quantidade = qtdField.text.toInt()
                        val nomeProduto = produtoField.text
                        val valorUnitario = parseMoneyValue(valorField.text)
                        val subtotal = parseMoneyValue(subtotalField.text)

                        val produtoId = findProdutoIdByName(connection, nomeProduto)

                        connection.prepareStatement(itemQuery).use { stmt ->
                            stmt.setLong(1, pedidoId)
                            stmt.setObject(2, produtoId)
                            stmt.setString(3, nomeProduto)
                            stmt.setInt(4, quantidade)
                            stmt.setDouble(5, valorUnitario)
                            stmt.setDouble(6, subtotal)
                            stmt.executeUpdate()
                        }

                        // If we found a valid product ID, update inventory
                        if (produtoId != null) {
                            // Update insumos inventory
                            atualizarEstoqueInsumos(connection, produtoId, quantidade)

                            // Also update the product's own inventory
                            atualizarEstoqueProduto(connection, produtoId, quantidade)
                        }
                    }

                    if (entregaInfo.first().second == "Sim") {
                        val entregaQuery = """
                        INSERT INTO entregas (pedido_id, nome_destinatario, telefone_destinatario,
                        endereco, numero, referencia, cidade, bairro, cep, valor_entrega, data_entrega, hora_entrega)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()

                        connection.prepareStatement(entregaQuery).use { stmt ->
                            stmt.setLong(1, pedidoId)
                            stmt.setString(2, formatarTextoCapitalizado(entregaInfo.find { it.first == "Nome" }?.second ?: ""))
                            stmt.setString(3, entregaInfo.find { it.first == "Telefone" }?.second)
                            stmt.setString(4, formatarTextoCapitalizado(entregaInfo.find { it.first == "Endereço" }?.second ?: ""))
                            stmt.setString(5, entregaInfo.find { it.first == "Número" }?.second)
                            stmt.setString(6, formatarTextoCapitalizado(entregaInfo.find { it.first == "Referência" }?.second ?: ""))
                            stmt.setString(7, formatarTextoCapitalizado(entregaInfo.find { it.first == "Cidade" }?.second ?: ""))
                            stmt.setString(8, formatarTextoCapitalizado(entregaInfo.find { it.first == "Bairro" }?.second ?: ""))
                            stmt.setString(9, entregaInfo.find { it.first == "CEP" }?.second)
                            stmt.setDouble(
                                10,
                                parseMoneyValue(entregaInfo.find { it.first == "Valor" }?.second ?: "0,00")
                            )
                            stmt.setString(11, entregaInfo.find { it.first == "Data" }?.second)
                            stmt.setString(12, entregaInfo.find { it.first == "Hora" }?.second)
                            stmt.executeUpdate()
                        }
                    }

                    connection.commit()
                    println("Pedido saved successfully. Starting print process...")
                    val printerController = PrinterController()

                    // Create products list for printing
                    val produtosList = produtosContainer.children.map { node ->
                        val hBox = node as HBox
                        val qtdField = ((hBox.children[1] as VBox).children[1] as HBox).children[1] as TextField
                        val produtoField = ((hBox.children[2] as VBox).children[1] as TextField)
                        val valorField = ((hBox.children[3] as VBox).children[1] as TextField)
                        val subtotalField = ((hBox.children[4] as VBox).children[1] as TextField)

                        mapOf(
                            "quantidade" to qtdField.text.toInt(),
                            "nome_produto" to produtoField.text,
                            "valor_unitario" to parseMoneyValue(valorField.text),
                            "subtotal" to parseMoneyValue(subtotalField.text)
                        )
                    }

                    // Create pedidoData map with all required information
                    val pedidoData = mutableMapOf<String, Any>()

                    // Basic order info
                    pedidoData["numero"] = numeroGerado
                    pedidoData["observacao"] = observacao ?: ""
                    pedidoData["telefone_contato"] = telefoneContato
                    pedidoData["data_pedido"] = java.time.LocalDate.now().toString()
                    pedidoData["status"] = status
                    pedidoData["status_pedido"] = "Pendente"

                    val clienteMap = mutableMapOf<String, Any>()

                    val clienteInfoList = clienteInfo

                    if (clienteInfoList.isNotEmpty()) {
                        clienteInfoList.forEach { (key, value) ->
                            when(key) {
                                "Nome" -> clienteMap["nome"] = value
                                "Telefone" -> pedidoData["telefone_contato"] = value
                                "Observação" -> pedidoData["observacao"] = value
                            }
                        }
                    } else {
                        pedidoData["telefone_contato"] = pedidoData["telefone_contato"] ?:
                                telefoneContato // Replace pedido["telefone_contato"]

                        clienteMap["nome"] = "Cliente"
                    }

                    pedidoData["cliente"] = clienteMap

                    // Payment info
                    pedidoData["valor_total"] = valorTotal
                    pedidoData["valor_desconto"] = valorDesconto
                    pedidoData["tipo_desconto"] = tipoDesconto
                    pedidoData["forma_pagamento"] = formaPagamento ?: ""
                    pedidoData["valor_troco_para"] = valorTrocoPara
                    pedidoData["valor_troco"] = valorTroco

                    pedidoData["itens"] = produtosList

                    if (entregaInfo.isNotEmpty() && entregaInfo.first().second == "Sim") {
                        val entregaMap = mutableMapOf<String, Any>()
                        entregaInfo.forEach { (key, value) ->
                            when(key) {
                                "Nome" -> entregaMap["nome_destinatario"] = formatarTextoCapitalizado(value)
                                "Telefone" -> entregaMap["telefone_destinatario"] = value
                                "Endereço" -> entregaMap["endereco"] = formatarTextoCapitalizado(value)
                                "Número" -> entregaMap["numero"] = value
                                "Referência" -> entregaMap["referencia"] = formatarTextoCapitalizado(value)
                                "Cidade" -> entregaMap["cidade"] = formatarTextoCapitalizado(value)
                                "Bairro" -> entregaMap["bairro"] = formatarTextoCapitalizado(value)
                                "CEP" -> entregaMap["cep"] = value
                                "Valor" -> entregaMap["valor_entrega"] = parseMoneyValue(value)
                                "Data" -> entregaMap["data_entrega"] = value
                                "Hora" -> entregaMap["hora_entrega"] = value
                            }
                        }
                        pedidoData["entrega"] = entregaMap
                    } else {
                        val dataRetirada = pagamentoInfo.find { it.first == "Data de Retirada" }?.second
                        val horaRetirada = pagamentoInfo.find { it.first == "Hora de Retirada" }?.second
                        if (dataRetirada != null) pedidoData["data_retirada"] = dataRetirada
                        if (horaRetirada != null) pedidoData["hora_retirada"] = horaRetirada
                    }

                    printerController.imprimirPedido(pedidoData = pedidoData)

                    return true
                } catch (e: Exception) {
                    connection.rollback()
                    throw e
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun calculateTotal() {
        if (!::totalLabelRef.isInitialized) return

        // Calculate the sum of all product subtotals
        var subtotal = 0.0
        produtosContainer.children.forEach { node ->
            val hBox = node as? HBox
            val subtotalField = ((hBox?.children?.get(4) as? VBox)?.children?.get(1) as? TextField)
            subtotalField?.let {
                subtotal += parseMoneyValue(it.text)
            }
        }

        // Add delivery value if any
        if (::valorEntregaField.isInitialized) {
            valorEntregaTotal = parseMoneyValue(valorEntregaField.text)
            subtotal += valorEntregaTotal
        }

        // Store subtotal before applying discount
        subtotalValue = subtotal

        // Calculate the discount separately
        aplicarDesconto()
    }

    fun aplicarDesconto() {
        if (!::descontoToggleGroup.isInitialized || !::totalLabelRef.isInitialized) return

        // Calculate current subtotal if it's null or zero
        if (subtotalValue == null || subtotalValue == 0.0) {
            var currentSubtotal = 0.0
            produtosContainer.children.forEach { node ->
                val hBox = node as? HBox
                val subtotalField = ((hBox?.children?.get(4) as? VBox)?.children?.get(1) as? TextField)
                subtotalField?.let {
                    currentSubtotal += parseMoneyValue(it.text)
                }
            }

            // Add delivery value if any
            if (::valorEntregaField.isInitialized) {
                valorEntregaTotal = parseMoneyValue(valorEntregaField.text)
                currentSubtotal += valorEntregaTotal
            }

            subtotalValue = currentSubtotal
        }

        // Now proceed with discount calculation
        val tipoDesconto = if ((descontoToggleGroup.selectedToggle as? RadioButton)?.id == "valor") "valor" else "percentual"
        val subtotal = subtotalValue ?: 0.0

        try {
            if (tipoDesconto == "valor") {
                // Parse currency value
                valorDesconto = parseMoneyValue(descontoField.text)
                // Ensure discount doesn't exceed subtotal
                valorDesconto = minOf(valorDesconto, subtotal)
            } else {
                // Parse percentage and calculate actual discount
                val percentualText = descontoField.text.replace("%", "").replace(",", ".")
                val percentual = percentualText.toDoubleOrNull() ?: 0.0
                valorDesconto = (subtotal * percentual / 100)
            }

            // Calculate new total
            val novoTotal = subtotal - valorDesconto

            // Format and update total label
            val formattedTotal = String.format("%,.2f", novoTotal)
                .replace(",", ".")
                .replace(".", ",")
                .replaceFirst(",", ".")

            Platform.runLater {
                totalLabelRef.text = "R$ $formattedTotal"

                // Also update troco calculation if needed
                if (::trocoParaField.isInitialized && ::trocoCalculadoLabel.isInitialized) {
                    calcularTroco()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatarTextoCapitalizado(texto: String): String {
        val excecoes = setOf("de", "da", "do", "das", "dos", "e", "com", "para", "a", "o", "em",
            "por", "sem", "sob", "sobre", "à", "às", "ao", "aos")

        return texto.lowercase().split(" ").joinToString(" ") { palavra ->
            if (palavra in excecoes) palavra else palavra.replaceFirstChar { it.uppercase() }
        }
    }
}