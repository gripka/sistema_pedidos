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
    init {
        // Initialize with the first product
        val primeiroProduto = Produto(1, "", 1, 0.0)
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
                Label("${produto.id}. "),  // Número do produto
                quantidadeField,
                createProdutoSection(),
                valorField,
                subtotalField
            )

            // Mais de um produto, botão de remover
            if (listaProdutos.size > 1) {
                children.add(createRemoveButton(this, produto))
            }

            // Atualizar subtotal ao alterar quantidade ou valor
            val updateSubtotal = {
                val quantidade = ((quantidadeField.children[1] as HBox).children[1] as TextField)
                val valorUnitario = (valorField.children[1] as TextField)
                val subtotal = (subtotalField.children[1] as TextField)

                val quantidadeValue = quantidade.text.toIntOrNull() ?: 1
                val valorUnitarioValue = valorUnitario.text.replace("R$ ", "").replace(",", ".").toDoubleOrNull() ?: 0.0
                val subtotalValue = (quantidadeValue * valorUnitarioValue)
                subtotal.text = "R$ ${String.format("%.2f", subtotalValue).replace(".", ",")}"
            }

            val quantidadeTextField = ((quantidadeField.children[1] as HBox).children[1] as TextField)
            val valorTextField = (valorField.children[1] as TextField)

            quantidadeTextField.textProperty().addListener { _, _, _ -> updateSubtotal() }
            valorTextField.textProperty().addListener { _, _, _ -> updateSubtotal() }
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
            labelNumero.text = "${index + 1}. " // Atualiza a numeração
        }

        // Atualizar listaProdutos para manter IDs sincronizados
        listaProdutos.forEachIndexed { index, produto ->
            produto.id = index + 1
        }
    }

    fun addNovoProduto() {
        val novoProduto = Produto(listaProdutos.size + 1, "", 1, 0.0)
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
        if (produtosContainer.children.isEmpty()) {
            produtosContainer.children.add(createProdutosHBox(Produto(1, "", 1, 0.0)))
        }
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
                            text = produto.quantidade.toString()
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

                    focusedProperty().addListener { _, _, isFocused ->
                        if (isFocused) {
                            Platform.runLater { positionCaret(text.length) }
                        }
                    }

                    var isUpdating = false
                    textProperty().addListener { _, _, newValue ->
                        if (isUpdating) return@addListener

                        isUpdating = true
                        Platform.runLater {
                            try {
                                val digits = newValue.replace(Regex("[^\\d]"), "")

                                if (digits.isEmpty()) {
                                    text = "R$ 0,00"
                                } else {
                                    val reais = if (digits.length <= 2) "0"
                                    else digits.substring(0, digits.length - 2).toInt().toString()
                                    val cents = digits.takeLast(2).padStart(2, '0')
                                    text = "R$ ${reais},${cents}"
                                }
                                positionCaret(text.length)
                            } finally {
                                isUpdating = false
                            }
                        }
                    }
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
                    val digits = newValue.replace(Regex("[^\\d]"), "").takeLast(10)

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
}