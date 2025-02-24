package com.sistema_pedidos.controller

import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.application.Platform
import javafx.scene.control.Label
import com.sistema_pedidos.model.Produto
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.beans.property.StringProperty
import javafx.scene.layout.*
import javafx.scene.paint.Color

class NovoPedidoController {
    private val produtosContainer = VBox(10.0)
    private val listaProdutos: ObservableList<Produto> = FXCollections.observableArrayList()
    private lateinit var totalLabelRef: Label

    init {
        val primeiroProduto = Produto(1L, "", "Produto 1", null, 0.0, null, "UN", 0, 0, "Ativo", null, null)
        listaProdutos.add(primeiroProduto)
        produtosContainer.children.add(createProdutosHBox(primeiroProduto))
    }
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
        var total = 0.0

        produtosContainer.children.forEach { node ->
            val hBox = node as HBox
            val subtotalVBox = hBox.children.find { it is VBox && it.children[0] is Label && (it.children[0] as Label).text == "Subtotal" } as? VBox
            val subtotalField = subtotalVBox?.children?.get(1) as? TextField

            subtotalField?.let { field ->
                val subtotalText = field.text.replace(Regex("[^\\d]"), "")
                total += (subtotalText.toDoubleOrNull() ?: 0.0) / 100
            }
        }

        val formattedValue = String.format("%,.2f", total)
            .replace(",", ".")
            .replace(".", ",", ignoreCase = true)
            .replaceFirst(",", ".")

        Platform.runLater {
            totalLabel.text = "Total do Pedido: R$ $formattedValue"
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
}