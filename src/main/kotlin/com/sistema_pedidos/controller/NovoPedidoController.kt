package com.sistema_pedidos.controller

import com.sistema_pedidos.database.DatabaseHelper
import com.sistema_pedidos.model.Pedido
import javafx.geometry.Pos
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.application.Platform
import com.sistema_pedidos.model.Produto
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.*
import javafx.scene.layout.*
import java.sql.SQLException
import java.sql.Statement

class NovoPedidoController {
    private val produtosContainer = VBox(10.0)
    private val listaProdutos: ObservableList<Produto> = FXCollections.observableArrayList()
    private lateinit var valorEntregaField: TextField
    private lateinit var totalLabelRef: Label
    private lateinit var descontoField: TextField
    internal lateinit var descontoToggleGroup: ToggleGroup
    private lateinit var trocoParaField: TextField
    private lateinit var trocoCalculadoLabel: Label
    private lateinit var contentContainer: VBox

    fun incrementQuantity(textField: TextField) {
        val value = textField.text.toInt()
        if (value < 999) textField.text = (value + 1).toString()
    }

    fun decrementQuantity(textField: TextField) {
        val value = textField.text.toInt()
        if (value > 1) textField.text = (value - 1).toString()
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
            alignment = Pos.CENTER_LEFT
            val quantidadeField = createQuantidadeSection(produto)
            val valorField = createValorSection(produto)
            val subtotalField = createSubtotalSection(produto)

            children.addAll(
                Label("${produto.id}. "),
                quantidadeField,
                createProdutoSection(),
                valorField,
                subtotalField
            )

            if (listaProdutos.size > 1) {
                children.add(createRemoveButton(this, produto))
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
                    updateTotal(totalLabelRef)
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

    fun updateTotal(totalLabel: Label) {
        var subtotal = 0.0

        produtosContainer.children.forEach { node ->
            val hBox = node as HBox
            val subtotalVBox = hBox.children.find { it is VBox && it.children[0] is Label && (it.children[0] as Label).text == "Subtotal" } as? VBox
            val subtotalField = subtotalVBox?.children?.get(1) as? TextField

            subtotalField?.let { field ->
                val subtotalText = field.text.replace(Regex("[^\\d]"), "")
                subtotal += (subtotalText.toDoubleOrNull() ?: 0.0) / 100
            }
        }

        val valorEntregaText = valorEntregaField.text.replace(Regex("[^\\d]"), "")
        val valorEntrega = (valorEntregaText.toDoubleOrNull() ?: 0.0) / 100
        subtotal += valorEntrega

        // Calculate discount
        var totalComDesconto = subtotal
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

        val formattedValue = String.format("%,.2f", totalComDesconto)
            .replace(",", ".")
            .replace(".", ",", ignoreCase = true)
            .replaceFirst(",", ".")

        Platform.runLater {
            totalLabel.text = "R$ $formattedValue"
        }
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
            graphic = ImageView(Image(NovoPedidoController::class.java.getResourceAsStream("/icons/closered.png"))).apply {
                fitHeight = 15.0
                fitWidth = 15.0
                isPreserveRatio = true
            }
            prefWidth = 25.0
            prefHeight = 25.0
            translateY = 15.0  // Adjusted to match the quantity buttons position
            setOnAction {
                produtosContainer.children.remove(produtoHBox)
                listaProdutos.remove(produto)
                atualizarNumeracao()
                atualizarBotoesRemover()
            }
        }
    }


    private fun atualizarNumeracao() {
        produtosContainer.children.forEachIndexed { index, node ->
            val produtoHBox = node as HBox
            val labelNumero = produtoHBox.children.first() as Label
            labelNumero.text = "${index + 1}. "
        }

        // Create new products with updated IDs
        val updatedProducts = listaProdutos.mapIndexed { index, produto ->
            produto.copy(id = (index + 1).toLong())
        }
        listaProdutos.clear()
        listaProdutos.addAll(updatedProducts)
    }
    fun setValorEntregaField(field: TextField) {
        valorEntregaField = field
    }
    fun addNovoProduto() {
        val novoProduto = Produto(
            id = (listaProdutos.size + 1).toLong(),
            codigo = "",
            nome = "Produto ${listaProdutos.size + 1}",
            descricao = null,
            valorUnitario = 0.0,
            categoria = null,
            unidadeMedida = "UN",
            estoqueMinimo = 0,
            estoqueAtual = 0,
            status = "Ativo",
            dataCadastro = null,
            dataAtualizacao = null
        )
        listaProdutos.add(novoProduto)
        produtosContainer.children.add(createProdutosHBox(novoProduto))
        atualizarBotoesRemover()
    }

    private fun atualizarBotoesRemover() {
        produtosContainer.children.forEach { node ->
            val produtoHBox = node as HBox
            val botaoRemover = produtoHBox.children.find { it is Button } as? Button
            if (listaProdutos.size > 1) {
                if (botaoRemover == null) {
                    val produto = listaProdutos[produtosContainer.children.indexOf(produtoHBox)]
                    produtoHBox.children.add(createRemoveButton(produtoHBox, produto))
                }
            } else {
                botaoRemover?.let { produtoHBox.children.remove(it) }
            }
        }
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
                            styleClass.add("text-field")
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
                    prefWidth = 100.0
                    maxWidth = 100.0
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
        return VBox(10.0).apply {
            HBox.setHgrow(this, Priority.ALWAYS)
            children.addAll(
                Label("Produto").apply {
                    styleClass.add("field-label")
                },
                TextField().apply {
                    styleClass.add("text-field")
                    maxWidth = Double.POSITIVE_INFINITY
                    HBox.setHgrow(this, Priority.ALWAYS)
                }
            )
        }
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
    }

    fun setDescontoToggleGroup(group: ToggleGroup) {
        descontoToggleGroup = group
    }

    fun setTrocoParaField(field: TextField) {
        trocoParaField = field
    }

    fun setTrocoCalculadoLabel(label: Label) {
        trocoCalculadoLabel = label
    }
    fun calcularTroco() {
        val trocoParaText = trocoParaField.text.replace(Regex("[^\\d]"), "")
        val trocoPara = (trocoParaText.toDoubleOrNull() ?: 0.0) / 100

        // Get total from total label
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

        // Reset date pickers
        contentContainer.lookupAll(".date-picker").forEach { node ->
            if (node is DatePicker) {
                if (node.parent?.parent?.toString()?.contains("retirada-fields") == true) {
                    node.value = null
                } else {
                    node.value = java.time.LocalDate.now()
                }
            }
        }

        // Reset time pickers
        contentContainer.lookupAll(".time-picker").forEach { node ->
            if (node is ComboBox<*>) {
                if ((node.parent as? HBox)?.parent?.toString()?.contains("retirada-fields") == true) {
                    node.value = if (node.prefWidth == 70.0) "08" else "00"
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

        // Reset discount type
        descontoToggleGroup.selectToggle(
            descontoToggleGroup.toggles.find { (it as RadioButton).id == "valor" }
        )

        // Reset delivery switch and form
        val deliverySwitch = contentContainer.lookup(".switch") as? StackPane
        deliverySwitch?.let {
            val checkbox = it.children.find { node -> node is CheckBox } as? CheckBox
            checkbox?.isSelected = false
        }

        // Reset products
        produtosContainer.children.clear()
        listaProdutos.clear()
        addNovoProduto()

        // Reset total label
        totalLabelRef.text = "R$ 0,00"

        // Reset troco fields
        trocoParaField.text = "R$ 0,00"
        trocoCalculadoLabel.text = "R$ 0,00"

        // Reset valor entrega
        valorEntregaField.text = "R$ 0,00"

        // Reset desconto field
        descontoField.text = if ((descontoToggleGroup.selectedToggle as? RadioButton)?.id == "valor")
            "R$ 0,00" else "0,00"
    }

    private fun parseMoneyValue(value: String): Double {
        // Remove R$ e espaços
        var cleanValue = value.replace(Regex("[R$\\s]"), "")

        // Remove os pontos de separador de milhar
        cleanValue = cleanValue.replace(".", "")

        // Substitui vírgula por ponto para decimal
        cleanValue = cleanValue.replace(",", ".")

        // Converte para Double dividindo por 100 apenas se não houver ponto decimal
        return try {
            if (!value.contains(",")) {
                // Se não tem vírgula, assume que é um valor em centavos
                cleanValue.toDouble() / 100
            } else {
                // Se tem vírgula, é um valor já formatado com decimais
                cleanValue.toDouble()
            }
        } catch (e: NumberFormatException) {
            0.0
        }
    }

    fun salvarPedido(clienteInfo: List<Pair<String, String>>,
                     pagamentoInfo: List<Pair<String, String>>,
                     entregaInfo: List<Pair<String, String>>): Boolean {
        try {
            val telefoneContato = clienteInfo.find { it.first == "Telefone" }?.second ?: ""
            val observacao = clienteInfo.find { it.first == "Observação" }?.second
            val status = pagamentoInfo.find { it.first == "Status" }?.second ?: "Pendente"

            val valorTotal = parseMoneyValue(pagamentoInfo.find { it.first == "Total do Pedido" }?.second ?: "0,00")

            val descontoInfo = pagamentoInfo.find { it.first.startsWith("Desconto") }
            val valorDesconto = parseMoneyValue(descontoInfo?.second ?: "0,00")
            val tipoDesconto = if (descontoInfo?.first?.contains("Percentual") == true) "percentual" else "valor"

            val formaPagamento = pagamentoInfo.find { it.first == "Forma de Pagamento" }?.second
            val valorTrocoPara = parseMoneyValue(trocoParaField.text)
            val valorTroco = parseMoneyValue(trocoCalculadoLabel.text)

            DatabaseHelper().getConnection().use { connection ->
                connection.autoCommit = false
                try {
                    var numeroGerado = ""
                    connection.createStatement().use { stmt ->
                        val rs = stmt.executeQuery("SELECT COALESCE(MAX(CAST(SUBSTR(numero, 4) AS INTEGER)), 0) + 1 as next_num FROM pedidos")
                        if (rs.next()) {
                            numeroGerado = "PED%04d".format(rs.getInt("next_num"))
                        }
                    }

                    val pedidoQuery = """
                        INSERT INTO pedidos (numero, telefone_contato, observacao, status,
                        valor_total, valor_desconto, tipo_desconto, forma_pagamento, valor_troco_para, valor_troco,
                        data_retirada, hora_retirada)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()

                    val pedidoId = connection.prepareStatement(pedidoQuery, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                        stmt.setString(1, numeroGerado)
                        stmt.setString(2, telefoneContato)
                        stmt.setString(3, observacao)
                        stmt.setString(4, status)
                        stmt.setDouble(5, valorTotal)
                        stmt.setDouble(6, valorDesconto)
                        stmt.setString(7, tipoDesconto)
                        stmt.setString(8, formaPagamento)
                        stmt.setDouble(9, valorTrocoPara)
                        stmt.setDouble(10, valorTroco)
                        stmt.setString(11, pagamentoInfo.find { it.first == "Data de Retirada" }?.second)
                        stmt.setString(12, pagamentoInfo.find { it.first == "Hora de Retirada" }?.second)

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

                        connection.prepareStatement(itemQuery).use { stmt ->
                            stmt.setLong(1, pedidoId)
                            stmt.setObject(2, null)
                            stmt.setString(3, nomeProduto)
                            stmt.setInt(4, quantidade)
                            stmt.setDouble(5, valorUnitario)
                            stmt.setDouble(6, subtotal)
                            stmt.executeUpdate()
                        }
                    }

                    if (entregaInfo.first().second == "Sim") {
                        val entregaQuery = """
                        INSERT INTO entregas (pedido_id, nome_destinatario, telefone_destinatario,
                        endereco, referencia, cidade, bairro, cep, valor_entrega, data_entrega, hora_entrega)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()

                        connection.prepareStatement(entregaQuery).use { stmt ->
                            stmt.setLong(1, pedidoId)
                            stmt.setString(2, entregaInfo.find { it.first == "Nome" }?.second)
                            stmt.setString(3, entregaInfo.find { it.first == "Telefone" }?.second)
                            stmt.setString(4, entregaInfo.find { it.first == "Endereço" }?.second)
                            stmt.setString(5, entregaInfo.find { it.first == "Referência" }?.second)
                            stmt.setString(6, entregaInfo.find { it.first == "Cidade" }?.second)
                            stmt.setString(7, entregaInfo.find { it.first == "Bairro" }?.second)
                            stmt.setString(8, entregaInfo.find { it.first == "CEP" }?.second)
                            stmt.setDouble(9, parseMoneyValue(entregaInfo.find { it.first == "Valor" }?.second ?: "0,00"))
                            stmt.setString(10, entregaInfo.find { it.first == "Data" }?.second)
                            stmt.setString(11, entregaInfo.find { it.first == "Hora" }?.second)
                            stmt.executeUpdate()
                        }
                    }

                    connection.commit()
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
}