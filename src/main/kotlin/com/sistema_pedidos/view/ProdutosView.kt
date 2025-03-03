package com.sistema_pedidos.view

import com.sistema_pedidos.database.DatabaseHelper
import com.sistema_pedidos.model.Produto
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.util.Callback
import java.awt.event.ActionEvent
import java.sql.SQLException
import java.text.NumberFormat
import java.util.*

data class UnidadeMedida(val codigo: String, val descricao: String) {
    override fun toString() = "$codigo - $descricao"
}

class ProdutosView : BorderPane() {
    private val tableView = TableView<Produto>()
    private val produtos = FXCollections.observableArrayList<Produto>()
    private val searchField = TextField()
    private val formPanel = VBox(10.0)
    private var produtoSelecionado: Produto? = null
    private val db = DatabaseHelper()

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
            prefHeight = 36.0  // Match HistoricoPedidosView height
            setOnAction { searchProducts(searchField.text) }
        }

        val newButton = Button("Novo Produto").apply {
            styleClass.add("primary-button")
            prefHeight = 36.0  // Match HistoricoPedidosView height
            setOnAction {
                limparFormulario()
                produtoSelecionado = null
            }
        }

        // In the setupUI() method, after creating the newButton
        val newCategoryButton = Button("Nova Categoria").apply {
            styleClass.add("primary-button")
            prefHeight = 36.0
            setOnAction {
                // Reuse the same dialog logic from the category context menu
                val dialog = TextInputDialog()
                dialog.title = "Nova Categoria"
                dialog.headerText = "Adicionar Nova Categoria"
                dialog.contentText = "Nome da categoria:"

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

        val topControls = HBox(10.0, searchField, searchButton, newButton, newCategoryButton).apply {
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

        // Call setupTableView to add columns
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
            // Optional: add a tooltip
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
            setWrapText(true) // Use method instead of direct property access
            style = """
        -fx-control-inner-background: white;
        -fx-background-color: white;
        -fx-background-radius: 4px;
        -fx-border-color: #cccccc;
        -fx-border-radius: 4px;
        /* Show vertical scrollbar as needed */
        -fx-scroll-bar-policy: as-needed;
    """
        }

        tfValorUnitario = TextField().apply {
            promptText = "0.00"
            styleFormField(this)
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

// Then in the formPanel.children.addAll(), replace the cbCategoria line with:
        Label("Categoria:"); VBox(5.0, cbCategoria, categoryButtonsBox)

        cbUnidadeMedida = ComboBox<String>().apply {
            items = FXCollections.observableArrayList(
                "UN",  // Exact values as per database constraint
                "KG",
                "L",
                "M",
                "CX"
            )
            value = "UN"  // Default value
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

        // Buttons
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

        val scrollPane = ScrollPane().apply {
            isFitToWidth = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
            style = "-fx-background-color: transparent;"
        }

        formPanel.apply {
            padding = Insets(15.0)
            spacing = 10.0
            prefWidth = 320.0
            maxWidth = 320.0
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
                Label("Unidade Medida:"), cbUnidadeMedida,
                Label("Estoque Mínimo:"), tfEstoqueMinimo,
                Label("Estoque Atual:"), tfEstoqueAtual,
                Label("Status:"), cbStatus,
                Separator().apply { padding = Insets(5.0, 0.0, 5.0, 0.0) },
                HBox(10.0, btnSalvar, btnCancelar).apply {
                    alignment = Pos.CENTER
                    padding = Insets(10.0, 0.0, 0.0, 0.0)
                }
            )
        }

        // Put the form in a scroll pane
        scrollPane.content = formPanel

        // Return a container with the scroll pane
        return VBox(scrollPane).apply {
            HBox.setHgrow(this, Priority.NEVER)
        }
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

            db.getConnection().use { conn ->
                if (produtoSelecionado == null) {
                    // Insert new product
                    val sql = """
                    INSERT INTO produtos (codigo, nome, descricao, valor_unitario, categoria,
                    unidade_medida, estoque_minimo, estoque_atual, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """

                    val stmt = conn.prepareStatement(sql)
                    stmt.setString(1, gerarCodigo())
                    stmt.setString(2, tfNome.text.trim())
                    stmt.setString(3, taDescricao.text.trim())
                    stmt.setDouble(4, valorUnitario)
                    stmt.setString(5, cbCategoria.value ?: "")
                    stmt.setString(6, cbUnidadeMedida.value)  // This was the issue
                    stmt.setInt(7, estoqueMinimo)
                    stmt.setInt(8, estoqueAtual)
                    stmt.setString(9, cbStatus.value)

                    stmt.executeUpdate()
                    showAlert("Sucesso", "Produto cadastrado com sucesso!", Alert.AlertType.INFORMATION)
                } else {
                    // Update existing product
                    val sql = """
                    UPDATE produtos 
                    SET nome = ?, descricao = ?, valor_unitario = ?, categoria = ?, 
                    unidade_medida = ?, estoque_minimo = ?, estoque_atual = ?, status = ?,
                    data_atualizacao = CURRENT_TIMESTAMP
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
                    stmt.setLong(9, produtoSelecionado!!.id)

                    stmt.executeUpdate()
                    showAlert("Sucesso", "Produto atualizado com sucesso!", Alert.AlertType.INFORMATION)
                }
            }

            // Reload products and clear form
            loadProducts()
            limparFormulario()
            produtoSelecionado = null

        } catch (e: SQLException) {
            showAlert("Erro ao salvar", e.message ?: "Erro ao acessar banco de dados")
        }
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
            showAlert("Erro de validação", mensagensErro.joinToString("\n"))
            return false
        }

        return true
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

        btnSalvar.text = "Atualizar"
        btnCancelar.isDisable = false
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
                prefWidth = 150.0
                cellFactory = Callback {
                    object : TableCell<Produto, Void>() {
                        private val editBtn = Button("Editar").apply {
                            styleClass.add("small-button")
                        }

                        private val deleteBtn = Button("Excluir").apply {
                            styleClass.add("small-button")
                            style = "-fx-background-color: #ff5252;"
                        }

                        private val box = HBox(5.0, editBtn, deleteBtn).apply {
                            alignment = Pos.CENTER
                        }

                        init {
                            editBtn.setOnAction {
                                val produto = tableRow.item
                                if (produto != null) {
                                    preencherFormulario(produto)
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
            }

            columns.addAll(
                codigoColumn, nomeColumn, categoriaColumn, valorColumn,
                estoqueColumn, statusColumn, acoesColumn
            )
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
        val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
            title = "Confirmar Exclusão"
            headerText = "Excluir Produto"
            contentText = "Tem certeza que deseja excluir o produto: ${produto.nome}?"
        }

        val result = alert.showAndWait()
        if (result.isPresent && result.get() == ButtonType.OK) {
            deleteProduct(produto)
        }
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