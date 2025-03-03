package com.sistema_pedidos.view

import com.sistema_pedidos.database.DatabaseHelper
import com.sistema_pedidos.model.Produto
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
        // Top Section
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

                // Add hover effects after getting button references
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

        val topControls = HBox(10.0, searchField, searchButton, refreshButton, newButton, newCategoryButton).apply {
            alignment = Pos.CENTER_LEFT
        }



        val headerBox = VBox(15.0, title, topControls).apply {
            padding = Insets(0.0, 0.0, 15.0, 0.0)
        }

        setTop(headerBox)

        // Center - Table and Form side by side
        val splitPane = HBox(20.0).apply {
            children.addAll(createTableView(), createFormPanel())
            HBox.setHgrow(tableView, Priority.ALWAYS)
        }

        setCenter(splitPane)
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

        // Set growth properties
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
            promptText = "0.00"
            styleFormField(this)
        }

        // Move cbEhInsumo declaration outside of the cbCategoria block
        cbEhInsumo = CheckBox("Este produto também pode ser usado como insumo").apply {
            isSelected = false
        }

        cbCategoria = ComboBox<String>().apply {
            items = FXCollections.observableArrayList(loadCategories())
            isEditable = true
            promptText = "Selecione ou digite uma categoria"
            prefWidth = Double.MAX_VALUE

            // Add listener to save new categories when focus is lost
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
        }

        tfEstoqueAtual = TextField().apply {
            promptText = "0"
            text = "0"
            styleFormField(this)
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
                        // Existing code for actions column
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
        // Find the spinner by traversing the hierarchy
        val quantidadeSpinner = formPanel.lookupAll(".spinner").firstOrNull() as? Spinner<Int>

        if (insumoSelecionado == null) {
            showAlert("Insumo não selecionado", "Por favor, selecione um insumo.", Alert.AlertType.WARNING)
            return
        }

        if (quantidadeSpinner == null) {
            showAlert("Erro", "Não foi possível encontrar o campo de quantidade.", Alert.AlertType.ERROR)
            return
        }

        val quantidade = quantidadeSpinner.value

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
                quantidade = quantidade.toDouble(),
                unidadeMedida = insumoSelecionado.unidadeMedida
            )
        )

        // Reset the selection
        cbInsumoDisponivel.selectionModel.clearSelection()
        quantidadeSpinner.valueFactory.value = 1
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


    // Form fields
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
            val valorUnitario = tfValorUnitario.text.replace(",", ".").toDoubleOrNull() ?: 0.0
            val estoqueMinimo = tfEstoqueMinimo.text.toIntOrNull() ?: 0
            val estoqueAtual = tfEstoqueAtual.text.toIntOrNull() ?: 0
            val ehInsumo = if (cbEhInsumo.isSelected) 1 else 0

            // First, try to add the column if it doesn't exist
            try {
                db.getConnection().use { conn ->
                    conn.createStatement().execute(
                        "ALTER TABLE produtos ADD COLUMN eh_insumo INTEGER DEFAULT 0 CHECK (eh_insumo IN (0, 1))"
                    )
                }
            } catch (e: SQLException) {
                // Column might already exist, ignore the error
            }

            db.getConnection().use { conn ->
                conn.autoCommit = false
                try {
                    var produtoId = produtoSelecionado?.id

                    if (produtoId == null) {
                        // Insert new product
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
                        // Update existing product
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
                tfValorUnitario.text.replace(",", ".").toDouble()
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
        tfValorUnitario.text = produto.valorUnitario.toString()
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

        // Carregar insumos do produto
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
                prefWidth = 120.0
            }

            val nomeColumn = TableColumn<Produto, String>("Nome").apply {
                cellValueFactory = PropertyValueFactory("nome")
                prefWidth = 250.0
            }

            val categoriaColumn = TableColumn<Produto, String>("Categoria").apply {
                cellValueFactory = PropertyValueFactory("categoria")
                prefWidth = 150.0
            }

            val valorColumn = TableColumn<Produto, Double>("Valor").apply {
                cellValueFactory = PropertyValueFactory("valorUnitario")
                prefWidth = 100.0
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
            }

            val statusColumn = TableColumn<Produto, String>("Status").apply {
                cellValueFactory = PropertyValueFactory("status")
                prefWidth = 80.0
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

            val acoesColumn = TableColumn<Produto, Void>("Ações").apply {
                prefWidth = 160.0  // Reduced from 180.0
                minWidth = 150.0   // Adjusted to be closer to actual content width
                maxWidth = 160.0   // Added max width to prevent column from growing too large

                cellFactory = Callback {
                    object : TableCell<Produto, Void>() {
                        private val editBtn = Button("Editar").apply {
                            styleClass.add("small-button")
                            prefWidth = 70.0  // Slightly smaller buttons
                        }

                        private val deleteBtn = Button("Excluir").apply {
                            styleClass.add("small-button")
                            style = "-fx-background-color: #ff5252;"
                            prefWidth = 70.0  // Fixed width for button
                        }

                        private val box = HBox(5.0, editBtn, deleteBtn).apply {  // Increased spacing
                            alignment = Pos.CENTER
                            padding = Insets(2.0)  // Added padding around buttons
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

                // Make this column have a higher growth priority
                style = "-fx-pref-width: 180px;"
            }

            columns.addAll(
                codigoColumn, nomeColumn, categoriaColumn, valorColumn,
                estoqueColumn, statusColumn, acoesColumn
            )

            // Add selection listener to load insumos when a product is selected
            selectionModel.selectedItemProperty().addListener { _, _, newSelection ->
                if (newSelection != null) {
                    produtoSelecionado = newSelection
                    preencherFormulario(newSelection)
                    carregarInsumosDoProduto(newSelection.id)
                }
            }
        }
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

                    // Calcular quantidade total a ser reduzida
                    val qtdTotalReduzir = qtdPorProduto * quantidade

                    // Atualizar estoque do insumo
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

    private fun loadProducts() {
        produtos.clear()
        try {
            db.getConnection().use { conn ->
                val stmt = conn.createStatement()
                val rs = stmt.executeQuery("SELECT * FROM produtos ORDER BY nome")

                var count = 0
                while (rs.next()) {
                    count++
                    produtos.add(
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
                println("Loaded $count products from database") // Debug statement
            }
        } catch (e: SQLException) {
            e.printStackTrace() // Print full stack trace for debugging
            showAlert("Erro ao carregar produtos", e.message ?: "Erro ao acessar banco de dados")
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
            mostrarMensagemEstilizada("Produto excluído com sucesso", "O produto foi removido com sucesso do sistema.", "success")
        } catch (e: SQLException) {
            if (e.message?.contains("foreign key constraint") == true) {
                mostrarMensagemEstilizada("Erro ao excluir", "Este produto não pode ser excluído pois está sendo usado em pedidos.")
            } else {
                mostrarMensagemEstilizada("Erro ao excluir", e.message ?: "Erro ao acessar banco de dados")
            }
        }
    }

    private fun mostrarMensagemEstilizada(titulo: String, mensagem: String, tipo: String = "error") {
        val dialog = Dialog<ButtonType>()
        dialog.title = if (tipo == "success") "Sucesso" else "Erro"
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
                isWrapText = true
            })
        }

        // Style the dialog
        dialog.dialogPane.style = """
        -fx-background-color: white;
        -fx-border-color: #D3D3D3;
        -fx-border-width: 1px;
    """

        dialog.dialogPane.content = content

        // Set header color based on type
        val headerColor = when (tipo) {
            "success" -> "#4CAF50" // Green for success
            else -> "#dc3545" // Red for errors
        }

        dialog.dialogPane.lookup(".header-panel")?.style = """
        -fx-background-color: $headerColor;
        -fx-background-radius: 0;
    """

        // Style header text
        val headerLabel = dialog.dialogPane.lookup(".header-panel .label") as? Label
        headerLabel?.style = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;"

        // Style the button
        val okButton = dialog.dialogPane.lookupButton(buttonTypeOk)
        okButton.styleClass.add(if (tipo == "success") "primary-button" else "botao-cancel")

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

    private fun showAlert(title: String, message: String, type: Alert.AlertType = Alert.AlertType.ERROR) {
        Alert(type).apply {
            this.title = title
            this.headerText = null
            this.contentText = message
            showAndWait()
        }
    }
}