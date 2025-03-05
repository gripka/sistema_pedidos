package com.sistema_pedidos.view

import com.sistema_pedidos.controller.ClientesController
import com.sistema_pedidos.model.Cliente
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.application.Platform

class ClientesView : VBox(10.0) {
    private val tableView = TableView<Cliente>()
    private val clientes = FXCollections.observableArrayList<Cliente>()
    private val searchField = TextField()
    private val formPanel = VBox(10.0)
    private var clienteSelecionado: Cliente? = null
    private val controller = ClientesController()

    private lateinit var tfId: TextField
    private lateinit var tfNome: TextField
    private lateinit var tfSobrenome: TextField
    private lateinit var tfTelefone: TextField
    private lateinit var taObservacao: TextArea
    private lateinit var btnSalvar: Button
    private lateinit var btnCancelar: Button

    init {
        padding = Insets(20.0)
        prefWidth = 1000.0
        prefHeight = 700.0

        styleClass.add("main-container")
        stylesheets.add(javaClass.getResource("/produtosview.css").toExternalForm())

        setupUI()
        carregarClientes()
    }

    private fun setupUI() {
        val title = Label("Gerenciamento de Clientes").apply {
            style = "-fx-font-size: 24px; -fx-font-weight: bold;"
        }

        // Search controls
        searchField.apply {
            promptText = "Buscar clientes por nome ou telefone..."
            prefWidth = 300.0
            prefHeight = 36.0
        }

        val searchButton = Button("Buscar").apply {
            styleClass.add("primary-button")
            prefHeight = 36.0
            setOnAction { buscarClientes(searchField.text) }
        }

        val refreshButton = Button("Atualizar").apply {
            styleClass.add("primary-button")
            prefHeight = 36.0
            setOnAction { carregarClientes() }
        }

        val newButton = Button("Novo Cliente").apply {
            styleClass.add("primary-button")
            prefHeight = 36.0
            setOnAction { limparFormulario() }
        }

        val topControls = HBox(10.0, searchField, searchButton, refreshButton, newButton).apply {
            alignment = Pos.CENTER_LEFT
        }

        val headerBox = VBox(15.0, title, topControls).apply {
            padding = Insets(0.0, 0.0, 15.0, 0.0)
        }

        val splitPane = HBox(20.0).apply {
            children.addAll(
                createTableView(),
                createFormPanel()
            )
            HBox.setHgrow(tableView, Priority.ALWAYS)
        }

        VBox.setVgrow(splitPane, Priority.ALWAYS)

        children.addAll(headerBox, splitPane)
    }

    private fun createTableView(): TableView<Cliente> {
        tableView.apply {
            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            items = clientes
            styleClass.add("table-view")
        }

        setupTableColumns()

        VBox.setVgrow(tableView, Priority.ALWAYS)
        HBox.setHgrow(tableView, Priority.ALWAYS)

        return tableView
    }

    private fun setupTableColumns() {
        val idColumn = TableColumn<Cliente, Long>("ID").apply {
            cellValueFactory = PropertyValueFactory("id")
            isVisible = false
        }

        val nomeColumn = TableColumn<Cliente, String>("Nome").apply {
            cellValueFactory = PropertyValueFactory("nomeCompleto")
            prefWidth = 250.0
        }

        val telefoneColumn = TableColumn<Cliente, String>("Telefone").apply {
            cellValueFactory = PropertyValueFactory("telefone")
            prefWidth = 150.0
        }

        val obsColumn = TableColumn<Cliente, String>("Observação").apply {
            cellValueFactory = PropertyValueFactory("observacao")
            prefWidth = 200.0
        }

        val acoesColumn = TableColumn<Cliente, Void>("Ações").apply {
            prefWidth = 120.0
            cellFactory = javafx.util.Callback {
                object : TableCell<Cliente, Void>() {
                    private val editButton = Button("Editar").apply {
                        styleClass.add("small-button")
                        setOnAction {
                            val cliente = tableView.items[index]
                            preencherFormulario(cliente)
                        }
                    }

                    private val deleteButton = Button("Excluir").apply {
                        styleClass.add("small-button")
                        styleClass.add("botao-cancel")
                        setOnAction {
                            val cliente = tableView.items[index]
                            confirmarExclusao(cliente)
                        }
                    }

                    private val buttonBox = HBox(5.0, editButton, deleteButton).apply {
                        alignment = Pos.CENTER
                    }

                    override fun updateItem(item: Void?, empty: Boolean) {
                        super.updateItem(item, empty)
                        graphic = if (empty) null else buttonBox
                    }
                }
            }
        }

        tableView.columns.addAll(idColumn, nomeColumn, telefoneColumn, obsColumn, acoesColumn)

    }

    private fun createFormPanel(): VBox {
        tfId = TextField().apply {
            isEditable = false
            promptText = "ID (gerado automaticamente)"
            styleFormField(this)
            style = """${style ?: ""}
                -fx-background-color: #f0f0f0;
                -fx-opacity: 0.9;
                -fx-border-color: #cccccc;
            """
        }

        tfNome = TextField().apply {
            promptText = "Nome"
            styleFormField(this)
        }

        tfSobrenome = TextField().apply {
            promptText = "Sobrenome"
            styleFormField(this)
        }

        tfTelefone = TextField().apply {
            promptText = "Telefone"
            styleFormField(this)
        }
        formatarTelefone(tfTelefone)

        taObservacao = TextArea().apply {
            promptText = "Observações"
            prefHeight = 100.0
            style = """
                -fx-background-color: rgb(250, 251, 252);
                -fx-border-color: rgb(223, 225, 230);
                -fx-text-fill: rgb(9, 30, 66);
                -fx-border-radius: 3px;
                -fx-border-width: 2px;
                -fx-font-size: 14px;
            """
        }

        btnSalvar = Button("Salvar").apply {
            styleClass.add("primary-button")
            prefWidth = 120.0
            setOnAction { salvarCliente() }
        }

        btnCancelar = Button("Cancelar").apply {
            styleClass.add("secondary-button")
            prefWidth = 120.0
            setOnAction { limparFormulario() }
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
            prefWidth = 350.0
            maxWidth = 350.0
            style = """
                -fx-background-color: white;
                -fx-border-color: rgb(223, 225, 230);
                -fx-border-width: 2px;
                -fx-border-radius: 3px;
            """

            children.addAll(
                Label("Dados do Cliente").apply { style = "-fx-font-weight: bold; -fx-font-size: 16px;" },
                Label("ID:"),
                tfId,
                Label("Nome:"),
                tfNome,
                Label("Sobrenome:"),
                tfSobrenome,
                Label("Telefone:"),
                tfTelefone,
                Label("Observações:"),
                taObservacao,
                Separator().apply { padding = Insets(5.0, 0.0, 5.0, 0.0) },
                HBox(10.0, btnSalvar, btnCancelar).apply {
                    alignment = Pos.CENTER_RIGHT
                    padding = Insets(10.0, 0.0, 0.0, 0.0)
                }
            )
        }

        scrollPane.content = formPanel

        return VBox(scrollPane).apply {
            HBox.setHgrow(this, Priority.NEVER)
        }
    }

    private fun formatarTelefone(textField: TextField) {
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

    private fun styleFormField(field: TextField) {
        field.apply {
            prefHeight = 36.0
            styleClass.add("text-field")
        }
    }

    private fun limparFormulario() {
        clienteSelecionado = null
        tfId.text = ""
        tfNome.text = ""
        tfSobrenome.text = ""
        tfTelefone.text = ""
        taObservacao.text = ""
        btnSalvar.text = "Salvar"
    }

    private fun preencherFormulario(cliente: Cliente) {
        clienteSelecionado = cliente
        tfId.text = cliente.id.toString()
        tfNome.text = cliente.nome
        tfSobrenome.text = cliente.sobrenome ?: ""
        tfTelefone.text = cliente.telefone
        taObservacao.text = cliente.observacao ?: ""
        btnSalvar.text = "Atualizar"
    }

    private fun salvarCliente() {
        if (!validarFormulario()) return

        val nome = tfNome.text.trim()
        val sobrenome = tfSobrenome.text.trim()
        val telefone = tfTelefone.text.trim()
        val observacao = taObservacao.text.trim()

        val cliente = Cliente(
            id = clienteSelecionado?.id ?: 0,
            nome = nome,
            sobrenome = sobrenome,
            telefone = telefone,
            observacao = observacao
        )

        val resultado = if (clienteSelecionado == null) {
            controller.adicionarCliente(cliente)
        } else {
            controller.atualizarCliente(cliente)
        }

        if (resultado) {
            showAlert(
                "Sucesso",
                if (clienteSelecionado == null) "Cliente cadastrado com sucesso!"
                else "Cliente atualizado com sucesso!",
                Alert.AlertType.INFORMATION
            )
            carregarClientes()
            limparFormulario()
        }
    }

    private fun validarFormulario(): Boolean {
        val erros = mutableListOf<String>()

        if (tfNome.text.trim().isEmpty()) {
            erros.add("Nome é obrigatório")
        }

        if (tfTelefone.text.trim().isEmpty()) {
            erros.add("Telefone é obrigatório")
        }

        if (erros.isNotEmpty()) {
            showAlert(
                "Erro de Validação",
                erros.joinToString("\n"),
                Alert.AlertType.ERROR
            )
            return false
        }
        return true
    }

    private fun confirmarExclusao(cliente: Cliente) {
        val dialog = Dialog<ButtonType>()
        dialog.title = "Confirmar Exclusão"
        dialog.headerText = "Excluir Cliente"
        dialog.initStyle(javafx.stage.StageStyle.UNDECORATED)

        val buttonTypeConfirm = ButtonType("Excluir", ButtonBar.ButtonData.OK_DONE)
        val buttonTypeCancel = ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE)
        dialog.dialogPane.buttonTypes.addAll(buttonTypeConfirm, buttonTypeCancel)
        dialog.dialogPane.stylesheets.addAll(this.stylesheets)

        val content = VBox(10.0).apply {
            padding = Insets(20.0)
            children.add(Label("Tem certeza que deseja excluir o cliente '${cliente.nomeCompleto}'?").apply {
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
            if (controller.excluirCliente(cliente.id)) {
                showAlert("Sucesso", "Cliente excluído com sucesso!", Alert.AlertType.INFORMATION)
                carregarClientes()
                if (clienteSelecionado?.id == cliente.id) {
                    limparFormulario()
                }
            }
        }
    }

    private fun carregarClientes() {
        clientes.clear()
        clientes.addAll(controller.buscarTodosClientes())
    }

    private fun buscarClientes(termo: String) {
        clientes.clear()
        clientes.addAll(controller.buscarClientesPorTermo(termo))
    }

    private fun showAlert(title: String, message: String, type: Alert.AlertType = Alert.AlertType.ERROR) {
        val dialog = Dialog<ButtonType>()
        dialog.title = title
        dialog.headerText = title
        dialog.initStyle(javafx.stage.StageStyle.UNDECORATED)

        val buttonTypeOk = ButtonType("OK", ButtonBar.ButtonData.OK_DONE)
        dialog.dialogPane.buttonTypes.add(buttonTypeOk)
        dialog.dialogPane.stylesheets.addAll(this.stylesheets)

        val content = VBox(10.0).apply {
            padding = Insets(20.0)
            children.add(Label(message).apply {
                style = "-fx-font-size: 14px;"
            })
        }

        dialog.dialogPane.style = """
        -fx-background-color: white;
        -fx-border-color: #D3D3D3;
        -fx-border-width: 1px;
    """
        dialog.dialogPane.content = content

        val headerColor = when(type) {
            Alert.AlertType.INFORMATION -> "#4CAF50"
            Alert.AlertType.WARNING -> "#FFA500"
            Alert.AlertType.ERROR -> "#dc3545"
            else -> "#2B2D30"
        }

        dialog.dialogPane.lookup(".header-panel")?.style = """
        -fx-background-color: $headerColor;
        -fx-background-radius: 0;
    """

        val headerLabel = dialog.dialogPane.lookup(".header-panel .label") as? Label
        headerLabel?.style = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;"

        val okButton = dialog.dialogPane.lookupButton(buttonTypeOk)
        okButton.styleClass.add(if (type == Alert.AlertType.ERROR) "botao-cancel" else "primary-button")

        val buttonBar = dialog.dialogPane.lookup(".button-bar") as ButtonBar
        buttonBar.apply {
            buttonOrder = ButtonBar.BUTTON_ORDER_NONE
            buttonMinWidth = 100.0
            style = """
            -fx-background-color: white;
            -fx-alignment: center;
        """
            padding = Insets(0.0, 50.0, 0.0, 0.0)
        }

        dialog.showAndWait()
    }
}
//pare aqui