package com.sistema_pedidos.view

import com.sistema_pedidos.database.DatabaseHelper
import com.sistema_pedidos.model.Produto
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.StageStyle
import javafx.util.Callback
import java.awt.event.ActionEvent
import java.sql.SQLException
import java.text.NumberFormat
import java.util.*
import java.sql.Connection

data class UnidadeMedida(val codigo: String, val descricao: String) {
    override fun toString() = "$codigo - $descricao"
}

data class InsumoQuantidade(
    val insumoId: Long,
    val nome: String,
    val quantidade: Double,
    val unidadeMedida: String
)

class ProdutosView : BorderPane() {
    private val tableView = TableView<Produto>()
    private val produtos = FXCollections.observableArrayList<Produto>()
    private val searchField = TextField()
    private val formPanel = VBox(10.0)
    private var produtoSelecionado: Produto? = null
    private val db = DatabaseHelper()
    private lateinit var cbEhInsumo: CheckBox
    private lateinit var tableInsumos: TableView<InsumoQuantidade>
    private lateinit var cbInsumoDisponivel: ComboBox<Produto>
    private lateinit var tfQuantidadeInsumo: TextField
    private val insumosDosProdutos = FXCollections.observableArrayList<InsumoQuantidade>()

    init {
        styleClass.add("main-container")
        background = Background(BackgroundFill(Color.WHITE, null, null))
        padding = Insets(20.0)

        stylesheets.add(javaClass.getResource("/produtosview.css").toExternalForm())

        minWidth = 800.0
        minHeight = 600.0

        setupUI()
        loadProducts()
    }

    private fun setupUI() {
        val title = Label("Gerenciamento de Produtos").apply {
            style = "-fx-font-size: 24px; -fx-font-weight: bold;"
        }

        // Search controls
        searchField.apply {
            promptText = "Buscar produtos..."
            prefWidth = 300.0
            prefHeight = 36.0
        }

        val searchButton = Button("Buscar").apply {
            styleClass.add("primary-button")
            prefHeight = 36.0
            setOnAction { searchProducts(searchField.text) }
        }

        val refreshButton = Button("Atualizar").apply {
            styleClass.add("primary-button")  // Changed from "action-button"
            prefHeight = 36.0  // Added to match other buttons
            setOnAction {
                loadProducts()
            }
        }

        val newButton = Button("Novo Produto").apply {
            styleClass.add("primary-button")
            prefHeight = 36.0  // Match HistoricoPedidosView height
            setOnAction {
                insumosDosProdutos.clear()
                carregarInsumosDisponiveis()
                limparFormulario()
                produtoSelecionado = null
            }
        }

        // In the setupUI() method, after creating the newButton
        val newCategoryButton = Button("Nova Categoria").apply {
            styleClass.add("primary-button")
            prefHeight = 36.0
            setOnAction {
                val dialog = Dialog<String>()
                dialog.title = "Nova Categoria"
                dialog.headerText = "Adicionar Nova Categoria"
                dialog.initStyle(StageStyle.UNDECORATED)

                val buttonTypeOk = ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE)
                val buttonTypeCancel = ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE)
                dialog.dialogPane.buttonTypes.addAll(buttonTypeOk, buttonTypeCancel)

                // Apply CSS to dialog
                dialog.dialogPane.stylesheets.addAll(this@ProdutosView.stylesheets)

                // Dialog content setup
                val textField = TextField().apply {
                    prefHeight = 36.0
                    prefWidth = 300.0
                    promptText = "Digite o nome da nova categoria"
                    styleClass.add("text-field")
                }

                val content = VBox(10.0).apply {
                    padding = Insets(20.0)
                    spacing = 10.0
                    prefWidth = 400.0
                    children.addAll(
                        Label("Nome da categoria:").apply {
                            style = "-fx-font-size: 14px; -fx-text-fill: #2B2D31;"
                        },
                        textField
                    )
                }

                dialog.dialogPane.style = """
            -fx-background-color: white;
            -fx-border-color: #D3D3D3;
            -fx-border-width: 1px;
        """

                dialog.dialogPane.content = content

                // Apply header style
                dialog.dialogPane.lookup(".header-panel")?.style = """
            -fx-background-color: #2B2D30;
            -fx-background-radius: 0;
        """

                // Style header text
                val headerLabel = dialog.dialogPane.lookup(".header-panel .label") as? Label
                headerLabel?.style = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;"

                // Important: Get button references before the dialog is shown
                val confirmButton = dialog.dialogPane.lookupButton(buttonTypeOk)
                val cancelButton = dialog.dialogPane.lookupButton(buttonTypeCancel)

                // Apply styles to buttons directly
                confirmButton.styleClass.add("primary-button")
                cancelButton.styleClass.add("secondary-button")

                // Apply inline styles to ensure visual consistency
                confirmButton.style = """
            -fx-background-color: #6056e8;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-background-radius: 5px;
            -fx-min-width: 100px;
            -fx-padding: 8px 16px;
            -fx-cursor: hand;
        """

                cancelButton.style = """
            -fx-background-color: #f0f0f0;
            -fx-text-fill: #333333;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-background-radius: 5px;
            -fx-min-width: 100px;
            -fx-padding: 8px 16px;
            -fx-cursor: hand;
        """

                // Configure button bar to center buttons
                val buttonBar = dialog.dialogPane.lookup(".button-bar") as ButtonBar
                buttonBar.apply {
                    buttonOrder = ""
                    buttonMinWidth = 100.0
                    alignment = Pos.CENTER
                    padding = Insets(0.0, 75.0, 0.0, 0.0)
                    style = "-fx-background-color: white;"
                }

                dialog.setResultConverter { buttonType ->
                    if (buttonType == buttonTypeOk) textField.text else null
                }

                confirmButton.setOnMouseEntered {
                    confirmButton.style += "-fx-background-color: #433a94;"
                }

                confirmButton.setOnMouseExited {
                    confirmButton.style += "-fx-background-color: #6056e8;"
                }

                dialog.showAndWait().ifPresent { newCategory ->
                    val trimmed = newCategory.trim()
                    if (trimmed.isNotEmpty() && !cbCategoria.items.contains(trimmed)) {
                        saveCategory(trimmed)
                        cbCategoria.items.add(trimmed)
                        FXCollections.sort(cbCategoria.items)
                        cbCategoria.value = trimmed
                    }
                }
            }
        }

        val btnHistorico = Button("Histórico de Movimentações").apply {
            styleClass.add("secondary-button")
            setOnAction { mostrarHistoricoMovimentacoes() }
        }

        val btnFiltrarBaixoEstoque = Button("Filtrar baixo estoque").apply {
            tooltip = Tooltip("Mostrar apenas itens com estoque baixo")
            styleClass.add("primary-button")

            var filtroAtivo = false

            setOnAction {
                filtroAtivo = !filtroAtivo
                if (filtroAtivo) {
                    style = "-fx-background-color: #FFA500; -fx-text-fill: white; -fx-font-weight: bold;"
                    val produtosBaixoEstoque = produtos.filtered {
                        it.estoqueAtual <= it.estoqueMinimo && it.estoqueMinimo > 0
                    }
                    tableView.items = produtosBaixoEstoque
                } else {
                    style = ""
                    tableView.items = produtos
                }
            }
        }


        val topControls = HBox(10.0, searchField, searchButton, refreshButton, newButton, newCategoryButton, btnHistorico, btnFiltrarBaixoEstoque).apply {
            alignment = Pos.CENTER_LEFT
        }



        val headerBox = VBox(15.0, title, topControls).apply {
            padding = Insets(0.0, 0.0, 15.0, 0.0)
        }

        setTop(headerBox)

        val topContainer = VBox(10.0).apply {
            children.addAll(headerBox, createStockDashboard())
        }

        setTop(topContainer)

        val splitPane = HBox(20.0).apply {
            children.addAll(
                createTableView(),
                VBox(20.0).apply {
                    children.add(createFormPanel())
                }
            )
            HBox.setHgrow(tableView, Priority.ALWAYS)
        }

        setCenter(splitPane)

        Platform.runLater {
            verificarAlertasEstoque()
        }
    }

    private fun limparFormulario() {
        // Reset text fields
        tfCodigo.text = ""
        tfNome.text = ""
        taDescricao.text = ""
        tfValorUnitario.text = ""
        cbCategoria.value = null
        cbUnidadeMedida.value = cbUnidadeMedida.items[0]  // Set to first item (UN)
        tfEstoqueMinimo.text = "0"
        tfEstoqueAtual.text = "0"
        cbStatus.value = "Ativo"

        // Reset focus to first field
        tfNome.requestFocus()

        // Update buttons state
        btnSalvar.text = "Salvar"
        btnCancelar.isDisable = false
    }

    private fun createTableView(): TableView<Produto> {
        tableView.apply {
            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            items = produtos
            styleClass.add("table-view")
            style = """
                -fx-border-color: rgb(223, 225, 230);
                -fx-border-width: 2px;
                -fx-border-radius: 3px;
            """
        }

        setupTableView()

        VBox.setVgrow(tableView, Priority.ALWAYS)
        HBox.setHgrow(tableView, Priority.ALWAYS)

        return tableView
    }


    private fun createFormPanel(): VBox {
        tfCodigo = TextField().apply {
            isEditable = false
            promptText = "Gerado automaticamente"
            styleFormField(this)
            style = """${style ?: ""}
            -fx-background-color: #f0f0f0;
            -fx-opacity: 0.9;
            -fx-border-color: #cccccc;
        """
            tooltip = Tooltip("Código gerado automaticamente pelo sistema")
        }

        tfNome = TextField().apply {
            promptText = "Nome do produto"
            styleFormField(this)

            val excecoes = setOf("de", "da", "do", "das", "dos", "e", "com", "para", "a", "o", "em",
                "por", "sem", "sob", "sobre", "à", "às", "ao", "aos")

            focusedProperty().addListener { _, wasFocused, isFocused ->
                if (wasFocused && !isFocused && text.isNotEmpty()) {
                    val words = text.trim().split(" ")
                    text = words.mapIndexed { index, word ->
                        if (word.isEmpty()) return@mapIndexed ""

                        // Always capitalize first word or if not in exceptions list
                        if (index == 0 || !excecoes.contains(word.lowercase())) {
                            word.first().uppercase() + word.substring(1).lowercase()
                        } else {
                            // Keep exception words lowercase
                            word.lowercase()
                        }
                    }.joinToString(" ")
                }
            }
        }

        taDescricao = TextArea().apply {
            promptText = "Descrição detalhada"
            prefHeight = 60.0
            maxHeight = 60.0
            setWrapText(true)
            style = """
                -fx-control-inner-background: white;
                -fx-background-color: white;
                -fx-background-radius: 4px;
                -fx-border-color: #cccccc;
                -fx-border-radius: 4px;
                -fx-scroll-bar-policy: as-needed;
            """
        }

        tfValorUnitario = TextField().apply {
            promptText = "R$ 0,00"
            text = "R$ 0,00"
            styleFormField(this)
            formatarMoeda(this)
        }

        cbEhInsumo = CheckBox("Este produto também pode ser usado como insumo").apply {
            isSelected = false
            style = """
                -fx-background-color: transparent;
                -fx-border-radius: 3px;
                -fx-background-radius: 3px;
            """
            styleClass.add("primary-button")
        }

        cbCategoria = ComboBox<String>().apply {
            items = FXCollections.observableArrayList(loadCategories())
            isEditable = true
            promptText = "Selecione ou digite uma categoria"
            prefWidth = Double.MAX_VALUE

            editor.focusedProperty().addListener { _, wasFocused, isFocused ->
                if (wasFocused && !isFocused) {
                    val newValue = editor.text.trim()
                    if (newValue.isNotEmpty() && !items.contains(newValue)) {
                        saveCategory(newValue)
                        items.add(newValue)
                        FXCollections.sort(items)
                    }
                }
            }

            // Context menu for category management
            val contextMenu = ContextMenu()
            val addMenuItem = MenuItem("Adicionar Nova Categoria")
            val deleteMenuItem = MenuItem("Excluir Categoria")

            addMenuItem.setOnAction {
                val dialog = TextInputDialog()
                dialog.title = "Nova Categoria"
                dialog.headerText = "Adicionar Nova Categoria"
                dialog.contentText = "Nome da categoria:"

                dialog.showAndWait().ifPresent { newCategory ->
                    val trimmed = newCategory.trim()
                    if (trimmed.isNotEmpty() && !items.contains(trimmed)) {
                        saveCategory(trimmed)
                        items.add(trimmed)
                        FXCollections.sort(items)
                        value = trimmed
                    }
                }
            }

            deleteMenuItem.setOnAction {
                val selectedCategory = value
                if (selectedCategory != null) {
                    val alert = Alert(Alert.AlertType.CONFIRMATION)
                    alert.title = "Confirmar Exclusão"
                    alert.headerText = "Excluir Categoria"
                    alert.contentText = "Tem certeza que deseja excluir a categoria: $selectedCategory?"

                    alert.showAndWait().ifPresent { buttonType ->
                        if (buttonType == ButtonType.OK) {
                            deleteCategory(selectedCategory)
                            items.remove(selectedCategory)
                            value = null
                        }
                    }
                }
            }

            contextMenu.items.addAll(addMenuItem, deleteMenuItem)
            setContextMenu(contextMenu)
        }

        val categoryButtonsBox = HBox(5.0).apply {
            alignment = Pos.CENTER_RIGHT

            val addButton = Button("+").apply {
                tooltip = Tooltip("Adicionar Nova Categoria")
                styleClass.add("small-button")
                setOnAction {
                    val event = cbCategoria.contextMenu.items[0].onAction
                    event.handle(javafx.event.ActionEvent())
                }
            }

            val deleteButton = Button("-").apply {
                tooltip = Tooltip("Excluir Categoria Selecionada")
                styleClass.add("small-button")
                setOnAction {
                    val event = cbCategoria.contextMenu.items[1].onAction
                    event.handle(javafx.event.ActionEvent())
                }
            }

            children.addAll(addButton, deleteButton)
        }

        cbUnidadeMedida = ComboBox<String>().apply {
            items = FXCollections.observableArrayList(
                "UN", "KG", "L", "M", "CX"
            )
            value = "UN"
            prefWidth = Double.MAX_VALUE
        }

        tfEstoqueMinimo = TextField().apply {
            promptText = "0"
            text = "0"
            styleFormField(this)

            textProperty().addListener { _, _, newValue ->
                if (newValue.isEmpty()) {
                    text = "0"
                } else if (!newValue.matches("\\d*".toRegex())) {
                    text = newValue.replace("[^\\d]".toRegex(), "")
                }

                if (text.length > 1 && text.startsWith("0")) {
                    text = text.replaceFirst("^0+".toRegex(), "")
                }
            }
        }

        tfEstoqueAtual = TextField().apply {
            promptText = "0"
            text = "0"
            styleFormField(this)

            textProperty().addListener { _, _, newValue ->
                if (newValue.isEmpty()) {
                    text = "0"
                } else if (!newValue.matches("\\d*".toRegex())) {
                    text = newValue.replace("[^\\d]".toRegex(), "")
                }

                if (text.length > 1 && text.startsWith("0")) {
                    text = text.replaceFirst("^0+".toRegex(), "")
                }
            }
        }

        cbStatus = ComboBox<String>().apply {
            items = FXCollections.observableArrayList("Ativo", "Inativo")
            value = "Ativo"
            prefWidth = Double.MAX_VALUE
        }

        btnSalvar = Button("Salvar").apply {
            styleClass.add("primary-button")
            prefWidth = 120.0
            setOnAction { salvarProduto() }
        }

        btnCancelar = Button("Cancelar").apply {
            styleClass.add("secondary-button")
            prefWidth = 120.0
            setOnAction {
                limparFormulario()
                produtoSelecionado = null
            }
        }

        val insumosPanel = createInsumosPanel()

        val scrollPane = ScrollPane().apply {
            isFitToWidth = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
            style = "-fx-background-color: transparent;"
        }

        formPanel.apply {
            padding = Insets(15.0)
            spacing = 10.0
            prefWidth = 450.0
            maxWidth = 450.0
            style = """
            -fx-background-color: white;
            -fx-border-color: rgb(223, 225, 230);
            -fx-border-width: 2px;
            -fx-border-radius: 3px;
        """

            children.addAll(
                Label("Código:"), tfCodigo,
                Label("Nome:"), tfNome,
                Label("Descrição:"), taDescricao,
                Label("Valor Unitário:"), tfValorUnitario,
                Label("Categoria:"), cbCategoria,
                Label("Insumo:"), cbEhInsumo,
                Label("Unidade Medida:"), cbUnidadeMedida,
                Label("Estoque Mínimo:"), tfEstoqueMinimo,
                Label("Estoque Atual:"), tfEstoqueAtual,
                Label("Status:"), cbStatus,
                Separator().apply { padding = Insets(5.0, 0.0, 5.0, 0.0) },
                insumosPanel,
                Separator().apply { padding = Insets(5.0, 0.0, 5.0, 0.0) },
                HBox(10.0, btnSalvar, btnCancelar).apply {
                    alignment = Pos.CENTER
                    padding = Insets(10.0, 0.0, 0.0, 0.0)
                }
            )
        }

        scrollPane.content = formPanel

        return VBox(scrollPane).apply {
            HBox.setHgrow(this, Priority.NEVER)
        }
    }

    private fun formatarMoeda(textField: TextField) {
        var isUpdating = false
        textField.text = "R$ 0,00"

        textField.textProperty().addListener { _, _, newValue ->
            if (isUpdating) return@addListener

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

        // Position caret at the end when focused
        textField.focusedProperty().addListener { _, _, isFocused ->
            if (isFocused) {
                Platform.runLater {
                    textField.positionCaret(textField.text.length)
                }
            }
        }

        // Always keep caret at the end to prevent editing in the middle
        textField.caretPositionProperty().addListener { _, _, _ ->
            Platform.runLater {
                textField.positionCaret(textField.text.length)
            }
        }
    }

    private fun createInsumosPanel(): VBox {
        val insumosPanel = VBox(10.0).apply {
            padding = Insets(10.0)
            style = """
        -fx-background-color: white;
        -fx-border-color: #dfe1e6;
        -fx-border-width: 1px;
        -fx-border-radius: 3px;
    """
        }

        val title = Label("Composição do Produto (Insumos)").apply {
            style = "-fx-font-weight: bold; -fx-font-size: 14px;"
        }

        tableInsumos = TableView<InsumoQuantidade>().apply {
            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            prefHeight = 150.0

            val insumoColumn = TableColumn<InsumoQuantidade, String>("Insumo").apply {
                cellValueFactory = PropertyValueFactory("nome")
                prefWidth = 150.0
            }

            val quantidadeColumn = TableColumn<InsumoQuantidade, Double>("Quantidade").apply {
                cellValueFactory = PropertyValueFactory("quantidade")
                prefWidth = 80.0
            }

            val unidadeColumn = TableColumn<InsumoQuantidade, String>("Unidade").apply {
                cellValueFactory = PropertyValueFactory("unidadeMedida")
                prefWidth = 60.0
            }

            val acoesColumn = TableColumn<InsumoQuantidade, Void>("Ações").apply {
                prefWidth = 80.0
                minWidth = 80.0
                maxWidth = 80.0
                style = "-fx-alignment: CENTER;"
                cellFactory = Callback {
                    object : TableCell<InsumoQuantidade, Void>() {
                        private val removeBtn = Button("X").apply {
                            styleClass.add("small-button")
                            style = "-fx-background-color: #ff5252; -fx-text-fill: white;"
                            prefWidth = 70.0
                        }

                        private val box = HBox().apply {
                            alignment = Pos.CENTER
                            children.add(removeBtn)
                        }

                        init {
                            removeBtn.setOnAction {
                                val insumo = tableRow.item
                                if (insumo != null) {
                                    insumosDosProdutos.remove(insumo)
                                }
                            }
                        }

                        override fun updateItem(item: Void?, empty: Boolean) {
                            super.updateItem(item, empty)
                            graphic = if (empty) null else box
                        }
                    }
                }
            }

            columns.addAll(insumoColumn, quantidadeColumn, unidadeColumn, acoesColumn)
            items = insumosDosProdutos
        }

        cbInsumoDisponivel = ComboBox<Produto>().apply {
            prefWidth = 160.0
            promptText = "Selecione um insumo"

            // Custom cell factory to display only product name
            buttonCell = createProductCell()
            cellFactory = Callback { createProductCell() }

            // Converter for string representation
            converter = object : javafx.util.StringConverter<Produto>() {
                override fun toString(produto: Produto?): String {
                    return produto?.nome ?: ""
                }

                override fun fromString(string: String): Produto? {
                    return items.find { it.nome == string }
                }
            }
        }

        // Hidden reference for compatibility
        tfQuantidadeInsumo = TextField("1").apply {
            prefWidth = 50.0
            maxWidth = 50.0
            alignment = Pos.CENTER
            text = "1"
            textProperty().addListener { _, _, newValue ->
                if (!newValue.matches(Regex("\\d*"))) {
                    text = newValue.replace(Regex("[^\\d]"), "")
                }
                if (text.isEmpty()) text = "1"
                val num = text.toIntOrNull() ?: 1
                if (num < 1) text = "1"
                if (num > 9999) text = "9999"
            }
        }

        val decrementBtn = createImageButton("menos").apply {
            setOnAction {
                val currentValue = tfQuantidadeInsumo.text.toIntOrNull() ?: 1
                if (currentValue > 1) tfQuantidadeInsumo.text = (currentValue - 1).toString()
            }
        }

        val incrementBtn = createImageButton("mais").apply {
            setOnAction {
                val currentValue = tfQuantidadeInsumo.text.toIntOrNull() ?: 1
                if (currentValue < 9999) tfQuantidadeInsumo.text = (currentValue + 1).toString()
            }
        }

        val quantidadeBox = HBox(5.0, decrementBtn, tfQuantidadeInsumo, incrementBtn).apply {
            alignment = Pos.CENTER_LEFT
        }

        val btnAdicionarInsumo = Button("Adicionar").apply {
            styleClass.add("small-button")
            setOnAction { adicionarInsumoAoProduto() }
        }

        val controlsBox = HBox(10.0, cbInsumoDisponivel, quantidadeBox, btnAdicionarInsumo).apply {
            alignment = Pos.CENTER_LEFT
        }

        insumosPanel.children.addAll(title, tableInsumos, controlsBox)
        return insumosPanel
    }

    private fun createImageButton(tipo: String): Button {
        return Button().apply {
            style = """
            -fx-background-color: transparent;
            -fx-padding: 0;
            -fx-border-color: transparent;
            -fx-focus-color: transparent;
            -fx-faint-focus-color: transparent;
        """

            ProdutosView::class.java.getResourceAsStream("/icons/${tipo}p.png")?.let {
                graphic = ImageView(Image(it)).apply {
                    fitHeight = 30.0
                    fitWidth = 30.0
                    isPreserveRatio = true
                }
            }

            // Override hover effect to show cursor pointer
            setOnMouseEntered {
                style += "-fx-cursor: hand;"
            }
        }
    }

    private fun createProductCell(): ListCell<Produto> {
        return object : ListCell<Produto>() {
            override fun updateItem(item: Produto?, empty: Boolean) {
                super.updateItem(item, empty)
                text = if (empty || item == null) null else item.nome
            }
        }
    }

    private fun carregarInsumosDisponiveis() {
        try {
            db.getConnection().use { conn ->
                val stmt = conn.prepareStatement(
                    """SELECT * FROM produtos 
                   WHERE eh_insumo = 1 AND status = 'Ativo'
                   ORDER BY nome"""
                )
                val rs = stmt.executeQuery()
                val insumos = FXCollections.observableArrayList<Produto>()

                while (rs.next()) {
                    insumos.add(
                        Produto(
                            id = rs.getLong("id"),
                            codigo = rs.getString("codigo"),
                            nome = rs.getString("nome"),
                            descricao = rs.getString("descricao") ?: "",
                            valorUnitario = rs.getDouble("valor_unitario"),
                            categoria = rs.getString("categoria") ?: "",
                            unidadeMedida = rs.getString("unidade_medida"),
                            estoqueMinimo = rs.getInt("estoque_minimo"),
                            estoqueAtual = rs.getInt("estoque_atual"),
                            status = rs.getString("status"),
                            dataCadastro = rs.getString("data_cadastro"),
                            dataAtualizacao = rs.getString("data_atualizacao"),
                            ehInsumo = rs.getInt("eh_insumo") == 1
                        )
                    )
                }
                cbInsumoDisponivel.items = insumos
            }
        } catch (e: SQLException) {
            showAlert("Erro ao carregar insumos", e.message ?: "Erro desconhecido")
        }
    }

    private fun adicionarInsumoAoProduto() {
        val insumoSelecionado = cbInsumoDisponivel.value

        if (insumoSelecionado == null) {
            showAlert("Insumo não selecionado", "Por favor, selecione um insumo.", Alert.AlertType.WARNING)
            return
        }

        // Use tfQuantidadeInsumo directly instead of looking for a Spinner
        val quantidade = tfQuantidadeInsumo.text.toDoubleOrNull()

        if (quantidade == null || quantidade <= 0) {
            showAlert("Quantidade inválida", "Por favor, informe uma quantidade válida maior que zero.", Alert.AlertType.WARNING)
            return
        }

        // Check if this insumo is already added to the product
        val existingInsumo = insumosDosProdutos.find { it.insumoId == insumoSelecionado.id }
        if (existingInsumo != null) {
            showAlert("Insumo duplicado", "Este insumo já foi adicionado ao produto.", Alert.AlertType.WARNING)
            return
        }

        // Add the insumo to the product
        insumosDosProdutos.add(
            InsumoQuantidade(
                insumoId = insumoSelecionado.id,
                nome = insumoSelecionado.nome,
                quantidade = quantidade,
                unidadeMedida = insumoSelecionado.unidadeMedida
            )
        )

        // Reset the selection
        cbInsumoDisponivel.selectionModel.clearSelection()
        tfQuantidadeInsumo.text = "1"
    }

    private fun styleFormField(field: TextField) {
        field.apply {
            prefHeight = 36.0
            styleClass.add("text-field")
        }
    }

    // Load categories from database
    private fun loadCategories(): List<String> {
        val categorias = mutableListOf(
            "Flores Frescas",
            "Buquês",
            "Arranjos",
            "Plantas Ornamentais",
            "Vasos e Cachepots",
            "Cestas",
            "Flores Artificiais",
            "Cartões e Embalagens",
            "Acessórios Florais",
            "Itens para Decoração"
        )

        try {
            db.getConnection().use { conn ->
                val stmt = conn.createStatement()
                val exists = conn.metaData.getTables(null, null, "categorias", null).next()

                if (!exists) {
                    // Create categories table if it doesn't exist
                    conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS categorias (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        nome VARCHAR(100) NOT NULL UNIQUE,
                        data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """)

                    // Insert default categories
                    val batchStmt = conn.prepareStatement("INSERT OR IGNORE INTO categorias (nome) VALUES (?)")
                    for (categoria in categorias) {
                        batchStmt.setString(1, categoria)
                        batchStmt.addBatch()
                    }
                    batchStmt.executeBatch()
                } else {
                    // Load from existing table
                    val rs = stmt.executeQuery("SELECT nome FROM categorias ORDER BY nome")
                    categorias.clear()
                    while (rs.next()) {
                        categorias.add(rs.getString("nome"))
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        return categorias
    }

    // Save new category to database
    private fun saveCategory(categoria: String) {
        try {
            db.getConnection().use { conn ->
                val stmt = conn.prepareStatement("INSERT OR IGNORE INTO categorias (nome) VALUES (?)")
                stmt.setString(1, categoria.trim())
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            // Silent exception - could be duplicate
        }
    }

    private fun deleteCategory(categoria: String) {
        try {
            db.getConnection().use { conn ->
                // First check if category is in use
                val checkStmt = conn.prepareStatement("SELECT COUNT(*) FROM produtos WHERE categoria = ?")
                checkStmt.setString(1, categoria)
                val rs = checkStmt.executeQuery()
                rs.next()
                val count = rs.getInt(1)

                if (count > 0) {
                    showAlert("Erro ao excluir",
                        "Esta categoria não pode ser excluída pois está sendo usada por $count produto(s).")
                    return
                }

                // If not in use, delete it
                val stmt = conn.prepareStatement("DELETE FROM categorias WHERE nome = ?")
                stmt.setString(1, categoria)
                stmt.executeUpdate()

                showAlert("Sucesso", "Categoria excluída com sucesso!", Alert.AlertType.INFORMATION)
            }
        } catch (e: SQLException) {
            showAlert("Erro ao excluir categoria", e.message ?: "Erro desconhecido")
        }
    }

    private lateinit var tfCodigo: TextField
    private lateinit var tfNome: TextField
    private lateinit var taDescricao: TextArea
    private lateinit var tfValorUnitario: TextField
    private lateinit var cbCategoria: ComboBox<String>
    private lateinit var cbUnidadeMedida: ComboBox<String>
    private lateinit var tfEstoqueMinimo: TextField
    private lateinit var tfEstoqueAtual: TextField
    private lateinit var cbStatus: ComboBox<String>
    private lateinit var btnSalvar: Button
    private lateinit var btnCancelar: Button

    private fun salvarProduto() {
        if (!validarCampos()) {
            return
        }

        try {
            val valorUnitario = extrairValorMonetario(tfValorUnitario.text)

            val estoqueMinimo = tfEstoqueMinimo.text.toIntOrNull() ?: 0
            val estoqueAtual = tfEstoqueAtual.text.toIntOrNull() ?: 0
            val ehInsumo = if (cbEhInsumo.isSelected) 1 else 0

            try {
                db.getConnection().use { conn ->
                    conn.createStatement().execute(
                        "ALTER TABLE produtos ADD COLUMN eh_insumo INTEGER DEFAULT 0 CHECK (eh_insumo IN (0, 1))"
                    )
                }
            } catch (e: SQLException) {
            }

            db.getConnection().use { conn ->
                conn.autoCommit = false
                try {
                    var produtoId = produtoSelecionado?.id

                    if (produtoId == null) {
                        val sql = """
                    INSERT INTO produtos (codigo, nome, descricao, valor_unitario, categoria,
                    unidade_medida, estoque_minimo, estoque_atual, status, eh_insumo)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """

                        val stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
                        stmt.setString(1, gerarCodigo())
                        stmt.setString(2, tfNome.text.trim())
                        stmt.setString(3, taDescricao.text.trim())
                        stmt.setDouble(4, valorUnitario)
                        stmt.setString(5, cbCategoria.value ?: "")
                        stmt.setString(6, cbUnidadeMedida.value)
                        stmt.setInt(7, estoqueMinimo)
                        stmt.setInt(8, estoqueAtual)
                        stmt.setString(9, cbStatus.value)
                        stmt.setInt(10, ehInsumo)

                        stmt.executeUpdate()

                        val rs = stmt.generatedKeys
                        if (rs.next()) {
                            produtoId = rs.getLong(1)
                        }
                    } else {
                        val sql = """
                        UPDATE produtos
                        SET nome = ?, descricao = ?, valor_unitario = ?, categoria = ?,
                        unidade_medida = ?, estoque_minimo = ?, estoque_atual = ?, status = ?,
                        eh_insumo = ?, data_atualizacao = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """

                        val stmt = conn.prepareStatement(sql)
                        stmt.setString(1, tfNome.text.trim())
                        stmt.setString(2, taDescricao.text.trim())
                        stmt.setDouble(3, valorUnitario)
                        stmt.setString(4, cbCategoria.value ?: "")
                        stmt.setString(5, cbUnidadeMedida.value)
                        stmt.setInt(6, estoqueMinimo)
                        stmt.setInt(7, estoqueAtual)
                        stmt.setString(8, cbStatus.value)
                        stmt.setInt(9, ehInsumo)
                        stmt.setLong(10, produtoId)

                        stmt.executeUpdate()

                        // Remove insumos existentes
                        val deleteStmt = conn.prepareStatement("DELETE FROM produto_insumos WHERE produto_id = ?")
                        deleteStmt.setLong(1, produtoId)
                        deleteStmt.executeUpdate()
                    }

                    // Salvar os insumos do produto
                    if (produtoId != null && insumosDosProdutos.isNotEmpty()) {
                        val insumosSql = "INSERT INTO produto_insumos (produto_id, insumo_id, quantidade) VALUES (?, ?, ?)"
                        val insumoStmt = conn.prepareStatement(insumosSql)

                        for (insumo in insumosDosProdutos) {
                            insumoStmt.setLong(1, produtoId)
                            insumoStmt.setLong(2, insumo.insumoId)
                            insumoStmt.setDouble(3, insumo.quantidade)
                            insumoStmt.addBatch()
                        }

                        insumoStmt.executeBatch()
                    }

                    conn.commit()
                    mostrarMensagemSucesso("Operação realizada com sucesso",
                        if (produtoSelecionado == null) "Produto cadastrado com sucesso!"
                        else "Produto atualizado com sucesso!"
                    )

                } catch (e: Exception) {
                    conn.rollback()
                    throw e
                } finally {
                    conn.autoCommit = true
                }
            }

            // Reload products and clear form
            loadProducts()
            limparFormulario()
            insumosDosProdutos.clear()
            produtoSelecionado = null

        } catch (e: SQLException) {
            e.printStackTrace()
            showAlert("Erro ao salvar", e.message ?: "Erro ao acessar banco de dados")
        }
    }

    private fun extrairValorMonetario(texto: String): Double {
        val digits = texto.replace(Regex("[^\\d]"), "")

        return if (digits.isEmpty()) 0.0 else digits.toDouble() / 100
    }

    private fun mostrarMensagemSucesso(titulo: String, mensagem: String) {
        val dialog = Dialog<ButtonType>()
        dialog.title = "Sucesso"
        dialog.headerText = titulo
        dialog.initStyle(StageStyle.UNDECORATED)

        val buttonTypeOk = ButtonType("OK", ButtonBar.ButtonData.OK_DONE)
        dialog.dialogPane.buttonTypes.add(buttonTypeOk)

        // Apply CSS
        dialog.dialogPane.stylesheets.addAll(this.stylesheets)

        // Create styled content
        val content = VBox(10.0).apply {
            padding = Insets(20.0)

            children.add(Label(mensagem).apply {
                style = "-fx-font-size: 14px;"
            })
        }

        // Style the dialog
        dialog.dialogPane.style = """
        -fx-background-color: white;
        -fx-border-color: #D3D3D3;
        -fx-border-width: 1px;
    """

        dialog.dialogPane.content = content

        // Style header with success color
        dialog.dialogPane.lookup(".header-panel")?.style = """
        -fx-background-color: #4CAF50;
        -fx-background-radius: 0;
    """

        // Style header text
        val headerLabel = dialog.dialogPane.lookup(".header-panel .label") as? Label
        headerLabel?.style = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;"

        // Style the button
        val okButton = dialog.dialogPane.lookupButton(buttonTypeOk)
        okButton.styleClass.add("primary-button")

        // Configure button bar
        val buttonBar = dialog.dialogPane.lookup(".button-bar") as ButtonBar
        buttonBar.apply {
            buttonOrder = ButtonBar.BUTTON_ORDER_NONE
            buttonMinWidth = 100.0
            style = """
            -fx-background-color: white;
            -fx-alignment: center;
        """
            padding = Insets(0.0, 80.0, 0.0, 0.0)
        }

        dialog.showAndWait()
    }

    private fun validarCampos(): Boolean {
        val mensagensErro = mutableListOf<String>()

        if (tfNome.text.trim().isEmpty()) {
            mensagensErro.add("O nome do produto é obrigatório")
        }

        try {
            if (tfValorUnitario.text.isNotEmpty()) {
                extrairValorMonetario(tfValorUnitario.text)
            }
        } catch (e: NumberFormatException) {
            mensagensErro.add("Valor unitário inválido")
        }

        try {
            if (tfEstoqueMinimo.text.isNotEmpty()) {
                tfEstoqueMinimo.text.toInt()
            }
        } catch (e: NumberFormatException) {
            mensagensErro.add("Estoque mínimo deve ser um número inteiro")
        }

        try {
            if (tfEstoqueAtual.text.isNotEmpty()) {
                tfEstoqueAtual.text.toInt()
            }
        } catch (e: NumberFormatException) {
            mensagensErro.add("Estoque atual deve ser um número inteiro")
        }

        if (mensagensErro.isNotEmpty()) {
            mostrarErrosValidacao(mensagensErro)
            return false
        }

        return true
    }

    private fun mostrarErrosValidacao(mensagens: List<String>) {
        val dialog = Dialog<ButtonType>()
        dialog.title = "Erro de validação"
        dialog.headerText = "Corrija os seguintes erros:"
        dialog.initStyle(StageStyle.UNDECORATED)

        val buttonTypeOk = ButtonType("OK", ButtonBar.ButtonData.OK_DONE)
        dialog.dialogPane.buttonTypes.add(buttonTypeOk)

        // Apply CSS
        dialog.dialogPane.stylesheets.addAll(this.stylesheets)

        // Create styled error messages
        val content = VBox(10.0).apply {
            padding = Insets(20.0)

            children.add(Label("Por favor, corrija os campos destacados:").apply {
                style = "-fx-font-weight: bold; -fx-font-size: 14px;"
            })

            // Add each error with bullet point and red text
            mensagens.forEach { mensagem ->
                children.add(Label("• $mensagem").apply {
                    style = "-fx-text-fill: #dc3545;"
                })
            }
        }

        // Style the dialog
        dialog.dialogPane.style = """
        -fx-background-color: white;
        -fx-border-color: #D3D3D3;
        -fx-border-width: 1px;
    """

        dialog.dialogPane.content = content

        // Style header
        dialog.dialogPane.lookup(".header-panel")?.style = """
        -fx-background-color: #2B2D30;
        -fx-background-radius: 0;
    """

        // Style header text
        val headerLabel = dialog.dialogPane.lookup(".header-panel .label") as? Label
        headerLabel?.style = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;"

        // Style the button
        val okButton = dialog.dialogPane.lookupButton(buttonTypeOk)
        okButton.styleClass.add("botao-cancel")

        // Configure button bar
        val buttonBar = dialog.dialogPane.lookup(".button-bar") as ButtonBar
        buttonBar.apply {
            buttonOrder = ButtonBar.BUTTON_ORDER_NONE
            buttonMinWidth = 100.0
            padding = Insets(0.0, 90.0, 0.0, 0.0)
            style = """
        -fx-background-color: white;
        -fx-alignment: center; 
    """
        }

        dialog.showAndWait()
    }

    private fun preencherFormulario(produto: Produto) {
        produtoSelecionado = produto

        tfCodigo.text = produto.codigo
        tfNome.text = produto.nome
        taDescricao.text = produto.descricao

        // Apply currency formatting to the value from the database
        var isUpdating = false
        isUpdating = true
        Platform.runLater {
            try {
                // Format the value as currency
                val value = produto.valorUnitario
                val formattedValue = String.format("%,.2f", value)
                    .replace(",", ".")
                    .replace(".", ",", ignoreCase = true)
                    .replaceFirst(",", ".")
                tfValorUnitario.text = "R$ $formattedValue"
            } finally {
                isUpdating = false
            }
        }

        if (produto.categoria.isNotEmpty()) {
            if (!cbCategoria.items.contains(produto.categoria)) {
                cbCategoria.items.add(produto.categoria)
            }
            cbCategoria.value = produto.categoria
        } else {
            cbCategoria.value = null
        }

        cbUnidadeMedida.value = produto.unidadeMedida
        tfEstoqueMinimo.text = produto.estoqueMinimo.toString()
        tfEstoqueAtual.text = produto.estoqueAtual.toString()
        cbStatus.value = produto.status
        cbEhInsumo.isSelected = produto.ehInsumo

        carregarInsumosDoProduto(produto.id)
        carregarInsumosDisponiveis()

        btnSalvar.text = "Atualizar"
        btnCancelar.isDisable = false
    }

    private fun carregarInsumosDoProduto(produtoId: Long) {
        insumosDosProdutos.clear()

        try {
            db.getConnection().use { conn ->
                val sql = """
                SELECT pi.insumo_id, p.nome, pi.quantidade, p.unidade_medida
                FROM produto_insumos pi
                JOIN produtos p ON pi.insumo_id = p.id
                WHERE pi.produto_id = ?
            """

                conn.prepareStatement(sql).use { stmt ->
                    stmt.setLong(1, produtoId)
                    val rs = stmt.executeQuery()

                    while (rs.next()) {
                        insumosDosProdutos.add(
                            InsumoQuantidade(
                                insumoId = rs.getLong("insumo_id"),
                                nome = rs.getString("nome"),
                                quantidade = rs.getDouble("quantidade"),
                                unidadeMedida = rs.getString("unidade_medida")
                            )
                        )
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            showAlert("Erro ao carregar insumos", e.message ?: "Erro desconhecido")
        }
    }

    private fun gerarCodigo(): String {
        val random = Random()
        return "PRD-${random.nextInt(10000, 99999)}"
    }

    private fun setupTableView() {
        tableView.apply {
            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            items = produtos

            val idColumn = TableColumn<Produto, Long>("ID").apply {
                cellValueFactory = PropertyValueFactory("id")
                prefWidth = 60.0
                isVisible = false
            }

            val codigoColumn = TableColumn<Produto, String>("Código").apply {
                cellValueFactory = PropertyValueFactory("codigo")
                prefWidth = 100.0
                minWidth = 100.0
                maxWidth = 100.0
            }

            val nomeColumn = TableColumn<Produto, String>("Nome").apply {
                cellValueFactory = PropertyValueFactory("nome")
                prefWidth = 250.0
                minWidth = 250.0
                maxWidth = 250.0
            }

            val categoriaColumn = TableColumn<Produto, String>("Categoria").apply {
                cellValueFactory = PropertyValueFactory("categoria")
                prefWidth = 150.0
                minWidth = 150.0
                maxWidth = 150.0
            }

            val valorColumn = TableColumn<Produto, Double>("Valor").apply {
                cellValueFactory = PropertyValueFactory("valorUnitario")
                prefWidth = 100.0
                minWidth = 100.0
                maxWidth = 100.0
                cellFactory = Callback {
                    object : TableCell<Produto, Double>() {
                        override fun updateItem(item: Double?, empty: Boolean) {
                            super.updateItem(item, empty)
                            if (empty || item == null) {
                                text = null
                            } else {
                                val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
                                text = format.format(item)
                            }
                        }
                    }
                }
            }

            val estoqueColumn = TableColumn<Produto, Int>("Estoque").apply {
                cellValueFactory = PropertyValueFactory("estoqueAtual")
                prefWidth = 80.0
                minWidth = 80.0
                maxWidth = 80.0
                cellFactory = Callback {
                    object : TableCell<Produto, Int>() {
                        override fun updateItem(item: Int?, empty: Boolean) {
                            super.updateItem(item, empty)
                            if (empty || item == null) {
                                text = null
                                style = ""
                            } else {
                                text = item.toString()

                                val row = tableRow
                                val rowItem = row?.item

                                style = if (rowItem != null && item <= rowItem.estoqueMinimo && rowItem.estoqueMinimo > 0) {
                                    "-fx-text-fill: white; -fx-background-color: #ff5252; -fx-font-weight: bold;"
                                } else {
                                    ""
                                }
                            }
                        }
                    }
                }
            }

            val statusColumn = TableColumn<Produto, String>("Status").apply {
                cellValueFactory = PropertyValueFactory("status")
                prefWidth = 80.0
                minWidth = 80.0
                maxWidth = 80.0
                cellFactory = Callback {
                    object : TableCell<Produto, String>() {
                        override fun updateItem(item: String?, empty: Boolean) {
                            super.updateItem(item, empty)
                            if (empty || item == null) {
                                text = null
                                graphic = null
                            } else {
                                text = item
                                style = when (item) {
                                    "Ativo" -> "-fx-text-fill: green;"
                                    "Inativo" -> "-fx-text-fill: red;"
                                    else -> ""
                                }
                            }
                        }
                    }
                }
            }

            val gerenciarEstoqueColumn = TableColumn<Produto, Void>("Gerenciar Estoque").apply {
                prefWidth = 250.0
                minWidth = 250.0
                maxWidth = 300.0

                cellFactory = Callback {
                    object : TableCell<Produto, Void>() {
                        private val btnEntrada = Button("Entrada").apply {
                            styleClass.add("small-button")
                            style = "-fx-background-color: #4CAF50; -fx-text-fill: white;"
                            prefWidth = 70.0
                        }

                        private val btnSaida = Button("Saída").apply {
                            styleClass.add("small-button")
                            style = "-fx-background-color: #FF9800; -fx-text-fill: white;"
                            prefWidth = 60.0
                        }

                        private val btnAjuste = Button("Ajuste").apply {
                            styleClass.add("small-button")
                            style = "-fx-background-color: #6056e8; -fx-text-fill: white;"
                            prefWidth = 60.0
                        }

                        private val box = HBox(5.0, btnEntrada, btnSaida, btnAjuste).apply {
                            alignment = Pos.CENTER
                        }

                        init {
                            btnEntrada.setOnAction {
                                val produto = tableRow.item
                                if (produto != null) {
                                    registrarMovimentacaoEstoqueProduto(produto, "entrada")
                                }
                            }

                            btnSaida.setOnAction {
                                val produto = tableRow.item
                                if (produto != null) {
                                    registrarMovimentacaoEstoqueProduto(produto, "saida")
                                }
                            }

                            btnAjuste.setOnAction {
                                val produto = tableRow.item
                                if (produto != null) {
                                    registrarMovimentacaoEstoqueProduto(produto, "ajuste")
                                }
                            }
                        }

                        override fun updateItem(item: Void?, empty: Boolean) {
                            super.updateItem(item, empty)
                            graphic = if (empty) null else box
                        }
                    }
                }
            }

            val acoesColumn = TableColumn<Produto, Void>("Ações").apply {
                prefWidth = 160.0
                minWidth = 150.0
                maxWidth = 160.0

                cellFactory = Callback {
                    object : TableCell<Produto, Void>() {
                        private val editBtn = Button("Editar").apply {
                            styleClass.add("small-button")
                            prefWidth = 70.0
                        }

                        private val deleteBtn = Button("Excluir").apply {
                            styleClass.add("small-button")
                            style = "-fx-background-color: #ff5252;"
                            prefWidth = 70.0
                        }

                        private val box = HBox(5.0, editBtn, deleteBtn).apply {
                            alignment = Pos.CENTER
                            padding = Insets(2.0)
                        }

                        init {
                            editBtn.setOnAction {
                                val produto = tableRow.item
                                if (produto != null) {
                                    preencherFormulario(produto)
                                    carregarInsumosDoProduto(produto.id)
                                }
                            }

                            deleteBtn.setOnAction {
                                val produto = tableRow.item
                                if (produto != null) {
                                    confirmDelete(produto)
                                }
                            }
                        }

                        override fun updateItem(item: Void?, empty: Boolean) {
                            super.updateItem(item, empty)
                            graphic = if (empty) null else box
                        }
                    }
                }

                style = "-fx-pref-width: 180px;"
            }

            columns.addAll(
                codigoColumn, nomeColumn, categoriaColumn, valorColumn,
                estoqueColumn, gerenciarEstoqueColumn, statusColumn, acoesColumn
            )

            selectionModel.selectedItemProperty().addListener { _, _, newSelection ->
                if (newSelection != null) {
                    produtoSelecionado = newSelection
                }
            }
        }
    }

    private fun registrarMovimentacaoEstoqueProduto(produto: Produto, tipo: String) {
        val dialog = Dialog<Map<String, Any>>()
        dialog.title = "Movimentação de Estoque"
        dialog.headerText = when(tipo) {
            "entrada" -> "Entrada de Estoque: ${produto.nome}"
            "saida" -> "Saída de Estoque: ${produto.nome}"
            else -> "Ajuste de Estoque: ${produto.nome}"
        }
        dialog.initStyle(StageStyle.UNDECORATED)

        val buttonTypeOk = ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE)
        val buttonTypeCancel = ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE)
        dialog.dialogPane.buttonTypes.addAll(buttonTypeOk, buttonTypeCancel)
        dialog.dialogPane.stylesheets.addAll(this.stylesheets)

        val tfQuantidade = TextField("1").apply {
            prefWidth = 80.0
            textProperty().addListener { _, _, newValue ->
                if (!newValue.matches(Regex("\\d*"))) {
                    text = newValue.replace(Regex("[^\\d]"), "")
                }
                if (text.isEmpty()) text = "1"
            }
        }

        val tfMotivo = TextField().apply {
            prefWidth = 300.0
            promptText = "Motivo da movimentação"
        }

        val content = VBox(10.0).apply {
            padding = Insets(20.0)
            prefWidth = 400.0

            children.addAll(
                Label("Quantidade:"),
                tfQuantidade,
                Label("Motivo:"),
                tfMotivo
            )
        }

        dialog.dialogPane.content = content
        dialog.dialogPane.style = """
        -fx-background-color: white;
        -fx-border-color: #D3D3D3;
        -fx-border-width: 1px;
    """

        dialog.dialogPane.lookup(".header-panel")?.style = """
        -fx-background-color: #2B2D30;
        -fx-background-radius: 0;
    """

        val headerLabel = dialog.dialogPane.lookup(".header-panel .label") as? Label
        headerLabel?.style = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;"

        val okButton = dialog.dialogPane.lookupButton(buttonTypeOk)
        val cancelButton = dialog.dialogPane.lookupButton(buttonTypeCancel)

        okButton.styleClass.add("primary-button")
        cancelButton.styleClass.add("secondary-button")

        val buttonBar = dialog.dialogPane.lookup(".button-bar") as ButtonBar
        buttonBar.apply {
            buttonOrder = ""
            buttonMinWidth = 100.0
            padding = Insets(0.0, 0.0, 0.0, 0.0)
            style = "-fx-background-color: white; -fx-alignment: center;"
        }

        dialog.setResultConverter {
            if (it == buttonTypeOk) {
                val quantidade = tfQuantidade.text.toIntOrNull() ?: 0
                val motivo = tfMotivo.text

                if (quantidade > 0) {
                    mapOf(
                        "produto" to produto,
                        "quantidade" to quantidade,
                        "motivo" to motivo,
                        "tipo" to tipo
                    )
                } else null
            } else null
        }

        val result = dialog.showAndWait()
        result.ifPresent { data ->
            processarMovimentacaoEstoque(
                data["produto"] as Produto,
                data["quantidade"] as Int,
                data["tipo"] as String,
                data["motivo"] as String
            )
        }
    }


    private fun processarMovimentacaoEstoque(produto: Produto, quantidade: Int, tipo: String, motivo: String) {
        try {
            db.getConnection().use { conn ->
                conn.autoCommit = false
                try {
                    // Get current stock
                    val stmtSelect = conn.prepareStatement("SELECT estoque_atual FROM produtos WHERE id = ?")
                    stmtSelect.setLong(1, produto.id)
                    val rs = stmtSelect.executeQuery()

                    if (rs.next()) {
                        val estoqueAtual = rs.getInt("estoque_atual")
                        var novoEstoque = estoqueAtual

                        // Calculate new stock based on movement type
                        when (tipo) {
                            "entrada" -> novoEstoque += quantidade
                            "saida" -> {
                                if (estoqueAtual < quantidade) {
                                    throw Exception("Estoque insuficiente para saída. Disponível: $estoqueAtual")
                                }
                                novoEstoque -= quantidade
                            }
                            "ajuste" -> novoEstoque = quantidade
                        }

                        // Update stock
                        val stmtUpdate = conn.prepareStatement(
                            "UPDATE produtos SET estoque_atual = ? WHERE id = ?"
                        )
                        stmtUpdate.setInt(1, novoEstoque)
                        stmtUpdate.setLong(2, produto.id)
                        stmtUpdate.executeUpdate()

                        val stmtMovimento = conn.prepareStatement("""
                        INSERT INTO movimentacao_estoque (
                            produto_id, quantidade_anterior, quantidade_nova, 
                            quantidade_movimentada, tipo_movimentacao, motivo, usuario
                        ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)
                        stmtMovimento.setLong(1, produto.id)
                        stmtMovimento.setInt(2, estoqueAtual)
                        stmtMovimento.setInt(3, novoEstoque)
                        stmtMovimento.setInt(4, quantidade)
                        stmtMovimento.setString(5, tipo)
                        stmtMovimento.setString(6, motivo)
                        stmtMovimento.setString(7, "Sistema") // In a real app, use logged user name
                        stmtMovimento.executeUpdate()

                        conn.commit()

                        // Update the local list
                        val index = produtos.indexOfFirst { it.id == produto.id }
                        if (index != -1) {
                            val updatedProduto = produto.copy(estoqueAtual = novoEstoque)
                            produtos[index] = updatedProduto
                            tableView.refresh()
                        }

                        mostrarMensagemSucesso(
                            "Movimentação Registrada",
                            "Movimentação de estoque registrada com sucesso!"
                        )
                    }
                } catch (e: Exception) {
                    conn.rollback()
                    throw e
                } finally {
                    conn.autoCommit = true
                }
            }
        } catch (e: SQLException) {
            showAlert("Erro na movimentação", e.message ?: "Erro ao processar movimentação de estoque")
        } catch (e: Exception) {
            showAlert("Erro na movimentação", e.message ?: "Erro ao processar movimentação de estoque")
        }
    }

    private fun mostrarHistoricoMovimentacoes() {
        val dialog = Dialog<Void>()
        dialog.title = "Histórico de Movimentações"
        dialog.headerText = "Histórico de Movimentações de Estoque"
        dialog.initStyle(StageStyle.UNDECORATED)

        val buttonTypeClose = ButtonType("Fechar", ButtonBar.ButtonData.CANCEL_CLOSE)
        dialog.dialogPane.buttonTypes.add(buttonTypeClose)
        dialog.dialogPane.stylesheets.addAll(this.stylesheets)

        val tableMovimentacoes = TableView<Map<String, Any>>()

        val colunas = listOf(
            TableColumn<Map<String, Any>, String>("Data/Hora").apply {
                cellValueFactory = Callback { param ->
                    javafx.beans.property.SimpleStringProperty(param.value["data"].toString())
                }
                prefWidth = 150.0
            },
            TableColumn<Map<String, Any>, String>("Produto").apply {
                cellValueFactory = Callback { param ->
                    javafx.beans.property.SimpleStringProperty(param.value["produto"].toString())
                }
                prefWidth = 200.0
            },
            TableColumn<Map<String, Any>, String>("Tipo").apply {
                cellValueFactory = Callback { param ->
                    javafx.beans.property.SimpleStringProperty(
                        when(param.value["tipo"]) {
                            "entrada" -> "Entrada"
                            "saida" -> "Saída"
                            "ajuste" -> "Ajuste"
                            else -> param.value["tipo"].toString()
                        }
                    )
                }
                prefWidth = 100.0
            },
            TableColumn<Map<String, Any>, Int>("Qtd. Anterior").apply {
                cellValueFactory = Callback { param ->
                    javafx.beans.property.SimpleIntegerProperty(param.value["anterior"] as Int).asObject()
                }
                prefWidth = 100.0
                style = "-fx-alignment: CENTER-RIGHT;"
            },
            TableColumn<Map<String, Any>, Int>("Qtd. Nova").apply {
                cellValueFactory = Callback { param ->
                    javafx.beans.property.SimpleIntegerProperty(param.value["nova"] as Int).asObject()
                }
                prefWidth = 100.0
                style = "-fx-alignment: CENTER-RIGHT;"
            },
            TableColumn<Map<String, Any>, String>("Motivo").apply {
                cellValueFactory = Callback { param ->
                    javafx.beans.property.SimpleStringProperty(param.value["motivo"].toString())
                }
                prefWidth = 200.0
            }
        )

        tableMovimentacoes.columns.addAll(colunas)
        tableMovimentacoes.prefHeight = 400.0
        tableMovimentacoes.prefWidth = 850.0

        // Load data
        val movimentacoes = carregarHistoricoMovimentacoes()
        tableMovimentacoes.items = FXCollections.observableArrayList(movimentacoes)

        val content = VBox(10.0).apply {
            padding = Insets(20.0)
            children.add(tableMovimentacoes)
        }

        dialog.dialogPane.content = content
        dialog.dialogPane.style = """
        -fx-background-color: white;
        -fx-border-color: #D3D3D3;
        -fx-border-width: 1px;
    """

        dialog.dialogPane.lookup(".header-panel")?.style = """
        -fx-background-color: #2B2D30;
        -fx-background-radius: 0;
    """

        val headerLabel = dialog.dialogPane.lookup(".header-panel .label") as? Label
        headerLabel?.style = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;"

        val closeButton = dialog.dialogPane.lookupButton(buttonTypeClose)
        closeButton.styleClass.add("secondary-button")

        // Fix the button bar alignment
        val buttonBar = dialog.dialogPane.lookup(".button-bar") as ButtonBar
        buttonBar.apply {
            buttonOrder = ""
            buttonMinWidth = 100.0
            padding = Insets(0.0, 0.0, 0.0, 0.0)
            style = "-fx-background-color: white; -fx-alignment: center;"
        }

        dialog.showAndWait()
    }


    private fun carregarHistoricoMovimentacoes(): List<Map<String, Any>> {
        val movimentacoes = mutableListOf<Map<String, Any>>()

        try {
            db.getConnection().use { conn ->
                val stmt = conn.prepareStatement("""
                SELECT m.*, p.nome as nome_produto
                FROM movimentacao_estoque m
                JOIN produtos p ON m.produto_id = p.id
                ORDER BY m.data_movimentacao DESC
                LIMIT 100
            """)

                val rs = stmt.executeQuery()
                while (rs.next()) {
                    movimentacoes.add(mapOf(
                        "id" to rs.getLong("id"),
                        "produto" to rs.getString("nome_produto"),
                        "anterior" to rs.getInt("quantidade_anterior"),
                        "nova" to rs.getInt("quantidade_nova"),
                        "movimentada" to rs.getInt("quantidade_movimentada"),
                        "tipo" to rs.getString("tipo_movimentacao"),
                        "motivo" to (rs.getString("motivo") ?: ""),
                        "usuario" to (rs.getString("usuario") ?: ""),
                        "data" to rs.getString("data_movimentacao")
                    ))
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }

        return movimentacoes
    }

    private fun verificarAlertasEstoque() {
        val produtosBaixoEstoque = mutableListOf<Produto>()

        try {
            db.getConnection().use { conn ->
                val stmt = conn.prepareStatement(
                    "SELECT * FROM produtos WHERE estoque_atual <= estoque_minimo AND estoque_minimo > 0 AND status = 'Ativo'"
                )
                val rs = stmt.executeQuery()

                while (rs.next()) {
                    produtosBaixoEstoque.add(
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
                            ehInsumo = rs.getInt("eh_insumo") == 1
                        )
                    )
                }
            }

            if (produtosBaixoEstoque.isNotEmpty()) {
                showLowStockAlert(produtosBaixoEstoque)
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    private fun showLowStockAlert(produtos: List<Produto>) {
        val dialog = Dialog<ButtonType>()
        dialog.title = "Alerta de Estoque"
        dialog.headerText = "Produtos com Estoque Baixo"
        dialog.initStyle(StageStyle.UNDECORATED)

        val buttonTypeOk = ButtonType("OK", ButtonBar.ButtonData.OK_DONE)
        dialog.dialogPane.buttonTypes.add(buttonTypeOk)
        dialog.dialogPane.stylesheets.addAll(this.stylesheets)

        val content = VBox(10.0).apply {
            padding = Insets(20.0)
            prefWidth = 500.0

            children.add(Label("Os seguintes produtos estão com estoque abaixo do mínimo:").apply {
                style = "-fx-font-size: 14px; -fx-font-weight: bold;"
            })

            val table = TableView<Produto>().apply {
                items = FXCollections.observableArrayList(produtos)
                columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
                prefHeight = 250.0

                columns.addAll(
                    TableColumn<Produto, String>("Código").apply {
                        cellValueFactory = PropertyValueFactory("codigo")
                        prefWidth = 80.0
                    },
                    TableColumn<Produto, String>("Nome").apply {
                        cellValueFactory = PropertyValueFactory("nome")
                        prefWidth = 150.0
                    },
                    TableColumn<Produto, Int>("Estoque Atual").apply {
                        cellValueFactory = PropertyValueFactory("estoqueAtual")
                        prefWidth = 100.0
                        style = "-fx-alignment: CENTER-RIGHT;"
                    },
                    TableColumn<Produto, Int>("Estoque Mínimo").apply {
                        cellValueFactory = PropertyValueFactory("estoqueMinimo")
                        prefWidth = 100.0
                        style = "-fx-alignment: CENTER-RIGHT;"
                    }
                )
            }

            children.add(table)
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

        val okButton = dialog.dialogPane.lookupButton(buttonTypeOk)
        okButton.styleClass.add("primary-button")

        val buttonBar = dialog.dialogPane.lookup(".button-bar") as ButtonBar
        buttonBar.apply {
            buttonOrder = ""
            buttonMinWidth = 100.0
            style = "-fx-background-color: white; -fx-alignment: center;"
            padding = Insets(0.0, 0.0, 0.0, 0.0)
        }

        dialog.showAndWait()
    }

    private lateinit var imgProduto: ImageView
    private var produtoImagePath: String? = null

    private fun setupImageUpload(): HBox {
        imgProduto = ImageView().apply {
            fitHeight = 100.0
            fitWidth = 100.0
            isPreserveRatio = true
            style = """
            -fx-border-color: #dfe1e6;
            -fx-border-width: 1px;
            -fx-border-radius: 3px;
            -fx-background-color: #f4f5f7;
        """

            // Default image if no product image is selected
            image = Image(javaClass.getResourceAsStream("/icons/no_image.png") ?: return@apply)
        }

        val btnSelecionarImagem = Button("Selecionar Imagem").apply {
            styleClass.add("secondary-button")
            setOnAction {
                val fileChooser = javafx.stage.FileChooser()
                fileChooser.title = "Selecionar Imagem do Produto"
                fileChooser.extensionFilters.addAll(
                    javafx.stage.FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg")
                )

                val selectedFile = fileChooser.showOpenDialog(scene.window)
                if (selectedFile != null) {
                    try {
                        val image = Image(selectedFile.toURI().toString())
                        imgProduto.image = image
                        produtoImagePath = selectedFile.absolutePath
                    } catch (e: Exception) {
                        showAlert("Erro ao carregar imagem", "Não foi possível carregar a imagem selecionada.")
                    }
                }
            }
        }

        val btnRemoverImagem = Button("Remover").apply {
            styleClass.add("secondary-button")
            style = "-fx-background-color: #ff5252; -fx-text-fill: white;"
            setOnAction {
                imgProduto.image = Image(javaClass.getResourceAsStream("/icons/no_image.png") ?: return@setOnAction)
                produtoImagePath = null
            }
        }

        return HBox(10.0, imgProduto, VBox(10.0, btnSelecionarImagem, btnRemoverImagem)).apply {
            alignment = Pos.CENTER_LEFT
        }
    }

    private lateinit var tfCodigoBarras: TextField

    private fun setupBarcodeSection(): VBox {
        val section = VBox(10.0)

        val title = Label("Código de Barras").apply {
            style = "-fx-font-weight: bold; -fx-font-size: 14px;"
        }

        tfCodigoBarras = TextField().apply {
            promptText = "Código de barras"
            isEditable = true
            prefHeight = 36.0
        }

        val btnGerarCodigoBarras = Button("Gerar").apply {
            styleClass.add("primary-button")
            setOnAction {
                // Generate a random EAN-13 barcode
                val random = Random()
                val digits = (1..12).map { random.nextInt(10) }

                // Calculate check digit
                val sum = digits.mapIndexed { index, digit ->
                    digit * if (index % 2 == 0) 1 else 3
                }.sum()
                val checkDigit = (10 - (sum % 10)) % 10

                val digitStr = digits.joinToString("") + checkDigit

                tfCodigoBarras.text = digitStr
            }
        }

        val btnValidarCodigo = Button("Validar").apply {
            styleClass.add("secondary-button")
            setOnAction {
                val codigo = tfCodigoBarras.text
                if (codigo.matches(Regex("\\d{13}"))) {
                    showAlert("Código Válido", "O código de barras é válido.", Alert.AlertType.INFORMATION)
                } else {
                    showAlert("Código Inválido", "O código de barras deve conter 13 dígitos numéricos.")
                }
            }
        }

        val controlsBox = HBox(10.0, tfCodigoBarras, btnGerarCodigoBarras, btnValidarCodigo).apply {
            alignment = Pos.CENTER_LEFT
        }

        section.children.addAll(title, controlsBox)
        return section
    }

    private lateinit var dashboardContainer: VBox
    private lateinit var lblTotalProdutos: Label
    private lateinit var lblBaixoEstoque: Label
    private lateinit var lblValorEstoque: Label
    private lateinit var lblSemMovimentacao: Label

    private fun createStockDashboard(): VBox {
        dashboardContainer = VBox(5.0).apply {
            padding = Insets(10.0)
            style = """
            -fx-background-color: white;
            -fx-border-color: #dfe1e6;
            -fx-border-width: 1px;
            -fx-border-radius: 3px;
        """
        }

        val title = Label("Dashboard de Estoque").apply {
            style = "-fx-font-weight: bold; -fx-font-size: 14px;"
        }

        lblTotalProdutos = Label("0")
        lblBaixoEstoque = Label("0")
        lblValorEstoque = Label("R$ 0,00")
        lblSemMovimentacao = Label("0")

        val cardsContainer = HBox(10.0).apply {
            alignment = Pos.CENTER_LEFT
            children.addAll(
                createKpiCard("Total de Produtos", lblTotalProdutos, "#4CAF50"),
                createKpiCard("Produtos com Estoque Baixo", lblBaixoEstoque, "#FFA500"),
                createKpiCard("Valor Total em Estoque", lblValorEstoque, "#2196F3"),
                createKpiCard("Produtos sem Movimentação", lblSemMovimentacao, "#9C27B0")
            )
        }

        dashboardContainer.children.addAll(title, cardsContainer)
        updateDashboard()

        return dashboardContainer
    }

    private fun createKpiCard(title: String, valueLabel: Label, color: String): HBox {
        val card = HBox(8.0).apply {
            padding = Insets(8.0)
            prefWidth = 180.0
            style = """
            -fx-background-color: white;
            -fx-border-color: $color;
            -fx-border-width: 0 0 0 3px;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);
        """
        }

        val textContainer = VBox(2.0)
        textContainer.children.addAll(
            Label(title).apply {
                style = "-fx-font-size: 11px; -fx-text-fill: #555;"
                wrapTextProperty().set(true)
            },
            valueLabel.apply {
                style = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: $color;"
            }
        )

        card.children.add(textContainer)
        return card
    }

    fun processarEstoqueInsumos(produtoId: Long, quantidade: Int) {
        try {
            DatabaseHelper().getConnection().use { conn ->
                // Buscar todos os insumos utilizados pelo produto
                val stmtInsumos = conn.prepareStatement("""
                SELECT pi.insumo_id, pi.quantidade, p.nome 
                FROM produto_insumos pi
                JOIN produtos p ON p.id = pi.insumo_id
                WHERE pi.produto_id = ?
            """)
                stmtInsumos.setLong(1, produtoId)
                val rsInsumos = stmtInsumos.executeQuery()

                // Para cada insumo, calcular a quantidade total usada
                while (rsInsumos.next()) {
                    val insumoId = rsInsumos.getLong("insumo_id")
                    val qtdPorProduto = rsInsumos.getDouble("quantidade")
                    val insumoNome = rsInsumos.getString("nome")

                    val qtdTotalReduzir = qtdPorProduto * quantidade

                    val updateStmt = conn.prepareStatement(
                        "UPDATE produtos SET estoque_atual = estoque_atual - ? WHERE id = ?"
                    )
                    updateStmt.setDouble(1, qtdTotalReduzir)
                    updateStmt.setLong(2, insumoId)
                    val rowsUpdated = updateStmt.executeUpdate()

                    if (rowsUpdated > 0) {
                        println("Estoque do insumo $insumoNome reduzido em $qtdTotalReduzir unidades")
                    }

                    // Verificar se estoque ficou negativo ou abaixo do mínimo
                    verificarEstoque(insumoId, conn)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Tratar erro conforme necessário
        }
    }

    private fun verificarEstoque(produtoId: Long, conn: Connection) {
        val stmt = conn.prepareStatement(
            "SELECT nome, estoque_atual, estoque_minimo FROM produtos WHERE id = ?"
        )
        stmt.setLong(1, produtoId)
        val rs = stmt.executeQuery()

        if (rs.next()) {
            val nome = rs.getString("nome")
            val estoqueAtual = rs.getInt("estoque_atual")
            val estoqueMinimo = rs.getInt("estoque_minimo")

            if (estoqueAtual < 0) {
                println("ALERTA: Estoque do produto $nome está negativo: $estoqueAtual")
                // Implementar notificação ao usuário
            } else if (estoqueAtual < estoqueMinimo) {
                println("ALERTA: Estoque do produto $nome está abaixo do mínimo: $estoqueAtual (mínimo: $estoqueMinimo)")
                // Implementar notificação ao usuário
            }
        }
    }

    private fun loadProductsFromDb(): List<Produto> {
        val produtosList = mutableListOf<Produto>()
        try {
            db.getConnection().use { conn ->
                val stmt = conn.createStatement()
                val rs = stmt.executeQuery("SELECT * FROM produtos ORDER BY nome")

                var count = 0
                while (rs.next()) {
                    count++
                    produtosList.add(
                        Produto(
                            id = rs.getLong("id"),
                            codigo = rs.getString("codigo"),
                            nome = rs.getString("nome"),
                            descricao = rs.getString("descricao") ?: "",
                            valorUnitario = rs.getDouble("valor_unitario"),
                            categoria = rs.getString("categoria") ?: "",
                            ehInsumo = rs.getInt("eh_insumo") == 1,
                            unidadeMedida = rs.getString("unidade_medida"),
                            estoqueMinimo = rs.getInt("estoque_minimo"),
                            estoqueAtual = rs.getInt("estoque_atual"),
                            status = rs.getString("status"),
                            dataCadastro = rs.getString("data_cadastro"),
                            dataAtualizacao = rs.getString("data_atualizacao")
                        )
                    )
                }
                println("Loaded $count products from database")
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            showAlert("Erro ao carregar produtos", e.message ?: "Erro ao acessar banco de dados")
        }
        return produtosList
    }

    private fun loadProducts() {
        produtos.setAll(loadProductsFromDb())
        updateDashboard()
    }

    private fun updateDashboard() {
        try {
            db.getConnection().use { conn ->
                // Calculate total products
                val totalStmt = conn.prepareStatement("SELECT COUNT(*) FROM produtos")
                totalStmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        lblTotalProdutos.text = rs.getInt(1).toString()
                    }
                }

                // Calculate low stock products
                val lowStockStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM produtos WHERE estoque_atual <= estoque_minimo AND estoque_minimo > 0"
                )
                lowStockStmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        lblBaixoEstoque.text = rs.getInt(1).toString()
                    }
                }

                // Calculate total stock value
                val valueStmt = conn.prepareStatement(
                    "SELECT SUM(valor_unitario * estoque_atual) FROM produtos"
                )
                valueStmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val totalValue = rs.getDouble(1)
                        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
                        lblValorEstoque.text = formatter.format(totalValue)
                    }
                }

                // Products without movement in last 30 days
                val noMovementStmt = conn.prepareStatement("""
                SELECT COUNT(*) FROM produtos
                WHERE id NOT IN (
                    SELECT DISTINCT produto_id FROM movimentacao_estoque
                    WHERE data_movimentacao >= datetime('now', '-30 day')
                )
            """)
                noMovementStmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        lblSemMovimentacao.text = rs.getInt(1).toString()
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    private fun searchProducts(term: String) {
        produtos.clear()
        try {
            db.getConnection().use { conn ->
                val stmt = conn.prepareStatement(
                    """SELECT * FROM produtos 
                       WHERE nome LIKE ? OR codigo LIKE ? OR categoria LIKE ?
                       ORDER BY nome"""
                )
                val searchTerm = "%$term%"
                stmt.setString(1, searchTerm)
                stmt.setString(2, searchTerm)
                stmt.setString(3, searchTerm)

                val rs = stmt.executeQuery()
                while (rs.next()) {
                    produtos.add(
                        Produto(
                            id = rs.getLong("id"),
                            codigo = rs.getString("codigo"),
                            nome = rs.getString("nome"),
                            descricao = rs.getString("descricao") ?: "",
                            valorUnitario = rs.getDouble("valor_unitario"),
                            categoria = rs.getString("categoria") ?: "",
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
        } catch (e: SQLException) {
            showAlert("Erro na busca", e.message ?: "Erro ao acessar banco de dados")
        }
    }

    private fun confirmDelete(produto: Produto) {
        val dialog = Dialog<ButtonType>()
        dialog.title = "Confirmar Exclusão"
        dialog.headerText = "Excluir Produto"
        dialog.initStyle(StageStyle.UNDECORATED)

        val buttonTypeConfirm = ButtonType("Excluir", ButtonBar.ButtonData.OK_DONE)
        val buttonTypeCancel = ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE)
        dialog.dialogPane.buttonTypes.addAll(buttonTypeConfirm, buttonTypeCancel)

        dialog.dialogPane.stylesheets.addAll(this.stylesheets)

        val content = VBox(10.0).apply {
            padding = Insets(20.0)
            children.add(Label("Tem certeza que deseja excluir o produto: ${produto.nome}?").apply {
                style = "-fx-font-size: 14px;"
            })
        }

        dialog.dialogPane.style = """
        -fx-background-color: white;
        -fx-border-color: #D3D3D3;
        -fx-border-width: 1px;
    """

        dialog.dialogPane.content = content

        dialog.dialogPane.lookup(".header-panel")?.style = """
        -fx-background-color: #dc3545;
        -fx-background-radius: 0;
    """


        val headerLabel = dialog.dialogPane.lookup(".header-panel .label") as? Label
        headerLabel?.style = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;"


        val cancelButton = dialog.dialogPane.lookupButton(buttonTypeCancel)
        cancelButton.styleClass.add("secondary-button")

        val confirmButton = dialog.dialogPane.lookupButton(buttonTypeConfirm)
        confirmButton.styleClass.add("botao-cancel")
// Add inline style to ensure red color
        confirmButton.style = """
    -fx-background-color: #dc3545;
    -fx-text-fill: white;
    -fx-font-weight: bold;
"""



        // Configure button bar
        val buttonBar = dialog.dialogPane.lookup(".button-bar") as ButtonBar
        buttonBar.apply {
            buttonOrder = ButtonBar.BUTTON_ORDER_NONE
            buttonMinWidth = 100.0
            style = """
            -fx-background-color: white;
            -fx-alignment: center;
        """
            padding = Insets(0.0, 55.0, 0.0, 0.0)
        }

        val result = dialog.showAndWait()
        if (result.isPresent && result.get() == buttonTypeConfirm) {
            deleteProduct(produto)
        }
    }

    private fun atualizarEstoqueProduto(produtoId: Long, quantidade: Int) {
        try {
            DatabaseHelper().getConnection().use { conn ->
                val stmt = conn.prepareStatement(
                    "UPDATE produtos SET estoque_atual = estoque_atual - ? WHERE id = ?"
                )
                stmt.setInt(1, quantidade)
                stmt.setLong(2, produtoId)
                stmt.executeUpdate()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buscarItensPedido(pedidoId: Long): List<Map<String, Any>> {
        val itens = mutableListOf<Map<String, Any>>()
        try {
            db.getConnection().use { conn ->
                val stmt = conn.prepareStatement(
                    """SELECT ip.produto_id, ip.quantidade 
                   FROM itens_pedido ip 
                   WHERE ip.pedido_id = ?"""
                )
                stmt.setLong(1, pedidoId)
                val rs = stmt.executeQuery()

                while (rs.next()) {
                    val item = mutableMapOf<String, Any>()
                    item["id"] = rs.getLong("produto_id")
                    item["quantidade"] = rs.getInt("quantidade")
                    itens.add(item)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return itens
    }


    private fun deleteProduct(produto: Produto) {
        try {
            db.getConnection().use { conn ->
                val stmt = conn.prepareStatement("DELETE FROM produtos WHERE id = ?")
                stmt.setLong(1, produto.id)
                stmt.executeUpdate()
            }

            loadProducts()
            showAlert("Sucesso", "Produto excluído com sucesso", Alert.AlertType.INFORMATION)
        } catch (e: SQLException) {
            if (e.message?.contains("foreign key constraint") == true) {
                showAlert("Erro ao excluir", "Este produto não pode ser excluído pois está sendo usado em pedidos.")
            } else {
                showAlert("Erro ao excluir", e.message ?: "Erro ao acessar banco de dados")
            }
        }
    }

    private fun showAlert(title: String, message: String, type: Alert.AlertType = Alert.AlertType.ERROR) {
        Alert(type).apply {
            this.title = title
            this.headerText = null
            this.contentText = message
            showAndWait()
        }
    }
}